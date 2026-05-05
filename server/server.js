import { WebSocketServer } from 'ws';
import crypto from 'crypto';

const PORT = process.env.PORT || 8787;
const wss = new WebSocketServer({ port: PORT });
const clients = new Map();
const rooms = new Map();

function send(ws, payload) {
  if (ws.readyState === ws.OPEN) ws.send(JSON.stringify(payload));
}

function broadcast(room, payload, exceptClientId = null) {
  const ids = rooms.get(room) || new Set();
  for (const id of ids) {
    if (id === exceptClientId) continue;
    const peer = clients.get(id);
    if (peer) send(peer.ws, payload);
  }
}

wss.on('connection', (ws) => {
  const id = crypto.randomUUID().slice(0, 8);
  clients.set(id, { ws, room: null, clientType: 'unknown' });
  send(ws, { type: 'connected', clientId: id });

  ws.on('message', (buffer) => {
    let msg;
    try { msg = JSON.parse(buffer.toString()); } catch { return send(ws, { type: 'error', message: 'Invalid JSON' }); }

    const client = clients.get(id);
    const room = msg.room || client?.room || 'walktalk-channel-1';

    if (msg.type === 'join') {
      if (client.room && rooms.has(client.room)) rooms.get(client.room).delete(id);
      client.room = room;
      client.clientType = msg.clientType || 'unknown';
      if (!rooms.has(room)) rooms.set(room, new Set());
      rooms.get(room).add(id);
      send(ws, { type: 'joined', room, clientId: id });
      broadcast(room, { type: 'peer-joined', room, clientId: id, clientType: client.clientType }, id);
      return;
    }

    const forwarded = { ...msg, room, from: msg.from || id, serverTime: Date.now() };
    broadcast(room, forwarded, null);
  });

  ws.on('close', () => {
    const client = clients.get(id);
    if (client?.room && rooms.has(client.room)) {
      rooms.get(client.room).delete(id);
      broadcast(client.room, { type: 'peer-left', room: client.room, clientId: id });
    }
    clients.delete(id);
  });
});

console.log(`WalkTalk signaling server running on ws://localhost:${PORT}`);

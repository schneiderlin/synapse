/**
 * WebSocket Test Client
 * 
 * Usage:
 *   node bases/ws-test/client.mjs [client-id] [--format edn|json]
 * 
 * Options:
 *   --format edn|json  - Message format (default: edn)
 * 
 * Interactive commands (type and press Enter):
 *   ping              - Send a ping, expect pong reply
 *   echo <message>    - Echo a message back
 *   broadcast <msg>   - Broadcast to all clients
 *   send <id> <msg>   - Send to specific client
 *   clients           - List connected clients
 *   quit              - Exit
 */

import WebSocket from 'ws';
import readline from 'readline';

// Parse command-line arguments
let clientId = `client-${Date.now()}`;
let format = 'edn'; // default to EDN

for (let i = 2; i < process.argv.length; i++) {
  const arg = process.argv[i];
  if (arg === '--format' && i + 1 < process.argv.length) {
    format = process.argv[++i];
    if (format !== 'edn' && format !== 'json') {
      console.error('Error: --format must be "edn" or "json"');
      process.exit(1);
    }
  } else if (!arg.startsWith('--')) {
    clientId = arg;
  }
}

const WS_PORT = process.env.WS_PORT || 3333;
const url = `ws://localhost:${WS_PORT}/ws?client-id=${clientId}`;

console.log('');
console.log('=================================================');
console.log('  WebSocket Test Client');
console.log('=================================================');
console.log('');
console.log(`Client ID: ${clientId}`);
console.log(`Format: ${format.toUpperCase()}`);
console.log(`Connecting to: ${url}`);
console.log('');

const ws = new WebSocket(url);

ws.on('open', () => {
  console.log('✓ Connected!\n');
  console.log('Commands:');
  console.log('  ping              - Send ping');
  console.log('  echo <message>    - Echo message');
  console.log('  broadcast <msg>   - Broadcast to all');
  console.log('  send <id> <msg>   - Send to client');
  console.log('  clients           - List clients');
  console.log('  quit              - Exit');
  console.log('');
  rl.prompt();
});

ws.on('message', (data) => {
  try {
    const str = data.toString();
    // Try to parse based on format
    if (format === 'json') {
      const parsed = JSON.parse(str);
      console.log('\n← Received (JSON):', JSON.stringify(parsed, null, 2));
    } else {
      // EDN format - just display as-is
      console.log('\n← Received (EDN):', str);
    }
    rl.prompt();
  } catch (e) {
    console.log('\n← Raw:', data.toString());
    rl.prompt();
  }
});

ws.on('close', () => {
  console.log('\nConnection closed.');
  process.exit(0);
});

ws.on('error', (err) => {
  console.error('\nWebSocket error:', err.message);
  process.exit(1);
});

// Interactive command line
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  prompt: '> '
});

rl.on('line', (line) => {
  const parts = line.trim().split(' ');
  const cmd = parts[0];
  const args = parts.slice(1).join(' ');

  let message = null;

  switch (cmd) {
    case 'ping':
      message = { event: 'test/ping', data: {} };
      break;

    case 'echo':
      message = { event: 'test/echo', data: args || 'hello' };
      break;

    case 'broadcast':
      message = { event: 'test/broadcast', data: args || 'hello everyone!' };
      break;

    case 'send':
      const [targetId, ...msgParts] = args.split(' ');
      message = { 
        event: 'test/send-to', 
        data: { 'target-id': targetId, message: msgParts.join(' ') } 
      };
      break;

    case 'clients':
      message = { event: 'test/list-clients', data: {} };
      break;

    case 'quit':
    case 'exit':
      ws.close();
      return;

    case '':
      rl.prompt();
      return;

    default:
      console.log('Unknown command:', cmd);
      rl.prompt();
      return;
  }

  if (message) {
    // Serialize based on format
    let serialized;
    if (format === 'json') {
      serialized = JSON.stringify(message);
      console.log('→ Sending (JSON):', serialized);
    } else {
      serialized = toEdn(message);
      console.log('→ Sending (EDN):', serialized);
    }
    ws.send(serialized);
  }
  
  rl.prompt();
});

rl.on('close', () => {
  ws.close();
  process.exit(0);
});

// Simple EDN serializer for Clojure keywords
function toEdn(obj) {
  if (obj === null || obj === undefined) return 'nil';
  if (typeof obj === 'string') return `"${obj}"`;
  if (typeof obj === 'number') return obj.toString();
  if (typeof obj === 'boolean') return obj.toString();
  if (Array.isArray(obj)) {
    return '[' + obj.map(toEdn).join(' ') + ']';
  }
  if (typeof obj === 'object') {
    const pairs = Object.entries(obj).map(([k, v]) => {
      // Convert event to keyword
      const key = k === 'event' ? `:${v.replace('/', '/')}` : `:${k}`;
      const val = k === 'event' ? null : toEdn(v);
      return val !== null ? `${key} ${val}` : key;
    }).filter(Boolean);
    
    // Special handling for event key
    const eventEntry = Object.entries(obj).find(([k]) => k === 'event');
    const otherEntries = Object.entries(obj).filter(([k]) => k !== 'event');
    
    const ednPairs = [];
    if (eventEntry) {
      ednPairs.push(`:event :${eventEntry[1]}`);
    }
    for (const [k, v] of otherEntries) {
      ednPairs.push(`:${k} ${toEdn(v)}`);
    }
    
    return '{' + ednPairs.join(' ') + '}';
  }
  return obj.toString();
}


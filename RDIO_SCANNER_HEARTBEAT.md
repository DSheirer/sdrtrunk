# Rdio Scanner heartbeat integration

sdrtrunk can send an online heartbeat for each enabled Rdio Scanner streaming destination. This is separate from the
normal completed-call upload and is disabled by default for compatibility with existing Rdio Scanner servers.

## Configure sdrtrunk

Open **Playlist Editor > Streaming**, select the Rdio Scanner destination, and configure:

- **Send Heartbeat:** enables heartbeat delivery.
- **Heartbeat Interval (seconds):** normal delivery interval, with a minimum of 5 seconds.
- **Heartbeat URL:** optional full POST URL for your monitoring application. When blank, sdrtrunk changes the
  configured call URL from `/api/call-upload` to `/api/heartbeat`.

For example, a call URL of `https://scanner.example/api/call-upload` uses
`https://scanner.example/api/heartbeat` unless a different heartbeat URL is entered.

The normal completed-call request remains a multipart POST to `<Rdio Scanner URL>/api/call-upload`.

## HTTP request contract

The heartbeat is an HTTP `POST` with these headers:

```http
POST /api/heartbeat HTTP/1.1
Content-Type: application/json; charset=UTF-8
User-Agent: sdrtrunk
X-SDRTrunk-Event: heartbeat
X-RdioScanner-System-Id: 42
X-API-Key: your-rdio-api-key
```

The API key is sent only in the `X-API-Key` header and is not included in the JSON body.

Example body:

```json
{
  "event": "heartbeat",
  "status": "online",
  "connected": true,
  "timestamp": 1790150400000,
  "timestampIso": "2026-09-23T08:00:00Z",
  "application": "sdrtrunk",
  "version": "0.7.0-beta-23",
  "destination": "County Rdio",
  "hostname": "scanner-pi",
  "uptimeMs": 86400000,
  "systemId": 42,
  "queuedCalls": 0
}
```

Field meanings:

- `event`: always `heartbeat`.
- `status`: always `online` while the application is running.
- `connected`: whether the Rdio destination was connected when the body was created.
- `timestamp`: Unix time in milliseconds.
- `timestampIso`: the same timestamp in ISO-8601 UTC format.
- `application` and `version`: sender identity.
- `destination`: the Rdio destination name from Playlist Editor.
- `hostname`: host name of the sdrtrunk computer or Raspberry Pi.
- `uptimeMs`: Java application runtime in milliseconds.
- `systemId`: configured Rdio Scanner system ID.
- `queuedCalls`: completed calls currently waiting in the Rdio upload queue.

Return any HTTP status from `200` through `299` to accept the heartbeat. Network errors, timeouts, and non-2xx
responses replace the HTTP client and retry with exponential backoff capped at 60 seconds or the configured heartbeat
interval, whichever is shorter. Heartbeat health is tracked separately and does not override the call-upload
connection state.

## Call upload recovery and durable queue

Rdio Scanner call-upload connection checks retry indefinitely. The delay grows after consecutive failures and is
capped at 60 seconds. A destination remains in a temporary retrying state instead of stopping permanently.

Before an upload begins, sdrtrunk copies the completed audio and its Rdio metadata into a destination-specific queue
under the configured `streaming/rdioscanner_queue` directory. Queue entries are removed only after Rdio Scanner returns
`Call imported successfully.` or `duplicate call rejected`. The queue is retained when the destination is edited,
disabled, re-enabled, or when sdrtrunk is restarted. Do not manually delete this directory while calls are pending.

## Node/Express receiver example

```javascript
const crypto = require('node:crypto');
const scannerStatus = new Map();

function validApiKey(value) {
  const expected = process.env.SDRTRUNK_RDIO_API_KEY || '';
  const supplied = value || '';
  const a = Buffer.from(expected);
  const b = Buffer.from(supplied);
  return a.length > 0 && a.length === b.length && crypto.timingSafeEqual(a, b);
}

app.post('/api/heartbeat', express.json(), (req, res) => {
  if (req.get('X-SDRTrunk-Event') !== 'heartbeat') {
    return res.status(400).json({ error: 'not a heartbeat' });
  }

  if (!validApiKey(req.get('X-API-Key'))) {
    return res.status(401).json({ error: 'invalid API key' });
  }

  const systemId = Number(req.get('X-RdioScanner-System-Id'));
  const heartbeat = req.body;

  scannerStatus.set(systemId, {
    online: true,
    lastSeen: Date.now(),
    connected: heartbeat.connected,
    queuedCalls: heartbeat.queuedCalls,
    destination: heartbeat.destination,
    hostname: heartbeat.hostname,
    version: heartbeat.version
  });

  return res.sendStatus(204);
});
```

Your application can consider the scanner offline when `lastSeen` is older than two or three configured heartbeat
intervals. Compare API keys using a constant-time comparison when exposing the endpoint outside a trusted network,
and use HTTPS for any non-local connection.

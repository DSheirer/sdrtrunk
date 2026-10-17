# Remote Call API and heartbeat

The Remote Call API destination sends completed calls and optional online heartbeats to a user-supplied HTTP POST
URL. Both request types use the same authentication header, prefix, API key, timeout, and redirect policy.

## Configure a destination

Open the web console and select **Remote Calls > Add Destination**. Configure:

- **Name:** a unique destination name used by talkgroup alias routing.
- **POST URL:** the receiving API endpoint for calls and heartbeats.
- **API key:** optionally save a key in the playlist from the web editor.
- **API key environment variable:** defaults to `SDRTRUNK_REMOTE_API_KEY`; when set, its value takes priority over the
  saved key.
- **Authentication header/prefix:** defaults to `Authorization` and `Bearer `.
- **Send heartbeat:** enables or disables online heartbeats for this destination.
- **Heartbeat interval seconds:** how often to send, with a minimum of 5 seconds and a default of 60 seconds.
- **Keep retrying queued calls:** enabled by default. Failed calls remain queued and retry with bounded backoff until
  they upload successfully or the destination is deleted. When disabled, the retry count and maximum call age use the
  previous discard behavior.

Save the destination, then assign it to talkgroups under **Playlist > Talkgroups & Aliases**.

## Heartbeat request

When enabled, sdrtrunk immediately sends a heartbeat when the destination starts and repeats it at the selected
interval. The request uses `Content-Type: application/json` and includes `X-SDRTrunk-Event: heartbeat` so the receiving
server can distinguish it from an audio upload.

Example:

```http
POST /calls HTTP/1.1
Content-Type: application/json; charset=UTF-8
Authorization: Bearer your-api-key
User-Agent: sdrtrunk
X-SDRTrunk-Event: heartbeat
```

```json
{
  "event": "heartbeat",
  "status": "online",
  "connected": true,
  "timestamp": 1788667200000,
  "timestampIso": "2026-09-06T12:00:00Z",
  "application": "sdrtrunk",
  "version": "0.7.0-beta-24",
  "destination": "Dispatch API",
  "hostname": "scanner-host",
  "uptimeMs": 86400000,
  "queuedCalls": 0
}
```

The receiving server must return any HTTP 2xx status. A successful response updates the destination to **Connected**
and records the latest success time in the web console. A network failure, timeout, stale connection, or non-2xx
response changes the state to **Connecting**, discards the current pooled HTTP connection, and retries with bounded
exponential backoff. Healthy heartbeats continue at the configured interval. The Remote Calls window shows the last
successful contact, HTTP error details, recovery count, and a manual **Reconnect** action.

A completed-call watchdog also covers the combined speech-processing and upload operation. If that operation stops
making progress, sdrtrunk cancels it, rebuilds the HTTP client, returns the call to the queue, and continues retrying.
The manual **Reconnect** action performs the same immediate requeue for every active upload.

Turning the heartbeat off stops heartbeat requests but does not disable call uploads. Disabling the entire destination
stops both calls and heartbeats. Disabling, editing, or re-enabling a destination retains its existing queued calls in
memory; deleting the destination intentionally releases them. A full application exit cannot preserve an in-memory
queue, so avoid shutting down while Remote Calls shows queued items.

An idle HTTP destination cannot be health-checked when heartbeat is disabled. Enable heartbeat for continuous stale
connection detection. With heartbeat disabled, a failed completed-call upload still rebuilds the HTTP connection and
uses the configured call retry policy. With **Keep retrying queued calls** enabled, the delay increases up to five
minutes during a prolonged outage and delivery continues automatically after the endpoint returns.

## Completed-call request

Completed calls use `multipart/form-data` with:

- `metadata`: JSON describing the call ID, timestamp, duration, frequency, identifiers, and optional transcription.
- `audio`: the completed MP3 recording.

The call request also includes `User-Agent: sdrtrunk`, the configured authentication header, and an `Idempotency-Key`
containing the stable call ID. The receiving API should use that key to avoid creating duplicate calls during retries.

## Example receiver behavior

The same endpoint can branch using the content type or event header:

```text
if X-SDRTrunk-Event == "heartbeat":
    parse JSON body
    update scanner last-seen/online status
    return HTTP 204
else:
    parse multipart metadata and audio
    store the completed call
    return HTTP 201
```

For online monitoring, consider a scanner offline after it has missed two or three expected heartbeat intervals.

## Security

Prefer the configured environment variable instead of saving an API key in the playlist. The web API reports only
whether a key resolves and never returns its value. Use HTTPS for destinations outside the trusted local network.
Heartbeats contain the scanner host name, software version, uptime, and queue size, but do not contain talkgroup or
audio data.

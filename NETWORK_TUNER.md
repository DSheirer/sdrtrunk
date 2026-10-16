# rtl_tcp Network Tuner Setup

sdrtrunk 0.7.0 beta 24 can use an RTL-SDR attached to another Raspberry Pi or Linux computer. The remote computer
runs `rtl_tcp`; sdrtrunk receives the raw I/Q samples over the network and uses them like a local tuner.

This feature is adapted from upstream pull request [DSheirer/sdrtrunk#2471](https://github.com/DSheirer/sdrtrunk/pull/2471)
and should be treated as experimental while that proposal remains under review.

## Start rtl_tcp on the SDR computer

Install the RTL-SDR tools using your operating system package manager. On Raspberry Pi OS or Debian/Ubuntu:

```bash
sudo apt update
sudo apt install rtl-sdr
```

Stop any service that has already claimed the dongle, then start the server:

```bash
rtl_tcp -a 0.0.0.0 -p 1234
```

The default `rtl_tcp` port is 1234. Allow that TCP port through the host firewall only for the computer running
sdrtrunk.

`rtl_tcp` does not encrypt traffic or authenticate clients. Do not expose it directly to the internet. Use a trusted
LAN, a VPN, or an SSH tunnel when the tuner is at another location.

## Add the tuner in the Java desktop

1. Open **Tuners**.
2. Select **Add Network Tuner**.
3. Enter the remote host/IP address, port, and initial center frequency in hertz.
4. Select **Add**. The tuner starts immediately and is saved for future launches.
5. Select the tuner to configure sample rate, automatic or manual gain, Bias-T, and **Auto-Reconnect**.

The Status field distinguishes the sdrtrunk tuner state from the network state. During an outage it shows
**Enabled - Reconnecting**, the attempt number, and the last socket error. Reconnection continues every ten seconds
after the initial 1, 2, and 5 second retries.

## Add or manage the tuner in the web console

1. Open `http://<sdrtrunk-host>:10000/` and sign in.
2. Open **System** from the top menu.
3. Select **Add Network Tuner** and enter the `rtl_tcp` host, port, center frequency, and sample rate.
4. Use **Disable**, **Enable**, **Reconnect**, or **Delete** from the tuner table.

Only enabled tuners appear in the Spectrum tuner selector. Once connected, an `rtl_tcp` tuner can feed the web
spectrum/waterfall and any compatible Playlist channels within its sampled bandwidth.

## Bandwidth and network use

RTL-SDR samples are unsigned 8-bit interleaved I/Q values, or two bytes per complex sample. A 2.4 MSPS tuner therefore
uses about 4.8 MB/s (38.4 Mbit/s) before TCP overhead. Wired Ethernet is recommended. The tuner can decode multiple
channels at once when all channel frequencies fit inside the selected sample-rate bandwidth, just like a locally
attached RTL-SDR.

## Troubleshooting

- **Connection refused:** confirm `rtl_tcp` is running, the host/port are correct, and the firewall permits the client.
- **Handshake timeout:** another service may be listening on that port, or a proxy is accepting TCP without passing the
  `rtl_tcp` protocol.
- **Audio gaps:** prefer wired Ethernet, lower the sample rate if the desired channels still fit, and check for packet
  loss or an overloaded remote computer.
- **Device in use:** stop another SDR application or service that has claimed the RTL-SDR before starting `rtl_tcp`.
- **Bias-T warning:** enable Bias-T only when the connected hardware and antenna accessory require and support it.

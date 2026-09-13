/*
 * *****************************************************************************
 * Copyright (C) 2026 Jared Szechy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.source.tuner.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.buffer.ByteNativeBufferFactory;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;

/**
 * Tuner controller for an rtl_tcp network tuner. Connects to a remote rtl_tcp server over TCP,
 * performs the rtl_tcp handshake/command protocol and streams unsigned 8-bit interleaved I/Q
 * samples from the server into the tuner's native buffer pipeline.
 */
public class NetworkTunerController extends TunerController {
  private final static Logger mLog = LoggerFactory.getLogger(NetworkTunerController.class);

  public static final int DC_NOISE_BANDWIDTH = 0;
  public static final double USABLE_BANDWIDTH_PERCENTAGE = 1.00;

  private static final int HANDSHAKE_LENGTH = 12;
  private static final int CONNECT_TIMEOUT_MS = 5000;
  private static final int READ_BUFFER_SIZE = 65536; //bytes - matches RTL2832's USB transfer buffer size
  //ByteNativeBuffer's iterator consumes 8192 complex (I/Q) samples per fragment, i.e. 16384 bytes, so buffers
  //broadcast to it must be an even multiple of 16384 bytes, not just 8192.
  private static final int FRAGMENT_SIZE = 16384;
  private static final long[] RECONNECT_DELAYS_MS = {1000, 2000, 5000, 10000};

  //rtl_tcp client command identifiers (see librtlsdr rtl_tcp.c)
  private static final int CMD_SET_FREQUENCY = 0x01;
  private static final int CMD_SET_SAMPLE_RATE = 0x02;
  private static final int CMD_SET_GAIN_MODE = 0x03;
  private static final int CMD_SET_GAIN = 0x04;
  private static final int CMD_SET_BIAS_TEE = 0x0E;

  private final String mHost;
  private final int mPort;
  private long mCenterFrequency;
  private long mSampleRate;
  private boolean mGainAuto;
  private int mGain;
  private boolean mBiasTEnabled;
  private boolean mAutoReconnect;

  private final ByteNativeBufferFactory mNativeBufferFactory = new ByteNativeBufferFactory();
  private volatile OutputStream mOutputStream;
  private volatile boolean mRunning;
  private Thread mConnectionThread;

  public NetworkTunerController(ITunerErrorListener tunerErrorListener, NetworkTunerConfiguration config) {
    super(tunerErrorListener);
    mHost = config.getHost();
    mPort = config.getPort() & 0xFFFF; //port is stored as a signed short - treat as unsigned
    mCenterFrequency = config.getFrequency();
    if (mCenterFrequency == 0) {
      mCenterFrequency = 100000000; // Default to 100 MHz if no frequency is provided
    }
    mSampleRate = config.getSampleRate();
    mGainAuto = config.isGainAuto();
    mGain = config.getGain();
    mBiasTEnabled = config.isBiasTEnabled();
    mAutoReconnect = config.isAutoReconnect();

    setMinimumFrequency(1000000L); // 1 MHz
    setMaximumFrequency(3000000000L); // 3 GHz
    setMiddleUnusableHalfBandwidth(DC_NOISE_BANDWIDTH);
    setUsableBandwidthPercentage(USABLE_BANDWIDTH_PERCENTAGE);
    mNativeBufferFactory.setSamplesPerMillisecond(mSampleRate / 1000.0f);
  }

  @Override
  public void start() throws SourceException {
    if (mRunning) {
      return;
    }

    //Broadcasts the sample rate to downstream listeners (e.g. the channel source manager) - without
    //this, getSampleRate() remains 0 and channelizing never starts.
    mFrequencyController.setSampleRate((int) mSampleRate);

    mRunning = true;
    mConnectionThread = new Thread(this::runConnectionLoop, "network-tuner-" + mHost + ":" + mPort);
    mConnectionThread.setDaemon(true);
    mConnectionThread.start();
  }

  @Override
  public void stop() {
    mRunning = false;

    Thread thread = mConnectionThread;
    mConnectionThread = null;

    if (thread != null) {
      thread.interrupt();
    }
  }

  /**
   * Runs on a background thread for the lifetime of this controller (until stop() is called).
   * Connects, streams samples until the connection drops or is closed, and - when auto-reconnect
   * is enabled - retries with an increasing backoff delay. When auto-reconnect is disabled, a
   * connection failure is reported as a tuner error and the loop exits.
   */
  private void runConnectionLoop() {
    int attempt = 0;

    while (mRunning) {
      try {
        connectAndStream();
        attempt = 0;
      } catch (IOException ioe) {
        if (!mRunning) {
          return; //stop() was called - exit quietly
        }

        if (!mAutoReconnect) {
          mLog.error("Network tuner [" + mHost + ":" + mPort + "] connection error", ioe);
          setErrorMessage("Error - " + ioe.getMessage());
          mRunning = false;
          return;
        }

        long delay = RECONNECT_DELAYS_MS[Math.min(attempt, RECONNECT_DELAYS_MS.length - 1)];
        attempt++;
        mLog.warn("Network tuner [" + mHost + ":" + mPort + "] disconnected - reconnecting in " + delay
            + " ms - " + ioe.getMessage());

        try {
          Thread.sleep(delay);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  /**
   * Connects to the rtl_tcp server, performs the handshake and initial command sequence, and then
   * blocks streaming samples until the connection is closed (by either side) or stop() is called.
   */
  private void connectAndStream() throws IOException {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(mHost, mPort), CONNECT_TIMEOUT_MS);

      InputStream in = socket.getInputStream();
      OutputStream out = socket.getOutputStream();
      mOutputStream = out;

      readAndValidateHandshake(in);

      sendCommand(out, CMD_SET_SAMPLE_RATE, (int) mSampleRate);
      sendCommand(out, CMD_SET_FREQUENCY, (int) mCenterFrequency);
      sendCommand(out, CMD_SET_GAIN_MODE, mGainAuto ? 0 : 1);
      if (!mGainAuto) {
        sendCommand(out, CMD_SET_GAIN, mGain);
      }
      sendCommand(out, CMD_SET_BIAS_TEE, mBiasTEnabled ? 1 : 0);

      mLog.info("Network tuner connected [" + mHost + ":" + mPort + "]");

      byte[] readBuffer = new byte[READ_BUFFER_SIZE];
      byte[] pending = new byte[0]; //leftover bytes (< FRAGMENT_SIZE) carried over from the previous read
      int read;

      while (mRunning && (read = in.read(readBuffer)) != -1) {
        //Socket reads rarely align to FRAGMENT_SIZE (ByteNativeBuffer requires an even multiple of it),
        //so accumulate leftover bytes across reads and only broadcast full FRAGMENT_SIZE-aligned chunks.
        byte[] combined = new byte[pending.length + read];
        System.arraycopy(pending, 0, combined, 0, pending.length);
        System.arraycopy(readBuffer, 0, combined, pending.length, read);

        int usableLength = combined.length - (combined.length % FRAGMENT_SIZE);
        if (usableLength > 0) {
          ByteBuffer samples = ByteBuffer.wrap(combined, 0, usableLength).slice();
          broadcast(mNativeBufferFactory.getBuffer(samples, System.currentTimeMillis()));
        }
        pending = Arrays.copyOfRange(combined, usableLength, combined.length);
      }

      if (mRunning) {
        throw new EOFException("Network tuner connection closed by remote host");
      }
    } finally {
      mOutputStream = null;
    }
  }

  /**
   * Reads and validates the 12-byte rtl_tcp handshake header: 4-byte "RTL0" magic, followed by a
   * 4-byte big endian tuner type and a 4-byte big endian tuner gain count (informational only).
   */
  void readAndValidateHandshake(InputStream in) throws IOException {
    byte[] header = readFully(in, HANDSHAKE_LENGTH);

    if (header[0] != 'R' || header[1] != 'T' || header[2] != 'L' || header[3] != '0') {
      throw new IOException("Unexpected rtl_tcp handshake magic from [" + mHost + ":" + mPort + "]");
    }
  }

  /**
   * Reads exactly length bytes from the stream, blocking until enough bytes are available.
   */
  static byte[] readFully(InputStream in, int length) throws IOException {
    byte[] buffer = new byte[length];
    int offset = 0;

    while (offset < length) {
      int read = in.read(buffer, offset, length - offset);

      if (read == -1) {
        throw new EOFException("Unexpected end of stream reading rtl_tcp handshake");
      }

      offset += read;
    }

    return buffer;
  }

  /**
   * Encodes an rtl_tcp client command as a 5-byte packet: 1-byte command id followed by a 4-byte
   * big endian unsigned integer parameter.
   */
  static byte[] encodeCommand(int commandId, int parameter) {
    return ByteBuffer.allocate(5).put((byte) commandId).putInt(parameter).array();
  }

  private void sendCommand(OutputStream out, int commandId, int parameter) throws IOException {
    out.write(encodeCommand(commandId, parameter));
    out.flush();
  }

  /**
   * Sends a command to the currently connected server, if any, ignoring (but logging and
   * reporting) any error. Used for applying live configuration changes from the tuner editor.
   */
  private void trySendCommand(int commandId, int parameter) {
    OutputStream out = mOutputStream;

    if (out != null) {
      try {
        sendCommand(out, commandId, parameter);
      } catch (IOException ioe) {
        mLog.error("Error sending command [" + commandId + "] to network tuner", ioe);
        setErrorMessage("Error sending command to network tuner - " + ioe.getMessage());
      }
    }
  }

  @Override
  public TunerType getTunerType() {
    return TunerType.RTL_TCP; // TODO: support more than this.
  }

  public String getHost() {
    return mHost;
  }

  public int getPort() {
    return mPort;
  }

  @Override
  public int getBufferSampleCount() {
    return READ_BUFFER_SIZE / 2; //2 bytes (I,Q) per complex sample
  }

  @Override
  public long getTunedFrequency() throws SourceException {
    return mCenterFrequency;
  }

  @Override
  public void setTunedFrequency(long frequency) throws SourceException {
    mCenterFrequency = frequency;
    trySendCommand(CMD_SET_FREQUENCY, (int) frequency);
  }

  @Override
  public double getCurrentSampleRate() {
    return mSampleRate;
  }

  public boolean isGainAuto() {
    return mGainAuto;
  }

  /**
   * Sets the gain mode and, when switching to manual, (re)sends the current gain value. Applies
   * immediately if connected; otherwise takes effect on the next connection.
   */
  public void setGainAuto(boolean gainAuto) {
    mGainAuto = gainAuto;
    trySendCommand(CMD_SET_GAIN_MODE, gainAuto ? 0 : 1);
    if (!gainAuto) {
      trySendCommand(CMD_SET_GAIN, mGain);
    }
  }

  public int getGain() {
    return mGain;
  }

  /**
   * Sets the manual gain value (tenths of dB). Only sent to the server when gain mode is manual.
   */
  public void setGain(int gain) {
    mGain = gain;
    if (!mGainAuto) {
      trySendCommand(CMD_SET_GAIN, gain);
    }
  }

  public boolean isBiasTEnabled() {
    return mBiasTEnabled;
  }

  public void setBiasTEnabled(boolean enabled) {
    mBiasTEnabled = enabled;
    trySendCommand(CMD_SET_BIAS_TEE, enabled ? 1 : 0);
  }

  public boolean isAutoReconnect() {
    return mAutoReconnect;
  }

  public void setAutoReconnect(boolean autoReconnect) {
    mAutoReconnect = autoReconnect;
  }

  public long getSampleRateConfigured() {
    return mSampleRate;
  }

  /**
   * Sets the sample rate. Applies immediately if connected; otherwise takes effect on the next
   * connection.
   */
  public void setSampleRate(long sampleRate) {
    mSampleRate = sampleRate;
    mNativeBufferFactory.setSamplesPerMillisecond(sampleRate / 1000.0f);

    try {
      mFrequencyController.setSampleRate((int) sampleRate);
    } catch (SourceException se) {
      mLog.error("Error updating sample rate", se);
      setErrorMessage("Error updating sample rate - " + se.getMessage());
    }

    trySendCommand(CMD_SET_SAMPLE_RATE, (int) sampleRate);
  }
}

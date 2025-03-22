package io.github.dsheirer.source.tuner.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;

public class NetworkTunerController extends TunerController {
  private final static Logger mLog = LoggerFactory.getLogger(NetworkTunerController.class);

  public static final int DC_NOISE_BANDWIDTH = 0;
  public static final double USABLE_BANDWIDTH_PERCENTAGE = 1.00;

  private String mHost;
  private short mPort;
  private long mCenterFrequency;
  private boolean mRunning;

  public NetworkTunerController(ITunerErrorListener tunerErrorListener, String host, short port, long centerFrequency) {
    super(tunerErrorListener);
    mHost = host;
    mPort = port;
    mCenterFrequency = centerFrequency;
    if (centerFrequency == 0) {
      mCenterFrequency = 100000000; // Default to 100 MHz if no frequency is provided
    }

    setMinimumFrequency(1000000L); // 1 MHz
    setMaximumFrequency(3000000000L); // 3 GHz
    setMiddleUnusableHalfBandwidth(DC_NOISE_BANDWIDTH);
    setUsableBandwidthPercentage(USABLE_BANDWIDTH_PERCENTAGE);
  }

  @Override
  public void start() throws SourceException {
  }

  @Override
  public void stop() {

  }

  @Override
  public TunerType getTunerType() {
    return TunerType.RTL_TCP; // TODO: fix
  }

  @Override
  public int getBufferSampleCount() {
    // TODO: fix me
    return 0;
  }

  @Override
  public long getTunedFrequency() throws SourceException {
    return mCenterFrequency;
  }

  @Override
  public void setTunedFrequency(long frequency) throws SourceException {
    mCenterFrequency = frequency;
  }

  @Override
  public double getCurrentSampleRate() {
    // TODO: fixme
    return 0d;
  }
}

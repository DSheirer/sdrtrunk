package io.github.dsheirer.source.tuner.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.manager.HeterodyneChannelSourceManager;
import io.github.dsheirer.source.tuner.manager.PassThroughSourceManager;
import io.github.dsheirer.source.tuner.manager.PolyphaseChannelSourceManager;

public class NetworkTuner extends Tuner {
  private final static Logger mLog = LoggerFactory.getLogger(NetworkTuner.class);
  private static int mInstanceCounter = 1;
  private final int mInstanceID = mInstanceCounter++;
  private UserPreferences mUserPreferences;

  public NetworkTuner(UserPreferences userPreferences, ITunerErrorListener tunerErrorListener,
      NetworkTunerConfiguration config) {
    super(new NetworkTunerController(tunerErrorListener, config.getHost(), config.getPort(), config.getFrequency()),
        tunerErrorListener);

    mUserPreferences = userPreferences;
  }

  @Override
  public void start() throws SourceException {
    super.start();

    if (getTunerController().getCurrentSampleRate() < 100000.0d) {
      setChannelSourceManager(new PassThroughSourceManager(getTunerController()));
    } else {
      ChannelizerType channelizerType = mUserPreferences.getTunerPreference().getChannelizerType();

      if (channelizerType == ChannelizerType.POLYPHASE) {
        setChannelSourceManager(new PolyphaseChannelSourceManager(getTunerController()));
      } else if (channelizerType == ChannelizerType.HETERODYNE) {
        setChannelSourceManager(new HeterodyneChannelSourceManager(getTunerController()));
      } else {
        throw new IllegalArgumentException("Unrecognized channelizer type: " + channelizerType);
      }
    }
  }

  @Override
  public String getPreferredName() {
    return "Network Tuner #" + mInstanceID;
  }

  public NetworkTunerController getTunerController() {
    return (NetworkTunerController) super.getTunerController();
  }

  @Override
  public String getUniqueID() {
    return getPreferredName();
  }

  @Override
  public TunerClass getTunerClass() {
    return TunerClass.NETWORK_TUNER;
  }

  @Override
  public TunerType getTunerType() {
    return TunerType.RTL_TCP; // TODO: support more than this.
  }

  @Override
  public double getSampleSize() {
    return 16.0; // TODO: fix me
  }

  @Override
  public int getMaximumUSBBitsPerSecond() {
    return 0;
  }
}

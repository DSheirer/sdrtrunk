package io.github.dsheirer.source.tuner.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.network.NetworkTunerConfiguration;

public class DiscoveredNetworkTuner extends DiscoveredTuner {
  private static final Logger mLog = LoggerFactory.getLogger(DiscoveredNetworkTuner.class);
  private UserPreferences mUserPreferences;

  public DiscoveredNetworkTuner(UserPreferences userPreferences, NetworkTunerConfiguration networkTunerConfiguration) {
    mUserPreferences = userPreferences;
    setTunerConfiguration(networkTunerConfiguration);

    // Default all network tuners to be disabled on startup
    setEnabled(false);
  }

  public NetworkTunerConfiguration getNetworkTunerConfiguration() {
    return (NetworkTunerConfiguration) getTunerConfiguration();
  }

  @Override
  public TunerClass getTunerClass() {
    return TunerClass.NETWORK_TUNER;
  }

  @Override
  public String getId() {
    return getNetworkTunerConfiguration().getHost();
  }

  @Override
  public void start() {

  }

  @Override
  public String toString() {
    return "Network [" + getNetworkTunerConfiguration().getHost() + ":" + getNetworkTunerConfiguration().getPort()
        + "]";
  }
}

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
package io.github.dsheirer.source.tuner.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.network.NetworkTuner;
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
    return "RTL TCP " + getNetworkTunerConfiguration().getHost() + ":" + getNetworkTunerConfiguration().getPort();
  }

  @Override
  public void start() {
    if (!hasTuner()) {
      mTuner = new NetworkTuner(mUserPreferences, this, getNetworkTunerConfiguration());

      try {
        mTuner.start();
      } catch (SourceException se) {
        setErrorMessage("Error - " + se.getMessage());
      }
    }
  }

  @Override
  public String toString() {
    return getId();
  }
}

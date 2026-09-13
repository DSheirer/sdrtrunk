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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerType;

public class NetworkTuner extends Tuner {
  private final static Logger mLog = LoggerFactory.getLogger(NetworkTuner.class);

  public NetworkTuner(UserPreferences userPreferences, ITunerErrorListener tunerErrorListener,
      NetworkTunerConfiguration config) {
    super(new NetworkTunerController(tunerErrorListener, config), tunerErrorListener,
        userPreferences.getTunerPreference().getChannelizerType());
  }

  @Override
  public String getPreferredName() {
    return "RTL TCP " + getTunerController().getHost() + ":" + getTunerController().getPort();
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
    // Note: although sample size is 8, we set it to 11 to align with the
    // actual noise floor.
    return 11.0;
  }

  @Override
  public int getMaximumUSBBitsPerSecond() {
    // 16 bits per sample * 2.4 MSPS
    return 38400000;
  }
}

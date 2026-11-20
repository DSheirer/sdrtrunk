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

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import javafx.scene.control.Label;

/**
 * Editor for a network tuner that is disabled or not connected.  Shows the status and the enable switch only.
 */
public class NetworkTunerDisabledEditor extends TunerEditor<NetworkTuner, NetworkTunerConfiguration> {
  public NetworkTunerDisabledEditor(UserPreferences userPreferences, TunerManager tunerManager,
      DiscoveredTuner discoveredTuner) {
    super(userPreferences, tunerManager, discoveredTuner);
    TunerSection section = addSection("Network Tuner");
    Label message = new Label("Enable the tuner to connect to the rtl_tcp server and change its settings.");
    message.setWrapText(true);
    section.addFullRow(message);
    refresh();
  }

  @Override
  protected boolean hasTuningControls() {
    return false;
  }

  @Override
  public long getMinimumTunableFrequency() {
    //Bogus value - matches NetworkTunerEditor's range since there's no live tuner to query yet.
    return 52000000;
  }

  @Override
  public long getMaximumTunableFrequency() {
    //Bogus value - matches NetworkTunerEditor's range since there's no live tuner to query yet.
    return 2200000000L;
  }

  @Override
  protected void save() {
    //No-op
  }

  @Override
  protected void tunerStatusUpdated() {
    //No-op - the header shows the status
  }
}

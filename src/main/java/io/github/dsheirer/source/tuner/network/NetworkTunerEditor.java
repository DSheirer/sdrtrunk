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
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

/**
 * Editor for an rtl_tcp network tuner.
 */
public class NetworkTunerEditor extends TunerEditor<NetworkTuner, NetworkTunerConfiguration> {
  private static final List<Integer> R820T_GAINS_TENTHS_DB = List.of(0, 9, 14, 27, 37, 77, 87, 125, 144, 157, 166,
      197, 207, 229, 254, 280, 297, 328, 338, 364, 372, 386, 402, 421, 434, 439, 445, 480, 496);

  private CheckBox mBiasTCheckBox;
  private ComboBox<SampleRate> mSampleRateCombo;
  private CheckBox mGainAutoCheckBox;
  private ComboBox<Integer> mGainCombo;
  private CheckBox mAutoReconnectCheckBox;

  public NetworkTunerEditor(UserPreferences userPreferences, TunerManager tunerManager,
      DiscoveredTuner discoveredTuner) {
    super(userPreferences, tunerManager, discoveredTuner);
    init();
    refresh();
  }

  @Override
  public long getMinimumTunableFrequency() {
    return 52000000;
  }

  @Override
  public long getMaximumTunableFrequency() {
    return 2200000000L;
  }

  private void init() {
    getTuningSection().addRow("Sample Rate", getSampleRateCombo());

    TunerSection gain = addSection("Gain");
    gain.addRow("Gain", getGainCombo(), getGainAutoCheckBox());

    TunerSection options = addSection("Options");
    options.addControl(getBiasTCheckBox());
    options.addControl(getAutoReconnectCheckBox());
  }

  @Override
  protected void tunerStatusUpdated() {
    getBiasTCheckBox().setDisable(!hasTuner());
    getGainAutoCheckBox().setDisable(!hasTuner());
    getAutoReconnectCheckBox().setDisable(!hasTuner());
    updateSampleRateControl();

    if (hasConfiguration()) {
      getBiasTCheckBox().setSelected(getConfiguration().isBiasTEnabled());
      getSampleRateCombo().setValue(closestSampleRate(getConfiguration().getSampleRate()));
      getGainAutoCheckBox().setSelected(getConfiguration().isGainAuto());
      getGainCombo().setValue(closestGain(getConfiguration().getGain()));
      getGainCombo().setDisable(!hasTuner() || getConfiguration().isGainAuto());
      getAutoReconnectCheckBox().setSelected(getConfiguration().isAutoReconnect());
    } else {
      getGainCombo().setDisable(true);
    }
  }

  @Override
  protected void tunerLockStateUpdated(boolean locked) {
    updateSampleRateControl();
  }

  private void updateSampleRateControl() {
    getSampleRateCombo().setDisable(!hasTuner() || getTuner().getTunerController().isLockedSampleRate());
  }

  @Override
  protected String getTunerInfo() {
    if (!hasConfiguration()) {
      return null;
    }

    return "<html><h3>rtl_tcp Network Tuner</h3>" +
        "<b>Host: </b>" + getConfiguration().getHost() + "<br>" +
        "<b>Port: </b>" + getConfiguration().getPort() + "<br>" +
        "<b>Tuner Type: </b>rtl_tcp<br>";
  }

  private CheckBox getBiasTCheckBox() {
    if (mBiasTCheckBox == null) {
      mBiasTCheckBox = new CheckBox("Bias-T");
      mBiasTCheckBox.setTooltip(new Tooltip("Ask the rtl_tcp server to provide DC power on the antenna connector"));
      mBiasTCheckBox.setDisable(true);
      mBiasTCheckBox.setOnAction(e -> {
        if (!isLoading()) {
          if (hasTuner()) {
            getTuner().getTunerController().setBiasTEnabled(mBiasTCheckBox.isSelected());
          }
          save();
        }
      });
    }

    return mBiasTCheckBox;
  }

  private ComboBox<SampleRate> getSampleRateCombo() {
    if (mSampleRateCombo == null) {
      mSampleRateCombo = new ComboBox<>(FXCollections.observableArrayList(SampleRate.values()));
      mSampleRateCombo.setMaxWidth(Double.MAX_VALUE);
      mSampleRateCombo.setDisable(true);
      mSampleRateCombo.setOnAction(e -> {
        SampleRate sampleRate = mSampleRateCombo.getValue();

        if (!isLoading() && sampleRate != null) {
          if (hasTuner()) {
            getTuner().getTunerController().setSampleRate(sampleRate.getRate());
            adjustForSampleRate(sampleRate.getRate());
          }
          save();
        }
      });
    }

    return mSampleRateCombo;
  }

  private CheckBox getGainAutoCheckBox() {
    if (mGainAutoCheckBox == null) {
      mGainAutoCheckBox = new CheckBox("Auto");
      mGainAutoCheckBox.setTooltip(new Tooltip("Let the rtl_tcp server's tuner set the gain automatically"));
      mGainAutoCheckBox.setOnAction(e -> {
        boolean gainAuto = mGainAutoCheckBox.isSelected();
        getGainCombo().setDisable(!hasTuner() || gainAuto);

        if (!isLoading()) {
          if (hasTuner()) {
            getTuner().getTunerController().setGainAuto(gainAuto);
          }
          save();
        }
      });
    }

    return mGainAutoCheckBox;
  }

  private ComboBox<Integer> getGainCombo() {
    if (mGainCombo == null) {
      mGainCombo = new ComboBox<>(FXCollections.observableArrayList(R820T_GAINS_TENTHS_DB));
      mGainCombo.setMaxWidth(Double.MAX_VALUE);
      mGainCombo.setDisable(true);
      mGainCombo.setTooltip(new Tooltip("Manual gain.  Uncheck Auto to enable manual gain."));
      mGainCombo.setConverter(new StringConverter<>() {
        @Override
        public String toString(Integer gain) {
          return gain == null ? "" : String.format("%.1f dB", gain / 10.0);
        }

        @Override
        public Integer fromString(String text) {
          return null;
        }
      });
      mGainCombo.setOnAction(e -> {
        Integer gain = mGainCombo.getValue();

        if (!isLoading() && gain != null) {
          if (hasTuner()) {
            getTuner().getTunerController().setGain(gain);
          }
          save();
        }
      });
    }

    return mGainCombo;
  }

  private CheckBox getAutoReconnectCheckBox() {
    if (mAutoReconnectCheckBox == null) {
      mAutoReconnectCheckBox = new CheckBox("Auto-reconnect");
      mAutoReconnectCheckBox.setTooltip(new Tooltip("Automatically reconnect if the connection to the rtl_tcp " +
          "server is lost"));
      mAutoReconnectCheckBox.setOnAction(e -> {
        if (!isLoading()) {
          if (hasTuner()) {
            getTuner().getTunerController().setAutoReconnect(mAutoReconnectCheckBox.isSelected());
          }
          save();
        }
      });
    }

    return mAutoReconnectCheckBox;
  }

  private static SampleRate closestSampleRate(long sampleRate) {
    SampleRate closest = SampleRate.values()[0];

    for (SampleRate candidate : SampleRate.values()) {
      if (Math.abs(candidate.getRate() - sampleRate) < Math.abs(closest.getRate() - sampleRate)) {
        closest = candidate;
      }
    }

    return closest;
  }

  private static int closestGain(int gain) {
    int closest = R820T_GAINS_TENTHS_DB.get(0);

    for (int candidate : R820T_GAINS_TENTHS_DB) {
      if (Math.abs(candidate - gain) < Math.abs(closest - gain)) {
        closest = candidate;
      }
    }

    return closest;
  }

  @Override
  public void save() {
    if (hasConfiguration() && !isLoading()) {
      NetworkTunerConfiguration config = getConfiguration();
      saveCommonSettings(config);
      SampleRate sampleRate = getSampleRateCombo().getValue();

      if (sampleRate != null) {
        config.setSampleRate(sampleRate.getRate());
      }

      config.setGainAuto(getGainAutoCheckBox().isSelected());
      Integer gain = getGainCombo().getValue();

      if (gain != null) {
        config.setGain(gain);
      }

      config.setBiasTEnabled(getBiasTCheckBox().isSelected());
      config.setAutoReconnect(getAutoReconnectCheckBox().isSelected());
      saveConfiguration();
    }
  }
}

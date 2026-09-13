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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import net.miginfocom.swing.MigLayout;

public class NetworkTunerEditor extends TunerEditor<NetworkTuner, NetworkTunerConfiguration> {
  private static final long serialVersionUID = 1L;
  private static final Logger mLog = LoggerFactory.getLogger(NetworkTunerEditor.class);

  //R820T total gain table (tenths of dB), from librtlsdr's rtlsdr_get_tuner_gains() - most rtl_tcp
  //servers are backed by an R820T/R820T2 dongle and report this same gain table.
  private static final Integer[] R820T_GAINS_TENTHS_DB = {0, 9, 14, 27, 37, 77, 87, 125, 144, 157, 166, 197, 207,
      229, 254, 280, 297, 328, 338, 364, 372, 386, 402, 421, 434, 439, 445, 480, 496};

  private JButton mTunerInfoButton;
  private JToggleButton mBiasTButton;
  private JComboBox<SampleRate> mSampleRateCombo;
  private JCheckBox mGainAutoCheckBox;
  private JComboBox<Integer> mGainCombo;
  private JCheckBox mAutoReconnectCheckBox;

  public NetworkTunerEditor(UserPreferences userPreferences, TunerManager tunerManager,
      DiscoveredTuner discoveredTuner) {
    super(userPreferences, tunerManager, discoveredTuner);
    init();
    tunerStatusUpdated();
  }

  @Override
  public void setTunerLockState(boolean locked) {
    getFrequencyPanel().updateControls();
    //Matches the USB RTL2832/R8x tuner pattern: sample rate can't change while channels are active. Gain and
    //Bias-T don't affect channelizing, so they remain adjustable regardless of lock state.
    getSampleRateCombo().setEnabled(hasTuner() && !locked);
  }

  @Override
  public long getMinimumTunableFrequency() {
    return 52000000;
  }

  @Override
  public long getMaximumTunableFrequency() {
    return 2200000000l;
  }

  @Override
  protected void tunerStatusUpdated() {
    setLoading(true);

    if (hasTuner()) {
      getTunerIdLabel().setText(getTuner().getPreferredName());
    } else {
      getTunerIdLabel().setText(getDiscoveredTuner().getId());
    }

    String status = getDiscoveredTuner().getTunerStatus().toString();
    if (getDiscoveredTuner().hasErrorMessage()) {
      status += " - " + getDiscoveredTuner().getErrorMessage();
    }
    getTunerStatusLabel().setText(status);
    getButtonPanel().updateControls();
    getFrequencyPanel().updateControls();

    getTunerInfoButton().setEnabled(hasTuner());
    getBiasTButton().setEnabled(hasTuner());
    getSampleRateCombo().setEnabled(hasTuner());
    getGainAutoCheckBox().setEnabled(hasTuner());
    getAutoReconnectCheckBox().setEnabled(hasTuner());

    if (hasConfiguration()) {
      getBiasTButton().setSelected(getConfiguration().isBiasTEnabled());
      getSampleRateCombo().setSelectedItem(SampleRate.getClosest((int) getConfiguration().getSampleRate()));
      getGainAutoCheckBox().setSelected(getConfiguration().isGainAuto());
      getGainCombo().setSelectedItem(closestGain(getConfiguration().getGain()));
      getGainCombo().setEnabled(hasTuner() && !getConfiguration().isGainAuto());
      getAutoReconnectCheckBox().setSelected(getConfiguration().isAutoReconnect());
    }

    setLoading(false);
  }

  private void init() {
    setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]", "[][][][][][][][][grow]"));

    add(new JLabel("Tuner:"));
    add(getTunerIdLabel());
    add(getTunerInfoButton());

    add(new JLabel("Status:"));
    add(getTunerStatusLabel());
    add(getBiasTButton(), "wrap");

    add(getButtonPanel(), "span,align left");
    add(new JSeparator(), "span,growx,push");

    add(new JLabel("Frequency (MHz):"));
    add(getFrequencyPanel(), "wrap");

    add(new JLabel("Sample Rate:"));
    add(getSampleRateCombo(), "wrap");

    add(new JLabel("Gain:"));
    add(getGainAutoCheckBox(), "split 2");
    add(getGainCombo(), "wrap");

    add(new JLabel("Auto-Reconnect:"));
    add(getAutoReconnectCheckBox(), "wrap");
  }

  /**
   * Bias-T toggle button, matching the USB RTL2832/R8x tuner editor's top-row placement.
   */
  private JToggleButton getBiasTButton() {
    if (mBiasTButton == null) {
      mBiasTButton = new JToggleButton("Bias-T");
      mBiasTButton.setEnabled(false);
      mBiasTButton.addActionListener(e -> {
        if (!isLoading()) {
          boolean enabled = mBiasTButton.isSelected();

          if (hasTuner()) {
            getTuner().getTunerController().setBiasTEnabled(enabled);
          }

          save();
        }
      });
    }

    return mBiasTButton;
  }

  /**
   * Hyperlink-style button that shows the network tuner's connection details.
   */
  private JButton getTunerInfoButton() {
    if (mTunerInfoButton == null) {
      mTunerInfoButton = new JButton("Info");
      mTunerInfoButton.setEnabled(false);
      mTunerInfoButton.addActionListener(e -> JOptionPane.showMessageDialog(NetworkTunerEditor.this,
          getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
    }

    return mTunerInfoButton;
  }

  private String getTunerInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("<html><h3>rtl_tcp Network Tuner</h3>");
    sb.append("<b>Host: </b>").append(getConfiguration().getHost()).append("<br>");
    sb.append("<b>Port: </b>").append(getConfiguration().getPort()).append("<br>");
    sb.append("<b>Tuner Type: </b>rtl_tcp<br>");
    return sb.toString();
  }

  private JComboBox<SampleRate> getSampleRateCombo() {
    if (mSampleRateCombo == null) {
      mSampleRateCombo = new JComboBox<>(SampleRate.values());
      mSampleRateCombo.setEnabled(false);
      mSampleRateCombo.addActionListener(e -> {
        if (!isLoading()) {
          SampleRate sampleRate = (SampleRate) mSampleRateCombo.getSelectedItem();

          if (sampleRate != null) {
            if (hasTuner()) {
              getTuner().getTunerController().setSampleRate(sampleRate.getRate());
            }

            save();
          }
        }
      });
    }

    return mSampleRateCombo;
  }

  private JCheckBox getGainAutoCheckBox() {
    if (mGainAutoCheckBox == null) {
      mGainAutoCheckBox = new JCheckBox("Auto");
      mGainAutoCheckBox.addActionListener(e -> {
        boolean gainAuto = mGainAutoCheckBox.isSelected();
        getGainCombo().setEnabled(hasTuner() && !gainAuto);

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

  private JComboBox<Integer> getGainCombo() {
    if (mGainCombo == null) {
      mGainCombo = new JComboBox<>(R820T_GAINS_TENTHS_DB);
      mGainCombo.setEnabled(false);
      mGainCombo.setToolTipText("Gain in tenths of dB.  Uncheck Auto to enable manual gain.");
      mGainCombo.addActionListener(e -> {
        if (!isLoading()) {
          Integer gain = (Integer) mGainCombo.getSelectedItem();

          if (gain != null) {
            if (hasTuner()) {
              getTuner().getTunerController().setGain(gain);
            }

            save();
          }
        }
      });
    }

    return mGainCombo;
  }

  private JCheckBox getAutoReconnectCheckBox() {
    if (mAutoReconnectCheckBox == null) {
      mAutoReconnectCheckBox = new JCheckBox();
      mAutoReconnectCheckBox.setToolTipText("Automatically reconnect if the connection to the rtl_tcp server is lost");
      mAutoReconnectCheckBox.addActionListener(e -> {
        if (!isLoading()) {
          boolean autoReconnect = mAutoReconnectCheckBox.isSelected();

          if (hasTuner()) {
            getTuner().getTunerController().setAutoReconnect(autoReconnect);
          }

          save();
        }
      });
    }

    return mAutoReconnectCheckBox;
  }

  /**
   * Finds the R820T gain table entry closest to the specified gain value (tenths of dB).
   */
  private static int closestGain(int gain) {
    int closest = R820T_GAINS_TENTHS_DB[0];

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
      config.setFrequency(getFrequencyControl().getFrequency());
      getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
      getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());

      SampleRate sampleRate = (SampleRate) getSampleRateCombo().getSelectedItem();
      if (sampleRate != null) {
        config.setSampleRate(sampleRate.getRate());
      }

      config.setGainAuto(getGainAutoCheckBox().isSelected());

      Integer gain = (Integer) getGainCombo().getSelectedItem();
      if (gain != null) {
        config.setGain(gain);
      }

      config.setBiasTEnabled(getBiasTButton().isSelected());
      config.setAutoReconnect(getAutoReconnectCheckBox().isSelected());
      saveConfiguration();
    }
  }
}

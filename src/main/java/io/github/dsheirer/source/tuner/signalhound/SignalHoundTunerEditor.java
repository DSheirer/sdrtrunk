/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.signalhound;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.signalhound.Diagnostics;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SpinnerNumberModel;

/**
 * Signal Hound tuner editor
 */
public class SignalHoundTunerEditor extends TunerEditor<SignalHoundTuner, SignalHoundTunerConfiguration>
{
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(SignalHoundTunerEditor.class);
    private final SignalHoundFamily mFamily;
    private JButton mTunerInfoButton;
    private JComboBox<SignalHoundSampleRate> mSampleRateCombo;
    private JSlider mReferenceLevelSlider;
    private JLabel mReferenceLevelValueLabel;
    private JComboBox<Setting> mAttenuationCombo;
    private JComboBox<Setting> mGainCombo;
    private JCheckBox mPreselectorCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable Signal Hound tuner and controller
     */
    public SignalHoundTunerEditor(UserPreferences userPreferences, TunerManager tunerManager,
                                  DiscoveredSignalHoundTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        mFamily = discoveredTuner.getFamily();
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return hasTuner() ? getTuner().getController().getMinimumTunableFrequency() :
                SignalHoundTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return hasTuner() ? getTuner().getController().getMaximumTunableFrequency() :
                SignalHoundTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]", "[][][][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel());
        add(getTunerInfoButton());

        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");

        add(getButtonPanel(), "span,align left");

        add(new JSeparator(), "span,growx");

        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");

        add(new JLabel("Sample Rate:"));
        add(getSampleRateCombo(), "wrap");

        add(new JSeparator(), "span,growx");

        add(new JLabel("Reference Level:"));
        add(getReferenceLevelSlider());
        add(getReferenceLevelValueLabel());

        add(new JLabel("Attenuation:"));
        add(getAttenuationCombo(), "wrap");

        if(mFamily.hasGain())
        {
            add(new JLabel("Gain:"));
            add(getGainCombo(), "wrap");
        }

        if(mFamily.hasPreselector())
        {
            add(new JLabel(""));
            add(getPreselectorCheckBox(), "wrap");
        }

        //Empty row that takes all extra vertical space so that the controls stay stacked at the top of the editor.
        add(new JLabel(), "span,pushy");
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        boolean started = hasStartedTuner();
        getTunerIdLabel().setText(started ? getTuner().getPreferredName() : getDiscoveredTuner().getId());

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();
        getTunerInfoButton().setEnabled(started);

        if(started)
        {
            SignalHoundTunerController controller = getTuner().getController();
            List<SignalHoundSampleRate> rates = controller.getSampleRates();
            getSampleRateCombo().setModel(new DefaultComboBoxModel<>(rates.toArray(new SignalHoundSampleRate[0])));
            getSampleRateCombo().setSelectedItem(controller.getSignalHoundSampleRate());
            getSampleRateCombo().setEnabled(!controller.isLockedSampleRate());

            getReferenceLevelSlider().setValue(controller.getReferenceLevel());
            getReferenceLevelSlider().setEnabled(true);
            getReferenceLevelValueLabel().setEnabled(true);

            getAttenuationCombo().setSelectedItem(new Setting(controller.getAttenuation(), null));
            getAttenuationCombo().setEnabled(true);

            if(mFamily.hasGain())
            {
                getGainCombo().setSelectedItem(new Setting(controller.getGain(), null));
                getGainCombo().setEnabled(true);
            }

            if(mFamily.hasPreselector())
            {
                getPreselectorCheckBox().setSelected(controller.isPreselector());
                getPreselectorCheckBox().setEnabled(controller.isPreselectorSupported());
            }
        }
        else
        {
            getSampleRateCombo().setModel(new DefaultComboBoxModel<>());
            getSampleRateCombo().setEnabled(false);
            getReferenceLevelSlider().setEnabled(false);
            getReferenceLevelValueLabel().setEnabled(false);
            getAttenuationCombo().setEnabled(false);

            if(mFamily.hasGain())
            {
                getGainCombo().setEnabled(false);
            }

            if(mFamily.hasPreselector())
            {
                getPreselectorCheckBox().setEnabled(false);
            }
        }

        updateReferenceLevelLabel();
        setLoading(false);
    }

    /**
     * Indicates if the tuner has finished starting, since a status update can arrive while it's starting or restarting.
     */
    private boolean hasStartedTuner()
    {
        return hasTuner() && getTuner().getController().getSignalHoundSampleRate() != null;
    }

    private void updateReferenceLevelLabel()
    {
        getReferenceLevelValueLabel().setText(getReferenceLevelSlider().getValue() + " dBm");
    }

    /**
     * Button that provides tuner information
     */
    private JButton getTunerInfoButton()
    {
        if(mTunerInfoButton == null)
        {
            mTunerInfoButton = new JButton("Info");
            mTunerInfoButton.setEnabled(false);
            mTunerInfoButton.addActionListener(_ -> JOptionPane.showMessageDialog(SignalHoundTunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    private JComboBox<SignalHoundSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new JComboBox<>();
            mSampleRateCombo.setEnabled(false);
            mSampleRateCombo.addActionListener(_ ->
            {
                if(hasTuner() && !isLoading())
                {
                    SignalHoundSampleRate rate = (SignalHoundSampleRate)mSampleRateCombo.getSelectedItem();

                    try
                    {
                        getTuner().getController().setSampleRate(rate);

                        //Adjust the min/max values for the sample rate.
                        adjustForSampleRate(rate.rate());

                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting Signal Hound sample rate", e1);
                        JOptionPane.showMessageDialog(SignalHoundTunerEditor.this, "Couldn't set sample rate to " +
                                rate + " - " + e1.getMessage());
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    private JSlider getReferenceLevelSlider()
    {
        if(mReferenceLevelSlider == null)
        {
            mReferenceLevelSlider = new JSlider(JSlider.HORIZONTAL, SignalHoundTunerController.REFERENCE_LEVEL_MINIMUM,
                    SignalHoundTunerController.REFERENCE_LEVEL_MAXIMUM,
                    SignalHoundTunerController.REFERENCE_LEVEL_DEFAULT);
            mReferenceLevelSlider.setEnabled(false);
            mReferenceLevelSlider.setMajorTickSpacing(10);
            mReferenceLevelSlider.setPaintTicks(true);
            mReferenceLevelSlider.setToolTipText("Strongest expected input level.  Lower values increase the gain.  " +
                    "Raise it when the log reports ADC overflow warnings.");
            mReferenceLevelSlider.addChangeListener(_ ->
            {
                updateReferenceLevelLabel();

                //Reconfigure the stream once the user releases the slider, since each change restarts the stream
                if(hasTuner() && !isLoading() && !mReferenceLevelSlider.getValueIsAdjusting())
                {
                    int referenceLevel = mReferenceLevelSlider.getValue();

                    try
                    {
                        getTuner().getController().setReferenceLevel(referenceLevel);
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set Signal Hound reference level to:" + referenceLevel, e);
                        JOptionPane.showMessageDialog(mReferenceLevelSlider, "Couldn't set reference level to " +
                                referenceLevel + " dBm");
                    }
                }
            });
        }

        return mReferenceLevelSlider;
    }

    private JLabel getReferenceLevelValueLabel()
    {
        if(mReferenceLevelValueLabel == null)
        {
            mReferenceLevelValueLabel = new JLabel("0 dBm");
            mReferenceLevelValueLabel.setEnabled(false);
        }

        return mReferenceLevelValueLabel;
    }

    private JComboBox<Setting> getAttenuationCombo()
    {
        if(mAttenuationCombo == null)
        {
            List<Setting> settings = new ArrayList<>();
            settings.add(new Setting(SignalHoundTunerController.AUTOMATIC, "Automatic (Reference Level)"));

            for(int x = 0; x <= mFamily.getMaximumAttenuation(); x++)
            {
                settings.add(new Setting(x, (x * mFamily.getAttenuationStepDb()) + " dB"));
            }

            mAttenuationCombo = new JComboBox<>(settings.toArray(new Setting[0]));
            mAttenuationCombo.setEnabled(false);
            mAttenuationCombo.setToolTipText(mFamily.hasGain() ? "The BB60 uses manual attenuation only when the " +
                    "gain is also manual" : "Input attenuation, or automatic to set it from the reference level");
            mAttenuationCombo.addActionListener(_ ->
            {
                if(hasTuner() && !isLoading() && mAttenuationCombo.getSelectedItem() instanceof Setting setting)
                {
                    try
                    {
                        getTuner().getController().setAttenuation(setting.value());
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set Signal Hound attenuation to:" + setting, e);
                        JOptionPane.showMessageDialog(mAttenuationCombo, "Couldn't set attenuation to " + setting);
                    }
                }
            });
        }

        return mAttenuationCombo;
    }

    private JComboBox<Setting> getGainCombo()
    {
        if(mGainCombo == null)
        {
            List<Setting> settings = new ArrayList<>();
            settings.add(new Setting(SignalHoundTunerController.AUTOMATIC, "Automatic (Reference Level)"));

            for(int x = 0; x <= mFamily.getMaximumGain(); x++)
            {
                settings.add(new Setting(x, Integer.toString(x)));
            }

            mGainCombo = new JComboBox<>(settings.toArray(new Setting[0]));
            mGainCombo.setEnabled(false);
            mGainCombo.setToolTipText("The BB60 uses manual gain only when the attenuation is also manual");
            mGainCombo.addActionListener(_ ->
            {
                if(hasTuner() && !isLoading() && mGainCombo.getSelectedItem() instanceof Setting setting)
                {
                    try
                    {
                        getTuner().getController().setGain(setting.value());
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set Signal Hound gain to:" + setting, e);
                        JOptionPane.showMessageDialog(mGainCombo, "Couldn't set gain to " + setting);
                    }
                }
            });
        }

        return mGainCombo;
    }

    private JCheckBox getPreselectorCheckBox()
    {
        if(mPreselectorCheckBox == null)
        {
            mPreselectorCheckBox = new JCheckBox("RF Preselector");
            mPreselectorCheckBox.setEnabled(false);
            mPreselectorCheckBox.setToolTipText(mFamily == SignalHoundFamily.SM ?
                    "Filters the input to reject strong out-of-band signals, up to 645 MHz" :
                    "SP145B only: filters the input to reject strong out-of-band signals, up to 520 MHz");
            mPreselectorCheckBox.addActionListener(_ ->
            {
                if(hasTuner() && !isLoading())
                {
                    boolean enabled = mPreselectorCheckBox.isSelected();

                    try
                    {
                        getTuner().getController().setPreselector(enabled);
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't " + (enabled ? "enable" : "disable") + " Signal Hound preselector", e);
                        JOptionPane.showMessageDialog(mPreselectorCheckBox, "Couldn't change preselector - " +
                                e.getMessage());
                    }
                }
            });
        }

        return mPreselectorCheckBox;
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            getConfiguration().setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = ((SpinnerNumberModel)getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            getConfiguration().setFrequencyCorrection(value);
            getConfiguration().setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());

            if(getSampleRateCombo().getSelectedItem() instanceof SignalHoundSampleRate rate)
            {
                getConfiguration().setSampleRate(rate.rate());
            }

            getConfiguration().setReferenceLevel(getReferenceLevelSlider().getValue());

            if(getAttenuationCombo().getSelectedItem() instanceof Setting setting)
            {
                getConfiguration().setAttenuation(setting.value());
            }

            if(mFamily.hasGain() && getGainCombo().getSelectedItem() instanceof Setting setting)
            {
                getConfiguration().setGain(setting.value());
            }

            if(mFamily.hasPreselector())
            {
                getConfiguration().setPreselector(getPreselectorCheckBox().isSelected());
            }

            saveConfiguration();
        }
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
        getSampleRateCombo().setEnabled(!locked);
        getSampleRateCombo().setToolTipText(locked ? "Sample Rate is locked.  Disable decoding channels to unlock." :
                "Select a sample rate for the tuner");
    }

    private String getTunerInfo()
    {
        if(getDiscoveredTuner().getTunerStatus() == TunerStatus.ERROR)
        {
            return getDiscoveredTuner().getErrorMessage();
        }

        if(hasTuner())
        {
            SignalHoundDeviceInformation info = getTuner().getController().getDeviceInfo();
            StringBuilder sb = new StringBuilder();
            sb.append("<html><h3>Signal Hound Tuner</h3>");
            sb.append("<b>Model: </b>").append(info.model()).append("<br>");
            sb.append("<b>Serial: </b>").append(info.serial()).append("<br>");
            sb.append("<b>Firmware: </b>").append(info.firmware()).append("<br>");
            sb.append("<b>Library: </b>").append(info.library()).append(" ").append(info.libraryVersion()).append("<br>");
            sb.append("<b>Location: </b>").append(info.libraryLocation()).append("<br>");

            Diagnostics diagnostics = getTuner().getController().getDiagnostics();

            if(diagnostics != null)
            {
                sb.append("<b>Temperature: </b>").append(String.format(Locale.ROOT, "%.1f C",
                        diagnostics.temperature())).append("<br>");
                sb.append("<b>Voltage: </b>").append(String.format(Locale.ROOT, "%.2f V",
                        diagnostics.voltage())).append("<br>");
                sb.append("<b>Current: </b>").append(String.format(Locale.ROOT, "%.3f A",
                        diagnostics.current())).append("<br>");
            }

            return sb.toString();
        }

        return null;
    }

    /**
     * Attenuation or gain combo box entry.  Entries are equal when their values are equal, so that the entry for a
     * value can be selected with a new instance.
     * @param value of the setting, or -1 for automatic
     * @param label to display
     */
    record Setting(int value, String label)
    {
        @Override
        public boolean equals(Object o)
        {
            return o instanceof Setting other && other.value == value;
        }

        @Override
        public int hashCode()
        {
            return Integer.hashCode(value);
        }

        @Override
        public String toString()
        {
            return label;
        }
    }
}

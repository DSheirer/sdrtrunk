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
package io.github.dsheirer.source.tuner.rtl;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xTunerConfiguration;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/**
 * RTL-2832 tuner editor for all supported embedded tuner types (E4000, FC0012, FC0013, FC2580, R820T and R828D).
 *
 * Gain options are the gains that librtlsdr reports for the embedded tuner, plus the tuner's automatic gain control.
 * The E4000 additionally has a total IF gain that is distributed across its IF gain stages.
 */
public class RTL2832TunerEditor extends TunerEditor<RTL2832Tuner, RTL2832TunerConfiguration>
{
    private ComboBox<SampleRate> mSampleRateCombo;
    private CheckBox mBiasTCheckBox;
    private CheckBox mAgcCheckBox;
    private ComboBox<Gain> mGainCombo;
    private Label mIFGainLabel;
    private ComboBox<Integer> mIFGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving configurations
     * @param discoveredTuner to edit
     */
    public RTL2832TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        refresh();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        if(hasTuner())
        {
            return getTuner().getController().getMinimumFrequency();
        }

        return R8xTunerConfiguration.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        if(hasTuner())
        {
            return getTuner().getController().getMaximumFrequency();
        }

        return R8xTunerConfiguration.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    /**
     * Indicates if the tuner has an E4000 embedded tuner, which supports IF gain control.
     */
    private boolean isE4K()
    {
        return hasTuner() && getTuner().getController().getTunerType() == TunerType.ELONICS_E4000;
    }

    private void init()
    {
        getTuningSection().addRow("Sample Rate", getSampleRateCombo());

        TunerSection gain = addSection("Gain");
        gain.addRow("Gain", getGainCombo(), getAgcCheckBox());
        mIFGainLabel = gain.addRow("IF Gain (dB)", getIFGainCombo());

        TunerSection options = addSection("Options");
        options.addControl(getBiasTCheckBox());
    }

    @Override
    protected String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();
        RTL2832TunerController.Descriptor descriptor = getTuner().getController().getDescriptor();
        sb.append("<html><h3>RTL-2832 with ").append(getTuner().getController().getTunerType().getLabel())
                .append(" Tuner</h3>");

        if(descriptor == null)
        {
            sb.append("No EEPROM Descriptor Available");
        }
        else
        {
            sb.append("<b>USB ID: </b>").append(descriptor.getVendorID()).append(":")
                    .append(descriptor.getProductID()).append("<br>");
            sb.append("<b>Vendor: </b>").append(descriptor.getVendorLabel()).append("<br>");
            sb.append("<b>Product: </b>").append(descriptor.getProductLabel()).append("<br>");
            sb.append("<b>Serial: </b>").append(descriptor.getSerial()).append("<br>");
            sb.append("<b>IR Enabled: </b>").append(descriptor.irEnabled()).append("<br>");
            sb.append("<b>Remote Wake: </b>").append(descriptor.remoteWakeupEnabled()).append("<br>");
        }

        return sb.toString();
    }

    @Override
    protected void tunerStatusUpdated()
    {
        getGainCombo().getItems().clear();

        if(hasTuner())
        {
            RTL2832TunerController controller = getTuner().getController();

            if(controller.hasGainControl())
            {
                for(int gain : controller.getTunerGains())
                {
                    getGainCombo().getItems().add(new Gain(gain));
                }
            }
        }

        boolean e4k = isE4K();
        mIFGainLabel.setVisible(e4k);
        getIFGainCombo().setVisible(e4k);

        if(hasTuner() && hasConfiguration())
        {
            RTL2832TunerConfiguration config = getConfiguration();
            RTL2832TunerController controller = getTuner().getController();
            getBiasTCheckBox().setDisable(false);
            getBiasTCheckBox().setSelected(config.isBiasT());
            getSampleRateCombo().setValue(config.getSampleRate());
            //The FC2580 always uses its internal AGC and the FC0012 doesn't support AGC
            boolean agc = controller.hasGainControl() ? controller.hasTunerAGC() && config.isAGC() : true;
            getAgcCheckBox().setDisable(!controller.hasTunerAGC());
            getAgcCheckBox().setSelected(agc);
            getAgcCheckBox().setTooltip(new Tooltip(getAgcToolTip(controller)));
            getGainCombo().setDisable(agc || getGainCombo().getItems().isEmpty());
            getGainCombo().setValue(new Gain(controller.getClosestTunerGain(config.getGain())));

            if(e4k && config instanceof E4KTunerConfiguration e4kConfig)
            {
                getIFGainCombo().setDisable(false);
                getIFGainCombo().setValue(e4kConfig.getIFGainDb());
            }
        }
        else
        {
            getBiasTCheckBox().setDisable(true);
            getBiasTCheckBox().setSelected(false);
            getAgcCheckBox().setDisable(true);
            getGainCombo().setDisable(true);
            getIFGainCombo().setDisable(true);
        }

        updateSampleRateControl();
    }

    @Override
    protected void tunerLockStateUpdated(boolean locked)
    {
        updateSampleRateControl();
    }

    /**
     * Updates the sample rate combo's enabled state and tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateControl()
    {
        boolean locked = hasTuner() && getTuner().getTunerController().isLockedSampleRate();
        getSampleRateCombo().setDisable(!hasTuner() || !hasConfiguration() || locked);

        if(locked)
        {
            getSampleRateCombo().setTooltip(new Tooltip("Sample rate is locked.  Disable decoding channels to unlock."));
        }
        else if(hasTuner())
        {
            getSampleRateCombo().setTooltip(new Tooltip("Select a sample rate for the tuner"));
        }
        else
        {
            getSampleRateCombo().setTooltip(new Tooltip("No tuner available"));
        }
    }

    private CheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new CheckBox("Bias-T");
            mBiasTCheckBox.setTooltip(new Tooltip("Provide DC power on the antenna connector (e.g. for an LNA)"));
            mBiasTCheckBox.setDisable(true);
            mBiasTCheckBox.setOnAction(e -> {
                if(!isLoading() && hasTuner())
                {
                    getTuner().getController().setBiasT(mBiasTCheckBox.isSelected());
                    save();
                }
            });
        }

        return mBiasTCheckBox;
    }

    /**
     * Applies the current AGC and gain settings to the tuner and saves the configuration.
     */
    private void applyGain()
    {
        if(isLoading() || !hasTuner())
        {
            return;
        }

        boolean agc = getAgcCheckBox().isSelected();
        Gain gain = getGainCombo().getValue();
        getGainCombo().setDisable(agc || getGainCombo().getItems().isEmpty());

        if(applyToTuner("gain", () -> getTuner().getController().setGain(agc,
                gain != null ? gain.value() : getConfiguration().getGain())))
        {
            save();
        }
    }

    /**
     * Tool tip for the AGC control that explains when the tuner doesn't support switching AGC.
     */
    private static String getAgcToolTip(RTL2832TunerController controller)
    {
        if(!controller.hasGainControl())
        {
            return "The " + controller.getTunerType().getLabel() + " tuner always uses its automatic gain control (AGC)";
        }
        else if(!controller.hasTunerAGC())
        {
            return "The " + controller.getTunerType().getLabel() + " tuner doesn't support automatic gain control (AGC)";
        }

        return "Turn on the tuner's automatic gain control (AGC)";
    }

    private CheckBox getAgcCheckBox()
    {
        if(mAgcCheckBox == null)
        {
            mAgcCheckBox = new CheckBox("AGC");
            mAgcCheckBox.setDisable(true);
            mAgcCheckBox.setTooltip(new Tooltip("Turn on the tuner's automatic gain control (AGC)"));
            mAgcCheckBox.setOnAction(e -> applyGain());
        }

        return mAgcCheckBox;
    }

    private ComboBox<Gain> getGainCombo()
    {
        if(mGainCombo == null)
        {
            mGainCombo = new ComboBox<>();
            mGainCombo.setMaxWidth(Double.MAX_VALUE);
            mGainCombo.setDisable(true);
            mGainCombo.setTooltip(new Tooltip("Tuner gain.  Turn off AGC to enable adjustment."));
            mGainCombo.setOnAction(e -> applyGain());
        }

        return mGainCombo;
    }

    private ComboBox<Integer> getIFGainCombo()
    {
        if(mIFGainCombo == null)
        {
            mIFGainCombo = new ComboBox<>();
            mIFGainCombo.setMaxWidth(Double.MAX_VALUE);

            for(int gain = RTL2832TunerController.E4K_IF_GAIN_MINIMUM; gain <= RTL2832TunerController.E4K_IF_GAIN_MAXIMUM; gain++)
            {
                mIFGainCombo.getItems().add(gain);
            }

            mIFGainCombo.setDisable(true);
            mIFGainCombo.setTooltip(new Tooltip("E4000 IF gain, distributed across the six IF gain stages"));
            mIFGainCombo.setOnAction(e -> {
                Integer gain = mIFGainCombo.getValue();

                if(!isLoading() && hasTuner() && gain != null &&
                    applyToTuner("IF gain", () -> getTuner().getController().setE4KIFGain(gain)))
                {
                    save();
                }
            });
        }

        return mIFGainCombo;
    }

    private ComboBox<SampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>(FXCollections.observableArrayList(SampleRate.values()));
            mSampleRateCombo.setMaxWidth(Double.MAX_VALUE);
            mSampleRateCombo.setDisable(true);
            mSampleRateCombo.setOnAction(e -> {
                SampleRate sampleRate = mSampleRateCombo.getValue();

                if(!isLoading() && hasTuner() && sampleRate != null &&
                    applyToTuner("sample rate [" + sampleRate.getLabel() + "]",
                        () -> getTuner().getController().setSampleRate(sampleRate)))
                {
                    //Adjust the min/max values for the sample rate.
                    adjustForSampleRate(sampleRate.getRate());
                    save();
                }
            });
        }

        return mSampleRateCombo;
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            RTL2832TunerConfiguration config = getConfiguration();
            saveCommonSettings(config);
            config.setBiasT(getTuner().getController().isBiasT());
            config.setSampleRate(getSampleRateCombo().getValue());
            config.setAGC(getAgcCheckBox().isSelected());

            if(getGainCombo().getValue() instanceof Gain gain)
            {
                config.setGain(gain.value());
            }

            if(config instanceof E4KTunerConfiguration e4k && getIFGainCombo().getValue() instanceof Integer ifGain)
            {
                e4k.setIFGainDb(ifGain);
            }

            saveConfiguration();
        }
    }

    /**
     * Tuner gain combo box entry.
     * @param value in tenths of a dB
     */
    private record Gain(int value)
    {
        @Override
        public String toString()
        {
            return String.format("%.1f dB", value / 10.0);
        }
    }
}

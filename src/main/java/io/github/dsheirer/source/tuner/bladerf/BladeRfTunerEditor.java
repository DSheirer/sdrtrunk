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
package io.github.dsheirer.source.tuner.bladerf;

import io.github.dsheirer.bladerf.GainMode;
import io.github.dsheirer.bladerf.GainModeInfo;
import io.github.dsheirer.bladerf.Range;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.GainSlider;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import java.util.List;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

/**
 * bladeRF tuner editor.  sdrtrunk uses the RX1 connector as a single tuner.
 */
public class BladeRfTunerEditor extends TunerEditor<BladeRfTuner, BladeRfTunerConfiguration>
{
    private ComboBox<BladeRfSampleRate> mSampleRateCombo;
    private CheckBox mBiasTCheckBox;
    private ComboBox<GainModeInfo> mGainModeCombo;
    private GainSlider mGainSlider;
    private Label mGainLabel;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable bladeRF tuner and controller
     */
    public BladeRfTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        refresh();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return hasTuner() ? getTuner().getController().getMinimumTunableFrequency() :
                BladeRfTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return hasTuner() ? getTuner().getController().getMaximumTunableFrequency() :
                BladeRfTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private void init()
    {
        getTuningSection().addRow("Sample Rate", getSampleRateCombo());

        TunerSection gain = addSection("Gain");
        gain.addRow("Mode", getGainModeCombo());
        mGainLabel = gain.addRow("Gain", getGainSlider());

        TunerSection options = addSection("Options");
        options.addControl(getBiasTCheckBox());
    }

    @Override
    protected void tunerStatusUpdated()
    {
        if(hasStartedTuner())
        {
            BladeRfTunerController controller = getTuner().getController();
            List<BladeRfSampleRate> rates = controller.getSampleRates();
            getSampleRateCombo().getItems().setAll(rates);
            getSampleRateCombo().setValue(controller.getBladeRfSampleRate());
            getSampleRateCombo().setDisable(controller.isLockedSampleRate());
            getBiasTCheckBox().setVisible(controller.isBiasTSupported());
            getBiasTCheckBox().setDisable(false);
            List<GainModeInfo> modes = controller.getGainModes();
            getGainModeCombo().getItems().setAll(modes);
            getGainModeCombo().setDisable(false);
            Range range = controller.getGainRange();
            getGainSlider().setRange((int)range.min(), (int)range.max());

            if(hasConfiguration())
            {
                getBiasTCheckBox().setSelected(getConfiguration().isBiasT());
                getGainSlider().setValue(getConfiguration().getGain());

                for(GainModeInfo mode : modes)
                {
                    if(mode.mode() == getConfiguration().getGainMode())
                    {
                        getGainModeCombo().setValue(mode);
                    }
                }
            }

            updateGainControls();
        }
        else
        {
            getSampleRateCombo().getItems().clear();
            getSampleRateCombo().setDisable(true);
            getBiasTCheckBox().setDisable(true);
            getBiasTCheckBox().setSelected(false);
            getGainModeCombo().getItems().clear();
            getGainModeCombo().setDisable(true);
            getGainSlider().setDisable(true);
            mGainLabel.setDisable(true);
        }
    }

    /**
     * Indicates if the tuner is started, which is when the controller has the gain range.
     */
    private boolean hasStartedTuner()
    {
        return hasTuner() && getTuner().getController().getGainRange() != null;
    }

    private void updateGainControls()
    {
        GainModeInfo mode = getGainModeCombo().getValue();
        boolean manual = hasTuner() && mode != null && mode.mode() == GainMode.MANUAL;
        getGainSlider().setDisable(!manual);
        mGainLabel.setDisable(!manual);
        getGainSlider().setFormatter(value -> manual ? value + " dB" : "AGC");
    }

    private ComboBox<BladeRfSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>();
            mSampleRateCombo.setMaxWidth(Double.MAX_VALUE);
            mSampleRateCombo.setDisable(true);
            mSampleRateCombo.setTooltip(new Tooltip("Select a sample rate for the tuner"));
            mSampleRateCombo.setOnAction(e -> {
                BladeRfSampleRate rate = mSampleRateCombo.getValue();

                if(hasTuner() && !isLoading() && rate != null &&
                    applyToTuner("sample rate [" + rate + "]", () -> getTuner().getController().setSampleRate(rate)))
                {
                    adjustForSampleRate(rate.getRate());
                    save();
                }
            });
        }

        return mSampleRateCombo;
    }

    private CheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new CheckBox("Bias-T");
            mBiasTCheckBox.setDisable(true);
            mBiasTCheckBox.setTooltip(new Tooltip("Enable Bias-T power output for active antennas.  Also powers the " +
                    "RX2 connector."));
            mBiasTCheckBox.setOnAction(e -> {
                if(hasTuner() && !isLoading() &&
                    applyToTuner("Bias-T", () -> getTuner().getController().setBiasT(mBiasTCheckBox.isSelected())))
                {
                    save();
                }
            });
        }

        return mBiasTCheckBox;
    }

    private ComboBox<GainModeInfo> getGainModeCombo()
    {
        if(mGainModeCombo == null)
        {
            mGainModeCombo = new ComboBox<>();
            mGainModeCombo.setMaxWidth(Double.MAX_VALUE);
            mGainModeCombo.setDisable(true);
            mGainModeCombo.setConverter(new StringConverter<>()
            {
                @Override
                public String toString(GainModeInfo mode)
                {
                    return mode == null ? "" : mode.name();
                }

                @Override
                public GainModeInfo fromString(String text)
                {
                    return null;
                }
            });
            mGainModeCombo.setOnAction(e -> {
                GainModeInfo mode = mGainModeCombo.getValue();

                if(hasTuner() && !isLoading() && mode != null &&
                    applyToTuner("gain mode [" + mode.name() + "]",
                        () -> getTuner().getController().setGainMode(mode.mode())))
                {
                    updateGainControls();
                    save();
                }
            });
        }

        return mGainModeCombo;
    }

    private GainSlider getGainSlider()
    {
        if(mGainSlider == null)
        {
            mGainSlider = new GainSlider(0, 60, BladeRfTunerController.GAIN_DEFAULT);
            mGainSlider.setDisable(true);
            mGainSlider.setFormatter(value -> value + " dB");
            mGainSlider.setOnChange(gain -> {
                if(hasTuner() && !isLoading() &&
                    applyToTuner("gain", () -> getTuner().getController().setGain(gain)))
                {
                    save();
                }
            });
        }

        return mGainSlider;
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            BladeRfTunerConfiguration config = getConfiguration();
            saveCommonSettings(config);

            if(getSampleRateCombo().getValue() instanceof BladeRfSampleRate rate)
            {
                config.setSampleRate(rate.getRate());
            }

            if(getGainModeCombo().getValue() instanceof GainModeInfo mode)
            {
                config.setGainMode(mode.mode());
            }

            config.setGain(getGainSlider().getValue());
            config.setBiasT(getBiasTCheckBox().isSelected());
            saveConfiguration();
        }
    }

    @Override
    protected void tunerLockStateUpdated(boolean locked)
    {
        getSampleRateCombo().setDisable(!hasStartedTuner() || locked);
        getSampleRateCombo().setTooltip(new Tooltip(locked ? "Sample rate is locked.  Disable decoding channels " +
                "to unlock." : "Select a sample rate for the tuner"));
    }

    @Override
    protected String getTunerInfo()
    {
        if(!hasStartedTuner())
        {
            return null;
        }

        BladeRfDeviceInformation info = getTuner().getController().getDeviceInfo();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><h3>bladeRF Tuner</h3>");
        sb.append("<b>Serial: </b>").append(info.serial()).append("<br>");
        sb.append("<b>Board: </b>").append(info.board()).append("<br>");
        sb.append("<b>Firmware: </b>").append(info.firmware()).append("<br>");
        sb.append("<b>FPGA: </b>").append(info.fpga()).append("<br>");
        sb.append("<b>USB Speed: </b>").append(info.speed()).append("<br>");
        return sb.toString();
    }
}

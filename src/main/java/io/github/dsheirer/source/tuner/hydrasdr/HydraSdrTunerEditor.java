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
package io.github.dsheirer.source.tuner.hydrasdr;

import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.source.tuner.hydrasdr.HydraSdrTunerController.Gain;
import io.github.dsheirer.source.tuner.hydrasdr.HydraSdrTunerController.GainMode;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.GainSlider;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

/**
 * HydraSDR tuner editor
 */
public class HydraSdrTunerEditor extends TunerEditor<HydraSdrTuner, HydraSdrTunerConfiguration>
{
    private ComboBox<HydraSdrSampleRate> mSampleRateCombo;
    private ComboBox<GainMode> mGainModeCombo;
    private GainSlider mMasterGainSlider;
    private Label mMasterGainLabel;
    private GainSlider mIFGainSlider;
    private Label mIFGainLabel;
    private GainSlider mLNAGainSlider;
    private Label mLNAGainLabel;
    private GainSlider mMixerGainSlider;
    private Label mMixerGainLabel;
    private CheckBox mLNAAGCCheckBox;
    private CheckBox mMixerAGCCheckBox;
    private CheckBox mBiasTCheckBox;

    /**
     * Constructs an instance
     * @param settings for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable HydraSDR tuner and controller
     */
    public HydraSdrTunerEditor(Settings settings, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(settings, tunerManager, discoveredTuner);
        init();
        refresh();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return HydraSdrTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return HydraSdrTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private void init()
    {
        getTuningSection().addRow("Sample Rate", getSampleRateCombo());

        TunerSection gain = addSection("Gain");
        gain.addRow("Mode", getGainModeCombo());
        mMasterGainLabel = gain.addRow("Master", getMasterGainSlider(), new Region());
        mIFGainLabel = gain.addRow("IF", getIFGainSlider(), new Region());
        mMixerGainLabel = gain.addRow("Mixer", getMixerGainSlider(), getMixerAGCCheckBox());
        mLNAGainLabel = gain.addRow("LNA", getLNAGainSlider(), getLNAAGCCheckBox());

        TunerSection options = addSection("Options");
        options.addControl(getBiasTCheckBox());
    }

    @Override
    protected void tunerStatusUpdated()
    {
        getSampleRateCombo().setDisable(!hasTuner() || getTuner().getTunerController().isLockedSampleRate());
        getBiasTCheckBox().setDisable(!hasTuner());
        getBiasTCheckBox().setSelected(hasTuner() && hasConfiguration() && getConfiguration().isBiasT());
        updateGainComponents((hasTuner() && hasConfiguration()) ? getConfiguration().getGain() : null);

        if(hasTuner())
        {
            List<HydraSdrSampleRate> rates = getTuner().getController().getSampleRates();
            getSampleRateCombo().getItems().setAll(rates);

            if(hasConfiguration())
            {
                getSampleRateCombo().setValue(getSampleRate(getConfiguration().getSampleRate()));
            }
        }
        else
        {
            getSampleRateCombo().getItems().clear();
        }

        updateSampleRateToolTip();
    }

    /**
     * Checkbox to enable/disable Bias-T power for active antennas
     */
    private CheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new CheckBox("Bias-T");
            mBiasTCheckBox.setDisable(true);
            mBiasTCheckBox.setTooltip(new Tooltip("Enable Bias-T power output for active antennas"));
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

    private CheckBox getLNAAGCCheckBox()
    {
        if(mLNAAGCCheckBox == null)
        {
            mLNAAGCCheckBox = new CheckBox("AGC");
            mLNAAGCCheckBox.setTooltip(new Tooltip("Automatic LNA gain control"));
            mLNAAGCCheckBox.setDisable(true);
            mLNAAGCCheckBox.setOnAction(e -> {
                if(hasTuner() && !isLoading() && applyToTuner("LNA AGC",
                        () -> getTuner().getController().setLNAAGC(getLNAAGCCheckBox().isSelected())))
                {
                    getLNAGainSlider().setDisable(getLNAAGCCheckBox().isSelected());
                    save();
                }
            });
        }

        return mLNAAGCCheckBox;
    }

    private CheckBox getMixerAGCCheckBox()
    {
        if(mMixerAGCCheckBox == null)
        {
            mMixerAGCCheckBox = new CheckBox("AGC");
            mMixerAGCCheckBox.setTooltip(new Tooltip("Automatic mixer gain control"));
            mMixerAGCCheckBox.setDisable(true);
            mMixerAGCCheckBox.setOnAction(e -> {
                if(hasTuner() && !isLoading() && applyToTuner("mixer AGC",
                        () -> getTuner().getController().setMixerAGC(getMixerAGCCheckBox().isSelected())))
                {
                    getMixerGainSlider().setDisable(getMixerAGCCheckBox().isSelected());
                    save();
                }
            });
        }

        return mMixerAGCCheckBox;
    }

    private GainSlider getLNAGainSlider()
    {
        if(mLNAGainSlider == null)
        {
            mLNAGainSlider = new GainSlider(HydraSdrTunerController.LNA_GAIN_MIN, HydraSdrTunerController.LNA_GAIN_MAX,
                    HydraSdrTunerController.LNA_GAIN_MIN);
            mLNAGainSlider.setDisable(true);
            mLNAGainSlider.setOnChange(gain -> {
                if(hasTuner() && !isLoading() &&
                    applyToTuner("LNA gain", () -> getTuner().getController().setLNAGain(gain)))
                {
                    save();
                }
            });
        }

        return mLNAGainSlider;
    }

    private GainSlider getMixerGainSlider()
    {
        if(mMixerGainSlider == null)
        {
            mMixerGainSlider = new GainSlider(HydraSdrTunerController.MIXER_GAIN_MIN,
                    HydraSdrTunerController.MIXER_GAIN_MAX, HydraSdrTunerController.MIXER_GAIN_MIN);
            mMixerGainSlider.setDisable(true);
            mMixerGainSlider.setOnChange(gain -> {
                if(hasTuner() && !isLoading() &&
                    applyToTuner("mixer gain", () -> getTuner().getController().setMixerGain(gain)))
                {
                    save();
                }
            });
        }

        return mMixerGainSlider;
    }

    private GainSlider getIFGainSlider()
    {
        if(mIFGainSlider == null)
        {
            mIFGainSlider = new GainSlider(HydraSdrTunerController.IF_GAIN_MIN, HydraSdrTunerController.IF_GAIN_MAX,
                    HydraSdrTunerController.IF_GAIN_MIN);
            mIFGainSlider.setDisable(true);
            mIFGainSlider.setOnChange(gain -> {
                if(hasTuner() && !isLoading() &&
                    applyToTuner("IF gain", () -> getTuner().getController().setIFGain(gain)))
                {
                    save();
                }
            });
        }

        return mIFGainSlider;
    }

    private GainSlider getMasterGainSlider()
    {
        if(mMasterGainSlider == null)
        {
            mMasterGainSlider = new GainSlider(HydraSdrTunerController.GAIN_MIN, HydraSdrTunerController.GAIN_MAX,
                    HydraSdrTunerController.GAIN_MIN);
            mMasterGainSlider.setDisable(true);
            mMasterGainSlider.setOnChange(value -> {
                Gain gain = Gain.getGain(getGainModeCombo().getValue(), value);

                if(hasTuner() && !isLoading() && applyToTuner("gain", () -> getTuner().getController().setGain(gain)))
                {
                    save();
                }
            });
        }

        return mMasterGainSlider;
    }

    private ComboBox<GainMode> getGainModeCombo()
    {
        if(mGainModeCombo == null)
        {
            mGainModeCombo = new ComboBox<>(FXCollections.observableArrayList(GainMode.values()));
            mGainModeCombo.setMaxWidth(Double.MAX_VALUE);
            mGainModeCombo.setDisable(true);
            mGainModeCombo.setTooltip(new Tooltip("Linearity and sensitivity modes set all gains from the master " +
                    "gain.  Custom mode sets each gain separately."));
            mGainModeCombo.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    Gain gain = Gain.getGain(mGainModeCombo.getValue(), getMasterGainSlider().getValue());
                    setLoading(true);
                    updateGainComponents(gain);
                    setLoading(false);
                    applyToTuner("gain mode", () -> {
                        HydraSdrTunerController controller = getTuner().getController();

                        if(gain == Gain.CUSTOM)
                        {
                            controller.setMixerAGC(getMixerAGCCheckBox().isSelected());
                            controller.setLNAAGC(getLNAAGCCheckBox().isSelected());
                            controller.setIFGain(getIFGainSlider().getValue());
                            controller.setMixerGain(getMixerGainSlider().getValue());
                            controller.setLNAGain(getLNAGainSlider().getValue());
                        }
                        else
                        {
                            controller.setGain(gain);
                        }
                    });
                    save();
                }
            });
        }

        return mGainModeCombo;
    }

    private ComboBox<HydraSdrSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>();
            mSampleRateCombo.setMaxWidth(Double.MAX_VALUE);
            mSampleRateCombo.setDisable(true);
            mSampleRateCombo.setOnAction(e -> {
                HydraSdrSampleRate rate = mSampleRateCombo.getValue();

                if(hasTuner() && !isLoading() && rate != null &&
                    applyToTuner("sample rate [" + rate.getLabel() + "]",
                        () -> getTuner().getController().setSampleRate(rate)))
                {
                    //Adjust the min/max values for the sample rate.
                    adjustForSampleRate(rate.getRate());
                    save();
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * Updates the enabled state of each of the gain controls according to the specified gain mode.  The master gain
     * control is enabled for linearity and sensitivity and the individual gain controls are disabled, and vice-versa
     * for custom mode.
     */
    private void updateGainComponents(Gain gain)
    {
        if(hasTuner() && gain != null)
        {
            boolean isCustom = gain.equals(Gain.CUSTOM);
            getGainModeCombo().setDisable(false);
            getGainModeCombo().setValue(gain.getGainMode());
            mMasterGainLabel.setDisable(isCustom);
            getMasterGainSlider().setDisable(isCustom);
            getMasterGainSlider().setValue(gain.getValue());
            mIFGainLabel.setDisable(!isCustom);
            getIFGainSlider().setDisable(!isCustom);
            mLNAGainLabel.setDisable(!isCustom);
            getLNAAGCCheckBox().setDisable(!isCustom);
            getLNAGainSlider().setDisable(!isCustom || getConfiguration().isLNAAGC());
            mMixerGainLabel.setDisable(!isCustom);
            getMixerAGCCheckBox().setDisable(!isCustom);
            getMixerGainSlider().setDisable(!isCustom || getConfiguration().isMixerAGC());

            if(isCustom)
            {
                getIFGainSlider().setValue(getConfiguration().getIFGain());
                getLNAGainSlider().setValue(getConfiguration().getLNAGain());
                getMixerGainSlider().setValue(getConfiguration().getMixerGain());
                getMixerAGCCheckBox().setSelected(getConfiguration().isMixerAGC());
                getLNAAGCCheckBox().setSelected(getConfiguration().isLNAAGC());
            }
            else
            {
                getIFGainSlider().setValue(0);
                getLNAGainSlider().setValue(0);
                getMixerGainSlider().setValue(0);
                getMixerAGCCheckBox().setSelected(false);
                getLNAAGCCheckBox().setSelected(false);
            }
        }
        else
        {
            getGainModeCombo().setDisable(true);
            mMasterGainLabel.setDisable(true);
            getMasterGainSlider().setDisable(true);
            getMasterGainSlider().setValue(0);
            mIFGainLabel.setDisable(true);
            getIFGainSlider().setDisable(true);
            getIFGainSlider().setValue(0);
            mLNAGainLabel.setDisable(true);
            getLNAAGCCheckBox().setDisable(true);
            getLNAAGCCheckBox().setSelected(false);
            getLNAGainSlider().setDisable(true);
            getLNAGainSlider().setValue(0);
            mMixerGainLabel.setDisable(true);
            getMixerAGCCheckBox().setDisable(true);
            getMixerAGCCheckBox().setSelected(false);
            getMixerGainSlider().setDisable(true);
            getMixerGainSlider().setValue(0);
        }
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            HydraSdrTunerConfiguration config = getConfiguration();
            saveCommonSettings(config);

            if(getSampleRateCombo().getValue() != null)
            {
                config.setSampleRate(getSampleRateCombo().getValue().getRate());
            }

            config.setGain(Gain.getGain(getGainModeCombo().getValue(), getMasterGainSlider().getValue()));
            config.setIFGain(getIFGainSlider().getValue());
            config.setMixerGain(getMixerGainSlider().getValue());
            config.setLNAGain(getLNAGainSlider().getValue());
            config.setMixerAGC(getMixerAGCCheckBox().isSelected());
            config.setLNAAGC(getLNAAGCCheckBox().isSelected());
            config.setBiasT(getBiasTCheckBox().isSelected());
            saveConfiguration();
        }
    }

    /**
     * Finds the HydraSDR sample rate entry that matches the value.
     * @param value in Hertz
     * @return the matching rate entry or null.
     */
    private HydraSdrSampleRate getSampleRate(int value)
    {
        if(hasTuner())
        {
            List<HydraSdrSampleRate> rates = getTuner().getController().getSampleRates();

            for(HydraSdrSampleRate rate : rates)
            {
                if(rate.getRate() == value)
                {
                    return rate;
                }
            }

            if(!rates.isEmpty())
            {
                return rates.get(0);
            }
        }

        return null;
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(hasTuner() && getTuner().getController().isLockedSampleRate())
        {
            getSampleRateCombo().setTooltip(new Tooltip("Sample rate is locked.  Disable decoding channels to unlock."));
        }
        else
        {
            getSampleRateCombo().setTooltip(new Tooltip("Select a sample rate for the tuner"));
        }
    }

    @Override
    protected void tunerLockStateUpdated(boolean locked)
    {
        getSampleRateCombo().setDisable(!hasTuner() || locked);
        updateSampleRateToolTip();
    }

    @Override
    protected String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();
        HydraSdrDeviceInformation info = getTuner().getController().getDeviceInfo();
        sb.append("<html><h3>HydraSDR Tuner</h3>");
        sb.append("<b>Serial: </b>").append(info.getSerialNumber()).append("<br>");
        sb.append("<b>Firmware: </b>").append(info.getVersion()).append("<br>");
        sb.append("<b>Part: </b>").append(info.getPartNumber()).append("<br>");
        sb.append("<b>Board: </b>").append(info.getBoardName()).append("<br>");
        return sb.toString();
    }
}

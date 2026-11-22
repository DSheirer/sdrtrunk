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
package io.github.dsheirer.source.tuner.airspy.hf;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;

/**
 * Airspy HF+ tuner editor
 */
public class AirspyHfTunerEditor extends TunerEditor<AirspyHfTuner,AirspyHfTunerConfiguration>
{
    private ComboBox<AirspyHfSampleRate> mSampleRateCombo;
    private ComboBox<Attenuation> mAttenuationCombo;
    private CheckBox mAgcCheckBox;
    private CheckBox mLnaCheckBox;
    private CheckBox mBiasTCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for settings
     * @param tunerManager for state updates
     * @param discoveredTuner to edit or control.
     */
    public AirspyHfTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        refresh();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return AirspyHfTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return AirspyHfTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private void init()
    {
        getTuningSection().addRow("Sample Rate", getSampleRateCombo());

        TunerSection gain = addSection("Gain");
        gain.addRow("Attenuation", getAttenuationCombo(), getAgcCheckBox());
        gain.addControl(getLnaCheckBox());

        TunerSection options = addSection("Options");
        options.addControl(getBiasTCheckBox());
    }

    @Override
    protected String getTunerInfo()
    {
        AirspyHfTunerController controller = getTuner().getController();
        return "<html><h3>Airspy HF+ Tuner</h3>" +
                "<b>Board: </b>" + controller.getBoardId() + "<br>" +
                "<b>Serial: </b>" + controller.getSerialNumber() + "<br>" +
                "<b>Firmware: </b>" + controller.getFirmwareVersion() + "<br>";
    }

    @Override
    protected void save()
    {
        if(hasConfiguration() && hasTuner() && !isLoading())
        {
            AirspyHfTunerConfiguration config = getConfiguration();
            saveCommonSettings(config);
            config.setSampleRate((int)getTuner().getController().getSampleRate());
            config.setAttenuation(getAttenuationCombo().getValue());
            config.setAgc(getAgcCheckBox().isSelected());
            config.setLna(getLnaCheckBox().isSelected());

            if(getTuner().getController().isBiasTSupported())
            {
                config.setBiasT(getBiasTCheckBox().isSelected());
            }

            saveConfiguration();
        }
    }

    @Override
    protected void tunerStatusUpdated()
    {
        if(hasTuner())
        {
            AirspyHfTunerController controller = getTuner().getController();
            getSampleRateCombo().setDisable(controller.isLockedSampleRate());
            getSampleRateCombo().getItems().setAll(controller.getAvailableSampleRates());
            getSampleRateCombo().setValue(controller.getCurrentAirspySampleRate());
            getAgcCheckBox().setDisable(false);
            getAgcCheckBox().setSelected(controller.getAgc());
            getLnaCheckBox().setDisable(false);
            getLnaCheckBox().setSelected(controller.getLna());
            boolean biasTSupported = controller.isBiasTSupported();
            getBiasTCheckBox().setDisable(!biasTSupported);
            getBiasTCheckBox().setSelected(biasTSupported && controller.getBiasT());
            getBiasTCheckBox().setTooltip(new Tooltip(biasTSupported ? "Enable Bias-T power output for active " +
                    "antennas" : "Bias-T not supported by this device or firmware"));
            getAttenuationCombo().setDisable(controller.getAgc());
            getAttenuationCombo().setValue(controller.getAttenuation());
        }
        else
        {
            getAgcCheckBox().setDisable(true);
            getAgcCheckBox().setSelected(false);
            getLnaCheckBox().setDisable(true);
            getLnaCheckBox().setSelected(false);
            getBiasTCheckBox().setDisable(true);
            getBiasTCheckBox().setSelected(false);
            getBiasTCheckBox().setTooltip(new Tooltip("No tuner available"));
            getSampleRateCombo().setDisable(true);
            getAttenuationCombo().setDisable(true);
            getAttenuationCombo().setValue(Attenuation.A0);
        }

        updateSampleRateToolTip();
    }

    private void updateSampleRateToolTip()
    {
        if(hasTuner() && getTuner().getTunerController().isLockedSampleRate())
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

    @Override
    protected void tunerLockStateUpdated(boolean locked)
    {
        getSampleRateCombo().setDisable(!hasTuner() || locked);
        updateSampleRateToolTip();
    }

    private ComboBox<AirspyHfSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>();
            mSampleRateCombo.setMaxWidth(Double.MAX_VALUE);
            mSampleRateCombo.setDisable(true);
            mSampleRateCombo.setOnAction(e -> {
                AirspyHfSampleRate sampleRate = mSampleRateCombo.getValue();

                if(!isLoading() && hasTuner() && sampleRate != null &&
                    applyToTuner("sample rate [" + sampleRate + "]",
                        () -> getTuner().getController().setSampleRate(sampleRate)))
                {
                    adjustForSampleRate(sampleRate.getSampleRate());
                    save();
                }
            });
        }

        return mSampleRateCombo;
    }

    private ComboBox<Attenuation> getAttenuationCombo()
    {
        if(mAttenuationCombo == null)
        {
            mAttenuationCombo = new ComboBox<>(FXCollections.observableArrayList(Attenuation.values()));
            mAttenuationCombo.setMaxWidth(Double.MAX_VALUE);
            mAttenuationCombo.setTooltip(new Tooltip("Attenuation applies when the AGC is off"));
            mAttenuationCombo.setDisable(true);
            mAttenuationCombo.setOnAction(e -> {
                Attenuation selected = mAttenuationCombo.getValue();

                if(!isLoading() && hasTuner() && selected != null &&
                    applyToTuner("attenuation [" + selected + "]",
                        () -> getTuner().getController().setAttenuation(selected)))
                {
                    save();
                }
            });
        }

        return mAttenuationCombo;
    }

    private CheckBox getAgcCheckBox()
    {
        if(mAgcCheckBox == null)
        {
            mAgcCheckBox = new CheckBox("AGC");
            mAgcCheckBox.setTooltip(new Tooltip("Automatic gain control"));
            mAgcCheckBox.setDisable(true);
            mAgcCheckBox.setOnAction(e -> {
                if(!isLoading() && hasTuner() &&
                    applyToTuner("AGC", () -> getTuner().getController().setAgc(mAgcCheckBox.isSelected())))
                {
                    getAttenuationCombo().setDisable(mAgcCheckBox.isSelected());
                    save();
                }
            });
        }

        return mAgcCheckBox;
    }

    private CheckBox getLnaCheckBox()
    {
        if(mLnaCheckBox == null)
        {
            mLnaCheckBox = new CheckBox("LNA (low noise amplifier)");
            mLnaCheckBox.setTooltip(new Tooltip("Turn on the low noise amplifier"));
            mLnaCheckBox.setDisable(true);
            mLnaCheckBox.setOnAction(e -> {
                if(!isLoading() && hasTuner() &&
                    applyToTuner("LNA", () -> getTuner().getController().setLna(mLnaCheckBox.isSelected())))
                {
                    save();
                }
            });
        }

        return mLnaCheckBox;
    }

    private CheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new CheckBox("Bias-T");
            mBiasTCheckBox.setTooltip(new Tooltip("Enable Bias-T power output for active antennas"));
            mBiasTCheckBox.setDisable(true);
            mBiasTCheckBox.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    if(applyToTuner("Bias-T", () -> getTuner().getController().setBiasT(mBiasTCheckBox.isSelected())))
                    {
                        save();
                    }
                    else
                    {
                        mBiasTCheckBox.setSelected(getTuner().getController().getBiasT());
                    }
                }
            });
        }

        return mBiasTCheckBox;
    }
}

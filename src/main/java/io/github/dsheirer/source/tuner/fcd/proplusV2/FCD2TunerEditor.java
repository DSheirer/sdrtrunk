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
package io.github.dsheirer.source.tuner.fcd.proplusV2;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.fcd.FCDTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;

/**
 * Funcube Dongle Pro Plus tuner editor
 */
public class FCD2TunerEditor extends TunerEditor<FCDTuner, FCD2TunerConfiguration>
{
    private CheckBox mLnaGainCheckBox;
    private CheckBox mMixerGainCheckBox;
    private CheckBox mBiasTCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable tuner and controller
     */
    public FCD2TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        TunerSection gain = addSection("Gain");
        gain.addControl(getLnaGainCheckBox());
        gain.addControl(getMixerGainCheckBox());
        TunerSection options = addSection("Options");
        options.addControl(getBiasTCheckBox());
        refresh();
    }

    private FCD2TunerController getController()
    {
        if(hasTuner())
        {
            return (FCD2TunerController)getTuner().getController();
        }

        return null;
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return FCD2TunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return FCD2TunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    protected void tunerStatusUpdated()
    {
        getLnaGainCheckBox().setDisable(!hasTuner());
        getMixerGainCheckBox().setDisable(!hasTuner());
        getBiasTCheckBox().setDisable(!hasTuner());

        if(hasTuner() && hasConfiguration())
        {
            getLnaGainCheckBox().setSelected(getConfiguration().getGainLNA());
            getMixerGainCheckBox().setSelected(getConfiguration().getGainMixer());
            getBiasTCheckBox().setSelected(getConfiguration().isBiasT());
        }
    }

    private CheckBox getLnaGainCheckBox()
    {
        if(mLnaGainCheckBox == null)
        {
            mLnaGainCheckBox = new CheckBox("LNA gain");
            mLnaGainCheckBox.setDisable(true);
            mLnaGainCheckBox.setOnAction(event -> {
                if(!isLoading() && hasTuner() && applyToTuner("LNA gain",
                        () -> getController().setLNAGain(getLnaGainCheckBox().isSelected())))
                {
                    save();
                }
            });
        }

        return mLnaGainCheckBox;
    }

    private CheckBox getMixerGainCheckBox()
    {
        if(mMixerGainCheckBox == null)
        {
            mMixerGainCheckBox = new CheckBox("Mixer gain");
            mMixerGainCheckBox.setDisable(true);
            mMixerGainCheckBox.setOnAction(event -> {
                if(!isLoading() && hasTuner() && applyToTuner("mixer gain",
                        () -> getController().setMixerGain(getMixerGainCheckBox().isSelected())))
                {
                    save();
                }
            });
        }

        return mMixerGainCheckBox;
    }

    private CheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new CheckBox("Bias-T");
            mBiasTCheckBox.setDisable(true);
            mBiasTCheckBox.setTooltip(new Tooltip("Enable Bias-T power output for active antennas"));
            mBiasTCheckBox.setOnAction(event -> {
                if(hasTuner() && !isLoading() && applyToTuner("Bias-T",
                        () -> getController().setBiasT(getBiasTCheckBox().isSelected())))
                {
                    save();
                }
            });
        }

        return mBiasTCheckBox;
    }

    @Override
    protected String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><h3>Funcube Dongle Pro Plus Tuner</h3>");
        sb.append("<b>USB ID: </b>").append(getController().getUSBID()).append("<br>");
        sb.append("<b>USB Address: </b>").append(getController().getUSBAddress()).append("<br>");
        sb.append("<b>USB Speed: </b>").append(getController().getUSBSpeed()).append("<br>");
        sb.append("<b>Cellular Band: </b>").append(getController().getConfiguration().getBandBlocking()).append("<br>");
        sb.append("<b>Firmware: </b>").append(getController().getConfiguration().getFirmware()).append("<br>");
        return sb.toString();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            FCD2TunerConfiguration config = getConfiguration();
            saveCommonSettings(config);
            config.setGainLNA(getLnaGainCheckBox().isSelected());
            config.setGainMixer(getMixerGainCheckBox().isSelected());
            config.setBiasT(getBiasTCheckBox().isSelected());
            saveConfiguration();
        }
    }
}

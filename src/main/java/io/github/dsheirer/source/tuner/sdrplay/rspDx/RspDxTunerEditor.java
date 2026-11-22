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
package io.github.dsheirer.source.tuner.sdrplay.rspDx;

import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import java.util.List;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

/**
 * RSPdx tuner editor
 */
public class RspDxTunerEditor extends RspTunerEditor<RspDxTunerConfiguration>
{
    private final ComboBox<RspDxAntennaSelection> mAntennaCombo;
    private final CheckBox mBiasTCheckBox;
    private final CheckBox mRfNotchCheckBox;
    private final CheckBox mRfDabNotchCheckBox;
    private final CheckBox mHdrModeCheckBox;
    private final ComboBox<HdrModeBandwidth> mHdrModeBandwidthCombo;

    /**
     * Constructs an instance
     * @param settings for preference settings
     * @param tunerManager for saving tuner configurations
     * @param discoveredTuner to edit
     */
    public RspDxTunerEditor(Settings settings, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(settings, tunerManager, discoveredTuner);
        TunerSection options = addSection("Options");
        mAntennaCombo = addChoice(options, "Antenna", List.of(RspDxAntennaSelection.values()),
                () -> control().getAntenna(), selection -> control().setAntenna(selection));
        mBiasTCheckBox = addOption(options, "ANT B Bias-T power", "Provide DC power on the ANT B connector",
                () -> control().isBiasT(), enabled -> control().setBiasT(enabled));
        mRfNotchCheckBox = addOption(options, "FM broadcast band filter (77-115 MHz)", null,
                () -> control().isRfNotch(), enabled -> control().setRfNotch(enabled));
        mRfDabNotchCheckBox = addOption(options, "DAB broadcast band filter (155-235 MHz)", null,
                () -> control().isRfDabNotch(), enabled -> control().setRfDabNotch(enabled));

        TunerSection hdr = addSection("High Dynamic Range");
        mHdrModeCheckBox = addOption(hdr, "HDR mode (1 kHz - 2 MHz)", "High dynamic range mode for " +
                "frequencies below 2 MHz", () -> control().isHighDynamicRange(),
                enabled -> control().setHighDynamicRange(enabled));
        mHdrModeBandwidthCombo = addChoice(hdr, "Bandwidth", HdrModeBandwidth.VALID,
                () -> control().getHdrModeBandwidth(), bandwidth -> control().setHdrModeBandwidth(bandwidth));
        refresh();
    }

    private IControlRspDx control()
    {
        return (IControlRspDx)getControlRsp();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            RspDxTunerConfiguration config = getConfiguration();
            saveRspSettings(config);
            config.setBiasT(mBiasTCheckBox.isSelected());
            config.setHdrMode(mHdrModeCheckBox.isSelected());
            config.setRfDabNotch(mRfDabNotchCheckBox.isSelected());
            config.setRfNotch(mRfNotchCheckBox.isSelected());

            if(mAntennaCombo.getValue() != null)
            {
                config.setAntenna(mAntennaCombo.getValue());
            }

            if(mHdrModeBandwidthCombo.getValue() != null)
            {
                config.setHdrModeBandwidth(mHdrModeBandwidthCombo.getValue());
            }

            saveConfiguration();
        }
    }
}

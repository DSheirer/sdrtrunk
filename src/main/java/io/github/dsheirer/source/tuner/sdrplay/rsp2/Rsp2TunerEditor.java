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
package io.github.dsheirer.source.tuner.sdrplay.rsp2;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import java.util.List;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

/**
 * RSP2 tuner editor
 */
public class Rsp2TunerEditor extends RspTunerEditor<Rsp2TunerConfiguration>
{
    private final ComboBox<Rsp2AntennaSelection> mAntennaCombo;
    private final CheckBox mBiasTCheckBox;
    private final CheckBox mExternalReferenceOutputCheckBox;
    private final CheckBox mRfNotchCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for preference settings
     * @param tunerManager for saving tuner configurations
     * @param discoveredTuner to edit
     */
    public Rsp2TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        TunerSection options = addSection("Options");
        mAntennaCombo = addChoice(options, "Antenna", List.of(Rsp2AntennaSelection.values()),
                () -> control().getAntennaSelection(), selection -> control().setAntennaSelection(selection));
        mBiasTCheckBox = addOption(options, "ANT B Bias-T power", "Provide DC power on the ANT B connector",
                () -> control().isBiasT(), enabled -> control().setBiasT(enabled));
        mRfNotchCheckBox = addOption(options, "FM broadcast band filter (77-115 MHz)", null,
                () -> control().isRfNotch(), enabled -> control().setRfNotch(enabled));
        mExternalReferenceOutputCheckBox = addOption(options, "External reference output", null,
                () -> control().isExternalReferenceOutput(), enabled -> control().setExternalReferenceOutput(enabled));
        refresh();
    }

    private IControlRsp2 control()
    {
        return (IControlRsp2)getControlRsp();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            Rsp2TunerConfiguration config = getConfiguration();
            saveRspSettings(config);
            config.setBiasT(mBiasTCheckBox.isSelected());
            config.setRfNotch(mRfNotchCheckBox.isSelected());
            config.setExternalReferenceOutput(mExternalReferenceOutputCheckBox.isSelected());

            if(mAntennaCombo.getValue() != null)
            {
                config.setAntennaSelection(mAntennaCombo.getValue());
            }

            saveConfiguration();
        }
    }
}

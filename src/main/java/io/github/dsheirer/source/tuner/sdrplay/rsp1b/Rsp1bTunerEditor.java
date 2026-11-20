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
package io.github.dsheirer.source.tuner.sdrplay.rsp1b;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import javafx.scene.control.CheckBox;

/**
 * RSP1B tuner editor
 */
public class Rsp1bTunerEditor extends RspTunerEditor<Rsp1bTunerConfiguration>
{
    private final CheckBox mBiasTCheckBox;
    private final CheckBox mRfNotchCheckBox;
    private final CheckBox mRfDabNotchCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for preference settings
     * @param tunerManager for saving tuner configurations
     * @param discoveredTuner to edit
     */
    public Rsp1bTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        TunerSection options = addSection("Options");
        mBiasTCheckBox = addOption(options, "Bias-T power", "Provide DC power on the antenna connector",
                () -> control().isBiasT(), enabled -> control().setBiasT(enabled));
        mRfNotchCheckBox = addOption(options, "FM broadcast band filter (77-115 MHz)", null,
                () -> control().isRfNotch(), enabled -> control().setRfNotch(enabled));
        mRfDabNotchCheckBox = addOption(options, "DAB broadcast band filter (160-235 MHz)", null,
                () -> control().isRfDabNotch(), enabled -> control().setRfDabNotch(enabled));
        refresh();
    }

    private IControlRsp1b control()
    {
        return (IControlRsp1b)getControlRsp();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            Rsp1bTunerConfiguration config = getConfiguration();
            saveRspSettings(config);
            config.setBiasT(mBiasTCheckBox.isSelected());
            config.setRfNotch(mRfNotchCheckBox.isSelected());
            config.setRfDabNotch(mRfDabNotchCheckBox.isSelected());
            saveConfiguration();
        }
    }
}

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
package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import java.util.Collection;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;

/**
 * RSPduo tuner 2 editor.  In master/slave mode, tuner 2 is the slave: its sample rate and external reference output
 * follow the master (tuner 1) and it is enabled or disabled with the master.
 */
public class RspDuoTuner2Editor extends RspTunerEditor<RspDuoTuner2Configuration>
{
    private final CheckBox mExternalReferenceOutputCheckBox;
    private final CheckBox mBiasTCheckBox;
    private final CheckBox mRfDabNotchCheckBox;
    private final CheckBox mRfNotchCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for preference settings
     * @param tunerManager for saving tuner configurations
     * @param discoveredTuner to edit
     */
    public RspDuoTuner2Editor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        addHeaderNode(RspDuoTuner1Editor.createPreferencesButton());

        TunerSection options = addSection("Options");
        mBiasTCheckBox = addOption(options, "Bias-T power", "Provide DC power on the tuner 2 antenna connector",
                () -> control().isBiasT(), enabled -> control().setBiasT(enabled));
        mRfNotchCheckBox = addOption(options, "FM broadcast band filter (78-114 MHz)", null,
                () -> control().isRfNotch(), enabled -> control().setRfNotch(enabled));
        mRfDabNotchCheckBox = addOption(options, "DAB broadcast band filter (157-235 MHz)", null,
                () -> control().isRfDabNotch(), enabled -> control().setRfDabNotch(enabled));
        mExternalReferenceOutputCheckBox = addOption(options, "External reference output", null,
                () -> !isSlaveMode() && control().isExternalReferenceOutput(),
                enabled -> control().setExternalReferenceOutput(enabled));
        refresh();
    }

    private IControlRspDuoTuner2 control()
    {
        return (IControlRspDuoTuner2)getControlRsp();
    }

    /**
     * Indicates if the tuner is configured as the slave in master/slave mode.
     */
    private boolean isSlaveMode()
    {
        return getDiscoveredRspTuner().getDeviceSelectionMode().isSlaveMode();
    }

    @Override
    protected Collection<RspSampleRate> getSampleRates()
    {
        return control().getSupportedSampleRates();
    }

    @Override
    protected boolean isSampleRateChangeable()
    {
        return !isSlaveMode();
    }

    @Override
    protected void rspTunerStatusUpdated()
    {
        if(isSlaveMode())
        {
            //The user disables the slave by disabling the master tuner
            getEnabledSwitch().setDisable(true);
            getEnabledSwitch().setTooltip(new Tooltip("Enable or disable the RSPduo with tuner 1 (master)"));
            mExternalReferenceOutputCheckBox.setDisable(true);
            mExternalReferenceOutputCheckBox.setTooltip(new Tooltip("Set by tuner 1 (master)"));
        }
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            RspDuoTuner2Configuration config = getConfiguration();
            saveRspSettings(config);
            config.setBiasT(mBiasTCheckBox.isSelected());

            if(hasTuner() && !isSlaveMode())
            {
                config.setExternalReferenceOutput(mExternalReferenceOutputCheckBox.isSelected());
            }

            config.setRfDabNotch(mRfDabNotchCheckBox.isSelected());
            config.setRfNotch(mRfNotchCheckBox.isSelected());
            saveConfiguration();
        }
    }
}

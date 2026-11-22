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

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.ViewSettingsEditorRequest;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;

/**
 * RSPduo tuner 1 editor
 */
public class RspDuoTuner1Editor extends RspTunerEditor<RspDuoTuner1Configuration>
{
    private final CheckBox mExternalReferenceOutputCheckBox;
    private final CheckBox mAmNotchCheckBox;
    private final CheckBox mRfDabNotchCheckBox;
    private final CheckBox mRfNotchCheckBox;
    private final ComboBox<RspDuoAmPortSelection> mAmPortCombo;

    /**
     * Constructs an instance
     * @param settings for preference settings
     * @param tunerManager for saving tuner configurations
     * @param discoveredTuner to edit
     */
    public RspDuoTuner1Editor(Settings settings, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(settings, tunerManager, discoveredTuner);
        addHeaderNode(createPreferencesButton());

        TunerSection options = addSection("Options");
        mAmPortCombo = addChoice(options, "AM Port", List.of(RspDuoAmPortSelection.values()),
                () -> control().getAmPort(), port -> control().setAmPort(port));
        mRfNotchCheckBox = addOption(options, "FM broadcast band filter (78-114 MHz)", null,
                () -> control().isRfNotch(), enabled -> control().setRfNotch(enabled));
        mRfDabNotchCheckBox = addOption(options, "DAB broadcast band filter (157-235 MHz)", null,
                () -> control().isRfDabNotch(), enabled -> control().setRfDabNotch(enabled));
        mAmNotchCheckBox = addOption(options, "AM broadcast band filter (415-1640 kHz)", null,
                () -> control().isAmNotch(), enabled -> control().setAmNotch(enabled));
        mExternalReferenceOutputCheckBox = addOption(options, "External reference output", null,
                () -> control().isExternalReferenceOutput(), enabled -> control().setExternalReferenceOutput(enabled));
        refresh();
    }

    /**
     * Button that opens the RSPduo (tuner) preferences, where the RSPduo dual tuner mode is chosen.
     */
    static Button createPreferencesButton()
    {
        Button button = new Button("RSPduo Settings");
        button.setTooltip(new Tooltip("Choose how sdrtrunk uses the RSPduo's two tuners"));
        button.setOnAction(e -> MyEventBus.getGlobalEventBus()
                .post(new ViewSettingsEditorRequest(SettingsPageType.TUNERS)));
        return button;
    }

    private IControlRspDuoTuner1 control()
    {
        return (IControlRspDuoTuner1)getControlRsp();
    }

    @Override
    protected Collection<RspSampleRate> getSampleRates()
    {
        return control().getSupportedSampleRates();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            RspDuoTuner1Configuration config = getConfiguration();
            saveRspSettings(config);
            config.setAmNotch(mAmNotchCheckBox.isSelected());
            config.setAmPort(mAmPortCombo.getValue());
            config.setExternalReferenceOutput(mExternalReferenceOutputCheckBox.isSelected());
            config.setRfDabNotch(mRfDabNotchCheckBox.isSelected());
            config.setRfNotch(mRfNotchCheckBox.isSelected());
            saveConfiguration();
        }
    }
}

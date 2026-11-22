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
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xTunerConfiguration;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerSection;
import javafx.scene.control.Label;

/**
 * Editor for an RTL-2832 with an embedded tuner type that sdrtrunk doesn't support.  Only shows the tuner status.
 */
public class RTL2832UnknownTunerEditor extends TunerEditor<RTL2832Tuner, RTL2832TunerConfiguration>
{
    private final Label mUniqueIdLabel = new Label();

    public RTL2832UnknownTunerEditor(UserPreferences userPreferences, TunerManager tunerManager,
                                     DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        TunerSection section = addSection("Unsupported Tuner");
        Label message = new Label("This RTL-2832 device has an embedded tuner type that isn't supported.");
        message.setWrapText(true);
        section.addFullRow(message);
        section.addRow("ID", mUniqueIdLabel);
        refresh();
    }

    @Override
    protected boolean hasTuningControls()
    {
        return false;
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        //Bogus value.
        return R8xTunerConfiguration.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        //Bogus value.
        return R8xTunerConfiguration.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    protected void save()
    {
        //No-op
    }

    @Override
    protected void tunerStatusUpdated()
    {
        mUniqueIdLabel.setText(hasTuner() ? getTuner().getUniqueID() : getDiscoveredTuner().getId());
    }
}

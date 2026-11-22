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
package io.github.dsheirer.source.tuner.sdrplay.rsp1;

import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerEditor;

/**
 * RSP1 tuner editor
 */
public class Rsp1TunerEditor extends RspTunerEditor<Rsp1TunerConfiguration>
{
    /**
     * Constructs an instance
     * @param settings for preference settings
     * @param tunerManager for saving tuner configurations
     * @param discoveredTuner to edit
     */
    public Rsp1TunerEditor(Settings settings, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(settings, tunerManager, discoveredTuner);
        refresh();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            saveRspSettings(getConfiguration());
            saveConfiguration();
        }
    }
}

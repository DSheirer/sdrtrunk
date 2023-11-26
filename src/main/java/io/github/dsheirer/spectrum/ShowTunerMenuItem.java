/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.spectrum;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import java.awt.EventQueue;

import javax.swing.JMenuItem;

/**
 * Swing menu item to show a tuner in the spectral display.
 */
public class ShowTunerMenuItem extends JMenuItem
{
    private final DiscoveredTunerModel mDiscoveredTunerModel;
    private final UserPreferences mUserPreferences;
    private final Tuner mTuner;

    /**
     * Constructs an instance
     * @param tuner to show
     * @param discoveredTunerModel to receive spectral display request
     * @param userPreferences to override the spectral display enabled state to true before showing the tuner.
     */
    public ShowTunerMenuItem(Tuner tuner, DiscoveredTunerModel discoveredTunerModel, UserPreferences userPreferences)
    {
        super(tuner != null ? "Show: " + tuner.getPreferredName() : "(empty)");
        mTuner = tuner;
        mDiscoveredTunerModel = discoveredTunerModel;
        mUserPreferences = userPreferences;
        addActionListener(e -> EventQueue.invokeLater(() -> {
            mUserPreferences.getApplicationPreference().setSpectralDisplayEnabled(true);
            mDiscoveredTunerModel.broadcast(new TunerEvent(mTuner, TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY));
        }));
    }
}

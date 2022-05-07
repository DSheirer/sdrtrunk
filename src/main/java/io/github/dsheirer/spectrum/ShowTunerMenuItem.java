/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;

import javax.swing.JMenuItem;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ShowTunerMenuItem extends JMenuItem
{
    private DiscoveredTunerModel mDiscoveredTunerModel;
    private Tuner mTuner;

    public ShowTunerMenuItem(DiscoveredTunerModel discoveredTunerModel, Tuner tuner)
    {
        super(tuner != null ? "Show: " + tuner.getPreferredName() : "(empty)");
        mDiscoveredTunerModel = discoveredTunerModel;
        mTuner = tuner;

        addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                EventQueue.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        SystemProperties properties = SystemProperties.getInstance();
                        properties.set(SpectralDisplayPanel.SPECTRAL_DISPLAY_ENABLED, true);
                        mDiscoveredTunerModel.broadcast(new TunerEvent(mTuner, TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY));
                    }
                });
            }
        });
    }
}

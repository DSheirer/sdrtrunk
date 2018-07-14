/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.spectrum;

import io.github.dsheirer.source.tuner.Tuner;

import javax.swing.JMenuItem;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ShowTunerMenuItem extends JMenuItem
{
    private SpectralDisplayPanel mSpectralDisplayPanel;
    private Tuner mTuner;

    public ShowTunerMenuItem(SpectralDisplayPanel spectralDisplayPanel, Tuner tuner)
    {
        super(tuner != null ? "Show: " + tuner.getName() : "(empty)");
        mSpectralDisplayPanel = spectralDisplayPanel;
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
                        mSpectralDisplayPanel.showTuner(mTuner);
                    }
                });
            }
        });
    }
}

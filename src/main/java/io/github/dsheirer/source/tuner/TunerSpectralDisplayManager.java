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
package io.github.dsheirer.source.tuner;

import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.spectrum.SpectrumFrame;

import java.awt.EventQueue;

public class TunerSpectralDisplayManager implements Listener<TunerEvent>
{
    private SpectralDisplayPanel mSpectralDisplayPanel;
    private PlaylistManager mPlaylistManager;
    private SettingsManager mSettingsManager;
    private TunerModel mTunerModel;

    public TunerSpectralDisplayManager(SpectralDisplayPanel panel, PlaylistManager playlistManager,
                                       SettingsManager settingsManager, TunerModel tunerModel)
    {
        mSpectralDisplayPanel = panel;
        mPlaylistManager = playlistManager;
        mSettingsManager = settingsManager;
        mTunerModel = tunerModel;
    }

    @Override
    public void receive(TunerEvent event)
    {
        switch(event.getEvent())
        {
            case CLEAR_MAIN_SPECTRAL_DISPLAY:
                mSpectralDisplayPanel.clearTuner();
                break;
            case REQUEST_MAIN_SPECTRAL_DISPLAY:
                mSpectralDisplayPanel.showTuner(event.getTuner());
                break;
            case REQUEST_NEW_SPECTRAL_DISPLAY:
                final SpectrumFrame frame = new SpectrumFrame(mPlaylistManager, mSettingsManager, mTunerModel, event.getTuner());
                EventQueue.invokeLater(() -> frame.setVisible(true));
                break;
            default:
                break;
        }
    }
}

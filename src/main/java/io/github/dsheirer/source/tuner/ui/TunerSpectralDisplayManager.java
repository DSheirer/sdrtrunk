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
package io.github.dsheirer.source.tuner.ui;

import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.spectrum.SpectrumFrame;
import io.github.dsheirer.util.SwingUtils;
import io.github.dsheirer.util.ThreadPool;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spectral display manager for displaying tuner spectral content.
 */
public class TunerSpectralDisplayManager implements Listener<TunerEvent>
{
    private Logger mLog = LoggerFactory.getLogger(TunerSpectralDisplayManager.class);

    private SpectralDisplayPanel mSpectralDisplayPanel;
    private PlaylistManager mPlaylistManager;
    private SettingsManager mSettingsManager;
    private DiscoveredTunerModel mDiscoveredTunerModel;

    /**
     * Constructs an instance
     * @param panel to manage
     * @param playlistManager for channel updates
     * @param settingsManager for settings
     * @param discoveredTunerModel to access tuners
     */
    public TunerSpectralDisplayManager(SpectralDisplayPanel panel, PlaylistManager playlistManager,
                                       SettingsManager settingsManager, DiscoveredTunerModel discoveredTunerModel)
    {
        mSpectralDisplayPanel = panel;
        mPlaylistManager = playlistManager;
        mSettingsManager = settingsManager;
        mDiscoveredTunerModel = discoveredTunerModel;
    }

    /**
     * Shows the first available tuner from the discovered tuner model
     */
    public Tuner showFirstTuner()
    {
        //Ensure spectral display is enabled before selecting first tuner
        if(SystemProperties.getInstance().get(SpectralDisplayPanel.SPECTRAL_DISPLAY_ENABLED, true))
        {
            List<DiscoveredTuner> availableTuners = mDiscoveredTunerModel.getAvailableTuners();

            for(DiscoveredTuner discoveredTuner: availableTuners)
            {
                if(discoveredTuner.hasTuner())
                {
                    mSpectralDisplayPanel.showTuner(discoveredTuner.getTuner());
                    return discoveredTuner.getTuner();
                }
            }
        }

        return null;
    }

    @Override
    public void receive(TunerEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_CLEAR_MAIN_SPECTRAL_DISPLAY:
                SwingUtils.run(() -> mSpectralDisplayPanel.clearTuner());
                break;
            case REQUEST_MAIN_SPECTRAL_DISPLAY:
                if(SystemProperties.getInstance().get(SpectralDisplayPanel.SPECTRAL_DISPLAY_ENABLED, true))
                {
                    SwingUtils.run(() -> mSpectralDisplayPanel.showTuner(event.getTuner()));
                }
                break;
            case REQUEST_NEW_SPECTRAL_DISPLAY:
                final SpectrumFrame frame = new SpectrumFrame(mPlaylistManager, mSettingsManager, mDiscoveredTunerModel, event.getTuner());
                SwingUtils.run(() -> frame.setVisible(true));
                break;
            case NOTIFICATION_ERROR_STATE:
            case NOTIFICATION_SHUTTING_DOWN:
                if(event.getTuner().equals(mSpectralDisplayPanel.getTuner()))
                {
                    SwingUtils.run(() ->
                    {
                        mSpectralDisplayPanel.clearTuner();
                        ThreadPool.SCHEDULED.schedule(() -> SwingUtils.run(() ->
                        {
                            showFirstTuner();
                        }), 1, TimeUnit.SECONDS);
                    });
                }
            default:
                break;
        }
    }
}

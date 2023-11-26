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
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.spectrum.SpectrumFrame;
import io.github.dsheirer.util.SwingUtils;
import io.github.dsheirer.util.ThreadPool;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Spectral display manager for displaying tuner spectral content.
 */
@Component("tunerSpectralDisplayManager")
public class TunerSpectralDisplayManager implements Listener<TunerEvent>
{
    private Logger mLog = LoggerFactory.getLogger(TunerSpectralDisplayManager.class);
    @Resource
    private PlaylistManager mPlaylistManager;
    @Resource
    private SettingsManager mSettingsManager;
    @Resource
    private DiscoveredTunerModel mDiscoveredTunerModel;
    @Resource
    private UserPreferences mUserPreferences;
    @Resource
    private ObjectProvider<SpectrumFrame> mSpectrumFrameObjectProvider;

    private SpectralDisplayPanel mSpectralDisplayPanel;

    /**
     * Constructs an instance
     */
    public TunerSpectralDisplayManager()
    {
    }

    /**
     * Sets the panel to be managed.
     * @param spectralDisplayPanel to be managed
     */
    public void setSpectralDisplayPanel(SpectralDisplayPanel spectralDisplayPanel)
    {
        mSpectralDisplayPanel = spectralDisplayPanel;
    }

    /**
     * Shows the first available tuner from the discovered tuner model
     */
    public Tuner showFirstTuner()
    {
        //Ensure spectral display is enabled before selecting first tuner
        boolean enabled = mUserPreferences.getApplicationPreference().isSpectralDisplayEnabled();

        if(enabled)
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
                boolean enabled = mUserPreferences.getApplicationPreference().isSpectralDisplayEnabled();

                if(enabled)
                {
                    SwingUtils.run(() -> mSpectralDisplayPanel.showTuner(event.getTuner()));
                }
                break;
            case REQUEST_NEW_SPECTRAL_DISPLAY:
                final SpectrumFrame frame = mSpectrumFrameObjectProvider.getObject();
                frame.setTuner(event.getTuner()); //This sets visible to true on the swing thread.
                break;
            case NOTIFICATION_ERROR_STATE:
            case NOTIFICATION_SHUTTING_DOWN:
                if(event.getTuner().equals(mSpectralDisplayPanel.getTuner()))
                {
                    SwingUtils.run(() ->
                    {
                        mSpectralDisplayPanel.clearTuner();
                        //Request to show an alternate tuner since this (currently displayed) tuner is shutting down.
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

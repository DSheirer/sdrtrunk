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

package io.github.dsheirer.gui.spectrum;

import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.util.ThreadPool;
import java.awt.EventQueue;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

/**
 * Swing panel that holds the JavaFX tuner spectral display for the main application window.
 *
 * The JavaFX display is created on the JavaFX application thread after construction.  A tuner requested before the
 * display exists is shown once the display is ready.
 */
public class SpectralDisplayPanel extends JFXPanel
{
    private final DiscoveredTunerModel mDiscoveredTunerModel;
    private TunerSpectralDisplay mTunerSpectralDisplay;
    private Tuner mPendingTuner;
    private boolean mDisposed;

    /**
     * Constructs an instance.
     * @param playlistManager for the channels
     * @param discoveredTunerModel for the list of tuners
     */
    public SpectralDisplayPanel(PlaylistManager playlistManager, DiscoveredTunerModel discoveredTunerModel)
    {
        mDiscoveredTunerModel = discoveredTunerModel;
        Platform.setImplicitExit(false);

        Platform.runLater(() -> {
            TunerSpectralDisplay display = new TunerSpectralDisplay(playlistManager, discoveredTunerModel, true);
            display.setTunerSelectionHandler(this::tunerSelected);
            Scene scene = new Scene(display);
            ThemeManager.getInstance().register(scene);
            setScene(scene);

            Tuner pending;

            synchronized(SpectralDisplayPanel.this)
            {
                if(mDisposed)
                {
                    ThreadPool.CACHED.submit(display::dispose);
                    return;
                }

                mTunerSpectralDisplay = display;
                pending = mPendingTuner;
                mPendingTuner = null;
            }

            if(pending != null)
            {
                ThreadPool.CACHED.submit(() -> display.showTuner(pending));
            }
        });
    }

    /**
     * Handles a tuner selection in the configuration panel.  Selecting a tuner turns the main spectral display on and
     * selecting no tuner turns it off, the same as the View menu.
     */
    private void tunerSelected(Tuner tuner)
    {
        //Broadcast on the Swing event thread, the same as the Swing tuner menu items, since listeners update Swing
        EventQueue.invokeLater(() -> {
            if(tuner != null)
            {
                SystemProperties.getInstance().set(TunerSpectralDisplay.SPECTRAL_DISPLAY_ENABLED, true);
                mDiscoveredTunerModel.broadcast(new TunerEvent(tuner, TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY));
            }
            else
            {
                SystemProperties.getInstance().set(TunerSpectralDisplay.SPECTRAL_DISPLAY_ENABLED, false);
                mDiscoveredTunerModel.broadcast(new TunerEvent(getTuner(),
                        TunerEvent.Event.REQUEST_CLEAR_MAIN_SPECTRAL_DISPLAY));
            }
        });
    }

    /**
     * Shows the tuner in the display.  Can block while the tuner starts its sample stream.
     */
    public void showTuner(Tuner tuner)
    {
        TunerSpectralDisplay display;

        synchronized(this)
        {
            if(mTunerSpectralDisplay == null)
            {
                mPendingTuner = tuner;
                return;
            }

            display = mTunerSpectralDisplay;
        }

        display.showTuner(tuner);
    }

    /**
     * Stops showing the current tuner.
     */
    public void clearTuner()
    {
        TunerSpectralDisplay display;

        synchronized(this)
        {
            if(mTunerSpectralDisplay == null)
            {
                mPendingTuner = null;
                return;
            }

            display = mTunerSpectralDisplay;
        }

        display.clearTuner();
    }

    /**
     * Currently displayed tuner, or null.
     */
    public Tuner getTuner()
    {
        TunerSpectralDisplay display;

        synchronized(this)
        {
            if(mTunerSpectralDisplay == null)
            {
                return mPendingTuner;
            }

            display = mTunerSpectralDisplay;
        }

        return display.getTuner();
    }

    /**
     * Stops showing the tuner and releases resources.
     */
    public void dispose()
    {
        TunerSpectralDisplay display;

        synchronized(this)
        {
            mDisposed = true;
            display = mTunerSpectralDisplay;
            mTunerSpectralDisplay = null;
            mPendingTuner = null;
        }

        if(display != null)
        {
            display.dispose();
        }
    }
}

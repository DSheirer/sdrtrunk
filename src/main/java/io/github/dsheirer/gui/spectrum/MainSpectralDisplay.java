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

import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.util.ThreadPool;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main window spectrum and waterfall display for the selected tuner.
 *
 * The display can be hidden.  A hidden display is disposed: it stops the DFT processing, disconnects from the tuner's
 * sample stream and releases the waterfall images, so it uses no processing or memory.  The selected tuner is kept
 * and a new display shows it again when the display is shown.
 *
 * Showing a tuner can block while the tuner starts its sample stream, so tuner changes run in order on a dedicated
 * thread and the methods can be called from any thread.
 */
public class MainSpectralDisplay extends StackPane
{
    private static final Logger mLog = LoggerFactory.getLogger(MainSpectralDisplay.class);
    //SDRTrunk.properties key used by earlier versions, read only to migrate it
    private static final String LEGACY_TUNER_ENABLED_KEY = "spectral.display.enabled";

    private final PlaylistManager mPlaylistManager;
    private final DiscoveredTunerModel mDiscoveredTunerModel;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor(
        new NamingThreadFactory("sdrtrunk main spectral display"));
    private final ReadOnlyBooleanWrapper mDisplayVisible = new ReadOnlyBooleanWrapper(true);
    private TunerSpectralDisplay mDisplay;
    private Tuner mTuner;
    private Runnable mHideRequestHandler;
    private boolean mDisposed;

    /**
     * Constructs an instance.  JavaFX thread only.
     * @param playlistManager for the channels
     * @param discoveredTunerModel for the list of tuners
     * @param visible true to create the display now, or false to start hidden
     */
    public MainSpectralDisplay(PlaylistManager playlistManager, DiscoveredTunerModel discoveredTunerModel,
                               boolean visible)
    {
        mPlaylistManager = playlistManager;
        mDiscoveredTunerModel = discoveredTunerModel;
        getStyleClass().add("main-spectral-display");
        setMinSize(0, 0);
        mDisplayVisible.set(visible);

        if(visible)
        {
            createDisplay();
        }
    }

    /**
     * Indicates if the display is showing (true) or hidden and disposed (false).
     */
    public ReadOnlyBooleanProperty displayVisibleProperty()
    {
        return mDisplayVisible.getReadOnlyProperty();
    }

    /**
     * Sets the handler that the display's hide button invokes, so that the main window can collapse its pane and
     * update its menu.  The handler is expected to call {@link #setDisplayVisible(boolean)}.
     */
    public void setHideRequestHandler(Runnable handler)
    {
        mHideRequestHandler = handler;

        TunerSpectralDisplay display = mDisplay;

        if(display != null)
        {
            display.hideHandlerProperty().set(handler);
        }
    }

    /**
     * Shows or hides the display.  Hiding disposes the display so that it uses no processing or memory.  Showing
     * creates a new display for the selected tuner.  JavaFX thread only.
     */
    public void setDisplayVisible(boolean visible)
    {
        if(visible == mDisplayVisible.get() || mDisposed)
        {
            return;
        }

        mDisplayVisible.set(visible);

        if(visible)
        {
            createDisplay();
        }
        else
        {
            TunerSpectralDisplay display;

            synchronized(this)
            {
                display = mDisplay;
                mDisplay = null;
            }

            getChildren().clear();

            if(display != null)
            {
                //Disconnecting from the tuner can block, so dispose on the display thread
                mExecutor.submit(() -> dispose(display));
            }
        }
    }

    /**
     * Creates the display and shows the selected tuner.  JavaFX thread only.
     */
    private void createDisplay()
    {
        TunerSpectralDisplay display = new TunerSpectralDisplay(mPlaylistManager, mDiscoveredTunerModel, true);
        display.setTunerSelectionHandler(this::tunerSelected);
        display.hideHandlerProperty().set(mHideRequestHandler);
        getChildren().setAll(display);

        Tuner tuner;

        synchronized(this)
        {
            mDisplay = display;
            tuner = mTuner;
        }

        if(tuner != null)
        {
            mExecutor.submit(() -> {
                synchronized(MainSpectralDisplay.this)
                {
                    //Skip when the display was hidden or the tuner changed before this ran
                    if(mDisplay != display || mTuner != tuner)
                    {
                        return;
                    }
                }

                display.showTuner(tuner);
            });
        }
    }

    /**
     * Handles a tuner selection in the configuration panel.  Selecting a tuner turns the main spectral display on and
     * selecting no tuner turns it off, the same as the View menu.
     */
    private void tunerSelected(Tuner tuner)
    {
        Tuner current = getTuner();

        ThreadPool.CACHED.submit(() -> {
            if(tuner != null)
            {
                setTunerEnabled(true);
                mDiscoveredTunerModel.broadcast(new TunerEvent(tuner, TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY));
            }
            else
            {
                setTunerEnabled(false);
                mDiscoveredTunerModel.broadcast(new TunerEvent(current,
                    TunerEvent.Event.REQUEST_CLEAR_MAIN_SPECTRAL_DISPLAY));
            }
        });
    }

    /**
     * Selects the tuner to show.  When the display is hidden, the tuner is shown when the display is shown again.
     * Can be called from any thread and doesn't block.
     */
    public void showTuner(Tuner tuner)
    {
        synchronized(this)
        {
            mTuner = tuner;
        }

        mExecutor.submit(() -> {
            TunerSpectralDisplay display;

            synchronized(MainSpectralDisplay.this)
            {
                if(mTuner != tuner)
                {
                    return;
                }

                display = mDisplay;
            }

            if(display != null)
            {
                display.showTuner(tuner);
            }
        });
    }

    /**
     * Zooms the display to the zoom level and centres it on the frequency.  Used when a channel's "show in
     * waterfall" request names the channel frequency.  Queued on the display thread after showTuner() so the tuner's
     * frequency range is known, then applied on the FX thread; the zoom is reset first so it always re-centres.
     * Can be called from any thread and doesn't block.
     * @param zoom level (0 = full bandwidth, each level doubles)
     * @param frequency to centre
     */
    public void zoomTo(int zoom, long frequency)
    {
        mExecutor.submit(() -> {
            TunerSpectralDisplay display;

            synchronized(MainSpectralDisplay.this)
            {
                display = mDisplay;
            }

            if(display != null)
            {
                Platform.runLater(() -> {
                    SpectralDisplayModel model = display.getSpectralDisplay().getModel();
                    model.resetZoom();
                    model.setZoom(zoom, frequency, 0.5);
                });
            }
        });
    }

    /**
     * Stops showing the selected tuner.  Can be called from any thread and doesn't block.
     */
    public void clearTuner()
    {
        showTuner(null);
    }

    /**
     * Indicates if a tuner can be shown automatically: false after the user chose no tuner, until they choose one.
     * Moves the value stored by earlier versions in the SDRTrunk.properties file to the setting, once.  Any thread.
     */
    public static boolean isTunerEnabled()
    {
        String stored = Setting.MAIN_WINDOW_SPECTRAL_TUNER_ENABLED.get(null);

        if(stored != null)
        {
            return Boolean.parseBoolean(stored);
        }

        boolean enabled = SystemProperties.getInstance().get(LEGACY_TUNER_ENABLED_KEY, true);
        setTunerEnabled(enabled);
        SystemProperties.getInstance().remove(LEGACY_TUNER_ENABLED_KEY);
        return enabled;
    }

    /**
     * Sets whether a tuner can be shown automatically.  Any thread.
     */
    public static void setTunerEnabled(boolean enabled)
    {
        Setting.MAIN_WINDOW_SPECTRAL_TUNER_ENABLED.putBoolean(enabled);
    }

    /**
     * Selected tuner, or null.  The tuner is kept while the display is hidden.
     */
    public synchronized Tuner getTuner()
    {
        return mTuner;
    }

    /**
     * Disposes the display and stops the display thread.  Blocks until the display has disconnected from the tuner.
     */
    public void dispose()
    {
        TunerSpectralDisplay display;

        synchronized(this)
        {
            mDisposed = true;
            display = mDisplay;
            mDisplay = null;
            mTuner = null;
        }

        mExecutor.shutdownNow();

        if(display != null)
        {
            dispose(display);
        }
    }

    private static void dispose(TunerSpectralDisplay display)
    {
        try
        {
            display.dispose();
        }
        catch(Exception e)
        {
            mLog.error("Error disposing the spectral display", e);
        }
    }
}

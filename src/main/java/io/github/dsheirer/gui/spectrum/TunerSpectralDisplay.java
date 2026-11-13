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

import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.gui.spectrum.layer.ChannelLayer;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spectral display for a tuner: the spectral display with the decibel, frequency and playlist channel layers, the DFT
 * processing for the tuner's sample stream, and the collapsible configuration panel with a tuner menu.
 *
 * Construct on the JavaFX application thread.  showTuner() and clearTuner() can be called from any thread, but they
 * can block while the tuner starts or stops its sample stream, so don't call them on the JavaFX application thread.
 */
public class TunerSpectralDisplay extends DftSpectralDisplay implements ISourceEventProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TunerSpectralDisplay.class);

    /**
     * Indicates if the main window spectral display shows a tuner.  Cleared when the user turns the display off.
     */
    public static final String SPECTRAL_DISPLAY_ENABLED = "spectral.display.enabled";
    public static final String SETTINGS_PREFIX = "spectral.display";
    private static final String CHANNEL_DISPLAY = "channel.display";
    private static final String CHANNEL_HOVER_DETAILS = "channel.hover.details";
    private static final String MESSAGE_NO_TUNER = "No tuner selected - choose a tuner in the display settings";

    private final ChannelLayer mChannelLayer;
    private final DiscoveredTunerModel mDiscoveredTunerModel;
    private final Object mTunerLock = new Object();
    private final ReadOnlyObjectWrapper<Tuner> mDisplayedTuner = new ReadOnlyObjectWrapper<>();
    private Consumer<Tuner> mTunerSelectionHandler;
    private Tuner mTuner;
    private boolean mProcessing;

    /**
     * Constructs an instance.
     * @param playlistManager for the channels
     * @param discoveredTunerModel for the list of tuners
     * @param saveSettings true to save setting changes, false to load the saved settings but not change them
     */
    public TunerSpectralDisplay(PlaylistManager playlistManager, DiscoveredTunerModel discoveredTunerModel,
                                boolean saveSettings)
    {
        this(playlistManager != null ? playlistManager.getChannelModel() : null,
                playlistManager != null ? playlistManager.getChannelProcessingManager() : null,
                discoveredTunerModel, saveSettings);
    }

    /**
     * Constructs an instance.
     * @param channelModel for the channels, or null
     * @param channelProcessingManager for channel processing events, or null
     * @param discoveredTunerModel for the list of tuners, or null
     * @param saveSettings true to save setting changes, false to load the saved settings but not change them
     */
    public TunerSpectralDisplay(ChannelModel channelModel, ChannelProcessingManager channelProcessingManager,
                                DiscoveredTunerModel discoveredTunerModel, boolean saveSettings)
    {
        this(new ChannelLayer(channelModel, channelProcessingManager), discoveredTunerModel, saveSettings);
    }

    private TunerSpectralDisplay(ChannelLayer channelLayer, DiscoveredTunerModel discoveredTunerModel,
                                 boolean saveSettings)
    {
        super(SETTINGS_PREFIX, saveSettings, channelLayer);
        mChannelLayer = channelLayer;
        mDiscoveredTunerModel = discoveredTunerModel;
        getSpectralDisplay().messageProperty().set(MESSAGE_NO_TUNER);

        SystemProperties properties = SystemProperties.getInstance();
        mChannelLayer.channelDisplayProperty().set(parse(ChannelLayer.ChannelDisplay.class,
                properties.get(key(CHANNEL_DISPLAY), ChannelLayer.ChannelDisplay.ALL.name()),
                ChannelLayer.ChannelDisplay.ALL));
        mChannelLayer.hoverDetailsProperty().set(properties.get(key(CHANNEL_HOVER_DETAILS), true));
        mChannelLayer.channelDisplayProperty().addListener((ob, ov, nv) -> save(CHANNEL_DISPLAY, nv.name()));
        mChannelLayer.hoverDetailsProperty().addListener((ob, ov, nv) -> save(CHANNEL_HOVER_DETAILS, nv));

        installConfigPanel();
    }

    /**
     * Tuner shown in the display, updated on the JavaFX application thread.
     */
    public ReadOnlyObjectProperty<Tuner> displayedTunerProperty()
    {
        return mDisplayedTuner.getReadOnlyProperty();
    }

    /**
     * Tuners that can be shown.
     */
    public List<Tuner> getAvailableTuners()
    {
        List<Tuner> tuners = new ArrayList<>();

        if(mDiscoveredTunerModel != null)
        {
            for(DiscoveredTuner discoveredTuner: mDiscoveredTunerModel.getAvailableTuners())
            {
                if(discoveredTuner.hasTuner())
                {
                    tuners.add(discoveredTuner.getTuner());
                }
            }
        }

        return tuners;
    }

    /**
     * Sets the handler for tuner selections in the configuration panel.  The default handler shows the selected tuner.
     * @param handler receives the selected tuner, or null when the user selects no tuner.  Called on the JavaFX thread.
     */
    public void setTunerSelectionHandler(Consumer<Tuner> handler)
    {
        mTunerSelectionHandler = handler;
    }

    /**
     * Called by the configuration panel on the JavaFX application thread when the user selects a tuner, or null.
     */
    void tunerSelected(Tuner tuner)
    {
        if(mTunerSelectionHandler != null)
        {
            mTunerSelectionHandler.accept(tuner);
        }
        else
        {
            ThreadPool.CACHED.submit(() -> {
                if(tuner != null)
                {
                    showTuner(tuner);
                }
                else
                {
                    clearTuner();
                }
            });
        }
    }

    /**
     * Currently displayed tuner, or null.
     */
    public Tuner getTuner()
    {
        synchronized(mTunerLock)
        {
            return mTuner;
        }
    }

    /**
     * Shows the tuner in the display and stops showing the previous tuner.
     */
    public void showTuner(Tuner tuner)
    {
        synchronized(mTunerLock)
        {
            if(tuner == mTuner)
            {
                return;
            }

            clearTuner();

            if(tuner == null)
            {
                return;
            }

            mTuner = tuner;
            tuner.getTunerController().addListener(this);
            long frequency = tuner.getTunerController().getFrequency();
            int sampleRate = (int)tuner.getTunerController().getSampleRate();
            double sampleSize = tuner.getSampleSize();

            runOnFxThread(() -> {
                if(getTuner() == tuner)
                {
                    SpectralDisplayModel model = getSpectralDisplay().getModel();
                    model.sampleSizeProperty().set(sampleSize);
                    model.centerFrequencyProperty().set(frequency);
                    model.sampleRateProperty().set(sampleRate);
                    getSpectralDisplay().messageProperty().set(null);
                    mDisplayedTuner.set(tuner);
                }
            });

            updateProcessing();
        }
    }

    /**
     * Stops showing the current tuner.
     */
    public void clearTuner()
    {
        synchronized(mTunerLock)
        {
            Tuner tuner = mTuner;

            if(tuner != null)
            {
                stopProcessing();
                tuner.getTunerController().removeListener(this);
                mTuner = null;
            }

            getSpectralDisplay().clear();

            runOnFxThread(() -> {
                if(getTuner() == null)
                {
                    SpectralDisplayModel model = getSpectralDisplay().getModel();
                    model.sampleRateProperty().set(0);
                    model.centerFrequencyProperty().set(0);
                    model.resetZoom();
                    getSpectralDisplay().messageProperty().set(MESSAGE_NO_TUNER);
                    mDisplayedTuner.set(null);
                }
            });
        }
    }

    @Override
    protected void processingEnabledChanged()
    {
        ThreadPool.CACHED.submit(() -> {
            synchronized(mTunerLock)
            {
                updateProcessing();
            }
        });
    }

    /**
     * Starts or stops processing the tuner sample stream.  Hold the tuner lock when calling.
     */
    private void updateProcessing()
    {
        if(mTuner != null && isProcessingEnabled())
        {
            startProcessing();
        }
        else
        {
            stopProcessing();
        }
    }

    private void startProcessing()
    {
        if(!mProcessing && mTuner != null)
        {
            mProcessing = true;
            startDft();

            try
            {
                mTuner.getTunerController().addBufferListener(getDftProcessor());
            }
            catch(Exception e)
            {
                LOGGER.error("Error registering spectral display for tuner sample buffers", e);
            }
        }
    }

    private void stopProcessing()
    {
        if(mProcessing)
        {
            mProcessing = false;

            if(mTuner != null)
            {
                mTuner.getTunerController().removeBufferListener(getDftProcessor());
            }

            stopDft();
        }
    }

    /**
     * Receives frequency and sample rate changes from the tuner.
     */
    @Override
    public void process(SourceEvent event)
    {
        switch(event.getEvent())
        {
            case NOTIFICATION_FREQUENCY_CHANGE ->
            {
                long frequency = event.getValue().longValue();
                runOnFxThread(() -> getSpectralDisplay().getModel().centerFrequencyProperty().set(frequency));
            }
            case NOTIFICATION_SAMPLE_RATE_CHANGE ->
            {
                int sampleRate = event.getValue().intValue();
                runOnFxThread(() -> getSpectralDisplay().getModel().sampleRateProperty().set(sampleRate));
            }
            default ->
            {
            }
        }
    }

    /**
     * Stops showing the tuner and releases resources.  Call from a thread other than the JavaFX application thread
     * when a tuner is showing, since stopping the sample stream can block.
     */
    @Override
    public void dispose()
    {
        clearTuner();
        super.dispose();
    }
}

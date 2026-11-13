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

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.gui.spectrum.layer.ChannelTuningLayer;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

/**
 * Spectral display for a single decoding channel's sample stream, centered on the channel frequency.  The channel
 * tuning layer marks the channel passband and the decoder's estimated carrier position (PLL).
 *
 * Construct on the JavaFX application thread.  The other methods can be called from any thread.  Feed the channel's
 * complex sample buffers to getSampleListener() and turn processing on with setActive(true) while the display is
 * showing.
 */
public class ChannelSpectralDisplay extends DftSpectralDisplay
{
    public static final String SETTINGS_PREFIX = "channel.spectral.display";
    public static final double DEFAULT_SAMPLE_SIZE = 18.0;
    public static final double MINIMUM_SAMPLE_SIZE = 8.0;
    public static final double MAXIMUM_SAMPLE_SIZE = 36.0;
    private static final String NOISE_FLOOR = "noise.floor";
    private static final String MESSAGE_NO_CHANNEL = "Select a decoding channel to view its spectrum";

    private final ChannelTuningLayer mTuningLayer;
    private final EdgeSlider mNoiseFloorSlider;
    private final Object mLock = new Object();
    private boolean mActive;
    private boolean mHasChannel;

    /**
     * Constructs an instance.
     */
    public ChannelSpectralDisplay()
    {
        this(new ChannelTuningLayer());
    }

    private ChannelSpectralDisplay(ChannelTuningLayer tuningLayer)
    {
        super(SETTINGS_PREFIX, true, tuningLayer);
        mTuningLayer = tuningLayer;
        SpectralDisplayModel model = getSpectralDisplay().getModel();
        getSpectralDisplay().messageProperty().set(MESSAGE_NO_CHANNEL);

        //Noise floor slider along the right edge, below the configuration panel tab
        mNoiseFloorSlider = new EdgeSlider("Floor", "Noise floor: moves the bottom of the spectrum's dB range.  " +
                "Higher settings show a deeper range and move the noise floor up the display.",
                MINIMUM_SAMPLE_SIZE, MAXIMUM_SAMPLE_SIZE, 1, this::formatPlotBottom);
        double sampleSize = SystemProperties.getInstance().get(key(NOISE_FLOOR), (int)DEFAULT_SAMPLE_SIZE);
        sampleSize = Math.max(MINIMUM_SAMPLE_SIZE, Math.min(MAXIMUM_SAMPLE_SIZE, sampleSize));
        model.sampleSizeProperty().set(sampleSize);
        mNoiseFloorSlider.valueProperty().set(sampleSize);
        mNoiseFloorSlider.valueProperty().addListener((ob, ov, nv) -> {
            int value = (int)Math.round(nv.doubleValue());
            model.sampleSizeProperty().set(value);
            save(NOISE_FLOOR, value);
        });
        //The value shows the plot bottom in dBFS, which also depends on the DFT size and window
        model.binCountProperty().addListener((ob, ov, nv) -> mNoiseFloorSlider.refreshLabel());
        model.noiseBandwidthProperty().addListener((ob, ov, nv) -> mNoiseFloorSlider.refreshLabel());
        mNoiseFloorSlider.refreshLabel();
        StackPane.setAlignment(mNoiseFloorSlider, Pos.CENTER_RIGHT);
        StackPane.setMargin(mNoiseFloorSlider, new Insets(44, 0, 8, 0));
        getChildren().add(mNoiseFloorSlider);

        installConfigPanel();
    }

    /**
     * Listener for the channel's complex sample buffers.
     */
    public Listener<INativeBuffer> getSampleListener()
    {
        return getDftProcessor();
    }

    /**
     * Shows a channel.
     * @param frequency of the channel center in Hertz
     * @param sampleRate of the channel sample stream in Hertz
     * @param bandwidth of the channel in Hertz, or zero to hide the passband markers
     */
    public void setChannel(long frequency, int sampleRate, int bandwidth)
    {
        synchronized(mLock)
        {
            mHasChannel = sampleRate > 0;
            updateProcessing();
        }

        getSpectralDisplay().clear();
        runOnFxThread(() -> {
            SpectralDisplayModel model = getSpectralDisplay().getModel();
            model.resetZoom();
            model.centerFrequencyProperty().set(frequency);
            model.sampleRateProperty().set(sampleRate);
            mTuningLayer.channelBandwidthProperty().set(bandwidth);
            mTuningLayer.carrierOffsetValidProperty().set(false);
            mTuningLayer.carrierOffsetProperty().set(0);
            getSpectralDisplay().messageProperty().set(sampleRate > 0 ? null : MESSAGE_NO_CHANNEL);
        });
    }

    /**
     * Stops showing the channel.
     */
    public void clearChannel()
    {
        setChannel(0, 0, 0);
    }

    /**
     * Sets the estimated carrier offset from the channel center, from the decoder's frequency correction.
     * @param offset in Hertz: where the signal is relative to the channel center
     */
    public void setCarrierOffset(long offset)
    {
        runOnFxThread(() -> {
            mTuningLayer.carrierOffsetProperty().set(offset);
            mTuningLayer.carrierOffsetValidProperty().set(true);
        });
    }

    /**
     * Noise floor slider along the right edge of the display.
     */
    public EdgeSlider getNoiseFloorSlider()
    {
        return mNoiseFloorSlider;
    }

    /**
     * Formats the bottom of the spectrum plot in dBFS for the noise floor slider value.
     */
    private String formatPlotBottom(double sampleSize)
    {
        double range = (20.0 * Math.log10(Math.pow(2.0, sampleSize - 1.0))) +
                getSpectralDisplay().getModel().getProcessingGain();
        return Math.round(-range) + " dB";
    }

    /**
     * Turns DFT processing on while the display is showing, and off when it's hidden.
     */
    public void setActive(boolean active)
    {
        synchronized(mLock)
        {
            mActive = active;
            updateProcessing();
        }
    }

    @Override
    protected void processingEnabledChanged()
    {
        synchronized(mLock)
        {
            updateProcessing();
        }
    }

    /**
     * Starts or stops the DFT processor.  Hold the lock when calling.
     */
    private void updateProcessing()
    {
        if(mActive && mHasChannel && isProcessingEnabled())
        {
            startDft();
        }
        else
        {
            stopDft();
        }
    }

    @Override
    public void dispose()
    {
        setActive(false);
        super.dispose();
    }
}

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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;

/**
 * Frequency and zoom state shared by the spectrum, the waterfall and the display layers, with the conversions
 * between frequency, DFT bin and screen position.  Access this model only on the JavaFX application thread.
 *
 * DFT bins are in display order (after the half swap) so bin 0 is the lowest frequency and bin (size / 2) is the
 * center frequency.  The center of bin i is at minimum frequency + (i * bin width).  The display shows the bins
 * [zoom offset, zoom offset + visible bin count) and each visible bin covers an equal share of the screen width.
 */
public class SpectralDisplayModel
{
    public static final int MIN_ZOOM = 0;
    public static final int MAX_ZOOM = 6;

    private final LongProperty mCenterFrequency = new SimpleLongProperty(0);
    private final IntegerProperty mSampleRate = new SimpleIntegerProperty(0);
    private final IntegerProperty mBinCount = new SimpleIntegerProperty(4096);
    private final IntegerProperty mZoom = new SimpleIntegerProperty(MIN_ZOOM);
    private final DoubleProperty mZoomOffset = new SimpleDoubleProperty(0);
    private final DoubleProperty mSampleSize = new SimpleDoubleProperty(16.0);
    private final DoubleProperty mNoiseBandwidth = new SimpleDoubleProperty(0);

    /**
     * Constructs an instance.
     */
    public SpectralDisplayModel()
    {
    }

    /**
     * Center frequency of the source in Hertz.
     */
    public LongProperty centerFrequencyProperty()
    {
        return mCenterFrequency;
    }

    public long getCenterFrequency()
    {
        return mCenterFrequency.get();
    }

    /**
     * Sample rate (bandwidth) of the source in Hertz.
     */
    public IntegerProperty sampleRateProperty()
    {
        return mSampleRate;
    }

    public int getSampleRate()
    {
        return mSampleRate.get();
    }

    /**
     * Number of DFT bins in each result set.
     */
    public IntegerProperty binCountProperty()
    {
        return mBinCount;
    }

    public int getBinCount()
    {
        return mBinCount.get();
    }

    /**
     * Sets the bin count and keeps the same frequency at the left edge of the zoom window.
     */
    public void setBinCount(int binCount)
    {
        if(binCount > 0 && binCount != getBinCount())
        {
            double ratio = getZoomOffset() / (double)getBinCount();
            mBinCount.set(binCount);
            setZoomOffset(ratio * binCount);
        }
    }

    /**
     * Zoom level where the zoom multiplier is 2 ^ zoom.
     */
    public IntegerProperty zoomProperty()
    {
        return mZoom;
    }

    public int getZoom()
    {
        return mZoom.get();
    }

    public int getZoomMultiplier()
    {
        return 1 << getZoom();
    }

    /**
     * Index of the first DFT bin shown at the left edge of the display.
     */
    public DoubleProperty zoomOffsetProperty()
    {
        return mZoomOffset;
    }

    public double getZoomOffset()
    {
        return mZoomOffset.get();
    }

    /**
     * Sets the zoom window offset, limited so the zoom window stays inside the DFT bins.
     */
    public void setZoomOffset(double offset)
    {
        double max = getBinCount() - getVisibleBinCount();
        mZoomOffset.set(Math.max(0, Math.min(max, offset)));
    }

    /**
     * Source sample size in bits, which sets the dB range of the spectrum plot.
     */
    public DoubleProperty sampleSizeProperty()
    {
        return mSampleSize;
    }

    /**
     * Equivalent noise bandwidth of the DFT window, in bins, for dBFS data.  When set, the spectrum plot extends
     * below the sample dynamic range by the DFT processing gain, since the noise in each bin is that much lower than
     * the total noise.  Zero (the default) for data that doesn't include the processing gain.
     */
    public DoubleProperty noiseBandwidthProperty()
    {
        return mNoiseBandwidth;
    }

    /**
     * DFT processing gain in dB: 10 * log10(bin count / window noise bandwidth), or zero when the noise bandwidth is
     * not set.
     */
    public double getProcessingGain()
    {
        double noiseBandwidth = mNoiseBandwidth.get();
        return noiseBandwidth > 0 ? 10.0 * Math.log10(getBinCount() / noiseBandwidth) : 0.0;
    }

    /**
     * Range of the spectrum plot in dB below full scale.  The bottom of the plot is at minus this value.  The range is
     * the dynamic range of the source sample size plus the DFT processing gain.
     */
    public double getDynamicRange()
    {
        return 20.0 * Math.log10(Math.pow(2.0, mSampleSize.get() - 1.0)) + getProcessingGain();
    }

    /**
     * Indicates if a source frequency and sample rate are set.
     */
    public boolean hasSource()
    {
        return getSampleRate() > 0;
    }

    /**
     * Number of DFT bins in the zoom window.
     */
    public int getVisibleBinCount()
    {
        return Math.max(1, getBinCount() / getZoomMultiplier());
    }

    /**
     * Width of each DFT bin in Hertz.
     */
    public double getBinWidth()
    {
        return (double)getSampleRate() / (double)getBinCount();
    }

    /**
     * Lowest frequency of the source.
     */
    public long getMinFrequency()
    {
        return getCenterFrequency() - (getSampleRate() / 2);
    }

    /**
     * Highest frequency of the source.
     */
    public long getMaxFrequency()
    {
        return getCenterFrequency() + (getSampleRate() / 2);
    }

    /**
     * Frequency at the left edge of the display.
     */
    public double getDisplayMinFrequency()
    {
        return getMinFrequency() + ((getZoomOffset() - 0.5) * getBinWidth());
    }

    /**
     * Frequency at the right edge of the display.
     */
    public double getDisplayMaxFrequency()
    {
        return getDisplayMinFrequency() + getDisplayBandwidth();
    }

    /**
     * Bandwidth shown across the display, in Hertz.
     */
    public double getDisplayBandwidth()
    {
        return getVisibleBinCount() * getBinWidth();
    }

    /**
     * Screen x position of the frequency.
     * @param frequency in Hertz
     * @param width of the display in pixels
     */
    public double getX(double frequency, double width)
    {
        double bandwidth = getDisplayBandwidth();

        if(bandwidth <= 0)
        {
            return 0;
        }

        return (frequency - getDisplayMinFrequency()) / bandwidth * width;
    }

    /**
     * Frequency at the screen x position.
     * @param x position in pixels
     * @param width of the display in pixels
     */
    public long getFrequency(double x, double width)
    {
        if(width <= 0)
        {
            return getCenterFrequency();
        }

        return Math.round(getDisplayMinFrequency() + (x / width * getDisplayBandwidth()));
    }

    /**
     * Pixel width of one DFT bin at the current zoom.
     */
    public double getPixelsPerBin(double width)
    {
        return width / getVisibleBinCount();
    }

    /**
     * Changes the zoom level and keeps the frequency at the same screen position.
     * @param zoom level, limited to MIN_ZOOM - MAX_ZOOM
     * @param frequency to hold in place
     * @param xFraction screen position of the frequency as a fraction (0 - 1.0) of the display width
     */
    public void setZoom(int zoom, long frequency, double xFraction)
    {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));

        if(zoom != getZoom())
        {
            double bin = hasSource() ? ((frequency - getMinFrequency()) / getBinWidth()) + 0.5 : getBinCount() / 2.0;
            mZoom.set(zoom);
            setZoomOffset(bin - (xFraction * getVisibleBinCount()));
        }
    }

    /**
     * Resets the zoom to the full source bandwidth.
     */
    public void resetZoom()
    {
        mZoom.set(MIN_ZOOM);
        setZoomOffset(0);
    }
}

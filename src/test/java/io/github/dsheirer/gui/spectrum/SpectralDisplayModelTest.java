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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the spectral display frequency, bin and screen position conversions.
 */
public class SpectralDisplayModelTest
{
    private static SpectralDisplayModel model(long center, int sampleRate, int bins)
    {
        SpectralDisplayModel model = new SpectralDisplayModel();
        model.centerFrequencyProperty().set(center);
        model.sampleRateProperty().set(sampleRate);
        model.setBinCount(bins);
        return model;
    }

    @Test
    void centerBinIsAtCenterFrequency()
    {
        SpectralDisplayModel model = model(100_000_000, 2_048_000, 4096);
        double width = 1024;
        //Bin 2048 (the DC bin) is centered at x = (2048 + 0.5) * pixels per bin
        double x = (2048 + 0.5) * model.getPixelsPerBin(width);
        assertEquals(100_000_000, model.getFrequency(x, width), 1);
        assertEquals(x, model.getX(100_000_000, width), 1E-6);
    }

    @Test
    void frequencyAndPositionRoundTrip()
    {
        SpectralDisplayModel model = model(851_000_000, 10_000_000, 8192);
        double width = 1500;

        for(int zoom = 0; zoom <= SpectralDisplayModel.MAX_ZOOM; zoom++)
        {
            model.setZoom(zoom, 852_000_000, 0.25);

            for(double x = 0; x <= width; x += 37.5)
            {
                long frequency = model.getFrequency(x, width);
                assertEquals(x, model.getX(frequency, width), 0.01, "zoom " + zoom);
            }
        }
    }

    @Test
    void zoomKeepsFrequencyUnderMouse()
    {
        SpectralDisplayModel model = model(460_000_000, 2_400_000, 4096);
        double width = 1200;
        double x = 900;
        long frequency = model.getFrequency(x, width);

        model.setZoom(3, frequency, x / width);

        assertEquals(8, model.getZoomMultiplier());
        assertEquals(4096 / 8, model.getVisibleBinCount());
        //Within one bin width of the original frequency
        assertEquals(frequency, model.getFrequency(x, width), model.getBinWidth());
    }

    @Test
    void zoomOffsetIsLimitedToTheBins()
    {
        SpectralDisplayModel model = model(460_000_000, 2_400_000, 4096);
        model.setZoom(2, model.getMaxFrequency(), 0.0);
        assertEquals(4096 - 1024, model.getZoomOffset(), 1E-9);
        model.setZoomOffset(-50);
        assertEquals(0, model.getZoomOffset(), 1E-9);
        model.setZoom(0, model.getCenterFrequency(), 0.5);
        assertEquals(0, model.getZoomOffset(), 1E-9);
    }

    @Test
    void zoomIsLimited()
    {
        SpectralDisplayModel model = model(460_000_000, 2_400_000, 4096);
        model.setZoom(99, model.getCenterFrequency(), 0.5);
        assertEquals(SpectralDisplayModel.MAX_ZOOM, model.getZoom());
        model.setZoom(-5, model.getCenterFrequency(), 0.5);
        assertEquals(SpectralDisplayModel.MIN_ZOOM, model.getZoom());
    }

    @Test
    void binCountChangeKeepsLeftEdgeFrequency()
    {
        SpectralDisplayModel model = model(460_000_000, 2_400_000, 4096);
        model.setZoom(2, 460_300_000, 0.0);
        double left = model.getMinFrequency() + model.getZoomOffset() * model.getBinWidth();
        model.setBinCount(16384);
        double after = model.getMinFrequency() + model.getZoomOffset() * model.getBinWidth();
        assertEquals(left, after, 1.0);
        assertTrue(model.getZoomOffset() <= model.getBinCount() - model.getVisibleBinCount());
    }

    @Test
    void dynamicRangeIncludesProcessingGain()
    {
        SpectralDisplayModel model = new SpectralDisplayModel();
        model.sampleSizeProperty().set(12.0);
        model.setBinCount(4096);
        model.noiseBandwidthProperty().set(2.0);
        //66.23 dB sample range + 10 * log10(4096 / 2) = 33.11 dB processing gain
        assertEquals(99.34, model.getDynamicRange(), 0.01);
        model.setBinCount(8192);
        assertEquals(102.35, model.getDynamicRange(), 0.01);
    }

    @Test
    void dynamicRangeFollowsSampleSize()
    {
        SpectralDisplayModel model = new SpectralDisplayModel();
        model.sampleSizeProperty().set(12.0);
        assertEquals(66.22, model.getDynamicRange(), 0.01);
    }
}

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

import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the spectrum attack/decay averaging and peak hold.
 */
public class SpectrumAveragerTest
{
    /**
     * Noise power in a DFT bin is exponentially distributed.  Averaging its dB values reads about 2.5 dB below the
     * mean power.  Averaging linear power (symmetric rates) should read the mean power.
     */
    @Test
    void linearPowerAveragingMeasuresMeanNoisePower()
    {
        Random random = new Random(3);
        int bins = 2048;
        double meanPower = 1E-6; //-60 dB
        double linearSum = 0.0;
        double decibelSum = 0.0;
        int count = 0;

        for(int frame = 0; frame < 4000; frame++)
        {
            for(int x = 0; x < bins; x++)
            {
                double power = -meanPower * Math.log(1.0 - random.nextDouble());
                double decibels = 10.0 * Math.log10(power);

                if(frame >= 200)
                {
                    linearSum += power;
                    decibelSum += decibels;
                    count++;
                }
            }
        }

        assertEquals(-60.0, 10.0 * Math.log10(linearSum / count), 0.05);
        //The bias of averaging decibel values that the linear averaging avoids
        assertEquals(-62.5, decibelSum / count, 0.1);
    }

    @Test
    void steadyInputSettlesAtTheInputLevel()
    {
        SpectrumAverager averager = new SpectrumAverager();
        averager.setAveraging(10);
        float[] frame = {-80f, -40f, -60f};
        SpectrumAverager.Frame result = null;

        for(int x = 0; x < 200; x++)
        {
            result = averager.process(frame);
        }

        assertEquals(-80f, result.average()[0], 0.01f);
        assertEquals(-40f, result.average()[1], 0.01f);
        assertEquals(-60f, result.average()[2], 0.01f);
    }

    @Test
    void signalsRiseFasterThanTheyFall()
    {
        SpectrumAverager averager = new SpectrumAverager();
        averager.setAveraging(10);
        averager.process(new float[]{-80f});

        //One frame of a strong signal rises 2.5x as far (in linear power) as one frame of it disappearing falls
        float risen = averager.process(new float[]{-20f}).average()[0];
        double risePower = SpectrumAverager.toPower(risen) - SpectrumAverager.toPower(-80f);
        assertEquals(0.25 * (SpectrumAverager.toPower(-20f) - SpectrumAverager.toPower(-80f)), risePower, 1E-6);

        float fallen = averager.process(new float[]{-80f}).average()[0];
        double fallPower = SpectrumAverager.toPower(risen) - SpectrumAverager.toPower(fallen);
        assertEquals(0.1 * (SpectrumAverager.toPower(risen) - SpectrumAverager.toPower(-80f)), fallPower, 1E-6);
    }

    @Test
    void fastestSettingFollowsTheInput()
    {
        SpectrumAverager averager = new SpectrumAverager();
        averager.setAveraging(SpectrumAverager.AVERAGING_MINIMUM);
        averager.process(new float[]{-80f});
        assertEquals(-20f, averager.process(new float[]{-20f}).average()[0], 0.001f);
        assertEquals(-90f, averager.process(new float[]{-90f}).average()[0], 0.001f);
    }

    @Test
    void peakHoldHoldsAndFalls()
    {
        SpectrumAverager averager = new SpectrumAverager();
        assertNull(averager.process(new float[]{-80f}).peak());

        averager.setPeakHold(true);
        assertEquals(-30f, averager.process(new float[]{-30f}).peak()[0], 0.001f);

        float[] peak = null;

        for(int x = 0; x < 10; x++)
        {
            peak = averager.process(new float[]{-80f}).peak();
        }

        assertEquals(-30f - (10 * SpectrumAverager.PEAK_DECAY_DB), peak[0], 0.001f);
        assertTrue(peak[0] > -80f);

        averager.resetPeak();
        assertEquals(-80f, averager.process(new float[]{-80f}).peak()[0], 0.001f);

        averager.setPeakHold(false);
        assertNull(averager.process(new float[]{-80f}).peak());
    }

    @Test
    void resolutionLabels()
    {
        assertEquals("585.9 Hz/bin", SpectralDisplayConfigPanel.formatResolution(2_400_000.0 / 4096));
        assertEquals("2.44 kHz/bin", SpectralDisplayConfigPanel.formatResolution(10_000_000.0 / 4096));
        assertEquals("73.2 Hz/bin", SpectralDisplayConfigPanel.formatResolution(2_400_000.0 / 32768));
    }
}

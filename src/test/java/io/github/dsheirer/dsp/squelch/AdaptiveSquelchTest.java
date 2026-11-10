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
package io.github.dsheirer.dsp.squelch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.source.SourceEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the adaptive squelch
 */
class AdaptiveSquelchTest
{
    private static final int SAMPLE_RATE = 25000;
    private static final float THRESHOLD_DB = -78.0f;
    private static final float NOISE = (float)Math.pow(10.0, -9.0); //-90 dB

    private static AdaptiveSquelch squelch(int sampleRate, boolean autoTrack)
    {
        AdaptiveSquelch squelch = new AdaptiveSquelch(THRESHOLD_DB, autoTrack, 6000.0);
        squelch.setSampleRate(sampleRate);
        return squelch;
    }

    private static void process(AdaptiveSquelch squelch, float power, double seconds, int sampleRate)
    {
        for(int x = 0; x < (int)(seconds * sampleRate); x++)
        {
            squelch.process(power);
        }
    }

    /**
     * Seconds of samples at the power level until the audio is disabled and until the squelch closes.
     */
    private static double[] closeTimes(float signal, int sampleRate)
    {
        AdaptiveSquelch squelch = squelch(sampleRate, false);
        process(squelch, signal, 0.5, sampleRate);
        assertTrue(squelch.isUnmuted());
        double audio = -1.0;

        for(int x = 0; x < sampleRate; x++)
        {
            squelch.process(NOISE);

            if(audio < 0.0 && !squelch.isAudioEnabled())
            {
                audio = (double)x / sampleRate;
            }

            if(squelch.isMuted())
            {
                return new double[]{audio, (double)x / sampleRate};
            }
        }

        throw new AssertionError("Squelch didn't close");
    }

    @Test
    void closeTimeIsIndependentOfSignalLevelAndSampleRate()
    {
        for(int sampleRate: new int[]{6250, 25000, 50000})
        {
            for(float signal: new float[]{1E-7f, 1E-6f, 1E-4f, 1E-2f})
            {
                double[] times = closeTimes(signal, sampleRate);
                assertEquals(0.0175, times[0], 0.0085, "Audio disable " + signal + " at " + sampleRate);
                assertEquals(0.1175, times[1], 0.0085, "Close " + signal + " at " + sampleRate);
            }
        }
    }

    @Test
    void startsMuted()
    {
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, false);
        assertTrue(squelch.isMuted());
        assertFalse(squelch.isAudioEnabled());
    }

    @Test
    void shortNoiseBurstDoesNotOpen()
    {
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, false);
        process(squelch, NOISE, 0.1, SAMPLE_RATE);
        process(squelch, 1E-4f, 0.005, SAMPLE_RATE);
        process(squelch, NOISE, 0.1, SAMPLE_RATE);
        assertTrue(squelch.isMuted());
    }

    @Test
    void hysteresisHoldsOpenBelowOpenThreshold()
    {
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, false);
        float threshold = (float)Math.pow(10.0, THRESHOLD_DB / 10.0);
        process(squelch, threshold * 1.2f, 0.1, SAMPLE_RATE);
        assertTrue(squelch.isUnmuted());
        process(squelch, threshold * 0.6f, 2.0, SAMPLE_RATE); //2 dB below open, above close
        assertTrue(squelch.isAudioEnabled());
    }

    @Test
    void fadeShorterThanHangKeepsCallOpen()
    {
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, false);
        process(squelch, 1E-5f, 0.1, SAMPLE_RATE);
        process(squelch, NOISE, 0.06, SAMPLE_RATE);
        assertFalse(squelch.isAudioEnabled());
        assertTrue(squelch.isUnmuted());
        process(squelch, 1E-5f, 0.03, SAMPLE_RATE);
        assertTrue(squelch.isAudioEnabled());
    }

    @Test
    void lowFrequencyModulationDoesNotDisableAudio()
    {
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, false);
        float threshold = (float)Math.pow(10.0, THRESHOLD_DB / 10.0);
        process(squelch, threshold * 2.0f, 0.1, SAMPLE_RATE);

        //Carrier 3 dB over the open threshold with 100% modulation at 100 Hz
        for(int x = 0; x < SAMPLE_RATE * 2; x++)
        {
            double envelope = 1.0 + Math.cos(2.0 * Math.PI * 100.0 * x / SAMPLE_RATE);
            squelch.process((float)(threshold * 2.0 / 1.5 * envelope * envelope));
            assertTrue(squelch.isAudioEnabled(), "Audio disabled at sample " + x);
        }
    }

    /**
     * Sample powers with the exponential distribution of complex gaussian noise.
     */
    private static void noise(AdaptiveSquelch squelch, float power, double seconds, int sampleRate, Random random)
    {
        for(int x = 0; x < (int)(seconds * sampleRate); x++)
        {
            squelch.process((float)(-Math.log(1.0 - random.nextDouble()) * power));
        }
    }

    /**
     * Sample powers for a steady carrier with noise 20 dB below the carrier
     */
    private static void carrier(AdaptiveSquelch squelch, float power, double seconds, int sampleRate, Random random)
    {
        double sigma = Math.sqrt(power * 0.01 / 2.0);
        double amplitude = Math.sqrt(power);

        for(int x = 0; x < (int)(seconds * sampleRate); x++)
        {
            double i = amplitude + sigma * random.nextGaussian();
            double q = sigma * random.nextGaussian();
            squelch.process((float)(i * i + q * q));
        }
    }

    @Test
    void thresholdBelowNoiseFloorDoesNotLatchOpen()
    {
        Random random = new Random(1);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        float noise = (float)Math.pow(10.0, -7.4); //-74 dB is above the -78 dB threshold

        for(int x = 0; x < SAMPLE_RATE * 30; x++)
        {
            squelch.process((float)(-Math.log(1.0 - random.nextDouble()) * noise));
            assertTrue(squelch.isMuted(), "Opened at sample " + x);
        }

        assertEquals(-68.0f, squelch.getSquelchThreshold(), 0.5f);
    }

    @Test
    void transmissionOpensOverLearnedNoiseFloor()
    {
        Random random = new Random(2);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        noise(squelch, 4E-8f, 2.0, SAMPLE_RATE, random); //-74 dB
        carrier(squelch, 4E-7f, 1.0, SAMPLE_RATE, random); //-64 dB
        assertTrue(squelch.isAudioEnabled());
        noise(squelch, 4E-8f, 0.5, SAMPLE_RATE, random);
        assertTrue(squelch.isMuted());
        assertEquals(-68.0f, squelch.getSquelchThreshold(), 0.5f);
    }

    @Test
    void userMarginAboveMinimumIsKept()
    {
        Random random = new Random(3);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        noise(squelch, 1E-9f, 2.0, SAMPLE_RATE, random); //-90 dB, with a -78 dB threshold
        assertEquals(THRESHOLD_DB, squelch.getSquelchThreshold(), 0.5f);
    }

    @Test
    void autoTrackKeepsDecibelMarginWhenNoiseFloorRises()
    {
        Random random = new Random(4);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        noise(squelch, 1E-9f, 2.0, SAMPLE_RATE, random); //-90 dB

        //Raise the noise floor 16 dB in steps that stay under the squelch threshold.
        for(int step = 1; step <= 8; step++)
        {
            noise(squelch, 1E-9f * (float)Math.pow(10.0, step * 0.2), 11.0, SAMPLE_RATE, random);
            assertTrue(squelch.isMuted(), "Opened at step " + step);
        }

        assertEquals(THRESHOLD_DB + 16.0f, squelch.getSquelchThreshold(), 0.5f);
    }

    @Test
    void steadyCarrierPresentAtStartPassesUntilTimeoutThenMutes()
    {
        Random random = new Random(5);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        carrier(squelch, 2.5E-7f, 59.0, SAMPLE_RATE, random); //-66 dB
        assertTrue(squelch.isAudioEnabled());
        carrier(squelch, 2.5E-7f, 3.0, SAMPLE_RATE, random);
        assertTrue(squelch.isMuted());
        carrier(squelch, 2.5E-7f, 30.0, SAMPLE_RATE, random);
        assertTrue(squelch.isMuted());
    }

    @Test
    void steadyCarrierAfterNoisePassesUntilTimeoutThenMutes()
    {
        Random random = new Random(6);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        noise(squelch, 4E-8f, 2.0, SAMPLE_RATE, random);
        carrier(squelch, 4E-7f, 59.0, SAMPLE_RATE, random);
        assertTrue(squelch.isAudioEnabled());
        carrier(squelch, 4E-7f, 3.0, SAMPLE_RATE, random);
        assertTrue(squelch.isMuted());
    }

    @Test
    void transmissionsAreHeardAfterCarrierTimeoutEnds()
    {
        Random random = new Random(7);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        noise(squelch, 4E-8f, 2.0, SAMPLE_RATE, random);
        carrier(squelch, 4E-7f, 63.0, SAMPLE_RATE, random);
        assertTrue(squelch.isMuted());

        noise(squelch, 4E-8f, 1.0, SAMPLE_RATE, random);
        assertEquals(-68.0f, squelch.getSquelchThreshold(), 0.5f);
        carrier(squelch, 4E-7f, 1.0, SAMPLE_RATE, random);
        assertTrue(squelch.isAudioEnabled());
    }

    @Test
    void autoTrackDisabledUsesUserThreshold()
    {
        Random random = new Random(8);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, false);
        noise(squelch, 4E-8f, 2.0, SAMPLE_RATE, random); //Noise above the threshold opens the squelch
        assertTrue(squelch.isUnmuted());
        assertEquals(THRESHOLD_DB, squelch.getSquelchThreshold(), 0.01f);
    }

    /**
     * Requests the threshold and returns the user threshold notification value.
     */
    private static double requestThreshold(AdaptiveSquelch squelch, double threshold)
    {
        List<SourceEvent> events = new ArrayList<>();
        squelch.setSourceEventListener(events::add);
        squelch.receive(SourceEvent.requestSquelchThreshold(null, threshold));
        squelch.setSourceEventListener(null);
        return events.stream().filter(event -> event.getEvent() == SourceEvent.Event.NOTIFICATION_SQUELCH_USER_THRESHOLD)
            .findFirst().orElseThrow().getValue().doubleValue();
    }

    @Test
    void thresholdChangeDuringTimedOutCarrierIsRelativeToNoiseFloor()
    {
        Random random = new Random(9);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        noise(squelch, 4E-8f, 2.0, SAMPLE_RATE, random); //-74 dB with a -68 dB threshold
        carrier(squelch, 4E-7f, 63.0, SAMPLE_RATE, random); //-64 dB with a -58 dB threshold
        assertEquals(-58.0f, squelch.getSquelchThreshold(), 0.5f);

        //One dB up from the displayed threshold saves one dB over the -68 dB threshold, not -57 dB
        assertEquals(-67.0, requestThreshold(squelch, squelch.getSquelchThreshold() + 1.0), 0.5);
        assertEquals(-57.0f, squelch.getSquelchThreshold(), 0.5f);

        //The setting applies when the carrier ends
        noise(squelch, 4E-8f, 1.0, SAMPLE_RATE, random);
        assertEquals(-67.0f, squelch.getSquelchThreshold(), 0.5f);
    }

    @Test
    void thresholdChangeBelowMinimumMarginIsLimited()
    {
        Random random = new Random(10);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        noise(squelch, 4E-8f, 2.0, SAMPLE_RATE, random); //-74 dB
        assertEquals(-68.0, requestThreshold(squelch, -72.0), 0.5);
        assertEquals(-68.0f, squelch.getSquelchThreshold(), 0.5f);
    }

    @Test
    void thresholdChangeWithAutoTrackDisabledIsUnchanged()
    {
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, false);
        assertEquals(-72.0, requestThreshold(squelch, -72.0), 0.01);
        assertEquals(-72.0f, squelch.getSquelchThreshold(), 0.01f);
    }

    /**
     * Sample powers for a carrier with noise 20 dB below the carrier and bursts of low frequency voice-like modulation.
     */
    private static void voiceCarrier(AdaptiveSquelch squelch, float power, double seconds, int sampleRate, Random random)
    {
        double sigma = Math.sqrt(power * 0.01 / 2.0);
        double amplitude = Math.sqrt(power);

        for(int x = 0; x < (int)(seconds * sampleRate); x++)
        {
            double time = (double)x / sampleRate;
            double modulation = 0.3 * Math.cos(2.0 * Math.PI * 500.0 * time) + 0.2 * Math.cos(2.0 * Math.PI * 800.0 * time);
            double i = amplitude * (1.0 + modulation) + sigma * random.nextGaussian();
            double q = sigma * random.nextGaussian();
            squelch.process((float)(i * i + q * q));
        }
    }

    @Test
    void carrierWithVoiceModulationDoesNotTimeOut()
    {
        Random random = new Random(11);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        noise(squelch, 4E-8f, 2.0, SAMPLE_RATE, random);
        voiceCarrier(squelch, 4E-7f, 190.0, SAMPLE_RATE, random);
        assertTrue(squelch.isAudioEnabled());
        assertEquals(-68.0f, squelch.getSquelchThreshold(), 0.5f);
    }

    @Test
    void voiceOnTimedOutCarrierReopensSquelch()
    {
        Random random = new Random(12);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        noise(squelch, 4E-8f, 2.0, SAMPLE_RATE, random);
        carrier(squelch, 4E-7f, 63.0, SAMPLE_RATE, random);
        assertTrue(squelch.isMuted());
        voiceCarrier(squelch, 4E-7f, 4.0, SAMPLE_RATE, random);
        assertEquals(-68.0f, squelch.getSquelchThreshold(), 0.5f);
        assertTrue(squelch.isAudioEnabled());
    }

    @Test
    void voiceOnCarrierPresentAtStartReopensSquelch()
    {
        Random random = new Random(13);
        AdaptiveSquelch squelch = squelch(SAMPLE_RATE, true);
        carrier(squelch, 4E-7f, 63.0, SAMPLE_RATE, random);
        assertTrue(squelch.isMuted());
        voiceCarrier(squelch, 4E-7f, 4.0, SAMPLE_RATE, random);
        assertTrue(squelch.isAudioEnabled());
    }
}

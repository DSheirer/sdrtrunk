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
package io.github.dsheirer.module.decode.am;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.module.decode.analog.DecodeConfigAnalog.Bandwidth;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the channel sample rates supported by the AM decoder.
 */
class AMDecoderSampleRateTest
{
    private static final int BUFFER = 2048;

    private static AMDecoder decoder(Bandwidth bandwidth, double sampleRate)
    {
        DecodeConfigAM config = new DecodeConfigAM();
        config.setBandwidth(bandwidth);
        AMDecoder decoder = new AMDecoder(config);
        decoder.getSourceEventListener().receive(SourceEvent.sampleRateChange(sampleRate));
        return decoder;
    }

    @Test
    void defaultBandwidthIsSixKilohertz()
    {
        assertEquals(Bandwidth.BW_6_0, new DecodeConfigAM().getBandwidth());
        assertTrue(Bandwidth.BW_6_0.isAM());
    }

    @Test
    void minimumChannelSampleRateIsAtLeastTwiceTheBandwidth()
    {
        for(Bandwidth bandwidth: Bandwidth.AM_BANDWIDTHS)
        {
            DecodeConfigAM config = new DecodeConfigAM();
            config.setBandwidth(bandwidth);
            assertTrue(config.getChannelSpecification().getMinimumSampleRate() >= 2.0 * bandwidth.getValue(), bandwidth.name());
        }
    }

    /**
     * Follows the heterodyne channel source decimation selection for tuner sample rates, including 1.8 and 3.6 MHz which
     * previously produced an unsupported 28,125 Hz sample rate for the default 15 kHz bandwidth.
     */
    @Test
    void heterodyneChannelRatesAreSupported()
    {
        double[] tunerRates = {1.8E6, 2.0E6, 2.4E6, 2.56E6, 3.0E6, 3.2E6, 3.6E6, 6.0E6, 7.2E6, 8.0E6, 10.0E6, 20.0E6};

        for(Bandwidth bandwidth: Bandwidth.AM_BANDWIDTHS)
        {
            DecodeConfigAM config = new DecodeConfigAM();
            config.setBandwidth(bandwidth);
            ChannelSpecification specification = config.getChannelSpecification();

            for(double tunerRate: tunerRates)
            {
                int decimation = DecimationFilterFactory.getDecimationRate((int)(tunerRate / specification.getMinimumSampleRate()));
                decoder(bandwidth, tunerRate / decimation);
            }
        }
    }

    /**
     * Polyphase channel rates don't use the channel specification minimum rate.
     */
    @Test
    void ratesBelowTwiceTheBandwidthDecodeAudio()
    {
        for(double sampleRate: new double[]{25000.0, 28125.0})
        {
            AMDecoder decoder = decoder(Bandwidth.BW_15_0, sampleRate);
            List<float[]> audio = new ArrayList<>();
            decoder.setBufferListener(audio::add);
            int sample = 0;

            for(int b = 0; b < (int)(sampleRate * 2.0 / BUFFER); b++)
            {
                float[] i = new float[BUFFER];
                float[] q = new float[BUFFER];

                for(int x = 0; x < BUFFER; x++)
                {
                    i[x] = 0.01f * (1.0f + 0.8f * (float)Math.cos(2.0 * Math.PI * 1000.0 * sample++ / sampleRate));
                }

                decoder.receive(new ComplexSamples(i, q, 0L));
            }

            float peak = 0.0f;

            for(float[] buffer: audio.subList(audio.size() / 2, audio.size()))
            {
                for(float value: buffer)
                {
                    peak = Math.max(peak, Math.abs(value));
                }
            }

            assertEquals(0.6f, peak, 0.05f, "Peak at sample rate " + sampleRate);
        }
    }

    @Test
    void rateBelowTheBandwidthIsRejected()
    {
        assertThrows(IllegalStateException.class, () -> decoder(Bandwidth.BW_25_0, 20000.0));
    }
}

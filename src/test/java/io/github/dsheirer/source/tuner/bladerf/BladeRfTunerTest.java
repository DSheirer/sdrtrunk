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
package io.github.dsheirer.source.tuner.bladerf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dsheirer.bladerf.GainMode;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the parts of the bladeRF tuner that don't require a device.
 */
class BladeRfTunerTest
{
    private static final float SCALE = 2048.0f;

    @Test
    void lookupFindsEachUsbProduct()
    {
        assertEquals(TunerClass.BLADERF, TunerClass.lookup((short)0x2CF0, (short)0x5250));
        assertEquals(TunerClass.BLADERF, TunerClass.lookup((short)0x2CF0, (short)0x5246));
        assertEquals(TunerClass.BLADERF, TunerClass.lookup((short)0x1D50, (short)0x6066));
    }

    @Test
    void scalesSamples()
    {
        short[] samples = {2047, -2048, 1024, -1024};

        assertArrayEquals(new float[]{2047 / SCALE, -1.0f, 0.5f, -0.5f}, BladeRfTunerController.scale(samples));
    }

    @Test
    void sampleRatesAreLimitedByTheDevice()
    {
        List<BladeRfSampleRate> rates = BladeRfSampleRate.getRates(4_000_000);

        assertEquals(List.of(BladeRfSampleRate.RATE_1_25, BladeRfSampleRate.RATE_2_5), rates);
        assertEquals(BladeRfSampleRate.RATE_2_5, BladeRfSampleRate.getClosest(rates, BladeRfSampleRate.DEFAULT.getRate()));
        assertEquals(BladeRfSampleRate.RATE_1_25, BladeRfSampleRate.getClosest(rates, 1));
    }

    @Test
    void sampleRatesDivideIntoPolyphaseChannels()
    {
        for(BladeRfSampleRate rate : BladeRfSampleRate.values())
        {
            assertEquals(0, rate.getRate() % 50_000, rate.toString());
        }
    }

    @Test
    void configurationSurvivesSerialization() throws Exception
    {
        BladeRfTunerConfiguration configuration = new BladeRfTunerConfiguration("test");
        configuration.setSampleRate(BladeRfSampleRate.RATE_5_0.getRate());
        configuration.setGainMode(GainMode.SLOW_ATTACK_AGC);
        configuration.setGain(12);
        configuration.setBiasT(true);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = mapper.writeValueAsString(configuration);
        TunerConfiguration restored = mapper.readValue(json, TunerConfiguration.class);

        BladeRfTunerConfiguration bladeRf = assertInstanceOf(BladeRfTunerConfiguration.class, restored);
        assertEquals(TunerType.BLADERF, bladeRf.getTunerType());
        assertEquals(BladeRfSampleRate.RATE_5_0.getRate(), bladeRf.getSampleRate());
        assertEquals(GainMode.SLOW_ATTACK_AGC, bladeRf.getGainMode());
        assertEquals(12, bladeRf.getGain());
        assertEquals(true, bladeRf.isBiasT());
        assertEquals("test", bladeRf.getUniqueID());
    }
}

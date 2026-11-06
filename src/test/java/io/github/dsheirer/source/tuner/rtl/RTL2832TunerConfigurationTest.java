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
package io.github.dsheirer.source.tuner.rtl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc0012.FC0012TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc0013.FC0013TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc2580.FC2580TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r8x.r820t.R820TTunerConfiguration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests conversion of tuner configurations saved by the previous RTL-2832 driver and the E4000 IF gain distribution.
 */
public class RTL2832TunerConfigurationTest
{
    //Same settings as TunerConfigurationManager
    private final ObjectMapper mObjectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private RTL2832TunerConfiguration read(String json) throws Exception
    {
        return (RTL2832TunerConfiguration)mObjectMapper.readValue(json, TunerConfiguration.class);
    }

    @Test
    public void legacyR8xMasterGain() throws Exception
    {
        RTL2832TunerConfiguration config = read("{\"type\":\"r820TTunerConfiguration\",\"masterGain\":\"GAIN_296\"," +
                "\"mixerGain\":\"GAIN_105\",\"lnagain\":\"GAIN_222\",\"vgagain\":\"GAIN_210\",\"sampleRate\":\"RATE_2_048MHZ\"," +
                "\"biasT\":true,\"uniqueID\":\"RTL2832/R820T 00000001\",\"frequency\":101100000}");
        assertInstanceOf(R820TTunerConfiguration.class, config);
        assertEquals(296, config.getGain());
        assertFalse(config.isAGC());
        assertTrue(config.isBiasT());
        assertEquals(RTL2832TunerController.SampleRate.RATE_2_048MHZ, config.getSampleRate());
    }

    @Test
    public void legacyR8xManualAndAutomaticGain() throws Exception
    {
        //Stage gains before the master gain, to check that property order doesn't matter
        RTL2832TunerConfiguration manual = read("{\"type\":\"r828DTunerConfiguration\",\"lnagain\":\"GAIN_222\"," +
                "\"mixerGain\":\"GAIN_105\",\"masterGain\":\"MANUAL\",\"uniqueID\":\"x\"}");
        assertEquals(327, manual.getGain());
        assertFalse(manual.isAGC());

        RTL2832TunerConfiguration automatic = read("{\"type\":\"r820TTunerConfiguration\",\"masterGain\":\"AUTOMATIC\"," +
                "\"uniqueID\":\"x\"}");
        assertTrue(automatic.isAGC());
    }

    @Test
    public void legacyE4K() throws Exception
    {
        RTL2832TunerConfiguration manual = read("{\"type\":\"e4KTunerConfiguration\",\"masterGain\":\"MANUAL\"," +
                "\"mixerGain\":\"GAIN_12\",\"lnagain\":\"GAIN_PLUS_200\",\"ifgain\":\"SENSITIVITY_30\",\"uniqueID\":\"x\"}");
        assertEquals(320, manual.getGain());
        assertEquals(30, ((E4KTunerConfiguration)manual).getIFGainDb());

        //Legacy IF gains above the saturation limit are capped
        TunerConfiguration high = read("{\"type\":\"e4KTunerConfiguration\",\"ifgain\":\"LINEARITY_60\",\"uniqueID\":\"x\"}");
        assertEquals(RTL2832TunerController.E4K_IF_GAIN_MAXIMUM, ((E4KTunerConfiguration)high).getIFGainDb());

        RTL2832TunerConfiguration minus = read("{\"type\":\"e4KTunerConfiguration\",\"masterGain\":\"MINUS_10\"," +
                "\"uniqueID\":\"x\"}");
        assertEquals(-10, minus.getGain());
    }

    @Test
    public void legacyFC0013() throws Exception
    {
        RTL2832TunerConfiguration config = read("{\"type\":\"fc0013TunerConfiguration\",\"lnaGain\":\"G23\"," +
                "\"agc\":true,\"uniqueID\":\"x\"}");
        assertInstanceOf(FC0013TunerConfiguration.class, config);
        assertEquals(197, config.getGain());
        assertTrue(config.isAGC());

        //Legacy settings map by LNA register value: G03 and G04 are both -6.3 dB
        int[] expected = {-99, -73, -65, -63, -63, -60, -58, -54, 58};

        for(int x = 0; x < expected.length; x++)
        {
            RTL2832TunerConfiguration legacy = read("{\"type\":\"fc0013TunerConfiguration\",\"lnaGain\":\"G0" + x +
                    "\",\"uniqueID\":\"x\"}");
            assertEquals(expected[x], legacy.getGain(), "G0" + x);
        }
    }

    @Test
    public void roundTrip() throws Exception
    {
        E4KTunerConfiguration config = new E4KTunerConfiguration("RTL2832/E4000 1");
        config.setGain(340);
        config.setAGC(true);
        config.setIFGainDb(41);
        String json = mObjectMapper.writeValueAsString(config);
        E4KTunerConfiguration copy = (E4KTunerConfiguration)read(json);
        assertEquals(340, copy.getGain());
        assertTrue(copy.isAGC());
        assertEquals(41, copy.getIFGainDb());
    }

    @Test
    public void e4kIFStageGains()
    {
        for(int gain = RTL2832TunerController.E4K_IF_GAIN_MINIMUM; gain <= RTL2832TunerController.E4K_IF_GAIN_MAXIMUM; gain++)
        {
            int[] stages = RTL2832TunerController.getE4KIFStageGains(gain);
            assertEquals(gain, Arrays.stream(stages).sum(), "Total IF gain " + gain);
        }

        assertArrayEquals(new int[]{-3, 0, 0, 0, 3, 3}, RTL2832TunerController.getE4KIFStageGains(0));
        assertArrayEquals(new int[]{6, 9, 9, 2, 15, 15}, RTL2832TunerController.getE4KIFStageGains(99));
    }

    @Test
    public void fc0012AndFC2580RoundTrip() throws Exception
    {
        TunerConfiguration fc0012 = TunerFactory.getTunerConfiguration(TunerType.FITIPOWER_FC0012, "RTL-2832/FC0012 1");
        assertInstanceOf(FC0012TunerConfiguration.class, fc0012);
        assertEquals(FC0012TunerConfiguration.DEFAULT_GAIN, ((RTL2832TunerConfiguration)fc0012).getGain());
        ((RTL2832TunerConfiguration)fc0012).setGain(71);
        RTL2832TunerConfiguration fc0012Copy = read(mObjectMapper.writeValueAsString(fc0012));
        assertInstanceOf(FC0012TunerConfiguration.class, fc0012Copy);
        assertEquals(71, fc0012Copy.getGain());

        TunerConfiguration fc2580 = TunerFactory.getTunerConfiguration(TunerType.FCI_FC2580, "RTL-2832/FC2580 1");
        assertInstanceOf(FC2580TunerConfiguration.class, fc2580);
        RTL2832TunerConfiguration fc2580Copy = read(mObjectMapper.writeValueAsString(fc2580));
        assertInstanceOf(FC2580TunerConfiguration.class, fc2580Copy);
    }
}

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
package io.github.dsheirer.source.wave;

import java.io.File;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SDRSharpIQRecordingTest
{
    private static SDRSharpIQRecording parse(String name)
    {
        return SDRSharpIQRecording.parse(new File(name));
    }

    @Test
    void parsesHertzFrequency()
    {
        SDRSharpIQRecording r = parse("20261102-071525_toronto_AIRSPY_424461724Hz_10MSps.iq");
        assertNotNull(r);
        assertEquals(LocalDateTime.of(2026, 11, 2, 7, 15, 25), r.getStartTime());
        assertEquals("toronto", r.getLabel());
        assertEquals("AIRSPY", r.getTunerType());
        assertEquals(424461724L, r.getCenterFrequency());
        assertEquals(10_000_000d, r.getSampleRate());
    }

    @Test
    void parsesKilohertzAndMegahertzFrequency()
    {
        assertEquals(855_125_000L, parse("20261102-072949_toronto_AIRSPY_855125kHz_10MSps.iq").getCenterFrequency());
        assertEquals(412_000_000L, parse("20261102-071621_toronto_AIRSPY_412MHz_10MSps.iq").getCenterFrequency());
        assertEquals(860_880_000L, parse("20261102-071621_toronto_AIRSPY_860.88MHz_10MSps.iq").getCenterFrequency());
    }

    @Test
    void parsesOptionalAndUnderscoredLabels()
    {
        SDRSharpIQRecording noLabel = parse("20261102-071525_AIRSPY_424461724Hz_10MSps.iq");
        assertNotNull(noLabel);
        assertNull(noLabel.getLabel());
        assertEquals("AIRSPY", noLabel.getTunerType());

        SDRSharpIQRecording underscored = parse("20261102-071525_toronto_north_AIRSPY_424461724Hz_2.5MSps.iq");
        assertNotNull(underscored);
        assertEquals("toronto_north", underscored.getLabel());
        assertEquals("AIRSPY", underscored.getTunerType());
        assertEquals(2_500_000d, underscored.getSampleRate());
    }

    @Test
    void rejectsUnparseableNames()
    {
        assertNull(parse("recording.iq"));
        assertNull(parse("20261102-071525_toronto_AIRSPY_424461724Hz_10MSps.wav"));
        assertNull(parse("20261302-071525_toronto_AIRSPY_424461724Hz_10MSps.iq")); //Invalid month
    }

    @Test
    void formatsSampleRate()
    {
        assertEquals("10MSps", SDRSharpIQRecording.formatSampleRate(10_000_000));
        assertEquals("2.5MSps", SDRSharpIQRecording.formatSampleRate(2_500_000));
        assertEquals("2.048MSps", SDRSharpIQRecording.formatSampleRate(2_048_000));
        assertEquals("25kSps", SDRSharpIQRecording.formatSampleRate(25_000));
        assertEquals("12.5kSps", SDRSharpIQRecording.formatSampleRate(12_500));
        assertEquals("26.042kSps", SDRSharpIQRecording.formatSampleRate(26_041.666));
        assertEquals("800Sps", SDRSharpIQRecording.formatSampleRate(800));
    }

    @Test
    void createdFileNamesRoundTrip()
    {
        LocalDateTime start = LocalDateTime.of(2026, 11, 3, 14, 5, 9);

        String name = SDRSharpIQRecording.createFileName(start, null, "AIRSPY", 851_012_500L, 10_000_000);
        assertEquals("20261103-140509_AIRSPY_851012500Hz_10MSps.iq", name);

        String channel = SDRSharpIQRecording.createFileName(start, "Site 1_Control", "SDRTRUNK", 851_012_500L, 25_000);
        assertEquals("20261103-140509_Site 1_Control_SDRTRUNK_851012500Hz_25kSps.iq", channel);
        SDRSharpIQRecording parsed = parse(channel);
        assertNotNull(parsed);
        assertEquals(start, parsed.getStartTime());
        assertEquals("Site 1_Control", parsed.getLabel());
        assertEquals("SDRTRUNK", parsed.getTunerType());
        assertEquals(851_012_500L, parsed.getCenterFrequency());
        assertEquals(25_000d, parsed.getSampleRate());

        //Underscores in the tuner type would break parsing - they're replaced with hyphens
        SDRSharpIQRecording hf = parse(SDRSharpIQRecording.createFileName(start, null, "AIRSPY_HF", 7_200_000L, 768_000));
        assertNotNull(hf);
        assertEquals("AIRSPY-HF", hf.getTunerType());
        assertEquals(768_000d, hf.getSampleRate());
    }

    @Test
    void detectsExtension()
    {
        assertTrue(SDRSharpIQRecording.hasIQExtension(new File("a.IQ")));
        assertFalse(SDRSharpIQRecording.hasIQExtension(new File("a.wav")));
        assertFalse(SDRSharpIQRecording.hasIQExtension(null));
    }
}

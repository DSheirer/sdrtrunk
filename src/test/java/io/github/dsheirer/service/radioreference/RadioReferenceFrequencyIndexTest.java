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

package io.github.dsheirer.service.radioreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.SiteFrequency;
import io.github.dsheirer.service.radioreference.RadioReferenceFrequencyIndex.RankedCandidate;
import io.github.dsheirer.service.radioreference.RadioReferenceFrequencyIndex.Source;
import java.util.List;
import java.util.NavigableMap;
import org.junit.jupiter.api.Test;

/**
 * Tests the frequency index lookup and candidate building, without calling the web service
 */
public class RadioReferenceFrequencyIndexTest
{
    //Fulton, NY
    private static final double LATITUDE = 43.331042;
    private static final double LONGITUDE = -76.37563;

    private static FrequencyCandidate candidate(long hz, String title, Double latitude, Double longitude)
    {
        return new FrequencyCandidate(hz, title, null, "FMN", null, null, false, "Test", latitude, longitude, null,
            null);
    }

    @Test
    void ranksByDistanceThenFrequencyOffset()
    {
        NavigableMap<Long,List<FrequencyCandidate>> frequencies = RadioReferenceFrequencyIndex.toFrequencies(List.of(
            new Source("county:1", 0, List.of(
                candidate(154_430_000, "Syracuse 34 km", 43.0481, -76.1474),
                candidate(154_430_000, "Fulton 0 km", LATITUDE, LONGITUDE),
                candidate(154_432_500, "Oswego 15 km offset", 43.4553, -76.5105),
                candidate(154_430_000, "Unknown location", null, null)), null),
            new Source("county:2", 0, List.of(
                candidate(154_445_000, "Outside tolerance", LATITUDE, LONGITUDE)), null)));

        List<RankedCandidate> ranked = RadioReferenceFrequencyIndex.rank(frequencies, 154_430_000, 3_000,
            LATITUDE, LONGITUDE);

        assertEquals(List.of("Fulton 0 km", "Oswego 15 km offset", "Syracuse 34 km", "Unknown location"),
            ranked.stream().map(c -> c.candidate().title()).toList());
        assertEquals(0.0, ranked.get(0).distanceKilometers(), 0.01);
        assertEquals(2_500, ranked.get(1).offsetHz());
        assertFalse(ranked.get(3).hasDistance());
    }

    @Test
    void sameDistanceRanksByFrequencyOffset()
    {
        NavigableMap<Long,List<FrequencyCandidate>> frequencies = RadioReferenceFrequencyIndex.toFrequencies(List.of(
            new Source("county:1", 0, List.of(
                candidate(460_012_500, "Offset", LATITUDE, LONGITUDE),
                candidate(460_000_000, "Exact", LATITUDE, LONGITUDE)), null)));

        List<RankedCandidate> ranked = RadioReferenceFrequencyIndex.rank(frequencies, 460_000_000, 12_500,
            LATITUDE, LONGITUDE);

        assertEquals(List.of("Exact", "Offset"), ranked.stream().map(c -> c.candidate().title()).toList());
    }

    @Test
    void removesDuplicateListings()
    {
        FrequencyCandidate frequency = candidate(155_475_000, "National Law Enforcement", LATITUDE, LONGITUDE);
        NavigableMap<Long,List<FrequencyCandidate>> frequencies = RadioReferenceFrequencyIndex.toFrequencies(List.of(
            new Source("county:1", 0, List.of(frequency), null), new Source("agency:2", 0, List.of(frequency), null)));

        assertEquals(1, RadioReferenceFrequencyIndex.rank(frequencies, 155_475_000, 0, LATITUDE, LONGITUDE).size());
    }

    @Test
    void siteCandidatesWithinRadius()
    {
        Site near = site(1, "Syracuse", 43.0481, -76.1474, "c", 851.0125, "", 852.5375);
        Site far = site(2, "Buffalo", 42.8864, -78.8784, "c", 851.5125);
        RadioReferenceFrequencyIndex.BuildParameters parameters =
            new RadioReferenceFrequencyIndex.BuildParameters(LATITUDE, LONGITUDE);

        List<FrequencyCandidate> candidates = RadioReferenceFrequencyIndex.siteCandidates(List.of(near, far), 42,
            "Onondaga County Interop", "Project 25 Phase II", parameters);

        assertEquals(2, candidates.size(), "only the site within the radius");
        assertEquals(851_012_500, candidates.get(0).frequencyHz());
        assertEquals("Control", candidates.get(0).use());
        assertNull(candidates.get(1).use());
        assertEquals("Onondaga County Interop", candidates.get(0).title());
        assertEquals("Site 1 · Syracuse", candidates.get(0).source());
        assertTrue(candidates.get(0).hasLocation());
        assertEquals(42, candidates.get(0).systemId());
        assertTrue(candidates.get(0).isSiteFrequency());
    }

    @Test
    void siteRecordsWithinRadius()
    {
        Site near = site(1, "Syracuse", 43.0481, -76.1474, 851.5125, "c", 851.0125);
        near.setSiteId(1234);
        near.setNac("1A3");
        near.setRfss(1);
        near.setZoneNumber(2);
        near.setCountyId(1855);
        Site far = site(2, "Buffalo", 42.8864, -78.8784, "c", 851.5125);
        io.github.dsheirer.rrapi.type.System system = new io.github.dsheirer.rrapi.type.System();
        system.setSystemId(42);
        system.setName("Onondaga County Interop");

        List<SiteRecord> records = RadioReferenceFrequencyIndex.siteRecords(List.of(near, far), system,
            "Project 25 Phase II", "APCO-25 Common Air Interface Exclusive", java.util.Map.of(1855, "Onondaga County"),
            new RadioReferenceFrequencyIndex.BuildParameters(LATITUDE, LONGITUDE));

        assertEquals(1, records.size(), "only the site within the radius");
        SiteRecord record = records.get(0);
        assertEquals(42, record.systemId());
        assertEquals("Onondaga County Interop", record.systemName());
        assertEquals("Project 25 Phase II", record.protocol());
        assertEquals(1234, record.siteId());
        assertEquals("Syracuse", record.description());
        assertEquals("Onondaga County", record.county());
        assertEquals("1A3", record.nac());
        assertEquals(1, record.rfss());
        assertEquals(2, record.zone());
        assertEquals(List.of(851_012_500L, 851_512_500L),
            record.channels().stream().map(SiteRecord.SiteChannel::frequencyHz).toList(), "sorted by frequency");
        assertEquals("Control", record.channels().get(0).use());
    }

    @Test
    void siteFrequencyUse()
    {
        assertEquals("Control", RadioReferenceFrequencyIndex.siteFrequencyUse("c"));
        assertEquals("Control", RadioReferenceFrequencyIndex.siteFrequencyUse("d"));
        assertEquals("Alternate control", RadioReferenceFrequencyIndex.siteFrequencyUse("a"));
        assertNull(RadioReferenceFrequencyIndex.siteFrequencyUse(""));
        assertNull(RadioReferenceFrequencyIndex.siteFrequencyUse(null));
    }

    /**
     * Site with frequencies, each frequency followed by its use code when the next value is a string
     */
    private static Site site(int number, String description, double latitude, double longitude, Object... values)
    {
        Site site = new Site();
        site.setSiteNumber(number);
        site.setDescription(description);
        site.setLatitude(latitude);
        site.setLongitude(longitude);
        site.setRange(10);
        java.util.ArrayList<SiteFrequency> frequencies = new java.util.ArrayList<>();
        String use = null;

        for(Object value: values)
        {
            if(value instanceof String code)
            {
                use = code;
            }
            else
            {
                SiteFrequency frequency = new SiteFrequency();
                frequency.setFrequency((Double)value);
                frequency.setUse(use);
                frequencies.add(frequency);
                use = null;
            }
        }

        site.setSiteFrequencies(frequencies);
        return site;
    }
}

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

package io.github.dsheirer.gui.playlist.radioreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.rrapi.type.Flavor;
import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.SiteFrequency;
import io.github.dsheirer.rrapi.type.SystemInformation;
import io.github.dsheirer.rrapi.type.Type;
import io.github.dsheirer.rrapi.type.Voice;
import io.github.dsheirer.service.radioreference.FrequencyCandidate;
import io.github.dsheirer.service.radioreference.SiteRecord;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests matching radio reference frequencies and sites to playlist channels, and creating site channels, without the
 * web service
 */
public class RadioReferencePlaylistActionsTest
{
    private static SiteRecord site(String protocol, String... uses)
    {
        java.util.ArrayList<SiteRecord.SiteChannel> channels = new java.util.ArrayList<>();

        for(int index = 0; index < uses.length; index++)
        {
            channels.add(new SiteRecord.SiteChannel(851_000_000L + (index * 250_000L), index + 1,
                uses[index].isEmpty() ? null : uses[index]));
        }

        return new SiteRecord(1, "System", protocol, null, 10, 3, "Syracuse", null, 43.0, -76.0, 10, null, 0, 0,
            channels);
    }

    private static Channel channel(DecoderType decoderType, long... frequencies)
    {
        Channel channel = new Channel("Test");
        channel.setDecodeConfiguration(DecoderFactory.getDecodeConfiguration(decoderType));

        if(frequencies.length == 1)
        {
            SourceConfigTuner tuner = new SourceConfigTuner();
            tuner.setFrequency(frequencies[0]);
            channel.setSourceConfiguration(tuner);
        }
        else
        {
            SourceConfigTunerMultipleFrequency multiple = new SourceConfigTunerMultipleFrequency();

            for(long frequency: frequencies)
            {
                multiple.addFrequency(frequency);
            }

            channel.setSourceConfiguration(multiple);
        }

        return channel;
    }

    @Test
    void siteProtocols()
    {
        assertEquals(RadioReferencePlaylistActions.getProtocolFamily(DecoderType.P25_PHASE1),
            RadioReferencePlaylistActions.getSiteDecoderTypes(site("Project 25 Phase II", "Control")));
        assertTrue(RadioReferencePlaylistActions.getSiteDecoderTypes(site("DMR Tier III", "Control"))
            .contains(DecoderType.DMR));
        assertTrue(RadioReferencePlaylistActions.isSupported(site("NXDN NEXEDGE 4800", "Control")));
        assertFalse(RadioReferencePlaylistActions.isSupported(site("Motorola Type II Smartnet", "Control")),
            "Motorola control channels aren't decoded");
        assertFalse(RadioReferencePlaylistActions.isSupported(site("Project 25 Phase I", "", "")),
            "no control channel listed");
    }

    @Test
    void ltrSitesHaveRepeatersInsteadOfAControlChannel()
    {
        SiteRecord ltr = site("LTR Standard", "", "", "");
        assertTrue(RadioReferencePlaylistActions.isRepeaterSite(ltr));
        assertTrue(RadioReferencePlaylistActions.isSupported(ltr), "no control channel is needed");
        assertNull(RadioReferencePlaylistActions.getUnsupportedReason(ltr));
        assertEquals(List.of(851_000_000L, 851_250_000L, 851_500_000L),
            RadioReferencePlaylistActions.getSiteFrequencies(ltr));

        assertEquals("Motorola Type II Smartnet isn't supported",
            RadioReferencePlaylistActions.getUnsupportedReason(site("Motorola Type II Smartnet", "Control")));
        assertEquals("Radio Reference lists no control channel",
            RadioReferencePlaylistActions.getUnsupportedReason(site("Project 25 Phase I", "", "")));
    }

    @Test
    void ltrRepeaterChannels()
    {
        Type type = new Type();
        type.setTypeId(1);
        type.setName("LTR");
        Flavor standard = new Flavor();
        standard.setFlavorId(2);
        standard.setName("Standard");
        Voice analog = new Voice();
        analog.setVoiceId(3);
        analog.setName("Analog");
        RadioReferenceDecoder decoder = new RadioReferenceDecoder(null, Map.of(1, type), Map.of(2, standard),
            Map.of(3, analog), Map.of());
        io.github.dsheirer.rrapi.type.System system = new io.github.dsheirer.rrapi.type.System();
        system.setName("Adams Communications");
        system.setTypeId(1);
        system.setFlavorId(2);
        system.setVoiceId(3);
        Site site = new Site();
        site.setSiteNumber(1);
        site.setDescription("Watertown");
        SiteFrequency lcn1 = frequency(461.0125, "");
        lcn1.setLogicalChannelNumber(1);
        SiteFrequency lcn2 = frequency(461.0375, "");
        lcn2.setLogicalChannelNumber(2);
        site.setSiteFrequencies(List.of(lcn1, lcn2));

        List<Channel> channels = RadioReferenceChannelFactory.repeaters(decoder, system, new SystemInformation(), site,
            List.of(461_037_500L));

        assertEquals(1, channels.size(), "only the requested repeater");
        assertEquals(DecoderType.LTR, channels.get(0).getDecodeConfiguration().getDecoderType());
        assertEquals("LCN 2", channels.get(0).getName());
        assertEquals("Adams Communications", channels.get(0).getSystem());
        assertEquals("Watertown", channels.get(0).getSite());
        assertEquals(List.of(461_037_500L),
            RadioReferencePlaylistActions.getFrequencies(channels.get(0).getSourceConfiguration()));
    }

    @Test
    void controlFrequenciesFirst()
    {
        SiteRecord site = site("Project 25 Phase I", "", "Alternate control", "Control", "");
        assertEquals(List.of(851_500_000L, 851_250_000L), RadioReferencePlaylistActions.getControlFrequencies(site));
    }

    @Test
    void findsExistingChannelByFrequencyAndProtocol()
    {
        Channel nbfm = channel(DecoderType.NBFM, 154_430_000);
        Channel p25 = channel(DecoderType.P25_PHASE2, 851_000_000, 851_250_000);
        Channel traffic = new Channel("Traffic", Channel.ChannelType.TRAFFIC);
        traffic.setDecodeConfiguration(DecoderFactory.getDecodeConfiguration(DecoderType.NBFM));
        SourceConfigTuner tuner = new SourceConfigTuner();
        tuner.setFrequency(460_000_000);
        traffic.setSourceConfiguration(tuner);
        List<Channel> channels = List.of(nbfm, p25, traffic);

        assertSame(nbfm, RadioReferencePlaylistActions.findChannel(channels, List.of(154_430_000L),
            RadioReferencePlaylistActions.getProtocolFamily(DecoderType.NBFM)));
        assertNull(RadioReferencePlaylistActions.findChannel(channels, List.of(154_430_000L),
            RadioReferencePlaylistActions.getProtocolFamily(DecoderType.P25_PHASE1)), "same frequency, other protocol");
        assertSame(p25, RadioReferencePlaylistActions.findChannel(channels, List.of(851_250_000L),
            RadioReferencePlaylistActions.getProtocolFamily(DecoderType.P25_PHASE1)), "P25 phases are one protocol");
        assertNull(RadioReferencePlaylistActions.findChannel(channels, List.of(460_000_000L),
            RadioReferencePlaylistActions.getProtocolFamily(DecoderType.NBFM)), "traffic channels are ignored");
    }

    @Test
    void conventionalDecoderType()
    {
        FrequencyCandidate fmn = new FrequencyCandidate(154_430_000, "Fire", null, "FMN", null, null, false, "x",
            null, null, null, null);
        FrequencyCandidate dstar = new FrequencyCandidate(145_000_000, "Ham", null, "D-STAR", null, null, false, "x",
            null, null, null, null);
        assertEquals(DecoderType.NBFM, RadioReferencePlaylistActions.getDecoderType(fmn));
        assertNull(RadioReferencePlaylistActions.getDecoderType(dstar));
    }

    @Test
    void trunkedSiteChannel()
    {
        Type type = new Type();
        type.setTypeId(1);
        type.setName("Project 25");
        Flavor phase2 = new Flavor();
        phase2.setFlavorId(2);
        phase2.setName("Phase II");
        Voice voice = new Voice();
        voice.setVoiceId(3);
        voice.setName("APCO-25 Common Air Interface Exclusive");
        RadioReferenceDecoder decoder = new RadioReferenceDecoder(null, Map.of(1, type), Map.of(2, phase2),
            Map.of(3, voice), Map.of());

        io.github.dsheirer.rrapi.type.System system = new io.github.dsheirer.rrapi.type.System();
        system.setSystemId(42);
        system.setName("Onondaga County Interop");
        system.setTypeId(1);
        system.setFlavorId(2);
        system.setVoiceId(3);

        Site site = new Site();
        site.setSiteNumber(3);
        site.setDescription("Syracuse");
        site.setTdmaControlChannel(0);
        site.setSiteFrequencies(List.of(frequency(851.0125, ""), frequency(851.2625, "a"), frequency(851.5125, "d")));

        Channel channel = RadioReferenceChannelFactory.trunkedSite(decoder, system, new SystemInformation(), site);

        assertEquals(DecoderType.P25_PHASE1, channel.getDecodeConfiguration().getDecoderType(),
            "FDMA control channel of a Phase II site");
        assertEquals(List.of(851_512_500L, 851_262_500L),
            RadioReferencePlaylistActions.getFrequencies(channel.getSourceConfiguration()), "control, then alternate");
        assertEquals("Onondaga County Interop", channel.getSystem());
        assertEquals("Syracuse", channel.getSite());
        assertEquals("Site 3", channel.getName());

        Channel startOnAlternate = RadioReferenceChannelFactory.trunkedSite(decoder, system, new SystemInformation(),
            site, 851_262_500L);
        assertEquals(List.of(851_262_500L, 851_512_500L),
            RadioReferencePlaylistActions.getFrequencies(startOnAlternate.getSourceConfiguration()),
            "the picked alternate control channel first");

        Channel voicePicked = RadioReferenceChannelFactory.trunkedSite(decoder, system, new SystemInformation(),
            site, 851_012_500L);
        assertEquals(List.of(851_512_500L, 851_262_500L),
            RadioReferencePlaylistActions.getFrequencies(voicePicked.getSourceConfiguration()),
            "a voice channel isn't added: control channels first");

        Type motorola = new Type();
        motorola.setTypeId(1);
        motorola.setName("Motorola");
        RadioReferenceDecoder motorolaDecoder = new RadioReferenceDecoder(null, Map.of(1, motorola),
            Map.of(2, phase2), Map.of(3, voice), Map.of());
        assertNull(RadioReferenceChannelFactory.trunkedSite(motorolaDecoder, system, new SystemInformation(), site),
            "Motorola Type II control channel");
    }

    private static SiteFrequency frequency(double megahertz, String use)
    {
        SiteFrequency frequency = new SiteFrequency();
        frequency.setFrequency(megahertz);
        frequency.setUse(use);
        return frequency;
    }
}

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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.edacs.DecodeConfigEDACS;
import io.github.dsheirer.rrapi.type.Flavor;
import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.SiteFrequency;
import io.github.dsheirer.rrapi.type.SystemInformation;
import io.github.dsheirer.rrapi.type.Type;
import io.github.dsheirer.rrapi.type.Voice;
import io.github.dsheirer.service.radioreference.SiteRecord;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests that radio reference EDACS sites are offered with the EDACS decoder, and that a channel created for one has
 * the site's logical channel number (LCN) frequency list and its control channels.
 */
class RadioReferenceEDACSTest
{
    private static SiteRecord siteRecord(String protocol)
    {
        List<SiteRecord.SiteChannel> channels = new ArrayList<>();
        channels.add(new SiteRecord.SiteChannel(853_900_000L, 11, "Control"));
        channels.add(new SiteRecord.SiteChannel(851_175_000L, 1, null));
        return new SiteRecord(1, "System", protocol, null, 10, 1, "Site", null, 42.0, -71.0, 10, null, 0, 0, channels);
    }

    private static SiteFrequency frequency(int lcn, double megahertz, String use)
    {
        SiteFrequency frequency = new SiteFrequency();
        frequency.setLogicalChannelNumber(lcn);
        frequency.setFrequency(megahertz);
        frequency.setUse(use);
        return frequency;
    }

    private static RadioReferenceDecoder decoder(String flavorName)
    {
        Type type = new Type();
        type.setTypeId(1);
        type.setName("EDACS");
        Flavor flavor = new Flavor();
        flavor.setFlavorId(2);
        flavor.setName(flavorName);
        Voice voice = new Voice();
        voice.setVoiceId(3);
        voice.setName("ProVoice");
        return new RadioReferenceDecoder(null, Map.of(1, type), Map.of(2, flavor), Map.of(3, voice), Map.of());
    }

    private static io.github.dsheirer.rrapi.type.System system()
    {
        io.github.dsheirer.rrapi.type.System system = new io.github.dsheirer.rrapi.type.System();
        system.setSystemId(42);
        system.setName("Transit");
        system.setTypeId(1);
        system.setFlavorId(2);
        system.setVoiceId(3);
        return system;
    }

    private static Site site()
    {
        Site site = new Site();
        site.setSiteNumber(1);
        site.setDescription("Simulcast");
        site.setSiteFrequencies(List.of(frequency(1, 851.175, ""), frequency(2, 851.225, ""),
            frequency(4, 853.6625, "a"), frequency(11, 853.9, "d")));
        return site;
    }

    @Test
    void widebandSitesAreSupported()
    {
        for(String protocol : List.of("EDACS Standard", "EDACS Networked", "EDACS Extended Addressing"))
        {
            assertEquals(EnumSet.of(DecoderType.EDACS), RadioReferencePlaylistActions.getSiteDecoderTypes(siteRecord(protocol)),
                protocol);
            assertTrue(RadioReferencePlaylistActions.isSupported(siteRecord(protocol)), protocol);
        }
    }

    @Test
    void narrowbandAndScatSitesAreNotSupported()
    {
        for(String protocol : List.of("EDACS Narrowband", "EDACS Narrowband Networked", "EDACS SCAT"))
        {
            assertFalse(RadioReferencePlaylistActions.isSupported(siteRecord(protocol)), protocol);
            assertEquals(protocol + " isn't supported",
                RadioReferencePlaylistActions.getUnsupportedReason(siteRecord(protocol)));
        }
    }

    @Test
    void siteChannelHasTheLcnFrequenciesAndControlChannels()
    {
        Channel channel = RadioReferenceChannelFactory.trunkedSite(decoder("Networked"), system(),
            new SystemInformation(), site());

        assertEquals(DecoderType.EDACS, channel.getDecodeConfiguration().getDecoderType());
        DecodeConfigEDACS config = assertInstanceOf(DecodeConfigEDACS.class, channel.getDecodeConfiguration());
        assertEquals(851_175_000L, config.getFrequency(1));
        assertEquals(851_225_000L, config.getFrequency(2));
        assertEquals(0L, config.getFrequency(3), "an LCN that radio reference doesn't list");
        assertEquals(853_662_500L, config.getFrequency(4));
        assertEquals(853_900_000L, config.getFrequency(11));

        assertEquals(List.of(853_900_000L, 853_662_500L),
            RadioReferencePlaylistActions.getFrequencies(channel.getSourceConfiguration()), "control, then alternate");
        assertEquals("Transit", channel.getSystem());
    }

    @Test
    void narrowbandSiteHasNoChannel()
    {
        assertNull(RadioReferenceChannelFactory.trunkedSite(decoder("Narrowband"), system(), new SystemInformation(),
            site()));
        assertNull(RadioReferenceChannelFactory.trunkedSite(decoder("Narrowband Networked"), system(),
            new SystemInformation(), site()));
        assertNull(RadioReferenceChannelFactory.trunkedSite(decoder("SCAT"), system(), new SystemInformation(), site()));
    }
}

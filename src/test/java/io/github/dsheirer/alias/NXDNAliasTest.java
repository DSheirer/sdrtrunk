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

package io.github.dsheirer.alias;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.radio.NXDNTypeDRadio;
import io.github.dsheirer.alias.id.radio.NXDNTypeDRadioRange;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.talkgroup.NXDNTypeDTalkgroup;
import io.github.dsheirer.alias.id.talkgroup.NXDNTypeDTalkgroupRange;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlRequest;
import io.github.dsheirer.protocol.Protocol;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NXDNAliasTest
{
    private static final int HOME_REPEATER = 3;
    private static final int RAW_TALKGROUP_03_0123 = 6267;
    private static final int RAW_RADIO_03_0456 = 6600;

    private static Alias alias(AliasID id, AliasList list)
    {
        Alias alias = new Alias();
        alias.setName(id.toString());
        alias.addAliasID(id);
        list.addAlias(alias);
        return alias;
    }

    private static void assertAlias(Alias expected, AliasList list, Identifier identifier)
    {
        assertEquals(List.of(expected), list.getAliases(identifier), "Alias for " + identifier);
    }

    @Test
    void standardIdentifiersStillMatch()
    {
        AliasList list = new AliasList("test");
        Alias talkgroup = alias(new Talkgroup(Protocol.NXDN, 123), list);
        Alias radio = alias(new Radio(Protocol.NXDN, 456), list);
        assertAlias(talkgroup, list, NXDNTalkgroupIdentifier.createTo(123));
        assertAlias(radio, list, NXDNRadioIdentifier.createFrom(456));
    }

    @Test
    void typeDTalkgroupMatchesDisplayedRepeaterAndTalkgroup()
    {
        NXDNTypeDTalkgroup id = new NXDNTypeDTalkgroup(HOME_REPEATER, 123);
        assertEquals(RAW_TALKGROUP_03_0123, id.getValue());
        assertEquals(HOME_REPEATER, id.getHomeRepeater());
        assertEquals(123, id.getIdentifier());
        assertTrue(id.isValid());

        AliasList list = new AliasList("test");
        Alias alias = alias(id, list);
        NXDNTalkgroupIdentifier identifier = NXDNTalkgroupIdentifier.createTypeDTo(RAW_TALKGROUP_03_0123);
        assertEquals("03-0123", identifier.toString());
        assertAlias(alias, list, identifier);
        assertTrue(list.getAliases(NXDNTalkgroupIdentifier.createTypeDTo(RAW_TALKGROUP_03_0123 + 2048)).isEmpty(),
                "Same talkgroup on another home repeater must not match");
    }

    @Test
    void typeDRadioMatchesDisplayedRepeaterAndRadio()
    {
        AliasList list = new AliasList("test");
        Alias alias = alias(new NXDNTypeDRadio(HOME_REPEATER, 456), list);
        assertAlias(alias, list, NXDNRadioIdentifier.createTypeDFrom(RAW_RADIO_03_0456));
        assertAlias(alias, list, NXDNRadioIdentifier.createTypeDTo(RAW_RADIO_03_0456));
    }

    @Test
    void typeDRangesMatchOnlyTheirHomeRepeater()
    {
        AliasList list = new AliasList("test");
        Alias talkgroups = alias(new NXDNTypeDTalkgroupRange(HOME_REPEATER, 100, 200), list);
        Alias radios = alias(new NXDNTypeDRadioRange(HOME_REPEATER, 400, 500), list);

        assertAlias(talkgroups, list, NXDNTalkgroupIdentifier.createTypeDTo(RAW_TALKGROUP_03_0123));
        assertAlias(radios, list, NXDNRadioIdentifier.createTypeDFrom(RAW_RADIO_03_0456));
        assertTrue(list.getAliases(NXDNTalkgroupIdentifier.createTypeDTo((4 << 11) | 123)).isEmpty());
        assertTrue(list.getAliases(NXDNRadioIdentifier.createTypeDFrom((4 << 11) | 456)).isEmpty());
    }

    @Test
    void typeDFieldsCanBeEditedIndependently()
    {
        NXDNTypeDTalkgroup talkgroup = new NXDNTypeDTalkgroup();
        assertEquals(Protocol.NXDN, talkgroup.getProtocol());
        talkgroup.setIdentifier(123);
        talkgroup.setHomeRepeater(HOME_REPEATER);
        assertEquals(RAW_TALKGROUP_03_0123, talkgroup.getValue());

        NXDNTypeDRadioRange range = new NXDNTypeDRadioRange();
        range.setMinIdentifier(400);
        range.setMaxIdentifier(500);
        range.setHomeRepeater(HOME_REPEATER);
        assertEquals(HOME_REPEATER, range.getHomeRepeater());
        assertEquals(400, range.getMinIdentifier());
        assertEquals(500, range.getMaxIdentifier());
        assertTrue(range.isValid());
    }

    @Test
    void typeDRangeAcrossHomeRepeatersIsNotValid()
    {
        NXDNTypeDTalkgroupRange range = new NXDNTypeDTalkgroupRange();
        range.setMinTalkgroup((3 << 11) | 100);
        range.setMaxTalkgroup((4 << 11) | 100);
        assertFalse(range.isValid());
    }

    @Test
    void copyRetainsTypeDIdentifierType()
    {
        for(AliasID id : List.of(new NXDNTypeDTalkgroup(HOME_REPEATER, 123), new NXDNTypeDRadio(HOME_REPEATER, 456),
                new NXDNTypeDTalkgroupRange(HOME_REPEATER, 100, 200), new NXDNTypeDRadioRange(HOME_REPEATER, 400, 500)))
        {
            Alias original = new Alias();
            original.setName("original");
            original.addAliasID(id);
            AliasID copy = AliasFactory.copyOf(original).getAliasIdentifiers().getFirst();
            assertNotSame(id, copy);
            assertEquals(id.getClass(), copy.getClass());
            assertEquals(id.toString(), copy.toString());
        }
    }

    @Test
    void typeDIdentifiersRoundTripThroughPlaylistXml() throws Exception
    {
        XmlMapper mapper = new XmlMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for(AliasID id : List.of(new NXDNTypeDTalkgroup(HOME_REPEATER, 123), new NXDNTypeDRadio(HOME_REPEATER, 456),
                new NXDNTypeDTalkgroupRange(HOME_REPEATER, 100, 200), new NXDNTypeDRadioRange(HOME_REPEATER, 400, 500)))
        {
            String xml = mapper.writeValueAsString(id);
            assertFalse(xml.contains("homeRepeater"), xml);
            AliasID restored = mapper.readValue(xml, AliasID.class);
            assertEquals(id.getClass(), restored.getClass(), xml);
            assertEquals(id.toString(), restored.toString(), xml);
        }
    }

    @Test
    void remoteControlDestinationWithLocationIdIsTheDestinationRadio()
    {
        int source = 456;
        int destination = 200;

        //Location option values: 0=COMMON, 1=DESTINATION, 2=SOURCE
        for(int locationOption = 0; locationOption < 3; locationOption++)
        {
            CorrectedBinaryMessage message = new CorrectedBinaryMessage(200);
            message.load(8, 8, 0x40); //Call control option: has location ID
            message.load(16, 3, 4); //Destination is a radio
            message.load(24, 16, source);
            message.load(40, 16, destination);
            message.load(72, 2, locationOption);

            RemoteControlRequest request = new RemoteControlRequest(message, 1, NXDNMessageType.CONTROL_OUT_52_CC_REMOTE_CONTROL_REQUEST, 1, null);

            NXDNRadioIdentifier radio = assertInstanceOf(NXDNRadioIdentifier.class, request.getDestination());
            assertEquals(destination, radio.getValue(), "Location option " + locationOption);
            assertEquals(Role.TO, radio.getRole(), "Location option " + locationOption);
            assertEquals(source, request.getSource().getValue(), "Location option " + locationOption);
        }
    }
}

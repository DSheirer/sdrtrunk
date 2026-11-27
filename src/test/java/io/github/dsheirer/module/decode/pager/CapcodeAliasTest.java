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

package io.github.dsheirer.module.decode.pager;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioFormat;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.module.decode.pager.pocsag.PocsagMessage;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.PlaylistV2;
import io.github.dsheirer.protocol.Protocol;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests aliasing of POCSAG and FLEX pager capcodes as protocol-specific radio identifiers.
 */
public class CapcodeAliasTest
{
    private static AliasList createAliasList()
    {
        AliasList aliasList = new AliasList("Pagers");

        Alias station = new Alias("Station 7");
        station.setAliasListName("Pagers");
        station.addAliasID(new Radio(Protocol.POCSAG, 1999998));
        aliasList.addAlias(station);

        Alias flexGroup = new Alias("FLEX Group");
        flexGroup.setAliasListName("Pagers");
        flexGroup.addAliasID(new RadioRange(Protocol.FLEX, 1500000, 1500100));
        aliasList.addAlias(flexGroup);

        return aliasList;
    }

    @Test
    public void capcodesMatchAliasesByProtocol()
    {
        AliasList aliasList = createAliasList();

        List<Alias> pocsag = aliasList.getAliases(new PagerCapcodeIdentifier(1999998, Protocol.POCSAG));
        assertEquals(1, pocsag.size());
        assertEquals("Station 7", pocsag.get(0).getName());

        List<Alias> flex = aliasList.getAliases(new PagerCapcodeIdentifier(1500050, Protocol.FLEX));
        assertEquals(1, flex.size());
        assertEquals("FLEX Group", flex.get(0).getName());

        //Same capcode value, different protocol - no match
        assertTrue(aliasList.getAliases(new PagerCapcodeIdentifier(1999998, Protocol.FLEX)).isEmpty());
        assertTrue(aliasList.getAliases(new PagerCapcodeIdentifier(1500050, Protocol.POCSAG)).isEmpty());
        assertTrue(aliasList.getAliases(new PagerCapcodeIdentifier(1500101, Protocol.FLEX)).isEmpty());
    }

    @Test
    public void decodedPageCapcodeMatchesAlias()
    {
        PocsagMessage page = new PocsagMessage(0, 1999998, 0, PagerMessageType.BINARY, "E82AE", 512, 0, true);
        AliasList aliasList = createAliasList();
        assertEquals(1, page.getIdentifiers().size());
        assertEquals("Station 7", aliasList.getAliases(page.getIdentifiers().get(0)).get(0).getName());
    }

    @Test
    public void capcodeValidityAndLabels()
    {
        assertTrue(new Radio(Protocol.POCSAG, 0).isValid());
        assertTrue(new Radio(Protocol.POCSAG, 0x1FFFFF).isValid());
        assertFalse(new Radio(Protocol.POCSAG, 0x200000).isValid());
        assertTrue(new Radio(Protocol.FLEX, 1933312).isValid());
        assertFalse(new Radio(Protocol.FLEX, 0).isValid());

        assertEquals("Capcode", RadioFormat.getIdentifierLabel(Protocol.POCSAG));
        assertEquals("Capcode", RadioFormat.getIdentifierLabel(Protocol.FLEX));
        assertEquals("Radio ID", RadioFormat.getIdentifierLabel(Protocol.APCO25));
        assertTrue(new Radio(Protocol.POCSAG, 1999998).toString().startsWith("Capcode:1999998"));
        assertTrue(new RadioRange(Protocol.FLEX, 1, 2).toString().startsWith("Capcode Range:1 to 2"));
        assertTrue(Protocol.RADIO_ID_PROTOCOLS.contains(Protocol.POCSAG));
        assertTrue(Protocol.RADIO_ID_PROTOCOLS.contains(Protocol.FLEX));
    }

    @Test
    public void capcodeAliasesPersistInPlaylist() throws Exception
    {
        JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(false);
        XmlMapper mapper = new XmlMapper(module);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        PlaylistV2 playlist = mapper.readValue("""
            <playlist version="%d">
                <alias name="Station 7" list="Pagers">
                    <id type="radio" protocol="POCSAG" value="1999998"/>
                </alias>
                <alias name="FLEX Group" list="Pagers">
                    <id type="radioRange" protocol="FLEX" min="1500000" max="1500100"/>
                </alias>
            </playlist>
            """.formatted(PlaylistManager.PLAYLIST_CURRENT_VERSION), PlaylistV2.class);

        playlist = mapper.readValue(mapper.writeValueAsString(playlist), PlaylistV2.class);

        Radio radio = assertInstanceOf(Radio.class, playlist.getAliases().get(0).getAliasIdentifiers().get(0));
        assertEquals(Protocol.POCSAG, radio.getProtocol());
        assertEquals(1999998, radio.getValue());

        RadioRange range = assertInstanceOf(RadioRange.class, playlist.getAliases().get(1).getAliasIdentifiers().get(0));
        assertEquals(Protocol.FLEX, range.getProtocol());
        assertEquals(1500000, range.getMinRadio());
        assertEquals(1500100, range.getMaxRadio());
    }
}

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

package io.github.dsheirer.playlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioFormatter;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.mdc1200.MDCMessage;
import io.github.dsheirer.module.decode.mdc1200.identifier.MDC1200Identifier;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.preference.identifier.MDC1200RadioFormatter;
import io.github.dsheirer.protocol.Protocol;
import java.util.List;
import org.junit.jupiter.api.Test;

class MdcRadioIdentifierTest
{
    @Test
    void currentPlaylistMigratesOldIdsAndMatchesImportedRadios() throws Exception
    {
        JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(false);
        XmlMapper mapper = new XmlMapper(module);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        PlaylistV2 playlist = mapper.readValue("""
            <playlist version="%d">
                <alias name="Legacy unit" list="MDC" group="Dispatch">
                    <id type="talkgroup" protocol="MDC1200" value="2748"/>
                    <id type="talkgroup" protocol="APCO25" value="2748"/>
                </alias>
                <alias name="Legacy range" list="MDC">
                    <id type="talkgroupRange" protocol="MDC1200" min="1" max="100"/>
                </alias>
                <alias name="Imported radio" list="MDC">
                    <id type="radio" protocol="MDC1200" value="65535"/>
                </alias>
                <alias name="Old hex ID" list="MDC">
                    <id type="mdc1200ID" ident="123A"/>
                </alias>
            </playlist>
            """.formatted(PlaylistManager.PLAYLIST_CURRENT_VERSION), PlaylistV2.class);
        assertTrue(PlaylistUpdater.update(playlist));

        // Verify the serialized aliases use radio types and that reopening them makes no further changes.
        playlist = mapper.readValue(mapper.writeValueAsString(playlist), PlaylistV2.class);
        assertFalse(PlaylistUpdater.update(playlist));

        Alias legacyUnit = playlist.getAliases().get(0);
        Alias legacyRange = playlist.getAliases().get(1);
        Alias importedRadio = playlist.getAliases().get(2);
        Alias oldHexId = playlist.getAliases().get(3);
        assertEquals("Dispatch", legacyUnit.getGroup());
        assertEquals(0x0ABC, assertInstanceOf(Radio.class, legacyUnit.getAliasIdentifiers().get(0)).getValue());
        assertEquals(Protocol.APCO25,
            assertInstanceOf(Talkgroup.class, legacyUnit.getAliasIdentifiers().get(1)).getProtocol());
        RadioRange range = assertInstanceOf(RadioRange.class, legacyRange.getAliasIdentifiers().getFirst());
        assertEquals(1, range.getMinRadio());
        assertEquals(100, range.getMaxRadio());

        AliasList aliasList = new AliasList("MDC");
        playlist.getAliases().forEach(aliasList::addAlias);
        assertEquals(List.of(legacyUnit), aliasList.getAliases(MDC1200Identifier.createFrom(0x0ABC)));
        assertEquals(List.of(legacyRange), aliasList.getAliases(MDC1200Identifier.createFrom(100)));
        assertEquals(List.of(oldHexId), aliasList.getAliases(MDC1200Identifier.createFrom(0x123A)));

        MDCMessage message = new MDCMessage(CorrectedBinaryMessage.loadHex("00000000000000FFFF"));
        MDC1200Identifier identifier = message.getFromIdentifier();
        assertEquals(Form.RADIO, identifier.getForm());
        assertEquals(Role.FROM, identifier.getRole());
        assertEquals(0xFFFF, identifier.getValue());
        assertEquals(List.of(importedRadio), aliasList.getAliases(identifier));
    }

    @Test
    void radioFormattingPreservesMdcWidthAndValueLimits()
    {
        assertEquals("00AB", MDC1200RadioFormatter.format(0xAB, IntegerFormat.HEXADECIMAL, true));
        assertEquals("00171", MDC1200RadioFormatter.format(0xAB, IntegerFormat.DECIMAL, true));
        assertEquals("AB", RadioFormatter.format(Protocol.MDC1200, 0xAB, IntegerFormat.HEXADECIMAL));
        assertFalse(new Radio(Protocol.MDC1200, 0).isValid());
        assertTrue(new Radio(Protocol.MDC1200, 1).isValid());
        assertTrue(new Radio(Protocol.MDC1200, 0xFFFF).isValid());
        assertFalse(new Radio(Protocol.MDC1200, 0x10000).isValid());
    }
}

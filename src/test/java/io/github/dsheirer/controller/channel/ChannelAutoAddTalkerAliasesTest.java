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
package io.github.dsheirer.controller.channel;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.playlist.PlaylistV2;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Tests channel auto-add talker aliases default and playlist persistence.
 */
public class ChannelAutoAddTalkerAliasesTest {
  @Test public void newChannelsDefaultOnLegacyChannelsStayOff() throws Exception {
    JacksonXmlModule m = new JacksonXmlModule(); m.setDefaultUseWrapper(false);
    XmlMapper mapper = new XmlMapper(m);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    assertTrue(new Channel().isAutoAddTalkerAliases(), "new channel default on");
    assertTrue(new Channel("n").isAutoAddTalkerAliases(), "new named channel default on");
    Channel off = new Channel("x"); off.setAutoAddTalkerAliases(false);
    assertFalse(mapper.readValue(mapper.writeValueAsString(off), Channel.class).isAutoAddTalkerAliases());
    assertTrue(mapper.readValue(mapper.writeValueAsString(new Channel("y")), Channel.class).isAutoAddTalkerAliases());
    String legacy = mapper.writeValueAsString(new Channel("z")).replace(" auto_add_talker_aliases=\"true\"", "");
    assertFalse(legacy.contains("auto_add"));
    Channel legacyChannel = mapper.readValue(legacy, Channel.class);
    assertFalse(legacyChannel.isAutoAddTalkerAliases(), "legacy channel stays off");
    assertEquals("z", legacyChannel.getName());
    PlaylistV2 pl = new PlaylistV2(); pl.getChannels().add(new Channel("p"));
    String plXml = mapper.writeValueAsString(pl).replace(" auto_add_talker_aliases=\"true\"", "");
    assertFalse(mapper.readValue(plXml, PlaylistV2.class).getChannels().get(0).isAutoAddTalkerAliases(), "legacy playlist off");
    off.setAutoAddTalkerAliases(false); assertFalse(off.copyOf().isAutoAddTalkerAliases());
  }
}

/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ****************************************************************************
 */
package io.github.dsheirer.alias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.ctcss.Ctcss;
import io.github.dsheirer.identifier.ctcss.CTCSSIdentifier;
import io.github.dsheirer.module.decode.squelch.ctcss.CTCSSCode;
import io.github.dsheirer.playlist.PlaylistV2;
import java.util.List;
import org.junit.jupiter.api.Test;

class CTCSSAliasTest
{
    @Test
    void looksUpAndRemovesCtcssAlias()
    {
        AliasList aliasList = new AliasList("NBFM");
        Alias alias = createAlias(CTCSSCode.TONE_1928);
        aliasList.addAlias(alias);

        assertEquals(List.of(alias), aliasList.getAliases(new CTCSSIdentifier(CTCSSCode.TONE_1928)));

        aliasList.removeAlias(alias);
        assertTrue(aliasList.getAliases(new CTCSSIdentifier(CTCSSCode.TONE_1928)).isEmpty());
    }

    @Test
    void copiesCtcssAliasIdentifier()
    {
        Ctcss original = new Ctcss();
        original.setCTCSSCode(CTCSSCode.TONE_1928);

        AliasID copiedId = AliasFactory.copyOf(original);

        Ctcss copy = assertInstanceOf(Ctcss.class, copiedId);
        assertNotSame(original, copy);
        assertEquals(CTCSSCode.TONE_1928, copy.getCTCSSCode());
    }

    @Test
    void persistsCtcssAliasIdentifier() throws Exception
    {
        PlaylistV2 playlist = new PlaylistV2();
        playlist.setAliases(List.of(createAlias(CTCSSCode.TONE_1928)));
        JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(false);
        ObjectMapper mapper = new XmlMapper(module)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String xml = mapper.writeValueAsString(playlist);
        PlaylistV2 restored = mapper.readValue(xml, PlaylistV2.class);

        Ctcss restoredCtcss = restored.getAliases().getFirst().getAliasIdentifiers().stream()
            .filter(Ctcss.class::isInstance).map(Ctcss.class::cast).findFirst().orElseThrow();
        assertEquals(CTCSSCode.TONE_1928, restoredCtcss.getCTCSSCode());
    }

    private static Alias createAlias(CTCSSCode code)
    {
        Alias alias = new Alias("County Dispatch");
        alias.setAliasListName("NBFM");
        Ctcss ctcss = new Ctcss();
        ctcss.setCTCSSCode(code);
        alias.addAliasID(ctcss);
        return alias;
    }
}

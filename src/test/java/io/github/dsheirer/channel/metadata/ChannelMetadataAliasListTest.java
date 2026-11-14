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

package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that channel metadata resolves the TO and FROM aliases, and notifies its listener, whichever of the alias list
 * and the identifiers arrives first.  A traffic channel that is started for a channel grant has its TO and FROM
 * identifiers before its alias list.
 */
public class ChannelMetadataAliasListTest
{
    private static final int TIMESLOT = 0;
    private static final String ALIAS_LIST = "Test Alias List";
    private static final String TALKGROUP_ALIAS = "Talkgroup 1";
    private static final String RADIO_ALIAS = "Radio 100";

    private ChannelMetadata mChannelMetadata;
    private MutableIdentifierCollection mIdentifierCollection;
    private final List<ChannelMetadataField> mUpdates = new ArrayList<>();

    @BeforeEach
    public void setup()
    {
        AliasModel aliasModel = new AliasModel();

        Alias talkgroup = new Alias(TALKGROUP_ALIAS);
        talkgroup.setAliasListName(ALIAS_LIST);
        talkgroup.addAliasID(new Talkgroup(Protocol.APCO25, 1));
        aliasModel.addAlias(talkgroup);

        Alias radio = new Alias(RADIO_ALIAS);
        radio.setAliasListName(ALIAS_LIST);
        radio.addAliasID(new Radio(Protocol.APCO25, 100));
        aliasModel.addAlias(radio);

        mChannelMetadata = new ChannelMetadata(aliasModel, TIMESLOT);
        mChannelMetadata.setUpdateEventListener((channelMetadata, field) -> mUpdates.add(field));
        mIdentifierCollection = new MutableIdentifierCollection(TIMESLOT);
        mIdentifierCollection.setIdentifierUpdateListener(mChannelMetadata);
        mUpdates.clear();
    }

    private void assertAliased()
    {
        Assertions.assertEquals(1, mChannelMetadata.getToIdentifierAliases().size());
        Assertions.assertEquals(TALKGROUP_ALIAS, mChannelMetadata.getToIdentifierAliases().get(0).getName());
        Assertions.assertEquals(1, mChannelMetadata.getFromIdentifierAliases().size());
        Assertions.assertEquals(RADIO_ALIAS, mChannelMetadata.getFromIdentifierAliases().get(0).getName());
    }

    @Test
    public void aliasesResolveWhenTheAliasListArrivesFirst()
    {
        mIdentifierCollection.update(AliasListConfigurationIdentifier.create(ALIAS_LIST));
        mIdentifierCollection.update(APCO25Talkgroup.create(1));
        mIdentifierCollection.update(APCO25RadioIdentifier.createFrom(100));

        assertAliased();
    }

    /**
     * The aliases are resolved when the alias list arrives after the identifiers, and the listener is told so that
     * the Now Playing row is repainted with them.
     */
    @Test
    public void listenerIsNotifiedWhenTheAliasListArrivesAfterTheIdentifiers()
    {
        mIdentifierCollection.update(APCO25Talkgroup.create(1));
        mIdentifierCollection.update(APCO25RadioIdentifier.createFrom(100));
        Assertions.assertTrue(mChannelMetadata.getToIdentifierAliases().isEmpty());
        mUpdates.clear();

        mIdentifierCollection.update(AliasListConfigurationIdentifier.create(ALIAS_LIST));

        assertAliased();
        Assertions.assertTrue(mUpdates.contains(ChannelMetadataField.USER_TO), "TO alias update notification");
        Assertions.assertTrue(mUpdates.contains(ChannelMetadataField.USER_FROM), "FROM alias update notification");
    }
}

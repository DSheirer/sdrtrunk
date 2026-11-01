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

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.alias.DmrTalkerAliasIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests channel metadata tracking of the FROM talker alias, driven by a mutable identifier collection in the same
 * way that a decoder state's identifier updates reach the channel metadata in production.
 */
public class ChannelMetadataTest
{
    private static final int TIMESLOT = 1;

    private ChannelMetadata mChannelMetadata;
    private MutableIdentifierCollection mIdentifierCollection;

    @BeforeEach
    public void setup()
    {
        mChannelMetadata = new ChannelMetadata(null, TIMESLOT);
        mIdentifierCollection = new MutableIdentifierCollection(TIMESLOT);
        mIdentifierCollection.setIdentifierUpdateListener(mChannelMetadata);
    }

    /**
     * End of a call as performed by the DMR decoder state: remove the alias and then the FROM radio.
     */
    private void endCall()
    {
        mIdentifierCollection.remove(IdentifierClass.USER, Form.TALKER_ALIAS, Role.FROM);
        mIdentifierCollection.remove(Role.FROM);
    }

    @Test
    public void talkerAliasIsAvailableWhenAdded()
    {
        DmrTalkerAliasIdentifier alias = new DmrTalkerAliasIdentifier("ALPHA");
        mIdentifierCollection.update(DMRRadio.createFrom(100));
        mIdentifierCollection.update(alias);

        Assertions.assertTrue(mChannelMetadata.hasTalkerAliasIdentifier());
        Assertions.assertEquals(alias, mChannelMetadata.getTalkerAliasIdentifier());
    }

    @Test
    public void talkerAliasIsClearedWhenRemoved()
    {
        mIdentifierCollection.update(DMRRadio.createFrom(100));
        mIdentifierCollection.update(new DmrTalkerAliasIdentifier("ALPHA"));
        endCall();

        Assertions.assertFalse(mChannelMetadata.hasTalkerAliasIdentifier());
        Assertions.assertNull(mChannelMetadata.getTalkerAliasIdentifier());
    }

    /**
     * Issue 2373: a radio without a talker alias must not inherit the alias from the previous call on the timeslot.
     */
    @Test
    public void talkerAliasDoesNotCarryOverToNextCall()
    {
        mIdentifierCollection.update(DMRRadio.createFrom(100));
        mIdentifierCollection.update(new DmrTalkerAliasIdentifier("ALPHA"));
        endCall();

        mIdentifierCollection.update(DMRRadio.createFrom(200));

        Assertions.assertEquals(200, mChannelMetadata.getFromIdentifier().getValue());
        Assertions.assertFalse(mChannelMetadata.hasTalkerAliasIdentifier());
    }

    @Test
    public void talkerAliasIsReplacedWhenUpdated()
    {
        DmrTalkerAliasIdentifier bravo = new DmrTalkerAliasIdentifier("BRAVO");
        mIdentifierCollection.update(DMRRadio.createFrom(100));
        mIdentifierCollection.update(new DmrTalkerAliasIdentifier("ALPHA"));
        mIdentifierCollection.update(bravo);

        Assertions.assertEquals(bravo, mChannelMetadata.getTalkerAliasIdentifier());
    }
}

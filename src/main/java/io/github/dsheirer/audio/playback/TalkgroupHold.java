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
package io.github.dsheirer.audio.playback;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Hold talkgroup for an audio channel.  While a channel holds a talkgroup, only calls to that talkgroup play on the
 * channel and calls to the talkgroup are steered to the channel.
 *
 * A call matches when its TO talkgroup has the same protocol and value, or when its TO patch group is (or includes)
 * the talkgroup.  APCO-25 phase 1 and phase 2 are treated as the same protocol since a P25 system's talkgroups are
 * used on both phase 1 and phase 2 traffic channels.
 *
 * @param protocol of the talkgroup
 * @param talkgroup value
 * @param label for display (formatted talkgroup and alias)
 */
public record TalkgroupHold(Protocol protocol, int talkgroup, String label)
{
    /**
     * Normalizes the protocol for comparison
     */
    private static Protocol normalize(Protocol protocol)
    {
        return protocol == Protocol.APCO25_PHASE2 ? Protocol.APCO25 : protocol;
    }

    /**
     * Indicates if the talkgroup identifier is this hold talkgroup
     */
    public boolean matches(TalkgroupIdentifier identifier)
    {
        return identifier != null && identifier.getValue() != null && identifier.getValue() == talkgroup &&
            normalize(identifier.getProtocol()) == normalize(protocol);
    }

    /**
     * Indicates if the call identifiers have this hold talkgroup (or a patch group with it) as the TO identifier.
     */
    public boolean matches(IdentifierCollection identifierCollection)
    {
        if(identifierCollection == null)
        {
            return false;
        }

        for(Identifier identifier: identifierCollection.getIdentifiers(Role.TO))
        {
            if(identifier instanceof TalkgroupIdentifier tg && matches(tg))
            {
                return true;
            }

            if(identifier instanceof PatchGroupIdentifier pgi && pgi.getValue() != null)
            {
                PatchGroup patchGroup = pgi.getValue();

                if(matches(patchGroup.getPatchGroup()))
                {
                    return true;
                }

                for(TalkgroupIdentifier patched: patchGroup.getPatchedTalkgroupIdentifiers())
                {
                    if(matches(patched))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Indicates if the audio segment is a call to this hold talkgroup.
     */
    public boolean matches(AudioSegment audioSegment)
    {
        return audioSegment != null && matches(audioSegment.getIdentifierCollection());
    }

    /**
     * Creates a hold for the TO talkgroup (or patch group) of the call identifiers.
     * @param identifierCollection of a call
     * @param label for display
     * @return hold or null if the call doesn't have a TO talkgroup
     */
    public static TalkgroupHold from(IdentifierCollection identifierCollection, String label)
    {
        if(identifierCollection == null)
        {
            return null;
        }

        Identifier to = identifierCollection.getToIdentifier();

        if(to instanceof PatchGroupIdentifier pgi && pgi.getValue() != null && pgi.getValue().getPatchGroup() != null)
        {
            to = pgi.getValue().getPatchGroup();
        }

        if(to instanceof TalkgroupIdentifier tg && tg.getValue() != null && tg.getValue() > 0)
        {
            return new TalkgroupHold(tg.getProtocol(), tg.getValue(), label != null ? label : tg.toString());
        }

        return null;
    }

    /**
     * Creates a hold for the TO talkgroup of a stored call.
     * @param call with a TO talkgroup
     * @param toAlias current alias name of the TO talkgroup for the label, or null
     * @return hold or null if the call doesn't have a TO talkgroup
     */
    public static TalkgroupHold from(CallRecord call, String toAlias)
    {
        if(call == null || !call.hasTalkgroup() || call.getToValue() > Integer.MAX_VALUE)
        {
            return null;
        }

        String label = call.getToId() + (toAlias != null ? " " + toAlias : "");
        return new TalkgroupHold(call.getProtocolValue(), call.getToValue().intValue(), label);
    }

    /**
     * Indicates if the other hold is for the same talkgroup (ignoring the label).
     */
    public boolean isSameTalkgroup(TalkgroupHold other)
    {
        return other != null && other.talkgroup == talkgroup && normalize(other.protocol) == normalize(protocol);
    }

    @Override
    public String toString()
    {
        return label;
    }
}

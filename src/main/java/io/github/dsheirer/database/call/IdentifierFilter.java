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
package io.github.dsheirer.database.call;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.talkgroup.P25FullyQualifiedTalkgroup;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Search filter for the TO or FROM identifier of a call.  The filter either matches typed text against the formatted
 * identifier (or the exact identifier value when the text is a number), or matches the identifiers of a selected
 * alias: calls of the alias's alias list whose identifier value is one of the alias's values or ranges, or whose fully
 * qualified (roaming) address is one of the alias's fully qualified identifiers.  This uses the same matching as the
 * alias list, so a search for an alias finds the calls that the alias list currently aliases with that alias.
 *
 * @param text typed text, or empty for no text filter (ignored when an alias is selected)
 * @param aliasName of the selected alias, or null
 * @param aliasList name of the selected alias, or null
 * @param ranges of identifier values for the selected alias
 * @param fullyQualified addresses for the selected alias
 */
public record IdentifierFilter(String text, String aliasName, String aliasList, List<ValueRange> ranges,
                               List<FullyQualifiedAddress> fullyQualified)
{
    /**
     * Range of identifier values (inclusive) for a protocol
     */
    public record ValueRange(Protocol protocol, int min, int max)
    {
        public boolean contains(Protocol callProtocol, long value)
        {
            return protocol == callProtocol && min <= value && value <= max;
        }
    }

    /**
     * Fully qualified identifier address (wacn.system.id) for a protocol
     */
    public record FullyQualifiedAddress(Protocol protocol, String address) {}

    public IdentifierFilter
    {
        text = text == null ? "" : text.trim();
        ranges = ranges == null ? List.of() : List.copyOf(ranges);
        fullyQualified = fullyQualified == null ? List.of() : List.copyOf(fullyQualified);
    }

    /**
     * Filter that matches every call
     */
    public static IdentifierFilter none()
    {
        return new IdentifierFilter("", null, null, null, null);
    }

    /**
     * Filter for typed text
     */
    public static IdentifierFilter text(String text)
    {
        return new IdentifierFilter(text, null, null, null, null);
    }

    /**
     * Filter for the talkgroup identifiers (talkgroups, ranges, fully qualified talkgroups) of the alias
     */
    public static IdentifierFilter talkgroupAlias(Alias alias)
    {
        List<ValueRange> ranges = new ArrayList<>();
        List<FullyQualifiedAddress> fullyQualified = new ArrayList<>();

        for(AliasID id: alias.getAliasIdentifiers())
        {
            if(!id.isValid())
            {
                continue;
            }

            if(id instanceof P25FullyQualifiedTalkgroup fqt)
            {
                fullyQualified.add(new FullyQualifiedAddress(fqt.getProtocol(), fqt.getHashKey()));
            }
            else if(id instanceof Talkgroup talkgroup)
            {
                ranges.add(new ValueRange(talkgroup.getProtocol(), talkgroup.getValue(), talkgroup.getValue()));
            }
            else if(id instanceof TalkgroupRange range)
            {
                ranges.add(new ValueRange(range.getProtocol(), range.getMinTalkgroup(), range.getMaxTalkgroup()));
            }
        }

        return new IdentifierFilter("", alias.getName(), alias.getAliasListName(), ranges, fullyQualified);
    }

    /**
     * Filter for the radio identifiers (radios, ranges, fully qualified radios) of the alias
     */
    public static IdentifierFilter radioAlias(Alias alias)
    {
        List<ValueRange> ranges = new ArrayList<>();
        List<FullyQualifiedAddress> fullyQualified = new ArrayList<>();

        for(AliasID id: alias.getAliasIdentifiers())
        {
            if(!id.isValid())
            {
                continue;
            }

            if(id instanceof P25FullyQualifiedRadio fqr)
            {
                fullyQualified.add(new FullyQualifiedAddress(fqr.getProtocol(), fqr.getHashKey()));
            }
            else if(id instanceof Radio radio)
            {
                ranges.add(new ValueRange(radio.getProtocol(), radio.getValue(), radio.getValue()));
            }
            else if(id instanceof RadioRange range)
            {
                ranges.add(new ValueRange(range.getProtocol(), range.getMinRadio(), range.getMaxRadio()));
            }
        }

        return new IdentifierFilter("", alias.getName(), alias.getAliasListName(), ranges, fullyQualified);
    }

    /**
     * Indicates if the alias has talkgroup identifiers that a talkgroup alias filter can search for
     */
    public static boolean hasTalkgroups(Alias alias)
    {
        IdentifierFilter filter = talkgroupAlias(alias);
        return !filter.ranges().isEmpty() || !filter.fullyQualified().isEmpty();
    }

    /**
     * Indicates if the alias has radio identifiers that a radio alias filter can search for
     */
    public static boolean hasRadios(Alias alias)
    {
        IdentifierFilter filter = radioAlias(alias);
        return !filter.ranges().isEmpty() || !filter.fullyQualified().isEmpty();
    }

    /**
     * Indicates if this filter is for a selected alias
     */
    public boolean isAlias()
    {
        return aliasName != null;
    }

    /**
     * Indicates if this filter doesn't filter anything.  An alias filter always filters: an alias without matching
     * identifiers matches no calls.
     */
    public boolean isEmpty()
    {
        return !isAlias() && text.isEmpty();
    }

    /**
     * Evaluates the filter against a call identifier in memory, using the same rules as the database query.
     * @param call to evaluate
     * @param formatted identifier text
     * @param value of the identifier, or null
     * @param fullyQualifiedAddress of the identifier, or null
     * @param formMatches true if the identifier form is the form that the alias identifiers apply to
     * @param patched talkgroups patched into the TO patch group, which also match, or an empty list
     */
    boolean matches(CallRecord call, String formatted, Long value, String fullyQualifiedAddress, boolean formMatches,
                    List<PatchedTalkgroup> patched)
    {
        if(isAlias())
        {
            if(!formMatches || call.getAliasList() == null || aliasList == null ||
                !call.getAliasList().equalsIgnoreCase(aliasList))
            {
                return false;
            }

            Protocol protocol = call.getProtocolValue();

            for(PatchedTalkgroup talkgroup: patched)
            {
                if(matchesAlias(protocol, talkgroup.value(), talkgroup.fullyQualified()))
                {
                    return true;
                }
            }

            return matchesAlias(protocol, value, fullyQualifiedAddress);
        }

        if(text.isEmpty())
        {
            return true;
        }

        Long parsed = CallSearchCriteria.parseValue(text);

        if(parsed != null)
        {
            if(parsed.equals(value))
            {
                return true;
            }

            for(PatchedTalkgroup talkgroup: patched)
            {
                if(talkgroup.value() == parsed)
                {
                    return true;
                }
            }
        }

        return formatted != null && formatted.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT));
    }

    /**
     * Indicates if the identifier value or fully qualified address matches the alias identifiers
     */
    private boolean matchesAlias(Protocol protocol, Long value, String fullyQualifiedAddress)
    {
        if(fullyQualifiedAddress != null)
        {
            for(FullyQualifiedAddress address: fullyQualified)
            {
                if(address.protocol() == protocol && address.address().equals(fullyQualifiedAddress))
                {
                    return true;
                }
            }

            return false;
        }

        if(value != null)
        {
            for(ValueRange range: ranges)
            {
                if(range.contains(protocol, value))
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public String toString()
    {
        return isAlias() ? aliasName + " (" + aliasList + ")" : text;
    }
}

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
package io.github.dsheirer.gui.calls;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.PatchedTalkgroup;

/**
 * Looks up the current aliases for the TO and FROM identifiers of a stored call, using the call's alias list from the
 * alias model.  Calls are aliased with the latest alias values, so historical calls pick up alias changes.
 */
public class CallAliasResolver
{
    private final AliasModel mAliasModel;

    /**
     * Constructs an instance
     * @param aliasModel with the current aliases, or null to disable alias lookup
     */
    public CallAliasResolver(AliasModel aliasModel)
    {
        mAliasModel = aliasModel;
    }

    private AliasList getAliasList(CallRecord call)
    {
        if(mAliasModel == null || call == null || call.getAliasList() == null || call.getAliasList().isEmpty())
        {
            return null;
        }

        return mAliasModel.getAliasList(call.getAliasList());
    }

    /**
     * Current alias for the TO talkgroup, patch group or radio of the call, or null.
     */
    public Alias getToAlias(CallRecord call)
    {
        AliasList aliasList = getAliasList(call);

        if(aliasList == null || call.getToValue() == null || call.getToValue() > Integer.MAX_VALUE)
        {
            return null;
        }

        int value = call.getToValue().intValue();

        if(call.isToTalkgroup())
        {
            return aliasList.getTalkgroupAlias(call.getProtocolValue(), value, call.getToFullyQualified());
        }

        if(CallRecord.FORM_RADIO.equals(call.getToForm()))
        {
            return aliasList.getRadioAlias(call.getProtocolValue(), value, call.getToFullyQualified());
        }

        return null;
    }

    /**
     * Current alias for the FROM radio of the call, or null.
     */
    public Alias getFromAlias(CallRecord call)
    {
        AliasList aliasList = getAliasList(call);

        if(aliasList == null || !call.isFromRadio() || call.getFromValue() == null ||
            call.getFromValue() > Integer.MAX_VALUE)
        {
            return null;
        }

        return aliasList.getRadioAlias(call.getProtocolValue(), call.getFromValue().intValue(),
            call.getFromFullyQualified());
    }

    /**
     * Talkgroups patched into the TO patch group, with their current alias names, e.g. "1001 Fire Dispatch, 1002",
     * or null when the call isn't a patch group call.
     */
    public String getPatchedTalkgroups(CallRecord call)
    {
        if(call == null || call.getPatchedTalkgroups().isEmpty())
        {
            return null;
        }

        AliasList aliasList = getAliasList(call);
        StringBuilder sb = new StringBuilder();

        for(PatchedTalkgroup talkgroup: call.getPatchedTalkgroups())
        {
            if(sb.length() > 0)
            {
                sb.append(", ");
            }

            sb.append(talkgroup.value());

            Alias alias = aliasList != null && talkgroup.value() <= Integer.MAX_VALUE ?
                aliasList.getTalkgroupAlias(call.getProtocolValue(), (int)talkgroup.value(),
                    talkgroup.fullyQualified()) : null;

            if(alias != null)
            {
                sb.append(" ").append(alias.getName());
            }
        }

        return sb.toString();
    }

    /**
     * Name of the current TO alias, or null
     */
    public String getToAliasName(CallRecord call)
    {
        Alias alias = getToAlias(call);
        return alias != null ? alias.getName() : null;
    }

    /**
     * Name of the current FROM alias, or null
     */
    public String getFromAliasName(CallRecord call)
    {
        Alias alias = getFromAlias(call);
        return alias != null ? alias.getName() : null;
    }
}

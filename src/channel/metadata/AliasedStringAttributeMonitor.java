/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package channel.metadata;

import alias.Alias;
import alias.AliasList;
import alias.id.AliasIDType;
import sample.Listener;

import static alias.id.AliasIDType.*;

public class AliasedStringAttributeMonitor extends AttributeMonitor<String>
{
    private static final AliasIDType[] VALID_ID_TYPES =  {ESN, FLEETSYNC, MDC1200, MIN, MPT1327, SITE, TALKGROUP};

    private AliasList mAliasList;
    private AliasIDType mAliasIDType;

    /**
     * Provides monitoring of a String attribute's values with occurrence heuristics enabled and alias lookup with each
     * change in the attribute's value.
     *
     * @param attribute to monitor
     * @param listener to receive change requests
     * @param aliasList to lookup alias from
     * @param aliasIDType to determine correct alias lookup method in alias list
     */
    public AliasedStringAttributeMonitor(Attribute attribute, Listener<AttributeChangeRequest> listener, AliasList aliasList,
                                         AliasIDType aliasIDType)
    {
        super(attribute, listener);

        if(!isValidIDType(aliasIDType))
        {
            throw new IllegalArgumentException("Alias ID Type [" + aliasIDType.name() + "] is not supported");
        }
        mAliasList = aliasList;
        mAliasIDType = aliasIDType;
    }

    /**
     * Indicates if the alias ID type is a supported/valid type for this monitor.
     */
    public static boolean isValidIDType(AliasIDType type)
    {
        for(AliasIDType validType: VALID_ID_TYPES)
        {
            if(type == validType)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Lookup of the alias that corresponds to the value's alias ID type
     */
    @Override
    protected Alias getAlias()
    {
        Alias alias = null;

        if(mAliasList != null)
        {
            if(hasValue())
            {
                switch(mAliasIDType)
                {
                    case ESN:
                        alias = mAliasList.getESNAlias(getValue());
                        break;
                    case FLEETSYNC:
                        alias = mAliasList.getFleetsyncAlias(getValue());
                        break;
                    case MDC1200:
                        alias = mAliasList.getMDC1200Alias(getValue());
                        break;
                    case MIN:
                        alias = mAliasList.getMobileIDNumberAlias(getValue());
                        break;
                    case MPT1327:
                        alias = mAliasList.getMPT1327Alias(getValue());
                        break;
                    case SITE:
                        alias = mAliasList.getSiteID(getValue());
                        break;
                    case TALKGROUP:
                        alias = mAliasList.getTalkgroupAlias(getValue());
                        break;
                }
            }
        }

        return alias;
    }
}

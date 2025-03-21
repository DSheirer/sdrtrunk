/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.identifier.alias;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Talker alias cache manager.  Collects observed talker aliases and inserts them into an identifier collection when
 * the corresponding radio is active in a FROM role.
 *
 * This implementation is thread safe and is intended to be used across control and traffic channels.
 */
public class TalkerAliasManager
{
    private Map<Integer,TalkerAliasIdentifier> mAliasMap = new ConcurrentHashMap<>();

    /**
     * Constructs an instance
     */
    public TalkerAliasManager()
    {
    }

    /**
     * Updates the alias for the
     * @param identifier
     * @param alias
     */
    public void update(RadioIdentifier identifier, TalkerAliasIdentifier alias)
    {
        if(identifier.getRole() == Role.FROM)
        {
            mAliasMap.put(identifier.getValue(), alias);
        }
    }

    /**
     * Indicates if an alias exists for the identifier
     * @param radioIdentifier to test
     * @return true if an alias exists.
     */
    public boolean hasAlias(RadioIdentifier radioIdentifier)
    {
        return mAliasMap.containsKey(radioIdentifier.getValue());
    }

    /**
     * Enriches the immutable identifier collection by detecting a radio identifier with the FROM role, lookup a
     * matching alias, and insert the alias into a new mutable identifier collection.
     * @param originalIC to enrich
     * @return enriched identifier collection or the original identifier collection if we don't have an alias.
     */
    public synchronized IdentifierCollection enrich(IdentifierCollection originalIC)
    {
        Identifier fromRadio = originalIC.getFromIdentifier();

        if(fromRadio instanceof RadioIdentifier rid)
        {
            TalkerAliasIdentifier alias = mAliasMap.get(rid.getValue());

            if(alias != null)
            {
                MutableIdentifierCollection enrichedIC = new MutableIdentifierCollection(originalIC.getIdentifiers());
                enrichedIC.update(alias);
                return enrichedIC;
            }
        }

        return originalIC;
    }

    /**
     * Enriches the mutable identifier collection by detecting a radio identifier with the FROM role, lookup a
     * matching alias, and insert the alias into the mutable identifier collection argument.
     * @param mic to enrich
     */
    public synchronized void enrichMutable(MutableIdentifierCollection mic)
    {
        Identifier fromRadio = mic.getFromIdentifier();

        if(fromRadio instanceof RadioIdentifier rid)
        {
            TalkerAliasIdentifier alias = mAliasMap.get(rid.getValue());

            if(alias != null)
            {
                mic.update(alias);
            }
        }
    }

    /**
     * Creates a summary listing of talker aliases
     * @return summary.
     */
    public synchronized String getAliasSummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Active System Radio Aliases\n");
        sb.append("  Radio\tTalker Alias (TA-)\n");
        List<Integer> radios = new ArrayList<>(mAliasMap.keySet());

        if(radios.size() > 0)
        {
            Collections.sort(radios);
            for(Integer radio : radios)
            {
                sb.append("  ").append(radio);
                sb.append("\t").append(mAliasMap.get(radio));
                sb.append("\n");
            }
        }
        else
        {
            sb.append("  None\n");
        }

        return sb.toString();
    }
}

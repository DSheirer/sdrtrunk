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

package io.github.dsheirer.identifier.alias;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.event.ActivitySummary;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Talker alias cache manager.  Collects observed talker aliases and inserts them into an identifier collection when
 * the corresponding radio is active in a FROM role.
 *
 * This implementation is thread safe and is intended to be used across control and traffic channels.
 */
public class TalkerAliasManager
{
    private Map<Integer,TalkerAliasIdentifier> mAliasMap = new ConcurrentHashMap<>();
    private volatile BiConsumer<RadioIdentifier,TalkerAliasIdentifier> mTalkerAliasListener;

    /**
     * Constructs an instance
     */
    public TalkerAliasManager()
    {
    }

    /**
     * Sets an optional listener to receive each talker alias that is assigned to a radio, e.g. to automatically add
     * talker aliases to an alias list.  The listener is invoked on the calling decoder thread.
     * @param listener to receive radio and talker alias updates, or null to clear the listener.
     */
    public void setTalkerAliasListener(BiConsumer<RadioIdentifier,TalkerAliasIdentifier> listener)
    {
        mTalkerAliasListener = listener;
    }

    /**
     * Updates the alias for the radio identifier
     *
     * @param identifier to update
     * @param alias to assign to the radio identifier
     */
    public void update(RadioIdentifier identifier, TalkerAliasIdentifier alias)
    {
        if(identifier.getRole() == Role.FROM)
        {
            mAliasMap.put(identifier.getValue(), alias);

            BiConsumer<RadioIdentifier,TalkerAliasIdentifier> listener = mTalkerAliasListener;

            if(listener != null && alias != null)
            {
                listener.accept(identifier, alias);
            }
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
     * Current FROM radio in the identifier collection
     * @param identifierCollection to inspect
     * @return FROM radio identifier, or null
     */
    public static Identifier getFromRadio(IdentifierCollection identifierCollection)
    {
        return identifierCollection.getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);
    }

    /**
     * Adds a talker alias section to the activity summary listing each radio and talker alias in radio order.
     * @param summary to add to
     */
    public synchronized void addAliasSummary(ActivitySummary summary)
    {
        summary.section("Talker Aliases");
        ActivitySummary.Table table = summary.table("Radio", "Talker Alias");
        List<Integer> radios = new ArrayList<>(mAliasMap.keySet());
        Collections.sort(radios);

        for(Integer radio : radios)
        {
            TalkerAliasIdentifier alias = mAliasMap.get(radio);
            table.row(radio, alias != null ? alias.getValue() : null);
        }
    }
}

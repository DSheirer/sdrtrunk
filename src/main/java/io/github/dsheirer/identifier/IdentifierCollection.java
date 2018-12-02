/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.identifier;

import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * (Immutable) Collection of identifiers with convenient accessor methods
 *
 * @see MutableIdentifierCollection for the mutable version of this class
 */
public class IdentifierCollection
{
    protected List<Identifier> mIdentifiers = new ArrayList<>();
    protected AliasListConfigurationIdentifier mAliasListConfigurationIdentifier;
    private boolean mUpdated = false;

    /**
     * Constructs an empty identifier collection
     */
    public IdentifierCollection()
    {
    }

    public IdentifierCollection(Collection<Identifier> identifiers)
    {
        mIdentifiers.addAll(identifiers);

        for(Identifier identifier: identifiers)
        {
            if(identifier instanceof AliasListConfigurationIdentifier)
            {
                mAliasListConfigurationIdentifier = (AliasListConfigurationIdentifier)identifier;
            }
        }
    }

    /**
     * Indicates if this identifier collection contains an updated list of identifiers.
     */
    public boolean isUpdated()
    {
        return mUpdated;
    }

    protected void setUpdated(boolean updated)
    {
        mUpdated = updated;
    }

    /**
     * Alias List configuration identifier containing the name of the alias list for this collection.
     * @return alias list or null
     */
    public AliasListConfigurationIdentifier getAliasListConfiguration()
    {
        return mAliasListConfigurationIdentifier;
    }

    /**
     * Indicates if this collection has an alias list specified.
     */
    public boolean hasAliasListConfiguration()
    {
        return mAliasListConfigurationIdentifier != null;
    }

    /**
     * Immutable list of identifiers contained in this collection
     */
    public List<Identifier> getIdentifiers()
    {
        return Collections.unmodifiableList(mIdentifiers);
    }

    /**
     * Get a list of identifiers by identifier class from this collection.
     *
     * @param identifierClass to match
     * @return list of zero or more identifiers
     */
    public List<Identifier> getIdentifiers(IdentifierClass identifierClass)
    {
        List<Identifier> identifiers = new ArrayList<>();

        for(Identifier identifier : mIdentifiers)
        {
            if(identifier.getIdentifierClass() == identifierClass)
            {
                identifiers.add(identifier);
            }
        }

        return identifiers;
    }

    /**
     * Get a list of identifiers by form from this collection.
     *
     * @param form to match
     * @return list of zero or more identifiers
     */
    public List<Identifier> getIdentifiers(Form form)
    {
        List<Identifier> identifiers = new ArrayList<>();

        for(Identifier identifier : mIdentifiers)
        {
            if(identifier.getForm() == form)
            {
                identifiers.add(identifier);
            }
        }

        return identifiers;
    }

    /**
     * Get a list of identifiers by role from this collection.
     *
     * @param role to match
     * @return list of zero or more identifiers
     */
    public List<Identifier> getIdentifiers(Role role)
    {
        List<Identifier> identifiers = new ArrayList<>();

        for(Identifier identifier : mIdentifiers)
        {
            if(identifier.getRole() == role)
            {
                identifiers.add(identifier);
            }
        }

        return identifiers;
    }

    /**
     * Get a list of identifiers by identifier class and role from this collection.
     *
     * @param identifierClass to match
     * @param role to match
     * @return list of zero or more identifiers
     */
    public List<Identifier> getIdentifiers(IdentifierClass identifierClass, Role role)
    {
        List<Identifier> identifiers = new ArrayList<>();

        for(Identifier identifier : mIdentifiers)
        {
            if(identifier.getIdentifierClass() == identifierClass && identifier.getRole() == role)
            {
                identifiers.add(identifier);
            }
        }

        return identifiers;
    }

    /**
     * Get a list of identifiers by identifier class and form from this collection.
     *
     * @param identifierClass to match
     * @param form to match
     * @return list of zero or more identifiers
     */
    public List<Identifier> getIdentifiers(IdentifierClass identifierClass, Form form)
    {
        List<Identifier> identifiers = new ArrayList<>();

        for(Identifier identifier : mIdentifiers)
        {
            if(identifier.getIdentifierClass() == identifierClass && identifier.getForm() == form)
            {
                identifiers.add(identifier);
            }
        }

        return identifiers;
    }

    /**
     * Get the single identifier by identifier class, form and role from this collection.
     *
     * @param identifierClass to match
     * @param form to match
     * @param role to match
     * @return matching identifier or null
     */
    public Identifier getIdentifier(IdentifierClass identifierClass, Form form, Role role)
    {
        for(Identifier identifier : mIdentifiers)
        {
            if(identifier.getIdentifierClass() == identifierClass &&
                identifier.getForm() == form &&
                identifier.getRole() == role)
            {
                return identifier;
            }
        }

        return null;
    }
}

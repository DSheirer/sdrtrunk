/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.identifier;

import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (Immutable) Collection of identifiers with convenient accessor methods
 *
 * @see MutableIdentifierCollection for the mutable version of this class
 */
public class IdentifierCollection
{
    private final static Logger mLog = LoggerFactory.getLogger(IdentifierCollection.class);
    protected List<Identifier> mIdentifiers = new CopyOnWriteArrayList<>();
    protected AliasListConfigurationIdentifier mAliasListConfigurationIdentifier;
    private int mTimeslot = 0;

    /**
     * Constructs an empty identifier collection for the specified timeslot
     * @param timeslot
     */
    public IdentifierCollection(int timeslot)
    {
        mTimeslot = timeslot;
    }

    /**
     * Constructs an empty identifier collection
     */
    public IdentifierCollection()
    {
        this(0);
    }

    public IdentifierCollection(Collection<Identifier> identifiers)
    {
        this(identifiers, 0);
    }

    public IdentifierCollection(Collection<Identifier> identifiers, int timeslot)
    {
        for(Identifier identifier: identifiers)
        {
            if(identifier == null)
            {
                throw new IllegalArgumentException("Identifier cannot be null");
            }

            mIdentifiers.add(identifier);

            if(identifier instanceof AliasListConfigurationIdentifier)
            {
                mAliasListConfigurationIdentifier = (AliasListConfigurationIdentifier)identifier;
            }
        }
    }

    public int getTimeslot()
    {
        return mTimeslot;
    }

    public void setTimeslot(int timeslot)
    {
        mTimeslot = timeslot;
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
     * Indicates if this collection has no identifiers
     */
    public boolean isEmpty()
    {
        return mIdentifiers.isEmpty();
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
            try
            {
                if(identifier.getRole() == role)
                {
                    identifiers.add(identifier);
                }
            }
            catch(NullPointerException npe)
            {
                StringBuilder sb = new StringBuilder();
                for(Identifier i : mIdentifiers)
                {
                    if(i == null)
                    {
                        sb.append("Identifier: (null)").append("\n");
                    }
                    else
                    {
                        sb.append("Identifier: ").append(i).append(" Class:").append(i.getClass()).append("\n");
                    }
                }
                mLog.warn("An identifier in a collection is somehow null ...\n" + sb);
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

    /**
     * Indicates if this collection has an identifier that matches the specified identiifer.
     * @param toCheck identifier
     * @return true if the identifier already exists in the collection.
     */
    public boolean hasIdentifier(Identifier toCheck)
    {
        if(toCheck == null)
        {
            return false;
        }

        for(Identifier identifier : mIdentifiers)
        {
            if(identifier.equals(toCheck))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the first entity identifier in this collection that is assigned a FROM role
     */
    public Identifier getFromIdentifier()
    {
        Identifier from = getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);

        if(from != null)
        {
            return from;
        }

        List<Identifier> identifiers = getIdentifiers(Form.TELEPHONE_NUMBER);

        if(!identifiers.isEmpty())
        {
            return identifiers.get(0);
        }

        List<Identifier> fromIdentifiers = getIdentifiers(Role.FROM);

        if(!fromIdentifiers.isEmpty())
        {
            return fromIdentifiers.get(0);
        }

        return null;
    }

    /**
     * Returns the first entity identifier in this collection that is assigned a TO role
     */
    public Identifier getToIdentifier()
    {
        Identifier to = getIdentifier(IdentifierClass.USER, Form.PATCH_GROUP, Role.TO);

        if(to != null)
        {
            return to;
        }

        to = getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.TO);

        if(to != null)
        {
            return to;
        }

        to = getIdentifier(IdentifierClass.USER, Form.RADIO, Role.TO);

        if(to != null)
        {
            return to;
        }

        List<Identifier> toIdentifiers = getIdentifiers(Role.TO);

        for(Identifier identifier: toIdentifiers)
        {
            if(identifier.getForm() != Form.ENCRYPTION_KEY)
            {
                return identifier;
            }
        }

        return to;
    }

    /**
     * Returns an encryption key identiier or null.
     * @return key or null.
     */
    public Identifier getEncryptionIdentifier()
    {
        return getIdentifier(IdentifierClass.USER, Form.ENCRYPTION_KEY, Role.ANY);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Identifier Collection - Timeslot:").append(mTimeslot).append("\n");
        for(Identifier identifier: getIdentifiers())
        {
            sb.append("\t").append(identifier.toString());
            sb.append("\t{").append(identifier.getIdentifierClass().name()).append("|")
                .append(identifier.getForm().name()).append("|")
                .append(identifier.getRole().name()).append("}");
            sb.append("\t").append(identifier.getClass()).append("\n");
        }
        return sb.toString();
    }
}

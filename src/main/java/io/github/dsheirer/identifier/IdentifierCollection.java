/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
     * Returns the first identifier in this collection that is assigned a FROM role
     */
    public Identifier getFromIdentifier()
    {
        List<Identifier> fromIdentifiers = getFromIdentifiers();
        if(!fromIdentifiers.isEmpty())
        {
            return fromIdentifiers.get(0);
        }

        return null;
    }

    /**
     * @return list of 'from' identifiers with radios first, then all others
     */
    public List<Identifier> getFromIdentifiers()
    {
        List<Identifier> from = new ArrayList<>();
        List<Identifier> radio = new ArrayList<>();
        List<Identifier> other = new ArrayList<>();
        
        for(Identifier identifier: getIdentifiers(Role.FROM))
        {
            switch(identifier.getForm())
            {
                case RADIO:
                    radio.add(identifier);
                    break;

                default:
                    other.add(identifier);
            }
        }

        from.addAll(radio);
        from.addAll(other);

        return from;
    }

    /**
     * Returns the first identifier in this collection that is assigned a TO role
     */
    public Identifier getToIdentifier()
    {
        List<Identifier> toIdentifiers = getToIdentifiers();
        if(!toIdentifiers.isEmpty())
        {
            return toIdentifiers.get(0);
        }

        return null;
    }

    /**
     * @return list of 'to' identifiers with patch groups first, followed by talkgroups, otherwise all others
     */
    public List<Identifier> getToIdentifiers()
    {
        List<Identifier> to = new ArrayList<>();
        List<Identifier> patch = new ArrayList<>();
        List<Identifier> talkgroup = new ArrayList<>();
        List<Identifier> radio = new ArrayList<>();
        List<Identifier> other = new ArrayList<>();
        
        for(Identifier identifier: getIdentifiers(Role.TO))
        {
            switch(identifier.getForm())
            {
                case PATCH_GROUP:
                    patch.add(identifier);
                    break;

                case RADIO:
                    radio.add(identifier);
                    break;

                case TALKGROUP:
                    talkgroup.add(identifier);
                    break;

                default:
                    other.add(identifier);
            }
        }

        to.addAll(patch);
        to.addAll(talkgroup);
        if((patch.isEmpty())&&(talkgroup.isEmpty()))
        {
            /**
             * DMR – specifically TRBO variants - sometimes has a radio ID as the destination,
             * in addition to talkgroup. To avoid confusion, only send the radio ID when there
             * are no patch/talk groups.
             */
            to.addAll(radio);
        }
        to.addAll(other);

        return to;
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

/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
import io.github.dsheirer.sample.Listener;

import java.util.Collection;
import java.util.Iterator;

/**
 * Identifier collection with methods for changing or updating managed identifiers
 */
public class MutableIdentifierCollection extends IdentifierCollection implements IdentifierUpdateProvider,
    Listener<IdentifierUpdateNotification>
{
    private Listener<IdentifierUpdateNotification> mListener;

    public MutableIdentifierCollection()
    {
    }

    public MutableIdentifierCollection(Collection<Identifier> identifiers)
    {
        for(Identifier identifier: identifiers)
        {
            update(identifier);
        }
    }

    /**
     * Broadcasts all of the currently held identifiers to the registered listener
     */
    public void broadcastIdentifiers()
    {
        for(Identifier identifier : getIdentifiers())
        {
            notifyAdd(identifier);
        }
    }

    /**
     * Registers a listener to be notified when identifiers are added to or removed from this collection
     */
    @Override
    public void setIdentifierUpdateListener(Listener<IdentifierUpdateNotification> listener)
    {
        mListener = listener;
    }

    /**
     * Unregisters the listener from receiving identifier update notifications
     */
    @Override
    public void removeIdentifierUpdateListener()
    {
        mListener = null;
    }

    /**
     * Notifies a registered listener that the identifier has been added to this collection
     */
    private void notifyAdd(Identifier identifier)
    {
        setUpdated(true);
        if(mListener != null)
        {
            mListener.receive(new IdentifierUpdateNotification(identifier, IdentifierUpdateNotification.Operation.ADD));
        }
    }

    /**
     * Notifies a registered listener that the identifier has been removed from this collection
     */
    private void notifyRemove(Identifier identifier)
    {
        setUpdated(true);
        if(mListener != null)
        {
            mListener.receive(new IdentifierUpdateNotification(identifier,
                IdentifierUpdateNotification.Operation.REMOVE));
        }
    }

    /**
     * Adds the identifier to this collection if not already contained in this collection.
     *
     * @param identifier to add
     */
    private void add(Identifier identifier)
    {
        if(identifier.isValid() && !mIdentifiers.contains(identifier))
        {
            mIdentifiers.add(identifier);
            notifyAdd(identifier);
        }

        //Retain a reference to the alias list identifier separately so that it can be accessed quickly.
        if(identifier instanceof AliasListConfigurationIdentifier)
        {
            mAliasListConfigurationIdentifier = (AliasListConfigurationIdentifier)identifier;
        }
    }

    /**
     * Adds the identifier to this collection if not already contained in this collection.  Does NOT broadcast an
     * update notification.
     *
     * @param identifier to add
     */
    private void silentAdd(Identifier identifier)
    {
        if(identifier.isValid() && !mIdentifiers.contains(identifier))
        {
            mIdentifiers.add(identifier);
        }

        //Retain a reference to the alias list identifier separately so that it can be accessed quickly.
        if(identifier instanceof AliasListConfigurationIdentifier)
        {
            mAliasListConfigurationIdentifier = (AliasListConfigurationIdentifier)identifier;
        }
    }

    /**
     * Removes the identifier from this collection
     */
    public void remove(Identifier identifier)
    {
        if(mIdentifiers.remove(identifier))
        {
            notifyRemove(identifier);
        }

        //Remove the reference to the alias list identifier.
        if(identifier instanceof AliasListConfigurationIdentifier)
        {
            mAliasListConfigurationIdentifier = null;
        }
    }

    /**
     * Removes the identifier from this collection and does NOT broadcast an update notification
     */
    public void silentRemove(Identifier identifier)
    {
        mIdentifiers.remove(identifier);

        //Remove the reference to the alias list identifier.
        if(identifier instanceof AliasListConfigurationIdentifier)
        {
            mAliasListConfigurationIdentifier = null;
        }
    }

    /**
     * Adds the identifier only if the value is different from the existing identifiers(s).  If
     * there are multiple matching identifiers, then they are all removed and replaced by this
     * single identifier.
     *
     * @param identifier
     */
    public void update(Identifier identifier)
    {
        if(identifier != null)
        {
            Identifier existing = getIdentifier(identifier.getIdentifierClass(),
                identifier.getForm(), identifier.getRole());

            if(existing != null)
            {
                if(!existing.equals(identifier))
                {
                    remove(existing);
                    add(identifier);
                }
            }
            else
            {
                add(identifier);
            }
        }
    }

    /**
     * Adds the identifier only if the value is different from the existing identifiers(s).  If
     * there are multiple matching identifiers, then they are all removed and replaced by this
     * single identifier.
     *
     * @param identifier
     */
    public void silentUpdate(Identifier identifier)
    {
        if(identifier != null)
        {
            Identifier existing = getIdentifier(identifier.getIdentifierClass(), identifier.getForm(), identifier.getRole());

            if(existing != null)
            {
                if(!existing.equals(identifier))
                {
                    silentRemove(existing);
                    silentAdd(identifier);
                }
            }
            else
            {
                silentAdd(identifier);
            }
        }
    }

    /**
     * Updates all identifiers
     */
    public void update(Collection<Identifier> identifiers)
    {
        for(Identifier identifier : identifiers)
        {
            update(identifier);
        }
    }

    /**
     * Removes/clears all identifiers from the collection
     */
    public void clear()
    {
        Iterator<Identifier> it = mIdentifiers.iterator();

        while(it.hasNext())
        {
            notifyRemove(it.next());
            it.remove();
        }
    }

    /**
     * Removes all identifiers of the specified identifier class
     */
    public void remove(IdentifierClass identifierClass)
    {
        Iterator<Identifier> it = mIdentifiers.iterator();

        Identifier next = null;

        while(it.hasNext())
        {
            next = it.next();

            if(next.getIdentifierClass() == identifierClass)
            {
                it.remove();
                notifyRemove(next);
            }
        }
    }

    /**
     * Removes all identifiers of the specified form
     */
    public void remove(Form form)
    {
        Iterator<Identifier> it = mIdentifiers.iterator();

        Identifier next = null;

        while(it.hasNext())
        {
            next = it.next();

            if(next.getForm() == form)
            {
                it.remove();
                notifyRemove(next);
            }
        }
    }

    /**
     * Removes all identifiers of the specified role
     */
    public void remove(Role role)
    {
        Iterator<Identifier> it = mIdentifiers.iterator();

        Identifier next = null;

        while(it.hasNext())
        {
            next = it.next();

            if(next.getRole() == role)
            {
                it.remove();
                notifyRemove(next);
            }
        }
    }

    /**
     * Removes all identifiers of the specified identifier class, form and role
     */
    public void remove(IdentifierClass identifierClass, Form form, Role role)
    {
        Iterator<Identifier> it = mIdentifiers.iterator();

        Identifier next = null;

        while(it.hasNext())
        {
            next = it.next();

            if(next.getIdentifierClass() == identifierClass && next.getForm() == form && next.getRole() == role)
            {
                it.remove();
                notifyRemove(next);
            }
        }
    }

    /**
     * Removes all identifiers of the specified identifier class and role
     */
    public void remove(IdentifierClass identifierClass, Role role)
    {
        Iterator<Identifier> it = mIdentifiers.iterator();

        Identifier next = null;

        while(it.hasNext())
        {
            next = it.next();

            if(next.getIdentifierClass() == identifierClass && next.getRole() == role)
            {
                it.remove();
                notifyRemove(next);
            }
        }
    }

    /**
     * Implements the listener interface to receive notifications of identifier updates.  This allows an
     * identifier collection to maintain a synchronized state with a remote identifier collection.
     *
     * @param identifierUpdateNotification to add or remove an identifier
     */
    @Override
    public void receive(IdentifierUpdateNotification identifierUpdateNotification)
    {
        if(identifierUpdateNotification.isAdd())
        {
            update(identifierUpdateNotification.getIdentifier());
        }
        else if(identifierUpdateNotification.isRemove())
        {
            remove(identifierUpdateNotification.getIdentifier());
        }
        else if(identifierUpdateNotification.isSilentAdd())
        {
            silentUpdate(identifierUpdateNotification.getIdentifier());
        }
        else if(identifierUpdateNotification.isSilentRemove())
        {
            silentRemove(identifierUpdateNotification.getIdentifier());
        }
    }

    /**
     * Creates a copy of this collection and resets the updated flag so that future invocations of this
     * method will return a copy with the updated flag set to false.
     */
    public IdentifierCollection copyOf()
    {
        IdentifierCollection copy = new IdentifierCollection(getIdentifiers());
        copy.setUpdated(isUpdated());
        setUpdated(false);
        return copy;
    }
}

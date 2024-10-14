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
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identifier collection with methods for changing or updating managed identifiers
 */
public class MutableIdentifierCollection extends IdentifierCollection implements IdentifierUpdateProvider,
    Listener<IdentifierUpdateNotification>
{
    private final static Logger mLog = LoggerFactory.getLogger(MutableIdentifierCollection.class);
    private Listener<IdentifierUpdateNotification> mListener;

    public MutableIdentifierCollection(int timeslot)
    {
        super(timeslot);
    }

    public MutableIdentifierCollection()
    {
        super(0);
    }

    public MutableIdentifierCollection(Collection<Identifier> identifiers)
    {
        this(identifiers, 0);
    }

    public MutableIdentifierCollection(Collection<Identifier> identifiers, int timeslot)
    {
        super(timeslot);

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
        if(mListener != null)
        {
            mListener.receive(new IdentifierUpdateNotification(identifier, IdentifierUpdateNotification.Operation.ADD, getTimeslot()));
        }
    }

    /**
     * Notifies a registered listener that the identifier has been removed from this collection
     */
    private void notifyRemove(Identifier identifier)
    {
        if(mListener != null)
        {
            mListener.receive(new IdentifierUpdateNotification(identifier,
                IdentifierUpdateNotification.Operation.REMOVE, getTimeslot()));
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
                //Always replace a radio identifier with a fully qualified variant of itself
                else if(existing instanceof  RadioIdentifier &&
                        !(existing instanceof FullyQualifiedRadioIdentifier) &&
                        identifier instanceof FullyQualifiedRadioIdentifier &&
                        existing.getValue().equals(identifier.getValue()))
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
        List<Identifier> identifiers = new ArrayList<>();

        for(Identifier identifier: identifiers)
        {
            remove(identifier);
        }
    }

    /**
     * Removes all identifiers of the specified identifier class
     */
    public void remove(IdentifierClass identifierClass)
    {
        List<Identifier> identifiers = new ArrayList<>(mIdentifiers);

        for(Identifier identifier: identifiers)
        {
            if(identifier.getIdentifierClass() == identifierClass)
            {
                remove(identifier);
            }
        }
    }

    /**
     * Removes all identifiers of the specified form
     */
    public void remove(Form form)
    {
        List<Identifier> identifiers = new ArrayList<>(mIdentifiers);

        for(Identifier identifier: identifiers)
        {
            if(identifier.getForm() == form)
            {
                remove(identifier);
            }
        }
    }

    /**
     * Removes all identifiers of the specified role
     */
    public void remove(Role role)
    {
        List<Identifier> identifiers = new ArrayList<>(mIdentifiers);

        for(Identifier identifier: identifiers)
        {
            if(identifier.getRole() == role)
            {
                remove(identifier);
            }
        }
    }

    /**
     * Removes all identifiers of the specified identifier class, form and role
     */
    public void remove(IdentifierClass identifierClass, Form form, Role role)
    {
        List<Identifier> identifiers = new ArrayList<>(mIdentifiers);

        for(Identifier identifier: identifiers)
        {
            if(identifier.getIdentifierClass() == identifierClass &&
                identifier.getForm() == form &&
                identifier.getRole() == role)
            {
                remove(identifier);
            }
        }
    }

    /**
     * Removes all identifiers of the specified identifier class and role
     */
    public void remove(IdentifierClass identifierClass, Role role)
    {
        List<Identifier> identifiers = new ArrayList<>(mIdentifiers);

        for(Identifier identifier: identifiers)
        {
            if(identifier.getIdentifierClass() == identifierClass && identifier.getRole() == role)
            {
                remove(identifier);
            }
        }
    }

    /**
     * Implements the listener interface to receive notifications of identifier updates.  This allows an
     * identifier collection to maintain a synchronized state with a remote identifier collection.
     *
     * Note: this method performs a silent add/update/remove on on the local collection and does not rebroadcast
     * the update to a registered listener in order to prevent infinite loop updates.
     *
     * @param identifierUpdateNotification to add or remove an identifier
     */
    @Override
    public void receive(IdentifierUpdateNotification identifierUpdateNotification)
    {
        //Only process notifications that match this timeslot
        if(identifierUpdateNotification.getTimeslot() == getTimeslot())
        {
            if(identifierUpdateNotification.isAdd() || identifierUpdateNotification.isSilentAdd())
            {
                silentUpdate(identifierUpdateNotification.getIdentifier());
            }
            else if(identifierUpdateNotification.isRemove() || identifierUpdateNotification.isSilentRemove())
            {
                silentRemove(identifierUpdateNotification.getIdentifier());
            }
        }
    }

    /**
     * Creates a copy of this collection and resets the updated flag so that future invocations of this
     * method will return a copy with the updated flag set to false.
     */
    public IdentifierCollection copyOf()
    {
        IdentifierCollection copy = new IdentifierCollection(getIdentifiers(), getTimeslot());
        return copy;
    }
}

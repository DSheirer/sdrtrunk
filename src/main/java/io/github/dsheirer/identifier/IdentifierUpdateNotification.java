/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.identifier;

/**
 * Request to add or update an identifier
 */
public class IdentifierUpdateNotification
{
    private Identifier<?> mIdentifier;
    private Operation mOperation;
    private int mTimeslot;

    /**
     * Constructs an identifier update notification
     * @param identifier that has been updated
     */
    public IdentifierUpdateNotification(Identifier<?> identifier, Operation operation, int timeslot)
    {
        mIdentifier = identifier;
        mOperation = operation;
        mTimeslot = timeslot;
    }

    public Identifier<?> getIdentifier()
    {
        return mIdentifier;
    }

    public Operation getOperation()
    {
        return mOperation;
    }

    public int getTimeslot()
    {
        return mTimeslot;
    }

    public boolean isAdd()
    {
        return mOperation == Operation.ADD;
    }

    public boolean isRemove()
    {
        return mOperation == Operation.REMOVE;
    }

    /**
     * Indicates that this update should not be rebroadcast by any of the consumers.
     */
    public boolean isSilentAdd()
    {
        return mOperation == Operation.SILENT_ADD;
    }

    /**
     * Indicates that this update should not be rebroadcast by any of the consumers.
     */
    public boolean isSilentRemove()
    {
        return mOperation == Operation.SILENT_REMOVE;
    }

    @Override
    public String toString()
    {
        return mOperation.name() + " - " + mIdentifier;
    }

    public enum Operation
    {
        ADD, REMOVE, SILENT_ADD, SILENT_REMOVE;
    }
}

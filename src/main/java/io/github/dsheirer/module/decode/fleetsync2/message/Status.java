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

package io.github.dsheirer.module.decode.fleetsync2.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.fleetsync2.identifier.FleetsyncStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Status message containing a numeric status indicator where the numeric value is pre-defined.
 */
public class Status extends Fleetsync2Message
{
    private static int[] STATUS = {21, 22, 23, 24, 25, 26, 27};

    private FleetsyncStatus mStatusIdentifier;
    private List<Identifier> mIdentifers;

    public Status(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    protected int getBlockCount()
    {
        return hasFleetExtensionFlag(getMessage()) ? 2 : 1;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("FROM:").append(getFromIdentifier()).append(" TO:").append(getToIdentifier());

        sb.append(" STATUS:").append(getStatus());

        if(hasEmergencyFlag(getMessage()))
        {
            sb.append(" EMERGENCY");
        }

        if(hasLoneWorkerFlag(getMessage()))
        {
            sb.append(" LONE WORKER");
        }

        if(hasPagingFlag(getMessage()))
        {
            sb.append(" PAGING");
        }

        if(hasAcknowledgeFlag(getMessage()))
        {
            sb.append(" ACKNOWLEDGE");
        }

        if(hasANIFlag(getMessage()))
        {
            sb.append(" ANI");
        }

        return sb.toString();
    }

    public FleetsyncStatus getStatus()
    {
        if(mStatusIdentifier == null)
        {
            mStatusIdentifier = FleetsyncStatus.createUserStatus(getMessage().getInt(STATUS));
        }

        return mStatusIdentifier;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifers == null)
        {
            mIdentifers = new ArrayList<>();
            mIdentifers.add(getFromIdentifier());
            mIdentifers.add(getToIdentifier());
            mIdentifers.add(getStatus());
        }

        return mIdentifers;
    }
}

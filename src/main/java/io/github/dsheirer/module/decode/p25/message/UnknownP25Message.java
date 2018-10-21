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

package io.github.dsheirer.module.decode.p25.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

/**
 * P25 Message with unknown or unrecognized data unit id
 */
public class UnknownP25Message extends P25Message
{
    private DataUnitID mDataUnitID;

    public UnknownP25Message(CorrectedBinaryMessage message, int nac, long timestamp, DataUnitID dataUnitID)
    {
        super(message, nac);
        mDataUnitID = dataUnitID;
    }

    @Override
    public DataUnitID getDUID()
    {
        return mDataUnitID;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }
}

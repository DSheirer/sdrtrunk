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

package io.github.dsheirer.module.decode.nxdn.layer3.mobility;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.DataWriteOption;
import java.util.List;

/**
 * Base class for header and acknowledge
 */
public abstract class DataWrite extends NXDNLayer3Message
{
    private static final IntField DATA_WRITE_OPTION = IntField.length8(OCTET_1);
    private DataWriteOption mDataWriteOption;
    private NXDNRadioIdentifier mSourceIdentifier;
    private NXDNRadioIdentifier mDestinationIdentifier;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type
     * @param ran
     * @param lich
     */
    public DataWrite(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    /**
     * Options for the data write
     */
    public DataWriteOption getDataWriteOption()
    {
        if(mDataWriteOption == null)
        {
            mDataWriteOption = new DataWriteOption(getMessage().getInt(DATA_WRITE_OPTION));
        }

        return mDataWriteOption;
    }

    /**
     * Source radio ID
     * @return source identifier.
     */
    public NXDNRadioIdentifier getSource()
    {
        if(mSourceIdentifier == null)
        {
            mSourceIdentifier = NXDNRadioIdentifier.createTypeDFrom(getMessage().getInt(IDENTIFIER_OCTET_3));
        }

        return mSourceIdentifier;
    }

    /**
     * Destination identifier
     * @return destination identifier
     */
    public NXDNRadioIdentifier getDestination()
    {
        if(mDestinationIdentifier == null)
        {
            mDestinationIdentifier = NXDNRadioIdentifier.createTypeDTo(getMessage().getInt(IDENTIFIER_OCTET_5));
        }

        return mDestinationIdentifier;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination());
    }
}

/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.broadcast;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.StationIDOption;
import java.util.Collections;
import java.util.List;

/**
 * Digital Station ID information (e.g. U.S. FCC Call sign)
 */
public class DigitalStationIDInformation extends NXDNLayer3Message
{
    private static final IntField OPTION = IntField.length8(OCTET_1);
    private StationIDOption mStationIDOption;
    private String mCharacters;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public DigitalStationIDInformation(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp, NXDNMessageType.BROADCAST_23_DIGITAL_STATION_ID_INFORMATION);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DIGITAL STATION ID ");
        sb.append(getStationIDOption());
        if(getStationIDOption().isComplete())
        {
            sb.append(" ID:").append(getCharacters());
        }
        else
        {
            sb.append(" FRAGMENT:").append(getCharacters());
        }

        return super.toString();
    }

    /**
     * Station ID option that specifies message sequencing and character length.
     */
    public StationIDOption getStationIDOption()
    {
        if(mStationIDOption == null)
        {
            mStationIDOption = new StationIDOption(getMessage().getInt(OPTION));
        }

        return mStationIDOption;
    }

    /**
     * Character fragment from this message.
     */
    public String getCharacters()
    {
        if(mCharacters == null)
        {
            StringBuilder sb = new StringBuilder();

            int pointer = OCTET_2;

            while(pointer < getMessage().size())
            {
                IntField field = IntField.length8(pointer);
                sb.append((char)(0xFF & getMessage().getInt(field)));
                pointer += 8;
            }

            mCharacters = sb.toString().trim();
        }

        return mCharacters;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}

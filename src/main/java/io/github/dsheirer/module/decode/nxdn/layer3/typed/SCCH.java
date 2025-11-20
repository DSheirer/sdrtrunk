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

package io.github.dsheirer.module.decode.nxdn.layer3.typed;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;

/**
 * Base SCCH information message
 */
public abstract class SCCH extends NXDNLayer3Message
{
    protected static final IntField STRUCTURE = IntField.length2(0);
    protected static final int AREA = 2;
    protected static final IntField REPEATER_1 = IntField.length5(3);
    protected static final IntField REPEATER_2 = IntField.length5(8);
    protected static final IntField IDENTIFIER = IntField.length11(13);

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran from the frame
     * @param lich from the frame
     */
    public SCCH(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public StringBuilder getMessageBuilder()
    {
        return super.getMessageBuilder().append("TYPE-D ").append(getArea()).append(mLICH).append(" ");
    }

    /**
     * Structure field indicates the message class for Information 1 - 4
     * @param message containing the structure field
     * @return value.
     */
    public static int getStructure(CorrectedBinaryMessage message)
    {
        return message.getInt(STRUCTURE);
    }

    /**
     * Area bit
     * @return true or false
     */
    public String getArea()
    {
        return "AREA:" + (getMessage().get(AREA) ? "1 " : "0 ");
    }

    /**
     * Repeater in Use field
     * @param message containing the repeater in use field
     * @return repeater
     */
    public static int getRepeater(CorrectedBinaryMessage message)
    {
        return message.getInt(REPEATER_1);
    }

    /**
     * Repeater number
     */
    public int getRepeater()
    {
        return getRepeater(getMessage());
    }

    /**
     * Repeater 2 value for certain messages
     */
    public int getRepeater2()
    {
        return getMessage().getInt(REPEATER_2);
    }

    /**
     * Home repeater value for certain messages
     */
    public int getHomeRepeater()
    {
        return getRepeater2();
    }

    /**
     * Static utility method to parse the home repeater value from the message
     * @param message with home repeater value.
     * @return repeater
     */
    public static int getHomeRepeater(CorrectedBinaryMessage message)
    {
        return message.getInt(REPEATER_2);
    }

    /**
     * Utility method to parse the identifier field from the message
     * @param message containing an identifier value
     * @return identifier
     */
    public static int getIdentifier(CorrectedBinaryMessage message)
    {
        return message.getInt(IDENTIFIER);
    }
}

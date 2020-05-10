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

package io.github.dsheirer.alias.id.radio;

import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.preference.identifier.talkgroup.APCO25TalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.AbstractIntegerFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.DMRTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.UnknownTalkgroupFormatter;
import io.github.dsheirer.protocol.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Protocol-specific formatting of radio id values and parsing of formatted radio id values.
 */
public class RadioFormatter
{
    private final static Logger mLog = LoggerFactory.getLogger(RadioFormatter.class);
    private static Map<Protocol,AbstractIntegerFormatter> mFormatterMap = new EnumMap<>(Protocol.class);

    static
    {
        mFormatterMap.put(Protocol.APCO25, new APCO25TalkgroupFormatter());
        mFormatterMap.put(Protocol.DMR, new DMRTalkgroupFormatter());
        mFormatterMap.put(Protocol.UNKNOWN, new UnknownTalkgroupFormatter());
    }

    public RadioFormatter()
    {
    }

    /**
     * Parses the formatted value into an integer using a protocol specific formatter
     */
    public static int parse(Protocol protocol, String value) throws ParseException
    {
        AbstractIntegerFormatter formatter = mFormatterMap.get(protocol);

        if(formatter == null)
        {
            formatter = mFormatterMap.get(Protocol.UNKNOWN);
        }

        return formatter.parse(value);
    }

    /**
     * Formats the integer value using a protocol-specific formatter
     */
    public static String format(Protocol protocol, int value)
    {
        AbstractIntegerFormatter formatter = mFormatterMap.get(protocol);

        if(formatter == null)
        {
            formatter = mFormatterMap.get(Protocol.UNKNOWN);
        }

        return formatter.format(value);
    }

    /**
     * Formats the integer value to the specified integer format using the protocol specific formatter
     */
    public static String format(Protocol protocol, int value, IntegerFormat format)
    {
        AbstractIntegerFormatter formatter = mFormatterMap.get(protocol);

        if(formatter == null)
        {
            formatter = mFormatterMap.get(Protocol.UNKNOWN);
        }

        return formatter.format(value, format);
    }
}

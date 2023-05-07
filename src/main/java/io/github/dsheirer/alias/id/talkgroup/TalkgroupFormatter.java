/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.alias.id.talkgroup;

import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.preference.identifier.talkgroup.APCO25TalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.AbstractIntegerFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.AnalogTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.DMRTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.FleetsyncTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.LTRTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.MDC1200TalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.MPT1327TalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.PassportTalkgroupFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.UnknownTalkgroupFormatter;
import io.github.dsheirer.protocol.Protocol;
import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protocol-specific formatting of talkgroup values and parsing of formatted talkgroup values.
 */
public class TalkgroupFormatter
{
    private final static Logger mLog = LoggerFactory.getLogger(TalkgroupFormatter.class);
    private static Map<Protocol,AbstractIntegerFormatter> mFormatterMap = new EnumMap<>(Protocol.class);

    static
    {
        AnalogTalkgroupFormatter analogTalkgroupFormatter = new AnalogTalkgroupFormatter();
        mFormatterMap.put(Protocol.AM, analogTalkgroupFormatter);
        mFormatterMap.put(Protocol.NBFM, analogTalkgroupFormatter);

        mFormatterMap.put(Protocol.APCO25, new APCO25TalkgroupFormatter());
        mFormatterMap.put(Protocol.DMR, new DMRTalkgroupFormatter());
        mFormatterMap.put(Protocol.FLEETSYNC, new FleetsyncTalkgroupFormatter());
        LTRTalkgroupFormatter ltr = new LTRTalkgroupFormatter();
        mFormatterMap.put(Protocol.LTR, ltr);
        mFormatterMap.put(Protocol.LTR_NET, ltr);
        mFormatterMap.put(Protocol.MDC1200, new MDC1200TalkgroupFormatter());
        mFormatterMap.put(Protocol.MPT1327, new MPT1327TalkgroupFormatter());
        mFormatterMap.put(Protocol.PASSPORT, new PassportTalkgroupFormatter());
        mFormatterMap.put(Protocol.UNKNOWN, new UnknownTalkgroupFormatter());
    }

    public TalkgroupFormatter()
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

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

package io.github.dsheirer.preference.identifier.talkgroup;

import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.mpt1327.identifier.MPT1327Talkgroup;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats Fleetsync 20-bit identifiers into either an integer value or an 8-bit fleet and 12-bit ident
 */
public class MPT1327TalkgroupFormatter extends AbstractIntegerFormatter
{
    private final static Logger mLog = LoggerFactory.getLogger(MPT1327TalkgroupFormatter.class);

    private static final int IDENTIFIER_DECIMAL_WIDTH = 7;
    private static final int IDENTIFIER_HEXADECIMAL_WIDTH = 5;
    private static final int FLEET_DECIMAL_WIDTH = 3;
    private static final int IDENT_DECIMAL_WIDTH = 4;
    private static final String SEPARATOR = "-";
    private static final Pattern MPT1327_PATTERN = Pattern.compile("(\\d{1,3})-(\\d{1,4})");

    /**
     * Formats the individual or group identifier to the specified format and width.
     */
    public static String format(TalkgroupIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        if(identifier instanceof MPT1327Talkgroup)
        {
            MPT1327Talkgroup mpt = (MPT1327Talkgroup)identifier;

            if(fixedWidth)
            {
                switch(format)
                {
                    case DECIMAL:
                        return toDecimal(mpt.getValue(), IDENTIFIER_DECIMAL_WIDTH);
                    case FORMATTED:
                        return toDecimal(mpt.getPrefix(), FLEET_DECIMAL_WIDTH) + SEPARATOR +
                            toDecimal(mpt.getIdent(), IDENT_DECIMAL_WIDTH);
                    case HEXADECIMAL:
                        return toHex(mpt.getValue(), IDENTIFIER_HEXADECIMAL_WIDTH);
                    default:
                        throw new IllegalArgumentException("Unrecognized integer format: " + format);
                }
            }
            else
            {
                switch(format)
                {
                    case DECIMAL:
                        return mpt.toString();
                    case FORMATTED:
                        return mpt.getPrefix() + SEPARATOR + mpt.getIdent();
                    case HEXADECIMAL:
                        return toHex(identifier.getValue());
                    default:
                        throw new IllegalArgumentException("Unrecognized integer format: " + format);
                }
            }
        }

        return identifier.toString();
    }

    /**
     * Parses an integer value from a formatted talkgroup string (e.g. 123-4567)
     */
    public int parse(String formattedTalkgroup) throws ParseException
    {
        if(formattedTalkgroup != null)
        {
            Matcher m = MPT1327_PATTERN.matcher(formattedTalkgroup);

            if(m.matches())
            {
                String rawPrefix = m.group(1);
                String rawIdent = m.group(2);

                try
                {
                    int prefix = Integer.parseInt(rawPrefix);

                    if(prefix < 0 || prefix > 127)
                    {
                        throw new ParseException("MPT-1327 prefix must be in range 0-127.  Error parsing [" + formattedTalkgroup + "]", 0);
                    }

                    int ident = Integer.parseInt(rawIdent);

                    if(ident < 1 || ident > 8191)
                    {
                        throw new ParseException("MPT-1327 ident must be in range 1-8192.  Error parsing [" + formattedTalkgroup + "]", 0);
                    }

                    return (prefix << 13) + ident;
                }
                catch(Exception e)
                {
                    //exception is rethrown below
                }
            }

        }

        throw new ParseException("Error parsing MPT1327 talkgroup value [" + formattedTalkgroup + "]", 0);
    }


    /**
     * Formats the MPT-1327 talkgroup as a string
     * @param talkgroup to format
     * @return formatted talkgroup
     */
    public String format(int talkgroup)
    {
        return toDecimal(getPrefix(talkgroup), FLEET_DECIMAL_WIDTH) + SEPARATOR + toDecimal(getIdent(talkgroup), IDENT_DECIMAL_WIDTH);
    }

    @Override
    public String format(int value, IntegerFormat integerFormat)
    {
        //Always use the fixed width format
        return format(value);
    }

    /**
     * Prefix for the specified talkgroup value
     * @param talkgroup value containing a prefix
     * @return prefix value
     */
    public static int getPrefix(int talkgroup)
    {
        return (talkgroup >> 13) & 0x7F;
    }

    /**
     * Ident for the specified talkgroup
     * @param talkgroup containing an ident
     * @return ident value
     */
    public static int getIdent(int talkgroup)
    {
        return talkgroup & 0x1FFF;
    }
}

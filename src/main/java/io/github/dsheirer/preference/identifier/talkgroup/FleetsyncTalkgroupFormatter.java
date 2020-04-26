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
import io.github.dsheirer.module.decode.fleetsync2.identifier.FleetsyncIdentifier;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats Fleetsync 20-bit identifiers into either an integer value or an 8-bit fleet and 12-bit ident
 */
public class FleetsyncTalkgroupFormatter extends AbstractIntegerFormatter
{
    private final static Logger mLog = LoggerFactory.getLogger(FleetsyncTalkgroupFormatter.class);
    private static final int IDENTIFIER_DECIMAL_WIDTH = 7;
    private static final int IDENTIFIER_HEXADECIMAL_WIDTH = 5;
    private static final int FLEET_DECIMAL_WIDTH = 3;
    private static final int IDENT_DECIMAL_WIDTH = 4;
    private static final String SEPARATOR = "-";
    private static final Pattern FLEETSYNC_PATTERN = Pattern.compile("(\\d{1,3})-(\\d{1,4})");

    /**
     * Formats the individual or group identifier to the specified format and width.
     */
    public static String format(TalkgroupIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        if(identifier instanceof FleetsyncIdentifier)
        {
            FleetsyncIdentifier fleetsync = (FleetsyncIdentifier)identifier;

            if(fixedWidth)
            {
                switch(format)
                {
                    case DECIMAL:
                        return toDecimal(fleetsync.getValue(), IDENTIFIER_DECIMAL_WIDTH);
                    case FORMATTED:
                        return toDecimal(fleetsync.getFleet(), FLEET_DECIMAL_WIDTH) + SEPARATOR +
                            toDecimal(fleetsync.getIdent(), IDENT_DECIMAL_WIDTH);
                    case HEXADECIMAL:
                        return toHex(fleetsync.getValue(), IDENTIFIER_HEXADECIMAL_WIDTH);
                    default:
                        throw new IllegalArgumentException("Unrecognized integer format: " + format);
                }
            }
            else
            {
                switch(format)
                {
                    case DECIMAL:
                        return fleetsync.toString();
                    case FORMATTED:
                        return fleetsync.getFleet() + SEPARATOR + fleetsync.getIdent();
                    case HEXADECIMAL:
                        return toHex(identifier.getValue());
                    default:
                        throw new IllegalArgumentException("Unrecognized integer format: " + format);
                }
            }
        }

        return identifier.toString();
    }

    @Override
    public String format(int talkgroup)
    {
        return toDecimal(getPrefix(talkgroup), FLEET_DECIMAL_WIDTH) + SEPARATOR +
            toDecimal(getIdent(talkgroup), IDENT_DECIMAL_WIDTH);
    }

    @Override
    public int parse(String formattedTalkgroup) throws ParseException
    {
        if(formattedTalkgroup != null)
        {
            Matcher m = FLEETSYNC_PATTERN.matcher(formattedTalkgroup);

            if(m.matches())
            {
                String rawPrefix = m.group(1);
                String rawIdent = m.group(2);

                try
                {
                    int prefix = Integer.parseInt(rawPrefix);

                    if(prefix < 1 || prefix > 127)
                    {
                        throw new ParseException("Fleetsync prefix must be in range 1-127.  Error parsing [" + formattedTalkgroup + "]", 0);
                    }

                    int ident = Integer.parseInt(rawIdent);

                    if(ident < 1 || ident > 8191)
                    {
                        throw new ParseException("Fleetsync ident must be in range 1-8192.  Error parsing [" + formattedTalkgroup + "]", 0);
                    }

                    return (prefix << 13) + ident;
                }
                catch(Exception e)
                {
                    //exception is rethrown below
                }
            }
        }

        throw new ParseException("Error parsing value from fleetsync talkgroup [" + formattedTalkgroup + "]", 0);
    }


    @Override
    public String format(int value, IntegerFormat format)
    {
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

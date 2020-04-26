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

import io.github.dsheirer.identifier.talkgroup.LTRTalkgroup;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats LTR identifiers
 */
public class LTRTalkgroupFormatter extends AbstractIntegerFormatter
{
    private final static Logger mLog = LoggerFactory.getLogger(LTRTalkgroupFormatter.class);
    private static final int IDENTIFIER_DECIMAL_WIDTH = 4;
    private static final int IDENTIFIER_HEXADECIMAL_WIDTH = 4;
    private static final int HOME_REPEATER_DECIMAL_WIDTH = 2;
    private static final int TALKGROUP_DECIMAL_WIDTH = 3;
    private static final String SEPARATOR = "-";
    private static final Pattern LTR_PATTERN = Pattern.compile("([01])-(\\d{1,2})-(\\d{1,3})");

    /**
     * Formats the group identifier to the specified format and width.
     */
    public static String format(TalkgroupIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        if(identifier instanceof LTRTalkgroup)
        {
            LTRTalkgroup ltr = (LTRTalkgroup)identifier;

            if(fixedWidth)
            {
                switch(format)
                {
                    case DECIMAL:
                        return toDecimal(ltr.getValue(), IDENTIFIER_DECIMAL_WIDTH);
                    case FORMATTED:
                        return ltr.getArea() + SEPARATOR +
                            toDecimal(ltr.getHomeChannel(), HOME_REPEATER_DECIMAL_WIDTH) + SEPARATOR +
                            toDecimal(ltr.getTalkgroup(), TALKGROUP_DECIMAL_WIDTH);
                    case HEXADECIMAL:
                        return toHex(ltr.getValue(), IDENTIFIER_HEXADECIMAL_WIDTH);
                    default:
                        throw new IllegalArgumentException("Unrecognized integer format: " + format);
                }
            }
            else
            {
                switch(format)
                {
                    case DECIMAL:
                        return ltr.toString();
                    case FORMATTED:
                        return ltr.getArea() + SEPARATOR + ltr.getHomeChannel() + SEPARATOR + ltr.getTalkgroup();
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
     * Parses an integer value from a formatted talkgroup string (e.g. 12-243)
     */
    public int parse(String formattedTalkgroup) throws ParseException
    {
        if(formattedTalkgroup != null)
        {
            Matcher m = LTR_PATTERN.matcher(formattedTalkgroup);

            if(m.matches())
            {
                String rawArea = m.group(1);
                String rawLcn = m.group(2);
                String rawGroup = m.group(3);

                try
                {
                    int area = Integer.parseInt(rawArea);

                    if(area < 0 || area > 1)
                    {
                        throw new ParseException("LTR area must be in range 0-1.  Error parsing [" + formattedTalkgroup + "]", 0);
                    }

                    int lcn = Integer.parseInt(rawLcn);

                    if(lcn < 1 || lcn > 20)
                    {
                        throw new ParseException("LTR repeater must be in range 1-20.  Error parsing [" + formattedTalkgroup + "]", 0);
                    }

                    int group = Integer.parseInt(rawGroup);

                    if(group < 0 || group > 255)
                    {
                        throw new ParseException("LTR talkgroup must be in range 1-255.  Error parsing [" + formattedTalkgroup + "]", 0);
                    }

                    return (area << 13) + (lcn << 8) + group;
                }
                catch(Exception e)
                {
                    //exception is rethrown below
                }
            }
        }

        throw new ParseException("Error parsing LTR talkgroup [" + formattedTalkgroup + "]", 0);
    }

    @Override
    public String format(int value, IntegerFormat format)
    {
        switch(format)
        {
            case DECIMAL:
            case FORMATTED:
            case HEXADECIMAL:
            default:
                return format(value);
        }
    }

    /**
     * Formats the talkgroup as a string
     * @param talkgroup to format
     * @return formatted talkgroup
     */
    public String format(int talkgroup)
    {
        return getArea(talkgroup) + SEPARATOR +
               toDecimal(getLcn(talkgroup), HOME_REPEATER_DECIMAL_WIDTH) + SEPARATOR +
               toDecimal(getTalkgroup(talkgroup), TALKGROUP_DECIMAL_WIDTH);
    }

    /**
     * Area value for the talkgroup, 0 or 1
     * @param value of talkgroup
     * @return area code
     */
    public static int getArea(int value)
    {
        return (value >> 13) & 0x1;
    }

    /**
     * LCN for the specified talkgroup value
     * @param value containing an LCN prefix
     * @return lcn value
     */
    public static int getLcn(int value)
    {
        return (value >> 8) & 0x1F;
    }

    /**
     * Talkgroup value for the specified composite talkgroup
     * @param value containing an LCN and talkgroup value
     * @return talkgroup value
     */
    public static int getTalkgroup(int value)
    {
        return value & 0xFF;
    }

}

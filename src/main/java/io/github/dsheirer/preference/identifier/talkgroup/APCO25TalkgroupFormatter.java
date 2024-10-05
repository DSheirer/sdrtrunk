/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.preference.identifier.talkgroup;

import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.preference.identifier.IntegerFormat;

public class APCO25TalkgroupFormatter extends AbstractIntegerFormatter
{
    public static final int RADIO_DECIMAL_WIDTH = 8;
    public static final int RADIO_HEXADECIMAL_WIDTH = 6;
    public static final int SYSTEM_HEXADECIMAL_WIDTH = 3;
    public static final int TALKGROUP_DECIMAL_WIDTH = 5;
    public static final int TALKGROUP_HEXADECIMAL_WIDTH = 4;
    public static final int WACN_HEXADECIMAL_WIDTH = 5;


    /**
     * Formats the individual or group identifier to the specified format and width.
     */
    public static String format(TalkgroupIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        if(identifier instanceof FullyQualifiedTalkgroupIdentifier fqti)
        {
            return format(fqti, format, fixedWidth);
        }

        if(fixedWidth)
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    return toDecimal(identifier.getValue(), TALKGROUP_DECIMAL_WIDTH);
                case HEXADECIMAL:
                    return toHex(identifier.getValue(), TALKGROUP_HEXADECIMAL_WIDTH);
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
        else
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    return identifier.getValue().toString();
                case HEXADECIMAL:
                    return toHex(identifier.getValue());
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
    }

    /**
     * Formats the fully qualified group identifier to the specified format and width.
     */
    public static String format(FullyQualifiedTalkgroupIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        int id = identifier.getValue();
        int wacn = identifier.getWacn();
        int system = identifier.getSystem();
        int talkgroup = identifier.getTalkgroup();

        StringBuilder sb = new StringBuilder();

        if(fixedWidth)
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    sb.append(toDecimal(id, TALKGROUP_DECIMAL_WIDTH));
                    sb.append("(").append(toHex(wacn, WACN_HEXADECIMAL_WIDTH));
                    sb.append(".").append(toHex(system, SYSTEM_HEXADECIMAL_WIDTH));
                    sb.append(".").append(toDecimal(talkgroup, TALKGROUP_DECIMAL_WIDTH)).append(")");
                    return sb.toString();
                case HEXADECIMAL:
                    sb.append(toHex(id, TALKGROUP_HEXADECIMAL_WIDTH));
                    sb.append("(").append(toHex(wacn, WACN_HEXADECIMAL_WIDTH));
                    sb.append(".").append(toHex(system, SYSTEM_HEXADECIMAL_WIDTH));
                    sb.append(".").append(toHex(talkgroup, TALKGROUP_HEXADECIMAL_WIDTH)).append(")");
                    return sb.toString();
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
        else
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    sb.append(id);
                    sb.append("(").append(toHex(wacn));
                    sb.append(".").append(toHex(system));
                    sb.append(".").append(talkgroup).append(")");
                    return sb.toString();
                case HEXADECIMAL:
                    sb.append(toHex(id));
                    sb.append("(").append(toHex(wacn));
                    sb.append(".").append(toHex(system));
                    sb.append(".").append(toHex(talkgroup)).append(")");
                    return sb.toString();
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
    }

    /**
     * Formats the patch group to the specified format and width
     */
    public static String format(PatchGroupIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        PatchGroup patchGroup = identifier.getValue();

        if(patchGroup != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("P:");
            sb.append(format(patchGroup.getPatchGroup(), format, fixedWidth));
            sb.append("[");

            int counter = 0;
            for(TalkgroupIdentifier patchedGroup: patchGroup.getPatchedTalkgroupIdentifiers())
            {
                sb.append(format(patchedGroup, format, fixedWidth));
                if(counter++ < patchGroup.getPatchedTalkgroupIdentifiers().size() - 1)
                {
                    sb.append(",");
                }
            }

            counter = 0;
            for(RadioIdentifier patchedRadio: patchGroup.getPatchedRadioIdentifiers())
            {
                sb.append(format(patchedRadio, format, fixedWidth));
                if(counter++ < patchGroup.getPatchedRadioIdentifiers().size() - 1)
                {
                    sb.append(",");
                }
            }
            sb.append("]");
            return sb.toString();
        }

        return identifier.toString();
    }

    /**
     * Formats the radio identifier to the specified format and width.
     */
    public static String format(RadioIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        if(identifier instanceof FullyQualifiedRadioIdentifier fqri)
        {
            return format(fqri, format, fixedWidth);
        }

        if(fixedWidth)
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    return toDecimal(identifier.getValue(), RADIO_DECIMAL_WIDTH);
                case HEXADECIMAL:
                    return toHex(identifier.getValue(), RADIO_HEXADECIMAL_WIDTH);
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
        else
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    return identifier.getValue().toString();
                case HEXADECIMAL:
                    return toHex(identifier.getValue());
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
    }

    /**
     * Formats the fully qualified radio identifier to the specified format and width.
     */
    public static String format(FullyQualifiedRadioIdentifier identifier, IntegerFormat format, boolean fixedWidth)
    {
        int id = identifier.getValue();
        int wacn = identifier.getWacn();
        int system = identifier.getSystem();
        int radio = identifier.getRadio();

        StringBuilder sb = new StringBuilder();

        if(fixedWidth)
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    if(id > 0)
                    {
                        sb.append(toDecimal(id, RADIO_DECIMAL_WIDTH)).append(" (");
                    }
                    sb.append(toHex(wacn, WACN_HEXADECIMAL_WIDTH));
                    sb.append(".").append(toHex(system, SYSTEM_HEXADECIMAL_WIDTH));
                    sb.append(".").append(toDecimal(radio, RADIO_DECIMAL_WIDTH));
                    if(id > 0)
                    {
                        sb.append(")");
                    }
                    return sb.toString();
                case HEXADECIMAL:
                    if(id > 0)
                    {
                        sb.append(toHex(id, RADIO_HEXADECIMAL_WIDTH)).append(" (");
                    }
                    sb.append(toHex(wacn, WACN_HEXADECIMAL_WIDTH));
                    sb.append(".").append(toHex(system, SYSTEM_HEXADECIMAL_WIDTH));
                    sb.append(".").append(toHex(radio, RADIO_HEXADECIMAL_WIDTH));
                    if(id > 0)
                    {
                        sb.append(")");
                    }
                    return sb.toString();
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
        else
        {
            switch(format)
            {
                case DECIMAL:
                case FORMATTED:
                    if(id > 0)
                    {
                        sb.append(id).append(" (");
                    }
                    sb.append(toHex(wacn));
                    sb.append(".").append(toHex(system));
                    sb.append(".").append(radio);
                    if(id > 0)
                    {
                        sb.append(")");
                    }
                    return sb.toString();
                case HEXADECIMAL:
                    if(id > 0)
                    {
                        sb.append(toHex(id)).append(" (");
                    }
                    sb.append(toHex(wacn));
                    sb.append(".").append(toHex(system));
                    sb.append(".").append(toHex(radio));
                    if(id > 0)
                    {
                        sb.append(")");
                    }
                    return sb.toString();
                default:
                    throw new IllegalArgumentException("Unrecognized integer format: " + format);
            }
        }
    }

    @Override
    public String format(int value, IntegerFormat integerFormat)
    {
        switch(integerFormat)
        {
            case DECIMAL:
            case FORMATTED:
                return format(value);
            case HEXADECIMAL:
                return toHex(value);
            default:
                throw new IllegalArgumentException("Unrecognized integer format: " + integerFormat);
        }
    }
}

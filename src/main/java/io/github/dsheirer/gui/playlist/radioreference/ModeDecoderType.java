/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.rrapi.type.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumeration of radio reference modes to corresponding sdrtrunk decoder type
 */
public enum ModeDecoderType
{
    AM("am", DecoderType.AM),
    APCO25("p25", DecoderType.P25_PHASE1),
    DMR("dmr", DecoderType.DMR),
    DSTAR("d-star", null),
    FM("fm", DecoderType.NBFM),
    FMN("fmn", DecoderType.NBFM),
    NXDN("nxdn", null),
    TELM("telm", null),
    USB("usb", null),
    LSB("lsb", null),
    YAESU_SYSTEM_FUSION("ysf", null),
    UNKNOWN("UNKNOWN", null);

    private String mValue;
    private DecoderType mDecoderType;

    ModeDecoderType(String value, DecoderType decoderType)
    {
        mValue = value;
        mDecoderType = decoderType;
    }

    private static final Logger mLog = LoggerFactory.getLogger(ModeDecoderType.class);

    public String getValue()
    {
        return mValue;
    }

    public DecoderType getDecoderType()
    {
        return mDecoderType;
    }

    public boolean hasDecoderType()
    {
        return mDecoderType != null;
    }

    /**
     * Lookup the entry that matches the mode
     * @param mode to match
     * @return matching entry or UNKNOWN
     */
    public static ModeDecoderType get(Mode mode)
    {
        if(mode != null)
        {
            for(ModeDecoderType modeDecoderType : ModeDecoderType.values())
            {
                if(modeDecoderType.getValue().contentEquals(mode.getName().toLowerCase()))
                {
                    return modeDecoderType;
                }
            }

            mLog.warn("Unrecognized Radio Reference Mode [" + mode.getName() + " ID:" + mode.getModeId() + "]");
        }

        return UNKNOWN;
    }
}

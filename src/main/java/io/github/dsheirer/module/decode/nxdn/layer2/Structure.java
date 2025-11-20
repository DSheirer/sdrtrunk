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

package io.github.dsheirer.module.decode.nxdn.layer2;

import java.util.EnumSet;

/**
 * LICH structure enumeration
 */
public enum Structure
{
    BEACON,
    CAC_NORMAL,
    CAC_IDLE,
    CAC_COMMON,
    CAC_LONG,
    CAC_SHORT,
    VOICE_VOICE,
    FACCH1_VOICE,
    VOICE_FACCH1,
    FACCH1_FACCH1,
    FACCH1_GUARD,
    FACCH2,
    FACCH3,
    UDCH,
    UDCH2,
    UNKNOWN;

    /**
     * Data carrying structures
     */
    private static final EnumSet<Structure> DATA = EnumSet.of(FACCH2, FACCH3, UDCH, UDCH2);

    /**
     * FACCH1 in first fragment
     */
    private static final EnumSet<Structure> FACCH1_FIRST = EnumSet.of(FACCH1_FACCH1, FACCH1_GUARD, FACCH1_VOICE);

    /**
     * FACCH1 in second fragment
     */
    private static final EnumSet<Structure> FACCH1_SECOND = EnumSet.of(FACCH1_FACCH1, VOICE_FACCH1);

    /**
     * Indicates if the frame contains audio frames
     */
    public boolean isVoice()
    {
        return this.equals(VOICE_VOICE) || this.equals(FACCH1_VOICE) || this.equals(VOICE_FACCH1);
    }

    /**
     * Indicates if this structure contains a FACCH1 frame
     */
    public boolean isFACCH1()
    {
        return FACCH1_FIRST.contains(this) || FACCH1_SECOND.contains(this);
    }

    /**
     * Indicates if a FACCH1 is in the first half of the frame
     */
    public boolean isFACCH1First()
    {
        return FACCH1_FIRST.contains(this);
    }

    /**
     * Indicates if a FACCH1 is in the second half of the frame
     */
    public boolean isFACCH1Second()
    {
        return FACCH1_SECOND.contains(this);
    }

    /**
     * Indicates if this is a data structure
     */
    public boolean isData()
    {
        return DATA.contains(this);
    }
}

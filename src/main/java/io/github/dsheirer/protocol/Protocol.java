/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.protocol;

/**
 * Binary protocols supported within this application
 */
public enum Protocol
{
    APCO25(true),
    FLEETSYNC(false),
    LOJACK(false),
    LTR_NET(true),
    LTR_STANDARD(true),
    MDC1200(false),
    MPT1327(true),
    PASSPORT(true),
    TAIT1200(false),
    UNKNOWN(false);

    private boolean mPrimary;

    Protocol(boolean primary)
    {
        mPrimary = primary;
    }

    /**
     * Indicates if this protocol has a corresponding primary decoder or an auxiliary decoder.
     */
    public boolean isPrimary()
    {
        return mPrimary;
    }
}

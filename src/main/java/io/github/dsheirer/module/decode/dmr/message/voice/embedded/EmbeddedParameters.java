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

package io.github.dsheirer.module.decode.dmr.message.voice.embedded;

/**
 * Parameters that are embedded in a DMR voice super frame.
 * @param algorithm identifier
 * @param key identifier
 * @param iv initialization vector
 */
public class EmbeddedParameters
{
    private ShortBurst mShortBurst;
    private String mIv;

    /**
     * Constructor
     * @param shortBurst payload
     * @param iv initialization vector extracted from DMR voice super-frame
     */
    public EmbeddedParameters(ShortBurst shortBurst)
    {
        mShortBurst = shortBurst;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getShortBurst());
        if(hasIv())
        {
            sb.append(" IV:").append(getIv());
        }
        return sb.toString();
    }

    /**
     * Short Burst payload message
     * @return short burst
     */
    public ShortBurst getShortBurst()
    {
        return mShortBurst;
    }

    /**
     * Optional initialization vector (IV) decoded from the voice super-frame
     * @return iv
     */
    public String getIv()
    {
        return mIv;
    }

    /**
     * Sets the optional IV value.
     * @param iv to set
     */
    public void setIv(String iv)
    {
        mIv = iv;
    }

    /**
     * Indicates if the optional IV is included.
     * @return true if included
     */
    public boolean hasIv()
    {
        return mIv != null;
    }
}

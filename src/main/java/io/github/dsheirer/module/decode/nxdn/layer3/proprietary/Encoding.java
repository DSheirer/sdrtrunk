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

package io.github.dsheirer.module.decode.nxdn.layer3.proprietary;

import java.nio.charset.Charset;

/**
 * Talker alias encoding scheme.
 */
public enum Encoding
{
    UTF8("UTF-8", "Latin/English (UTF-8)"),
    BIG5("BIG5", "Taiwan/China (BIG5)");

    private Charset mCharset;
    private String mLabel;

    /**
     * Constructs an instance
     * @param charsetName to lookup
     */
    Encoding(String charsetName, String label)
    {
        mCharset = Charset.forName(charsetName);
        mLabel = label;
    }

    /**
     * Character set
     */
    public Charset getCharset()
    {
        return mCharset;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}

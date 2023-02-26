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

package io.github.dsheirer.module.decode.ip.hytera.sds;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.type.HyteraEncodeFormat;

/**
 * Encoding
 */
public class Encoding extends HyteraToken
{
    private static final int[] CONTENT_LENGTH = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] ENCODING = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};

    /**
     * Constructs an instance
     *
     * @param message containing both the token, run-length, and content
     */
    public Encoding(CorrectedBinaryMessage message)
    {
        super(message);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ENCODING FORMAT:").append(getEncoding());
        return sb.toString();
    }

    /**
     * Length of the content field in bytes.
     * @return content field length.
     */
    public int getContentLength()
    {
        return mMessage.getInt(CONTENT_LENGTH);
    }

    /**
     * Hytera encoding format
     */
    public HyteraEncodeFormat getEncoding()
    {
        return HyteraEncodeFormat.fromValue(getEncodingValue());
    }

    /**
     * Encoding lookup value.
     * @return value
     */
    private int getEncodingValue()
    {
        return mMessage.getInt(ENCODING);
    }

    @Override
    HyteraTokenType getTokenType()
    {
        return HyteraTokenType.ENCODING;
    }
}

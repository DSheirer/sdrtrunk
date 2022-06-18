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

package com.github.dsheirer.sdrplay.error;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_ErrorInfoT;

import java.lang.foreign.MemorySegment;

/**
 * Error Information structure (sdrplay_api_ErrorInfoT)
 */
public class ErrorInformation
{
    private String mFile;
    private String mFunction;
    private int mLine;
    private String mMessage;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public ErrorInformation(MemorySegment memorySegment)
    {
        mFile = sdrplay_api_ErrorInfoT.file$slice(memorySegment).getUtf8String(0);
        mFunction = sdrplay_api_ErrorInfoT.function$slice(memorySegment).getUtf8String(0);
        mLine = sdrplay_api_ErrorInfoT.line$get(memorySegment);
        mMessage = sdrplay_api_ErrorInfoT.message$slice(memorySegment).getUtf8String(0);
    }

    public String getFile()
    {
        return mFile;
    }

    public String getFunction()
    {
        return mFunction;
    }

    public int getLine()
    {
        return mLine;
    }

    public String getMessage()
    {
        return mMessage;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Error Information:").append("\n");
        sb.append("\t    File: ").append(getFile()).append("\n");
        sb.append("\tFunction: ").append(getFunction()).append("\n");
        sb.append("\t    Line: ").append(getLine()).append("\n");
        sb.append("\t Message: ").append(getMessage()).append("\n");
        return sb.toString();
    }
}

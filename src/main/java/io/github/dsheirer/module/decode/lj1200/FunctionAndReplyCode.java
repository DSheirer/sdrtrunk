/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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

package io.github.dsheirer.module.decode.lj1200;

/**
 * LoJack Function and Reply Code
 */
public class FunctionAndReplyCode
{
    private LJ1200Message.Function mFunction;
    private String mReplyCode;

    public FunctionAndReplyCode(LJ1200Message.Function function, String replyCode)
    {
        mFunction = function;
        mReplyCode = replyCode;
    }

    public LJ1200Message.Function getFunction()
    {
        return mFunction;
    }

    public String getReplyCode()
    {
        return mReplyCode;
    }

    @Override
    public String toString()
    {
        return "FUNCTION:" + mFunction + " REPLY CODE:" + mReplyCode;
    }
}

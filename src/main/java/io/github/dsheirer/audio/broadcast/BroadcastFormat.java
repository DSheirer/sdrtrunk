/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.audio.broadcast;

public enum BroadcastFormat
{
    MP3("audio/mpeg", ".mp3");

    private String mValue;
    private String mFileExtension;

    private BroadcastFormat(String value, String fileExtension)
    {
        mValue = value;
        mFileExtension = fileExtension;
    }

    public String getValue()
    {
        return mValue;
    }

    public String getFileExtension()
    {
        return mFileExtension;
    }
}

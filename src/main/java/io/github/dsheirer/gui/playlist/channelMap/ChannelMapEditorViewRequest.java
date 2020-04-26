/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.channelMap;

/**
 * Request to show the channel map editor
 */
public class ChannelMapEditorViewRequest
{
    private String mChannelMapName;

    /**
     * Constructs an instance
     * @param channelMapName to select once the editor is showing.
     */
    public ChannelMapEditorViewRequest(String channelMapName)
    {
        mChannelMapName = channelMapName;
    }

    /**
     * Constructs an instance.
     */
    public ChannelMapEditorViewRequest()
    {
    }

    public String getChannelMapName()
    {
        return mChannelMapName;
    }

    public boolean hasChannelMapName()
    {
        return mChannelMapName != null;
    }
}

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

package io.github.dsheirer.gui.playlist.streaming;

import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.playlist.PlaylistManager;

/**
 * Factory for creating broadcast configuration editors
 */
public class StreamEditorFactory
{
    /**
     * Creates a new editor for the specified broadcast server type
     * @param broadcastServerType to edit
     * @return editor or the default unknown editor
     */
    public static AbstractBroadcastEditor getEditor(BroadcastServerType broadcastServerType, PlaylistManager playlistManager)
    {
        switch(broadcastServerType)
        {
            case BROADCASTIFY:
                return new BroadcastifyStreamEditor(playlistManager);
            case RDIOSCANNER_CALL:
                return new RdioScannerEditor(playlistManager);
            case OPENMHZ:
                return new OpenMHzEditor(playlistManager);
            case BROADCASTIFY_CALL:
                return new BroadcastifyCallEditor(playlistManager);
            case ICECAST_HTTP:
                return new IcecastHTTPStreamEditor(playlistManager);
            case ICECAST_TCP:
                return new IcecastTCPStreamEditor(playlistManager);
            case SHOUTCAST_V1:
                return new ShoutcastV1StreamEditor(playlistManager);
            case SHOUTCAST_V2:
                return new ShoutcastV2StreamEditor(playlistManager);
            default:
                return new UnknownStreamEditor(playlistManager);
        }
    }
}

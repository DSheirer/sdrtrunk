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

package io.github.dsheirer.gui.playlist.streaming;

import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.playlist.PlaylistManager;

/**
 * Thinline Radio Core stream editor. The site key lives in User Preferences, not on the stream.
 */
public class ThinlineRadioCoreEditor extends RdioScannerEditor
{
    private static final String SUMMARY = "Uses the site key from User Preferences. Calls stay on this site until ThinLine asks for one.";
    private static final String MISSING_KEY = "Open User Preferences → Thinline Radio and enter the site key before enabling this stream.";

    public ThinlineRadioCoreEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.THINLINE_RADIO_CORE;
    }

    @Override
    protected String getHostLabel()
    {
        return "Thinline Radio URL";
    }

    @Override
    protected boolean showsApiKey()
    {
        return false;
    }

    @Override
    public void setItem(io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerConfiguration item)
    {
        super.setItem(item);
        boolean ready = hasSiteKey();
        if(item != null)
        {
            getEnabledSwitch().setDisable(!ready);
            if(!ready)
            {
                // Leave a clear reason in the name tooltip path via disabled switch; log once is enough in UI later.
            }
            String preferenceUrl = getPlaylistManager().getUserPreferences().getThinlineRadioPreference().getUrl();
            String host = getHostTextField().getText();
            if(preferenceUrl != null && !preferenceUrl.isBlank() &&
                (host == null || host.isBlank() || host.startsWith("http://localhost")))
            {
                String shown = preferenceUrl;
                String uploadPath = "/api/call-upload";
                if(shown.endsWith(uploadPath))
                {
                    shown = shown.substring(0, shown.length() - uploadPath.length());
                }
                getHostTextField().setText(shown);
                modifiedProperty().set(false);
            }
        }
    }
}

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

package io.github.dsheirer.gui.settings;

import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.web.WebServer;

/**
 * What the settings pages can use: the settings, the Radio Reference service for the account controls, and the
 * channel processing manager so that calibration can wait until no channels are processing, the database manager for
 * the database page and the web server for the web server page.
 * @param settings to edit
 * @param radioReference service shared with the playlist editor, or null when there isn't one
 * @param channelProcessingManager for the processing state of channels, or null when there isn't one
 * @param databaseManager for the call and event database, or null when there isn't one (playlist editor)
 * @param webServer for the web server page status, or null when there isn't one
 */
public record SettingsContext(Settings settings, RadioReference radioReference,
                              ChannelProcessingManager channelProcessingManager, DatabaseManager databaseManager,
                              WebServer webServer)
{
}

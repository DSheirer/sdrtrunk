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

import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.settings.Settings;

/**
 * What the settings pages can use: the settings, and the Radio Reference service for the account controls.
 * @param settings to edit
 * @param radioReference service shared with the playlist editor, or null when there isn't one
 */
public record SettingsContext(Settings settings, RadioReference radioReference)
{
}

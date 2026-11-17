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

package io.github.dsheirer.gui.spectrum;

import java.util.List;

/**
 * Channel shown in the display's Channels hover card
 * @param title of the channel, drawn in the color of its state
 * @param details lines
 * @param state of the channel: an enabled or disabled playlist channel, or a radio reference candidate
 * @param key identifying the channel, e.g. the playlist channel, so the card lists it once, or null
 * @param prompts true when the entry is reason to show the card, e.g. the channel's labels don't fit in its box, or
 * false when it's only listed while the card is shown for other entries
 */
public record ChannelCardEntry(String title, List<String> details, ChannelMenus.State state, Object key,
                               boolean prompts)
{
}

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

package io.github.dsheirer.audio.broadcast;

/**
 * Options for streaming management of patch groups.  These options are used when streaming an audio call that is
 * tagged to a patch group.  The audio will either be streamed once and identified as the patch group, or it will
 * be streamed multiple times, once for each individual patched talkgroup.
 */
public enum PatchGroupStreamingOption
{
    PATCH_GROUP("Patch Group"),
    TALKGROUPS("Individual Talkgroups");

    private String mLabel;

    /**
     * Constructs an instance
     * @param label to display
     */
    PatchGroupStreamingOption(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}

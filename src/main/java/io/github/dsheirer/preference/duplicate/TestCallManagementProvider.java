/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.preference.duplicate;

/**
 * Test implementation of duplicate call detection preferences.
 */
public class TestCallManagementProvider implements ICallManagementProvider
{
    private final boolean mByTalkgroup;
    private final boolean mByRadio;

    /**
     * Constructs an instance
     * @param byTalkgroup to enable duplicate detection by talkgroup
     * @param byRadio to enable duplicate detection by radio
     */
    public TestCallManagementProvider(boolean byTalkgroup, boolean byRadio)
    {
        mByTalkgroup = byTalkgroup;
        mByRadio = byRadio;
    }

    @Override
    public boolean isDuplicateCallDetectionEnabled()
    {
        return mByTalkgroup || mByRadio;
    }

    @Override
    public boolean isDuplicateCallDetectionByTalkgroupEnabled()
    {
        return mByTalkgroup;
    }

    @Override
    public boolean isDuplicateCallDetectionByRadioEnabled()
    {
        return mByRadio;
    }
}

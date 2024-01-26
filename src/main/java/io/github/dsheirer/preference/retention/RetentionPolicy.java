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

package io.github.dsheirer.preference.retention;

import java.util.EnumSet;

/**
 * Retention (ie age-off) policy for managing call events and related audio recording files.
 */
public enum RetentionPolicy
{
    AGE("Age"),
    SIZE("Directory Size"),
    AGE_AND_SIZE("Age & Directory Size");

    private String mLabel;

    /**
     * Constructs an instance
     * @param label to display for the entry
     */
    RetentionPolicy(String label)
    {
        mLabel = label;
    }

    public static EnumSet<RetentionPolicy> AGE_POLICIES = EnumSet.of(AGE, AGE_AND_SIZE);
    public static EnumSet<RetentionPolicy> SIZE_POLICIES = EnumSet.of(SIZE, AGE_AND_SIZE);

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Indicates if the entry is an age-based retention policy.
     */
    public boolean isAgePolicy()
    {
        return AGE_POLICIES.contains(this);
    }

    /**
     * Indicates if the entry is a size-based retention policy.
     */
    public boolean isSizePolicy()
    {
        return SIZE_POLICIES.contains(this);
    }
}

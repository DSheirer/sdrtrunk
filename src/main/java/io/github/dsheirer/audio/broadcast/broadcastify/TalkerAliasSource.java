/*
 * *****************************************************************************
 * Copyright (C) 2026 Robert Guenther
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * *****************************************************************************
 */

package io.github.dsheirer.audio.broadcast.broadcastify;

/**
 * Source for the Broadcastify Calls {@code srcId_alias} value.
 */
public enum TalkerAliasSource
{
    OVER_THE_AIR("Over-the-air talker alias"),
    PLAYLIST("Stored playlist alias");

    private final String mLabel;

    TalkerAliasSource(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}

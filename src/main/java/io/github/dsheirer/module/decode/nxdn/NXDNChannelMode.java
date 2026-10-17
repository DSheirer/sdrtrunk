/*
 * *****************************************************************************
 * Copyright (C) 2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ****************************************************************************
 */
package io.github.dsheirer.module.decode.nxdn;

/**
 * Configured operating mode for an NXDN channel.
 */
public enum NXDNChannelMode
{
    CONVENTIONAL("Conventional"),
    TRUNKED("Trunked");

    private final String mLabel;

    NXDNChannelMode(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}

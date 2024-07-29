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

package io.github.dsheirer.source.tuner.sdrplay.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SDRPlay API Versions
 */
public enum Version
{
    UNKNOWN(0.0f, false),
    V3_07(3.07f, true),
    V3_08(3.08f, true),
    V3_09(3.09f, true),
    V3_10(3.10f, true),
    V3_11(3.11f, true),
    V3_12(3.12f, true),
    V3_13(3.13f, true), //No changes - OSX build only.
    V3_14(3.14f, true),
    V3_15(3.15f, true),
    V3_16(3.16f, true);

    private float mValue;
    private boolean mSupported;
    private static final Logger mLog = LoggerFactory.getLogger(Version.class);

    Version(float value, boolean supported)
    {
        mValue = value;
        mSupported = supported;
    }

    /**
     * Indicates if the API version is supported by the jsdrplay library
     */
    public boolean isSupported()
    {
        return mSupported;
    }

    /**
     * Indicates if this version is greater than or equal to the specified version.
     *
     * @param version to compare
     * @return true if this version is greater than or equal to
     */
    public boolean gte(Version version)
    {
        return this.ordinal() >= version.ordinal();
    }

    /**
     * Numeric API version/value
     */
    public float getVersion()
    {
        return mValue;
    }

    /**
     * Lookup the version from the specified value.
     *
     * @param value to lookup
     * @return version or UNKNOWN
     */
    public static Version fromValue(float value)
    {

        for(Version version : values())
        {
            if(version.mValue == value)
            {
                return version;
            }
        }

        mLog.warn("Unrecognized SDRplay API version [" + value + "]");
        return UNKNOWN;
    }


    @Override
    public String toString()
    {
        return String.valueOf(mValue);
    }
}

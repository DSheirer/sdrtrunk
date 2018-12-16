/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.protocol;

import java.util.EnumSet;

/**
 * Binary protocols supported within this application
 */
public enum Protocol
{
    APCO25("APCO-25", "APCO25PHASE1", 9600),
    FLEETSYNC("Fleetsync", "FLEETSYNC", 1200),
    IPV4("IPV4", "IPV4", 0),
    LOJACK("LoJack", "LOJACK", 1200),
    LTR("LTR", "LTR", 300),
    LTR_NET("LTR-Net", "LTRNET", 300),
    LTR_STANDARD("LTR-Standard", "LTRSTANDARD", 300),
    MDC1200("MDC-1200", "MDC1200", 1200),
    MPT1327("MPT-1327", "MPT1327", 1200),
    PASSPORT("Passport", "PASSPORT", 300),
    TAIT1200("Tait 1200", "TAIT1200", 1200),
    UNKNOWN("Unknown", "UNKNOWN", 0);

    private String mLabel;
    private String mFileNameLabel;
    private int mBitRate;


    Protocol(String label, String fileNameLabel, int bitRate)
    {
        mLabel = label;
        mFileNameLabel = fileNameLabel;
        mBitRate = bitRate;
    }

    public static EnumSet<Protocol> TALKGROUP_PROTOCOLS = EnumSet.of(APCO25, FLEETSYNC, LTR_NET, LTR_STANDARD, MDC1200,
        MPT1327, PASSPORT, TAIT1200);

    @Override
    public String toString()
    {
        return mLabel;
    }

    public String getFileNameLabel()
    {
        return mFileNameLabel;
    }

    public int getBitRate()
    {
        return mBitRate;
    }
}

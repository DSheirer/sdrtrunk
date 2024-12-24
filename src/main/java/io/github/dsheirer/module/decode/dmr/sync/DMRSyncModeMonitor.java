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

package io.github.dsheirer.module.decode.dmr.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitors sync pattern detections to automatically detect the prevailing or dominant sync mode group used on a channel
 * and optimizes the DMR sync detectors to reduce the quantity of false detects of unused sync patterns and the overall
 * sync detection processing workload.
 */
public class DMRSyncModeMonitor
{
    private static final int DOMINANT_THRESHOLD = 10;
    private final List<DMRSyncDetector> mSyncDetectors = new ArrayList<>();
    private int mBaseCount;
    private int mMobileCount;
    private int mDirectCount;
    private DMRSyncDetectMode mMode = DMRSyncDetectMode.AUTOMATIC;

    /**
     * Constructs an instance
     */
    public DMRSyncModeMonitor()
    {
    }

    /**
     * Sets (and locks) the sync detection mode.  This can be directly invoked when the operating mode is known
     * in advance, such as for a repeated traffic channel where the expected mode is BASE_ONLY, otherwise this method
     * is automatically invoked by this monitor when operating in ALL mode, once the dominant mode is established.
     * @param mode to set
     */
    public void setMode(DMRSyncDetectMode mode)
    {
        mMode = mode;

        for(DMRSyncDetector detector : mSyncDetectors)
        {
            detector.setMode(mode);
        }
    }

    /**
     * Indicates a sync pattern is positively detected and updates the current sync detection mode for all monitored
     * sync detectors once the dominant mode is established.
     * @param pattern that was detected.
     */
    public void detected(DMRSyncPattern pattern)
    {
        if(mMode != DMRSyncDetectMode.AUTOMATIC)
        {
            return;
        }

        switch(pattern)
        {
            case BASE_STATION_DATA:
            case BASE_STATION_VOICE:
                mBaseCount++;
                break;
            case MOBILE_STATION_DATA:
            case MOBILE_STATION_VOICE:
            case REVERSE_CHANNEL:
                mMobileCount++;
                break;
            case DIRECT_DATA_TIMESLOT_1:
            case DIRECT_DATA_TIMESLOT_2:
            case DIRECT_VOICE_TIMESLOT_1:
            case DIRECT_VOICE_TIMESLOT_2:
                mDirectCount++;
                break;
        }

        if(mBaseCount - mMobileCount > DOMINANT_THRESHOLD && mBaseCount - mDirectCount > DOMINANT_THRESHOLD)
        {
            setMode(DMRSyncDetectMode.BASE_ONLY);
        }
        else if(mMobileCount - mBaseCount > DOMINANT_THRESHOLD && mMobileCount - mDirectCount > DOMINANT_THRESHOLD)
        {
            setMode(DMRSyncDetectMode.MOBILE_ONLY);
        }
        else if(mDirectCount - mBaseCount > DOMINANT_THRESHOLD && mDirectCount - mMobileCount > DOMINANT_THRESHOLD)
        {
            setMode(DMRSyncDetectMode.DIRECT_ONLY);
        }
    }

    /**
     * Adds a sync detector to be managed by this instance.
     * @param syncDetector to add
     */
    public void add(DMRSyncDetector syncDetector)
    {
        mSyncDetectors.add(syncDetector);
    }
}

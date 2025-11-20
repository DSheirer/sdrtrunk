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

package io.github.dsheirer.module.decode.nxdn;

/**
 * Tracks sync detections for fixed length messages
 */
public class SyncTracker
{
    private final int mSymbolsPerMessage;
    private final int mSyncLossThreshold;
    private final int mMaxSyncWindowSize;
    private int mSyncCount = 0;
    private int mSymbolCount;

    /**
     * Constructs an instance
     * @param symbolsPerMessage a count of how many symbols per message, between each sync interval (ie message length).
     * @param messageWindowSize window size for retaining sync across messages sequences
     */
    public SyncTracker(int symbolsPerMessage, int messageWindowSize)
    {
        mSymbolsPerMessage = symbolsPerMessage;
        mSyncLossThreshold = symbolsPerMessage * 2;
        mMaxSyncWindowSize = messageWindowSize;
    }

    /**
     * Increments the symbol count.
     */
    public void symbol()
    {
        mSymbolCount++;

        if(mSymbolCount >= mSyncLossThreshold)
        {
            mSymbolCount -= mSymbolsPerMessage;
            mSyncCount--;
            mSyncCount = Math.max(mSyncCount, 0);
        }
    }

    /**
     * Indicates when a sync pattern is detected.
     */
    public void syncDetected()
    {
        mSyncCount++;
        mSyncCount = Math.min(mSyncCount, mMaxSyncWindowSize);
        mSymbolCount -= mSymbolsPerMessage;
        mSymbolCount = Math.max(mSymbolCount, 0);
    }

    /**
     * Indicates if there has been at least one sync detection in the past maxSyncSequence message windows.
     * @return true if there is at least one sync detection
     */
    public boolean isSynchronized()
    {
        return mSyncCount > 0;
    }
}

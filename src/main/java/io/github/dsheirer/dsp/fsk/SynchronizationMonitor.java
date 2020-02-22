/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.dsp.fsk;

import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors a symbol stream and externally provided sync detection events to track the sync state as the number of
 * message synchronization events relative the count of symbols that have been decoded to provide synchronization state
 * events to a registered listener.
 *
 * This might be used to provide a control signal to a symbol timing error detector to adjust timing error gain levels.
 *
 * This class only works with messages of uniform symbol length.
 *
 * Synchronization States:
 *
 * -COARSE - default state.  demoted from MEDIUM after one message period with no sync detection.
 *
 * -MEDIUM - promoted from COARSE when one sync detection occurs after two message periods, or demoted from FINE
 * after two message periods with no sync detections.
 *
 * -FINE   - promoted from MEDIUM after two or more consecutive sync detections within the preceding two or more
 * message periods.
 */
public class SynchronizationMonitor implements ISyncDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(SynchronizationMonitor.class);

    private ISyncStateListener mSyncStateListener;
    private SyncState mSyncState = SyncState.COARSE;
    private int mSyncCount;
    private int mSymbolCount = 0;
    private int mMessageLength;

    /**
     * Constructs the synchronization monitor for the specified message symbol length.
     *
     * @param messageLength in symbols.
     */
    public SynchronizationMonitor(int messageLength)
    {
        mMessageLength = messageLength;
    }

    /**
     * Resets to the default state of COARSE
     */
    public void reset()
    {
        setSyncState(SyncState.COARSE);
        mSymbolCount = 0;
    }

    /**
     * Updates the sync state and broadcasts any state changes to the registered sync state listener.
     */
    private void setSyncState(SyncState syncState)
    {
        if(mSyncState != syncState)
        {
            mSyncState = syncState;

            if(mSyncStateListener != null)
            {
                mSyncStateListener.setSyncState(mSyncState);
            }
        }
    }

    /**
     * Updates the message synchronization state based on the current number of sync detections.
     */
    private void updateSyncState()
    {
        if(mSyncCount < 0)
        {
            mSyncCount = 0;

            //The previous state was also zero, so there are no changes
            return;
        }

        switch(mSyncCount)
        {
            case 0:
                setSyncState(SyncState.COARSE);
                break;
            case 1:
                setSyncState(SyncState.MEDIUM);
                break;
            default:
                setSyncState(SyncState.FINE);
        }

        //Max is 3 which means state will drop to MEDIUM after 2 elapsed message periods and COARSE after 3 elapsed
        // message periods with no sync detection.
        if(mSyncCount > 3)
        {
            mSyncCount = 3;
        }
    }

    /**
     * Implements the ISyncDetectionListener interface to be notified when an external entity detects a message
     * sync word in the symbol stream.
     */
    @Override
    public void syncDetected(int bitErrors)
    {
        mSymbolCount = 0;
        mSyncCount++;
        updateSyncState();
    }

    @Override
    public void syncLost(int bitsProcessed)
    {
        //no-op
    }

    /**
     * Increments the symbol count for each decoded symbol.
     */
    public void increment()
    {
        mSymbolCount++;

        if(mSymbolCount > mMessageLength)
        {
            mSymbolCount = 0;
            mSyncCount--;
            updateSyncState();
        }
    }

    public void setListener(ISyncStateListener listener)
    {
        mSyncStateListener = listener;
    }
}

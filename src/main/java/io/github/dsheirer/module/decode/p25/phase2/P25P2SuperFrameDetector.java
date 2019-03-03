/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.SuperFrameFragment;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.ScramblingSequence;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APCO25 Phase 2 super-frame fragment detector uses a sync pattern detector and a circular
 * dibit buffer to detect sync patterns and correctly frame a 1440-bit super-frame fragment
 * containing 4 timeslots and surrounding ISCH messaging.
 */
public class P25P2SuperFrameDetector implements Listener<Dibit>, ISyncDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2SuperFrameDetector.class);

    private static final int FRAGMENT_DIBIT_LENGTH = 720;
    private static final int DIBIT_COUNT_MISALIGNED_SYNC = FRAGMENT_DIBIT_LENGTH - 180;
    private static final int BROADCAST_SYNC_LOSS_DIBIT_COUNT = 3720;
    private static final int DIBIT_DELAY_BUFFER_INDEX_SYNC_1 = 360;
    private static final int DIBIT_DELAY_BUFFER_INDEX_SYNC_2 = 540;
    private static final int SYNCHRONIZED_SYNC_MATCH_THRESHOLD = 10;
    private static final int UN_SYNCHRONIZED_SYNC_MATCH_THRESHOLD = 4;

    private ScramblingSequence mScramblingSequence = new ScramblingSequence();
    private Listener<IMessage> mMessageListener;
    private P25P2SyncDetector mSyncDetector;
    private DibitDelayBuffer mSyncDetectionDelayBuffer = new DibitDelayBuffer(160);
    private DibitDelayBuffer mFragmentBuffer = new DibitDelayBuffer(720);
    private int mDibitsProcessed = 0;
    private boolean mSynchronized = false;

    public P25P2SuperFrameDetector(IPhaseLockedLoop phaseLockedLoop)
    {
        mSyncDetector = new P25P2SyncDetector(this, phaseLockedLoop);
    }

    public void setListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Sets the sample rate for the phase inversion sync detector
     */
    public void setSampleRate(double sampleRate)
    {
        mSyncDetector.setSampleRate(sampleRate);
    }

    public void reset()
    {
    }

    @Override
    public void syncDetected(int bitErrors)
    {
        checkFragmentSync(bitErrors);
    }

    @Override
    public void syncLost()
    {
    }

    private long getCurrentTimestamp()
    {
        //TODO: implement a dibit counter and timestamp calculator.  We should receive a timestamp update with each
        //TODO: buffer that arrives.  Use the timestamp dibit counter to calculate the exact timestamp of where we're at.
        return System.currentTimeMillis();
    }

    @Override
    public void receive(Dibit dibit)
    {
        mDibitsProcessed++;

        mFragmentBuffer.put(dibit);

        if(mSynchronized)
        {
            mSyncDetectionDelayBuffer.put(dibit);

            if(mDibitsProcessed >= FRAGMENT_DIBIT_LENGTH)
            {
                checkFragmentSync(0);
            }
        }
        else
        {
            //Only feed the sync pattern detector if we're not synchronized
            Dibit delayed = mSyncDetectionDelayBuffer.getAndPut(dibit);
            mSyncDetector.receive(delayed);
        }

        //Broadcast sync loss message once a second (3000 dibits/6000 bits) when we're not synchronized
        if(mDibitsProcessed > BROADCAST_SYNC_LOSS_DIBIT_COUNT)
        {
            mDibitsProcessed -= 3000;
            broadcastSyncLoss(3000);
        }
    }

    /**
     * Creates a super-frame fragment from the current contents of the fragment dibit buffer and broadcasts it to
     * a registered listener.
     *
     * @param bitErrors detected for first and second ISCH-S segments combined
     */
    private void broadcastFragment(int bitErrors)
    {
        if(mDibitsProcessed > FRAGMENT_DIBIT_LENGTH)
        {
            broadcastSyncLoss(mDibitsProcessed - FRAGMENT_DIBIT_LENGTH);
        }

        mDibitsProcessed = 0;
        CorrectedBinaryMessage message = mFragmentBuffer.getMessage(0, 720);
        message.setCorrectedBitCount(bitErrors);
        SuperFrameFragment frameFragment = new SuperFrameFragment(message, getCurrentTimestamp(), mScramblingSequence);
        broadcast(frameFragment);
    }

    private void broadcastSyncLoss(int dibitsProcessed)
    {
        broadcast(new SyncLossMessage(getCurrentTimestamp(), dibitsProcessed * 2, Protocol.APCO25_PHASE2));
    }

    private void broadcast(IMessage message)
    {
        if(mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }


    private void checkFragmentSync(int syncDetectorBitErrorCount)
    {
        //Since we're using multi-sync detection, only proceed if we've processed enough dibits.  The first sync detector
        //to fire will cause the fragment to be processed and any subsequent, simultaneous detection will be ignored.
        if(mDibitsProcessed > 0)
        {
            if(mSynchronized)
            {
                //If we're synchronized, then this is a counter based trigger and we check both sync locations
                Dibit[] sync1Dibits = mFragmentBuffer.getBuffer(DIBIT_DELAY_BUFFER_INDEX_SYNC_1, 20);
                int sync1BitErrorCount = P25P2SyncPattern.getBitErrorCount(sync1Dibits);

                if(sync1BitErrorCount <= SYNCHRONIZED_SYNC_MATCH_THRESHOLD)
                {
                    Dibit[] sync2Dibits = mFragmentBuffer.getBuffer(DIBIT_DELAY_BUFFER_INDEX_SYNC_2, 20);
                    int sync2BitErrorCount = P25P2SyncPattern.getBitErrorCount(sync2Dibits);

                    if(sync2BitErrorCount <= SYNCHRONIZED_SYNC_MATCH_THRESHOLD)
                    {
                        broadcastFragment(sync1BitErrorCount + sync2BitErrorCount);
                        //broadcast message
                        return;
                    }
                    else
                    {
                        mSynchronized = false;
                        return;
                    }
                }
                else
                {
                    mSynchronized = false;
                    return;
                }
            }

            //If we're not synchronized, this is a sync detector trigger and we only have to check sync 1 for error
            // count because the sync detector has already triggered on sync 2
            Dibit[] sync1Dibits = mFragmentBuffer.getBuffer(DIBIT_DELAY_BUFFER_INDEX_SYNC_1, 20);
            int sync1BitErrorCount = P25P2SyncPattern.getBitErrorCount(sync1Dibits);

            if(sync1BitErrorCount <= UN_SYNCHRONIZED_SYNC_MATCH_THRESHOLD)
            {
                mSynchronized = true;
                broadcastFragment(sync1BitErrorCount + syncDetectorBitErrorCount);
            }
            else
            {
                //We're probably mis-aligned on the fragment.  Setup as if we're synchronized and adjust the dibits
                // processed counter so that we guarantee a fragment check after another 180 dibits have arrived which
                // means that sync1 and sync2 should be aligned
                mSynchronized = true;

                if(mDibitsProcessed > DIBIT_COUNT_MISALIGNED_SYNC)
                {
                    broadcastSyncLoss(mDibitsProcessed - DIBIT_COUNT_MISALIGNED_SYNC);
                }

                mDibitsProcessed = DIBIT_COUNT_MISALIGNED_SYNC;
            }
        }
    }
}

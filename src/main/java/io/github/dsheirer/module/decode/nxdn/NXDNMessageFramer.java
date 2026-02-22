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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.nxdn.layer1.Frame;
import io.github.dsheirer.module.decode.nxdn.layer2.LICHTracker;
import io.github.dsheirer.module.decode.nxdn.layer2.Option;
import io.github.dsheirer.module.decode.nxdn.sync.standard.NXDNStandardHardSyncDetector;
import io.github.dsheirer.sample.Listener;

/**
 * Provides message framing for the demodulated dibit stream.  This framer is notified by an external sync detection
 * process using the two syncDetected() methods below to indicate if the NID that follows the sync was correctly error
 * detected and corrected.  When the NID does not pass error correction, we use a PLACEHOLDER data unit ID to allow the
 * uncertain message to assemble and then we'll inspect before and after data unit IDs and the quantity of captured
 * dibits to make a best guess on what the assembled message represents.
 */
public class NXDNMessageFramer
{
    private static final float SYNC_DETECTION_THRESHOLD = 60;
    private final NXDNStandardHardSyncDetector mHardSyncDetector = new NXDNStandardHardSyncDetector();
    private boolean mSyncDetected = false;

    private static final double MILLISECONDS_PER_SYMBOL = 1.0 / 4800.0 / 1000.0;
    private Listener<IMessage> mMessageListener;
    private boolean mRunning = false;
    private int mDibitCounter = 58; //Set to 1-greater than SYNC+NID to avoid triggering message assembly on startup
    private NXDNFrameAssembler mMessageAssembler;
    private int mDebugSymbolCount = 0;
    private int mDibitSinceTimestampCounter = 0;
    private long mReferenceTimestamp = 0;
    private final LICHTracker mLICHTracker = new LICHTracker();
    private int mSymbolsSinceLastSync = 384;

    /**
     * Constructs an instance
     * @param messageListener to receive framed messages
     */
    public NXDNMessageFramer(Listener<IMessage> messageListener)
    {
        mMessageListener = messageListener;
    }

    /**
     * Sets or updates the current dibit stream time from an incoming sample buffer.
     * @param time to use as a reference timestamp.
     */
    public void setTimestamp(long time)
    {
        mReferenceTimestamp = time;
        mDibitSinceTimestampCounter = 0;
    }


    /**
     * Calculates the timestamp accurate to the currently received dibit.
     * @return timestamp in milliseconds.
     */
    private long getTimestamp()
    {
        if(mReferenceTimestamp > 0)
        {
            return mReferenceTimestamp + (long)(1000.0 * mDibitSinceTimestampCounter / 4800);
        }
        else
        {
            mDibitSinceTimestampCounter = 0;
            return System.currentTimeMillis();
        }
    }

    private void dispatchMessage()
    {
        if(mMessageAssembler != null)
        {
            CorrectedBinaryMessage cbm = mMessageAssembler.getMessage();

            Frame frame = new Frame(cbm, getTimestamp(), mLICHTracker.getChannel(), mLICHTracker.getDirection());
            if(frame.getLICH().getOption() != Option.UNKNOWN)
            {
                mLICHTracker.track(frame.getLICH());
            }

            for(NXDNMessage message: frame.getMessages())
            {
                dispatch(message);
            }
        }

        mMessageAssembler = null;
    }

    /**
     * Dispatches the message to an optional listener
     * @param message to dispatch
     */
    private void dispatch(IMessage message)
    {
        if(mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }

    public void syncDetected()
    {
        if(mMessageAssembler != null)
        {
            dispatchMessage();
        }

        mMessageAssembler = new NXDNFrameAssembler();
    }

    /**
     * Process hard symbol decision without sync detection.
     * @param symbol that was demodulated.
     */
    public void process(Dibit symbol)
    {
        mDibitSinceTimestampCounter++;

        if(mMessageAssembler != null)
        {
            mMessageAssembler.receive(symbol);

            if(mMessageAssembler.isComplete())
            {
                dispatchMessage();
            }
        }
    }

    /**
     * Process symbol decision with (hard) sync detection
     * @param symbol to process
     */
    public void processWithHardSyncDetection(Dibit symbol)
    {
        mSymbolsSinceLastSync++;

        process(symbol);

        if(mHardSyncDetector.process(symbol))
        {
            syncDetected();
            mSymbolsSinceLastSync = 0;
        }
        else if(mSymbolsSinceLastSync == 384)
        {
            if(mHardSyncDetector.getCurrentBitErrors() <= 10)
            {
                syncDetected();
            }
        }

    }

    /**
     * Indicates if there is a non-null message assembler and it is completed, but not yet dispatched.
     * @return true if there is a complete message.
     */
    public boolean isComplete()
    {
        return false;
    }

}

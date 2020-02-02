/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.bits.DMRSoftSyncDetector;
import io.github.dsheirer.bits.MultiSyncPatternMatcher;
import io.github.dsheirer.bits.SoftSyncDetector;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.FrameSync;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.sample.Listener;

public class DMRSyncDetector implements Listener<Dibit>
{
    /* Determines the threshold for sync pattern soft matching */
    private static final int SYNC_MATCH_THRESHOLD = 4;

    private MultiSyncPatternMatcher mMatcher;
    private DMRSoftSyncDetector mBSDSyncDetector, mBSVSyncDetector, mMSDSyncDetector, mMSVSyncDetector;
    private DMRSoftSyncDetector mTDMATS1D;
    public DMRSyncDetector(IDMRSyncDetectListener syncDetectListener, IPhaseLockedLoop phaseLockedLoop)
    {
        //TODO: since we're only going to feed dibits to find next frame, it makes sense to
        //TODO: update the sync lost parameter to 48 bits ....

        //TODO: only enable the phase inversion detectors when we're in a sync-lost state
        mMatcher = new MultiSyncPatternMatcher(syncDetectListener, P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1.getMessageLength(), 48);
        mBSDSyncDetector = new DMRSoftSyncDetector(DMRSyncPattern.BASE_STATION_DATA,SYNC_MATCH_THRESHOLD, syncDetectListener);
        mMSDSyncDetector = new DMRSoftSyncDetector(DMRSyncPattern.MOBILE_STATION_DATA,SYNC_MATCH_THRESHOLD, syncDetectListener);
        mBSVSyncDetector = new DMRSoftSyncDetector(DMRSyncPattern.MOBILE_STATION_VOICE,SYNC_MATCH_THRESHOLD, syncDetectListener);
        mMSVSyncDetector = new DMRSoftSyncDetector(DMRSyncPattern.BASE_STATION_VOICE,SYNC_MATCH_THRESHOLD, syncDetectListener);
        mTDMATS1D = new DMRSoftSyncDetector(DMRSyncPattern.MOBILE_STATION_REVERSE_CHANNEL,SYNC_MATCH_THRESHOLD, syncDetectListener);

        mMatcher.add(mBSDSyncDetector);
        mMatcher.add(mBSVSyncDetector);
        mMatcher.add(mMSVSyncDetector);
        mMatcher.add(mMSDSyncDetector);
        mMatcher.add(mTDMATS1D);

        if(phaseLockedLoop != null)
        {

        }
    }

    /**
     * Calculates the number of bits that match in the current primary detector
     * @return
     */
    public int getPrimarySyncMatchErrorCount()
    {
        return 0; //Long.bitCount(mMatcher.getCurrentValue() ^ FrameSync.DMR_DIRECT_MODE_DATA_TIMESLOT_1.getSync());
    }

    @Override
    public void receive(Dibit dibit)
    {
        mMatcher.receive(dibit.getBit1(), dibit.getBit2());
    }

}

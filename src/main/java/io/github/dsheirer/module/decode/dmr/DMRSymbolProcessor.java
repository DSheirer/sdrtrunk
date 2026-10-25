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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.dsp.c4fm.C4FMSymbolProcessor;
import io.github.dsheirer.dsp.c4fm.ISoftC4FMSyncDetector;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.dmr.sync.DMRSoftSyncDetector;
import io.github.dsheirer.module.decode.dmr.sync.DMRSoftSyncDetectorFactory;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncDetectMode;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncModeMonitor;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;

/**
 * DMR implementation of the C4FM symbol processor.
 *
 * DMR employs multiple sync patterns (BASE STATION, MOBILE STATION and DIRECT MODE, each with DATA and VOICE variants)
 * and the sync detectors correlate against all active sync patterns, reporting the best scoring pattern.  The sync mode
 * monitor processes the sync detections to identify the dominant sync mode for the channel and then disables
 * processing of the two unused sync modes for the remainder of the channel.
 *
 * On each sync detection, the detected sync pattern is passed to the message framer which maintains a delay line to
 * retain the burst dibits from the CACH through the sync pattern so that the full burst can be framed.
 *
 * DMR sync patterns are made up of equal quantities of +3 and -3 symbols which supports the default (mean) balance
 * measurement approach of the parent class.
 */
public class DMRSymbolProcessor extends C4FMSymbolProcessor
{
    private static final int BURST_DIBIT_LENGTH = 144;
    private static final int SYNC_DIBIT_LENGTH = 24;
    //Maximum sync interval for data bursts is two bursts for single timeslot (ie mobile/direct mode) data.
    private static final int MAX_DATA_SYNC_INTERVAL = BURST_DIBIT_LENGTH * 2 + SYNC_DIBIT_LENGTH;
    //Maximum sync interval for a voice super frame is 6x bursts per timeslot for single timeslot (ie direct mode) voice.
    private static final int MAX_VOICE_SYNC_INTERVAL = BURST_DIBIT_LENGTH * 12 + SYNC_DIBIT_LENGTH;
    //Training period is limited to the remainder of the burst that follows the sync since the next burst can be
    //empty (ie noise) for mobile station and direct mode channels.  Testing: longer periods (120-144) gained less
    //than 0.2% on base station recordings, which doesn't justify the risk of training on empty timeslots.
    private static final int EQUALIZER_TRAINING_PERIOD_SYMBOL_COUNT = 54;
    private static final int EQUALIZER_SKIP_TRAINING_FRAME_COUNT = 10;
    private static final int EQUALIZER_SYMBOL_LENGTH = 7;
    private static final int SYMBOL_RATE = 4800;
    private static final double OUTER_SYMBOL_DEVIATION = 1944.0; //Hertz
    //Sync thresholds.  Testing against DMR recordings and injected noise: lowering the optimized and equalized
    //thresholds from 100 to 70 (60 - 70 perform the same) increased valid bursts 5% on multipath recordings and
    //7 - 27% with injected noise.  Lowering the detection threshold below 80 floods the optimizer with false syncs.
    private static final float SYNC_THRESHOLD_DETECTED = 80;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 70;
    private static final float SYNC_THRESHOLD_EQUALIZED = 70;
    //Once synchronized, sync detections are only allowed within this many symbols of a burst boundary.
    private static final int SYNC_WINDOW_SYMBOLS = 2;
    private final DMRSyncModeMonitor mSyncModeMonitor = new DMRSyncModeMonitor();
    private final DMRMessageFramer mMessageFramer;

    /**
     * Constructs an instance
     *
     * @param feedbackDecoder parent decoder
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public DMRSymbolProcessor(FeedbackDecoder feedbackDecoder, DMRMessageFramer messageFramer)
    {
        this(feedbackDecoder, messageFramer, DMRSoftSyncDetectorFactory.getDetector(),
                DMRSoftSyncDetectorFactory.getDetector());
    }

    /**
     * Constructs an instance
     *
     * @param feedbackDecoder parent decoder
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     * @param primary sync detector
     * @param lagging sync detector
     */
    private DMRSymbolProcessor(FeedbackDecoder feedbackDecoder, DMRMessageFramer messageFramer,
                                   DMRSoftSyncDetector primary, DMRSoftSyncDetector lagging)
    {
        super(feedbackDecoder, messageFramer, primary, lagging);
        mMessageFramer = messageFramer;
        mSyncModeMonitor.add(primary);
        mSyncModeMonitor.add(lagging);
    }

    /**
     * Sets base station sync detection mode.  This disables detection for MOBILE and DIRECT sync patterns.  This can
     * be used for traffic channels that are known to be part of a base station.
     * @param enabled true for base mode only, false otherwise.
     */
    public void setBaseStationMode(boolean enabled)
    {
        mSyncModeMonitor.setMode(enabled ? DMRSyncDetectMode.BASE_ONLY : DMRSyncDetectMode.AUTOMATIC);
    }

    /**
     * Captures the detected sync pattern to update the sync mode monitor and the message framer.
     * @param syncDetector that triggered the sync detection.
     */
    @Override
    protected void syncDetected(ISoftC4FMSyncDetector syncDetector)
    {
        if(syncDetector instanceof DMRSoftSyncDetector dmrSyncDetector)
        {
            DMRSyncPattern pattern = dmrSyncDetector.getDetectedPattern();
            mSyncModeMonitor.detected(pattern);
            mMessageFramer.setSyncPattern(pattern);
        }
    }

    /**
     * Once synchronized with at least two consecutive sync detections, only allows sync detections within a small
     * window around each expected burst boundary (multiples of the burst length) to prevent false sync detections
     * from burst payload (ie voice) symbols that would otherwise cause the framer to abandon the current burst.
     * @param symbolsSinceLastSync count of symbols since the final symbol of the previous sync detection.
     * @return true if a sync detection is allowed.
     */
    @Override
    protected boolean isSyncDetectionAllowed(int symbolsSinceLastSync)
    {
        if(!mSynchronized || getConsecutiveSyncPeriods() < 2)
        {
            return true;
        }

        int offset = symbolsSinceLastSync % BURST_DIBIT_LENGTH;
        return Math.min(offset, BURST_DIBIT_LENGTH - offset) <= SYNC_WINDOW_SYMBOLS;
    }

    @Override
    public boolean isDynamicBalanceTrackingRequired()
    {
        return true;
    }

    /**
     * Maximum dibit interval between sync detections, which is longer while the framer is assembling a voice super
     * frame where only the first burst of each super frame contains a sync pattern.
     */
    @Override
    public int getMaxMessageDibitLength()
    {
        return mMessageFramer.isVoiceSuperFrame() ? MAX_VOICE_SYNC_INTERVAL : MAX_DATA_SYNC_INTERVAL;
    }

    @Override
    public int getEqualizerTrainingPeriodDurationSymbolCount()
    {
        return EQUALIZER_TRAINING_PERIOD_SYMBOL_COUNT;
    }

    @Override
    public int getEqualizerSymbolLength()
    {
        return EQUALIZER_SYMBOL_LENGTH;
    }

    @Override
    public int getEqualizerSkipTrainingFrameCount()
    {
        return EQUALIZER_SKIP_TRAINING_FRAME_COUNT;
    }

    @Override
    public int getSymbolRate()
    {
        return SYMBOL_RATE;
    }

    @Override
    public double getOuterSymbolDeviation()
    {
        return OUTER_SYMBOL_DEVIATION;
    }

    @Override
    public float getSyncThresholdDetected()
    {
        return SYNC_THRESHOLD_DETECTED;
    }

    @Override
    public float getSyncThresholdOptimized()
    {
        return SYNC_THRESHOLD_OPTIMIZED;
    }

    @Override
    public float getSyncThresholdEqualized()
    {
        return SYNC_THRESHOLD_EQUALIZED;
    }
}

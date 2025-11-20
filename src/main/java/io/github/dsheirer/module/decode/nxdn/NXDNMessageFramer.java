/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.nxdn.sync.NXDNHardSyncDetector;
import io.github.dsheirer.module.decode.nxdn.sync.NXDNSoftSyncDetector;
import io.github.dsheirer.module.decode.nxdn.sync.NXDNSoftSyncDetectorFactory;
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
    private final NXDNSoftSyncDetector mSoftSyncDetector = NXDNSoftSyncDetectorFactory.getDetector();
    private final NXDNHardSyncDetector mHardSyncDetector = new NXDNHardSyncDetector();
    private boolean mSyncDetected = false;

    private static final double MILLISECONDS_PER_SYMBOL = 1.0 / 4800.0 / 1000.0;
    private Listener<IMessage> mMessageListener;
    private boolean mRunning = false;
    private int mDibitCounter = 58; //Set to 1-greater than SYNC+NID to avoid triggering message assembly on startup
    private NXDNFrameAssembler mMessageAssembler;
    private int mDebugSymbolCount = 0;

    /**
     * Constructs an instance
     */
    public NXDNMessageFramer()
    {
    }

    /**
     * Process hard symbol decision without sync detection.
     * @param symbol that was demodulated.
     */
    public boolean process(Dibit symbol, float softSymbol)
    {
        return true;
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

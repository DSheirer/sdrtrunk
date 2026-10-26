/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.EmbeddedParameters;

/**
 * DMR Voice Frames B - F
 */
public class VoiceEMBMessage extends VoiceMessage
{
    private static final int[] EMB = new int[]{132, 133, 134, 135, 136, 137, 138, 139, 172, 173, 174,
        175, 176, 177, 178, 179};
    private static final int PAYLOAD_START = 140;
    private static final int PAYLOAD_END = 172;

    private EMB mEMB;
    private final float[] mSoftBits;
    private EmbeddedParameters mEmbeddedParameters;

    /**
     * DMR message frame.  This message is comprised of a 24-bit prefix and a 264-bit message frame.  Outbound base
     * station frames transmit a Common Announcement Channel (CACH) in the 24-bit prefix, whereas Mobile inbound frames
     * do not use the 24-bit prefix.
     *
     * @param syncPattern
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public VoiceEMBMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, long timestamp, int timeslot)
    {
        this(syncPattern, message, null, cach, timestamp, timeslot);
    }

    /**
     * Constructs an instance with soft bits to use for soft decision decoding of the EMB and the embedded signalling
     * full link control fragment.
     *
     * @param syncPattern
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     * @param softBits for the 288-bit DMR message where a positive value favors a 1 bit, or null.
     */
    public VoiceEMBMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, float[] softBits, CACH cach,
                           long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, timestamp, timeslot);
        mSoftBits = softBits;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!getSyncPattern().isMobileSyncPattern() && getEMB().isValid())
        {
            sb.append("CC:").append(getEMB().getColorCode()).append(" ");
        }

        sb.append(getSyncPattern().toString());

        if(hasEmbeddedParameters())
        {
            sb.append(" ").append(getEmbeddedParameters());
        }
        else if(getEMB().isValid() && getEMB().isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }

        return sb.toString();
    }

    /**
     * EMB field
     */
    public EMB getEMB()
    {
        if(mEMB == null)
        {
            //Transfer the embedded signalling bits into a new binary message
            CorrectedBinaryMessage segment = new CorrectedBinaryMessage(16);

            for(int x = 0; x < EMB.length; x++)
            {
                segment.set(x, getMessage().get(EMB[x]));
            }

            float[] softBits = null;

            if(mSoftBits != null)
            {
                softBits = new float[EMB.length];

                for(int x = 0; x < EMB.length; x++)
                {
                    softBits[x] = mSoftBits[EMB[x]];
                }
            }

            mEMB = new EMB(segment, softBits);
        }

        return mEMB;
    }

    /**
     * Embedded signalling full link control fragment
     */
    public BinaryMessage getFLCFragment()
    {
        return getMessage().getSubMessage(PAYLOAD_START, PAYLOAD_END);
    }

    /**
     * Soft bits for the embedded signalling full link control fragment
     * @return soft bits or null if soft bits are not available.
     */
    public float[] getFLCFragmentSoftBits()
    {
        if(mSoftBits == null)
        {
            return null;
        }

        float[] softBits = new float[PAYLOAD_END - PAYLOAD_START];
        System.arraycopy(mSoftBits, PAYLOAD_START, softBits, 0, softBits.length);
        return softBits;
    }

    /**
     * Optional embedded parameters for this voice super-frame.
     * @return encryption parameters or null.
     */
    public EmbeddedParameters getEmbeddedParameters()
    {
        return mEmbeddedParameters;
    }

    /**
     * Sets the embedded parameters for this voice message that apply to the entire voice super-frame.
     *
     * These parameters are normally extracted by an external process and applied to voice frame F and the parameters
     * apply to the entire voice super-frame.
     * @param embeddedParameters to set
     */
    public void setEmbeddedParameters(EmbeddedParameters embeddedParameters)
    {
        mEmbeddedParameters = embeddedParameters;
    }

    /**
     * Indicates if this voice message contains embedded parameters.
     * @return true if it contains.
     */
    public boolean hasEmbeddedParameters()
    {
        return mEmbeddedParameters != null;
    }
}

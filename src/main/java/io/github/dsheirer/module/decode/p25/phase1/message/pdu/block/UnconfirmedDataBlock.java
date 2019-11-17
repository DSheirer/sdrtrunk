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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.block;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_1_2_P25;

/**
 * P25 Unconfirmed Data block that uses 1/2 rate trellis coding.
 */
public class UnconfirmedDataBlock extends DataBlock
{
    private static final ViterbiDecoder_1_2_P25 VITERBI_HALF_RATE_DECODER = new ViterbiDecoder_1_2_P25();
    private CorrectedBinaryMessage mDecodedMessage;

    /**
     * Constructs an unconfirmed data block from the deinterleaved message.
     * @param correctedBinaryMessage containing deinterleaved 196-bit data block.
     */
    public UnconfirmedDataBlock(CorrectedBinaryMessage correctedBinaryMessage)
    {
        mDecodedMessage = VITERBI_HALF_RATE_DECODER.decode(correctedBinaryMessage);
        mDecodedMessage.incrementCorrectedBitCount(correctedBinaryMessage.getCorrectedBitCount());
    }

    /**
     * Message payload
     */
    @Override
    public CorrectedBinaryMessage getMessage()
    {
        return mDecodedMessage;
    }

    /**
     * Number of bit errors detected/corrected during viterbi decoding.
     */
    @Override
    public int getBitErrorsCount()
    {
        return mDecodedMessage.getCorrectedBitCount();
    }

    /**
     * Indicates if this data block passes any block-level CRC.
     * @return true always for unconfirmed data blocks.
     */
    @Override
    public boolean isValid()
    {
        return true;
    }
}

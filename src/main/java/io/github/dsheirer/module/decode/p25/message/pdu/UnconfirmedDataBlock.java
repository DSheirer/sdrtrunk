package io.github.dsheirer.module.decode.p25.message.pdu;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_1_2_P25;

/**
 * P25 Unconfirmed Data block that uses 1/2 rate trellis coding.
 */
public class UnconfirmedDataBlock extends DataBlock
{
    private static final ViterbiDecoder_1_2_P25 VITERBI_HALF_RATE_DECODER = new ViterbiDecoder_1_2_P25();
    private CorrectedBinaryMessage mDecodedMessage;

    public UnconfirmedDataBlock(CorrectedBinaryMessage correctedBinaryMessage)
    {
        mDecodedMessage = VITERBI_HALF_RATE_DECODER.decode(correctedBinaryMessage);
        mDecodedMessage.incrementCorrectedBitCount(correctedBinaryMessage.getCorrectedBitCount());
    }

    @Override
    public CorrectedBinaryMessage getMessage()
    {
        return mDecodedMessage;
    }

    @Override
    public int getBitErrorsCount()
    {
        return mDecodedMessage.getCorrectedBitCount();
    }
}

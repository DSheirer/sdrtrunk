package io.github.dsheirer.module.decode.p25.message.pdu;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_3_4_P25;
import io.github.dsheirer.module.decode.p25.P25DataUnitDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfirmedDataBlock extends DataBlock
{
    private final static Logger mLog = LoggerFactory.getLogger(P25DataUnitDetector.class);

    private static final ViterbiDecoder_3_4_P25 VITERBI_THREE_QUARTER_RATE_DECODER = new ViterbiDecoder_3_4_P25();
    private CorrectedBinaryMessage mDecodedMessage;

    public ConfirmedDataBlock(CorrectedBinaryMessage correctedBinaryMessage)
    {
        mDecodedMessage = VITERBI_THREE_QUARTER_RATE_DECODER.decode(correctedBinaryMessage);
        mLog.debug("Confirmed Viterbi Corrected Errors: [" + mDecodedMessage.getCorrectedBitCount() + "]");
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

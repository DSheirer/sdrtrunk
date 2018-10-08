package io.github.dsheirer.module.decode.p25.message.pdu;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_3_4_P25;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfirmedDataBlock extends DataBlock
{
    private final static Logger mLog = LoggerFactory.getLogger(ConfirmedDataBlock.class);

    public static final int[] SEQUENCE_NUMBER = {0, 1, 2, 3, 4, 5, 6};
    public static final int PAYLOAD_START = 16;
    public static final int PAYLOAD_END = 144;

    private static final ViterbiDecoder_3_4_P25 VITERBI_THREE_QUARTER_RATE_DECODER = new ViterbiDecoder_3_4_P25();
    private CorrectedBinaryMessage mDecodedMessage;
    private boolean mValid;

    public ConfirmedDataBlock(CorrectedBinaryMessage correctedBinaryMessage)
    {
        mDecodedMessage = VITERBI_THREE_QUARTER_RATE_DECODER.decode(correctedBinaryMessage);
        mDecodedMessage.incrementCorrectedBitCount(correctedBinaryMessage.getCorrectedBitCount());

        checkCRC();
    }

    /**
     * Sequence number for the data block.
     *
     * @return sequence number between 0 - 127, or -1 if there was an error during decoding.
     */
    public int getSequenceNumber()
    {
        if(mDecodedMessage != null)
        {
            return mDecodedMessage.getInt(SEQUENCE_NUMBER);
        }

        return -1;
    }

    private void checkCRC()
    {
        CRC crc = CRCP25.checkCRC9(mDecodedMessage, 0);
        mValid = (crc == CRC.PASSED || crc == CRC.CORRECTED);
    }

    @Override
    public BinaryMessage getMessage()
    {
        return mDecodedMessage.getSubMessage(PAYLOAD_START, PAYLOAD_END);
    }

    @Override
    public int getBitErrorsCount()
    {
        return mDecodedMessage.getCorrectedBitCount();
    }

    @Override
    public boolean isValid()
    {
        return mValid;
    }
}

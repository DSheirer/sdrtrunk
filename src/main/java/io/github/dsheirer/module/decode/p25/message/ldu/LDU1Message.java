package io.github.dsheirer.module.decode.p25.message.ldu;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Hamming10;
import io.github.dsheirer.edac.ReedSolomon_63_47_17;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWordFactory;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LDU1Message extends LDUMessage implements FrequencyBandReceiver
{
    private final static Logger mLog = LoggerFactory.getLogger(LDU1Message.class);

    private static final int[] GOLAY_WORD_STARTS = {352, 362, 372, 382, 536, 546, 556, 566, 720, 730, 740, 750, 904, 914,
            924, 934, 1088, 1098, 1108, 1118, 1272, 1282, 1292, 1302};
    private static final int[] CW_HEX_0 = {352, 353, 354, 355, 356, 357};
    private static final int[] CW_HEX_1 = {362, 363, 364, 365, 366, 367};
    private static final int[] CW_HEX_2 = {372, 373, 374, 375, 376, 377};
    private static final int[] CW_HEX_3 = {382, 383, 384, 385, 386, 387};
    private static final int[] CW_HEX_4 = {536, 537, 538, 539, 540, 541};
    private static final int[] CW_HEX_5 = {546, 547, 548, 549, 550, 551};
    private static final int[] CW_HEX_6 = {556, 557, 558, 559, 560, 561};
    private static final int[] CW_HEX_7 = {566, 567, 568, 569, 570, 571};
    private static final int[] CW_HEX_8 = {720, 721, 722, 723, 724, 725};
    private static final int[] CW_HEX_9 = {730, 731, 732, 733, 734, 735};
    private static final int[] CW_HEX_10 = {740, 741, 742, 743, 744, 745};
    private static final int[] CW_HEX_11 = {750, 751, 752, 753, 754, 755};
    private static final int[] RS_HEX_0 = {904, 905, 906, 907, 908, 909};
    private static final int[] RS_HEX_1 = {914, 915, 916, 917, 918, 919};
    private static final int[] RS_HEX_2 = {924, 925, 926, 927, 928, 929};
    private static final int[] RS_HEX_3 = {934, 935, 936, 937, 938, 939};
    private static final int[] RS_HEX_4 = {1088, 1089, 1090, 1091, 1092, 1093};
    private static final int[] RS_HEX_5 = {1098, 1099, 1100, 1101, 1102, 1103};
    private static final int[] RS_HEX_6 = {1108, 1109, 1110, 1111, 1112, 1113};
    private static final int[] RS_HEX_7 = {1118, 1119, 1120, 1121, 1122, 1123};
    private static final int[] RS_HEX_8 = {1272, 1273, 1274, 1275, 1276, 1277};
    private static final int[] RS_HEX_9 = {1282, 1283, 1284, 1285, 1286, 1287};
    private static final int[] RS_HEX_10 = {1292, 1293, 1294, 1295, 1296, 1297};
    private static final int[] RS_HEX_11 = {1302, 1303, 1304, 1305, 1306, 1307};

    /* Reed-Solomon(24,12,13) code protects the link control word.  Maximum
     * correctable errors are: Hamming Distance(13) / 2 = 6  */
    private static final ReedSolomon_63_47_17 REED_SOLOMON_63_47_17 = new ReedSolomon_63_47_17(6);

    private LinkControlWord mLinkControlWord;
    private List<IIdentifier> mIdentifiers;

    public LDU1Message(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);
    }

    @Override
    public DataUnitID getDUID()
    {
        return DataUnitID.LOGICAL_LINK_DATA_UNIT_1;
    }

    public LinkControlWord getLinkControlWord()
    {
        if(mLinkControlWord == null)
        {
            createLinkControlWord();
        }

        return mLinkControlWord;
    }

    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getNAC());

            if(getLinkControlWord().isValid())
            {
                mIdentifiers.addAll(getLinkControlWord().getIdentifiers());
            }
        }

        return mIdentifiers;
    }

    /**
     * Performs error detection and correction on Link Control Word bits and extracts the 72-bit word
     */
    private void createLinkControlWord()
    {
        //Perform error detection and a maximum of one bit error correction using Hamming( 10,6,3 )
        for(int index : GOLAY_WORD_STARTS)
        {
            //Attempt to fix any single-bit errors
            Hamming10.checkAndCorrect(getMessage(), index);
        }

        //Perform Reed-Solomon( 24,16,9 ) error detection and correction.  Check the Reed-Solomon parity bits. The RS
        // decoder expects the code words and reed solomon parity hex codewords in reverse order.  Since this is a
        // truncated RS(63) codes, we pad the code with zeros
        int[] input = new int[63];
        int[] output = new int[63];

        input[0] = getMessage().getInt(RS_HEX_11);
        input[1] = getMessage().getInt(RS_HEX_10);
        input[2] = getMessage().getInt(RS_HEX_9);
        input[3] = getMessage().getInt(RS_HEX_8);
        input[4] = getMessage().getInt(RS_HEX_7);
        input[5] = getMessage().getInt(RS_HEX_6);
        input[6] = getMessage().getInt(RS_HEX_5);
        input[7] = getMessage().getInt(RS_HEX_4);
        input[8] = getMessage().getInt(RS_HEX_3);
        input[9] = getMessage().getInt(RS_HEX_2);
        input[10] = getMessage().getInt(RS_HEX_1);
        input[11] = getMessage().getInt(RS_HEX_0);

        input[12] = getMessage().getInt(CW_HEX_11);
        input[13] = getMessage().getInt(CW_HEX_10);
        input[14] = getMessage().getInt(CW_HEX_9);
        input[15] = getMessage().getInt(CW_HEX_8);
        input[16] = getMessage().getInt(CW_HEX_7);
        input[17] = getMessage().getInt(CW_HEX_6);
        input[18] = getMessage().getInt(CW_HEX_5);
        input[19] = getMessage().getInt(CW_HEX_4);
        input[20] = getMessage().getInt(CW_HEX_3);
        input[21] = getMessage().getInt(CW_HEX_2);
        input[22] = getMessage().getInt(CW_HEX_1);
        input[23] = getMessage().getInt(CW_HEX_0);
        /* indexes 24 - 62 are defaulted to zero */

        boolean irrecoverableErrors = REED_SOLOMON_63_47_17.decode(input, output);

        //Transfer error corrected output to a new binary message
        BinaryMessage binaryMessage = new BinaryMessage(72);

        int pointer = 0;

        for(int x = 23; x >= 12; x--)
        {
            if(output[x] != -1)
            {
                binaryMessage.load(pointer, 6, output[x]);
            }

            pointer += 6;
        }

        mLinkControlWord = LinkControlWordFactory.create(binaryMessage);

        if(irrecoverableErrors)
        {
            mLinkControlWord.setValid(false);
        }
        else
        {
            //If we corrected any bit errors, update the original message with the bit error count
            for(int x = 0; x < 23; x++)
            {
                if(output[x] != input[x])
                {
                    getMessage().incrementCorrectedBitCount(Integer.bitCount((output[x] ^ input[x])));
                }
            }
        }
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        if(getLinkControlWord().isValid() && getLinkControlWord() instanceof FrequencyBandReceiver)
        {
            return ((FrequencyBandReceiver) getLinkControlWord()).getChannels();
        }

        return Collections.EMPTY_LIST;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());
        sb.append(" ").append(getLinkControlWord());

        return sb.toString();
    }
}

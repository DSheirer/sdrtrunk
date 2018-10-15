package io.github.dsheirer.module.decode.p25.message.ldu;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Hamming10;
import io.github.dsheirer.edac.ReedSolomon_63_47_17;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LDU2Message extends LDUMessage
{
    private final static Logger mLog = LoggerFactory.getLogger(LDU2Message.class);

    public static final int[] GOLAY_WORD_STARTS = {352, 362, 372, 382, 536, 546, 556, 566, 720, 730, 740, 750, 904,
            914, 924, 934, 1088, 1098, 1108, 1118, 1272, 1282, 1292, 1302};

    public static final int[] CW_HEX_0 = {352, 353, 354, 355, 356, 357};
    public static final int[] CW_HEX_1 = {362, 363, 364, 365, 366, 367};
    public static final int[] CW_HEX_2 = {372, 373, 374, 375, 376, 377};
    public static final int[] CW_HEX_3 = {382, 383, 384, 385, 386, 387};
    public static final int[] CW_HEX_4 = {536, 537, 538, 539, 540, 541};
    public static final int[] CW_HEX_5 = {546, 547, 548, 549, 550, 551};
    public static final int[] CW_HEX_6 = {556, 557, 558, 559, 560, 561};
    public static final int[] CW_HEX_7 = {566, 567, 568, 569, 570, 571};
    public static final int[] CW_HEX_8 = {720, 721, 722, 723, 724, 725};
    public static final int[] CW_HEX_9 = {730, 731, 732, 733, 734, 735};
    public static final int[] CW_HEX_10 = {740, 741, 742, 743, 744, 745};
    public static final int[] CW_HEX_11 = {750, 751, 752, 753, 754, 755};
    public static final int[] CW_HEX_12 = {904, 905, 906, 907, 908, 909};
    public static final int[] CW_HEX_13 = {914, 915, 916, 917, 918, 919};
    public static final int[] CW_HEX_14 = {924, 925, 926, 927, 928, 929};
    public static final int[] CW_HEX_15 = {934, 935, 936, 937, 938, 939};
    public static final int[] RS_HEX_0 = {1088, 1089, 1090, 1091, 1092, 1093};
    public static final int[] RS_HEX_1 = {1098, 1099, 1100, 1101, 1102, 1103};
    public static final int[] RS_HEX_2 = {1108, 1109, 1110, 1111, 1112, 1113};
    public static final int[] RS_HEX_3 = {1118, 1119, 1120, 1121, 1122, 1123};
    public static final int[] RS_HEX_4 = {1272, 1273, 1274, 1275, 1276, 1277};
    public static final int[] RS_HEX_5 = {1282, 1283, 1284, 1285, 1286, 1287};
    public static final int[] RS_HEX_6 = {1292, 1293, 1294, 1295, 1296, 1297};
    public static final int[] RS_HEX_7 = {1302, 1303, 1304, 1305, 1306, 1307};

    /* Reed-Solomon(24,16,9) code protects the encryption sync word.  Maximum
     * correctable errors are: Hamming Distance(9) / 2 = 4  */
    public static final ReedSolomon_63_47_17 REED_SOLOMON_63_47_17 = new ReedSolomon_63_47_17(4);

    private EncryptionSyncParameters mEncryptionSyncParameters;
    private List<IIdentifier> mIdentifiers;

    public LDU2Message(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);
    }

    @Override
    public DataUnitID getDUID()
    {
        return DataUnitID.LOGICAL_LINK_DATA_UNIT_2;
    }

    /**
     * Encryption sync parameters allow a user to join an encrypted audio call.
     */
    public EncryptionSyncParameters getEncryptionSyncParameters()
    {
        if(mEncryptionSyncParameters == null)
        {
            createEncryptionSyncParameters();
        }

        return mEncryptionSyncParameters;
    }

    /**
     * Performs error detection and correction against the encryption sync parameters portion of the message and
     * extracts that message into an encryption sync parameters instance.
     */
    private void createEncryptionSyncParameters()
    {
        /* Hamming( 10,6,3 ) error detection and correction */
        for(int index : GOLAY_WORD_STARTS)
        {
            int errors = Hamming10.checkAndCorrect(getMessage(), index);
        }

        /* Reed-Solomon( 24,16,9 ) error detection and correction
         * Check the Reed-Solomon parity bits. The RS decoder expects the code
         * words and reed solomon parity hex codewords in reverse order.
         *
         * Since this is a truncated RS(63) codes, we pad the code with zeros */
        int[] input = new int[63];
        int[] output = new int[63];

        input[0] = getMessage().getInt(RS_HEX_7);
        input[1] = getMessage().getInt(RS_HEX_6);
        input[2] = getMessage().getInt(RS_HEX_5);
        input[3] = getMessage().getInt(RS_HEX_4);
        input[4] = getMessage().getInt(RS_HEX_3);
        input[5] = getMessage().getInt(RS_HEX_2);
        input[6] = getMessage().getInt(RS_HEX_1);
        input[7] = getMessage().getInt(RS_HEX_0);

        input[8] = getMessage().getInt(CW_HEX_15);
        input[9] = getMessage().getInt(CW_HEX_14);
        input[10] = getMessage().getInt(CW_HEX_13);
        input[11] = getMessage().getInt(CW_HEX_12);
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

        BinaryMessage binaryMessage = new BinaryMessage(96);

        int pointer = 0;

        for(int x = 23; x >= 8; x--)
        {
            if(output[x] != -1)
            {
                binaryMessage.load(pointer, 6, output[x]);
            }

            pointer += 6;
        }

        mEncryptionSyncParameters = new EncryptionSyncParameters(binaryMessage);

        if(irrecoverableErrors)
        {
            mEncryptionSyncParameters.setValid(false);
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

    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getNAC());

            if(getEncryptionSyncParameters().isValid())
            {
                mIdentifiers.addAll(getEncryptionSyncParameters().getIdentifiers());
            }
        }

        return mIdentifiers;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" ").append(getEncryptionSyncParameters());

        return sb.toString();
    }
}

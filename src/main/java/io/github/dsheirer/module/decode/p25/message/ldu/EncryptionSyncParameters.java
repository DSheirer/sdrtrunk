package io.github.dsheirer.module.decode.p25.message.ldu;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.node.APCO25EncryptionKey;
import io.github.dsheirer.module.decode.p25.reference.Encryption;

import java.util.ArrayList;
import java.util.List;

/**
 * Encryption Sync Parameters from Logical Link Data Unit 2 voice frame.
 */
public class EncryptionSyncParameters
{
    private static final int[] MESSAGE_INDICATOR_1 = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] MESSAGE_INDICATOR_2 = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] MESSAGE_INDICATOR_3 = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] MESSAGE_INDICATOR_4 = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] MESSAGE_INDICATOR_5 = {32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] MESSAGE_INDICATOR_6 = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] MESSAGE_INDICATOR_7 = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] MESSAGE_INDICATOR_8 = {56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] MESSAGE_INDICATOR_9 = {64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] ALGORITHM_ID = {72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] KEY_ID = {80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};

    private BinaryMessage mMessage;
    private boolean mValid = true;
    private String mMessageIndicator;
    private APCO25EncryptionKey mEncryptionKey;
    private List<IIdentifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public EncryptionSyncParameters(BinaryMessage message)
    {
        mMessage = message;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(isEncryptedAudio())
        {
            sb.append(getEncryptionKey());
            sb.append(" MSG INDICATOR:").append(getMessageIndicator());
        }
        else
        {
            sb.append("UNENCRYPTED");
        }
        return sb.toString();
    }

    private BinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Indicates if this message is valid or not.
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Flags this message as valid or invalid
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    public String getMessageIndicator()
    {
        if(mMessageIndicator == null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_1, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_2, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_3, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_4, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_5, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_6, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_7, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_8, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_9, 2));
            mMessageIndicator = sb.toString();
        }

        return mMessageIndicator;
    }

    public APCO25EncryptionKey getEncryptionKey()
    {
        if(mEncryptionKey == null)
        {
            mEncryptionKey = APCO25EncryptionKey.create(getMessage().getInt(ALGORITHM_ID), getMessage().getInt(KEY_ID));
        }

        return mEncryptionKey;
    }

    /**
     * Indicates if the audio stream is encrypted
     */
    public boolean isEncryptedAudio()
    {
        return getEncryptionKey().getEncryption() != Encryption.UNENCRYPTED;
    }

    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(isEncryptedAudio())
            {
                mIdentifiers.add(getEncryptionKey());
            }
        }

        return mIdentifiers;
    }
}

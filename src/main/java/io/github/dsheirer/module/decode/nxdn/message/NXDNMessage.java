package io.github.dsheirer.module.decode.nxdn.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.protocol.Protocol;

/**
 * Base NXDN message implementation
 */
public abstract class NXDNMessage implements IMessage
{
    protected static int OCTET_0 = 0;
    protected static int OCTET_1 = 8;
    protected static int OCTET_2 = 16;
    protected static int OCTET_3 = 24;
    protected static int OCTET_4 = 32;
    protected static int OCTET_5 = 40;
    protected static int OCTET_6 = 48;
    protected static int OCTET_7 = 56;
    protected static int OCTET_8 = 64;
    protected static int OCTET_9 = 72;
    protected static int OCTET_10 = 80;
    protected static int OCTET_11 = 88;
    protected static int OCTET_12 = 96;

    protected static int FLAG_1_INDEX = 0;
    protected static int FLAG_2_INDEX = 1;
    protected static final IntField MESSAGE_TYPE = IntField.length6(OCTET_0 + 2);
    protected static final IntField SOURCE = IntField.length16(OCTET_3);
    protected static final IntField DESTINATION = IntField.length16(OCTET_5);

    private CorrectedBinaryMessage mMessage;
    private long mTimestamp;
    private boolean mValid = true;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public NXDNMessage(CorrectedBinaryMessage message, long timestamp)
    {
        mMessage = message;
        mTimestamp = timestamp;
    }

    /**
     * Identifies the message type value from the binary message.
     *
     * @param message with message type value.
     * @return value.
     */
    public static int getMessageTypeValue(CorrectedBinaryMessage message)
    {
        return message.getInt(MESSAGE_TYPE);
    }

    /**
     * Message type
     *
     * @return message type
     */
    public abstract NXDNMessageType getMessageType();

    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Underlying binary message
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    @Override
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Marks this message as valid or invalid.
     *
     * @param valid flag
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.NXDN;
    }

    @Override
    public int getTimeslot()
    {
        return 0;
    }
}

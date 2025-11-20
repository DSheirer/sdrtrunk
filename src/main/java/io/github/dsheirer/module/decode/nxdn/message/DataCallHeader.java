package io.github.dsheirer.module.decode.nxdn.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.bits.LongField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.type.DataCallOption;
import io.github.dsheirer.module.decode.nxdn.type.Delivery;

import java.util.List;

/**
 * Data call header message
 */
public class DataCallHeader extends Call
{
    private static final int DELIVERY_FLAG = OCTET_8;
    private static final int SELECTIVE_RETRY_FLAG = OCTET_8 + 2;
    private static final IntField BLOCK_COUNT = IntField.length4(OCTET_8 + 4);
    private static final IntField PAD_OCTET_COUNT = IntField.length4(OCTET_9);
    private static final int START_FRAGMENT_FLAG = OCTET_9 + 5;
    private static final int CIRCULAR_FRAGMENT_FLAG = OCTET_9 + 6;
    private static final IntField TX_FRAGMENT_COUNT = IntField.length9(OCTET_9 + 7);
    private static final LongField INITIALIZATION_VECTOR = LongField.length64(OCTET_11);

    private DataCallOption mDataCallOption;

    /**
     * Constructs an instance
     *
     * @param message   with binary data
     * @param timestamp for the message
     */
    public DataCallHeader(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.TC_DATA_CALL_HEADER;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DATA CALL HEADER");
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getDelivery()).append(" DELIVERY");
        if(isSelectiveRetry())
        {
            sb.append(" SELECTIVE RETRY");
        }
        sb.append(" BLOCKS:").append(getBlockCount());
        sb.append(" PAD OCTETS:").append(getPadOctetCount());
        if(isFirstFragment())
        {
            sb.append(" FIRST FRAGMENT REMAINING:").append(getTXFragmentCount());
        }
        else
        {
            sb.append(" FRAGMENTS REMAINING:").append(getTXFragmentCount());
        }

        if(isCircularFragmentCount())
        {
            sb.append(" OF MANY");
        }

        if(getEncryptionKeyIdentifier().isEncrypted())
        {
            sb.append(" ").append(getEncryptionKeyIdentifier());
            sb.append(" IV:").append(getInitializationVector());
        }

        return sb.toString();
    }

    /**
     * Data call options
     */
    public DataCallOption getCallOption()
    {
        if(mDataCallOption == null)
        {
            mDataCallOption = new DataCallOption(getMessage().getInt(CALL_OPTION));
        }

        return mDataCallOption;
    }

    /**
     * Initialization vector as hex string.
     */
    public String getInitializationVector()
    {
        return Long.toHexString(getMessage().getLong(INITIALIZATION_VECTOR)).toUpperCase();
    }

    /**
     * Packet delivery confirmation
     */
    public Delivery getDelivery()
    {
        return getMessage().get(DELIVERY_FLAG) ? Delivery.CONFIRMED : Delivery.UNCONFIRMED;
    }

    /**
     * Indicates if this is a selective retry (true) or normal (false) transmission packet.
     */
    public boolean isSelectiveRetry()
    {
        return getMessage().get(SELECTIVE_RETRY_FLAG);
    }

    /**
     * User data block count (to follow)
     */
    public int getBlockCount()
    {
        return getMessage().getInt(BLOCK_COUNT);
    }

    /**
     * Count of pad octets added to the message to round up to the nearest block size.
     */
    public int getPadOctetCount()
    {
        return getMessage().getInt(PAD_OCTET_COUNT);
    }

    /**
     * Indicates if this is the first fragment (true) or not first fragment (false).
     */
    public boolean isFirstFragment()
    {
        return getMessage().get(START_FRAGMENT_FLAG);
    }

    /**
     * Indicates if the TX fragment count circulates (true) or not (false).
     */
    public boolean isCircularFragmentCount()
    {
        return getMessage().get(CIRCULAR_FRAGMENT_FLAG);
    }

    /**
     * Fragment sequence count.
     *
     * Circular Fragment Count=FALSE: value is total fragments minus one and last fragement will be zero
     * Circular Fragment Count=TRUE: value is 15 in first fragment, and then decremented mod 9 each fragment after
     */
    public int getTXFragmentCount()
    {
        return getMessage().getInt(TX_FRAGMENT_COUNT);
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination(), getEncryptionKeyIdentifier());
    }
}

package io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.alias.DmrTalkerAliasIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Capacity Max Talker Alias (FLCO 20 / FID 0x10)
 * <p>
 * See also: FLCO 21 - Talker Alias Continuation
 */
public class CapacityMaxTalkerAlias extends CapacityPlusVoiceChannelUser
{
    private static final int[] ENCODING_TYPE = new int[]{16, 17, 18};
    private static final int[] LENGTH = new int[]{19, 20, 21, 22};
    private static final int ALIAS_START = 24;
    private static final int ALIAS_END = ALIAS_START + (6 * 8); //6 characters?
    private DmrTalkerAliasIdentifier mTalkerAliasIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     * @param timestamp
     * @param timeslot
     */
    public CapacityMaxTalkerAlias(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    public int getLength()
    {
        return getMessage().getInt(LENGTH);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }
        if(isEncrypted())
        {
            sb.append(" *ENCRYPTED*");
        }
        if(isReservedBitSet())
        {
            sb.append(" *RESERVED-BIT*");
        }

        sb.append("FLC MOTOROLA CAPMAX TALKER ALIAS:").append(getTalkerAliasIdentifier());
        sb.append(" LENGTH:").append(getLength());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Talker alias identifier
     *
     * @return identifier
     */
    public DmrTalkerAliasIdentifier getTalkerAliasIdentifier()
    {
        if(mTalkerAliasIdentifier == null)
        {
            String alias = new String(getMessage().get(ALIAS_START, ALIAS_END).getBytes()).trim();
            mTalkerAliasIdentifier = DmrTalkerAliasIdentifier.create(alias);
        }

        return mTalkerAliasIdentifier;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkerAliasIdentifier());
        }

        return mIdentifiers;
    }
}

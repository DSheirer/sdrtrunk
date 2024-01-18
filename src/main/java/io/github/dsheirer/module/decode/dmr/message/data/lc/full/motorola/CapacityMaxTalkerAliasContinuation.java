package io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.alias.DmrTalkerAlias2Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Capacity Max Talker Alias Continuation (FLCO 21 / FID 0x10)
 */
public class CapacityMaxTalkerAliasContinuation extends CapacityPlusVoiceChannelUser
{
    private static final int ALIAS_START = 16;
    private static final int ALIAS_END = ALIAS_START + (7 * 8);
    private DmrTalkerAlias2Identifier mTalkerAliasIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     * @param timestamp
     * @param timeslot
     */
    public CapacityMaxTalkerAliasContinuation(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
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

        sb.append("FLC MOTOROLA CAPMAX TALKER ALIAS CONTINUED:").append(getTalkerAliasIdentifier());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Talker alias identifier
     * @return identifier
     */
    public DmrTalkerAlias2Identifier getTalkerAliasIdentifier()
    {
        if(mTalkerAliasIdentifier == null)
        {
            String alias = new String(getMessage().get(ALIAS_START, ALIAS_END).getBytes()).trim();
            mTalkerAliasIdentifier = DmrTalkerAlias2Identifier.create(alias);
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

package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.identifier.IdentifierCollection;

/**
 * Snapshot of a decode event that also preserves the object instance hash code.
 */
public class DecodeEventSnapshot extends DecodeEvent
{
    private int mOriginalHashCode;

    /**
     * Constructs an instance
     *
     * @param decodeEvent to snapshot
     */
    public DecodeEventSnapshot(DecodeEvent decodeEvent)
    {
        super(decodeEvent.getEventType(), decodeEvent.getTimeStart());
        mOriginalHashCode = decodeEvent.hashCode();
        setDetails(decodeEvent.getDetails());
        setDuration(decodeEvent.getDuration());
        setProtocol(decodeEvent.getProtocol());
        setTimeslot(decodeEvent.getTimeslot());
        setChannelDescriptor(decodeEvent.getChannelDescriptor());
        if(decodeEvent.getIdentifierCollection() != null)
        {
            setIdentifierCollection(new IdentifierCollection(decodeEvent.getIdentifierCollection().getIdentifiers()));
        }
    }

    /**
     * Hashcode of the original decode event for this snapshot (not this snapshot instance's hash code).
     * @return
     */
    public int getOriginalHashCode()
    {
        return mOriginalHashCode;
    }

    /**
     * Frequency for the channel descriptor
     * @return frequency or zero.
     */
    public long getFrequency()
    {
        return getChannelDescriptor() != null ? getChannelDescriptor().getDownlinkFrequency() : 0;
    }

}

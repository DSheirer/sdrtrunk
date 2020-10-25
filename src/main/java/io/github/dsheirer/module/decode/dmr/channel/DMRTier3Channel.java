package io.github.dsheirer.module.decode.dmr.channel;

/**
 * DMR Tier III Trunking Channel
 */
public class DMRTier3Channel extends DMRLogicalChannel
{
    /**
     * Constructs an instance.  Note: radio reference uses a one based index, so we add a value of one to the
     * calculated logical slot value for visual compatibility for users.
     *
     * @param channel number or repeater number
     * @param timeslot
     */
    public DMRTier3Channel(int channel, int timeslot)
    {
        super(channel, timeslot);
    }

    /**
     * Logical slot number for this channel.
     *
     * Formula: LSN = (channel * 2) + timeslot
     *
     * @return logical slot number, a 1-based index value
     */
    @Override
    public int getLogicalSlotNumber()
    {
        int repeater = getRepeater();

        if(repeater > 0)
        {
            return ((repeater) * 2) + getTimeslot();
        }

        return 0;
    }

}

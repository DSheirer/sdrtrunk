package io.github.dsheirer.module.decode.p25.reference;

/**
 * Harris group regroup options
 */
public class RegroupOptions
{
    private static final int MASK_REGROUP_TYPE = 0x80;
    private static final int MASK_GROUP_INDIVIDUAL = 0x40;
    private static final int MASK_ACTIVATE = 0x20;
    private static final int MASK_SUPERGROUP_SEQUENCE_NUMBER = 0x1F;

    private int mValue;

    /**
     * Constructs an instance
     * @param value
     */
    public RegroupOptions(int value)
    {
        mValue = value;
    }

    /**
     * Type of regrouping
     * @return regroup type
     */
    public RegroupType getRegroupType()
    {
        return (mValue & MASK_REGROUP_TYPE) == MASK_REGROUP_TYPE ?
                RegroupType.SIMULSELECT_ONE_WAY_REGROUP : RegroupType.PATCH_TWO_WAY_REGROUP;
    }

    /**
     * Indicates if this is a two-way patch (true) or one-way simul-select (false) regrouping
     * @return patch (true) or simulselect (false)
     */
    public boolean isPatch()
    {
        return getRegroupType() == RegroupType.PATCH_TWO_WAY_REGROUP;
    }

    /**
     * Indicates if the regroup target address is a group address (true) or individual address (false).
     * @return true for group or false for individual address.
     */
    public boolean isTalkgroupAddress()
    {
        return (mValue & MASK_GROUP_INDIVIDUAL) == MASK_GROUP_INDIVIDUAL;
    }

    /**
     * Indicates if this is an activation (true) or a deactivation (false) of the supergroup.
     * @return activation (true) or deactivation (false)
     */
    public boolean isActivate()
    {
        return (mValue & MASK_ACTIVATE) == MASK_ACTIVATE;
    }

    /**
     * Unique identifier for the regrouping
     * @return unique id SSN
     */
    public int getSupergroupSequenceNumber()
    {
        return (mValue & MASK_SUPERGROUP_SEQUENCE_NUMBER);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(isActivate() ? "ACTIVATE" : "DEACTIVATE");
        sb.append(isTalkgroupAddress() ? " TALKGROUP" : " INDIVIDUAL");
        sb.append(isPatch() ? " PATCH" : " SIMUL-SELECT");
        return sb.toString();
    }

    public enum RegroupType {PATCH_TWO_WAY_REGROUP, SIMULSELECT_ONE_WAY_REGROUP};
}

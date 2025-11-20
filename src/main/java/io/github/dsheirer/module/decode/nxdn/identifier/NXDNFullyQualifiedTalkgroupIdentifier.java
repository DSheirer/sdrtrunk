package io.github.dsheirer.module.decode.nxdn.identifier;

import io.github.dsheirer.identifier.Role;

public class NXDNFullyQualifiedTalkgroupIdentifier extends NXDNTalkgroupIdentifier
{
    private int mSystem;

    /**
     * Constructs an instance
     *
     * @param value for the talkgroup
     * @param role  for the talkgroup
     */
    public NXDNFullyQualifiedTalkgroupIdentifier(int system, int value, Role role)
    {
        super(value, role);
        mSystem = system;
    }

    /**
     * Creates an instance with a role of TO
     * @param system that is home for the talkgroup
     * @param talkgroup value
     * @return TO instance
     */
    public static NXDNFullyQualifiedTalkgroupIdentifier createTo(int system, int talkgroup)
    {
        return new NXDNFullyQualifiedTalkgroupIdentifier(system, talkgroup, Role.TO);
    }

    /**
     * Creates an instance with a role of ANY
     * @param system that is home for the talkgroup
     * @param talkgroup value
     * @return ANY instance
     */
    public static NXDNFullyQualifiedTalkgroupIdentifier createAny(int system, int talkgroup)
    {
        return new NXDNFullyQualifiedTalkgroupIdentifier(system, talkgroup, Role.ANY);
    }
}

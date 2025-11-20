package io.github.dsheirer.module.decode.nxdn.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * NXDN Talkgroup Identifier
 */
public class NXDNTalkgroupIdentifier extends TalkgroupIdentifier
{
    /**
     * Constructs an instance
     *
     * @param value for the talkgroup
     * @param role  for the talkgroup
     */
    public NXDNTalkgroupIdentifier(Integer value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.NXDN;
    }

    /**
     * Creates an NXDN talkgroup identifier with the TO role.
     * @param value of the talkgroup
     * @return identifier
     */
    public static NXDNTalkgroupIdentifier to(int value)
    {
        return new NXDNTalkgroupIdentifier(value, Role.TO);
    }
}

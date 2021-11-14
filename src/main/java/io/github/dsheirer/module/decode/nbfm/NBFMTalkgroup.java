package io.github.dsheirer.module.decode.nbfm;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Narrowband FM talkgroup.  Note: this is a logical identifier that is assignable from a user-specified
 * configuration so that NBFM audio events are compatible with other features of the sdrtrunk system.
 */
public class NBFMTalkgroup extends TalkgroupIdentifier
{
    /**
     * Constructs an instance
     * @param value 1-65,535
     */
    public NBFMTalkgroup(int value)
    {
        super(value, Role.TO);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.NBFM;
    }
}

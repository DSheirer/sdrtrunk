package io.github.dsheirer.module.decode.p25.message;

import io.github.dsheirer.channel.traffic.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;

/**
 * Interface for adjacent site (ie neighbor) messages
 */
public interface IAdjacentSite
{
    String getUniqueID();

    Identifier getRFSSId();

    Identifier getSystemID();

    Identifier getSiteID();

    Identifier getLRAId();

    String getSystemServiceClass();

    IChannelDescriptor getChannel();
}

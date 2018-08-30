package io.github.dsheirer.module.decode.p25.message;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;

/**
 * Interface for adjacent site (ie neighbor) messages
 */
public interface IAdjacentSite
{
    String getUniqueID();

    IIdentifier getRFSSId();

    IIdentifier getSystemID();

    IIdentifier getSiteID();

    IIdentifier getLRAId();

    String getSystemServiceClass();

    IAPCO25Channel getChannel();
}

package io.github.dsheirer.module.decode.nxdn.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * NXDN Radio Identifier
 */
public class NXDNRadioIdentifier extends RadioIdentifier
{
    public NXDNRadioIdentifier(Integer value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.NXDN;
    }

    /**
     * Creates an NXDN radio identifier with the FROM role.
     * @param value of the radio ID
     * @return identifier
     */
    public static NXDNRadioIdentifier createFrom(int value)
    {
        return new NXDNRadioIdentifier(value, Role.FROM);
    }

    /**
     * Creates an NXDN radio identifier with the TO role.
     * @param value of the radio ID
     * @return identifier
     */
    public static NXDNRadioIdentifier createTo(int value)
    {
        return new NXDNRadioIdentifier(value, Role.TO);
    }

    /**
     * Creates an NXDN radio identifier with the ANY role.
     * @param value of the radio ID
     * @return identifier
     */
    public static NXDNRadioIdentifier createAny(int value)
    {
        return new NXDNRadioIdentifier(value, Role.ANY);
    }
}

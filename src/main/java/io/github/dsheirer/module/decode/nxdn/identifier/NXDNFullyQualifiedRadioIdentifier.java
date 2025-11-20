package io.github.dsheirer.module.decode.nxdn.identifier;

import io.github.dsheirer.identifier.Role;

/**
 * Inter-system, ISSI or Roaming NXDN radio identifier.
 */
public class NXDNFullyQualifiedRadioIdentifier extends NXDNRadioIdentifier
{
    private int mSystem;

    /**
     * Constructs an instance
     * @param system that is home for the radio
     * @param radio identifier
     * @param role for the identifier
     */
    public NXDNFullyQualifiedRadioIdentifier(int system, int radio, Role role)
    {
        super(radio, role);
        mSystem = system;
    }

    @Override
    public String toString()
    {
        return mSystem + "." + super.toString();
    }

    /**
     * Creates a fully qualified radio identifier with a role of TO
     * @param system that is home for the radio
     * @param radio identifier
     * @return identifier
     */
    public static NXDNFullyQualifiedRadioIdentifier createTo(int system, int radio)
    {
        return new NXDNFullyQualifiedRadioIdentifier(system, radio, Role.TO);
    }

    /**
     * Creates a fully qualified radio identifier with a role of FRON
     * @param system that is home for the radio
     * @param radio identifier
     * @return identifier
     */
    public static NXDNFullyQualifiedRadioIdentifier createFrom(int system, int radio)
    {
        return new NXDNFullyQualifiedRadioIdentifier(system, radio, Role.FROM);
    }

    /**
     * Creates a fully qualified radio identifier with a role of ANY
     * @param system that is home for the radio
     * @param radio identifier
     * @return identifier
     */
    public static NXDNFullyQualifiedRadioIdentifier createAny(int system, int radio)
    {
        return new NXDNFullyQualifiedRadioIdentifier(system, radio, Role.ANY);
    }
}

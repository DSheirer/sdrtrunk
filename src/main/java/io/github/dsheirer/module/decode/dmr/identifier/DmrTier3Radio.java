/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.dmr.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.message.type.Tier3Gateway;

/**
 * DMR Tier-III (ie trunked) radio identifier with custom overrides to correctly label Gateways & Identities
 */
public class DmrTier3Radio extends DMRRadio
{
    public DmrTier3Radio(Integer value, Role role)
    {
        super(value, role);
    }

    public boolean isGateway()
    {
        return Tier3Gateway.isGateway(getValue());
    }

    @Override
    public String toString()
    {
        if(isGateway())
        {
            return Tier3Gateway.fromValue(getValue()).getLabel();
        }
        else
        {
            return super.toString();
        }
    }

    /**
     * Creates a DMR TO radio identifier
     */
    public static RadioIdentifier createTo(int radioId)
    {
        return new DmrTier3Radio(radioId, Role.TO);
    }

    /**
     * Creates a DMR FROM radio identifier
     */
    public static RadioIdentifier createFrom(int radioId)
    {
        return new DmrTier3Radio(radioId, Role.FROM);
    }

    /**
     * Creates a DMR ANY radio identifier
     */
    public static RadioIdentifier createAny(int radioId)
    {
        return new DmrTier3Radio(radioId, Role.ANY);
    }

}

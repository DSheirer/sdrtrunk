/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Status call options field.
 */
public class StatusCallOption extends CallOption
{
    private static final int MASK_DELIVERY = 0x08;
    private static final int MASK_FORMAT = 0x03;

    /**
     * Constructs an instance
     *
     * @param value for the field
     */
    public StatusCallOption(int value)
    {
        super(value);
    }

    /**
     * Delivery, confirmed or unconfirmed.
     */
    public Delivery getDelivery()
    {
        return (mValue & MASK_DELIVERY) == MASK_DELIVERY ? Delivery.CONFIRMED : Delivery.UNCONFIRMED;
    }

    @Override
    public String toString()
    {
        return getDelivery() + " DELIVERY";
    }
}

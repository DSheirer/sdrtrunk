/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.libusb.UsbDeviceInfo;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Formats the USB port address of a device: the port numbers from the root hub through any intermediate hubs, in
 * dotted notation such as 2.3.  Together with the bus number it identifies the physical USB socket of a tuner.
 *
 * The tuner manager discovers tuners at this address and the tuner controllers find their device again by it, so it
 * is formatted in one place.
 */
public final class UsbPortAddress
{
    private UsbPortAddress()
    {
    }

    /**
     * Port address of a device.
     * @param device to inspect
     * @return formatted address, or an empty string for a root hub
     */
    public static String of(UsbDeviceInfo device)
    {
        return format(device.portNumbers());
    }

    private static String format(List<Integer> ports)
    {
        return ports.stream().map(String::valueOf).collect(Collectors.joining("."));
    }
}

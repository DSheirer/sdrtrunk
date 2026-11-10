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

import io.github.dsheirer.libusb.LibUsb;
import io.github.dsheirer.libusb.UsbContext;
import io.github.dsheirer.libusb.UsbDeviceHandle;
import io.github.dsheirer.libusb.UsbDeviceInfo;
import io.github.dsheirer.libusb.UsbException;
import io.github.dsheirer.source.SourceException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the USB device at a tuner's bus and port address and reads its USB identity through jlibusb.
 *
 * The tuner libraries open a device by serial number or by index rather than by bus and port, so a tuner controller
 * uses this to identify the device that the tuner manager discovered at a bus and port before handing it to its
 * library.  Devices are closed again before returning so that the library can open them.
 */
public final class UsbDeviceLookup
{
    private static final Logger mLog = LoggerFactory.getLogger(UsbDeviceLookup.class);

    /**
     * libusb error code when the user doesn't have permission to open the device (LIBUSB_ERROR_ACCESS)
     */
    public static final int ERROR_ACCESS = -3;

    /**
     * libusb error code when another program or driver has claimed the interface (LIBUSB_ERROR_BUSY)
     */
    public static final int ERROR_BUSY = -6;

    private UsbDeviceLookup()
    {
    }

    /**
     * Creates a libusb context.  Close it when done.
     * @throws SourceException if libusb can't be initialized
     */
    public static UsbContext createContext() throws SourceException
    {
        try
        {
            return UsbContext.create();
        }
        catch(UsbException e)
        {
            throw new SourceException("Can't initialize libusb library - " + LibUsb.errorName(e.code()));
        }
    }

    /**
     * Lists the USB devices, in libusb device list order.
     * @param usb context
     * @throws SourceException if the device list isn't available
     */
    public static List<UsbDeviceInfo> devices(UsbContext usb) throws SourceException
    {
        try
        {
            return usb.devices();
        }
        catch(UsbException e)
        {
            throw new SourceException("Can't get the USB device list - " + LibUsb.errorName(e.code()));
        }
    }

    /**
     * Indicates if the device is at the bus and port address.
     * @param device to test
     * @param bus number
     * @param portAddress in dotted notation, such as 2.3
     */
    public static boolean isAt(UsbDeviceInfo device, int bus, String portAddress)
    {
        return device.busNumber() == bus && !device.portNumbers().isEmpty() && portAddress != null &&
                portAddress.equals(UsbPortAddress.of(device));
    }

    /**
     * Finds the device at the bus and port address.
     * @param usb context
     * @param bus number
     * @param portAddress in dotted notation, such as 2.3
     * @throws SourceException if no device is at that bus and port
     */
    public static UsbDeviceInfo find(UsbContext usb, int bus, String portAddress) throws SourceException
    {
        for(UsbDeviceInfo device: devices(usb))
        {
            if(isAt(device, bus, portAddress))
            {
                return device;
            }
        }

        throw new SourceException("Couldn't find USB device at bus [" + bus + "] port [" + portAddress + "]");
    }

    /**
     * Finds the device at the bus and port address.
     * @param bus number
     * @param portAddress in dotted notation, such as 2.3
     * @throws SourceException if no device is at that bus and port
     */
    public static UsbDeviceInfo find(int bus, String portAddress) throws SourceException
    {
        try(UsbContext usb = createContext())
        {
            return find(usb, bus, portAddress);
        }
    }

    /**
     * Opens the device.  Close the handle before the context.
     * @param usb context
     * @param device to open
     * @throws SourceException if the device can't be opened, logging the likely cause
     */
    public static UsbDeviceHandle open(UsbContext usb, UsbDeviceInfo device) throws SourceException
    {
        try
        {
            return usb.open(device);
        }
        catch(UsbException e)
        {
            if(e.code() == ERROR_ACCESS)
            {
                mLog.error("Access to USB tuner denied - (windows) reinstall zadig driver or (linux) blacklist driver and/or check udev rules");
                throw new SourceException("access denied - if using linux, blacklist the default driver and/or install udev rules");
            }

            mLog.error("Can't open USB tuner - check driver or Linux udev rules");
            throw new SourceException("Can't open USB tuner - reinstall driver? - " + LibUsb.errorName(e.code()));
        }
    }

    /**
     * Reads the device's USB serial string.
     * @param usb context
     * @param device to read
     * @return serial, or null if the device doesn't have a serial string or it can't be read
     * @throws SourceException if the device has a serial string but can't be opened
     */
    public static String readSerialNumber(UsbContext usb, UsbDeviceInfo device) throws SourceException
    {
        if(device.serialNumberIndex() == 0)
        {
            return null;
        }

        try(UsbDeviceHandle handle = open(usb, device))
        {
            return handle.readString(device.serialNumberIndex());
        }
    }

    /**
     * Reads the USB serial string of the device at the bus and port address.
     * @param bus number
     * @param portAddress in dotted notation, such as 2.3
     * @return serial, or null if the device doesn't have a serial string or it can't be read
     * @throws SourceException if no device is at that bus and port, or it can't be opened
     */
    public static String readSerialNumber(int bus, String portAddress) throws SourceException
    {
        try(UsbContext usb = createContext())
        {
            return readSerialNumber(usb, find(usb, bus, portAddress));
        }
    }
}

/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.usb;

import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbServices;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class UsbUtils
{
    /**
     * Returns a List of currently connected usb devices
     */
    public static List<UsbDevice> getDevices() throws SecurityException, UsbException, UnsupportedEncodingException
    {
        UsbServices services = UsbHostManager.getUsbServices();
        UsbHub root = services.getRootUsbHub();
        List<UsbDevice> devices = new ArrayList<UsbDevice>(getHubDevices(root));
        return devices;
    }

    /**
     * Returns a list of devices attached to the hub
     */
    public static List<UsbDevice> getHubDevices(UsbHub hub) throws UnsupportedEncodingException, UsbException
    {
        List<UsbDevice> devices = new ArrayList<UsbDevice>();
        List<UsbDevice> children = hub.getAttachedUsbDevices();

        for(UsbDevice child : children)
        {
            if(child.isUsbHub())
            {
                devices.addAll(getHubDevices((UsbHub) child));
            }
            else
            {
                devices.add(child);
            }
        }

        return devices;
    }

    public static String getDeviceDetails(UsbDevice device) throws UsbException, UnsupportedEncodingException, UsbDisconnectedException
    {
        StringBuilder sb = new StringBuilder();
        sb.append(device.getUsbDeviceDescriptor().toString()).append("\n\n");

        for(Object configObject : device.getUsbConfigurations())
        {
            UsbConfiguration config = (UsbConfiguration) configObject;

            sb.append(config.getUsbConfigurationDescriptor().toString()).append("\n\n");

            for(Object interfaceObject : config.getUsbInterfaces())
            {
                UsbInterface iface = (UsbInterface) interfaceObject;

                sb.append(iface.getUsbInterfaceDescriptor().toString()).append("\n\n");

                for(Object endpointObject : iface.getUsbEndpoints())
                {
                    UsbEndpoint endpoint = (UsbEndpoint) endpointObject;

                    sb.append(endpoint.getUsbEndpointDescriptor().toString()).append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    public static String getDeviceClass(byte deviceClass)
    {
        switch(deviceClass)
        {
            case 0:
                return "Unknown Device - Class 0";
            case 2:
                return "Communications Device";
            case 3:
                return "HID Device";
            case 5:
                return "Physical Device";
            case 6:
                return "Still Imaging Device";
            case 7:
                return "Printer Device";
            case 8:
                return "Mass Storage Device";
            case 9:
                return "Hub Device";
            case 0xA:
                return "Communications Device";
            case 0xB:
                return "Smart Card Device";
            case 0xD:
                return "Content Security Device";
            case 0xE:
                return "Video Device";
            case 0xF:
                return "Personal Healthcare Device";
            case 0x10:
                return "Audio/Video Device";
            case (byte)0xDC:
                return "Diagnostic Device";
            case (byte)0xE0:
                return "Wireless Controller Device";
            case (byte)0xEF:
                return "Miscellaneous Device";
            default:
                return "Unknown Device - Class " + deviceClass;
        }
    }
}

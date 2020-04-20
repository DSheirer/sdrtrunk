/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
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
import java.util.Iterator;
import java.util.List;

public class USBUtils
{
	/**
	 * Returns a List of currently connected usb devices
	 */
	public static List<UsbDevice> getDevices() 
			throws SecurityException, UsbException, UnsupportedEncodingException
	{
		UsbServices services = UsbHostManager.getUsbServices();

		UsbHub root = services.getRootUsbHub();

		ArrayList<UsbDevice> devices = new ArrayList<UsbDevice>(getHubDevices(root));

		return devices;
	}

	/**
	 * Returns a list of devices attached to the hub
	 */
	public static List<UsbDevice> getHubDevices( UsbHub hub ) 
						throws UnsupportedEncodingException, UsbException
	{
		ArrayList<UsbDevice> devices = new ArrayList<UsbDevice>();
		
		@SuppressWarnings( "unchecked" )
		List<UsbDevice> children = hub.getAttachedUsbDevices();

		for (UsbDevice child : children) {
			if (child.isUsbHub()) {
				devices.addAll(getHubDevices((UsbHub) child));
			} else {
				devices.add(child);
			}
		}
				
		return devices;
	}
	
	public static String getDeviceDetails( UsbDevice device ) 
		throws UsbException, UnsupportedEncodingException, UsbDisconnectedException
	{
		StringBuilder sb = new StringBuilder();

		sb.append(device.getUsbDeviceDescriptor().toString()).append("\n\n");
		
		for( Object configObject: device.getUsbConfigurations() )
		{
			UsbConfiguration config = (UsbConfiguration)configObject;
			
			sb.append(config.getUsbConfigurationDescriptor().toString()).append("\n\n");
			
			for( Object interfaceObject: config.getUsbInterfaces() )
			{
				UsbInterface iface = (UsbInterface)interfaceObject;
				
				sb.append(iface.getUsbInterfaceDescriptor().toString()).append("\n\n");
				
				for( Object endpointObject: iface.getUsbEndpoints() )
				{
					UsbEndpoint endpoint = (UsbEndpoint)endpointObject;
					
					sb.append(endpoint.getUsbEndpointDescriptor().toString()).append("\n\n");
				}
			}
		}

		return sb.toString();
	}
}

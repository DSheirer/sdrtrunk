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

import io.github.dsheirer.source.tuner.TunerClass;

import javax.usb.UsbDevice;

public class USBTunerDevice
{
	private UsbDevice mDevice;
	private TunerClass mTunerClass;
	
	public USBTunerDevice( UsbDevice device, TunerClass tunerClass )
	{
		mDevice = device;
		mTunerClass = tunerClass;
	}
	
	public UsbDevice getDevice()
	{
		return mDevice;
	}
	
	public TunerClass getTunerClass()
	{
		return mTunerClass;
	}
}

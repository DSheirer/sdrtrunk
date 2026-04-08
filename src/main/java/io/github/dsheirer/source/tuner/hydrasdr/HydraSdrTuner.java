/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
 * Copyright (C) 2026 Benjamin Vernoux
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
package io.github.dsheirer.source.tuner.hydrasdr;

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HydraSDR Tuner using native libhydrasdr via JNI.
 */
public class HydraSdrTuner extends Tuner
{
	private static final Logger mLog = LoggerFactory.getLogger(HydraSdrTuner.class);

	/**
	 * Constructs an instance
	 * @param controller for the HydraSDR
	 * @param tunerErrorListener to listen for errors from this tuner
	 * @param channelizerType for the channelizer
	 */
	public HydraSdrTuner(HydraSdrTunerController controller, ITunerErrorListener tunerErrorListener,
		ChannelizerType channelizerType)
	{
		super(controller, tunerErrorListener, channelizerType);
	}

	@Override
	public String getPreferredName()
	{
		HydraSdrDeviceInfo info = getController().getDeviceInfo();
		if(info != null)
		{
			return "HydraSDR " + info.getSerialNumber();
		}
		return "HydraSDR";
	}

	/**
	 * HydraSDR tuner controller
	 */
	public HydraSdrTunerController getController()
	{
		return (HydraSdrTunerController)getTunerController();
	}

	@Override
	public String getUniqueID()
	{
		try
		{
			HydraSdrDeviceInfo info = getController().getDeviceInfo();
			if(info != null)
			{
				return info.getSerialNumber();
			}
		}
		catch(Exception e)
		{
			mLog.error("Error getting serial number", e);
		}

		return "HydraSDR";
	}

	@Override
	public TunerClass getTunerClass()
	{
		return TunerClass.HYDRASDR;
	}

	@Override
	public double getSampleSize()
	{
		/* 12-bit ADC + ~1 bit processing gain from libhydrasdr decimation */
		return 13.0;
	}

	@Override
	public int getMaximumUSBBitsPerSecond()
	{
		/* HydraSDR streams raw 16-bit ADC samples at 20 MSps over USB
		 * (libhydrasdr converts to 10 MHz complex IQ on the host).
		 * 2 bytes/sample x 20 MSps = 40 MB/s = 320 Mbps */
		return 320_000_000;
	}
}

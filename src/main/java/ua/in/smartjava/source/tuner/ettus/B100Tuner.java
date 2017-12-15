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
package ua.in.smartjava.source.tuner.ettus;

import java.util.concurrent.RejectedExecutionException;

import ua.in.smartjava.source.SourceException;
import ua.in.smartjava.source.tuner.Tuner;
import ua.in.smartjava.source.tuner.TunerChannel;
import ua.in.smartjava.source.tuner.TunerChannelSource;
import ua.in.smartjava.source.tuner.TunerClass;
import ua.in.smartjava.source.tuner.TunerType;
import ua.in.smartjava.source.tuner.usb.USBTunerDevice;

public class B100Tuner extends Tuner
{
	public B100Tuner( USBTunerDevice device )
	{
		//TODO: this should extend a USBTuner class, so that we can push the
		//complex ua.in.smartjava.sample listener and the frequency change listener up to that class
		
		super( "Ettus B100 with WBX Tuner", null );
	}
	
	public void dispose()
	{
		
	}
	
	@Override
    public TunerClass getTunerClass()
    {
	    return TunerClass.ETTUS_USRP_B100;
    }

	@Override
    public TunerType getTunerType()
    {
	    return TunerType.ETTUS_WBX;
    }

	@Override
	public double getSampleSize()
	{
		return 16.0;
	}

	@Override
    public TunerChannelSource getChannel( TunerChannel channel ) 
    		throws RejectedExecutionException, SourceException
    {
		//TODO: pass this call to the tuner ua.in.smartjava.controller
	    return null;
    }
	
	@Override
    public void releaseChannel( TunerChannelSource source )
    {
		//TODO: pass this call to the tuner ua.in.smartjava.controller
    }

	@Override
    public String getUniqueID()
    {
	    // TODO Auto-generated method stub
	    return null;
    }
}

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
package source.tuner.ettus;

import java.util.concurrent.RejectedExecutionException;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.SettingsManager;
import source.SourceException;
import source.tuner.Tuner;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import source.tuner.TunerClass;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.usb.USBTunerDevice;
import controller.ThreadPoolManager;

public class B100Tuner extends Tuner
{
	private final static Logger mLog = LoggerFactory.getLogger( B100Tuner.class );

	private USBTunerDevice mDevice;
	
	public B100Tuner( USBTunerDevice device )
	{
		//TODO: this should extend a USBTuner class, so that we can push the
		//complex sample listener and the frequency change listener up to that class
		
		super( "Ettus B100 with WBX Tuner" );
		
		mDevice = device;
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
    public JPanel getEditor( SettingsManager settingsManager )
    {
	    return new B100TunerEditorPanel( this, settingsManager );
    }

	@Override
    public void apply( TunerConfiguration config ) throws SourceException
    {
		mLog.error( "B100 Tuner - can't apply tuner configuration" );
    }

	@Override
    public int getSampleRate()
    {
	    return 0;
    }

	@Override
	public double getSampleSize()
	{
		return 16.0;
	}

	@Override
    public long getFrequency() throws SourceException
    {
	    return 0;
    }

	@Override
    public TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
		TunerChannel channel ) throws RejectedExecutionException, SourceException
    {
		//TODO: pass this call to the tuner controller
	    return null;
    }

	@Override
    public void releaseChannel( TunerChannelSource source )
    {
		//TODO: pass this call to the tuner controller
    }

	@Override
    public String getUniqueID()
    {
	    // TODO Auto-generated method stub
	    return null;
    }
}

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package module.decode.p25;

import instrument.tap.Tap;
import instrument.tap.stream.DibitTap;
import instrument.tap.stream.FloatBufferTap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.real.IRealBufferListener;
import sample.real.RealBuffer;
import source.tuner.frequency.FrequencyCorrectionControl;
import alias.AliasList;
import controller.channel.Channel.ChannelType;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.fir.real.RealFIRFilter_RB_RB;

public class P25_C4FMDecoder extends P25Decoder implements IRealBufferListener
{
	private final static Logger mLog = LoggerFactory.getLogger( P25_C4FMDecoder.class );
	
    /* Instrumentation Taps */
	private static final String INSTRUMENT_FILTER_OUTPUT = "Tap Point: Pre-Filter Output";
	private static final String INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT = "Tap Point: Symbol Filter Output";
	private static final String INSTRUMENT_C4FM_SLICER_OUTPUT = "Tap Point: C4FM Slicer Output";

	private final static int MAXIMUM_FREQUENCY_CORRECTION = 3000; //Hertz, +/-

	private List<Tap> mAvailableTaps;
	private RealFIRFilter_RB_RB mC4FMPreFilter;
	private FrequencyCorrectionControl mFrequencyCorrectionControl;
	private C4FMSymbolFilter mSymbolFilter;
	private C4FMSlicer mC4FMSlicer;
	private P25MessageFramer mMessageFramer;
	
	/**
	 * P25 Phase 1 C4FM Decoder processes real buffers of un-filtered, 
	 * demodulated audio and produces decoded P25 Phase 1 Messages. 
	 * 
	 * Provides Frequency Control to steer external tuner source and incorporates
	 * internal gain control.
	 * 
	 * Note: use the P25AudioModule to convert the decoded messages into audio.
	 * 
	 * @param aliasList - optional (can be null) list of alias values for network 
	 * infrastructure and subscriber identities that will be included in each
	 * decoded message
	 */
	public P25_C4FMDecoder( AliasList aliasList, ChannelType channelType )
	{
		super( aliasList, channelType );
		
		/* Filter demodulated sample buffers */
		mC4FMPreFilter = new RealFIRFilter_RB_RB( FilterFactory.getLowPass( 
				48000, 6750, 7500, 60, WindowType.HANNING, true ), 1.0f );

		/* Issue tuned frequency correction commands, remotely controlled by the 
		 * downstream symbol filter */
		mFrequencyCorrectionControl = new FrequencyCorrectionControl( 
				MAXIMUM_FREQUENCY_CORRECTION );
		
		/* Shape gain and frequency offsets to optimize sample stream */
		mSymbolFilter = new C4FMSymbolFilter( mFrequencyCorrectionControl );
		mC4FMPreFilter.setListener( mSymbolFilter );
		
		/* Convert samples to symbols */
		mC4FMSlicer = new C4FMSlicer();
		mSymbolFilter.setListener( mC4FMSlicer );

		/* Sync pattern detection and message construction */
		mMessageFramer = new P25MessageFramer( aliasList );
        mC4FMSlicer.addListener( mMessageFramer );
        
        /* Process and broadcast messages */
        mMessageFramer.setListener( getMessageProcessor() );
	}

	@Override
	public void dispose()
	{
		super.dispose();
		
		mC4FMPreFilter.dispose();
		mC4FMPreFilter = null;
		
		mFrequencyCorrectionControl.dispose();
		mFrequencyCorrectionControl = null;
		
		mSymbolFilter.dispose();
		mSymbolFilter = null;
		
		mMessageFramer.dispose();
		mMessageFramer = null;

		mC4FMSlicer.dispose();
		mC4FMSlicer = null;
	}
	
	public Modulation getModulation()
	{
		return Modulation.C4FM;
	}
	
	@Override
	public boolean hasFrequencyCorrectionControl()
	{
		return mFrequencyCorrectionControl != null;
	}

	@Override
	public FrequencyCorrectionControl getFrequencyCorrectionControl()
	{
		return mFrequencyCorrectionControl;
	}

	@Override
	public Listener<RealBuffer> getRealBufferListener()
	{
		return mC4FMPreFilter;
	}
	
	/**
	 * Instrumentation taps for monitoring internal processing
	 */
	@Override
    public List<Tap> getTaps()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<Tap>();

			mAvailableTaps.add( new FloatTap( INSTRUMENT_FILTER_OUTPUT, 0, 1.0f ) );
			
			if( mSymbolFilter != null )
			{
				mAvailableTaps.addAll( mSymbolFilter.getTaps() );
			}
			
			mAvailableTaps.add( new FloatTap( INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT, 0, 0.1f ) );
			mAvailableTaps.add( new DibitTap( INSTRUMENT_C4FM_SLICER_OUTPUT, 0, 0.1f ) );
		}
		
		return mAvailableTaps;
    }

	/**
	 * Adds an instrumentation tap to monitor internal processing.  
	 */
	@Override
    public void addTap( Tap tap )
    {
		if( mSymbolFilter != null )
		{
			mSymbolFilter.addTap( tap );
		}
		
		switch( tap.getName() )
		{
			case INSTRUMENT_FILTER_OUTPUT:
				FloatBufferTap filterOutput = (FloatBufferTap)tap;
				mC4FMPreFilter.setListener( filterOutput );
				filterOutput.setListener( mSymbolFilter );
				break;
			case INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT:
				FloatTap symbolTap = (FloatTap)tap;
				mSymbolFilter.setListener( symbolTap );
				symbolTap.setListener( mC4FMSlicer );
				break;
			case INSTRUMENT_C4FM_SLICER_OUTPUT:
				DibitTap slicerTap = (DibitTap)tap;
				
				if( mC4FMSlicer != null )
				{
					mC4FMSlicer.addListener( slicerTap );
				}
				break;
			default:
				throw new IllegalArgumentException( "Unrecognized tap: " + 
							tap.getName() );
		}
    }

	/**
	 * Removes the instrumentation tap.
	 */
	@Override
    public void removeTap( Tap tap )
    {
		if( mSymbolFilter != null )
		{
			mSymbolFilter.removeTap( tap );
		}
		
		switch( tap.getName() )
		{
			case INSTRUMENT_FILTER_OUTPUT:
				mC4FMPreFilter.setListener( mSymbolFilter );
				break;
			case INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT:
				mSymbolFilter.setListener( mC4FMSlicer );
				break;
			case INSTRUMENT_C4FM_SLICER_OUTPUT:
				DibitTap slicerTap = (DibitTap)tap;
				
				if( mC4FMSlicer != null )
				{
					mC4FMSlicer.removeListener( slicerTap );
				}
				break;
			default:
				throw new IllegalArgumentException( "Unrecognized tap: " + 
							tap.getName() );
		}
    }
}

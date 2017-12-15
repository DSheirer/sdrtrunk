/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package ua.in.smartjava.module.decode.p25;

import ua.in.smartjava.instrument.tap.Tap;
import ua.in.smartjava.instrument.tap.TapGroup;
import ua.in.smartjava.instrument.tap.stream.DibitTap;
import ua.in.smartjava.instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.real.IFilteredRealBufferListener;
import ua.in.smartjava.sample.real.RealBuffer;
import ua.in.smartjava.source.tuner.frequency.FrequencyChangeEvent;

public class P25_C4FMDecoder extends P25Decoder implements IFilteredRealBufferListener
{
	private final static Logger mLog = LoggerFactory.getLogger( P25_C4FMDecoder.class );
	
    /* Instrumentation Taps */
	private static final String INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT = "Tap Point: Symbol Filter Output";
	private static final String INSTRUMENT_C4FM_SLICER_OUTPUT = "Tap Point: C4FM Slicer Output";

	private List<TapGroup> mAvailableTaps;
	private C4FMSymbolFilter mSymbolFilter;
	private C4FMSlicer mC4FMSlicer;
	private P25MessageFramer mMessageFramer;
	
	/**
	 * P25 Phase 1 C4FM Decoder processes real buffers of un-filtered, 
	 * demodulated ua.in.smartjava.audio and produces decoded P25 Phase 1 Messages.
	 * 
	 * Provides Frequency Control to steer external tuner ua.in.smartjava.source and incorporates
	 * internal gain control.
	 * 
	 * Note: use the P25AudioModule to convert the decoded messages into ua.in.smartjava.audio.
	 * 
	 * @param aliasList - optional (can be null) list of ua.in.smartjava.alias values for network
	 * infrastructure and subscriber identities that will be included in each
	 * decoded ua.in.smartjava.message
	 */
	public P25_C4FMDecoder( AliasList aliasList, int frequencyCorrectionMaximum )
	{
		super( aliasList );
		
		/* Shape gain and frequency offsets to optimize ua.in.smartjava.sample stream */
		mSymbolFilter = new C4FMSymbolFilter( frequencyCorrectionMaximum );

		/* Convert samples to symbols */
		mC4FMSlicer = new C4FMSlicer();
		mSymbolFilter.setListener( mC4FMSlicer );

		/* Sync pattern detection and ua.in.smartjava.message construction */
		mMessageFramer = new P25MessageFramer( aliasList );
        mC4FMSlicer.addListener( mMessageFramer );
        
        /* Process and broadcast messages */
        mMessageFramer.setListener( getMessageProcessor() );
	}

	@Override
	public void dispose()
	{
		super.dispose();
		
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
	public Listener<RealBuffer> getFilteredRealBufferListener()
	{
		return mSymbolFilter;
	}
	
	/**
	 * Instrumentation taps for monitoring internal processing
	 */
	@Override
    public List<TapGroup> getTapGroups()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<>();

			TapGroup group = new TapGroup( "P25 C4FM Decoder" );
			
			group.add( new FloatTap( INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT, 0, 0.1f ) );
			group.add( new DibitTap( INSTRUMENT_C4FM_SLICER_OUTPUT, 0, 0.1f ) );

			mAvailableTaps.add( group );
			
			if( mSymbolFilter != null )
			{
				mAvailableTaps.addAll( mSymbolFilter.getTapGroups() );
			}
		}
		
		return mAvailableTaps;
    }

	/**
	 * Adds an instrumentation tap to monitor internal processing.  
	 */
	@Override
    public void registerTap( Tap tap )
    {
		if( mSymbolFilter != null )
		{
			mSymbolFilter.registerTap( tap );
		}
		
		switch( tap.getName() )
		{
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
				break;
		}
    }

	/**
	 * Removes the instrumentation tap.
	 */
	@Override
    public void unregisterTap( Tap tap )
    {
		if( mSymbolFilter != null )
		{
			mSymbolFilter.unregisterTap( tap );
		}
		
		switch( tap.getName() )
		{
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

	@Override
	public void reset()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start( ScheduledExecutorService executor )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFrequencyChangeListener(	Listener<FrequencyChangeEvent> listener )
	{
		if( mSymbolFilter != null )
		{
			mSymbolFilter.setFrequencyChangeListener( listener );
		}
	}

	@Override
	public void removeFrequencyChangeListener()
	{
		if( mSymbolFilter != null )
		{
			mSymbolFilter.setFrequencyChangeListener( null );
		}
	}

	@Override
	public Listener<FrequencyChangeEvent> getFrequencyChangeListener()
	{
		if( mSymbolFilter != null )
		{
			return mSymbolFilter.getFrequencyChangeListener();
		}
		
		return null;
	}
}

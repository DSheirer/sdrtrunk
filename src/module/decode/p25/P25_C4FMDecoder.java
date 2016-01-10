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
import instrument.tap.TapGroup;
import instrument.tap.stream.DibitTap;
import instrument.tap.stream.FloatBufferTap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.real.IFilteredRealBufferListener;
import sample.real.RealBuffer;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.IFrequencyChangeListener;
import source.tuner.frequency.IFrequencyChangeProvider;
import alias.AliasList;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.fir.real.RealFIRFilter_RB_RB;

public class P25_C4FMDecoder extends P25Decoder 
	implements IFrequencyChangeListener, IFrequencyChangeProvider,IFilteredRealBufferListener
{
	private final static Logger mLog = LoggerFactory.getLogger( P25_C4FMDecoder.class );
	
    /* Instrumentation Taps */
	private static final String INSTRUMENT_FILTER_OUTPUT = "Tap Point: Pre-Filter Output";
	private static final String INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT = "Tap Point: Symbol Filter Output";
	private static final String INSTRUMENT_C4FM_SLICER_OUTPUT = "Tap Point: C4FM Slicer Output";

	private List<TapGroup> mAvailableTaps;
	private RealFIRFilter_RB_RB mC4FMPreFilter;
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
	public P25_C4FMDecoder( AliasList aliasList, int frequencyCorrectionMaximum )
	{
		super( aliasList );
		
		/* Filter demodulated sample buffers */
		float[] filter = FilterFactory.getLowPass( 48000, 2500, 4000, 80, WindowType.HANNING, true );

//		mLog.debug( "Demod Filter tap count:" + filter.length + " coefficients:" + Arrays.toString( filter ) );
		mC4FMPreFilter = new RealFIRFilter_RB_RB( filter, 1.0f );

		/* Shape gain and frequency offsets to optimize sample stream */
		mSymbolFilter = new C4FMSymbolFilter( frequencyCorrectionMaximum );
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
		return mC4FMPreFilter;
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
			
			group.add( new FloatBufferTap( INSTRUMENT_FILTER_OUTPUT, 0, 1.0f ) );
			
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

	@Override
	public void reset()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start()
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

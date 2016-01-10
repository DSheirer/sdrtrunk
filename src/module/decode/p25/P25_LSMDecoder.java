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
import instrument.tap.stream.ComplexSampleTap;
import instrument.tap.stream.ComplexTap;
import instrument.tap.stream.DibitTap;
import instrument.tap.stream.QPSKTap;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.ComplexBufferToStreamConverter;
import sample.complex.IComplexBufferListener;
import alias.AliasList;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import dsp.gain.ComplexFeedForwardGainControl;
import dsp.psk.LSMDemodulator;
import dsp.psk.QPSKPolarSlicer;

public class P25_LSMDecoder extends P25Decoder implements IComplexBufferListener
{
	private final static Logger mLog = LoggerFactory.getLogger( P25_LSMDecoder.class );
	
    /* Instrumentation Taps */
	private static final String INSTRUMENT_BASEBAND_FILTER_OUTPUT = "Tap Point: Baseband Filter Output";
	private static final String INSTRUMENT_AGC_OUTPUT = "Tap Point: AGC Output";
	private static final String INSTRUMENT_QPSK_DEMODULATOR_OUTPUT = "Tap Point: CQPSK Demodulator Output";
	private static final String INSTRUMENT_CQPSK_SLICER_OUTPUT = "Tap Point: CQPSK Slicer Output";

	private List<TapGroup> mAvailableTaps;
	
	private ComplexFIRFilter_CB_CB mBasebandFilter;
	private ComplexBufferToStreamConverter mStreamConverter = 
							new ComplexBufferToStreamConverter();
	private ComplexFeedForwardGainControl mAGC = 
							new ComplexFeedForwardGainControl( 32 );
	private LSMDemodulator mCQPSKDemodulator = new LSMDemodulator();
	private QPSKPolarSlicer mCQPSKSlicer = new QPSKPolarSlicer();
	private P25MessageFramer mMessageFramer;
	
	public P25_LSMDecoder( AliasList aliasList )
	{
		super( aliasList );
		
		mBasebandFilter = new ComplexFIRFilter_CB_CB( FilterFactory.getLowPass( 
				48000, 7250, 8000, 60, WindowType.HANNING, true ), 1.0f );
		
		mBasebandFilter.setListener( mStreamConverter );
		
		mStreamConverter.setListener( mAGC );

		mAGC.setListener( mCQPSKDemodulator );
		
		mCQPSKDemodulator.setSymbolListener( mCQPSKSlicer );
		
		mMessageFramer = new P25MessageFramer( aliasList, mCQPSKDemodulator );
		mCQPSKSlicer.addListener( mMessageFramer );
		
        mMessageFramer.setListener( getMessageProcessor() );
	}
	
	public void dispose()
	{
		super.dispose();
		
		mBasebandFilter.dispose();
		mBasebandFilter = null;
		
		mStreamConverter.dispose();
		mStreamConverter = null;
		
		mAGC.dispose();
		mAGC = null;

		mCQPSKDemodulator.dispose();
		mCQPSKDemodulator = null;
		
		mCQPSKSlicer.dispose();
		mCQPSKSlicer = null;
		
		mMessageFramer.dispose();
		mMessageFramer = null;
	}
	
	@Override
	public Listener<ComplexBuffer> getComplexBufferListener()
	{
		return mBasebandFilter;
	}

	public Modulation getModulation()
	{
		return Modulation.CQPSK;
	}
	
	/**
	 * Provides a list of instrumentation taps for monitoring internal processing
	 */
	@Override
    public List<TapGroup> getTapGroups()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<>();

			TapGroup group = new TapGroup( "P25 LSM Decoder" );
			
			group.add( new ComplexTap( INSTRUMENT_BASEBAND_FILTER_OUTPUT, 0, 1.0f ) );
			group.add( new ComplexTap( INSTRUMENT_AGC_OUTPUT, 0, 1.0f ) );
			group.add( new QPSKTap( INSTRUMENT_QPSK_DEMODULATOR_OUTPUT, 0, 1.0f ) );
			group.add( new DibitTap( INSTRUMENT_CQPSK_SLICER_OUTPUT, 0, 0.1f ) );

			mAvailableTaps.add( group );
			
			if( mCQPSKDemodulator != null )
			{
				mAvailableTaps.addAll( mCQPSKDemodulator.getTapGroups() );
			}
		}
		
		return mAvailableTaps;
    }

	/**
	 * Adds the instrumentation tap
	 */
	@Override
    public void registerTap( Tap tap )
    {
		if( mCQPSKDemodulator != null )
		{
			mCQPSKDemodulator.registerTap( tap );
		}
		
		switch( tap.getName() )
		{
			case INSTRUMENT_BASEBAND_FILTER_OUTPUT:
				ComplexSampleTap baseband = (ComplexSampleTap)tap;
				mStreamConverter.setListener( baseband );
				baseband.setListener( mAGC );
				break;
			case INSTRUMENT_AGC_OUTPUT:
				ComplexSampleTap agcSymbol = (ComplexSampleTap)tap;
				mAGC.setListener( agcSymbol );
				agcSymbol.setListener( mCQPSKDemodulator );
				break;
			case INSTRUMENT_QPSK_DEMODULATOR_OUTPUT:
				QPSKTap qpsk = (QPSKTap)tap;
				mCQPSKDemodulator.setSymbolListener( qpsk );
				qpsk.setListener( mCQPSKSlicer );
				break;
			case INSTRUMENT_CQPSK_SLICER_OUTPUT:
				mCQPSKSlicer.addListener( (DibitTap)tap );
				break;
			default:
				throw new IllegalArgumentException( "Unrecognized tap: " + 
							tap.getName() );
		}
    }

	/**
	 * Removes the instrumentation tap
	 */
	@Override
    public void unregisterTap( Tap tap )
    {
		if( mCQPSKDemodulator != null )
		{
			mCQPSKDemodulator.unregisterTap( tap );
		}
		
		switch( tap.getName() )
		{
			case INSTRUMENT_BASEBAND_FILTER_OUTPUT:
				mStreamConverter.setListener( mAGC );
				break;
			case INSTRUMENT_AGC_OUTPUT:
				mAGC.setListener( mCQPSKDemodulator );
				break;
			case INSTRUMENT_QPSK_DEMODULATOR_OUTPUT:
				mCQPSKDemodulator.setSymbolListener( mCQPSKSlicer );
				break;
			case INSTRUMENT_CQPSK_SLICER_OUTPUT:
				mCQPSKSlicer.removeListener( (DibitTap)tap );
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
}

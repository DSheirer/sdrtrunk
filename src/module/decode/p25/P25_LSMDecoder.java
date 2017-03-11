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
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasList;
import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.ComplexBufferToStreamConverter;
import sample.complex.IComplexBufferListener;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import dsp.gain.ComplexFeedForwardGainControl;
import dsp.psk.LSMDemodulator;
import dsp.psk.QPSKPolarSlicer;
import source.tuner.frequency.FrequencyChangeEvent;

public class P25_LSMDecoder extends P25Decoder implements IComplexBufferListener
{
	private final static Logger mLog = LoggerFactory.getLogger( P25_LSMDecoder.class );
	
    /* Instrumentation Taps */
	private static final String INSTRUMENT_BASEBAND_FILTER_OUTPUT = "Tap Point: Baseband Filter Output";
	private static final String INSTRUMENT_AGC_OUTPUT = "Tap Point: AGC Output";
	private static final String INSTRUMENT_LSM_DEMODULATOR_OUTPUT = "Tap Point: LSM Demodulator Output";
	private static final String INSTRUMENT_QPSK_SLICER_OUTPUT = "Tap Point: QPSK Slicer Output";

	private List<TapGroup> mAvailableTaps;
	
	private ComplexFIRFilter_CB_CB mBasebandFilter;
	private ComplexBufferToStreamConverter mStreamConverter = new ComplexBufferToStreamConverter();
	private ComplexFeedForwardGainControl mAGC = 
							new ComplexFeedForwardGainControl( 32 );
	private LSMDemodulator mLSMDemodulator = new LSMDemodulator();
	private QPSKPolarSlicer mQPSKSlicer = new QPSKPolarSlicer();
	private P25MessageFramer mMessageFramer;
	
	public P25_LSMDecoder( AliasList aliasList )
	{
		super( aliasList );
		
		mBasebandFilter = new ComplexFIRFilter_CB_CB( FilterFactory.getLowPass( 
				48000, 7250, 8000, 60, WindowType.HANNING, true ), 1.0f );
		
		mBasebandFilter.setListener( mStreamConverter );
		
		mStreamConverter.setListener( mAGC );

		mAGC.setListener( mLSMDemodulator );
		
		mLSMDemodulator.setSymbolListener( mQPSKSlicer );
		
		mMessageFramer = new P25MessageFramer( aliasList, mLSMDemodulator );
		mQPSKSlicer.addListener( mMessageFramer );
		
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

		mLSMDemodulator.dispose();
		mLSMDemodulator = null;
		
		mQPSKSlicer.dispose();
		mQPSKSlicer = null;
		
		mMessageFramer.dispose();
		mMessageFramer = null;
	}

    @Override
    public void setFrequencyChangeListener(Listener<FrequencyChangeEvent> listener)
    {
    }

    @Override
    public void removeFrequencyChangeListener()
    {
    }

    @Override
    public Listener<FrequencyChangeEvent> getFrequencyChangeListener()
    {
        return new Listener<FrequencyChangeEvent>()
        {
            @Override
            public void receive(FrequencyChangeEvent frequencyChangeEvent)
            {
                //Ignored
            }
        };
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
			group.add( new QPSKTap( INSTRUMENT_LSM_DEMODULATOR_OUTPUT, 0, 1.0f ) );
			group.add( new DibitTap( INSTRUMENT_QPSK_SLICER_OUTPUT, 0, 0.1f ) );

			mAvailableTaps.add( group );
			
			if( mLSMDemodulator != null )
			{
				mAvailableTaps.addAll( mLSMDemodulator.getTapGroups() );
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
		if( mLSMDemodulator != null )
		{
			mLSMDemodulator.registerTap( tap );
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
				agcSymbol.setListener( mLSMDemodulator );
				break;
			case INSTRUMENT_LSM_DEMODULATOR_OUTPUT:
				QPSKTap qpsk = (QPSKTap)tap;
				mLSMDemodulator.setSymbolListener( qpsk );
				qpsk.setListener( mQPSKSlicer );
				break;
			case INSTRUMENT_QPSK_SLICER_OUTPUT:
				mQPSKSlicer.addListener( (DibitTap)tap );
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
		if( mLSMDemodulator != null )
		{
			mLSMDemodulator.unregisterTap( tap );
		}
		
		switch( tap.getName() )
		{
			case INSTRUMENT_BASEBAND_FILTER_OUTPUT:
				mStreamConverter.setListener( mAGC );
				break;
			case INSTRUMENT_AGC_OUTPUT:
				mAGC.setListener( mLSMDemodulator );
				break;
			case INSTRUMENT_LSM_DEMODULATOR_OUTPUT:
				mLSMDemodulator.setSymbolListener( mQPSKSlicer );
				break;
			case INSTRUMENT_QPSK_SLICER_OUTPUT:
				mQPSKSlicer.removeListener( (DibitTap)tap );
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
}

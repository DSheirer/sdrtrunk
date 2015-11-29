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
package module.demodulate.fm;

import module.Module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.IComplexBufferListener;
import sample.real.IRealBufferProvider;
import sample.real.RealBuffer;
import source.tuner.frequency.AutomaticFrequencyControl_RB;
import source.tuner.frequency.FrequencyCorrectionControl;
import source.tuner.frequency.IFrequencyCorrectionController;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.dc.DCRemovalFilter_RB;
import dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import dsp.fm.FMDemodulator_CB;

public class FMDemodulatorModule extends Module implements IComplexBufferListener, 
				IFrequencyCorrectionController,	IRealBufferProvider
{
	private final static Logger mLog = LoggerFactory.getLogger( FMDemodulatorModule.class );
	
	/* Determines responsiveness of DC filter to frequency changes */
	private static final float DC_REMOVAL_RATIO = 0.000003f;

	private static final int SAMPLE_RATE = 48000;
	
	private ComplexFIRFilter_CB_CB mIQFilter;
	private FMDemodulator_CB mDemodulator;
	private Broadcaster<RealBuffer> mUnfilteredBufferBroadcaster = new Broadcaster<RealBuffer>();
	private DCRemovalFilter_RB mDCRemovalFilter;
	private AutomaticFrequencyControl_RB mAutomaticFrequencyControl;
	private boolean mRemoveDC = true;

	private Listener<RealBuffer> mListener;
	
	/**
	 * FM Demodulator with integrated DC removal and automatic frequency
	 * correction control.
	 * 
	 * @param pass - pass frequency for IQ filtering prior to demodulation.  This
	 * frequency should be half of the signal bandwidth since the filter will
	 * be applied against each of the inphase and quadrature signals and the 
	 * combined pass bandwidth will be twice this value.
	 * 
	 * @param stop - stop frequency for IQ filtering prior to demodulation.
	 * 
	 * @param maxFrequencyCorrection - defines the maximum frequency +/- that 
	 * the correction controller can adjust from the center tuned frequency.
	 * Set this value to 0 for no frequency correction control.
	 */
	public FMDemodulatorModule( int pass, int stop, int maxFrequencyCorrection )
	{
		this( pass, stop, maxFrequencyCorrection, true );
	}

	public FMDemodulatorModule( int pass, int stop, int maxFrequencyCorrection, boolean removeDC )
	{
		assert( stop > pass );

		mRemoveDC = removeDC;
		
		mIQFilter = new ComplexFIRFilter_CB_CB( FilterFactory.getLowPass( 
				SAMPLE_RATE, pass, stop, 60, WindowType.HAMMING, true ), 1.0f );
		
		mDemodulator = new FMDemodulator_CB( 1.0f );
		mIQFilter.setListener( mDemodulator );

		mUnfilteredBufferBroadcaster = new Broadcaster<RealBuffer>();			
		mDemodulator.setListener( mUnfilteredBufferBroadcaster );

		if( maxFrequencyCorrection > 0 )
		{
			/* Frequency correction requires unfiltered samples in order to detect
			 * the DC component present in a mistuned FM demodulated sample stream */
			mAutomaticFrequencyControl = new AutomaticFrequencyControl_RB( maxFrequencyCorrection );
			mUnfilteredBufferBroadcaster.addListener( mAutomaticFrequencyControl );
		}
		
		if( mRemoveDC )
		{
			mDCRemovalFilter = new DCRemovalFilter_RB( DC_REMOVAL_RATIO );
			mUnfilteredBufferBroadcaster.addListener( mDCRemovalFilter );
		}
	}

	@Override
	public boolean hasFrequencyCorrectionControl()
	{
		return mAutomaticFrequencyControl != null;
	}

	@Override
	public FrequencyCorrectionControl getFrequencyCorrectionControl()
	{
		return mAutomaticFrequencyControl;
	}

	@Override
	public Listener<ComplexBuffer> getComplexBufferListener()
	{
		return mIQFilter;
	}

	@Override
	public void dispose()
	{
		mIQFilter.dispose();
		mIQFilter = null;
		
		mDemodulator.dispose();
		mDemodulator = null;
		
		if( mDCRemovalFilter != null )
		{
			mDCRemovalFilter.dispose();
		}
		mDCRemovalFilter = null;
		
		if( mAutomaticFrequencyControl != null )
		{
			mAutomaticFrequencyControl.dispose();
			mAutomaticFrequencyControl = null;
		}
		
		if( mUnfilteredBufferBroadcaster != null )
		{
			mUnfilteredBufferBroadcaster.dispose();
			mUnfilteredBufferBroadcaster = null;
		}
	}

	@Override
	public void reset()
	{
		mDemodulator.reset();
		
		if( mDCRemovalFilter != null )
		{
			mDCRemovalFilter.reset();
		}
		
		if( mAutomaticFrequencyControl != null )
		{
			mAutomaticFrequencyControl.reset();
		}
	}

	@Override
	public void setRealBufferListener( Listener<RealBuffer> listener )
	{
		mListener = listener;
		
		if( mRemoveDC )
		{
			mDCRemovalFilter.setListener( listener );
		}
		else
		{
			mUnfilteredBufferBroadcaster.addListener( listener );
		}
	}

	@Override
	public void removeRealBufferListener()
	{
		if( mRemoveDC )
		{
			mDCRemovalFilter.removeListener();
		}
		else
		{
			mUnfilteredBufferBroadcaster.removeListener( mListener );
		}
	}

	@Override
	public void start()
	{
	}

	@Override
	public void stop()
	{
	}
}

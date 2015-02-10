package dsp.psk;

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.Provider;
import sample.complex.ComplexSample;
import dsp.filter.interpolator.QPSKInterpolator;

public class CQPSKDemodulator implements Listener<ComplexSample>, 
										 Provider<ComplexSample>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( CQPSKDemodulator.class );

	/* Sample Rate 48000 / Symbol Rate 4800 = Samples Per Symbol 10 */
	public static final int SAMPLES_PER_SYMBOL = 10;
	public static final int d_twice_sps = 20;
	public static final int half_sps = 5;
	
	public static final double TWO_PI = 2.0 * Math.PI;
	
	/* CQPSK constellation is offset from normal QPSK by 45 degrees or pi/4 */
	public static final float THETA = (float)( Math.PI / 4.0d ); 
	
	private float d_mu = 10.5f;
	private float d_gain_mu = 0.05f;

	/* samples per symbol */
	private float d_omega = 10.0f;
	private float d_half_omega = 5.0f;
	private float d_gain_omega = 0.1f * d_gain_mu * d_gain_mu;
	private float d_omega_rel = 0.005f;
	private float d_min_omega = d_omega * ( 1.0f - d_omega_rel );
	private float d_max_omega = d_omega * ( 1.0f + d_omega_rel );
	private float d_omega_mid = 10.0f;
	private float d_phase = 0.0f;
	private float d_freq = 0.0f;

	private ComplexSample[] d_dl = new ComplexSample[ 40 ];
	private int d_dl_index = 0;

	private Listener<ComplexSample> mListener;
	private ComplexSample[] mConstellationPoints;
	
	private SymbolTracker mSymbolTracker = new SymbolTracker();
	private QPSKPhaseErrorTracker mPhaseTracker = new QPSKPhaseErrorTracker();

	private QPSKInterpolator mInterpolator = new QPSKInterpolator();
	
	private ComplexSample d_last_sample = new ComplexSample( 0.0f, 0.0f );
	
	public CQPSKDemodulator()
	{
		createConstellationPoints();
		
		/* prefill the delay line */
		for( int x = 0; x < 40; x++ )
		{
			d_dl[ x ] = new ComplexSample( 0.0f, 0.0f );
		}
		
	}
	
	private void createConstellationPoints()
	{
		mConstellationPoints = new ComplexSample[ 4 ];
		
		for( int x = 0; x < 4; x++ )
		{
			mConstellationPoints[ x ] = ComplexSample.fromAngle( 
					( (float)( TWO_PI / 4.0f ) * (float)x ) );
		}
	}
	
	@Override
	public void receive( ComplexSample sample )
	{
		/* skip a number of symbols between sampling */
		d_mu--;

		/* increment phase based on the frequency of the rotation */
		d_phase += d_freq;

		/* keep phase clamped and not walk to infinity */
		unwrapPhase();

		/* NCO value for derotating the current sample */
		ComplexSample nco = ComplexSample.fromAngle( d_phase + THETA );
		
		/* Mixed/downconverted symbol */
		ComplexSample symbol = ComplexSample.multiply( nco, sample );

		/* Fill up the delay line for the interpolator */
		d_dl[ d_dl_index ] = symbol;
		d_dl[ d_dl_index + d_twice_sps ] = symbol;
		
		/* Keep the delay line index in bounds */
		d_dl_index = ( d_dl_index + 1 ) % d_twice_sps;
		
		if( d_mu <= 1.0f )
		{
			ComplexSample interp_samp_mid = mInterpolator.filter( d_dl, d_dl_index, d_mu );
			
			ComplexSample interp_samp = mInterpolator.filter( d_dl, d_dl_index + half_sps, d_mu );
			
			float error_real = ( d_last_sample.real() - interp_samp.real() ) * interp_samp_mid.real();
			float error_imag = ( d_last_sample.imaginary() - interp_samp.imaginary() ) * interp_samp_mid.imaginary();
			
			ComplexSample diffdec = ComplexSample.multiply( interp_samp, d_last_sample.conjugate() );

			d_last_sample = interp_samp;
			
			float symbol_error = normalize( error_real + error_imag ); //Garner loop error
			
			d_omega = d_omega + d_gain_omega * symbol_error * interp_samp.magnitude();
			d_omega = d_omega_mid + clip( d_omega_mid, d_omega_rel );

			d_mu += d_omega + d_gain_mu * symbol_error;
			
//			mSymbolTracker.receive( interpolated );
			
			mPhaseTracker.receive( diffdec );
			
			if( mListener != null )
			{
//				mListener.receive( interp_samp );
				mListener.receive( diffdec );
			}
		}
	}
	
	private float normalize( float symbol_error )
	{
		if( Float.isNaN( symbol_error ) )
		{
			return 0.0f;
		}
		
		if( symbol_error < -1.0 )
		{
			return -1.0f;
		}
		
		if( symbol_error > 1.0 )
		{
			return 1.0f;
		}
		
		return symbol_error;
	}
	
	/**
	 * Constrains value to the range of ( -maximum <> maximum )
	 */
	private float clip( float value, float maximum )
	{
		if( value > maximum )
		{
			return maximum;
		}
		else if( value < -maximum )
		{
			return -maximum;
		}
		
		return value;
	}
	
	public static int getQPSKConstellationPoint( ComplexSample sample )
  	{
		if( sample.real() >= 0.0f )
		{
			if( sample.imaginary() >= 0.0f )
			{
				return 0;
			}
			else
			{
				return 2;
			}
		}
		else
		{
			if( sample.imaginary() >= 0.0f )
			{
				return 1;
			}
			else
			{
				return 3;
			}
		}
	}
	
	private void unwrapPhase()
	{
		while( d_phase > TWO_PI )
		{
			d_phase -= TWO_PI;
		}
		
		while( d_phase < -TWO_PI )
		{
			d_phase += TWO_PI;
		}
	}
	
	

	/**
	 * Mueller & Mueller symbol timing tracker
	 */
	public class SymbolTracker implements Listener<ComplexSample>
	{
		ComplexSample mPoint2T = new ComplexSample();
		ComplexSample mPoint1T = new ComplexSample();
		ComplexSample mPoint0T = new ComplexSample();
		
		ComplexSample mConstellation2T = new ComplexSample();
		ComplexSample mConstellation1T = new ComplexSample();
		ComplexSample mConstellation0T = new ComplexSample();
		
		
		@Override
		public void receive( ComplexSample sample )
		{
			mPoint2T = mPoint1T;
			mPoint1T = mPoint0T;
			mPoint0T = sample;
			
			mConstellation2T = mConstellation1T;
			mConstellation1T = mConstellation0T;
			
			int currentConstellation = getQPSKConstellationPoint( mPoint0T );
			mConstellation0T = mConstellationPoints[ currentConstellation ];

			ComplexSample x = ComplexSample.multiply( 
				ComplexSample.subtract( mConstellation0T, mConstellation2T ), 
					mPoint1T.conjugate() );

			ComplexSample y = ComplexSample.multiply( 
					ComplexSample.subtract( mPoint0T, mPoint2T ), 
						mConstellation1T.conjugate() );
			
			ComplexSample u = ComplexSample.subtract( y, x );

			/* Real part of u contains error -- constrain to -1.0 to 1.0 */
			float mm_error = clip( u.real(), 1.0f );

			/* Update omega (samples per symbol) based on loop error */
			d_omega = d_omega + d_gain_omega * mm_error;
			
			/* Make sure we don't walk away */
			d_omega = d_omega_mid + clip( d_omega - d_omega_mid, d_omega_rel );
			
			/* update mu based on loop error */
			d_mu += d_omega + d_gain_mu * mm_error;
		}
	}
	
	/**
	 * Implements a control loop for QPSK
	 * 
	 * Ported to java from gnuradio/control_loop and gnuradio/mpsk_receiver_cc
	 * using initialization values from OP25/cqpsk.py
	 */
	public class QPSKPhaseErrorTracker implements Listener<ComplexSample>
	{
		private float d_loop_bw = 0.03f;
		private float d_damping = (float)Math.sqrt( 2.0 ) / 2.0f;
		private float d_alpha = 0.03f;
		private float d_beta = 0.125f * d_alpha * d_alpha;
		private float d_max_freq = ( 2.0f * (float)Math.PI * 1200.0f ) / 48000.0f;
		
		public QPSKPhaseErrorTracker()
		{
		}
		
		@Override
		public void receive( ComplexSample sample )
		{
			float phase_error = -getPhaseError( sample );

			advance_loop(phase_error, sample );
			
			unwrapPhase();
			
			frequency_limit();
		}
		
		private void advance_loop( float phase_error, ComplexSample sample )
		{
			float abs = sample.magnitude();
			
			d_freq += d_beta * phase_error * abs;
			
			d_phase += d_freq + d_alpha * phase_error * abs;
		}
		
		private void frequency_limit()
		{
			if( d_freq > d_max_freq )
			{
				d_freq = d_max_freq;
			}
			
			if( d_freq < -d_max_freq )
			{
				d_freq = -d_max_freq;
			}
		}

		/**
		 * Calculates the phase error for a qpsk sample.  Standard qpsk samples
		 * should be aligned along the x/y axis and any deviation from either
		 * axis represents an error.  Whichever component, real or imaginary,
		 * that has the lesser absolute value, represents the error, since that
		 * component should be zero valued.  
		 * 
		 * For example, constellation point 0 is zero degrees and is represented
		 * by a sample that contains a real = +1 and an imaginary = 0 component.
		 * If we sample too early or too late, that will cause a +/- phase 
		 * deviation in the resultant sample and the sample will reflect that
		 * error value in the imaginary component. That deviation is the error.
		 */
		public float getPhaseError( ComplexSample sample )
		{
			if( sample.realAbsolute() > sample.imaginaryAbsolute() )
			{
				if( sample.real() > 0 )
				{
					return -sample.imaginary();
				}
				else
				{
					return sample.imaginary();
				}
			}
			else
			{
				if( sample.imaginary() > 0 )
				{
					return sample.real();
				}
				else
				{
					return -sample.real();
				}
			}
		}
	}

	@Override
	public void setListener( Listener<ComplexSample> listener )
	{
		mListener = listener;
	}

	@Override
	public void removeListener( Listener<ComplexSample> listener )
	{
		mListener = null;
	}
}

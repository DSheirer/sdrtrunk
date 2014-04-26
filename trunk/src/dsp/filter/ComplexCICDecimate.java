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
package dsp.filter;

import sample.Listener;
import sample.Provider;
import sample.complex.ComplexSample;
import dsp.filter.Window.WindowType;

/**
 * Cascaded Integrator Comb (CIC) filter - anti-aliasing decimating filter for 
 * reducing a high rate complex sample stream to a lower rate by an integral 
 * decimation (ie reduction) value.
 * 
 * Note: comb delay is fixed at 1 for this filter.
 */
public class ComplexCICDecimate extends ComplexFilter 
								implements Provider<ComplexSample>
{
	private Integrator[] mIntegrator;
	private Comb[] mComb;
	private Decimator mDecimator;
	private Output mOutput;

	/**
	 * Constructs the complex sample CIC filter
	 * 
	 * @param decimation - decimation factor
	 * 
	 * @param stageCount - number of integrator/comb sections to use.  The
	 * higher the number of stages, the sharper dropoff of the filter response
	 * 
	 * @param outputSampleRate - output sample rate is used to calculate the 
	 * coefficients for the final cleanup filter to adjust for frequency 
	 * response droop induced by this CIC filter
	 */
	public ComplexCICDecimate( int decimation, 
							   int stageCount,
							   int outputSampleRate )
	{
		/* Construct integrator(s) and comb(s) */
		mIntegrator = new Integrator[ stageCount ];
		mComb = new Comb[ stageCount ];
		
		for( int x = 0; x < stageCount; x++ )
		{
			mIntegrator[ x ] = new Integrator();
			mComb[ x ] = new Comb();
		}
		
		/* Connect each stage */
		int y = 1;
		
		while( y < stageCount )
		{
			mIntegrator[ y - 1 ].setListener( mIntegrator[ y ] );
			mComb[ y - 1 ].setListener( mComb[ y ] );
			
			y++;
		}
		
		/* Connect decimator to integrator output and comb input */
		mDecimator = new Decimator( decimation );
		
		mIntegrator[ stageCount - 1 ].setListener( mDecimator );
		mDecimator.setListener( mComb[ 0 ] );
		
		/* Connect output adapter to the comb output */
		mOutput = new Output( outputSampleRate, stageCount, decimation );
		mComb[ stageCount - 1 ].setListener( mOutput );
	}

	/**
	 * Sets a new decimation rate for this filter.  This method can be invoked
	 * after filter construction to change the decimation rate dynamically.
	 */
	public void setDecimationRate( int rate )
	{
		mDecimator.setDecimationRate( rate );
		mOutput.setDecimationRate( rate );
	}

	/**
	 * Input method for this filter to receive complex samples.
	 */
	@Override
    public void receive( ComplexSample sample )
    {
		mIntegrator[ 0 ].receive( (double)sample.left(), (double)sample.right() );
    }

	/**
	 * Sets the listener to receive the output of this CIC filter
	 */
	@Override
    public void setListener( Listener<ComplexSample> listener )
    {
		mOutput.setListener( listener );
    }

	/**
	 * Removes the listener from receiving the output of this CIC filter
	 */
	@Override
    public void removeListener( Listener<ComplexSample> listener )
    {
		mOutput.removeListener( listener );
    }
	
	@Override
	public Listener<ComplexSample> getListener()
	{
		return mOutput.getListener();
	}
	
	/**
	 * CIC Comb filter section
	 */
	public class Comb implements InternalListener
	{
		private double mPreviousLeft;
		private double mPreviousRight;
		private InternalListener mCombListener;

		/**
		 * Performs comb filtering on the received values, subtracting the 
		 * previous received samples from these samples.
		 */
		@Override
        public void receive( double left, double right )
        {
			mCombListener.receive( ( left - mPreviousLeft ), 
								   ( right - mPreviousRight ) );
			
			mPreviousLeft = left;
			mPreviousRight = right;
        }
		
		/**
		 * Sets the comb output listener.
		 */
		public void setListener( InternalListener listener )
		{
			mCombListener = listener;
		}
	}

	/**
	 * CIC Integrator section
	 */
	public class Integrator implements InternalListener
	{
		private double mRunningSumLeft = 0.0d;
		private double mRunningSumRight = 0.0d;
		private InternalListener mIntegratorListener;

		/**
		 * Integrates the received values with the previous running sum and
		 * sends the new running sum to the registered listener
		 */
		@Override
        public void receive( double left, double right )
        {
			mRunningSumLeft += left;
			mRunningSumRight += right;
			
			mIntegratorListener.receive( mRunningSumLeft, mRunningSumRight );
        }

		/**
		 * Sets the integrator output listener
		 */
		public void setListener( InternalListener listener )
		{
			mIntegratorListener = listener;
		}
	}

	/**
	 * Decimator - passes only 1 of xx samples to a registered listener and
	 * discards all other samples.
	 */
	public class Decimator implements InternalListener
	{
		private InternalListener mDecimatorListener;
		private int mPointer = 0;
		private int mDecimationRate;

		/**
		 * Constructs a new Decimator object with the specified decimation rate.
		 */
		public Decimator( int rate )
		{
			mDecimationRate = rate;
		}

		/**
		 * Sets a new decimation rate.  Rate can be changed after this object
		 * is constructed.
		 */
		public synchronized void setDecimationRate( int rate )
		{
			mDecimationRate = rate;
		}

		/**
		 * Receives samples allowing only 1 of every (rate) sample to go on 
		 * to the registered listener
		 */
		@Override
        public void receive( double left, double right )
        {
			mPointer++;
			
			if( mPointer >= mDecimationRate )
			{
				mDecimatorListener.receive( left, right );

				mPointer = 0;
			}
        }
		
		/**
		 * Sets the decimated output listener
		 */
		public void setListener( InternalListener listener )
		{
			mDecimatorListener = listener;
		}
	}
	
	/**
	 * Output adapter - applies gain correction, applies cleanup filter, casts 
	 * double-valued samples into float-valued complex sample and sends the 
	 * result to the registered listener.
	 */
	public class Output implements InternalListener
	{
		private int mStageCount;
		private int mDecimation;
		private double mGain = 1.0d;
		private ComplexFilter mCleanupFilter;

		public Output( int outputSampleRate, int stageCount, int decimation )
		{
			mStageCount = stageCount;
			mDecimation = decimation;
			mCleanupFilter = new ComplexFIRFilter( FilterFactory
					.getCICCleanupFilter( outputSampleRate, 
										  stageCount, 
										  WindowType.BLACKMAN ), 1.0d );
			
			setGain();
		}

		/**
		 * Receiver method for the output adapter to receive a filtered,
		 * decimated sample, apply gain correction, apply cleanup filtering
		 * and output the sample values as a complex sample.
		 */
		@Override
        public void receive( double left, double right )
        {
			mCleanupFilter.receive( 
					new ComplexSample( (float)( left * mGain ), 
									   (float)( right * mGain ) ) );
        }
		
		/**
		 * Sets a new decimation rate
		 */
		public void setDecimationRate( int rate )
		{
			mDecimation = rate;
			setGain();
		}

		/**
		 * Adds a listener to receive output samples
		 */
        public void setListener( Listener<ComplexSample> listener )
        {
			mCleanupFilter.setListener( listener );
        }

		/**
		 * Removes the listener from receiving output samples
		 */
        public void removeListener( Listener<ComplexSample> listener )
        {
			mCleanupFilter.setListener( null );
        }
		
		public Listener<ComplexSample> getListener()
		{
			return mCleanupFilter.getListener();
		}
		
		/**
		 * Adjusts the gain correction factor for output samples.
		 * 
		 * The CIC decimation filter induces significant gain as a factor of 
		 * delay, decimation rate and stage count.  So, we apply a gain reduction 
		 * to offset the added gain estimate:
		 * 
		 * gain = ( decimation * delay ) to power of stageCount
		 * 
		 * Delay is ONE for the Comb used in this filter.
		 */
		private void setGain()
		{
			/* decimation is NOT multiplied by delay since delay is 1 */
			double gain = Math.pow( (double)mDecimation, (double)mStageCount );
			
			mGain = 10.0d / gain;
		}
	}
	
	/**
	 * Internal interface for each of the CIC components to pass samples
	 */
	private interface InternalListener
	{
		public void receive( double left, double right );
	}
}

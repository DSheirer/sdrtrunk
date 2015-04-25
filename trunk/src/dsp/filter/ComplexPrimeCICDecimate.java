package dsp.filter;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexSample;
import sample.complex.ComplexSampleListener;
import sample.decimator.ComplexDecimator;
import dsp.filter.Window.WindowType;

public class ComplexPrimeCICDecimate 
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ComplexPrimeCICDecimate.class );

	public static int[] PRIMES = { 2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,
		59,61,67,71,73,79,83,89,97,101,103,107,109,113,127,131,137,139,149,151,
		157,163,167,173,179,181,191,193,197,199,211,223,227,229,233,239,241,251,
		257,263,269,271,277,281,283,293,307,311,313,317,331,337,347,349,353,359,
		367,373,379,383,389,397,401,409,419,421,431,433,439,443,449,457,461,463,
		467,479,487,491,499,503,509,521,523,541,547,557,563,569,571,577,587,593,
		599,601,607,613,617,619,631,641,643,647,653,659,661,673,677,683,691 };
	
	private ArrayList<DecimatingStage> mDecimatingStages = 
				new ArrayList<DecimatingStage>();
	
	private DecimatingStage mFirstDecimatingStage;

	private Output mOutput;
	
	private Listener<ComplexSample> mListener;
	
	/**
	 * Non-Recursive Prime-Factor CIC Filter with float sample array inputs and
	 * decimated, single paired i/q float sample output.
	 * 
	 * Implements the CIC filter described in Understanding Digital Signal 
	 * Processing, 3e, Lyons, on page 769.  This filter is comprised of multiple 
	 * decimating stages each with a prime factor decimation rate.  Multiple 
	 * stages are cascaded to achieve the overall decimation rate.
	 * 
	 * This filter supports a maximum decimation rate of 700.  This filter can
	 * be adapted to higher decimation rates by adding additional prime factors
	 * to the PRIMES array.
	 * 
	 * @param decimation - overall decimation rate
	 * @param order - filter order
	 */
	public ComplexPrimeCICDecimate( int decimation, int order, 
			int passFrequency, int attenuation, WindowType windowType )
	{
		assert( decimation <= 700 );
		
		List<Integer> stageSizes = getPrimeFactors( decimation );
		
		for( int x = 0; x < stageSizes.size(); x++ )
		{
			DecimatingStage stage = new DecimatingStage( stageSizes.get( x ), order );
			
			mDecimatingStages.add( stage );
			
			if( x == 0 )
			{
				/* Reference to first stage -- will receive all samples */
				mFirstDecimatingStage = stage;
			}
			else
			{
				/* Wire the current stage to the previous stage */
				mDecimatingStages.get( x - 1 ).setListener( stage );
			}
		}
		
		mOutput = new Output( 48000, passFrequency, attenuation, windowType );
		
		mDecimatingStages.get( mDecimatingStages.size() - 1 )
							.setListener( mOutput );
	}
	
	public void dispose()
	{
		for( DecimatingStage stage: mDecimatingStages )
		{
			stage.dispose();
		}
		
		mDecimatingStages.clear();
	}
	
	/**
	 * Adds a listener to receive the output of this CIC decimation filter
	 */
	public void setListener( Listener<ComplexSample> listener )
	{
		mOutput.setListener( listener );
	}
	
	/**
	 * Removes listener from output of this CIC decimation filter
	 */
	public void removeListener( Listener<ComplexSample> listener )
	{
		mOutput.removeListener( listener );
	}

	/**
	 * Calculates the prime factors of the decimation rate up to a maximum 
	 * decimation rate of 700.  If you wish to have decimation rates higher 
	 * than 700, then add additional prime factors to the PRIMES array.
	 * 
	 * @param decimation - integral decimation rate
	 * @return - ordered list (smallest to largest) of prime factors
	 */
	public static List<Integer> getPrimeFactors( int decimation )
	{
		ArrayList<Integer> stages = new ArrayList<Integer>();

		int pointer = 0;
		
		while( decimation > 0 && pointer < PRIMES.length )
		{
			int prime = PRIMES[ pointer ];
			
			if( decimation % prime == 0 )
			{
				stages.add( prime );
				
				decimation /= prime;
			}
			else
			{
				pointer++;
			}
		}

		return stages;
	}

	/**
	 * Primary input method for receiving sample arrays composed as I,Q,I,Q, etc.
	 */
    public void receive( float[] samples )
    {
		for( int x = 0; x < samples.length; x += 2 )
		{
			mFirstDecimatingStage.receive( samples[ x ], samples[ x + 1 ] );
		}

		samples = null;
    }
	
	/**
	 * Decimating stage combines multiple CIC stages with a decimator.  The 
	 * number of stages is indicated by the order value and the size indicates
	 * the decimation rate of this stage.
	 */
	public class DecimatingStage implements ComplexSampleListener
	{
		private ArrayList<Stage> mStages = new ArrayList<Stage>();
		private Stage mFirstStage;
		private ComplexDecimator mDecimator;
		
		public DecimatingStage( int size, int order )
		{
			for( int x = 0; x < order; x++ )
			{
				Stage stage;
				
				if( size == 2 )
				{
					stage = new TwoStage();
				}
				else
				{
					stage = new Stage( size );
				}
				
				mStages.add( stage );
				
				if( x == 0 )
				{
					mFirstStage = stage;
				}
				else
				{
					mStages.get( x - 1 ).setListener( stage );
				}
			}
			
			mDecimator = new ComplexDecimator( size );
			
			mStages.get( mStages.size() - 1 ).setListener( mDecimator );
		}
		
		public void dispose()
		{
			for( Stage stage: mStages )
			{
				stage.dispose();
			}
			
			mStages.clear();
			mDecimator.dispose();
			mDecimator = null;
			mFirstStage = null;
			mStages = null;
		}

		@Override
        public void receive( float i, float q )
        {
			mFirstStage.receive( i, q );
        }
		
		public void setListener( ComplexSampleListener listener )
		{
			mDecimator.setListener( listener );
		}
	}
	
	/**
	 * Single non-decimating CIC stage component.  Uses a circular buffer and a
	 * running average internally to implement the stage so that stage size has 
	 * essentially no impact on the computational requirements of the stage 
	 */
	public class Stage implements ComplexSampleListener
	{
		protected ComplexSampleListener mListener;
		
		private float[] mISamples;
		private float[] mQSamples;
		
		protected float mISum;
		protected float mQSum;
		
		private int mSamplePointer = 0;
		private int mSize;
	
		protected float mGain;
		
		public Stage()
		{
		}

		public Stage( int size )
		{
			mSize = size - 1;
			
			mISamples = new float[ mSize ];
			mQSamples = new float[ mSize ];
			
			mGain = 1.0f / (float)size;
		}
		
		public void dispose()
		{
			mListener = null;
		}
		
		public void receive( float i, float q )
		{
			/* Subtract the oldest sample and add back in the newest */
			mISum = mISum - mISamples[ mSamplePointer ] + i;
			mQSum = mQSum - mQSamples[ mSamplePointer ] + q;

			/* Overwrite the oldest sample with the newest */
			mISamples[ mSamplePointer ] = i;
			mQSamples[ mSamplePointer ] = q;
			
			mSamplePointer++;
			
			if( mSamplePointer >= mSize )
			{
				mSamplePointer = 0;
			}

			if( mListener != null )
			{
				mListener.receive( ( mISum * mGain ), ( mQSum * mGain ) );
			}
		}
		
		public void setListener( ComplexSampleListener listener )
		{
			mListener = listener;
		}
	}
	
	/**
	 * Size 2 stage that removes the unnecessary circular buffer management for
	 * a two-stage.
	 */
	public class TwoStage extends Stage
	{
		public TwoStage()
		{
			super();
			
			mGain = 0.5f;
		}
		
		public void receive( float i, float q )
		{
			float iSum = mISum + i;
			float qSum = mQSum + q;
			
			mISum = i;
			mQSum = q;
			
			if( mListener != null )
			{
				mListener.receive( ( iSum * mGain ), ( qSum * mGain ) );
			}
		}
	}
	

	/**
	 * Output adapter - applies gain correction, applies cleanup filter, casts 
	 * double-valued samples into float-valued complex sample and sends the 
	 * result to the registered listener.
	 */
	public class Output implements ComplexSampleListener
	{
		private ComplexFilter mCleanupFilter;
		private ComplexHalfBandNoDecimateFilter mHalfBandFilter = 
				new ComplexHalfBandNoDecimateFilter( 
						Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 0.4 );

		public Output( int outputSampleRate, int passFrequency, int attenuation, 
				WindowType windowType )
		{
			mCleanupFilter = new ComplexFIRFilter( FilterFactory
					.getCICCleanupFilter( outputSampleRate, 
										  passFrequency,
										  attenuation,
										  windowType ), 0.4d );
			
			mCleanupFilter.setListener( mHalfBandFilter );
		}

		/**
		 * Receiver method for the output adapter to receive a filtered,
		 * decimated sample, apply gain correction, apply cleanup filtering
		 * and output the sample values as a complex sample.
		 */
		@Override
        public void receive( float left, float right )
        {
			mCleanupFilter.receive( new ComplexSample( left, right ) );
        }
		
		/**
		 * Adds a listener to receive output samples
		 */
        public void setListener( Listener<ComplexSample> listener )
        {
			mHalfBandFilter.setListener( listener );
        }

		/**
		 * Removes the listener from receiving output samples
		 */
        public void removeListener( Listener<ComplexSample> listener )
        {
			mHalfBandFilter.setListener( null );
        }
		
		public Listener<ComplexSample> getListener()
		{
			return mHalfBandFilter.getListener();
		}
	}
}

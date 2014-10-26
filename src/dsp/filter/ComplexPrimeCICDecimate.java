package dsp.filter;

import java.util.ArrayList;
import java.util.List;

import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.ComplexSample;
import sample.complex.ComplexSampleListener;
import sample.decimator.ComplexDecimator;
import dsp.filter.Window.WindowType;

public class ComplexPrimeCICDecimate implements Listener<ComplexBuffer>
{
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
	
	public Broadcaster<ComplexSample> mBroadcaster = 
			new Broadcaster<ComplexSample>();

	public Output mOutput;
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
	public ComplexPrimeCICDecimate( int decimation, int order )
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
				mDecimatingStages.get( x - 1 ).addListener( stage );
			}
		}

		mOutput = new Output( 48000, stageSizes.size(), decimation );
		
		mDecimatingStages.get( mDecimatingStages.size() - 1 )
							.addListener( mOutput );
		
		mOutput.setListener( mBroadcaster );
	}
	
	public void dispose()
	{
		for( DecimatingStage stage: mDecimatingStages )
		{
			stage.dispose();
		}
		
		mDecimatingStages.clear();
		
		mBroadcaster.dispose();
		
		mBroadcaster = null;
	}
	
	/**
	 * Adds a listener to receive the output of this CIC decimation filter
	 */
	public void addListener( Listener<ComplexSample> listener )
	{
		mBroadcaster.addListener( listener );
	}

	/**
	 * Removes listener from output of this CIC decimation filter
	 */
	public void removeListener( Listener<ComplexSample> listener )
	{
		mBroadcaster.removeListener( listener );
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
	@Override
    public void receive( ComplexBuffer sampleArray )
    {
		float[] samples = sampleArray.getSamples();
		
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
				Stage stage = new Stage( size );
				
				mStages.add( stage );
				
				if( x == 0 )
				{
					mFirstStage = stage;
				}
				else
				{
					mStages.get( x - 1 ).addListener( stage );
				}
			}
			
			mDecimator = new ComplexDecimator( size );
			
			mStages.get( mStages.size() - 1 ).addListener( mDecimator );
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
		
		public void addListener( ComplexSampleListener listener )
		{
			mDecimator.setListener( listener );
		}
	}
	
	/**
	 * Single non-decimating CIC stage component.  Uses a circular buffer 
	 * internally to implement the stage so that stage size has essentially 
	 * no impact on the performance of the stage 
	 */
	public class Stage implements ComplexSampleListener
	{
		private ComplexSampleListener mListener;
		
		private float[] mISamples;
		private float[] mQSamples;
		
		private float mISum;
		private float mQSum;
		
		private int mSamplePointer;
		private int mSize;

		public Stage( int size )
		{
			mSize = size;
			mISamples = new float[ size ];
			mQSamples = new float[ size ];
		}
		
		public void dispose()
		{
			mListener = null;
			mISamples = null;
			mQSamples = null;
		}
		
		public void receive( float i, float q )
		{
			/* Subtract the oldest sample and add back in the newest */
			mISum = mISum - mISamples[ mSamplePointer ] + i;
			mQSum = mQSum - mISamples[ mSamplePointer ] + q;

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
				mListener.receive( mISum, mQSum );
			}
		}
		
		public void addListener( ComplexSampleListener listener )
		{
			mListener = listener;
		}
	}

	/**
	 * Output adapter - applies gain correction, applies cleanup filter, casts 
	 * double-valued samples into float-valued complex sample and sends the 
	 * result to the registered listener.
	 */
	public class Output implements ComplexSampleListener
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
        public void receive( float left, float right )
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
	
}

package dsp.psk;

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014, 2015 Dennis Sheirer
 * 
 * 	   Gardner detector, Costas loop and Interpolator derived from:
 * 
 * 	   OP25 - gardner_costas_cc_impl.cc
 *     Copyright 2010, 2011, 2012, 2013 KA1RBI (gardner_costas_cc_impl.cc)
 *     
 *     OP25 - scope.py
 *     Copyright 2008-2011 Steve Glass
 *     Copyright 2011, 2012, 2013 KA1RBI
 *     
 *     GNURadio - control_loop and mpsk_receiver_cc
 *     Copyright 2011,2013 Free Software Foundation, Inc.
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

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.TapGroup;
import instrument.tap.stream.EyeDiagramData;
import instrument.tap.stream.EyeDiagramDataTap;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.Provider;
import sample.complex.Complex;
import sample.complex.ComplexSampleListener;
import source.tuner.frequency.IFrequencyChangeListener;
import buffer.FloatAveragingBuffer;
import dsp.filter.interpolator.RealInterpolator;
import dsp.symbol.Dibit;

/**
 * Implements an LSM (Pi/4) demodulator using an early/late symbol timing error 
 * detector and a Costas Loop as a phase locked loop synchronized with the 
 * incoming signal carrier frequency.
 * 
 * Sample Rate: 48000
 * Symbol Rate: 4800
 */
public class LSMDemodulator2 implements Instrumentable, ComplexSampleListener, 
				Provider<Complex>
{
	private List<TapGroup> mAvailableTaps; 
	
	private final static Logger mLog = 
			LoggerFactory.getLogger( LSMDemodulator2.class );
	
	/* 45 degree rotation angle */
	public static final float THETA = (float)( Math.PI / 4.0d ); 

	/* 45 degree point */
	public static final Complex POINT_45_DEGREES = 
		new Complex( (float)Math.sin( THETA ), (float)Math.cos( THETA ) );
	
	private DecimalFormat mDecimalFormat = new DecimalFormat( "0.000000" );

	private Listener<Complex> mListener;
	
	private EarlyLateSymbolTiming mGardnerDetector = new EarlyLateSymbolTiming();
	
	private CostasLoop mCostasLoop = new CostasLoop();
	
	private IFrequencyChangeListener mFrequencyChangeListener;
	
	private EyeDiagramDataTap mEyeDiagramDataTap;
	
	public LSMDemodulator2()
	{
	}
	
	@Override
	public void receive( float inphase, float quadrature )
	{
		mGardnerDetector.receive( inphase, quadrature );
	}
	
	/**
	 * Applies a phase correction value to the costas loop to correct when a
	 * phase lock error is detected in the binary output stream.
	 * 
	 * @param correction - value in radians
	 */
	public void correctPhaseError( double correction )
	{
		mCostasLoop.correctPhaseError( correction );
	}
	
	/**
	 * Constrains value to the range of ( -maximum <> maximum )
	 */
	public static float clip( float value, float maximum )
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
	
	/**
	 * Constrains timing error to +/- the maximum value and corrects any
	 * floating point invalid numbers
	 */
	@SuppressWarnings("unused")
	private float normalize( float error, float maximum )
	{
		if( Float.isNaN( error ) )
		{
			return 0.0f;
		}
		else
		{
			return clip( error, maximum );
		}
	}
	
	public void addListener( IFrequencyChangeListener listener )
	{
		mFrequencyChangeListener = listener;
	}
	
	public void removeListener( IFrequencyChangeListener listener )
	{
		mFrequencyChangeListener = null;
	}

	/**
	 * Processes the current loop frequency of the costas loop and broadcasts
	 * tuner frequency offset adjustments 10 times a second as needed to keep
	 * the frequency offset below half of the maximum frequency
	 */
	public class FrequencyControl
	{	
		/* Set the trigger threshold at half of the maximum costas loop control
		 * frequency */
		private static final float CORRECTION_THRESHOLD_FREQUENCY = 
			( 2.0f * (float)Math.PI * 600.0f ) / 48000.0f;

		private FloatAveragingBuffer mBuffer = new FloatAveragingBuffer( 40 );
		
		private long mFrequencyError = 0;
		
		private int mCounter = 0;
		
		public void receive( float frequency )
		{
//			mCounter++;
//			
//			if( mCounter >= 480 )
//			{
//				float average = mBuffer.get( frequency );
//				mLog.debug( "Avg:" + average + " Threshold:" + CORRECTION_THRESHOLD_FREQUENCY);
//
//				boolean correctionNeeded = false;
//				
//				if( average > CORRECTION_THRESHOLD_FREQUENCY )
//				{
//					mFrequencyError--;
//
//					correctionNeeded = true;
//				}
//				else if( average < -CORRECTION_THRESHOLD_FREQUENCY )
//				{
//					mFrequencyError++;
//					
//					correctionNeeded = true;
//				}
//				
//				if( correctionNeeded && mFrequencyChangeListener != null )
//				{
//					mLog.debug( "Issuing Frequency Correction: " + mFrequencyError );
//					mFrequencyChangeListener.frequencyChanged( 
//						new FrequencyChangeEvent( Attribute.FREQUENCY_ERROR, 
//								mFrequencyError ) );
//				}
//				
//				mCounter = 0;
//			}
//			else
//			{
//				mBuffer.get( frequency );
//			}
		}
	}

	/**
	 * Early/Late symbol timing error detector.  Linear Simulcast Modulation (LSM)
	 * uses a Raised Cosine Filter at the transmitter which causes the 
	 * signal magnitude to rise to unity at the optimal sampling point and drop
	 * to zero or a midpoint between unity and zero between each symbol period.
	 * 
	 * This error detector continually adjusts symbol timing by continuously 
	 * attempting to find the largest magnitude value between two interpolated
	 * sample points during each symbol period. 
	 */
	public class EarlyLateSymbolTiming implements ComplexSampleListener
	{
		public static final int SAMPLES_PER_SYMBOL = 10;
		public static final int TWICE_SAMPLES_PER_SYMBOL = 20;
		
		private float[] mDelayLineInphase = new float[ 2 * TWICE_SAMPLES_PER_SYMBOL ];
		private float[] mDelayLineQuadrature = new float[ 2 * TWICE_SAMPLES_PER_SYMBOL ];

		private int mDelayLinePointer = 0;

		/* Sampling point */
		private float mMu = 10.0f;
		private float mGainMu = 0.5f;
		
		/* Samples per symbol */
		private float mOmega = 10.0f;
		private float mGainOmega = 0.1f * mGainMu * mGainMu;
		private float mOmegaRel = 0.005f;
		private float mOmegaMid = 10.0f;

		private RealInterpolator mInterpolator = new RealInterpolator( 1.0f );
		
		private Complex mPreviousSample = new Complex( 0.0f, 0.0f );

		/**
		 * Provides symbol sampling timing control
		 */
		public EarlyLateSymbolTiming()
		{
		}

		@Override
		public void receive( float inphase, float quadrature )
		{
			/* Count down samples per symbol until we calculate the symbol */
			mMu--;

			mCostasLoop.increment();
			
			/* Mix incoming sample with costas loop to remove any rotation 
			 * that is present from a mis-tuned carrier frequency */
			Complex derotatedSample = Complex.multiply( 
					mCostasLoop.getCurrentVector(), inphase, quadrature );

			/* Fill up the delay line to use with the interpolator */
			mDelayLineInphase[ mDelayLinePointer ] = derotatedSample.inphase();
			mDelayLineInphase[ mDelayLinePointer + TWICE_SAMPLES_PER_SYMBOL ] = derotatedSample.inphase();
			mDelayLineQuadrature[ mDelayLinePointer ] = derotatedSample.quadrature();
			mDelayLineQuadrature[ mDelayLinePointer + TWICE_SAMPLES_PER_SYMBOL ] = derotatedSample.quadrature();
			
			/* Imcrement pointer and keep pointer in bounds */
			mDelayLinePointer = ( mDelayLinePointer + 1 ) % TWICE_SAMPLES_PER_SYMBOL;
			
			/* Calculate the symbol once we've stored enough samples */
			if( mMu <= 1.0f )
			{
				Complex earlySample = mInterpolator.filter( mDelayLineInphase, 
						mDelayLineQuadrature, mDelayLinePointer, mMu );
				
				Complex middleSample = mInterpolator.filter( mDelayLineInphase, 
						mDelayLineQuadrature, mDelayLinePointer + 1, mMu );

				Complex lateSample = mInterpolator.filter( mDelayLineInphase, 
						mDelayLineQuadrature, mDelayLinePointer + 2, mMu );
				
				Complex currentSymbol = Complex.multiply( 
						middleSample, mPreviousSample.conjugate() );

				/* Set symbol gain to unity */
				currentSymbol.normalize();

				float earlyLateError = 
						lateSample.magnitude() - earlySample.magnitude();
				
				if( mEyeDiagramDataTap != null )
				{
					mEyeDiagramDataTap.receive( new EyeDiagramData( 
						Arrays.copyOfRange( mDelayLineInphase, 0, 20 ), 
						Arrays.copyOfRange( mDelayLineQuadrature, 0, 20 ),
						(float)mDelayLinePointer + 4.0f + mMu, 
						(float)mDelayLinePointer + 4.0f + mMu + 0.25f, 
						earlyLateError ) );
				}

				/* mOmega is samples per symbol and is constrained to floating
				 * between +/- .005 of the nominal 10.0 samples per symbol */
				mOmega = mOmega + mGainOmega * earlyLateError;
				mOmega = mOmegaMid + clip( mOmega - mOmegaMid, mOmegaRel );


				/* Adjust sample timing based on error of current sample */
				mMu += mOmega + ( mGainMu * earlyLateError );
				

				/* Store current samples/symbols to use for the next period */
				mPreviousSample = middleSample;

				/* Update costas loop using phase error present in current 
				 * symbol.  The symbol is rotated from star orientation to polar
				 * orientation to simplify error calculation */
				mCostasLoop.receive( currentSymbol );
//				mCostasLoop.receive( 
//						ComplexSample.multiply( currentSymbol, POINT_45_DEGREES ) );
				
				/* Dispatch the differentiated symbol to the registered listener */
				if( mListener != null )
				{
					mListener.receive( currentSymbol );
				}
			}
		}
	}
	
	/**
	 * Costas Loop - phase locked loop synchronized to the frequency offset of
	 * the incoming signal to enable the signal to be mixed down to zero offset
	 * for proper synchronization.  The mFrequency value indicates the detected
	 * frequency offset.  We attempt to keep that value close to zero by issuing
	 * frequency adjustments to the tuner channel source.
	 * 
	 * Most of the costas loop code was ported from gnuradio/control_loop and 
	 * gnuradio/mpsk_receiver_cc using initialization values from KA1RBI's 
	 * OP25/cqpsk.py
	 */
	public class CostasLoop implements Listener<Complex>
	{
		public static final double TWO_PI = 2.0 * Math.PI;
		
		private static final float MAXIMUM_FREQUENCY = 
				( 2400.0f * (float)TWO_PI ) / 48000.0f;
		
		/* http://www.trondeau.com/blog/2011/8/13/control-loop-gain-values.html */
		private float mDamping = (float)Math.sqrt( 2.0 ) / 2.0f;
		
		/* Use denominator between 100 (most) and 400 (least) to adjust costas
		 * loop control level */
		private float mLoopBandwidth = (float)( TWO_PI / 400.0d );
		
		private float mAlphaGain = ( 4.0f * mDamping * mLoopBandwidth ) / 
						  ( 1.0f + ( 2.0f * mDamping * mLoopBandwidth ) + 
								   ( mLoopBandwidth * mLoopBandwidth ) );

		private float mBetaGain = ( 4.0f * mLoopBandwidth * mLoopBandwidth ) / 
							   ( 1.0f + ( 2.0f * mDamping * mLoopBandwidth ) + 
								   ( mLoopBandwidth * mLoopBandwidth ) );

		private float mLoopPhase = 0.0f;
		
		private float mLoopFrequency = 0.0f;
		
		private FrequencyControl mFrequencyControl = new FrequencyControl();
		
		public CostasLoop()
		{
		}
		
		/**
		 * Applies a phase correction value to the current phasor.  Use this 
		 * method to apply correction when a +/- 90, or 180 degree phase error
		 * is detected in the binary output stream.
		 * 
		 * If the supplied correction value places the loop frequency outside
		 * of the max frequency, then the frequency will be corrected 360
		 * degrees in the opposite direction to maintain within the max
		 * frequency bounds.
		 * 
		 * @param correction - correction value in radians
		 */
		public void correctPhaseError( double correction )
		{
			mLoopFrequency += correction;
		
			if( mLoopFrequency > MAXIMUM_FREQUENCY )
			{
				mLoopFrequency -= 2.0d * MAXIMUM_FREQUENCY;
			}
			
			if( mLoopFrequency < -MAXIMUM_FREQUENCY )
			{
				mLoopFrequency += 2.0d * MAXIMUM_FREQUENCY;
			}
		}
		
		/**
		 * Increments the phase of the loop
		 */
		public void increment()
		{
			mLoopPhase += mLoopFrequency;

			/* Keep the loop phase in bounds */
			unwrapPhase();
		}

		private void unwrapPhase()
		{
			while( mLoopPhase > TWO_PI )
			{
				mLoopPhase -= TWO_PI;
			}
			
			while( mLoopPhase < -TWO_PI )
			{
				mLoopPhase += TWO_PI;
			}
		}
		
		/**
		 * Current vector of the loop rotated by THETA degrees.
		 */
		public Complex getCurrentVector()
		{
//			return ComplexSample.fromAngle( mLoopPhase + THETA );
			return Complex.fromAngle( mLoopPhase );
		}
		
		@Override
		public void receive( Complex sample )
		{
			/* Calculate phase error */
			float phaseError = getPhaseError( sample );
			
			Dibit d = QPSKPolarSlicer.decide( sample );
//			mLog.debug( "Dibit: " + d.name() + " Error: " + phaseError );

			/* Adjust for phase error */
			adjust( phaseError );
		}

		/**
		 * Updates the costas loop frequency and phase to adjust for the phase
		 * error value 
		 * 
		 * @param phase_error - (-)= late and (+)= early
		 */
		private void adjust( float phase_error )
		{
			mLoopFrequency += mBetaGain * phase_error;
			mLoopPhase += mLoopFrequency + mAlphaGain * phase_error;

			/* Maintain phase between +/- 2 * PI */
			unwrapPhase();

			/* Limit frequency to +/- maximum loop frequency */
			limitFrequency();
		}

		/**
		 * Constrains the frequency within the bounds of +/- loop frequency
		 */
		private void limitFrequency()
		{
			/* Check for and issue tuner offset correction */
			mFrequencyControl.receive( mLoopFrequency );
			
			if( mLoopFrequency > MAXIMUM_FREQUENCY )
			{
				mLoopFrequency = MAXIMUM_FREQUENCY;
			}
			
			if( mLoopFrequency < -MAXIMUM_FREQUENCY )
			{
				mLoopFrequency = -MAXIMUM_FREQUENCY;
			}
		}

		/**
		 * Detects rotational error present when the costas loop is not aligned
		 * with the carrier frequency.  Provides error feedback to adjust 
		 * mixer frequency.
		 */
		public float getPhaseError( Complex sample )
		{
			  float phase_error = 0;
			  
			  if( Math.abs( sample.real() ) > Math.abs( sample.imaginary() ) ) 
			  {
				  if( sample.real() > 0 )
				  {
					  phase_error = -sample.imaginary();
				  }
				  else
				  {
					  phase_error = sample.imaginary();
				  }
			  }
			  else 
			  {
				  if( sample.imaginary() > 0 )
				  {
					  phase_error = sample.real();
				  }
				  else
				  {
					  phase_error = -sample.real();
				  }
			  }
			  
			  return phase_error;
		}
	}

	@Override
	public void setListener( Listener<Complex> listener )
	{
		mListener = listener;
	}

	@Override
	public void removeListener( Listener<Complex> listener )
	{
		mListener = null;
	}

	@Override
	public List<TapGroup> getTapGroups()
	{
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<>();
			
			TapGroup group = new TapGroup( "LSM Demodulator 2" );
			group.add( new EyeDiagramDataTap( "Eye Diagram", 0, 4800 ) );

			mAvailableTaps.add( group );
		}
	
		return mAvailableTaps;
	}

	@Override
	public void registerTap( Tap tap )
	{
		if( tap instanceof EyeDiagramDataTap )
		{
			mEyeDiagramDataTap = (EyeDiagramDataTap)tap;
		}
	}

	@Override
	public void unregisterTap( Tap tap )
	{
		if( tap instanceof EyeDiagramDataTap )
		{
			mEyeDiagramDataTap = null;
		}
	}
}

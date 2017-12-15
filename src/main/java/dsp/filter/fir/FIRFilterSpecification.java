package dsp.filter.fir;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dsp.filter.fir.remez.FIRLinearPhaseFilterType;

public class FIRFilterSpecification
{
	private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat( "0.00000" );

	private FIRLinearPhaseFilterType mRemezFilterType;
	private int mOrder;
	private int mGridDensity = 16;
	private List<FrequencyBand> mFrequencyBands = new ArrayList<>();

	/**
	 * Constructs a Remez filter specification. Use one of the filter builders to construct a Remez
	 * filter specification.
	 * 
	 * @param order of the filter
	 * @param frequencyBands containing the start and stop values for each frequency band normalized
	 *            to the sample rate where each frequency value is in the range (0.0 <> 0.5)
	 * @param amplitudes of each frequency band
	 * @param weights of the band ripples normalized to the largest ripple value
	 */
	private FIRFilterSpecification( FIRLinearPhaseFilterType type, int order, int gridDensity )
	{
		mRemezFilterType = type;
		mOrder = order;
		mGridDensity = gridDensity;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append( "Type: " );
		sb.append( mRemezFilterType.name() );
		sb.append( " Order: " );
		sb.append( mOrder );
		sb.append( " Extrema: " );
		sb.append( getExtremaCount() );
		sb.append( " Grid Density: " );
		sb.append( mGridDensity );
		sb.append( " Grid Size: " );
		sb.append( getGridSize() );

		for ( FrequencyBand band : mFrequencyBands )
		{
			sb.append( "\n" );
			sb.append( band.toString() );
		}

		return sb.toString();
	}

	public void addFrequencyBand( FrequencyBand band )
	{
		mFrequencyBands.add( band );

		updateGridSize();
	}

	public void addFrequencyBands( Collection<FrequencyBand> bands )
	{
		mFrequencyBands.addAll( bands );

		updateGridSize();
	}

	public List<FrequencyBand> getFrequencyBands()
	{
		return mFrequencyBands;
	}

	/**
	 * Returns the total bandwidth of the defined frequency bands in the specification.
	 * 
	 * @return total bandwidth -- should be less than or equal to 0.5
	 */
	public double getTotalBandwidth()
	{
		double bandwidth = 0.0;

		for ( FrequencyBand band : mFrequencyBands )
		{
			bandwidth += band.getBandWidth();
		}

		return bandwidth;
	}

	/**
	 * Creates a Low-Pass FIR filter specification builder that allows you to define the filter
	 * parameters and create a filter specification.
	 */
	public static LowPassBuilder lowPassBuilder()
	{
		return new LowPassBuilder();
	}

	/**
	 * Creates a High-Pass FIR filter specification builder that allows you to define the filter
	 * parameters and create a filter specification.
	 */
	public static HighPassBuilder highPassBuilder()
	{
		return new HighPassBuilder();
	}

	/**
	 * Creates a Band-Pass FIR filter specification builder that allows you to define the filter
	 * parameters and create a filter specification.
	 */
	public static BandPassBuilder bandPassBuilder()
	{
		return new BandPassBuilder();
	}

	public FIRLinearPhaseFilterType getFilterType()
	{
		return mRemezFilterType;
	}

	public boolean isSymmetrical()
	{
		return mRemezFilterType == FIRLinearPhaseFilterType.TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL ||
			   mRemezFilterType == FIRLinearPhaseFilterType.TYPE_2_EVEN_LENGTH_ODD_ORDER_SYMMETRICAL;
	}
	
	public boolean isAntiSymmetrical()
	{
		return mRemezFilterType == FIRLinearPhaseFilterType.TYPE_3_ODD_LENGTH_EVEN_ORDER_ANTI_SYMMETRICAL ||
			   mRemezFilterType == FIRLinearPhaseFilterType.TYPE_4_EVEN_LENGTH_ODD_ORDER_ANTI_SYMMETRICAL;
	}
	
	public int getOrder()
	{
		return mOrder;
	}

	public boolean isEvenOrder()
	{
		return mOrder % 2 == 0;
	}

	public boolean isOddOrder()
	{
		return mOrder % 2 == 1;
	}

	public int getFilterLength()
	{
		return mOrder + 1;
	}

	public int getBandCount()
	{
		return mFrequencyBands.size();
	}

	/**
	 * Grid density setting. Default value is 16.
	 */
	public int getGridDensity()
	{
		return mGridDensity;
	}

	/**
	 * Sets the grid density setting. Default value is 16.
	 */
	public void setGridDensity( int density )
	{
		mGridDensity = density;

		updateGridSize();
	}

	/**
	 * Maximum number of alternations/extrema (L + 2 or M + 2)
	 */
	public int getExtremaCount()
	{
		return getHalfFilterOrder() + 2;
	}

	/**
	 * Half of the filter order.
	 * 
	 * This value is referred to as L or M, depending on the source.
	 */
	public int getHalfFilterOrder()
	{
		switch ( mRemezFilterType )
		{
		case TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL:
			return ( mOrder ) / 2;
		case TYPE_2_EVEN_LENGTH_ODD_ORDER_SYMMETRICAL:
			return ( mOrder - 1 ) / 2;
		case TYPE_3_ODD_LENGTH_EVEN_ORDER_ANTI_SYMMETRICAL:
			return ( mOrder - 2 ) / 2;
		case TYPE_4_EVEN_LENGTH_ODD_ORDER_ANTI_SYMMETRICAL:
			return ( mOrder - 1 ) / 2;
		default:
			return 0;
		}
	}

	/**
	 * Determines the max band ripple from across the frequency bands in this specification.
	 */
	public double getMaxBandAmplitude()
	{
		double maxRippleAmplitude = 0.0;

		for ( FrequencyBand band : mFrequencyBands )
		{
			double bandRippleAmplitude = band.getRippleAmplitude();

			if ( bandRippleAmplitude > maxRippleAmplitude )
			{
				maxRippleAmplitude = bandRippleAmplitude;
			}
		}

		return maxRippleAmplitude;
	}

	public int getNominalGridSize()
	{
		return ( getExtremaCount() - 1 ) * getGridDensity() + 1;
	}

	public int getGridSize()
	{
		int gridSize = 0;

		for ( FrequencyBand band : mFrequencyBands )
		{
			gridSize += band.getGridSize();
		}

		return gridSize;
	}

	private void updateGridSize()
	{
		if ( !mFrequencyBands.isEmpty() )
		{
			int gridSize = getNominalGridSize();

			double totalBandwidth = getTotalBandwidth();

			for ( FrequencyBand band : mFrequencyBands )
			{
				band.setGridSize( gridSize, totalBandwidth );
			}
		}
	}

	// /**
	// * Determines the grid size needed to represent the percentage of weighted
	// frequency bands
	// * over the entire frequency band.
	// */
	// public int getGridSize()
	// {
	// int gridSize = 0;
	//
	// int maxGridSize = getMaxGridSize();
	//
	// //Add the proportional index counts from each weighted band
	// for( FrequencyBand band: mFrequencyBands )
	// {
	// gridSize += band.getBandGridSize( maxGridSize );
	// }
	//
	// if( getSymmetry() == Symmetry.NEGATIVE )
	// {
	// gridSize--;
	// }
	//
	// return gridSize;
	// }

	/**
	 * Dense frequency spacing for extrema count * grid density over a frequency range of 0.0 - 1.0
	 */
	public double getGridFrequencyInterval()
	{
		return getTotalBandwidth() / (double) ( ( getGridSize() - mFrequencyBands.size() ) );
	}

	// /**
	// * ''' FIR order estimator (lowpass, highpass, bandpass, mulitiband).
	// *
	// * (n, fo, ao, w) = remezord (f, a, dev) (n, fo, ao, w) = remezord (f, a,
	// * dev, fs)
	// *
	// * (n, fo, ao, w) = remezord (f, a, dev) finds the approximate order,
	// * normalized frequency band edges, frequency band amplitudes, and weights
	// * that meet input specifications f, a, and dev, to use with the remez
	// * command.
	// *
	// * f is a sequence of frequency band edges (between 0 and Fs/2, where Fs
	// is
	// * the sampling frequency), and a is a sequence specifying the desired
	// * amplitude on the bands defined by f. The length of f is twice the
	// length
	// * of a, minus 2. The desired function is piecewise constant.
	// *
	// * dev is a sequence the same size as a that specifies the maximum
	// allowable
	// * deviation or ripples between the frequency response and the desired
	// * amplitude of the output filter, for each band.
	// *
	// * Use remez with the resulting order n, frequency sequence fo, amplitude
	// * response sequence ao, and weights w to design the filter b which
	// * approximately meets the specifications given by remezord input
	// parameters
	// * f, a, and dev:
	// *
	// * b = remez (n, fo, ao, w)
	// *
	// * (n, fo, ao, w) = remezord (f, a, dev, Fs) specifies a sampling
	// frequency
	// * Fs.
	// *
	// * Fs defaults to 2 Hz, implying a Nyquist frequency of 1 Hz. You can
	// * therefore specify band edges scaled to a particular applications
	// sampling
	// * frequency.
	// *
	// * In some cases remezord underestimates the order n. If the filter does
	// not
	// * meet the specifications, try a higher order such as n+1 or n+2. '''
	// *
	// * @param fcuts
	// * @param mags
	// * @param devs
	// * @param fsamp
	// */
	// // TODO: rename to calculateFilterOrder()
	// public static FIRFilterSpecification remezOrd( int[] fcutoffs,
	// double[] mags, double[] devs, int fsamp )
	// {
	// // def remezord (fcuts, mags, devs, fsamp = 2):
	//
	// double[] fcuts = new double[ fcutoffs.length ];
	//
	// for ( int x = 0; x < fcuts.length; x++ )
	// {
	// fcuts[ x ] = (double) fcutoffs[ x ] / (double) fsamp;
	// }
	//
	// int nf = fcuts.length;
	// int nm = mags.length;
	// int nd = devs.length;
	// int nbands = nm;
	//
	// if ( mags.length != devs.length )
	// {
	// throw new IllegalArgumentException(
	// "Length of magnitudes and deviations must be equal" );
	// }
	//
	// if ( fcuts.length != ( 2 * ( mags.length - 1 ) ) )
	// {
	// throw new IllegalArgumentException(
	// "Length of frequency band edges must be 2 * mags length - 2" );
	// }
	//
	// for ( int x = 0; x < mags.length; x++ )
	// {
	// if ( mags[ x ] != 0 )
	// {
	// devs[ x ] = devs[ x ] / mags[ x ];
	// }
	// }
	//
	// // Separate the passband and stopband edges
	// double[] f1 = new double[ fcuts.length / 2 ];
	// double[] f2 = new double[ fcuts.length / 2 ];
	//
	// for ( int x = 0; x < fcuts.length / 2; x++ )
	// {
	// f1[ x ] = fcuts[ 2 * x ];
	// f2[ x ] = fcuts[ 2 * x + 1 ];
	// }
	//
	// int n = 0;
	//
	// double min_delta = 2.0;
	//
	// for ( int x = 0; x < f1.length; x++ )
	// {
	// if ( f2[ x ] - f1[ x ] < min_delta )
	// {
	// n = x;
	// min_delta = f2[ x ] - f1[ x ];
	// }
	// }
	//
	// double l;
	//
	// if ( nbands == 2 )
	// {
	// // lowpass or highpass case (use formula)
	// l = lporder( f1[ n ], f2[ n ], devs[ 0 ], devs[ 1 ] );
	// } else
	// {
	// // bandpass or multipass case
	// // try different lowpasses and take the worst one that
	// // goes through the BP specs
	// l = 0;
	//
	// for ( int x = 1; x < nbands - 1; x++ )
	// {
	// double l1 = lporder( f1[ x - 1 ], f2[ x - 1 ], devs[ x ],
	// devs[ x - 1 ] );
	// double l2 = lporder( f1[ x ], f2[ x ], devs[ x ],
	// devs[ x + 1 ] );
	// l = Math.max( l1, l2 );
	// }
	// }
	//
	// n = (int) ( Math.ceil( l ) - 1 ); // need order, not length for remez
	//
	// // // cook up remez compatible result
	// double[] ff = new double[ fcuts.length + 2 ];
	// ff[ 0 ] = 0.0;
	// ff[ ff.length - 1 ] = 1.0;
	//
	// System.arraycopy( fcuts, 0, ff, 1, fcuts.length );
	//
	// for ( int x = 1; x < ff.length - 1; x++ )
	// {
	// ff[ x ] *= 2;
	// }
	//
	// double[] aa = new double[ mags.length * 2 ];
	//
	// for ( int x = 0; x < mags.length; x++ )
	// {
	// aa[ 2 * x ] = mags[ x ];
	// aa[ 2 * x + 1 ] = mags[ x ];
	// }
	//
	// double max_dev = 0;
	//
	// for ( double dev : devs )
	// {
	// if ( dev > max_dev )
	// {
	// max_dev = dev;
	// }
	// }
	//
	// double[] wts = new double[ devs.length ];
	//
	// for ( int x = 0; x < devs.length; x++ )
	// {
	// wts[ x ] = max_dev / devs[ x ];
	// }
	//
	// return null;
	// // return new RemezFilterSpecification( RemezFilterType.BANDPASS, n, ff,
	// // aa, wts, 16);
	// }

	/**
	 * Low-pass FIR filter specification builder
	 */
	public static class LowPassBuilder
	{
		private int mSampleRate;
		private int mOrder;
		private int mGridDensity = 16;
		private int mPassBandEndFrequency;
		private int mStopBandStartFrequency;
		private double mPassBandRipple;
		private double mStopBandRipple;
		private double mPassBandAmplitude = 1.0;
		private double mStopBandAmplitude = 0.0;

		public LowPassBuilder()
		{
		}

		public LowPassBuilder sampleRate( int sampleRateHz )
		{
			mSampleRate = sampleRateHz;
			return this;
		}

		/**
		 * Sets the filter order. If the order is not specified, it will be calculated from the
		 * other parameters.
		 */
		public LowPassBuilder order( int order )
		{
			mOrder = order;
			return this;
		}

		public LowPassBuilder gridDensity( int density )
		{
			mGridDensity = density;
			return this;
		}

		public LowPassBuilder passBandCutoff( int frequencyHz )
		{
			mPassBandEndFrequency = frequencyHz;
			return this;
		}

		public LowPassBuilder stopBandStart( int frequencyHz )
		{
			mStopBandStartFrequency = frequencyHz;
			return this;
		}

		public LowPassBuilder passBandRipple( double rippleDb )
		{
			mPassBandRipple = rippleDb;
			return this;
		}

		public LowPassBuilder stopBandRipple( double rippleDb )
		{
			mStopBandRipple = rippleDb;
			return this;
		}

		public LowPassBuilder passBandAmplitude( double amplitude )
		{
			mPassBandAmplitude = amplitude;
			return this;
		}

		public LowPassBuilder stopBandAmplitude( double amplitude )
		{
			mStopBandAmplitude = amplitude;
			return this;
		}

		public FIRFilterSpecification build()
		{
			if ( mOrder < 6 )
			{
				mOrder = estimateFilterOrder( mSampleRate, mPassBandEndFrequency,
						mStopBandStartFrequency, mPassBandRipple, mStopBandRipple );
			}

			FIRLinearPhaseFilterType type = ( mOrder % 2 == 0
					? FIRLinearPhaseFilterType.TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL
					: FIRLinearPhaseFilterType.TYPE_2_EVEN_LENGTH_ODD_ORDER_SYMMETRICAL );

			FIRFilterSpecification spec = new FIRFilterSpecification( type, mOrder, mGridDensity );

			FrequencyBand passBand = new FrequencyBand( mSampleRate, 0, mPassBandEndFrequency,
					mPassBandAmplitude, mPassBandRipple );
			spec.addFrequencyBand( passBand );

			FrequencyBand stopBand = new FrequencyBand( mSampleRate, mStopBandStartFrequency,
					mSampleRate / 2, mStopBandAmplitude, mStopBandRipple );
			spec.addFrequencyBand( stopBand );

			return spec;
		}
	}

	/**
	 * Remez high-pass FIR filter specification builder
	 */
	public static class HighPassBuilder
	{
		private int mSampleRate;
		private int mOrder = -1;
		private int mGridDensity = 16;
		private int mPassBandStartFrequency;
		private int mStopBandEndFrequency;
		private double mPassBandRipple;
		private double mStopBandRipple;
		private double mPassBandAmplitude = 1.0;
		private double mStopBandAmplitude = 0.0;

		public HighPassBuilder()
		{
		}

		public HighPassBuilder sampleRate( int sampleRateHz )
		{
			mSampleRate = sampleRateHz;
			return this;
		}

		/**
		 * Sets the filter order. If the order is not specified, it will be calculated from the
		 * other parameters.
		 */
		public HighPassBuilder order( int order )
		{
			mOrder = order;
			return this;
		}

		public HighPassBuilder gridDensity( int density )
		{
			mGridDensity = density;
			return this;
		}

		public HighPassBuilder passBandStart( int frequencyHz )
		{
			mPassBandStartFrequency = frequencyHz;
			return this;
		}

		public HighPassBuilder stopBandCutoff( int frequencyHz )
		{
			mStopBandEndFrequency = frequencyHz;
			return this;
		}

		public HighPassBuilder passBandRipple( double rippleDb )
		{
			mPassBandRipple = rippleDb;
			return this;
		}

		public HighPassBuilder stopBandRipple( double rippleDb )
		{
			mStopBandRipple = rippleDb;
			return this;
		}

		public HighPassBuilder passBandAmplitude( double amplitude )
		{
			mPassBandAmplitude = amplitude;
			return this;
		}

		public HighPassBuilder stopBandAmplitude( double amplitude )
		{
			mStopBandAmplitude = amplitude;
			return this;
		}

		public FIRFilterSpecification build()
		{
			if ( mOrder < 6 )
			{
				mOrder = estimateFilterOrder( mSampleRate, mStopBandEndFrequency,
						mPassBandStartFrequency, mPassBandRipple, mStopBandRipple );
			}

			// High pass is a Type 1 filter -- ensure order is even
			if ( mOrder % 2 == 1 )
			{
				mOrder++;
			}

			FIRFilterSpecification spec = new FIRFilterSpecification(
					FIRLinearPhaseFilterType.TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL, mOrder,
					mGridDensity );

			FrequencyBand stopBand = new FrequencyBand( mSampleRate, 0, mStopBandEndFrequency,
					mStopBandAmplitude, mStopBandRipple );
			spec.addFrequencyBand( stopBand );

			FrequencyBand passBand = new FrequencyBand( mSampleRate, mPassBandStartFrequency,
					mSampleRate / 2, mPassBandAmplitude, mPassBandRipple );
			spec.addFrequencyBand( passBand );

			return spec;
		}
	}

	/**
	 * Band-pass or band-stop FIR filter specification builder
	 */
	public static class BandPassBuilder
	{
		private int mOrder;
		private int mGridDensity = 16;
		private int mSampleRate;
		private int mStopFrequency1;
		private int mPassFrequencyBegin;
		private int mPassFrequencyEnd;
		private int mStopFrequency2;
		private double mStopRipple = 0.01;
		private double mPassRipple = 0.01;
		private double mStopAmplitude = 0.0;
		private double mPassAmplitude = 1.0;

		public BandPassBuilder()
		{
		}

		/**
		 * Sets the filter order. If the order is not specified, it will be calculated from the
		 * other parameters. If you specify and odd-length order, it will be corrected (+1) to make
		 * it an even order number.
		 */
		public BandPassBuilder order( int order )
		{
			mOrder = order;
			return this;
		}

		/**
		 * Sets the frequency grid density. Default is 16.
		 */
		public BandPassBuilder gridDensity( int density )
		{
			mGridDensity = density;
			return this;
		}

		/**
		 * Sets the filter sample rate in hertz
		 */
		public BandPassBuilder sampleRate( int sampleRate )
		{
			mSampleRate = sampleRate;
			return this;
		}

		/**
		 * Specifies the ending frequency of the first stop band
		 */
		public BandPassBuilder stopFrequency1( int stopFrequency )
		{
			mStopFrequency1 = stopFrequency;
			return this;
		}

		/**
		 * Specifies the beginning frequency of the second stop band
		 */
		public BandPassBuilder stopFrequency2( int stopFrequency )
		{
			mStopFrequency2 = stopFrequency;
			return this;
		}

		/**
		 * Specifies the pass band beginning frequency
		 */
		public BandPassBuilder passFrequencyBegin( int passFrequency )
		{
			mPassFrequencyBegin = passFrequency;
			return this;
		}

		/**
		 * Specifies the pass band ending frequency
		 */
		public BandPassBuilder passFrequencyEnd( int passFrequency )
		{
			mPassFrequencyEnd = passFrequency;
			return this;
		}

		/**
		 * Specifies the ripple in dB for both stop bands
		 */
		public BandPassBuilder stopRipple( double rippleDb )
		{
			mStopRipple = rippleDb;
			return this;
		}

		/**
		 * Specifies the ripple in dB for the pass band
		 */
		public BandPassBuilder passRipple( double rippleDb )
		{
			mPassRipple = rippleDb;
			return this;
		}

		/**
		 * Specifies the amplitude of the stop bands. Default is 0.0
		 */
		public BandPassBuilder stopAmplitude( double amplitude )
		{
			mStopAmplitude = amplitude;
			return this;
		}

		/**
		 * Specifies the amplitude of the pass band. Default is 1.0
		 */
		public BandPassBuilder passAmplitude( double amplitude )
		{
			mPassAmplitude = amplitude;
			return this;
		}

		/**
		 * Validates the frequency bands to test for overlap and completeness.
		 */
		private void validateFrequencyBands()
		{
			if ( mStopFrequency1 >= mPassFrequencyBegin )
			{
				throw new IllegalArgumentException( "Stop band 1 ending frequency ["
						+ mStopFrequency1 + "] must be less than the pass band start frequency ["
						+ mPassFrequencyBegin + "]" );
			}

			if ( mPassFrequencyBegin >= mPassFrequencyEnd )
			{
				throw new IllegalArgumentException( "Pass band begin frequency ["
						+ mPassFrequencyBegin + "] must be less than the pass band end frequency ["
						+ mPassFrequencyEnd + "]" );
			}

			if ( mPassFrequencyEnd >= mStopFrequency2 )
			{
				throw new IllegalArgumentException( "Pass band end frequency [" + mPassFrequencyEnd
						+ "] must be less than stop band 2 beginning frequency [" + mStopFrequency2
						+ "]" );
			}

			if ( mStopFrequency2 >= ( mSampleRate / 2 ) )
			{
				throw new IllegalArgumentException( "Stop band 2 beginning frequency ["
						+ mStopFrequency2 + "] must be less than half of the sample rate ["
						+ ( mSampleRate / 2 ) + "]" );
			}
		}

		public FIRFilterSpecification build()
		{
			validateFrequencyBands();

			FIRLinearPhaseFilterType type = FIRLinearPhaseFilterType.TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL;

			if ( mOrder < 10 )
			{
				mOrder = estimateBandPassOrder( mSampleRate, mPassFrequencyBegin, mPassFrequencyEnd,
						mPassRipple, mStopRipple );
			}

			// Ensure even order since we're designing a Type 1 filter
			if ( mOrder % 2 == 1 )
			{
				mOrder++;
			}

			FIRFilterSpecification spec = new FIRFilterSpecification( type, mOrder, mGridDensity );

			spec.addFrequencyBand( new FrequencyBand( mSampleRate, 0, mStopFrequency1,
					mStopAmplitude, mStopRipple ) );
			spec.addFrequencyBand( new FrequencyBand( mSampleRate, mPassFrequencyBegin,
					mPassFrequencyEnd, mPassAmplitude, mPassRipple ) );
			spec.addFrequencyBand( new FrequencyBand( mSampleRate, mStopFrequency2,
					(int) ( mSampleRate / 2 ), mStopAmplitude, mStopRipple ) );

			return spec;
		}
	}

	// /**
	// * Converts the set of band amplitude values into a set of weights
	// * normalized to the largest band amplitude value
	// */
	// private static double[] getBandWeights( double... amplitudes )
	// {
	// double maximum = 0.0;
	//
	// for ( double amplitude : amplitudes )
	// {
	// if ( amplitude > maximum )
	// {
	// maximum = amplitude;
	// }
	// }
	//
	// double[] weights = new double[ amplitudes.length ];
	//
	// for ( int x = 0; x < amplitudes.length; x++ )
	// {
	// weights[ x ] = maximum / amplitudes[ x ];
	// }
	//
	// return weights;
	// }

	/**
	 * Calculates filter order from filter length
	 */
	private static int getFilterOrder( double length )
	{
		return (int) ( Math.ceil( length ) - 1 );
	}

	/**
	 * Estimates filter length for a two-band (high or low pass) FIR filter.
	 * 
	 * Note: this estimator doesn't work well if the transition frequency is near 0 or sampleRate/2
	 * 
	 * From Herrmann et al (1973), Practical design rules for optimum finite impulse response
	 * filters. Bell System Technical J., 52, 769-99
	 * 
	 * @param sampleRate in hertz
	 * @param frequency1 in hertz
	 * @param frequency2 in hertz
	 * @param passBandRipple pass band ripple in dB
	 * @param stopBandRipple stop band ripple in dB
	 * 
	 * @return estimated filter length
	 */
	private static int estimateFilterOrder( int sampleRate, int frequency1, int frequency2,
			double passBandRipple, double stopBandRipple )
	{
		double df = Math.abs( frequency2 - frequency1 ) / (double) sampleRate;

		double ddp = Math
				.log10( stopBandRipple <= passBandRipple ? passBandRipple : stopBandRipple );
		double dds = Math
				.log10( stopBandRipple <= passBandRipple ? stopBandRipple : passBandRipple );

		double a1 = 5.309e-3;
		double a2 = 7.114e-2;
		double a3 = -4.761e-1;
		double a4 = -2.66e-3;
		double a5 = -5.941e-1;
		double a6 = -4.278e-1;

		double b1 = 11.01217;
		double b2 = 0.5124401;

		double t1 = a1 * ddp * ddp;
		double t2 = a2 * ddp;
		double t3 = a4 * ddp * ddp;
		double t4 = a5 * ddp;

		double dinf = ( ( t1 + t2 + a3 ) * dds ) + ( t3 + t4 + a6 );
		double ff = b1 + b2 * ( ddp - dds );
		double n = dinf / df - ff * df + 1.0;

		return (int) Math.ceil( n );
	}

	/**
	 * Estimates band pass filter length;
	 * 
	 * @param passBandStart frequency normalized to sample rate
	 * @param passBandEnd frequency normalized to sample rate
	 * @param passBandRippleDb
	 * @param stopBandRippleDb
	 */
	public static int estimateBandPassOrder( int sampleRate, int passBandStart, int passBandEnd,
			double passBandRippleDb, double stopBandRippleDb )
	{
		double df = (double) Math.abs( passBandEnd - passBandStart ) / (double) sampleRate;
		double ddp = (double) Math.log10( passBandRippleDb );
		double dds = (double) Math.log10( stopBandRippleDb );

		double a1 = 0.01201;
		double a2 = 0.09664;
		double a3 = -0.51325;
		double a4 = 0.00203;
		double a5 = -0.57054;
		double a6 = -0.44314;

		double t1 = a1 * ddp * ddp;
		double t2 = a2 * ddp;
		double t3 = a4 * ddp * ddp;
		double t4 = a5 * ddp;

		double cinf = dds * ( t1 + t2 + a3 ) + t3 + t4 + a6;
		double ginf = -14.6f * (double) Math.log10( passBandRippleDb / stopBandRippleDb ) - 16.9;
		double n = cinf / df + ginf * df + 1.0;

		return (int) Math.ceil( n );
	}

	/**
	 * Frequency band
	 */
	public static class FrequencyBand
	{
		private int mGridSize;
		private double mStart;
		private double mEnd;
		private double mAmplitude;
		private double mRippleDB;

		/**
		 * Constructs a frequency band where the start and end frequencies are normalized to a
		 * sample rate of 1.
		 * 
		 * @param start edge of the frequency band (0.0 - 1.0)
		 * @param end edge of the frequency band (0.0 - 1.0)
		 * @param amplitude of the frequency band on the unity scale (0.0 - 1.0)
		 * @param ripple of the frequency band specified as db ripple
		 */
		public FrequencyBand( double start, double end, double amplitude, double ripple )
		{
			assert ( 0.0 <= mStart );
			assert ( mStart < mEnd );
			assert ( mEnd <= 1.0 );
			assert ( 0.0 <= amplitude && amplitude <= 1.0 );
			assert ( 0.0 <= ripple );

			mStart = start;
			mEnd = end;
			mAmplitude = amplitude;
			mRippleDB = ripple;
		}

		/**
		 * Constructs a frequency band for the sample rate and start and end frequencies.
		 * 
		 * @param sampleRate in hertz
		 * @param start edge of the frequency band in hertz
		 * @param end edge of the frequency band in hertz
		 * @param amplitude of the frequency band relative to unity (0.0 - 1.0)
		 * @param ripple of the frequency band specified as db ripple
		 */
		public FrequencyBand( int sampleRate, int start, int end, double amplitude, double ripple )
		{
			this( (double) start / (double) sampleRate, (double) end / (double) sampleRate,
					amplitude, ripple );
		}

		public int getGridSize()
		{
			return mGridSize;
		}

		public void setGridSize( int totalGridSize, double totalBandwidth )
		{
			mGridSize = (int) Math
					.ceil( (double) totalGridSize * ( getBandWidth() / totalBandwidth ) );
		}

		public String toString()
		{
			StringBuilder sb = new StringBuilder();

			sb.append( "Band Start: " );
			sb.append( mStart );
			sb.append( " End:" );
			sb.append( mEnd );
			sb.append( " Amplitude: " );
			sb.append( mAmplitude );
			sb.append( " Ripple dB: " );
			sb.append( mRippleDB );
			sb.append( " Ripple Amplitude: " );
			sb.append( DECIMAL_FORMATTER.format( getRippleAmplitude() ) );
			sb.append( " Grid Size: " );
			sb.append( mGridSize );

			return sb.toString();
		}

		/**
		 * Start frequency edge for this band normalized to 1 Hz.
		 */
		public double getStart()
		{
			return mStart;
		}

		/**
		 * End frequency edge for this band normalized to 1 Hz.
		 */
		public double getEnd()
		{
			return mEnd;
		}

		/**
		 * Desired amplitude response for this frequency band
		 * 
		 * @return amplitude (0.0 - 1.0)
		 */
		public double getAmplitude()
		{
			return mAmplitude;
		}

		/**
		 * Frequency band ripple in decibels
		 */
		public double getRippleDB()
		{
			return mRippleDB;
		}

		/**
		 * Frequency band ripple as an amplitude value
		 */
		public double getRippleAmplitude()
		{
			return ( Math.pow( 10.0, ( mRippleDB / 20 ) ) - 1 )
					/ ( Math.pow( 10.0, ( mRippleDB / 20 ) ) + 1 );
		}

		/**
		 * Calculates the weighting of this frequency band's ripple relative to the max ripple for a
		 * filter specification.
		 * 
		 * @param maxRippleAmplitude for the filter specification
		 * 
		 * @return weight of this frequency band
		 */
		public double getWeight( double maxRippleAmplitude )
		{
			return 1.0 / ( getRippleAmplitude() / maxRippleAmplitude );
		}

		/**
		 * Double-sided bandwidth of this frequency band
		 */
		public double getBandWidth()
		{
			return mEnd - mStart;
		}
	}
}

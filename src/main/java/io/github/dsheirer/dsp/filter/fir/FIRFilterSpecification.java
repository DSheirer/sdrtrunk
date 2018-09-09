package io.github.dsheirer.dsp.filter.fir;

import io.github.dsheirer.dsp.filter.fir.remez.FIRLinearPhaseFilterType;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FIRFilterSpecification
{
    private final static Logger mLog = LoggerFactory.getLogger(FIRFilterSpecification.class);

    private static final double PERFECT_RECONSTRUCTION_GAIN_AT_BAND_EDGE = -6.020599842071533; //0.5 magnitude
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("0.00000");

    private FIRLinearPhaseFilterType mRemezFilterType;
    private int mOrder;
    private int mGridDensity = 16;
    private List<FrequencyBand> mFrequencyBands = new ArrayList<>();

    /**
     * Constructs a Remez filter specification. Use one of the filter builders to construct a Remez
     * filter specification.
     *
     * @param type of filter
     * @param order of the filter
     * @param gridDensity to use when calculating the coefficients
     */
    protected FIRFilterSpecification(FIRLinearPhaseFilterType type, int order, int gridDensity)
    {
        mRemezFilterType = type;
        mOrder = order;
        mGridDensity = gridDensity;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: ");
        sb.append(mRemezFilterType.name());
        sb.append(" Order: ");
        sb.append(mOrder);
        sb.append(" Extrema: ");
        sb.append(getExtremaCount());
        sb.append(" Grid Density: ");
        sb.append(mGridDensity);
        sb.append(" Grid Size: ");
        sb.append(getGridSize());

        for(FrequencyBand band : mFrequencyBands)
        {
            sb.append("\n");
            sb.append(band.toString());
        }

        return sb.toString();
    }

    public void addFrequencyBand(FrequencyBand band)
    {
        mFrequencyBands.add(band);

        updateGridSize();
    }

    public void addFrequencyBands(Collection<FrequencyBand> bands)
    {
        mFrequencyBands.addAll(bands);

        updateGridSize();
    }

    /**
     * Removes all frequency bands from this filter specification
     */
    public void clearFrequencyBands()
    {
        mFrequencyBands.clear();
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

        for(FrequencyBand band : mFrequencyBands)
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

    /**
     * Creates a Polyphase TunerChannelizer filter specification builder that allows you to define the filter parameters and
     * create a filter specification
     */
    public static ChannelizerBuilder channelizerBuilder()
    {
        return new ChannelizerBuilder();
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
    public void setGridDensity(int density)
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
        switch(mRemezFilterType)
        {
            case TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL:
                return (mOrder) / 2;
            case TYPE_2_EVEN_LENGTH_ODD_ORDER_SYMMETRICAL:
                return (mOrder - 1) / 2;
            case TYPE_3_ODD_LENGTH_EVEN_ORDER_ANTI_SYMMETRICAL:
                return (mOrder - 2) / 2;
            case TYPE_4_EVEN_LENGTH_ODD_ORDER_ANTI_SYMMETRICAL:
                return (mOrder - 1) / 2;
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

        for(FrequencyBand band : mFrequencyBands)
        {
            double bandRippleAmplitude = band.getRippleAmplitude();

            if(bandRippleAmplitude > maxRippleAmplitude)
            {
                maxRippleAmplitude = bandRippleAmplitude;
            }
        }

        return maxRippleAmplitude;
    }

    public int getNominalGridSize()
    {
        return (getExtremaCount() - 1) * getGridDensity() + 1;
    }

    public int getGridSize()
    {
        int gridSize = 0;

        for(FrequencyBand band : mFrequencyBands)
        {
            gridSize += band.getGridSize();
        }

        return gridSize;
    }

    private void updateGridSize()
    {
        if(!mFrequencyBands.isEmpty())
        {
            int gridSize = getNominalGridSize();

            double totalBandwidth = getTotalBandwidth();

            for(FrequencyBand band : mFrequencyBands)
            {
                band.setGridSize(gridSize, totalBandwidth);
            }
        }
    }

    /**
     * Dense frequency spacing for extrema count * grid density over a frequency range of 0.0 - 1.0
     */
    public double getGridFrequencyInterval()
    {
        return getTotalBandwidth() / (double)((getGridSize() - mFrequencyBands.size()));
    }

    /**
     * Low-pass FIR filter specification builder
     */
    public static class LowPassBuilder
    {
        private double mSampleRate;
        private int mOrder;
        private Boolean mOddLength;
        private int mGridDensity = 16;
        private double mPassBandEndFrequency;
        private double mStopBandStartFrequency;
        private double mPassBandRipple;
        private double mStopBandRipple;
        private double mPassBandAmplitude = 1.0;
        private double mStopBandAmplitude = 0.0;

        public LowPassBuilder()
        {
        }

        public LowPassBuilder sampleRate(double sampleRateHz)
        {
            mSampleRate = sampleRateHz;
            return this;
        }

        /**
         * Sets the filter order. If the order is not specified, it will be calculated from the
         * other parameters.
         */
        public LowPassBuilder order(int order)
        {
            mOrder = order;
            return this;
        }

        /**
         * Indicates if the filters should be an odd-length (even order) filter.  This will override any requested
         * filter order if it is set, otherwise the filter length will be determined from the filter order.
         */
        public LowPassBuilder oddLength(boolean oddLength)
        {
            mOddLength = oddLength;
            return this;
        }

        public LowPassBuilder gridDensity(int density)
        {
            mGridDensity = density;
            return this;
        }

        public LowPassBuilder passBandCutoff(double frequencyHz)
        {
            mPassBandEndFrequency = frequencyHz;
            return this;
        }

        public LowPassBuilder stopBandStart(double frequencyHz)
        {
            mStopBandStartFrequency = frequencyHz;
            return this;
        }

        public LowPassBuilder passBandRipple(double rippleDb)
        {
            mPassBandRipple = rippleDb;
            return this;
        }

        public LowPassBuilder stopBandRipple(double rippleDb)
        {
            mStopBandRipple = rippleDb;
            return this;
        }

        public LowPassBuilder passBandAmplitude(double amplitude)
        {
            mPassBandAmplitude = amplitude;
            return this;
        }

        public LowPassBuilder stopBandAmplitude(double amplitude)
        {
            mStopBandAmplitude = amplitude;
            return this;
        }

        public FIRFilterSpecification build()
        {
            if(mOrder < 6)
            {
                mOrder = estimateFilterOrder(mSampleRate, mPassBandEndFrequency, mStopBandStartFrequency,
                    mPassBandRipple, mStopBandRipple);
            }

            FIRLinearPhaseFilterType type = null;

            if(mOddLength != null)
            {
                if(mOddLength)
                {
                    type = FIRLinearPhaseFilterType.TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL;
                    //Force order to even
                    mOrder += mOrder % 2;
                }
                else
                {
                    type = FIRLinearPhaseFilterType.TYPE_2_EVEN_LENGTH_ODD_ORDER_SYMMETRICAL;
                    //Force order to odd
                    mOrder += (mOrder % 2 == 0 ? 1 : 0);
                }
            }
            else
            {
                type = (mOrder % 2 == 0 ? FIRLinearPhaseFilterType.TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL
                    : FIRLinearPhaseFilterType.TYPE_2_EVEN_LENGTH_ODD_ORDER_SYMMETRICAL);
            }

            FIRFilterSpecification spec = new FIRFilterSpecification(type, mOrder, mGridDensity);

            FrequencyBand passBand = new FrequencyBand(mSampleRate, 0, mPassBandEndFrequency,
                mPassBandAmplitude, mPassBandRipple);
            spec.addFrequencyBand(passBand);

            FrequencyBand stopBand = new FrequencyBand(mSampleRate, mStopBandStartFrequency,
                (int)(mSampleRate / 2), mStopBandAmplitude, mStopBandRipple);
            spec.addFrequencyBand(stopBand);

            return spec;
        }
    }

    /**
     * Remez high-pass FIR filter specification builder
     */
    public static class HighPassBuilder
    {
        private double mSampleRate;
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

        public HighPassBuilder sampleRate(double sampleRateHz)
        {
            mSampleRate = sampleRateHz;
            return this;
        }

        /**
         * Sets the filter order. If the order is not specified, it will be calculated from the
         * other parameters.
         */
        public HighPassBuilder order(int order)
        {
            mOrder = order;
            return this;
        }

        public HighPassBuilder gridDensity(int density)
        {
            mGridDensity = density;
            return this;
        }

        public HighPassBuilder passBandStart(int frequencyHz)
        {
            mPassBandStartFrequency = frequencyHz;
            return this;
        }

        public HighPassBuilder stopBandCutoff(int frequencyHz)
        {
            mStopBandEndFrequency = frequencyHz;
            return this;
        }

        public HighPassBuilder passBandRipple(double rippleDb)
        {
            mPassBandRipple = rippleDb;
            return this;
        }

        public HighPassBuilder stopBandRipple(double rippleDb)
        {
            mStopBandRipple = rippleDb;
            return this;
        }

        public HighPassBuilder passBandAmplitude(double amplitude)
        {
            mPassBandAmplitude = amplitude;
            return this;
        }

        public HighPassBuilder stopBandAmplitude(double amplitude)
        {
            mStopBandAmplitude = amplitude;
            return this;
        }

        public FIRFilterSpecification build()
        {
            if(mOrder < 6)
            {
                mOrder = estimateFilterOrder(mSampleRate, mStopBandEndFrequency, mPassBandStartFrequency,
                    mPassBandRipple, mStopBandRipple);
            }

            // High pass is a Type 1 filter -- ensure order is even
            if(mOrder % 2 == 1)
            {
                mOrder++;
            }

            FIRFilterSpecification spec = new FIRFilterSpecification(
                FIRLinearPhaseFilterType.TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL, mOrder,
                mGridDensity);

            FrequencyBand stopBand = new FrequencyBand(mSampleRate, 0, mStopBandEndFrequency,
                mStopBandAmplitude, mStopBandRipple);
            spec.addFrequencyBand(stopBand);

            FrequencyBand passBand = new FrequencyBand(mSampleRate, mPassBandStartFrequency,
                (int)(mSampleRate / 2), mPassBandAmplitude, mPassBandRipple);
            spec.addFrequencyBand(passBand);

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
        private double mSampleRate;
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
        public BandPassBuilder order(int order)
        {
            mOrder = order;
            return this;
        }

        /**
         * Sets the frequency grid density. Default is 16.
         */
        public BandPassBuilder gridDensity(int density)
        {
            mGridDensity = density;
            return this;
        }

        /**
         * Sets the filter sample rate in hertz
         */
        public BandPassBuilder sampleRate(double sampleRate)
        {
            mSampleRate = sampleRate;
            return this;
        }

        /**
         * Specifies the ending frequency of the first stop band
         */
        public BandPassBuilder stopFrequency1(int stopFrequency)
        {
            mStopFrequency1 = stopFrequency;
            return this;
        }

        /**
         * Specifies the beginning frequency of the second stop band
         */
        public BandPassBuilder stopFrequency2(int stopFrequency)
        {
            mStopFrequency2 = stopFrequency;
            return this;
        }

        /**
         * Specifies the pass band beginning frequency
         */
        public BandPassBuilder passFrequencyBegin(int passFrequency)
        {
            mPassFrequencyBegin = passFrequency;
            return this;
        }

        /**
         * Specifies the pass band ending frequency
         */
        public BandPassBuilder passFrequencyEnd(int passFrequency)
        {
            mPassFrequencyEnd = passFrequency;
            return this;
        }

        /**
         * Specifies the ripple in dB for both stop bands
         */
        public BandPassBuilder stopRipple(double rippleDb)
        {
            mStopRipple = rippleDb;
            return this;
        }

        /**
         * Specifies the ripple in dB for the pass band
         */
        public BandPassBuilder passRipple(double rippleDb)
        {
            mPassRipple = rippleDb;
            return this;
        }

        /**
         * Specifies the amplitude of the stop bands. Default is 0.0
         */
        public BandPassBuilder stopAmplitude(double amplitude)
        {
            mStopAmplitude = amplitude;
            return this;
        }

        /**
         * Specifies the amplitude of the pass band. Default is 1.0
         */
        public BandPassBuilder passAmplitude(double amplitude)
        {
            mPassAmplitude = amplitude;
            return this;
        }

        /**
         * Validates the frequency bands to test for overlap and completeness.
         */
        private void validateFrequencyBands()
        {
            if(mStopFrequency1 >= mPassFrequencyBegin)
            {
                throw new IllegalArgumentException("Stop band 1 ending frequency ["
                    + mStopFrequency1 + "] must be less than the pass band start frequency ["
                    + mPassFrequencyBegin + "]");
            }

            if(mPassFrequencyBegin >= mPassFrequencyEnd)
            {
                throw new IllegalArgumentException("Pass band begin frequency ["
                    + mPassFrequencyBegin + "] must be less than the pass band end frequency ["
                    + mPassFrequencyEnd + "]");
            }

            if(mPassFrequencyEnd >= mStopFrequency2)
            {
                throw new IllegalArgumentException("Pass band end frequency [" + mPassFrequencyEnd
                    + "] must be less than stop band 2 beginning frequency [" + mStopFrequency2 + "]");
            }

            if(mStopFrequency2 >= (mSampleRate / 2))
            {
                throw new IllegalArgumentException("Stop band 2 beginning frequency [" + mStopFrequency2 +
                    "] must be less than half of the sample rate [" + (mSampleRate / 2) + "]");
            }
        }

        public FIRFilterSpecification build()
        {
            validateFrequencyBands();

            FIRLinearPhaseFilterType type = FIRLinearPhaseFilterType.TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL;

            if(mOrder < 10)
            {
                mOrder = estimateBandPassOrder(mSampleRate, mPassFrequencyBegin, mPassFrequencyEnd,
                    mPassRipple, mStopRipple);
            }

            // Ensure even order since we're designing a Type 1 filter
            if(mOrder % 2 == 1)
            {
                mOrder++;
            }

            FIRFilterSpecification spec = new FIRFilterSpecification(type, mOrder, mGridDensity);

            spec.addFrequencyBand(new FrequencyBand(mSampleRate, 0, mStopFrequency1,
                mStopAmplitude, mStopRipple));
            spec.addFrequencyBand(new FrequencyBand(mSampleRate, mPassFrequencyBegin,
                mPassFrequencyEnd, mPassAmplitude, mPassRipple));
            spec.addFrequencyBand(new FrequencyBand(mSampleRate, mStopFrequency2,
                (int)(mSampleRate / 2), mStopAmplitude, mStopRipple));

            return spec;
        }
    }

    /**
     * Builder for creating polyphase channelizer low-pass prototype filter.
     */
    public static class ChannelizerBuilder
    {
        private int mGridDensity = 16;
        private double mSampleRate;
        private int mChannelCount;
        private int mChannelBandwidth;
        private int mTapsPerChannel;
        private double mStopRipple = 0.001;
        private double mPassRipple = 0.01;
        private double mAlpha = 0.2;

        public ChannelizerBuilder()
        {
        }

        /**
         * Sets the filter sample rate in hertz
         */
        public ChannelizerBuilder sampleRate(double sampleRate)
        {
            mSampleRate = sampleRate;
            return this;
        }

        /**
         * Number of Channels
         * @param channels - must be a multiple of 2
         * @return
         */
        public ChannelizerBuilder channels(int channels)
        {
            Validate.isTrue(channels % 2 == 0, "Channel count must be a multiple of 2");
            mChannelCount = channels;
            return this;
        }

        /**
         * Sets the channel bandwidth in hertz
         */
        public ChannelizerBuilder channelBandwidth(int channelBandwidth)
        {
            mChannelBandwidth = channelBandwidth;
            return this;
        }

        /**
         * Number of filter taps per channel.  Note: total filter length will be channels * tapsPerChannel
         */
        public ChannelizerBuilder tapsPerChannel(int tapsPerChannel)
        {
            mTapsPerChannel = tapsPerChannel;
            return this;
        }

        /**
         * Sets the frequency grid density. Default is 16.
         */
        public ChannelizerBuilder gridDensity(int density)
        {
            mGridDensity = density;
            return this;
        }

        /**
         * Specifies the ripple in dB for both stop bands
         */
        public ChannelizerBuilder stopRipple(double rippleDb)
        {
            mStopRipple = rippleDb;
            return this;
        }

        /**
         * Specifies the ripple in dB for the pass band
         */
        public ChannelizerBuilder passRipple(double rippleDb)
        {
            mPassRipple = rippleDb;
            return this;
        }

        /**
         * Rolloff factor for the start band and stop band.
         * @param alpha - value in range 0.0 - 1.0
         */
        public ChannelizerBuilder alpha(double alpha)
        {
            Validate.isTrue(0.0 <= alpha && alpha <= 1.0);
            mAlpha = alpha;

            return this;
        }

        public FIRFilterSpecification build()
        {
            FIRLinearPhaseFilterType type = FIRLinearPhaseFilterType.TYPE_2_EVEN_LENGTH_ODD_ORDER_SYMMETRICAL;

            int passFrequencyEnd = (int)(mChannelBandwidth * (1.0 - mAlpha));
            int stopFrequencyBegin = (int)(mChannelBandwidth * (1.0 + mAlpha));

            int estimatedOrder = estimateFilterOrder(mSampleRate, passFrequencyEnd, stopFrequencyBegin, mPassRipple,
                mStopRipple);

            // Ensure odd order since we're designing a Type 2 filter
            if(estimatedOrder % 2 == 0)
            {
                estimatedOrder++;
            }

            int order = mChannelCount * mTapsPerChannel - 1;

            mLog.debug("Order:" + order + " Sample Rate:" + mSampleRate + " Estimated Order:" + estimatedOrder + " Requested Order:" + order);

            FIRFilterSpecification spec = new FIRFilterSpecification(type, order, mGridDensity);

            double bandEdge = (double)mChannelBandwidth / mSampleRate;
            double passEnd = bandEdge * (1.0 - mAlpha);
            double stopStart = bandEdge * (1.0 + mAlpha);

            mLog.debug("Pass End:" + passEnd + " Band Edge:" + bandEdge + " Stop Begin:" + stopStart);
            mLog.debug("Pass End:" + (int)(mSampleRate * passEnd) +
                " Band Edge:" + (int)(mSampleRate * bandEdge) +
                " Stop Begin:" + (int)(mSampleRate * stopStart));

            FrequencyBand passBand = new FrequencyBand(0.0, passEnd, 1.0, mPassRipple);
            FrequencyBand edgeBand = new FrequencyBand(bandEdge, bandEdge, 0.5, mPassRipple);
            edgeBand.setWeight(8.0 * edgeBand.getWeight(mPassRipple));
            FrequencyBand stopBand = new FrequencyBand(stopStart, 0.5, 0.0, mStopRipple);

            spec.addFrequencyBand(passBand);
            spec.addFrequencyBand(edgeBand);
            spec.addFrequencyBand(stopBand);

            mLog.debug(spec.toString());
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
    private static int getFilterOrder(double length)
    {
        return (int)(Math.ceil(length) - 1);
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
     * @return estimated filter length
     */
    public static int estimateFilterOrder(double sampleRate, double frequency1, double frequency2,
                                          double passBandRipple, double stopBandRipple)
    {
        double df = Math.abs(frequency2 - frequency1) / sampleRate;

        double ddp = Math.log10(stopBandRipple <= passBandRipple ? passBandRipple : stopBandRipple);
        double dds = Math.log10(stopBandRipple <= passBandRipple ? stopBandRipple : passBandRipple);

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

        double dinf = ((t1 + t2 + a3) * dds) + (t3 + t4 + a6);
        double ff = b1 + b2 * (ddp - dds);
        double n = dinf / df - ff * df + 1.0;

        return (int)Math.ceil(n);
    }

    /**
     * Estimates band pass filter length;
     *
     * @param passBandStart frequency normalized to sample rate
     * @param passBandEnd frequency normalized to sample rate
     * @param passBandRippleDb
     * @param stopBandRippleDb
     */
    public static int estimateBandPassOrder(double sampleRate, int passBandStart, int passBandEnd,
                                            double passBandRippleDb, double stopBandRippleDb)
    {
        double df = (double)Math.abs(passBandEnd - passBandStart) / sampleRate;
        double ddp = Math.log10(passBandRippleDb);
        double dds = Math.log10(stopBandRippleDb);

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

        double cinf = dds * (t1 + t2 + a3) + t3 + t4 + a6;
        double ginf = -14.6f * (double)Math.log10(passBandRippleDb / stopBandRippleDb) - 16.9;
        double n = cinf / df + ginf * df + 1.0;

        return (int)Math.ceil(n);
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
        private Double mWeight;

        /**
         * Constructs a frequency band where the start and end frequencies are normalized to a
         * sample rate of 1.
         *
         * @param start edge of the frequency band (0.0 - 1.0)
         * @param end edge of the frequency band (0.0 - 1.0)
         * @param amplitude of the frequency band on the unity scale (0.0 - 1.0)
         * @param ripple of the frequency band specified as db ripple
         */
        public FrequencyBand(double start, double end, double amplitude, double ripple)
        {
            assert (0.0 <= mStart);
            assert (mStart <= mEnd);
            assert (mEnd <= 1.0);
            assert (0.0 <= amplitude && amplitude <= 1.0);
            assert (0.0 <= ripple);

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
        public FrequencyBand(double sampleRate, double start, double end, double amplitude, double ripple)
        {
            this(start / sampleRate, end / sampleRate,
                amplitude, ripple);
        }

        public FrequencyBand(double sampleRate, double start, double end, double amplitude, double ripple, double weight)
        {
            this(sampleRate, start, end, amplitude, ripple);
            mWeight = weight;
        }

        /**
         * Explicitly sets the weight of this frequency band.
         */
        public void setWeight(double weight)
        {
            mWeight = weight;
        }

        public int getGridSize()
        {
            return mGridSize;
        }

        public void setGridSize(int totalGridSize, double totalBandwidth)
        {
            mGridSize = Math.max(1, (int)Math.ceil((double)totalGridSize * (getBandWidth() / totalBandwidth)));
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("Band Start: ");
            sb.append(mStart);
            sb.append(" End:");
            sb.append(mEnd);
            sb.append(" Amplitude: ");
            sb.append(mAmplitude);
            sb.append(" Ripple dB: ");
            sb.append(mRippleDB);
            sb.append(" Ripple Amplitude: ");
            sb.append(DECIMAL_FORMATTER.format(getRippleAmplitude()));
            sb.append(" Grid Size: ");
            sb.append(mGridSize);
            sb.append(" Weight: ");
            sb.append(getWeight(mRippleDB));

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
            return (Math.pow(10.0, (mRippleDB / 20)) - 1)
                / (Math.pow(10.0, (mRippleDB / 20)) + 1);
        }

        /**
         * Calculates the weighting of this frequency band's ripple relative to the max ripple for a
         * filter specification.
         *
         * @param maxRippleAmplitude for the filter specification
         * @return weight of this frequency band
         */
        public double getWeight(double maxRippleAmplitude)
        {
            if(mWeight == null)
            {
                return 1.0 / (getRippleAmplitude() / maxRippleAmplitude);
            }

            return mWeight;
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

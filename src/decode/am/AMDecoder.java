package decode.am;

import sample.real.RealSampleListener;
import source.Source.SampleType;
import decode.Decoder;
import decode.DecoderType;
import dsp.agc.ComplexAutomaticGainControl;
import dsp.agc.RealAutomaticGainControl;
import dsp.am.AMDemodulator;
import dsp.filter.ComplexFIRFilter;
import dsp.filter.DCRemovalFilter2;
import dsp.filter.FilterFactory;
import dsp.filter.FloatFIRFilter;
import dsp.filter.Window.WindowType;

public class AMDecoder extends Decoder
{
    /**
     * This value determines how quickly the DC remove filter responds to 
     * changes in frequency.
     */
    private static final double DC_REMOVAL_RATIO = 0.003;

    private ComplexFIRFilter mIQFilter;
    private ComplexAutomaticGainControl mBasebandAGC = 
    							new ComplexAutomaticGainControl();
    private AMDemodulator mDemodulator = new AMDemodulator();
    private DCRemovalFilter2 mDCRemovalFilter = 
    						new DCRemovalFilter2( DC_REMOVAL_RATIO );
    private RealAutomaticGainControl mAudioAGC = new RealAutomaticGainControl();

    private FloatFIRFilter mLowPassFilter;
    
    public AMDecoder( SampleType sampleType )
    {
        super( sampleType );

        /**
         * Only setup a demod chain if we're receiving complex samples.  If
         * we're receiving demodulated samples, they'll be handled the same 
         * was as we handle the output of the demodulator.
         */
        if( mSourceSampleType == SampleType.COMPLEX )
        {
        	mIQFilter = new ComplexFIRFilter( 
    			FilterFactory.getLowPass( 48000, 6500, 73, WindowType.HAMMING ), 
                    1.0 );
            /**
             * The Decoder super class is both a Complex listener and a float
             * listener.  So, we add the demod to listen to the incoming 
             * quadrature samples, and we wire the output of the demod right
             * back to this class, so we can receive the demodulated output
             * to process
             */
            this.addComplexListener( mIQFilter );
            mIQFilter.setListener( mBasebandAGC );
            mBasebandAGC.setListener( mDemodulator );
            

            /**
             * Remove the DC component that is present when we're mistuned
             */
            mDemodulator.setListener( mDCRemovalFilter );
            
            /**
             * Route the demodulated, filtered samples back to this class to send
             * to all registered listeners
             */
            
            mLowPassFilter = new FloatFIRFilter( 
        		FilterFactory.getLowPass( 48000, 3000, 31, WindowType.COSINE ), 1.0 );
            
            mDCRemovalFilter.setListener( mLowPassFilter );
            
            mLowPassFilter.setListener( mAudioAGC );
            mAudioAGC.setListener( this.getRealReceiver() );
        }
    }

    @Override
    public DecoderType getType()
    {
        return DecoderType.AM;
    }

    @Override
    public void addUnfilteredRealSampleListener( RealSampleListener listener )
    {
    	throw new IllegalArgumentException( "cannot add real sample "
    			+ "listener to AM demodulator - not implemented" );
    }
}

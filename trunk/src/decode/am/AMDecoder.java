package decode.am;

import sample.Listener;
import source.Source.SampleType;
import decode.Decoder;
import decode.DecoderType;
import dsp.am.AMDemodulator;
import dsp.filter.DCRemovalFilter2;

public class AMDecoder extends Decoder
{
    /**
     * This value determines how quickly the DC remove filter responds to 
     * changes in frequency.
     */
    private static final double DC_REMOVAL_RATIO = 0.000003;

    private AMDemodulator mDemodulator;
    private DCRemovalFilter2 mDCRemovalFilter;
    
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
            /**
             * The Decoder super class is both a Complex listener and a float
             * listener.  So, we add the demod to listen to the incoming 
             * quadrature samples, and we wire the output of the demod right
             * back to this class, so we can receive the demodulated output
             * to process
             */
            mDemodulator = new AMDemodulator();
            
            this.addComplexListener( mDemodulator );

            /**
             * Remove the DC component that is present when we're mistuned
             */
            mDCRemovalFilter = new DCRemovalFilter2( DC_REMOVAL_RATIO );
            mDemodulator.addListener( mDCRemovalFilter );
            
            /**
             * Route the demodulated, filtered samples back to this class to send
             * to all registered listeners
             */
            mDCRemovalFilter.setListener( this.getFloatReceiver() );
        }
    }

    @Override
    public DecoderType getType()
    {
        return DecoderType.AM;
    }

    @Override
    public void addUnfilteredFloatListener( Listener<Float> listener )
    {
        mDemodulator.addListener( listener );
    }
}

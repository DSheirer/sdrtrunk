package dsp.am;

import sample.Listener;
import sample.complex.ComplexSample;
import sample.simplex.SimplexSampleBroadcaster;
import sample.simplex.SimplexSampleListener;
import dsp.filter.ComplexFIRFilter;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;

public class AMDemodulator implements Listener<ComplexSample>
{
    private SimplexSampleBroadcaster mBroadcaster = new SimplexSampleBroadcaster();
    private ComplexFIRFilter mIQFilter;

    public AMDemodulator()
    {
        this( FilterFactory.getLowPass( 48000, 3000, 73, WindowType.HAMMING ), 
                1000.0 );
    }
    
    public AMDemodulator( double[] iqFilter, double iqGain )
    {
        mIQFilter = new ComplexFIRFilter( iqFilter, iqGain );
        
        mIQFilter.setListener( new Demodulator() );
    }
    
    @Override
    public void receive( ComplexSample sample )
    {
        mIQFilter.receive( sample );
    }

    /**
     * Adds a listener to receive demodulated samples
     */
    public void addListener( SimplexSampleListener listener )
    {
        mBroadcaster.addListener( listener );
    }

    /**
     * Removes a listener from receiving demodulated samples
     */
    public void removeListener( SimplexSampleListener listener )
    {
        mBroadcaster.removeListener( listener );
    }

    public class Demodulator implements Listener<ComplexSample>
    {
        @Override
        public void receive( ComplexSample sample )
        {
            float demodulated = (float)Math.abs( 
                    Math.sqrt( ( sample.real() * sample.real() ) +
                               ( sample.imaginery() * sample.imaginery() ) ) ); 
            
            mBroadcaster.broadcast( demodulated * 30.0f );
        }
    }
}

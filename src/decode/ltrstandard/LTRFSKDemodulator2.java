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
package decode.ltrstandard;

import sample.Listener;
import dsp.BooleanZeroCrossingDetector;
import dsp.FloatToBinarySlicerLTR;
import dsp.SyncDetectListener;
import dsp.filter.BooleanAveragingFilter;
import dsp.filter.Filters;
import dsp.filter.FloatHalfBandFilter;
import dsp.filter.FloatHalfBandNoDecimateFilter;
import dsp.filter.SquaringFilter;

public class LTRFSKDemodulator2 implements Listener<Float>, SyncDetectListener
{
	FloatHalfBandFilter mHBFilter1;
	FloatHalfBandFilter mHBFilter2;
	FloatHalfBandFilter mHBFilter3;
	FloatHalfBandFilter mHBFilter4;
	FloatHalfBandFilter mHBFilter5;
	FloatHalfBandNoDecimateFilter mHBFilter6;

	SquaringFilter mSquaringFilter;
	BooleanAveragingFilter mAveragingFilter;
	BooleanZeroCrossingDetector mZeroCrossingDetector;
	FloatToBinarySlicerLTR mSlicer;
	
	/**
	 * Implements a Logic Trunked Radio sub-audible 300 baud FSK signaling 
	 * decoder.  Expects a 48k short-valued sample rate, which is decimated
	 * to a final 1.5k sample rate via five cascaded decimating half-band 
	 * filters, and applies a sixth, non-decimating half-band filter to bring 
	 * the overall bandwidth down to .75k.  Each half band filter requires
	 * 8 multiply/accumulate (MAC) cycles per input sample, totaling 2560
	 * MAC filtering cycles per output bit, to get from 48k input to 300-baud
	 * bit stream output.
	 * 
	 * Applies a balancer to the demodulated fsk waveform to normalize the 
	 * values about the zero-axis and to offset fluctuations induced by the 
	 * DC correction filter.
	 * 
	 * Converts filtered, decimated samples to squared bit values and then 
	 * applies a zero crossing detector to make a baud decision every 5 samples.
	 * 
	 * Outputs the decoded bit stream.
	 */
	public LTRFSKDemodulator2( int syncLength, int messageLength )
	{
		mHBFilter1 = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
		
		mHBFilter2 = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
		mHBFilter1.setListener( mHBFilter2 );
		
		mHBFilter3 = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
		mHBFilter2.setListener( mHBFilter3 );
		
		mHBFilter4 = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
		mHBFilter3.setListener( mHBFilter4 );

		mHBFilter5 = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
		mHBFilter4.setListener( mHBFilter5 );

		mHBFilter6 = new FloatHalfBandNoDecimateFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
		mHBFilter5.setListener( mHBFilter6 );

		mSlicer = new FloatToBinarySlicerLTR( syncLength, messageLength, 5 );
		mHBFilter6.setListener( mSlicer );

		mAveragingFilter = new BooleanAveragingFilter( 5 );
		mSlicer.setListener( mAveragingFilter );

		mZeroCrossingDetector = new BooleanZeroCrossingDetector( 5 );
		mAveragingFilter.setListener( mZeroCrossingDetector );
	}

	@Override
    public void receive( Float sample )
    {
		mHBFilter1.receive( sample );
    }

    public void setListener( Listener<Boolean> listener )
    {
		mZeroCrossingDetector.setListener( listener );
    }

    public void removeListener( Listener<Boolean> listener )
    {
		mZeroCrossingDetector.removeListener( listener );
    }

    /**
     * Pass the sync detection down to the slicer
     */
	@Override
    public void syncDetected()
    {
		mSlicer.syncDetected();
    }
}

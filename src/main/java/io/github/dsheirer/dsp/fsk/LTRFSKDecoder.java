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
package io.github.dsheirer.dsp.fsk;

import io.github.dsheirer.dsp.filter.Filters;
import io.github.dsheirer.dsp.filter.FloatHalfBandFilter;
import io.github.dsheirer.dsp.filter.FloatHalfBandNoDecimateFilter;
import io.github.dsheirer.dsp.filter.LTRPulseShapingFilter;
import io.github.dsheirer.dsp.filter.SquaringFilter;
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.dsp.filter.cic.RealPrimeCICDecimate;
import io.github.dsheirer.dsp.filter.dc.IIRSinglePoleDCRemovalFilter;
import io.github.dsheirer.dsp.symbol.Slicer;
import io.github.dsheirer.dsp.symbol.Slicer.Output;
import io.github.dsheirer.instrument.Instrumentable;
import io.github.dsheirer.instrument.tap.Tap;
import io.github.dsheirer.instrument.tap.TapGroup;
import io.github.dsheirer.instrument.tap.stream.BinaryTap;
import io.github.dsheirer.instrument.tap.stream.FloatBufferTap;
import io.github.dsheirer.instrument.tap.stream.FloatTap;
import io.github.dsheirer.instrument.tap.stream.SymbolEventTap;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.RealBuffer;

import java.util.ArrayList;
import java.util.List;

public class LTRFSKDecoder implements Listener<RealBuffer>, Instrumentable
{
	private RealPrimeCICDecimate mCICDecimator;
	
	private FloatHalfBandFilter mHBFilter1;
	private FloatHalfBandFilter mHBFilter2;
	private FloatHalfBandFilter mHBFilter3;
	private FloatHalfBandFilter mHBFilter4;
	private FloatHalfBandFilter mHBFilter5;
	private FloatHalfBandNoDecimateFilter mHBFilter6;
	private IIRSinglePoleDCRemovalFilter mDCFilter;
	private SquaringFilter mSquaringFilter;
	private LTRPulseShapingFilter mPulseShaper;
	private Slicer mSlicer;
	
	private List<TapGroup> mAvailableTaps;
	private final String TAP_F1_F2 = "LTR FSK Demod Filter1 >< Filter2";
	private final String TAP_F2_F3 = "LTR FSK Demod Filter2 >< Filter3";
	private final String TAP_F3_F4 = "LTR FSK Demod Filter3 >< Filter4";
	private final String TAP_F4_F5 = "LTR FSK Demod Filter4 >< Filter5";
	private final String TAP_F5_F6 = "LTR FSK Demod Filter5 >< Filter6";
	private final String TAP_F6_DT = "LTR FSK Demod Filter6 >< DC Filter";
	private final String TAP_CIC = "LTR FSK Demod CIC Decimator";
	private final String TAP_DT_SF1 = "LTR FSK Demod DC Filter >< Squaring Filter";
	private final String TAP_SF1_PS = "LTR FSK Demod Squaring Filter >< Pulse Shaper";
	private final String TAP_PS_SLICER = "LTR FSK Demod Pulse Shaper >< Slicer";
	private final String TAP_SYMBOL_EVENT = "LTR FSK Demod Symbol Event";
	
	/**
	 * Implements a Logic Trunked Radio sub-audible 300 baud FSK signaling 
	 * decoder.  Expects a 48000 sample rate input.
	 */
	public LTRFSKDecoder()
	{
		mCICDecimator = new RealPrimeCICDecimate( 32, 1, 375, 60, WindowType.HAMMING, 2 );
		
		mHBFilter1 = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.1002f );
		
		mHBFilter2 = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.1002f );
		mHBFilter1.setListener( mHBFilter2 );
		
		mHBFilter3 = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.1002f );
		mHBFilter2.setListener( mHBFilter3 );
		
		mHBFilter4 = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.1002f );
		mHBFilter3.setListener( mHBFilter4 );

		mHBFilter5 = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.1002f );
		mHBFilter4.setListener( mHBFilter5 );
		
		mHBFilter6 = new FloatHalfBandNoDecimateFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.1002f );
		mHBFilter5.setListener( mHBFilter6 );

		mDCFilter = new IIRSinglePoleDCRemovalFilter( 0.9946f );
		mHBFilter6.setListener( mDCFilter );
		
		mSquaringFilter = new SquaringFilter();
		mDCFilter.setListener( mSquaringFilter );
		
		mPulseShaper = new LTRPulseShapingFilter();
		mSquaringFilter.setListener( mPulseShaper );
		
		mSlicer = new Slicer( Output.NORMAL, 5 );
		mPulseShaper.setListener( mSlicer );
	}

	@Override
    public void receive( RealBuffer buffer )
    {
		for( float sample: buffer.getSamples() )
		{
			mHBFilter1.receive( sample );
		}
		
		mCICDecimator.receive(buffer);
    }

    public void addListener( Listener<Boolean> listener )
    {
		mSlicer.setListener( listener );
    }

    public void removeListener( Listener<Boolean> listener )
    {
		mSlicer.removeListener( listener );
    }

	@Override
    public List<TapGroup> getTapGroups()
    {
	    if( mAvailableTaps == null )
	    {
	    	mAvailableTaps = new ArrayList<TapGroup>();
	    	
	    	TapGroup group = new TapGroup( "LTR FSK Decoder" );
	    	
	    	group.add( new FloatTap( TAP_F1_F2, 0, 1.0f ) );
	    	group.add( new FloatTap( TAP_F2_F3, 8, 0.5f ) );
	    	group.add( new FloatTap( TAP_F3_F4, 12, 0.25f ) );
	    	group.add( new FloatTap( TAP_F4_F5, 14, 0.125f ) );
	    	group.add( new FloatTap( TAP_F5_F6, 15, 0.0625f ) );
	    	group.add( new FloatTap( TAP_F6_DT, 31, 0.0625f ) );
	    	group.add( new FloatBufferTap( TAP_CIC, 31, 0.0625f ) );
	    	group.add( new FloatTap( TAP_DT_SF1, 31, 0.0625f ) );
	    	group.add( new BinaryTap( TAP_SF1_PS, 33, 0.0625f ) );
	    	group.add( new BinaryTap( TAP_PS_SLICER, 33, 0.0625f ) );
	    	group.add( new SymbolEventTap( TAP_SYMBOL_EVENT, 7, 0.0125f ) );
	    	
	    	mAvailableTaps.add( group );
	    }
	    
	    return mAvailableTaps;
    }

	@Override
    public void registerTap( Tap tap )
    {
		switch( tap.getName() )
		{
			case TAP_CIC:
				mCICDecimator.setListener( (FloatBufferTap)tap );
				break;
			case TAP_F1_F2:
				mHBFilter1.setListener( (FloatTap)tap );
				((FloatTap)tap).setListener( mHBFilter2 );
				break;
			case TAP_F2_F3:
				mHBFilter2.setListener( (FloatTap)tap );
				((FloatTap)tap).setListener( mHBFilter3 );
				break;
			case TAP_F3_F4:
				mHBFilter3.setListener( (FloatTap)tap );
				((FloatTap)tap).setListener( mHBFilter4 );
				break;
			case TAP_F4_F5:
				mHBFilter4.setListener( (FloatTap)tap );
				((FloatTap)tap).setListener( mHBFilter5 );
				break;
			case TAP_F5_F6:
				mHBFilter5.setListener( (FloatTap)tap );
				((FloatTap)tap).setListener( mHBFilter6 );
				break;
			case TAP_F6_DT:
				mHBFilter6.setListener( (FloatTap)tap );
				((FloatTap)tap).setListener( mDCFilter );
				break;
			case TAP_DT_SF1:
				mDCFilter.setListener( (FloatTap)tap );
				((FloatTap)tap).setListener( mSquaringFilter );
				break;
			case TAP_SF1_PS:
				mSquaringFilter.setListener( (BinaryTap)tap );
				((BinaryTap)tap).setListener( mPulseShaper );
				break;
			case TAP_PS_SLICER:
				mPulseShaper.setListener( (BinaryTap)tap );
				((BinaryTap)tap).setListener( mSlicer );
				break;
			case TAP_SYMBOL_EVENT:
				mSlicer.addTap( (SymbolEventTap)tap );
				break;
		}
    }

	@Override
    public void unregisterTap( Tap tap )
    {
		switch( tap.getName() )
		{
			case TAP_CIC:
				mCICDecimator.removeListener();
				break;
			case TAP_F1_F2:
				mHBFilter1.setListener( mHBFilter2 );
				break;
			case TAP_F2_F3:
				mHBFilter2.setListener( mHBFilter3 );
				break;
			case TAP_F3_F4:
				mHBFilter3.setListener( mHBFilter4 );
				break;
			case TAP_F4_F5:
				mHBFilter4.setListener( mHBFilter5 );
				break;
			case TAP_F5_F6:
				mHBFilter5.setListener( mHBFilter6 );
				break;
			case TAP_F6_DT:
				mHBFilter6.setListener( mDCFilter );
				break;
			case TAP_DT_SF1:
				mDCFilter.setListener( mSquaringFilter );
				break;
			case TAP_SF1_PS:
				mSquaringFilter.setListener( mPulseShaper );
				break;
			case TAP_PS_SLICER:
				mPulseShaper.setListener( mSlicer );
				break;
			case TAP_SYMBOL_EVENT:
				mSlicer.removeTap();
				break;
		}
    }
}

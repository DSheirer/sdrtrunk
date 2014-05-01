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
package decode.passport;

import instrument.Instrumentable;
import instrument.tap.Tap;

import java.util.ArrayList;
import java.util.List;

import source.Source.SampleType;
import alias.AliasList;
import bits.MessageFramer;
import bits.SyncPattern;
import decode.Decoder;
import decode.DecoderType;
import dsp.filter.DCRemovalFilter2;
import dsp.fsk.LTRFSKDecoder;
import dsp.nbfm.FilteringNBFMDemodulator;

public class PassportDecoder extends Decoder implements Instrumentable
{
	/**
	 * This value determines how quickly the DC remove filter responds to 
	 * changes in frequency.
	 */
	private static final double sDC_REMOVAL_RATIO = 0.000003;

	public static final int sPASSPORT_MESSAGE_LENGTH = 68;
	public static final int sPASSPORT_SYNC_LENGTH = 9;
	private FilteringNBFMDemodulator mNBFMDemodulator;
	private DCRemovalFilter2 mDCRemovalFilter; 
	private LTRFSKDecoder mPassportFSKDecoder;
	private MessageFramer mPassportMessageFramer;
	private PassportMessageProcessor mPassportMessageProcessor;
	
    private List<Tap> mAvailableTaps;

	public PassportDecoder( SampleType sampleType, AliasList aliasList )
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
			 * The Decoder super class is both a Complex listener and a Short
			 * listener.  So, we add the demod to listen to the incoming 
			 * quadrature samples, and we wire the output of the demod right
			 * back to this class, so we can receive the demodulated output
			 * to process
			 */
			mNBFMDemodulator = new FilteringNBFMDemodulator();
			this.addComplexListener( mNBFMDemodulator );

			/**
			 * Remove the DC component that is present when we're mistuned
			 */
			mDCRemovalFilter = new DCRemovalFilter2( sDC_REMOVAL_RATIO );
			mNBFMDemodulator.addListener( mDCRemovalFilter );
			mDCRemovalFilter.setListener( this.getFloatReceiver() );
		}

		mPassportFSKDecoder = 
				new LTRFSKDecoder();

		/**
		 * The DC removal filter will impact performance of the LTRFSKDemod,
		 * so we get the samples straight from the NBFM demod
		 */
		if( mSourceSampleType == SampleType.COMPLEX )
		{
			mNBFMDemodulator.addListener( mPassportFSKDecoder );
		}
		else
		{
			addFloatListener( mPassportFSKDecoder );
		}
		

		mPassportMessageFramer = 
				new MessageFramer( SyncPattern.PASSPORT.getPattern(),
						sPASSPORT_MESSAGE_LENGTH );

		mPassportFSKDecoder.addListener( mPassportMessageFramer );

		mPassportMessageProcessor = new PassportMessageProcessor( aliasList );
		mPassportMessageFramer.addMessageListener( mPassportMessageProcessor );
		mPassportMessageProcessor.addMessageListener( this );
	}

	@Override
    public DecoderType getType()
    {
	    return DecoderType.PASSPORT;
    }

	@Override
    public List<Tap> getTaps()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<Tap>();
			
			mAvailableTaps.addAll( mPassportFSKDecoder.getTaps() );
		}

		return mAvailableTaps;
    }

	@Override
    public void addTap( Tap tap )
    {
		mPassportFSKDecoder.addTap( tap );
    }

	@Override
    public void removeTap( Tap tap )
    {
		mPassportFSKDecoder.removeTap( tap );
    }
}

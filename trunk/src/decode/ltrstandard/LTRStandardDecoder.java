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

import message.MessageDirection;
import source.Source.SampleType;
import alias.AliasList;
import bits.MessageFramer;
import bits.SyncPattern;
import decode.Decoder;
import decode.DecoderType;
import dsp.filter.DCRemovalFilter2;
import dsp.fsk.LTRFSKDecoder;
import dsp.nbfm.FilteringNBFMDemodulator;

public class LTRStandardDecoder extends Decoder
{
	/**
	 * This value determines how quickly the DC remove filter responds to 
	 * changes in frequency.
	 */
	private static final double sDC_REMOVAL_RATIO = 0.000003;

	public static final int sLTR_STANDARD_MESSAGE_LENGTH = 40;
	private FilteringNBFMDemodulator mDemodulator;
	private DCRemovalFilter2 mDCRemovalFilter; 
	private LTRFSKDecoder mLTRFSKDecoder;
	private MessageFramer mLTRMessageFramer;
	private LTRStandardMessageProcessor mLTRMessageProcessor;

	public LTRStandardDecoder( SampleType sampleType, 
					   AliasList aliasList,
					   MessageDirection direction )
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
			mDemodulator = new FilteringNBFMDemodulator();
			this.addComplexListener( mDemodulator );

			/**
			 * Remove the DC component that is present when we're mistuned
			 */
			mDCRemovalFilter = new DCRemovalFilter2( sDC_REMOVAL_RATIO );
			mDemodulator.addListener( mDCRemovalFilter );
			
			mDCRemovalFilter.setListener( this.getFloatReceiver() );
		}

		mLTRFSKDecoder = new LTRFSKDecoder();

		/**
		 * The DC removal filter will impact performance of the LTRFSKDemod,
		 * so we get the samples straight from the NBFM demod
		 */
		if( mSourceSampleType == SampleType.COMPLEX )
		{
			mDemodulator.addListener( mLTRFSKDecoder );
		}
		else
		{
			this.addFloatListener( mLTRFSKDecoder );
		}
		
		if( direction == MessageDirection.OSW )
		{
			mLTRMessageFramer = 
					new MessageFramer( SyncPattern.LTR_STANDARD_OSW.getPattern(),
							sLTR_STANDARD_MESSAGE_LENGTH );
		}
		else
		{
			mLTRMessageFramer = 
					new MessageFramer( SyncPattern.LTR_STANDARD_ISW.getPattern(),
							sLTR_STANDARD_MESSAGE_LENGTH );
		}
		
		mLTRFSKDecoder.addListener( mLTRMessageFramer );

		mLTRMessageProcessor = new LTRStandardMessageProcessor( direction, aliasList );
		mLTRMessageFramer.addMessageListener( mLTRMessageProcessor );
		mLTRMessageProcessor.addMessageListener( this );
	}

	@Override
    public DecoderType getType()
    {
	    return DecoderType.LTR_STANDARD;
    }
}

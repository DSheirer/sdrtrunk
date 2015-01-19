package dsp.fsk;

import java.util.ArrayList;

import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import alias.AliasList;
import bits.BitSetBuffer;
import bits.BitSetFullException;
import bits.SyncPatternMatcher;
import decode.p25.P25Interleave;
import decode.p25.Trellis_1_2_Rate;
import decode.p25.Trellis_3_4_Rate;
import decode.p25.message.P25Message;
import decode.p25.message.hdu.HDUMessage;
import decode.p25.message.ldu.LDU1Message;
import decode.p25.message.ldu.LDU2Message;
import decode.p25.message.ldu.lc.LDULCMessageFactory;
import decode.p25.message.pdu.PDUConfirmedMessage;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.message.pdu.PDUMessageFactory;
import decode.p25.message.tdu.TDUMessage;
import decode.p25.message.tdu.lc.TDULCMessageFactory;
import decode.p25.message.tdu.lc.TDULinkControlMessage;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.message.tsbk.TSBKMessageFactory;
import decode.p25.reference.DataUnitID;
import edac.BCH_63_16_11;
import edac.CRC;
import edac.CRCP25;

public class P25MessageFramer implements Listener<Dibit>
{
	private final static Logger mLog = 
							LoggerFactory.getLogger( P25MessageFramer.class );
	
	private ArrayList<P25MessageAssembler> mAssemblers =
			new ArrayList<P25MessageAssembler>();

	public static final int TSBK_BEGIN = 64;
	public static final int TSBK_CRC_START = 144;
	public static final int TSBK_END = 260;
	public static final int TSBK_DECODED_END = 160;

	public static final int PDU0_BEGIN = 64;
	public static final int PDU0_CRC_BEGIN = 144;
	public static final int PDU0_END = 260;
	public static final int PDU1_BEGIN = 160;
	public static final int PDU1_END = 356;
	public static final int PDU2_BEGIN = 256;
	public static final int PDU2_END = 452;
	public static final int PDU3_BEGIN = 352;
	public static final int PDU3_END = 548;
	public static final int PDU3_DECODED_END = 448;
	
	private Listener<Message> mListener;
	private AliasList mAliasList;
	private SyncPatternMatcher mMatcher;
	private boolean mInverted = false;
	private Trellis_1_2_Rate mHalfRate = new Trellis_1_2_Rate();
	private Trellis_3_4_Rate mThreeQuarterRate = new Trellis_3_4_Rate();
	private BCH_63_16_11 mNIDDecoder = new BCH_63_16_11();
	
	/**
	 * Constructs a C4FM message framer to receive a stream of C4FM symbols and
	 * detect the sync pattern then capture the following stream of symbols up
	 * to the message length, and then broadcast that bit buffer to the registered
	 * listener.
	 * 
	 * @param sync - sync pattern (maximum of 63 bits)
	 * @param messageLength - in bits
	 * @param inverted - optional flag to indicate the symbol stream should be
	 * uninverted prior to processing 
	 */
	public P25MessageFramer( long sync, 
	                         int messageLength, 
	                         boolean inverted,
	                         AliasList aliasList )
	{
		mMatcher = new SyncPatternMatcher( sync );
		mInverted = inverted;
		mAliasList = aliasList;

		/**
		 * We use two message assemblers to catch any sync detections, so that
		 * if we have a false trigger then we still have a chance to catch the 
		 * valid message. If we have two false triggers before the arrival of 
		 * the actual message sync, then the third or subsequent real or false
		 * sync pattern will produce a debug message indicating no assemblers
		 * are available..
		 */
		mAssemblers.add( new P25MessageAssembler() );
		mAssemblers.add( new P25MessageAssembler() );
	}
	
	private void dispatch( Message message )
	{
		if( mListener != null )
		{
			mListener.receive( message );
		}
	}
	
	@Override
    public void receive( Dibit symbol )
    {
    	mMatcher.receive( symbol.getBit1() );
    	mMatcher.receive( symbol.getBit2() );

    	for( P25MessageAssembler assembler: mAssemblers )
    	{
    		if( assembler.isActive() )
    		{
        		assembler.receive( symbol );
        		
        		if( assembler.complete() )
        		{
        			assembler.reset();
        		}
    		}
    	}
    	
        /* Check for sync match and activate a message assembler, if we can
         * find an inactive assembler.  Otherwise, ignore and log the issue */
    	if( mMatcher.matches() )
    	{
    		boolean found = false;
    		
        	for( P25MessageAssembler assembler: mAssemblers )
        	{
        		if( !assembler.isActive() )
        		{
        			assembler.setActive( true );
        			found = true;
        			break;
        		}
        	}
        	
        	if( !found )
        	{
            	mLog.debug( "no inactive P25 message assemblers available" );
        	}
    	}
    }

    public void setListener( Listener<Message> listener )
    {
		mListener = listener;
    }

    public void removeListener( Listener<Message> listener )
    {
		mListener = null;
    }
	
    private class P25MessageAssembler
    {
    	/* Starting position of the status symbol counter is 24 symbols to 
    	 * account for the 48-bit sync pattern which is not included in message */
    	private int mStatusSymbolPointer = 24;
    	private BitSetBuffer mMessage;
        private int mMessageLength;
        private boolean mComplete = false;
        private boolean mActive = false;
        private DataUnitID mDUID = DataUnitID.NID;
        
        public P25MessageAssembler()
        {
        	mMessageLength = mDUID.getMessageLength();
            mMessage = new BitSetBuffer( mMessageLength );
        	reset();
        }
        
        public void receive( Dibit dibit )
        {
        	if( mActive )
        	{
        		if( mStatusSymbolPointer == 35 )
        		{
        			mStatusSymbolPointer = 0;
        		}
        		else
        		{
        			mStatusSymbolPointer++;

                    try
                    {
                        mMessage.add( dibit.getBit1() );
                        mMessage.add( dibit.getBit2() );
                    }
                    catch( BitSetFullException e )
                    {
                         mComplete = true;
                    }
         
                    /* Check the message for complete */
                    if( mMessage.isFull() )
                    {
                    	checkComplete();
                    }
        		}
        	}
        }

        public void reset()
        {
        	mDUID = DataUnitID.NID;
        	mMessage.setSize( mDUID.getMessageLength() );
        	mMessage.clear();
        	mStatusSymbolPointer = 24;
            mComplete = false;
            mActive = false;
        }
        
        public void setActive( boolean active )
        {
        	mActive = active;
        }

        private void setDUID( DataUnitID id )
        {
        	mDUID = id;
        	mMessageLength = id.getMessageLength();
        	mMessage.setSize( mMessageLength );
        }

        private void checkComplete()
        {
        	switch( mDUID )
        	{
				case NID:
					CRC crc = mNIDDecoder.correctNID( mMessage );
					
					if( crc != CRC.FAILED_CRC )
					{
						int value = mMessage.getInt( P25Message.DUID );
						
						DataUnitID duid = DataUnitID.fromValue( value );
						
						if( duid != DataUnitID.UNKN )
						{
							setDUID( duid );
						}
						else
						{
							mComplete = true;
						}
					}
					else
					{
						mComplete = true;
					}
					break;
				case HDU:
					mComplete = true;
                    dispatch( new HDUMessage( mMessage.copy(), mDUID, mAliasList ) );
					break;
				case LDU1:
					mComplete = true;
					
					LDU1Message ldu1 = new LDU1Message( mMessage.copy(), 
							mDUID, mAliasList );

					/* Convert the LDU1 message into a link control LDU1 message */
                    dispatch( LDULCMessageFactory.getMessage( ldu1 ) );
					break;
				case LDU2:
					mComplete = true;
                    dispatch( new LDU2Message( mMessage.copy(), mDUID, mAliasList ) );
					break;
				case PDU0:
					/* Remove interleaving */
					P25Interleave.deinterleaveData( mMessage, PDU0_BEGIN, PDU0_END );
	
					/* Remove trellis encoding - abort processing if we have an
					 * unsuccessful decode due to excessive errors */
					if( mHalfRate.decode( mMessage, PDU0_BEGIN, PDU0_END ) )
					{
						CRC pduCRC = CRCP25.correctCCITT80( mMessage, 
								PDU0_BEGIN, PDU0_CRC_BEGIN );
						

						if( pduCRC != CRC.FAILED_CRC )
						{
							boolean confirmed = mMessage.get( 
									PDUMessage.CONFIRMATION_REQUIRED_INDICATOR );
							
							if( confirmed )
							{
								setDUID( DataUnitID.PDUC );
							}
							else
							{
								setDUID( DataUnitID.PDU1 );
							}
							
							mMessage.setPointer( PDU1_BEGIN );
						}
						else
						{
							mComplete = true;
						}
					}
					else
					{
						mComplete = true;
					}
					break;
				case PDU1:
					/* Remove interleaving */
					P25Interleave.deinterleaveData( mMessage, PDU1_BEGIN, PDU1_END );
	
					/* Remove trellis encoding - abort processing if we have an
					 * unsuccessful decode due to excessive errors */
					if( mHalfRate.decode( mMessage, PDU1_BEGIN, PDU1_END ) )
					{
						if( mMessage.getInt( PDUMessage.BLOCKS_TO_FOLLOW ) == 1 )
						{
							mMessage.setSize( PDU2_BEGIN );
							
		                    PDUMessage pduMessage1 = PDUMessageFactory.getMessage( 
		                    		mMessage.copy(), DataUnitID.PDU1, mAliasList ); 

		                    dispatch( pduMessage1 );
							
							mComplete = true;
						}
						else
						{
							setDUID( DataUnitID.PDU2 );
							
							mMessage.setPointer( PDU2_BEGIN );
						}
					}
					else
					{
						mComplete = true;
					}
					break;
				case PDU2:
					/* Remove interleaving */
					P25Interleave.deinterleaveData( mMessage, PDU2_BEGIN, PDU2_END );
	
					/* Remove trellis encoding - abort processing if we have an
					 * unsuccessful decode due to excessive errors */
					if( mHalfRate.decode( mMessage, PDU2_BEGIN, PDU2_END ) )
					{
						if( mMessage.getInt( PDUMessage.BLOCKS_TO_FOLLOW ) == 2 )
						{
							mMessage.setSize( PDU3_BEGIN );
							
		                    PDUMessage pduMessage2 = PDUMessageFactory.getMessage( 
		                    		mMessage.copy(), DataUnitID.PDU2, mAliasList ); 

		                    dispatch( pduMessage2 );
							
							mComplete = true;
						}
						else
						{
							setDUID( DataUnitID.PDU3 );
							
							mMessage.setPointer( PDU3_BEGIN );
						}
					}
					else
					{
						mComplete = true;
					}
					break;
				case PDU3:
					/* Remove interleaving */
					P25Interleave.deinterleaveData( mMessage, PDU3_BEGIN, PDU3_END );
	
					/* Remove trellis encoding - abort processing if we have an
					 * unsuccessful decode due to excessive errors */
					if( mHalfRate.decode( mMessage, PDU3_BEGIN, PDU3_END ) )
					{
						mMessage.setSize( PDU3_DECODED_END );
						
	                    PDUMessage pduMessage3 = PDUMessageFactory.getMessage( 
	                    		mMessage.copy(), DataUnitID.PDU3, mAliasList ); 

	                    dispatch( pduMessage3 );
					}
					
					mComplete = true;
					break;
				case PDUC:
					/* De-interleave the latest block*/
					P25Interleave.deinterleaveData( mMessage, 
							mMessage.size() - 196, mMessage.size() );

					/* Decode 3/4 rate convolutional encoding from latest block */
					if( mThreeQuarterRate.decode( mMessage, 
							mMessage.size() - 196, mMessage.size() ) )
					{
						/* Resize the message and adjust the message pointer
						 * to account for removing 48 + 4 parity bits */
						mMessage.setSize( mMessage.size() - 52 );
						mMessage.adjustPointer( -52 );
						
						int blocks = mMessage.getInt( PDUMessage.BLOCKS_TO_FOLLOW );
						
						int current = ( mMessage.size() - 160 ) / 144;
						
						if( current < blocks )
						{
							mMessage.setSize( mMessage.size() + 196 );
							mMessageLength = mMessage.size();
						}
						else
						{
							dispatch( new PDUConfirmedMessage( mMessage.copy(), 
									mDUID, mAliasList ) );
							
							mComplete = true;
						}
						break;
					}
					else
					{
						dispatch( new PDUConfirmedMessage( mMessage.copy(), 
								mDUID, mAliasList ) );
						
						mComplete = true;
					}
					break;
				case TDU:
                    dispatch( new TDUMessage( mMessage.copy(), mDUID, mAliasList ) );
					mComplete = true;
					break;
				case TDULC:
					TDULinkControlMessage tdulc =  new TDULinkControlMessage( 
							mMessage.copy(), mDUID, mAliasList );

					/* Convert to an appropriate link control message */
					tdulc = TDULCMessageFactory.getMessage( tdulc );

					dispatch( tdulc );
					mComplete = true;
					break;
				case TSBK1:
					/* Remove interleaving */
					P25Interleave.deinterleaveData( mMessage, TSBK_BEGIN, TSBK_END );
	
					/* Remove trellis encoding - abort processing if we have an
					 * unsuccessful decode due to excessive errors */
					
					if( mHalfRate.decode( mMessage, TSBK_BEGIN, TSBK_END ) )
					{
						CRC tsbkCRC = CRCP25.correctCCITT80( mMessage, 
								TSBK_BEGIN, TSBK_CRC_START );

						if( tsbkCRC != CRC.FAILED_CRC )
						{
							BitSetBuffer tsbkBuffer1 = mMessage.copy();
							tsbkBuffer1.setSize( TSBK_DECODED_END );
							
		                    TSBKMessage tsbkMessage1 = TSBKMessageFactory.getMessage( 
		                            tsbkBuffer1, DataUnitID.TSBK1, mAliasList ); 

							if( tsbkMessage1.isLastBlock() )
							{
								mComplete = true;
							}
							else
							{
								setDUID( DataUnitID.TSBK2 );
								mMessage.setPointer( TSBK_BEGIN );
							}
							
							dispatch( tsbkMessage1 );
						}
						else
						{
							mComplete = true;
						}
					}
					else
					{
						mComplete = true;
					}
					break;
				case TSBK2:
					/* Remove interleaving */
					P25Interleave.deinterleaveData( mMessage, TSBK_BEGIN, TSBK_END );

					/* Remove trellis encoding - abort processing if we have an
					 * unsuccessful decode due to excessive errors */
					if( mHalfRate.decode( mMessage, TSBK_BEGIN, TSBK_END ) )
					{
						CRC tsbkCRC = CRCP25.correctCCITT80( mMessage, 
								TSBK_BEGIN, TSBK_CRC_START );
						
						if( tsbkCRC != CRC.FAILED_CRC )
						{
							BitSetBuffer tsbkBuffer2 = mMessage.copy();
							tsbkBuffer2.setSize( TSBK_DECODED_END );
							
		                    TSBKMessage tsbkMessage2 = TSBKMessageFactory.getMessage( 
		                            tsbkBuffer2, DataUnitID.TSBK2, mAliasList ); 

							if( tsbkMessage2.isLastBlock() )
							{
								mComplete = true;
							}
							else
							{
								setDUID( DataUnitID.TSBK3 );
								mMessage.setPointer( TSBK_BEGIN );
							}
							
							dispatch( tsbkMessage2 );
						}
						else
						{
							mComplete = true;
						}
					}
					else
					{
						mComplete = true;
					}
					break;
				case TSBK3:
					/* Remove interleaving */
					P25Interleave.deinterleaveData( mMessage, TSBK_BEGIN, TSBK_END );
	
					/* Remove trellis encoding - abort processing if we have an
					 * unsuccessful decode due to excessive errors */
					if( mHalfRate.decode( mMessage, TSBK_BEGIN, TSBK_END ) )
					{
						CRC tsbkCRC = CRCP25.correctCCITT80( mMessage, 
								TSBK_BEGIN, TSBK_CRC_START );
						
						if( tsbkCRC != CRC.FAILED_CRC )
						{
		                    BitSetBuffer tsbkBuffer3 = mMessage.copy();
							tsbkBuffer3.setSize( TSBK_DECODED_END );
		                    
		                    TSBKMessage tsbkMessage3 = TSBKMessageFactory.getMessage( 
		                            tsbkBuffer3, DataUnitID.TSBK3, mAliasList ); 

							dispatch( tsbkMessage3 );
						}
					}

					mComplete = true;
					break;
				case UNKN:
					mComplete = true;
                    dispatch( new P25Message( mMessage.copy(), mDUID, mAliasList ) );
					break;
				default:
					mComplete = true;
					break;
        	}
        }
        
        public void dispose()
        {
        	mMessage = null;
        	mHalfRate.dispose();
        }

        /**
         * Flag to indicate when this assembler has received all of the bits it
         * is looking for (ie message length), and should then be removed from
         * receiving any more bits
         */
        public boolean complete()
        {
            return mComplete;
        }
        
        public boolean isActive()
        {
        	return mActive;
        }
    }
}

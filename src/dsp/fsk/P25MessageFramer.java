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
import decode.p25.TrellisHalfRate;
import decode.p25.message.P25Message;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.message.pdu.PDUMessageFactory;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.message.tsbk.TSBKMessageFactory;
import decode.p25.reference.DataUnitID;

public class P25MessageFramer implements Listener<Dibit>
{
	private final static Logger mLog = 
							LoggerFactory.getLogger( P25MessageFramer.class );
	
	private ArrayList<P25MessageAssembler> mAssemblers =
			new ArrayList<P25MessageAssembler>();

	public static final int TSBK_BEGIN = 64;
	public static final int TSBK_END = 260;
	public static final int TSBK_DECODED_END = 160;

	public static final int PDU0_BEGIN = 64;
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
	private TrellisHalfRate mHalfRate = new TrellisHalfRate();
	
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
					break;
				case HDU:
					mComplete = true;
                    dispatch( new P25Message( mMessage.copy(), mDUID, mAliasList ) );
					break;
				case LDU1:
					mComplete = true;
                    dispatch( new P25Message( mMessage.copy(), mDUID, mAliasList ) );
					break;
				case LDU2:
					mComplete = true;
                    dispatch( new P25Message( mMessage.copy(), mDUID, mAliasList ) );
					break;
				case PDU0:
					/* Remove interleaving */
					P25Interleave.deinterleave( mMessage, PDU0_BEGIN, PDU0_END );
	
					/* Remove trellis encoding */
					mHalfRate.decode( mMessage, PDU0_BEGIN, PDU0_END );

					setDUID( DataUnitID.PDU1 );
					
					mMessage.setPointer( PDU1_BEGIN );
					break;
				case PDU1:
					/* Remove interleaving */
					P25Interleave.deinterleave( mMessage, PDU1_BEGIN, PDU1_END );
	
					/* Remove trellis encoding */
					mHalfRate.decode( mMessage, PDU1_BEGIN, PDU1_END );

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
					break;
				case PDU2:
					/* Remove interleaving */
					P25Interleave.deinterleave( mMessage, PDU2_BEGIN, PDU2_END );
	
					/* Remove trellis encoding */
					mHalfRate.decode( mMessage, PDU2_BEGIN, PDU2_END );

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
					break;
				case PDU3:
					/* Remove interleaving */
					P25Interleave.deinterleave( mMessage, PDU3_BEGIN, PDU3_END );
	
					/* Remove trellis encoding */
					mHalfRate.decode( mMessage, PDU3_BEGIN, PDU3_END );
					
					mMessage.setSize( PDU3_DECODED_END );
					
                    PDUMessage pduMessage3 = PDUMessageFactory.getMessage( 
                    		mMessage.copy(), DataUnitID.PDU3, mAliasList ); 

                    dispatch( pduMessage3 );
					
					mComplete = true;
					break;
				case TDU:
					mComplete = true;
                    dispatch( new P25Message( mMessage.copy(), mDUID, mAliasList ) );
					break;
				case TDULC:
					mComplete = true;
                    dispatch( new P25Message( mMessage.copy(), mDUID, mAliasList ) );
					break;
				case TSBK1:
					/* Remove interleaving */
					P25Interleave.deinterleave( mMessage, TSBK_BEGIN, TSBK_END );
	
					/* Remove trellis encoding */
					mHalfRate.decode( mMessage, TSBK_BEGIN, TSBK_END );

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
					break;
				case TSBK2:
					/* Remove interleaving */
					P25Interleave.deinterleave( mMessage, TSBK_BEGIN, TSBK_END );

					/* Remove trellis encoding */
					mHalfRate.decode( mMessage, TSBK_BEGIN, TSBK_END );

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
					
					break;
				case TSBK3:
					/* Remove interleaving */
					P25Interleave.deinterleave( mMessage, TSBK_BEGIN, TSBK_END );
	
					/* Remove trellis encoding */
					mHalfRate.decode( mMessage, TSBK_BEGIN, TSBK_END );

                    BitSetBuffer tsbkBuffer3 = mMessage.copy();
					tsbkBuffer3.setSize( TSBK_DECODED_END );
                    
                    TSBKMessage tsbkMessage3 = TSBKMessageFactory.getMessage( 
                            tsbkBuffer3, DataUnitID.TSBK3, mAliasList ); 

					mComplete = true;

					dispatch( tsbkMessage3 );
					
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

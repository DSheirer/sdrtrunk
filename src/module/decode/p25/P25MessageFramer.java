package module.decode.p25;

import java.util.ArrayList;

import message.Message;
import module.decode.p25.message.P25Message;
import module.decode.p25.message.hdu.HDUMessage;
import module.decode.p25.message.ldu.LDU1Message;
import module.decode.p25.message.ldu.LDU2Message;
import module.decode.p25.message.ldu.lc.LDULCMessageFactory;
import module.decode.p25.message.pdu.PDUMessage;
import module.decode.p25.message.pdu.PDUMessageFactory;
import module.decode.p25.message.pdu.confirmed.PDUConfirmedMessage;
import module.decode.p25.message.pdu.confirmed.PDUConfirmedMessageFactory;
import module.decode.p25.message.tdu.TDUMessage;
import module.decode.p25.message.tdu.lc.TDULCMessageFactory;
import module.decode.p25.message.tdu.lc.TDULinkControlMessage;
import module.decode.p25.message.tsbk.TSBKMessage;
import module.decode.p25.message.tsbk.TSBKMessageFactory;
import module.decode.p25.message.vselp.VSELP1Message;
import module.decode.p25.message.vselp.VSELP2Message;
import module.decode.p25.reference.DataUnitID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import alias.AliasList;
import bits.BinaryMessage;
import bits.BitSetFullException;
import bits.ISyncDetectListener;
import bits.MultiSyncPatternMatcher;
import bits.SoftSyncDetector;
import bits.SyncDetector;
import dsp.psk.LSMDemodulator;
import dsp.symbol.Dibit;
import dsp.symbol.FrameSync;
import edac.BCH_63_16_11;
import edac.CRC;
import edac.CRCP25;

public class P25MessageFramer implements Listener<Dibit>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( P25MessageFramer.class );

	/* Determines the threshold for sync pattern soft matching */
	private static final int SYNC_MATCH_THRESHOLD = 2;
	private static final int SYNC_IN_CALL_THRESHOLD = 4;

	/* Costas Loop phase lock error correction values.  A phase lock error of
	 * 90 degrees requires a correction of 1/4 of the symbol rate (1200Hz).  An 
	 * error of 180 degrees requires a correction of 1/2 of the symbol rate */
	public static final double SYMBOL_FREQUENCY = 2.0d * Math.PI * 4800.0d / 48000.0d;
	public static final double PHASE_CORRECTION_90_DEGREES = SYMBOL_FREQUENCY / 4.0d; 
	public static final double PHASE_CORRECTION_180_DEGREES = SYMBOL_FREQUENCY / 2.0d; 
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
	
	private SoftSyncDetector mPrimarySyncDetector = new SoftSyncDetector( 
			FrameSync.P25_PHASE1_NORMAL.getSync(), SYNC_MATCH_THRESHOLD );

	private MultiSyncPatternMatcher mMatcher = new MultiSyncPatternMatcher( 48 ); 
	private ArrayList<P25MessageAssembler> mAssemblers =
						new ArrayList<P25MessageAssembler>();

	private Listener<Message> mListener;
	private AliasList mAliasList;

	private Trellis_1_2_Rate mHalfRate = new Trellis_1_2_Rate();
	private Trellis_3_4_Rate mThreeQuarterRate = new Trellis_3_4_Rate();
	private BCH_63_16_11 mNIDDecoder = new BCH_63_16_11();
	
	/**
	 * Constructs a P25 message framer to receive a stream of symbols and
	 * detect the sync pattern then capture the following stream of symbols up
	 * to the message length, and then broadcast that bit buffer to the registered
	 * listener.
	 */
	public P25MessageFramer( AliasList aliasList )
	{
		mAliasList = aliasList;
		
		mPrimarySyncDetector.setListener( new ISyncDetectListener()
		{
			@Override
			public void syncDetected()
			{
	        	for( P25MessageAssembler assembler: mAssemblers )
	        	{
	        		if( !assembler.isActive() )
	        		{
	        			assembler.setActive( true );
	        			break;
	        		}
	        	}
			}
		} );
		
		mMatcher.add( mPrimarySyncDetector );

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
	
	public P25MessageFramer( AliasList aliasList, LSMDemodulator demodulator )
	{
		this( aliasList );

		if( demodulator != null )
		{
			/* For CQPSK, we include 3 additional sync detectors to watch for and
			 * correct +/-90 and 180 degree costas loop phase lock errors */
			mMatcher.add( new CostasPhaseErrorDetector( FrameSync.P25_PHASE1_ERROR_90_CCW, 
					demodulator, PHASE_CORRECTION_90_DEGREES  ) );

			mMatcher.add( new CostasPhaseErrorDetector( FrameSync.P25_PHASE1_ERROR_90_CW, 
					demodulator, -PHASE_CORRECTION_90_DEGREES  ) );

			mMatcher.add( new CostasPhaseErrorDetector( FrameSync.P25_PHASE1_ERROR_180, 
					demodulator, PHASE_CORRECTION_180_DEGREES  ) );
		}
	}
	
	public void dispose()
	{
		for( P25MessageAssembler a: mAssemblers )
		{
			a.dispose();
		}
		
		mAssemblers.clear();

		mListener = null;
		mAliasList = null;
		
		mMatcher.dispose();
		mMatcher = null;
		
		mPrimarySyncDetector.dispose();
		mPrimarySyncDetector = null;
		
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
    	
		mMatcher.receive( symbol.getBit1(), symbol.getBit2() );
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
    	private BinaryMessage mMessage;
        private int mMessageLength;
        private boolean mComplete = false;
        private boolean mActive = false;
        private DataUnitID mDUID = DataUnitID.NID;
        
        public P25MessageAssembler()
        {
        	mMessageLength = mDUID.getMessageLength();
            mMessage = new BinaryMessage( mMessageLength );
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
					mMessage = mNIDDecoder.correctNID( mMessage );
					
					if( mMessage.getCRC() != CRC.FAILED_CRC )
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
		                    dispatch( new P25Message( mMessage.copy(), mDUID, mAliasList ) );
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
                    
                    /* We're in a call now, lower the sync match threshold */
                    mPrimarySyncDetector.setThreshold( SYNC_IN_CALL_THRESHOLD );
					break;
				case LDU1:
					mComplete = true;
					
					LDU1Message ldu1 = new LDU1Message( mMessage.copy(), 
							mDUID, mAliasList );

					/* Convert the LDU1 message into a link control LDU1 message */
                    dispatch( LDULCMessageFactory.getMessage( ldu1 ) );

                    /* We're in a call now, lower the sync match threshold */
                    mPrimarySyncDetector.setThreshold( SYNC_IN_CALL_THRESHOLD );
					break;
				case LDU2:
					mComplete = true;
                    dispatch( new LDU2Message( mMessage.copy(), mDUID, mAliasList ) );

                    /* We're in a call now, lower the sync match threshold */
                    mPrimarySyncDetector.setThreshold( SYNC_IN_CALL_THRESHOLD );
					break;
				case PDU0:

					/* Remove interleaving */
					P25Interleave.deinterleaveData( mMessage, PDU0_BEGIN, PDU0_END );
	
					/* Remove trellis encoding - abort processing if we have an
					 * unsuccessful decode due to excessive errors */
					if( mHalfRate.decode( mMessage, PDU0_BEGIN, PDU0_END ) )
					{
						mMessage = CRCP25.correctCCITT80( mMessage, 
								PDU0_BEGIN, PDU0_CRC_BEGIN );
						

						if( mMessage.getCRC() != CRC.FAILED_CRC )
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
					
                    /* Set sync match threshold to normal */
                    mPrimarySyncDetector.setThreshold( SYNC_MATCH_THRESHOLD );
                    
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

					/* Set sync match threshold to normal */
                    mPrimarySyncDetector.setThreshold( SYNC_MATCH_THRESHOLD );
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
							PDUConfirmedMessage pducm = 
								new PDUConfirmedMessage( mMessage.copy(), 
									mAliasList );

							/* Translate into correct subclass */
							dispatch( PDUConfirmedMessageFactory.getMessage( pducm ) );
							
							mComplete = true;
						}
						break;
					}
					else
					{
						PDUConfirmedMessage pducm = 
								new PDUConfirmedMessage( mMessage.copy(), 
									mAliasList );

						/* Translate into correct subclass */
						dispatch( PDUConfirmedMessageFactory.getMessage( pducm ) );
						
						mComplete = true;
					}
					
                    /* Set sync match threshold to normal */
                    mPrimarySyncDetector.setThreshold( SYNC_MATCH_THRESHOLD );
					break;
				case TDU:
                    dispatch( new TDUMessage( mMessage.copy(), mDUID, mAliasList ) );
					mComplete = true;

					/* Set sync match threshold to normal */
                    mPrimarySyncDetector.setThreshold( SYNC_MATCH_THRESHOLD );
					break;
				case TDULC:
					TDULinkControlMessage tdulc =  new TDULinkControlMessage( 
							mMessage.copy(), mDUID, mAliasList );

					/* Convert to an appropriate link control message */
					tdulc = TDULCMessageFactory.getMessage( tdulc );

					dispatch( tdulc );
					mComplete = true;

					/* Set sync match threshold to normal */
                    mPrimarySyncDetector.setThreshold( SYNC_MATCH_THRESHOLD );
					break;
				case TSBK1:
					/* Remove interleaving */
					P25Interleave.deinterleaveData( mMessage, TSBK_BEGIN, TSBK_END );
	
					/* Remove trellis encoding - abort processing if we have an
					 * unsuccessful decode due to excessive errors */
					
					if( mHalfRate.decode( mMessage, TSBK_BEGIN, TSBK_END ) )
					{
						mMessage = CRCP25.correctCCITT80( mMessage, 
								TSBK_BEGIN, TSBK_CRC_START );

						if( mMessage.getCRC() != CRC.FAILED_CRC )
						{
							BinaryMessage tsbkBuffer1 = mMessage.copy();
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

					/* Set sync match threshold to normal */
                    mPrimarySyncDetector.setThreshold( SYNC_MATCH_THRESHOLD );
					break;
				case TSBK2:
					/* Remove interleaving */
					P25Interleave.deinterleaveData( mMessage, TSBK_BEGIN, TSBK_END );

					/* Remove trellis encoding - abort processing if we have an
					 * unsuccessful decode due to excessive errors */
					if( mHalfRate.decode( mMessage, TSBK_BEGIN, TSBK_END ) )
					{
						mMessage = CRCP25.correctCCITT80( mMessage, 
								TSBK_BEGIN, TSBK_CRC_START );
						
						if( mMessage.getCRC() != CRC.FAILED_CRC )
						{
							BinaryMessage tsbkBuffer2 = mMessage.copy();
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
						mMessage = CRCP25.correctCCITT80( mMessage, 
								TSBK_BEGIN, TSBK_CRC_START );
						
						if( mMessage.getCRC() != CRC.FAILED_CRC )
						{
		                    BinaryMessage tsbkBuffer3 = mMessage.copy();
							tsbkBuffer3.setSize( TSBK_DECODED_END );
		                    
		                    TSBKMessage tsbkMessage3 = TSBKMessageFactory.getMessage( 
		                            tsbkBuffer3, DataUnitID.TSBK3, mAliasList ); 

							dispatch( tsbkMessage3 );
						}
					}

					mComplete = true;
					break;
				case VSELP1:
					mComplete = true;
                    dispatch( new VSELP1Message( mMessage.copy(), mDUID, mAliasList ) );
					break;
				case VSELP2:
					mComplete = true;
                    dispatch( new VSELP2Message( mMessage.copy(), mDUID, mAliasList ) );
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
    
    /**
     * Sync pattern detector to listen for costas loop phase lock errors and 
     * apply a phase correction to the costas loop so that we don't miss any 
     * messages.
     * 
     * When the costas loop locks with a +/- 90 degree or 180 degree phase 
     * error, the slicer will incorrectly apply the symbol pattern rotated left
     * or right by the phase error.  However, we can detect these rotated sync
     * patterns and apply immediate phase correction so that message processing
     * can continue. 
     */
    public class CostasPhaseErrorDetector extends SyncDetector
    {
    	private LSMDemodulator mDemodulator;
    	private double mCorrection;
    	
    	public CostasPhaseErrorDetector( FrameSync frameSync, LSMDemodulator demodulator, double correction )
    	{
    		super( frameSync.getSync() );

    		mDemodulator = demodulator;
    		
    		mCorrection = correction;

    		setListener( new ISyncDetectListener()
			{
				@Override
				public void syncDetected()
				{
					mDemodulator.correctPhaseError( mCorrection );

					/* Since we detected a sync pattern, start a message assembler */
		        	for( P25MessageAssembler assembler: mAssemblers )
		        	{
		        		if( !assembler.isActive() )
		        		{
		        			assembler.setActive( true );
		        			break;
		        		}
		        	}
				}
			} );
    	}
    }
}

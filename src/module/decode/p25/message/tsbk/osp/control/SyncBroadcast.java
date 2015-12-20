package module.decode.p25.message.tsbk.osp.control;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import module.decode.p25.message.tsbk.TSBKMessage;
import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;
import alias.AliasList;
import bits.BinaryMessage;

/**
 * Sync Broadcast - used to broadcast FDMA-TDMA synchronization timestamp so
 * that a subscriber unit that is time synchronized on an FDMA control channel
 * is also synchronized with a TDMA traffic channel.
 */
public class SyncBroadcast extends TSBKMessage
{
	public static final int US = 92;
	public static final int IST = 93;
	public static final int MMU = 94;
	public static final int[] MC = { 95,96 };
	public static final int VL = 97;
	public static final int LOCAL_TIME_OFFSET_SIGN = 98;
	public static final int[] LOCAL_TIME_OFFSET = { 99,100,101,102 };
	public static final int LOCAL_TIME_OFFSET_HALF = 103;
	
	public static final int[] YEAR = { 104,105,106,107,108,109,110 };
	public static final int[] MONTH = { 111,112,113,114 };
	public static final int[] DAY = { 115,116,117,118,119 };
	public static final int[] HOURS = { 120,121,122,123,124 };
	public static final int[] MINUTES = { 125,126,127,128,129,130 };
	public static final int[] MICRO_SLOTS = { 131,132,133,134,135,136,137,138,
		139,140,141,142,143 };
	public static final int[] TSBK_CRC = { 144,145,146,147,148,149,150,151,152,
		153,154,155,156,157,158,159 };
    
    public SyncBroadcast( BinaryMessage message, 
    					  DataUnitID duid,
    					  AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.TDMA_SYNC_BROADCAST.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " " + new Date( getTime() ).toString() );
        sb.append( " TIME ZONE OFFSET:" + getTimeZone().toUpperCase() );
        sb.append( " US:" + isUS() );
        sb.append( " IST:" + isIST() );
        sb.append( " MMU:" + isMMU() );
        sb.append( " VL:" + isVL() );
        
        return sb.toString();
    }

    /**
     * Returns the time sync in java time (ie milliseconds since epoch )
     */
    public long getTime()
    {
    	Calendar cal = new GregorianCalendar();
    	cal.clear();
    	cal.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
    	cal.set( Calendar.YEAR, getYear() );    	
    	cal.set( Calendar.MONTH, getMonth() - 1 );
    	cal.set( Calendar.DAY_OF_MONTH, getDay() );
    	cal.set( Calendar.HOUR_OF_DAY, getHours() );
    	cal.set( Calendar.MINUTE, getMinutes() );
    	cal.set( Calendar.MILLISECOND, getMilliSeconds() );

    	return cal.getTimeInMillis();
    }
    
    public boolean isUS()
    {
    	return mMessage.get( US );
    }

    /**
     * Daylight Savings Time
     */
    public boolean isIST()
    {
    	return mMessage.get( IST );
    }

    /**
     * No idea ...
     */
    public boolean isMMU()
    {
    	return mMessage.get( MMU );
    }

    /**
     * Millenium ...?
     */
    public int getMC()
    {
    	return mMessage.getInt( MC );
    }

    /**
     * No idea ....
     */
    public boolean isVL()
    {
    	return mMessage.get( VL );
    }

    /**
     * Calculates the time zone from the local time offset field.  
     */
    public String getTimeZone()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( mMessage.get( LOCAL_TIME_OFFSET_SIGN ) ? "-" : "+" );
    	
    	int hours = mMessage.getInt( LOCAL_TIME_OFFSET );
		sb.append( ( hours < 10 ? "0" : "" ) + String.valueOf( hours ) );

		sb.append( ":" );
		
		sb.append( mMessage.get( LOCAL_TIME_OFFSET_HALF ) ? "30" : "00" );
		
		if( mMessage.get( IST ) )
		{
			sb.append( " DST" );
		}
		
		return sb.toString();
    }

    public int getYear()
    {
    	return 2000 + mMessage.getInt( YEAR );
    }
    
    public int getMonth()
    {
    	return mMessage.getInt( MONTH );
    }
    
    public int getDay()
    {
    	return mMessage.getInt( DAY );
    }
    
    public int getHours()
    {
    	return mMessage.getInt( HOURS );
    }
    
    public int getMinutes()
    {
    	return mMessage.getInt( MINUTES );
    }
    
    public int getMilliSeconds()
    {
    	return (int)( (double)getMicroSlots() * 7.5 );
    }

    /**
     * Micro Slots - each uslot = 7.5 milliseconds.  The value ranges 0 - 7999
     * and represents 60,000 milliseconds ( 8000 x 7.5 ms).  Superframes occur
     * every 360 ms (48 micro-slots) on FDMA and Ultraframes occur every 4
     * super frames on TDMA.
     */
    public int getMicroSlots()
    {
    	return mMessage.getInt( MICRO_SLOTS );
    }
}

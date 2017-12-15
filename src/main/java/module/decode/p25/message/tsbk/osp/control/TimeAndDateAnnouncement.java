package module.decode.p25.message.tsbk.osp.control;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import module.decode.p25.message.tsbk.TSBKMessage;
import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;
import alias.AliasList;
import bits.BinaryMessage;

public class TimeAndDateAnnouncement extends TSBKMessage
{
	public static final int VALID_DATE_INDICATOR = 80;
	public static final int VALID_TIME_INDICATOR = 81;
	public static final int VALID_LOCAL_TIME_OFFSET_INDICATOR = 82;

	public static final int LOCAL_TIME_OFFSET_SIGN = 84;
    public static final int[] LOCAL_TIME_OFFSET = { 85,86,87,88,89,90,91,92,
    	93,94,95 };
    public static final int[] MONTH = { 96,97,98,99 };
    public static final int[] DAY = { 100,101,102,103 };
    public static final int[] YEAR = { 105,106,107,108,109,110,111,112,113,114,
    	115,116,117 };
    public static final int[] HOURS = { 120,121,122,123,124 };
    public static final int[] MINUTES = { 125,126,127,128,129,130 };
    public static final int[] SECONDS = { 131,132,133,134,135,136 };
    
    private SimpleDateFormat mTimeFormatter = 
    		new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss Z" );

	public TimeAndDateAnnouncement( BinaryMessage message, DataUnitID duid,
								AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.TIME_DATE_ANNOUNCEMENT.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        if( hasValidDate() )
        {
        	sb.append( " DATE-TIME:" );
        }

        sb.append( " " );
        sb.append( mTimeFormatter.format( new Date( getDateTimestamp() ) ) );
        
        return sb.toString();
    }
    
    public long getDateTimestamp()
    {
    	Calendar cal = new GregorianCalendar( getTimeZone() );

    	if( hasValidDate() )
    	{
        	cal.set( Calendar.YEAR, getYear() );
        	cal.set( Calendar.MONTH, getMonth() );
        	cal.set( Calendar.DAY_OF_MONTH, getDay() );
    	}
    	
    	if( hasValidTime() )
    	{
    		cal.set( Calendar.HOUR, getHour() );
    		cal.set( Calendar.MINUTE, getMinute() );
    		cal.set( Calendar.SECOND, getSecond() );
    	}
    	
    	return cal.getTimeInMillis();
    }
    
    public boolean hasValidDate()
	{
		return mMessage.get( VALID_DATE_INDICATOR );
	}
	
	public boolean hasValidTime()
	{
		return mMessage.get( VALID_TIME_INDICATOR );
	}

	public TimeZone getTimeZone()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "GMT " );
		
		sb.append( mMessage.get( LOCAL_TIME_OFFSET_SIGN ) ? "-" : "+" );

		int offsetMinutes = getLocalTimeOffset();
		
		sb.append( (int)( offsetMinutes / 60 )  );
		
		sb.append( " : " );
		
		sb.append( offsetMinutes % 60 );
		
		return TimeZone.getTimeZone( sb.toString() );
	}
	
	public boolean hasValidLocalTimeOffset()
	{
		return mMessage.get( VALID_LOCAL_TIME_OFFSET_INDICATOR );
	}

	/**
	 * Local Time Offset in minutes
	 */
	public int getLocalTimeOffset()
	{
		return mMessage.getInt( LOCAL_TIME_OFFSET );
	}

	public int getMonth()
	{
		return mMessage.getInt( MONTH );
	}
	
	public int getDay()
	{
		return mMessage.getInt( DAY );
	}
	
	public int getYear()
	{
		return mMessage.getInt( YEAR );
	}
	
	public int getHour()
	{
		return mMessage.getInt( HOURS );
	}
	
	public int getMinute()
	{
		return mMessage.getInt( MINUTES );
	}
	
	public int getSecond()
	{
		return mMessage.getInt( SECONDS );
	}
}

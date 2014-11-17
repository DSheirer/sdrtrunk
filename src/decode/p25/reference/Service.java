package decode.p25.reference;

import java.util.ArrayList;
import java.util.List;

public enum Service
{
	EXTENDED_SERVICES( 0x800000l ),
	EXTENDED_SERVICES_EXTENSION( 0x400000l ),
	
	/* NORMAL SERVICES */
	NETWORK_ACTIVE( 0x200000l ),
	RESERVED_4( 0x100000l ),
	GROUP_VOICE( 0x080000l ),
	INDIVIDUAL_VOICE( 0x040000l ),
	PSTN_TO_UNIT_VOICE( 0x020000l ),
	UNIT_TO_PSTN_VOICE( 0x010000l ),
	RESERVED_9( 0x008000l ),
	GROUP_DATA( 0x004000l ),
	INDIVIDUAL_DATA( 0x002000l ),
	RESERVED_12( 0x001000l ),
	UNIT_REGISTRATION( 0x000800l ),
	GROUP_AFFILIATION( 0x000400l ),
	GROUP_AFFILIATION_QUERY( 0x000200l ),
	AUTHENTICATION( 0x000100l ),
	ENCRYPTION_SETTINGS( 0x000080l ),
	USER_STATUS( 0x000040l ),
	USER_MESSAGE( 0x000020l ),
	UNIT_STATUS( 0x000010l ),
	USER_STATUS_QUERY( 0x000008l ),
	UNIT_STATUS_QUERY( 0x000004l ),
	UNIT_PAGE( 0x000002l ),
	EMERGENCY_ALARM( 0x000001l ),
	UNKNOWN( 0x0l );
	
	private long mCode;
	
	private Service( long code )
	{
		mCode = code;
	}
	
	public long getCode()
	{
		return mCode;
	}

	public static boolean isSupported( Service service, long serviceBitmap )
	{
		return ( service.getCode() & serviceBitmap ) == service.getCode();
	}
	
	public static List<Service> getServices( long serviceBitmap )
	{
		List<Service> services = new ArrayList<Service>();
		
		for( Service service: values() )
		{
			if( isSupported( service, serviceBitmap ) )
			{
				services.add( service );
			}
		}
	
		return services;
	}
}

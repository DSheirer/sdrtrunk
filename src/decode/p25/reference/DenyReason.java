package decode.p25.reference;

public enum DenyReason
{
    RESERVED( 0x00 ),
    REQUESTING_UNIT_NOT_VALID( 0x10 ),
    REQUESTING_UNIT_NOT_AUTHORIZED_FOR_SERVICE( 0x11 ),
    TARGET_UNIT_NOT_VALID( 0x20 ),
    TARGET_UNIT_NOT_AUTHORIZED_FOR_SERVICE( 0x21 ),
    TARGET_UNIT_REFUSED_CALL( 0x2F ),
    TARGET_GROUP_NOT_VALID( 0x30 ),
    TARGET_GROUP_NOT_AUTHORIZED_FOR_SERVICE( 0x31 ),
    INVALID_DIALING( 0x40 ),
    TELEPHONE_NUMBER_NOT_AUTHORIZED( 0x41 ),
    PSTN_NOT_VALID( 0x42 ),
    CALL_TIMEOUT( 0x50 ),
    LANDLINE_TERMINATED_CALL( 0x51 ),
    SUBSCRIBER_UNIT_TERMINATED_CALL( 0x52 ),
    CALL_PREEMPTED( 0x5F ),
    SITE_ACCESS_DENIAL( 0x60 ),
    USER_OR_SYSTEM_DEFINED( 0x61 ),
    CALL_OPTIONS_NOT_VALID_FOR_SERVICE( 0xF0 ),
    PROTECTION_SERVICE_OPTION_NOT_VALID( 0xF1 ),
    DUPLEX_SERVICE_OPTION_NOT_VALID( 0xF2 ),
    CIRCUIT_OR_PACKET_MODE_OPTION_NOT_VALID( 0xF3 ),
    SYSTEM_DOES_NOT_SUPPORT_SERVICE( 0xFF ),
    UNKNOWN( -1 );
    
    private int mCode;
    
    private DenyReason( int code )
    {
        mCode = code;
    }
    
    public static DenyReason fromCode( int code )
    {
        if( code == 0x10 )
        {
            return DenyReason.REQUESTING_UNIT_NOT_VALID;
        }
        else if( code == 0x11 )
        {
            return REQUESTING_UNIT_NOT_AUTHORIZED_FOR_SERVICE;
        }
        else if( code == 0x20 )
        {
            return DenyReason.TARGET_UNIT_NOT_VALID;
        }
        else if( code == 0x21 )
        {
            return TARGET_UNIT_NOT_AUTHORIZED_FOR_SERVICE;
        }
        else if( code == 0x2F )
        {
            return DenyReason.TARGET_UNIT_REFUSED_CALL;
        }
        else if( code == 0x30 )
        {
            return TARGET_GROUP_NOT_VALID;
        }
        else if( code == 0x31 )
        {
            return DenyReason.TARGET_GROUP_NOT_AUTHORIZED_FOR_SERVICE;
        }
        else if( code == 0x40 )
        {
            return DenyReason.INVALID_DIALING;
        }
        else if( code == 0x41 )
        {
            return DenyReason.TELEPHONE_NUMBER_NOT_AUTHORIZED;
        }
        else if( code == 0x42 )
        {
            return DenyReason.PSTN_NOT_VALID;
        }
        else if( code == 0x50 )
        {
            return CALL_TIMEOUT;
        }
        else if( code == 0x51 )
        {
            return LANDLINE_TERMINATED_CALL;
        }
        else if( code == 0x52 )
        {
            return SUBSCRIBER_UNIT_TERMINATED_CALL;
        }
        else if( code == 0x5F )
        {
            return DenyReason.CALL_PREEMPTED;
        }
        else if( code == 0x60 )
        {
            return SITE_ACCESS_DENIAL;
        }
        else if( code == 0xF0 )
        {
            return CALL_OPTIONS_NOT_VALID_FOR_SERVICE;
        }
        else if( code == 0xF1 )
        {
            return PROTECTION_SERVICE_OPTION_NOT_VALID;
        }
        else if( code == 0xF2 )
        {
            return DenyReason.DUPLEX_SERVICE_OPTION_NOT_VALID;
        }
        else if( code == 0xF3 )
        {
            return CIRCUIT_OR_PACKET_MODE_OPTION_NOT_VALID;
        }
        else if( code <= 0x5E )
        {
            return DenyReason.RESERVED;
        }
        else if( code >= 0x61 )
        {
            return DenyReason.USER_OR_SYSTEM_DEFINED;
        }
        
        return UNKNOWN;
    }
}

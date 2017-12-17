package io.github.dsheirer.module.decode.p25.reference;

public enum QueuedResponseReason
{
    RESERVED( 0x00 ),
    REQUESTING_UNIT_BUSY_OTHER_SERVICE( 0x10 ),
    TARGET_UNIT_BUSY_OTHER_SERVICE( 0x20 ),
    TARGET_UNIT_QUEUED_THIS_CALL( 0x2F ),
    TARGET_GROUP_CURRENTLY_ACTIVE( 0x30 ),
    CHANNEL_RESOURCES_UNAVAILABLE( 0x40 ),
    TELEPHONE_RESOURCES_UNAVAILABLE( 0x41 ),
    DATA_RESOURCES_UNAVAILABLE( 0x42 ),
    SUPERSEDING_SERVICE_CURRENTLY_ACTIVE( 0x50 ),
    USER_OR_SYSTEM_DEFINED( 0x80 ),
    UNKNOWN( -1 );
    
    private int mCode;
    
    private QueuedResponseReason( int code )
    {
        mCode = code;
    }
    
    public static QueuedResponseReason fromCode( int code )
    {
        if( code == 0x10 )
        {
            return QueuedResponseReason.REQUESTING_UNIT_BUSY_OTHER_SERVICE;
        }
        else if( code == 0x20 )
        {
            return QueuedResponseReason.TARGET_UNIT_BUSY_OTHER_SERVICE;
        }
        else if( code == 0x2F )
        {
            return QueuedResponseReason.TARGET_UNIT_QUEUED_THIS_CALL;
        }
        else if( code == 0x30 )
        {
            return QueuedResponseReason.TARGET_GROUP_CURRENTLY_ACTIVE;
        }
        else if( code == 0x40 )
        {
            return QueuedResponseReason.CHANNEL_RESOURCES_UNAVAILABLE;
        }
        else if( code == 0x41 )
        {
            return QueuedResponseReason.TELEPHONE_RESOURCES_UNAVAILABLE;
        }
        else if( code == 0x42 )
        {
            return QueuedResponseReason.DATA_RESOURCES_UNAVAILABLE;
        }
        else if( code == 0x50 )
        {
            return QueuedResponseReason.SUPERSEDING_SERVICE_CURRENTLY_ACTIVE;
        }
        else if( code <= 0x7F )
        {
            return QueuedResponseReason.RESERVED;
        }
        else if( code >= 0x80 )
        {
            return QueuedResponseReason.USER_OR_SYSTEM_DEFINED;
        }
        
        return UNKNOWN;
    }
}

package module.decode.p25.message.tsbk.osp.control;

public class SystemService
{
    public static boolean isCompositeControlChannel( int service )
    {
        return ( service & 0x01 ) == 0x01;
    }
    
    public static boolean isUpdateControlChannelOnly( int service )
    {
        return ( service & 0x02 ) == 0x02;
    }
    
    public static boolean isBackupControlChannelOnly( int service )
    {
        return ( service & 0x04 ) == 0x04;
    }
    
    public static boolean providesDataServiceRequests( int service )
    {
        return ( service & 0x10 ) == 0x10;
    }
    
    public static boolean providesVoiceServiceRequests( int service )
    {
        return ( service & 0x20 ) == 0x20;
    }
    
    public static boolean providesRegistrationServices( int service )
    {
        return ( service & 0x40 ) == 0x40;
    }
    
    public static boolean providesAuthenticationServices( int service )
    {
        return ( service & 0x80 ) == 0x80;
    }

    public static String toString( int service )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "CONTROL CHAN[" );
        
        if( isCompositeControlChannel( service ) )
        {
            sb.append( " COMPOSITE" );
        }
        
        if( isUpdateControlChannelOnly( service ) )
        {
            sb.append( " UPDATE" );
        }
        
        if( isBackupControlChannelOnly( service ) )
        {
            sb.append( " BACKUP" );
        }
        
        sb.append( " ] SERVICES[" );
        
        if( providesAuthenticationServices( service ) )
        {
            sb.append( " AUTHENTICATION" );
        }
        
        if( providesDataServiceRequests( service ) )
        {
            sb.append( " DATA" );
        }
        
        if( providesRegistrationServices( service ) )
        {
            sb.append( " REGISTRATION" );
        }
        
        if( providesVoiceServiceRequests( service ) )
        {
            sb.append( " VOICE" );
        }

        sb.append( " ]" );
        
        return sb.toString();
    }
}

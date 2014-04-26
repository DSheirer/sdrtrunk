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
package log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import properties.SystemProperties;
import util.TimeStamp;

public class Log
{
	private static Writer mLogFile;
	
	public Log()
	{
	}

	private static void log( String msg )
	{
		log( msg, null);
	}
	
	/**
	 * Logs just the raw message, without a preceeding timestamp
	 */
	public void logRaw( String rawmsg )
	{
		try
        {
	        mLogFile.write( rawmsg );
        }
        catch ( IOException e )
        {
    		System.out.println( 
    				"Couldn't write warning msg to log file: " + rawmsg );
        }
	}
	
	private static void log( String msg, String label)
	{
        if( mLogFile != null )
    	{
        	try 
        	{
    			mLogFile.write( TimeStamp.getTimeStamp( " " ) );
    			mLogFile.write( " " );

    			if( label != null )
        		{
        			mLogFile.write( label + ":" );
        		}
        		
				mLogFile.write( msg + "\n" );
				mLogFile.flush();
				
			} 
        	catch (IOException e) 
        	{
        		System.out.println( 
        				"Couldn't write warning msg to log file: " + msg );
			}
    	}

        //echo to console
		System.out.println( ( label == null ? "" : label + ": " ) + msg );
	}
	
	public static void header( String name )
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "********** " );
		sb.append( name );
		sb.append( " **********" );
		
		log( sb.toString() );
	}
	
    public static void info( String msg )
    {
    	log( msg );
    }
    
    public static void info( byte[] bytes )
    {
    	StringBuilder sb = new StringBuilder();
    	
    	for( int x = 0; x < bytes.length; x++ )
    	{
    		sb.append( String.format( "%02X ", bytes[ x ] ) );
    	}
    	
    	info( sb.toString() );
    }
    
    public static void info( String header, byte[] bytes )
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( header );
    	
    	for( int x = 0; x < bytes.length; x++ )
    	{
    		sb.append( String.format( "%02X ", bytes[ x ] ) );
    	}
    	
    	info( sb.toString() );
    }
    
    public static void warning( String msg )
    {
    	log( msg, "WARNING" );
    }
    
    public static void error( String msg )
    {
    	log( msg, "ERROR" );
    }
    
    public static void debug( String msg )
    {
    	log( msg, "DEBUG" );
    }

    /**
     * Initializes logging to the specified filename
     */
    public static void init( String filename )
    {
    	try 
    	{
			mLogFile = new OutputStreamWriter(
					new FileOutputStream( filename ) );
			
			Log.info( "SDRTrunk Multi-Channel Trunking Decoder Application Log" );
			Log.info( "Log - application logging started [" + filename + "]" );
			
			logUserEnvironment();
		} 
    	catch (FileNotFoundException e) 
    	{
			System.out.println("Couldn't open log file:" + filename );
		}    	
    }

    /**
     * Initializes the log file using system properties settings
     */
    public static void init()
    {
    	SystemProperties props = SystemProperties.getInstance();
    	
    	String logBaseFileName = props.get( "log.basefilename", 
    										"_SDRTrunk_application" );
    	
    	Path logPath = props.getApplicationFolder( "logs" );
    	
    	if( !Files.exists( logPath ) )
    	{
    		try
            {
	            Files.createDirectory( logPath );
            }
            catch ( IOException e )
            {
            	Log.error( "Log - couldn't create 'logs' subdirectory in the " +
            			"SDRTrunk application directory" );
            }
    	}
    	
    	String logFileName = TimeStamp.getTimeStamp( "_" ) + logBaseFileName +
    			".log";

    	Path logFile = logPath.resolve( logFileName );
    	
    	init( logFile.toString() );
    }
    
    public static void logUserEnvironment()
    {
    	Log.header( "User Environment" );
    	Log.info( "OS:\t\t\t" + System.getProperty( "os.name" ) );
    	Log.info( "OS Version:\t\t" + System.getProperty( "os.version" ) );
    	Log.info( "OS Arch:\t\t" + System.getProperty( "os.arch" ) );
    	Log.info( "Java Vendor:\t\t" + System.getProperty( "java.vendor" ) );
    	Log.info( "Java Vendor URL:\t" + System.getProperty( "java.vendor.url" ) );
    	Log.info( "Java Version:\t\t" + System.getProperty( "java.version" ) );
    	Log.info( "User:\t\t\t" + System.getProperty( "user.name" ) );
    	Log.info( "User Home:\t\t" + System.getProperty( "user.home" ) );
    	Log.info( "User Directory:\t\t" + System.getProperty( "user.dir" ) );
    	
    	Log.header( "Java Class Path" );
    	Log.info( System.getProperty( "java.class.path" ) );
    	
    }
    
    public static void close()
    {
    	if( mLogFile != null )
    	{
    		try
    		{
    	    	mLogFile.flush();
    	    	mLogFile.close();
    		}
    		catch( Exception e )
    		{
    			System.out.println( "Couldn't close log file" );
    		}
    	}
    }
}

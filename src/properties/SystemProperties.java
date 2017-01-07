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
package properties;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SystemProperties - provides an isolated instance of properties for the
 * application
 */

public class SystemProperties
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( SystemProperties.class );

	private static String sDEFAULT_APP_ROOT = "SDRTrunk";
	private static String sPROPERTIES_FILENAME = "SDRTrunk.properties";
	
	private static SystemProperties mInstance;
	private static Properties mProperties;
	private Path mPropertiesPath;
	private String mApplicationName;
	
	private SystemProperties()
	{
		mProperties = new Properties();
	}
	
	/**
	 * Returns a SINGLETON instance of the application properties set
	 * @return
	 */
	public static SystemProperties getInstance()
	{
		if( mInstance == null )
		{
			mInstance = new SystemProperties();
		}
		
		return mInstance;
	}

	/**
	 * Saves any currently changed settings to the application properties file
	 */
	public void save()
	{
		Path propsPath = 
				getApplicationRootPath().resolve( sPROPERTIES_FILENAME );

		OutputStream out = null;
		
		try
		{
			out = new FileOutputStream( propsPath.toString() );

			String comments = 
					"SDRTrunk - SDR Trunking Decoder Application Settings";
			
			mProperties.store( out, comments );
		}
		catch( Exception e )
		{
			mLog.error( "SystemProperties - exception while saving " +
					"application properties", e );
		}
		finally
		{
			if( out != null )
			{
				try
                {
	                out.close();
                }
                catch ( IOException e )
                {
                }
			}
		}
	}

	/**
	 * Application root directory.  Normally returns "SDRTRunk" from the user's
	 * home directory, unless that has been changed to another location on the 
	 * file system by the user.
	 */
	public Path getApplicationRootPath()
	{
		Path retVal = null;
		
		String root = get( "root.directory", sDEFAULT_APP_ROOT );
		
		if( root.equalsIgnoreCase( sDEFAULT_APP_ROOT ) )
		{
			retVal = Paths.get( 
					System.getProperty( "user.home" ), sDEFAULT_APP_ROOT );
		}
		else
		{
			retVal = Paths.get( root );
		}
		
		return retVal;
	}
	
	public Path getApplicationFolder( String folder )
	{
		Path retVal = getApplicationRootPath().resolve( folder );
		
		if( !Files.exists( retVal ) )
		{
			try
            {
	            Files.createDirectory( retVal );
            }
            catch ( IOException e )
            {
            	mLog.error( "SystemProperties - exception while creating " +
            			"app folder [" + folder + "]", e );
            }
		}
		
		return retVal;
	}
	
	public void logCurrentSettings()
	{
		if( mPropertiesPath == null )
		{
			mLog.info( "SystemProperties - no properties file loaded - using defaults" );
		}
		else
		{
			mLog.info( "SystemProperties - application properties loaded [" + mPropertiesPath.toString() + "]" );
		}
	}
	
	/**
	 * Loads the properties file into this properties set
	 * @param file
	 */
	public void load( Path propertiesPath )
	{
		if( propertiesPath != null )
		{
			mPropertiesPath = propertiesPath;
			
			InputStream in = null;
			
            try
            {
	            in = new FileInputStream( propertiesPath.toString() );
            }
            catch ( FileNotFoundException e )
            {
            	mLog.error( "SDRTrunk - exception while opening inputstream on " +
    			"application properties file", e  );
            }

			if( in != null )
			{
				try
                {
	                mProperties.load( in );
                }
                catch ( IOException e )
                {
                	mLog.error( "SDRTrunk - exception while loading properties " +
                			"inputstream into SystemProperties", e );
                }
				finally
				{
					try
                    {
	                    in.close();
                    }
                    catch ( IOException e )
                    {
                    }
				}
			}
		}
		
		mLog.info( "SystemProperties - loaded [" + 
						propertiesPath.toString() + "]" );
	}

	public String getApplicationName()
	{
		if(mApplicationName == null)
		{
			StringBuilder sb = new StringBuilder();

			sb.append( "sdrtrunk" );

			try(BufferedReader reader = new BufferedReader(
					new InputStreamReader( this.getClass()
							.getResourceAsStream( "/sdrtrunk-version" ) ) ) )
			{
				String version = reader.readLine();

				if( version != null )
				{
					sb.append( " V" );
					sb.append( version );
				}
			}
			catch( Exception e )
			{
				mLog.error( "Couldn't read sdrtrunk version from application jar file" );
			}

			mApplicationName = sb.toString();
		}

		return mApplicationName;
	}

	/**
	 * Returns the value of the property, or null if the property doesn't exist
	 */
	private String get( String key )
	{
		return mProperties.getProperty( key );
	}

	/**
	 * Returns the value of the property, or the defaultValue if the
	 * property doesn't exist
	 */
	public String get( String key, String defaultValue )
	{
		String value = get( key );
		
		if( value != null )
		{
			return value;
		}

		set( key, defaultValue );
		
		return defaultValue;
	}
	
	/**
	 * Returns the value of the property, or the defaultValue if the
	 * property doesn't exist
	 */
	public boolean get( String key, boolean defaultValue )
	{
		String value = get( key );
		
		if( value != null )
		{
			try
			{
				boolean stored = Boolean.parseBoolean( value );
				
				return stored;
			}
			catch( Exception e )
			{
				//Do nothing, we couldn't parse the stored value
			}
		}

		set( key, String.valueOf( defaultValue ) );
		
		return defaultValue;
	}
	
	/**
	 * Returns the value of the property, or the defaultValue if the
	 * property doesn't exist
	 */
	public int get( String key, int defaultValue )
	{
		String value = get( key );
		
		if( value != null )
		{
			try
			{
				int stored = Integer.parseInt( value );
				
				return stored;
			}
			catch( Exception e )
			{
				//Do nothing, we couldn't parse the stored value
			}
		}

		set( key, String.valueOf( defaultValue ) );
		
		return defaultValue;
	}

	/**
	 * Sets (overrides) the property key with the new value
	 */
	public void set( String key, String value )
	{
		mProperties.setProperty( key, value );
		
		save();
	}
	
	
}

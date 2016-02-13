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
package settings;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import map.DefaultIcon;
import map.MapIcon;

import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import properties.SystemProperties;
import settings.ColorSetting.ColorSettingName;
import source.recording.RecordingConfiguration;
import source.tuner.TunerConfiguration;
import source.tuner.TunerConfigurationAssignment;
import source.tuner.TunerType;
import source.tuner.airspy.AirspyTunerConfiguration;
import source.tuner.fcd.proV1.FCD1TunerConfiguration;
import source.tuner.fcd.proplusV2.FCD2TunerConfiguration;
import source.tuner.hackrf.HackRFTunerConfiguration;
import source.tuner.rtl.e4k.E4KTunerConfiguration;
import source.tuner.rtl.r820t.R820TTunerConfiguration;

public class SettingsManager
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( SettingsManager.class );

	private Settings mSettings = new Settings();
	
	private ArrayList<SettingChangeListener> mListeners = 
				new ArrayList<SettingChangeListener>();

	public static final String DEFAULT_ICON = "No Icon";
	public String mCurrentDefaultIconName = DEFAULT_ICON;
	public static final int DEFAULT_ICON_SIZE = 12;

	private HashMap<String,MapIcon> mIcons = new HashMap<String,MapIcon>();

	private HashMap<String,ImageIcon> mResizedIcons = 
							new HashMap<String,ImageIcon>();
	
	public SettingsManager()
	{
		init();
	}

	/**
	 * Loads settings from the current settings file, or the default settings file,
	 * as specified in the current SDRTrunk system settings
	 */
	private void init()
	{
		SystemProperties props = SystemProperties.getInstance();
		
		Path settingsFolder = props.getApplicationFolder( "settings" );
		
		String defaultSettingsFile = 
				props.get( "settings.defaultFilename", "settings.xml" );
		
		String settingsFile = 
				props.get( "settings.currentFilename", defaultSettingsFile );
		
		load( settingsFolder.resolve( settingsFile ) );
		
		refreshIcons();
	}

	/**
	 * Reloads the icon hash map, and sets up the default iconology.  If the
	 * saved default icon name is not part of the current set of icons, we 
	 * set the default icon to the standard system default name, which should
	 * be guaranteed to be available.
	 */
	public void refreshIcons()
	{
		mIcons.clear();
		
		mCurrentDefaultIconName = DEFAULT_ICON;

		DefaultIcon defaultIcon = getDefaultIcon();

		ArrayList<MapIcon> icons = mSettings.getMapIcons();
		icons.addAll( getStandardIcons() );
		
		for( MapIcon icon: icons )
		{
			mIcons.put( icon.getName(), icon );
			
			if( icon.getName().contentEquals( defaultIcon.getName() ) )
			{
				mCurrentDefaultIconName = defaultIcon.getName();
				icon.setDefaultIcon( true );
			}
			else
			{
				icon.setDefaultIcon( false );
			}
		}
	}
	
	public Settings getSettings()
	{
		return mSettings;
	}
	
	public void setSettings( Settings settings )
	{
		mSettings = settings;
	}
	
	public Setting getSetting( String name )
	{
		return mSettings.getSetting( name );
	}
	
	/**
	 * Returns the current setting, or if the setting doesn't exist
	 * returns a newly created setting with the specified parameters
	 */
	public ColorSetting getColorSetting( ColorSettingName name )
	{
		ColorSetting setting = mSettings.getColorSetting( name );
		
		if( setting == null )
		{
			setting = new ColorSetting( name );

			addSetting( setting );
		}
		
		return setting;
	}
	
	/**
	 * Fetches the current setting and applies the parameter(s) to it.  Creates
	 * the setting if it does not exist
	 */
	public void setColorSetting( ColorSettingName name, Color color )
	{
		ColorSetting setting = getColorSetting( name );
		
		setting.setColor( color );
		
		broadcastSettingChange( setting );
		
		save();
	}
	
	public void resetColorSetting( ColorSettingName name )
	{
		setColorSetting( name, name.getDefaultColor() );
	}
	
	public void resetAllColorSettings()
	{
		for( ColorSetting color: mSettings.getColorSettings() )
		{
			resetColorSetting( color.getColorSettingName() );
		}
	}
	
	/**
	 * Returns the current setting, or if the setting doesn't exist
	 * returns a newly created setting with the specified parameters
	 */
	public FileSetting getFileSetting( String name, String defaultPath )
	{
		FileSetting setting = mSettings.getFileSetting( name );
		
		if( setting == null )
		{
			setting = new FileSetting( name, defaultPath );
			
			addSetting( setting );
		}
		
		return setting;
	}
	
	/**
	 * Fetches the current setting and applies the parameter(s) to it.  Creates
	 * the setting if it does not exist
	 */
	public void setFileSetting( String name, String path )
	{
		FileSetting setting = getFileSetting( name, path );
		
		setting.setPath( path );
		
		broadcastSettingChange( setting );
		
		save();
	}
	
	public MapIcon[] getMapIcons()
	{
		MapIcon[] returnIcons = new MapIcon[mIcons.values().size() ];
		
		return mIcons.values().toArray( returnIcons );
	}
	
	private ArrayList<MapIcon> getStandardIcons()
	{
		ArrayList<MapIcon> icons = new ArrayList<MapIcon>();

		MapIcon defaultIcon = new MapIcon( DEFAULT_ICON, "images/no_icon.png", false );
		icons.add( defaultIcon );
		
		MapIcon ambulance = 
				new MapIcon( "Ambulance", "images/ambulance.png", false );
		icons.add( ambulance );

		MapIcon blockTruck = 
				new MapIcon( "Block Truck", "images/concrete_block_truck.png", false );
		icons.add( blockTruck );

		MapIcon cwid = new MapIcon( "CWID", "images/cwid.png", false );
		icons.add( cwid );

		MapIcon dispatcher = 
				new MapIcon( "Dispatcher", "images/dispatcher.png", false );
		icons.add( dispatcher );

		MapIcon dumpTruck = 
				new MapIcon( "Dump Truck", "images/dump_truck_red.png", false );
		icons.add( dumpTruck );

		MapIcon fireTruck = 
				new MapIcon( "Fire Truck", "images/fire_truck.png", false );
		icons.add( fireTruck );

		MapIcon garbageTruck = 
				new MapIcon( "Garbage Truck", "images/garbage_truck.png", false );
		icons.add( garbageTruck );

		MapIcon loader = new MapIcon( "Loader", "images/loader.png", false );
		icons.add( loader );

		MapIcon police = 
				new MapIcon( "Police", "images/police.png", false );
		icons.add( police );

		MapIcon propaneTruck = 
				new MapIcon( "Propane Truck", "images/propane_truck.png", false );
		icons.add( propaneTruck );

		MapIcon rescueTruck = 
				new MapIcon( "Rescue Truck", "images/rescue_truck.png", false );
		icons.add( rescueTruck );

		MapIcon schoolBus = 
				new MapIcon( "School Bus", "images/school_bus.png", false );
		icons.add( schoolBus );

		MapIcon taxi = 
				new MapIcon( "Taxi", "images/taxi.png", false );
		icons.add( taxi );
		
		MapIcon train = 
				new MapIcon( "Train", "images/train.png", false );
		icons.add( train );
		
		MapIcon transportBus = 
				new MapIcon( "Transport Bus", "images/opt_bus.png", false );
		icons.add( transportBus );
		
		MapIcon van = 
				new MapIcon( "Van", "images/van.png", false );
		icons.add( van );
		
		return icons;
	}

	/**
	 * Deletes the map icon
	 */
	public void deleteMapIcon( MapIcon icon )
	{
		MapIcon existing = mIcons.get( icon.getName() );
		
		if( existing != null )
		{
			mIcons.remove( icon.getName() );
			
			mSettings.removeSetting( existing );

			broadcastSettingDeleted( existing );

			save();
		}
	}

	/**
	 * Adds the icon, overwriting any existing icon with the same name
	 * @param icon
	 */
	public void addMapIcon( MapIcon icon )
	{
		MapIcon existing = mIcons.get( icon.getName() );
		
		if( existing == null )
		{
			MapIcon newIcon = new MapIcon( icon.getName(), icon.getPath() ); 
			mIcons.put( newIcon.getName(), newIcon );
			addSetting( newIcon );
		}
		else
		{
			existing.setName( icon.getName() );
			existing.setPath( icon.getPath() );
			broadcastSettingChange( existing );
			save();
		}
	}
	
	public void updatMapIcon( MapIcon icon, String newName, String newPath )
	{
		MapIcon existing = mSettings.getMapIcon( icon.getName() );

		if( existing == null )
		{
			addMapIcon( new MapIcon( newName, newPath ) );
		}
		else
		{
			existing.setName( newName );
			existing.setPath( newPath );
			broadcastSettingChange( existing );
			save();
		}
	}

	/**
	 * Returns named map icon scaled to height, if necessary.
	 * @param name - name of icon
	 * @param heigh - height of icon in pixels
	 * 
	 * @return - ImageIcon or null
	 */
	public ImageIcon getImageIcon( String name, int height )
	{
		if( name != null )
		{
			String mergedName = name + height;
			
			if( mResizedIcons.containsKey( mergedName ) )
			{
				return mResizedIcons.get( mergedName );
			}
			
			MapIcon mapIcon = mIcons.get( name );
			
			if( mapIcon == null )
			{
				mapIcon = new MapIcon( name, name );
				mIcons.put( name, mapIcon );
			}

			if( mapIcon != null )
			{
				if( mapIcon.getImageIcon().getIconHeight() > height )
				{
					ImageIcon scaled = getScaledIcon( mapIcon.getImageIcon(), height );

	        		mResizedIcons.put( mergedName, scaled );
	        		
	        		return scaled;
				}
				else
				{
					return mapIcon.getImageIcon();
				}
			}

			/* Use the default Icon */
			String mergedDefault = mCurrentDefaultIconName + height;
			
			if( mResizedIcons.containsKey( mergedDefault ) )
			{
				return mResizedIcons.get( mergedDefault );
			}
			else
			{
				MapIcon defaultIcon = mIcons.get( mCurrentDefaultIconName );

				if( defaultIcon != null )
				{
					if( defaultIcon.getImageIcon().getIconHeight() > height )
					{
						ImageIcon scaledDefault = getScaledIcon( defaultIcon.getImageIcon(), height );
						mResizedIcons.put( mergedDefault, scaledDefault );
						return scaledDefault;
					}
					else
					{
						mResizedIcons.put( mergedDefault, defaultIcon.getImageIcon() );
						return defaultIcon.getImageIcon();
					}
				}
			}

			/* Something happened ... the above should always return an icon */
			mLog.error( "SettingsManager - couldn't return an icon named [" + 
					name + "] of heigh [" + height + "]" );
		}

		return null;
	}
	
	public static ImageIcon getScaledIcon( ImageIcon original, int height )
	{
		double scale = (double)original.getIconHeight() / (double)height;
		
		int scaledWidth = (int)( (double)original.getIconWidth() / scale );
		
		Image scaledImage = original.getImage().getScaledInstance( scaledWidth, 
				height, java.awt.Image.SCALE_SMOOTH );  

		return new ImageIcon( scaledImage );
	}
	
	public MapIcon getMapIcon( String name )
	{
		if( mIcons.containsKey( name ) )
		{
			return mIcons.get( name );
		}

		/* Return the default */
		if( mIcons.containsKey( mCurrentDefaultIconName ) )
		{
			return mIcons.get( mCurrentDefaultIconName );
		}
		
		/* Return the real default */
		return mIcons.get( DEFAULT_ICON );
	}
	
	/**
	 * Returns the named map icon, or creates it with the default path
	 * @param name - name of the icon
	 * @param defaultPath - path to icon image file 
	 * @return - map icon
	 */
	public MapIcon getMapIcon( String name, String defaultPath )
	{
		MapIcon icon = mSettings.getMapIcon( name );
		
		if( icon == null )
		{
			icon = new MapIcon( name, defaultPath );
			
			addSetting( icon );
		}
		
		return icon;
	}
	
	public DefaultIcon getDefaultIcon()
	{
		DefaultIcon icon = mSettings.getDefaultIcon();
		
		if( icon == null )
		{
			icon = new DefaultIcon( DEFAULT_ICON );
			
			addSetting( icon );
		}
		
		return icon;
	}
	
	public void setDefaultIcon( MapIcon icon )
	{
		if( mIcons.containsValue( icon ) )
		{
			MapIcon currentDefault = mIcons.get( mCurrentDefaultIconName );
			
			currentDefault.setDefaultIcon( false );
			
			icon.setDefaultIcon( true );
			
			mCurrentDefaultIconName = icon.getName();
			
			DefaultIcon defaultIconSetting = getDefaultIcon();
			
			defaultIconSetting.setName( icon.getName() );
			
			save();
			
			broadcastSettingChange( defaultIconSetting );
		}
	}

	/**
	 * Adds the setting and stores the set of settings
	 * @param setting
	 */
	private void addSetting( Setting setting )
	{
		mSettings.addSetting( setting );

		save();
		
		broadcastSettingChange( setting );
	}
	
	public ArrayList<RecordingConfiguration> getRecordingConfigurations()
	{
		return mSettings.getRecordingConfigurations();
	}
	
	public void addRecordingConfiguration( RecordingConfiguration config )
	{
		mSettings.addRecordingConfiguration( config );
		save();
	}
	
	public void removeRecordingConfiguration( RecordingConfiguration config )
	{
		mSettings.removeRecordingConfiguration( config );
		save();
	}
	
	public ArrayList<TunerConfiguration> getTunerConfigurations( TunerType type )
	{
		ArrayList<TunerConfiguration> configs = getSettings()
				.getTunerConfigurations( type );

		if( configs.isEmpty() )
		{
			configs.add( addNewTunerConfiguration( type, "Default" ) );
		}
		
		return configs;
	}
	
	public void deleteTunerConfiguration( TunerConfiguration config )
	{
		getSettings().removeTunerConfiguration( config );
		save();
	}
	
	public TunerConfiguration addNewTunerConfiguration( TunerType type, 
														String name )
	{
		switch( type )
		{
			case AIRSPY_R820T:
				AirspyTunerConfiguration airspyConfig = 
						new AirspyTunerConfiguration( name );
				
				getSettings().addTunerConfiguration( airspyConfig );
				
				save();
				return airspyConfig;
			case ELONICS_E4000:
				E4KTunerConfiguration e4KConfig = 
							new E4KTunerConfiguration( name );

				getSettings().addTunerConfiguration( e4KConfig );
				
				save();
				return e4KConfig;
			case FUNCUBE_DONGLE_PRO:
				FCD1TunerConfiguration config = 
					new FCD1TunerConfiguration( name );
				
				getSettings().addTunerConfiguration( config );
				
				save();
				
				return config;
			case FUNCUBE_DONGLE_PRO_PLUS:
				FCD2TunerConfiguration configPlus = 
					new FCD2TunerConfiguration( name );
				
				getSettings().addTunerConfiguration( configPlus );
				
				save();
				
				return configPlus;
			case HACKRF:
				HackRFTunerConfiguration hackConfig = 
							new HackRFTunerConfiguration( name );
				
				getSettings().addTunerConfiguration( hackConfig );
				
				save();
				
				return hackConfig;
			case RAFAELMICRO_R820T:
				R820TTunerConfiguration r820TConfig = 
							new R820TTunerConfiguration( name );

				getSettings().addTunerConfiguration( r820TConfig );
				
				save();
				
				return r820TConfig;
			default:
				throw new IllegalArgumentException( "TunerConfiguration"
						+ "Directory - tuner type is unrecognized [" + 
						type.toString() + "]" );
		}
	}

    public TunerConfigurationAssignment getSelectedTunerConfiguration( 
    							TunerType type, String address )
	{
    	return mSettings.getConfigurationAssignment( type, address );
	}
    
    public void setSelectedTunerConfiguration( TunerType type, 
    			String address, TunerConfiguration config )
    {
    	mSettings.setConfigurationAssignment( type, address, config.getName() );
    	
    	save();
    }
    
    public MapViewSetting getMapViewSetting( String name, GeoPosition position, int zoom )
    {
    	MapViewSetting loc = mSettings.getMapViewSetting( name );
    	
    	if( loc != null )
    	{
    		return loc;
    	}
    	else
    	{
    		MapViewSetting newLoc = new MapViewSetting( name, position, zoom );
    		
    		addSetting( newLoc );

    		return newLoc;
    	}
    }
    
    public void setMapViewSetting( String name, GeoPosition position, int zoom )
    {
    	MapViewSetting loc = getMapViewSetting( name, position, zoom );
    	
		loc.setGeoPosition( position );
		loc.setZoom( zoom );
		
		save();
    }

    public void save()
	{
		JAXBContext context = null;
		
		SystemProperties props = SystemProperties.getInstance();

		Path settingsPath = props.getApplicationFolder( "settings" );
		
		String settingsDefault = props.get( "settings.defaultFilename", 
										 "settings.xml" );

		String settingsCurrent = props.get( "settings.currentFilename", 
										 settingsDefault );
		
		Path filePath = settingsPath.resolve( settingsCurrent );
		
		File outputFile = new File( filePath.toString() );

		try
		{
			if( !outputFile.exists() )
			{
				outputFile.createNewFile();
			}
		}
		catch( Exception e )
		{
			mLog.error( "SettingsManager - couldn't create file to save "
					+ "settings [" + filePath.toString() + "]", e );
		}
		
		OutputStream out = null;
		
		try
        {
	        out = new FileOutputStream( outputFile );
	        
			try
	        {
		        context = JAXBContext.newInstance( Settings.class );

		        Marshaller m = context.createMarshaller();

		        m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
	        
		        m.marshal( mSettings, out );
	        }
	        catch ( JAXBException e )
	        {
	        	mLog.error( "SettingsManager - jaxb exception while saving " +
		        		"settings", e );
	        }
        }
        catch ( Exception e )
        {
        	mLog.error( "SettingsManager - coulcn't open outputstream to " +
        			"save settings [" + filePath.toString() + "]" );
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
	                e.printStackTrace();
                }
			}
		}
	}
	
	/**
	 * Erases current settings and loads settings from the settingsPath filename,
	 * if it exists.
	 */
	public void load( Path settingsPath )
	{
		if( Files.exists( settingsPath ) )
		{
			mLog.info( "SettingsManager - loading settings file [" + 
							settingsPath.toString() + "]" );
			
			JAXBContext context = null;
			
			InputStream in = null;
			
			try
	        {
		        in = new FileInputStream( settingsPath.toString() );
		        
				try
		        {
			        context = JAXBContext.newInstance( Settings.class );

			        Unmarshaller m = context.createUnmarshaller();

			        mSettings = (Settings)m.unmarshal( in );
		        }
		        catch ( JAXBException e )
		        {
		        	mLog.error( "SettingsManager - jaxb exception while loading " +
			        		"settings", e );
		        }
	        }
	        catch ( Exception e )
	        {
	        	mLog.error( "SettingsManager - coulcn't open inputstream to " +
	        			"load settings [" + settingsPath.toString() + "]", e );
	        }
			finally
			{
				if( in != null )
				{
					try
	                {
		                in.close();
	                }
	                catch ( IOException e )
	                {
	                	mLog.error( "SettingsManager - exception while closing " +
	                			"the settings file inputstream reader", e );
	                }
				}
			}
		}
		else
		{
			mLog.info( "SettingsManager - settings does not exist [" + 
							settingsPath.toString() + "]" );
		}
		
		if( mSettings == null )
		{
			mSettings = new Settings();
		}
	}
	
	public void broadcastSettingChange( Setting setting )
	{
		Iterator<SettingChangeListener> it = mListeners.iterator();
		
		while( it.hasNext() )
		{
			SettingChangeListener listener = it.next();
			
			if( listener == null )
			{
				it.remove();
			}
			else
			{
				listener.settingChanged( setting );
			}
		}
	}
	
	public void broadcastSettingDeleted( Setting setting )
	{
		Iterator<SettingChangeListener> it = mListeners.iterator();
		
		while( it.hasNext() )
		{
			SettingChangeListener listener = it.next();
			
			if( listener == null )
			{
				it.remove();
			}
			else
			{
				listener.settingDeleted( setting );
			}
		}
	}
	
	public void addListener( SettingChangeListener listener )
	{
		mListeners.add( listener );
	}
	
	public void removeListener( SettingChangeListener listener )
	{
		mListeners.remove( listener );
	}
}

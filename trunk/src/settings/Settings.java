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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import map.DefaultIcon;
import map.MapIcon;

import org.jdesktop.swingx.mapviewer.GeoPosition;

import settings.ColorSetting.ColorSettingName;
import source.tuner.TunerConfiguration;
import source.tuner.TunerConfigurationAssignment;
import source.tuner.TunerType;
import source.tuner.fcd.proV1.FCD1TunerConfiguration;
import source.tuner.fcd.proplusV2.FCD2TunerConfiguration;
import source.tuner.rtl.e4k.E4KTunerConfiguration;
import source.tuner.rtl.r820t.R820TTunerConfiguration;

/**
 * Support for persisting via JAXB all configured SDRTrunk settings
 */
@XmlSeeAlso( { ColorSetting.class,
			   DefaultIcon.class,
			   E4KTunerConfiguration.class,
			   FCD1TunerConfiguration.class,
			   FCD2TunerConfiguration.class,
			   FileSetting.class,
			   MapViewSetting.class,
			   MapIcon.class,
			   R820TTunerConfiguration.class,
			   Setting.class,
			   TunerConfiguration.class, 
			   TunerConfigurationAssignment.class} )

@XmlRootElement( name = "SDRTrunk_settings" )
public class Settings
{
	private ArrayList<Setting> mSettings = new ArrayList<Setting>();

	private ArrayList<TunerConfiguration> mTunerConfiguration =
			new ArrayList<TunerConfiguration>();
	
	private ArrayList<TunerConfigurationAssignment> mConfigurationAssignments =
			new ArrayList<TunerConfigurationAssignment>();
	
	public Settings()
	{
	}

	@XmlElement( name="tuner_configuration" )
	public ArrayList<TunerConfiguration> getTunerConfigurations()
	{
		return mTunerConfiguration;
	}
	
	public void setTunerConfigurations( ArrayList<TunerConfiguration> configs )
	{
		mTunerConfiguration = configs;
	}
	
	public void addTunerConfiguration( TunerConfiguration config )
	{
		mTunerConfiguration.add( config );
		
	}
	
	public void removeTunerConfiguration( TunerConfiguration config )
	{
		mTunerConfiguration.remove( config );
	}
	
    public ArrayList<TunerConfiguration> getTunerConfigurations( TunerType type )
	{
		ArrayList<TunerConfiguration> configs = 
				new ArrayList<TunerConfiguration>();
		
		for( TunerConfiguration config: mTunerConfiguration )
		{
			if( config.getTunerType() == type )
			{
				configs.add( config );
			}
		}

		return configs;
	}
    
	@XmlElement( name="setting" )
	public ArrayList<Setting> getSettings()
	{
		return mSettings;
	}
	
	public void setSettings( ArrayList<Setting> settings )
	{
		mSettings = settings;
	}
	
	public Setting getSetting( String name )
	{
		for( Setting setting: mSettings )
		{
			if( setting.getName().contentEquals( name ) )
			{
				return setting;
			}
		}
		
		return null;
	}
	
	public void addSetting( Setting setting )
	{
		mSettings.add( setting );
	}
	
	public void removeSetting( Setting setting )
	{
		mSettings.remove( setting );
	}
	
	public DefaultIcon getDefaultIcon()
	{
		for( Setting setting: mSettings )
		{
			if( setting instanceof DefaultIcon )
			{
				return (DefaultIcon)setting;
			}
		}
		
		return null;
	}
	
	public ColorSetting getColorSetting( ColorSettingName name )
	{
		for( Setting setting: mSettings )
		{
			if( setting instanceof ColorSetting &&
				((ColorSetting)setting).getColorSettingName() == name )
			{
				return (ColorSetting)setting;
			}
		}
		
		return null;
	}
	
	public List<ColorSetting> getColorSettings()
	{
		ArrayList<ColorSetting> colors = new ArrayList<ColorSetting>();
		
		for( Setting setting: mSettings )
		{
			if( setting instanceof ColorSetting )
			{
				colors.add( (ColorSetting)setting );
			}
		}
		
		return colors;
	}

	public FileSetting getFileSetting( String name )
	{
		for( Setting setting: mSettings )
		{
			if( setting instanceof FileSetting && 
				setting.getName().contentEquals( name ) )
			{
				return (FileSetting)setting;
			}
		}
		
		return null;
	}

	public MapIcon getMapIcon( String name )
	{
		for( Setting setting: mSettings )
		{
			if( setting instanceof MapIcon && 
				setting.getName().contentEquals( name ) )
			{
				return (MapIcon)setting;
			}
		}
		
		return null;
	}
	
	public ArrayList<MapIcon> getMapIcons()
	{
		ArrayList<MapIcon> icons = new ArrayList<MapIcon>();
		
		for( Setting setting: mSettings )
		{
			if( setting instanceof MapIcon )
			{
				icons.add( (MapIcon)setting );
			}
		}
		
		return icons;
	}

	@XmlElement( name="tuner_configuration_assignment" )
	public ArrayList<TunerConfigurationAssignment> getSelectedTunerConfiguration()
	{
		return mConfigurationAssignments;
	}
	
	public void setSelectedTunerConfiguration( ArrayList<TunerConfigurationAssignment> configs )
	{
		mConfigurationAssignments = configs;
	}
	
	public void addSelectedTunerConfiguration( TunerConfigurationAssignment config )
	{
		mConfigurationAssignments.add( config );
		
	}
	
	public void removeSelectedTunerConfiguration( TunerConfigurationAssignment config )
	{
		mConfigurationAssignments.remove( config );
	}
	
    public TunerConfigurationAssignment getConfigurationAssignment( TunerType type, String uniqueID )
	{
		for( TunerConfigurationAssignment config: mConfigurationAssignments )
		{
			if( config.getTunerType() == type && 
				uniqueID.contentEquals( config.getUniqueID() ) )
			{
				return config;
			}
		}

		return null;
	}
    
    public void setConfigurationAssignment( TunerType type, 
    							  			String uniqueID, 
    							  			String tunerConfigurationName )
    {
    	TunerConfigurationAssignment config = getConfigurationAssignment( type, uniqueID );
    	
    	if( config == null )
    	{
    		config = new TunerConfigurationAssignment();
    		config.setTunerType( type );
    		config.setUniqueID( uniqueID );
        	addSelectedTunerConfiguration( config );
    	}
    	
    	config.setTunerConfigurationName( tunerConfigurationName );
    }
    
    public MapViewSetting getMapViewSetting( String name )
    {
    	for( Setting setting: mSettings )
    	{
    		if( setting instanceof MapViewSetting &&
    			setting.getName().contentEquals( name ) )
    		{
    			return (MapViewSetting)setting;
    		}
    	}
    	
    	return null;
    }
}

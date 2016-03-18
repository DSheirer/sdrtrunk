/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
import settings.ColorSetting.ColorSettingName;
import source.recording.RecordingConfiguration;
import source.tuner.airspy.AirspyTunerConfiguration;
import source.tuner.configuration.TunerConfiguration;
import source.tuner.fcd.proV1.FCD1TunerConfiguration;
import source.tuner.fcd.proplusV2.FCD2TunerConfiguration;
import source.tuner.hackrf.HackRFTunerConfiguration;
import source.tuner.rtl.e4k.E4KTunerConfiguration;
import source.tuner.rtl.r820t.R820TTunerConfiguration;

/**
 * Support for persisting via JAXB all configured SDRTrunk settings
 */
@XmlSeeAlso( { AirspyTunerConfiguration.class,
			   ColorSetting.class,
			   DefaultIcon.class,
			   E4KTunerConfiguration.class,
			   FCD1TunerConfiguration.class,
			   FCD2TunerConfiguration.class,
			   FileSetting.class,
			   HackRFTunerConfiguration.class,
			   MapViewSetting.class,
			   MapIcon.class,
			   R820TTunerConfiguration.class,
			   RecordingConfiguration.class,
			   Setting.class,
			   TunerConfiguration.class } )

@XmlRootElement( name = "SDRTrunk_settings" )
public class Settings
{
	private List<Setting> mSettings = new ArrayList<Setting>();

	private List<TunerConfiguration> mTunerConfiguration =
			new ArrayList<TunerConfiguration>();
	
	private List<RecordingConfiguration> mRecordingConfigurations =
			new ArrayList<RecordingConfiguration>();

	public Settings()
	{
	}

	@XmlElement( name="recording_configuration" )
	public List<RecordingConfiguration> getRecordingConfigurations()
	{
		return mRecordingConfigurations;
	}
	
	public void setRecordingConfigurations( ArrayList<RecordingConfiguration> configs )
	{
		mRecordingConfigurations = configs;
	}
	
	public void addRecordingConfiguration( RecordingConfiguration config )
	{
		mRecordingConfigurations.add( config );
	}
	
	public void removeRecordingConfiguration( RecordingConfiguration config )
	{
		mRecordingConfigurations.remove( config );
	}
	
	@XmlElement( name="tuner_configuration" )
	public List<TunerConfiguration> getTunerConfigurations()
	{
		return mTunerConfiguration;
	}
	
	public void setTunerConfigurations( List<TunerConfiguration> configs )
	{
		mTunerConfiguration = configs;
	}
    
	@XmlElement( name="setting" )
	public List<Setting> getSettings()
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

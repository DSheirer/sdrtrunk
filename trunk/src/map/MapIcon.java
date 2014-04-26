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
package map;

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import settings.Setting;

public class MapIcon extends Setting implements Comparable<MapIcon>
{
	private static final int sMAX_IMAGE_DIMENSION = 48;
	private String mPath;
	private ImageIcon mImageIcon;

	/**
	 * Only map icons created at runtime can be marked as non-editable, and 
	 * therefore this property is transient.  
	 */
	@XmlTransient
	private boolean mEditable;
	
	@XmlTransient
	private boolean mDefaultIcon;
	
	/**
	 * Wrapper class for a map icon.  
	 * @param name - name of the icon - also used as key to lookup the icon
	 * @param path - file path to the icon
	 * @param editable - defines if the map icon or details can be edited
	 * 
	 * Note: the default icons are constructed with editable = false, so that
	 * they cannot be deleted from the Icon Manager editor window
	 */
	public MapIcon( String name, String path, boolean editable )
	{
		super( name );
		mPath = path;
		mEditable = editable;
	}
	
	public MapIcon( String name, String path )
	{
		this( name, path, true );
	}

	/**
	 * Don't use this constructor.  This is used by JAXB to unmarshall saved
	 * map icons.
	 */
	public MapIcon()
	{
		mEditable = true;
	}


	@XmlAttribute( name="editable" )
	public boolean isEditable()
	{
		return mEditable;
	}
	
	@XmlAttribute( name="default" )
	public boolean isDefaultIcon()
	{
		return mDefaultIcon;
	}
	
	public void setDefaultIcon( boolean isDefault )
	{
		mDefaultIcon = isDefault;
	}
	
	public ImageIcon getImageIcon()
	{
		if( mImageIcon == null )
		{
			mImageIcon = new ImageIcon( mPath );
			
			/**
			 * If the image is too big, scale it down to max pixel size squared
			 */
			if( mImageIcon.getIconWidth() > sMAX_IMAGE_DIMENSION ||
				mImageIcon.getIconHeight() > sMAX_IMAGE_DIMENSION )
			{
				/**
				 * getScaled instance will correct any negative value to the
				 * correct value, maintaining original aspect ratio.  So, we
				 * only scale the larger value, and allow the image class to
				 * determine the correct value for the other measurement
				 */
				int height = -1;
				int width = -1;
				
				/**
				 * Use the larger width or height value to determine the 
				 * scaling factor
				 */
				if( mImageIcon.getIconHeight() > mImageIcon.getIconWidth() )
				{
					height = sMAX_IMAGE_DIMENSION;
				}
				else
				{
					width = sMAX_IMAGE_DIMENSION;
				}
				
				mImageIcon = new ImageIcon( mImageIcon.getImage()
						.getScaledInstance( width, height, Image.SCALE_SMOOTH ) );
			}
			
		}
		
		return mImageIcon;
	}
	
	@XmlAttribute( name="path" )
	public String getPath()
	{
		return mPath;
	}
	
	public void setPath( String path )
	{
		mPath = path;
	}
	
	public String toString()
	{
		if( mDefaultIcon )
		{
			return getName() + " (default)";
		}
		else
		{
			return getName();
		}
	}

	@Override
	public boolean equals( Object obj )
	{
		if( obj instanceof MapIcon )
		{
			MapIcon other = (MapIcon)obj;
			
			return other.getName().contentEquals( getName() ) &&
				   other.getPath().contentEquals( getPath() );
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		return getName().hashCode() + getPath().hashCode();
	}

	/**
	 * Sort order is determined by the icon name
	 */
	@Override
    public int compareTo( MapIcon other )
    {
		return getName().compareTo( other.getName() );
    }
}

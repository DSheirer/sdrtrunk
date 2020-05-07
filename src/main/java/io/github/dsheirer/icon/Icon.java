/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.icon;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

@JacksonXmlRootElement(localName = "icon")
public class Icon implements Comparable<Icon>
{
    private final static Logger mLog = LoggerFactory.getLogger(Icon.class);
    private static final int ICON_HEIGHT_JAVAFX = 16;
    private StringProperty mName = new SimpleStringProperty();
    private StringProperty mPath = new SimpleStringProperty();
    private BooleanProperty mDefaultIcon = new SimpleBooleanProperty();
    private BooleanProperty mStandardIcon = new SimpleBooleanProperty();
    private ImageIcon mImageIcon;
    private Image mFxImage;
    private boolean mFxImageLoaded = false;

    /**
     * JAXB constructor - do not use
     */
    public Icon()
    {
        //No-arg JAXB constructor
    }

    /**
     * Constructs an instance
     * @param name for the icon
     * @param path to the icon
     */
    public Icon(String name, String path)
    {
        setName(name);
        setPath(path);
    }

    @JsonIgnore
    public StringProperty nameProperty()
    {
        return mName;
    }

    @JsonIgnore
    public StringProperty pathProperty()
    {
        return mPath;
    }

    @JsonIgnore
    public BooleanProperty defaultIconProperty()
    {
        return mDefaultIcon;
    }

    @JsonIgnore
    public BooleanProperty standardIconProperty()
    {
        return mStandardIcon;
    }

    /**
     * Name of the icon
     */
    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String getName()
    {
        return mName.get();
    }

    /**
     * Sets the name of the icon
     */
    public void setName(String name)
    {
        mName.set(name);
    }

    /**
     * Indicates if this is a standard icon
     */
    @JsonIgnore
    public boolean getStandardIcon()
    {
        return mStandardIcon.get();
    }

    /**
     * Sets or flags this icon as a standard icon indicating that it should not be deleted
     */
    void setStandardIcon(boolean standardIcon)
    {
        mStandardIcon.set(standardIcon);
    }

    /**
     * Indicates if this icon is the default icon
     */
    @JsonIgnore
    public boolean getDefaultIcon()
    {
        return mDefaultIcon.get();
    }

    /**
     * Sets the default icon state.
     *
     * Note: uniqueness of default flag is only enforced through the icon model and package private access
     */
    void setDefaultIcon(boolean defaultIcon)
    {
        mDefaultIcon.set(defaultIcon);
    }

    public String toString()
    {
        return getName();
    }

    /**
     * Path to the icon
     */
    @JacksonXmlProperty(isAttribute = true, localName = "path")
    public String getPath()
    {
        return mPath.get();
    }

    /**
     * Sets the path to the icon
     */
    public void setPath(String path)
    {
        mPath.set(path);
    }

    @JsonIgnore
    public ImageIcon getIcon()
    {
        if(mImageIcon == null && mPath != null)
        {
            try
            {
                if(!getPath().startsWith("images"))
                {
                    mImageIcon = new ImageIcon(getPath());
                }
                else
                {
                    URL imageURL = Icon.class.getResource(getPath());

                    if(imageURL == null && !getPath().startsWith("/"))
                    {
                        imageURL = (Icon.class.getResource("/" + getPath()));
                    }

                    if(imageURL != null)
                    {
                        mImageIcon = new ImageIcon(imageURL);
                    }
                }
            }
            catch(Exception e)
            {
                mLog.error("Error loading Icon [" + getPath() + "]", e);
            }
        }

        return mImageIcon;
    }

    /**
     * Lazy loads an FX image for the icon and retains it in memory.
     * @return loaded image or null if the image can't be loaded
     */
    @JsonIgnore
    public Image getFxImage()
    {
        if(!mFxImageLoaded && getPath() != null && !getPath().isEmpty())
        {
            //Set the loaded flag to true regardless if the image loads so that if there is an error loading the
            //image we log it once and don't attempt to reload it again
            mFxImageLoaded = true;

            if(getPath() == null || getPath().isEmpty())
            {
                mLog.error("Error loading icon [" + getName() + "] - null or empty file path to image");
            }
            else
            {
                if(getPath().startsWith("images"))
                {
                    mFxImage = new Image(getPath(), 0, ICON_HEIGHT_JAVAFX, true, true);
                }
                else
                {
                    Path filePath = Path.of(getPath());
                    mFxImage = new Image(filePath.toUri().toString(), 0, ICON_HEIGHT_JAVAFX, true, true);
                }

                if(mFxImage.getException() != null)
                {
                    mLog.error("Error loading icon [" + getName() + " " + getPath() + "] - " +
                        mFxImage.getException().getLocalizedMessage());
                }
            }
        }

        return mFxImage;
    }

    @Override
    public int compareTo(Icon other)
    {
        if(other == null)
        {
            return -1;
        }
        else if(hashCode() == other.hashCode())
        {
            return 0;
        }
        else if(getName() != null && other.getName() != null)
        {
            if(getName().contentEquals(other.getName()))
            {
                if(getPath() != null && other.getPath() != null)
                {
                    return getPath().compareTo(other.getPath());
                }
                else if(getPath() != null)
                {
                    return -1;
                }
                else
                {
                    return 1;
                }
            }
            else
            {
                return getName().compareTo(other.getName());
            }
        }
        else if(getName() != null)
        {
            return -1;
        }
        else
        {
            return 1;
        }
   }

    @Override
    public int hashCode()
    {
        return Objects.hash(getName(), getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Icon)) return false;
        return compareTo((Icon) o) == 0;
    }

    /**
     * Creates an observable property extractor for use with observable lists to detect changes internal to this object.
     */
    @JsonIgnore
    public static Callback<Icon,Observable[]> extractor()
    {
        return (Icon i) -> new Observable[] {i.nameProperty(), i.pathProperty(), i.standardIconProperty(), i.defaultIconProperty()};
    }
}

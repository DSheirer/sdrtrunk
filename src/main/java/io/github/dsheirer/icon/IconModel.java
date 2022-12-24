/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.util.ThreadPool;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;

public class IconModel
{
    private final static Logger mLog = LoggerFactory.getLogger(IconModel.class);
    public static final int DEFAULT_ICON_SIZE = 12;
    public static final String DEFAULT_ICON = "No Icon";

    private Path mIconFolderPath;
    private Path mIconFilePath;
    private Path mIconBackupFilePath;
    private Path mIconLockFilePath;

    private AtomicBoolean mSavingIcons = new AtomicBoolean();
    private ObservableList<Icon> mIcons = FXCollections.observableArrayList(Icon.extractor());
    private StringProperty mDefaultIconName = new SimpleStringProperty();
    private Map<String,ImageIcon> mResizedIcons = new HashMap<>();
    private Icon mDefaultIcon;
    private IconSet mStandardIcons;

    public IconModel()
    {
        IconSet iconSet = load();

        if(iconSet == null)
        {
            iconSet = getStandardIconSet();
        }

        IconSet standardIcons = getStandardIconSet();

        mIcons.addAll(iconSet.getIcons());

        for(Icon icon: mIcons)
        {
            if(iconSet.getDefaultIcon() != null && iconSet.getDefaultIcon().matches(icon.getName()))
            {
                icon.setDefaultIcon(true);
                mDefaultIcon = icon;
            }

            if(standardIcons.getIcons().contains(icon))
            {
                icon.setStandardIcon(true);
            }
        }

        if(mDefaultIcon == null && !mIcons.isEmpty())
        {
            setDefaultIcon(mIcons.get(0));
        }

        //Add a change detection listener to schedule saves when the list changes.
        mIcons.addListener((ListChangeListener<Icon>)c -> scheduleSave());
    }

    /**
     * Adds the icon to this model
     */
    public void addIcon(Icon icon)
    {
        if(icon != null && !mIcons.contains(icon))
        {
            mIcons.add(icon);
        }
    }

    /**
     * Removes the icon from this model
     */
    public void removeIcon(Icon icon)
    {
        if(icon != null && !icon.getStandardIcon() && !icon.getDefaultIcon())
        {
            mIcons.remove(icon);
        }
    }

    /**
     * Sets the default icon
     */
    public void setDefaultIcon(Icon icon)
    {
        if(icon != null)
        {
            if(mDefaultIcon != null)
            {
                mDefaultIcon.setDefaultIcon(false);
            }

            mDefaultIcon = icon;
            mDefaultIcon.setDefaultIcon(true);
        }
    }

    /**
     * Lookup an icon by name.
     * @param iconName to lookup
     * @return icon if found, or the default icon
     */
    public Icon getIcon(String iconName)
    {
        if(iconName != null)
        {
            for(Icon icon: iconsProperty())
            {
                if(icon.getName() != null && icon.getName().contentEquals(iconName))
                {
                    return icon;
                }
            }
        }

        return getDefaultIcon();
    }

    /**
     * Current set of icons managed by this model
     */
    public ObservableList<Icon> iconsProperty()
    {
        return mIcons;
    }

    public Icon getDefaultIcon()
    {
        return mDefaultIcon;
    }

    /**
     * Returns named icon scaled to the specified height.  Utilizes an internal map to retain scaled icons so that they
     * are only scaled/generated once.
     *
     * @param name - name of icon
     * @param height - height of icon in pixels
     * @return - scaled named icon (if it exists) or a scaled version of the default icon
     */
    public ImageIcon getIcon(String name, int height)
    {
        if(name == null)
        {
            name = getDefaultIcon().getName();
        }

        String scaledIconName = name + height;

        ImageIcon mapValue = mResizedIcons.get(scaledIconName);
        if (mapValue != null)
        {
            return mapValue;
        }

        Icon icon = getIcon(name);

        ImageIcon scaledIcon = getScaledIcon(icon.getIcon(), height);

        if(scaledIcon != null)
        {
            mResizedIcons.put(scaledIconName, scaledIcon);
        }

        return scaledIcon;
    }

    /**
     * Scales the icon to the new pixel height value
     *
     * @param original image icon
     * @param height new height to scale the image (width will be scaled accordingly)
     * @return
     */
    public static ImageIcon getScaledIcon(ImageIcon original, int height)
    {
        if(original != null)
        {
            double scale = (double) original.getIconHeight() / (double) height;

            int scaledWidth = (int) ((double) original.getIconWidth() / scale);

            Image scaledImage = original.getImage().getScaledInstance(scaledWidth,
                height, java.awt.Image.SCALE_SMOOTH);

            return new ImageIcon(scaledImage);
        }

        return null;
    }

    /**
     * Constructs an icon and scales it to the specified height
     * @param path
     * @param height
     * @return
     */
    public static ImageIcon getScaledIcon(String path, int height)
    {
        if(path != null)
        {
            Icon icon = new Icon("", path);
            return getScaledIcon(icon.getIcon(), height);
        }

        return null;
    }

    /**
     * Folder where icon, backup and lock files are stored
     */
    private Path getIconFolderPath()
    {
        if(mIconFolderPath == null)
        {
            SystemProperties props = SystemProperties.getInstance();

            mIconFolderPath = props.getApplicationFolder("settings");
        }

        return mIconFolderPath;
    }

    /**
     * Path to current icon file
     */
    private Path getIconFilePath()
    {
        if(mIconFilePath == null)
        {
            mIconFilePath = getIconFolderPath().resolve("icons.xml");
        }

        return mIconFilePath;
    }

    /**
     * Path to most recent playlist backup
     */
    private Path getIconBackupFilePath()
    {
        if(mIconBackupFilePath == null)
        {
            mIconBackupFilePath = getIconFolderPath().resolve("icons.backup");
        }

        return mIconBackupFilePath;
    }

    /**
     * Path to playlist lock file that is created prior to saving a playlist and removed immediately thereafter.
     * Presence of a lock file indicates an incomplete or corrupt playlist file on startup.
     */
    private Path getIconLockFilePath()
    {
        if(mIconLockFilePath == null)
        {
            mIconLockFilePath = getIconFolderPath().resolve("icons.lock");
        }

        return mIconLockFilePath;
    }

    /**
     * Saves the current playlist
     */
    private void save()
    {
        IconSet iconSet = new IconSet();
        iconSet.setDefaultIcon(getDefaultIcon().getName());
        iconSet.setIcons(new ArrayList<>(mIcons));

        //Create a backup copy of the current playlist
        if(Files.exists(getIconFilePath()))
        {
            try
            {
                Files.copy(getIconFilePath(), getIconBackupFilePath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch(Exception e)
            {
                mLog.error("Error creating backup copy of current icons prior to saving updates [" +
                    getIconFilePath().toString() + "]", e);
            }
        }

        //Create a temporary lock file to signify that we're in the process of updating the playlist
        if(!Files.exists(getIconLockFilePath()))
        {
            try
            {
                Files.createFile(getIconLockFilePath());
            }
            catch(IOException e)
            {
                mLog.error("Error creating temporary lock file prior to saving icons [" +
                    getIconLockFilePath().toString() + "]", e);
            }
        }

        try(OutputStream out = Files.newOutputStream(getIconFilePath()))
        {
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(out, iconSet);
            out.flush();

            //Remove the playlist lock file to indicate that we successfully saved the file
            if(Files.exists(getIconLockFilePath()))
            {
                Files.delete(getIconLockFilePath());
            }
        }
        catch(IOException ioe)
        {
            mLog.error("IO error while writing icons to a file [" + getIconFilePath().toString() + "]", ioe);
        }
        catch(Exception e)
        {
            mLog.error("Error while saving icons [" + getIconFilePath().toString() + "]", e);
        }
    }

    /**
     * Loads icons from file or creates a default set of icons
     */
    public IconSet load()
    {
        mLog.info("loading icons file [" + getIconFilePath().toString() + "]");

        IconSet iconSet = null;

        //Check for a lock file that indicates the previous save attempt was incomplete or had an error
        if(Files.exists(getIconLockFilePath()))
        {
            try
            {
                //Remove the previous icons file
                Files.delete(getIconFilePath());

                //Copy the backup file to restore the previous icons file
                if(Files.exists(getIconBackupFilePath()))
                {
                    Files.copy(getIconBackupFilePath(), getIconFilePath());
                }

                //Remove the lock file
                Files.delete(getIconLockFilePath());
            }
            catch(IOException ioe)
            {
                mLog.error("Previous icons save attempt was incomplete and there was an error restoring the " +
                    "icons backup file", ioe);
            }
        }

        if(Files.exists(getIconFilePath()))
        {
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule);

            try(InputStream in = Files.newInputStream(getIconFilePath()))
            {
                iconSet = objectMapper.readValue(in, IconSet.class);
            }
            catch(IOException ioe)
            {
                mLog.error("IO error while reading icons file", ioe);
            }
        }
        else
        {
            mLog.info("Icons file not found at [" + getIconFilePath().toString() + "]");
        }

        return iconSet;
    }

    /**
     * Creates a default icon set
     */
    private IconSet getStandardIconSet()
    {
        if(mStandardIcons == null)
        {
            mStandardIcons = new IconSet();

            Icon defaultIcon = new Icon(DEFAULT_ICON, "images/no_icon.png");
            mStandardIcons.add(defaultIcon);
            mStandardIcons.setDefaultIcon(defaultIcon.getName());

            mStandardIcons.add(new Icon("Ambulance", "images/ambulance.png"));
            mStandardIcons.add(new Icon("Block Truck", "images/concrete_block_truck.png"));
            mStandardIcons.add(new Icon("CWID", "images/cwid.png"));
            mStandardIcons.add(new Icon("Dispatcher", "images/dispatcher.png"));
            mStandardIcons.add(new Icon("Dump Truck", "images/dump_truck_red.png"));
            mStandardIcons.add(new Icon("Fire Truck", "images/fire_truck.png"));
            mStandardIcons.add(new Icon("Garbage Truck", "images/garbage_truck.png"));
            mStandardIcons.add(new Icon("Loader", "images/loader.png"));
            mStandardIcons.add(new Icon("Police", "images/police.png"));
            mStandardIcons.add(new Icon("Propane Truck", "images/propane_truck.png"));
            mStandardIcons.add(new Icon("Rescue Truck", "images/rescue_truck.png"));
            mStandardIcons.add(new Icon("School Bus", "images/school_bus.png"));
            mStandardIcons.add(new Icon("Taxi", "images/taxi.png"));
            mStandardIcons.add(new Icon("Train", "images/train.png"));
            mStandardIcons.add(new Icon("Transport Bus", "images/opt_bus.png"));
            mStandardIcons.add(new Icon("Van", "images/van.png"));
        }

        return mStandardIcons;
    }

    /**
     * Schedules an icon file save task.  Subsequent calls to this method will be ignored until the save event occurs,
     * thus limiting repetitive saving to a minimum.
     */
    private void scheduleSave()
    {
        if(mSavingIcons.compareAndSet(false, true))
        {
            ThreadPool.SCHEDULED.schedule(new IconSaveTask(), 2, TimeUnit.SECONDS);
        }
    }

    /**
     * Resets the playlist save pending flag to false and proceeds to save the playlist.
     */
    public class IconSaveTask implements Runnable
    {
        @Override
        public void run()
        {
            save();

            mSavingIcons.set(false);
        }
    }
}

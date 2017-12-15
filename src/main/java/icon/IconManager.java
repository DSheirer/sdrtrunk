/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package icon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import util.ThreadPool;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IconManager
{
    private final static Logger mLog = LoggerFactory.getLogger(IconManager.class);
    public static final int DEFAULT_ICON_SIZE = 12;

    private Path mIconFolderPath;
    private Path mIconFilePath;
    private Path mIconBackupFilePath;
    private Path mIconLockFilePath;

    private IconEditor mIconEditor;
    private IconTableModel mIconTableModel;
    private AtomicBoolean mSavingIcons = new AtomicBoolean();

    private Map<String,ImageIcon> mResizedIcons = new HashMap<>();

    public IconManager()
    {
    }

    /**
     * Display the icon editor centered over the specified component.
     *
     * @param centerOnComponent to center the editor
     */
    public void showEditor(Component centerOnComponent)
    {
        if(mIconEditor == null)
        {
            mIconEditor = new IconEditor(this);
        }

        mIconEditor.setLocationRelativeTo(centerOnComponent);

        if(mIconEditor.isVisible())
        {
            mIconEditor.requestFocus();
        }
        else
        {
            EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    mIconEditor.setVisible(true);
                }
            });
        }
    }

    public IconTableModel getModel()
    {
        if(mIconTableModel == null)
        {
            IconSet loadedIcons = load();

            boolean saveRequired = false;

            if(loadedIcons == null)
            {
                loadedIcons = getDefaultIconSet();
                saveRequired = true;
            }

            mIconTableModel = new IconTableModel(loadedIcons);
            mIconTableModel.addTableModelListener(new TableModelListener()
            {
                @Override
                public void tableChanged(TableModelEvent e)
                {
                    scheduleSave();
                }
            });

            if(saveRequired)
            {
                scheduleSave();
            }
        }

        return mIconTableModel;
    }

    /**
     * All icons in a sorted array
     */
    public Icon[] getIcons()
    {
        return getModel().getIconsAsArray();
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
            name = getModel().getDefaultIcon().getName();
        }

        String scaledIconName = name + height;

        if(mResizedIcons.containsKey(scaledIconName))
        {
            return mResizedIcons.get(scaledIconName);
        }

        Icon icon = getModel().getIcon(name);

        ImageIcon scaledIcon = getScaledIcon(icon.getIcon(), height);
        mResizedIcons.put(scaledIconName, scaledIcon);

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
        double scale = (double) original.getIconHeight() / (double) height;

        int scaledWidth = (int) ((double) original.getIconWidth() / scale);

        Image scaledImage = original.getImage().getScaledInstance(scaledWidth,
            height, java.awt.Image.SCALE_SMOOTH);

        return new ImageIcon(scaledImage);
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
        IconSet iconSet = getModel().getIconSet();

        JAXBContext context = null;

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
            context = JAXBContext.newInstance(IconSet.class);

            Marshaller m = context.createMarshaller();

            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            m.marshal(iconSet, out);

            out.flush();

            //Remove the playlist lock file to indicate that we successfully saved the file
            if(Files.exists(getIconLockFilePath()))
            {
                Files.delete(getIconLockFilePath());
            }
        }
        catch(JAXBException je)
        {
            mLog.error("JAXB exception while serializing icons to a file [" + getIconFilePath().toString() + "]", je);
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
            JAXBContext context = null;

            try(InputStream in = Files.newInputStream(getIconFilePath()))
            {
                context = JAXBContext.newInstance(IconSet.class);

                Unmarshaller m = context.createUnmarshaller();

                iconSet = (IconSet) m.unmarshal(in);
            }
            catch(JAXBException je)
            {
                mLog.error("JAXB exception while loading/unmarshalling icons", je);
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
    private IconSet getDefaultIconSet()
    {
        IconSet iconSet = new IconSet();

        Icon defaultIcon = new Icon(IconTableModel.DEFAULT_ICON, "images/no_icon.png");
        iconSet.add(defaultIcon);
        iconSet.setDefaultIcon(defaultIcon.getName());

        iconSet.add(new Icon("Ambulance", "images/ambulance.png"));
        iconSet.add(new Icon("Block Truck", "images/concrete_block_truck.png"));
        iconSet.add(new Icon("CWID", "images/cwid.png"));
        iconSet.add(new Icon("Dispatcher", "images/dispatcher.png"));
        iconSet.add(new Icon("Dump Truck", "images/dump_truck_red.png"));
        iconSet.add(new Icon("Fire Truck", "images/fire_truck.png"));
        iconSet.add(new Icon("Garbage Truck", "images/garbage_truck.png"));
        iconSet.add(new Icon("Loader", "images/loader.png"));
        iconSet.add(new Icon("Police", "images/police.png"));
        iconSet.add(new Icon("Propane Truck", "images/propane_truck.png"));
        iconSet.add(new Icon("Rescue Truck", "images/rescue_truck.png"));
        iconSet.add(new Icon("School Bus", "images/school_bus.png"));
        iconSet.add(new Icon("Taxi", "images/taxi.png"));
        iconSet.add(new Icon("Train", "images/train.png"));
        iconSet.add(new Icon("Transport Bus", "images/opt_bus.png"));
        iconSet.add(new Icon("Van", "images/van.png"));

        return iconSet;
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

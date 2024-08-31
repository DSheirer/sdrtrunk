/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.properties;

import io.github.dsheirer.gui.SDRTrunk;
import io.github.dsheirer.util.ThreadPool;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SystemProperties - provides an isolated instance of properties for the application
 */
public class SystemProperties
{
    private final static Logger mLog = LoggerFactory.getLogger(SystemProperties.class);

    private static String DEFAULT_APP_ROOT = "SDRTrunk";
    private static String PROPERTIES_FILENAME = "SDRTrunk.properties";
    private static String MANIFEST_VERSION = "Implementation-Version";
    private static String BUILD_TIMESTAMP = "Build-Timestamp";

    private static SystemProperties INSTANCE;
    private static Properties mProperties;
    private Path mPropertiesPath;
    private String mApplicationName;
    private AtomicBoolean mSavePending = new AtomicBoolean();

    private SystemProperties()
    {
        mProperties = new Properties();
    }

    /**
     * Returns a SINGLETON instance of the application properties set
     *
     * @return
     */
    public static SystemProperties getInstance()
    {
        if(INSTANCE == null)
        {
            INSTANCE = new SystemProperties();
        }

        return INSTANCE;
    }

    /**
     * Saves any currently changed settings to the application properties file
     */
    private void save()
    {
        if(mSavePending.compareAndSet(false, true))
        {
            ThreadPool.SCHEDULED.schedule(new SavePropertiesTask(), 2, TimeUnit.SECONDS);
        }
    }

    /**
     * Get the Manifest for the jar that contains the specified class
     * @param clz contained in a jar
     * @return manifest from the containing jar
     */
    public static Manifest getManifest(Class<?> clz)
    {
        String resource = "/" + clz.getName().replace(".", "/") + ".class";
        String fullPath = clz.getResource(resource).toString();
        String archivePath = fullPath.substring(0, fullPath.length() - resource.length());
        if (archivePath.endsWith("\\WEB-INF\\classes") || archivePath.endsWith("/WEB-INF/classes")) {
            archivePath = archivePath.substring(0, archivePath.length() - "/WEB-INF/classes".length()); // Required for wars
        }

        try (InputStream input = new URL(archivePath + "/META-INF/MANIFEST.MF").openStream())
        {
            return new Manifest(input);
        }
        catch (Exception e)
        {
            mLog.error("Unable to load jar manifest - we're probably not running from a release jar");
        }

        return null;
    }

    /**
     * Application root directory.  Normally returns "SDRTRunk" from the user's
     * home directory, unless that has been changed to another location on the
     * file system by the user.
     */
    public Path getApplicationRootPath()
    {
        Path retVal = null;

        String root = get("root.directory", DEFAULT_APP_ROOT);

        if(root.equalsIgnoreCase(DEFAULT_APP_ROOT))
        {
            retVal = Paths.get(System.getProperty("user.home"), DEFAULT_APP_ROOT);
        }
        else
        {
            retVal = Paths.get(root);
        }

        if(!Files.exists(retVal))
        {
            try
            {
                mLog.info("Creating application root folder: " + retVal);
                Files.createDirectory(retVal);
            }
            catch(IOException e)
            {
                mLog.error("Error creating sdrtrunk application root folder", e);
            }
        }

        return retVal;
    }

    public Path getApplicationFolder(String folder)
    {
        Path retVal = getApplicationRootPath().resolve(folder);

        if(!Files.exists(retVal))
        {
            try
            {
                Files.createDirectory(retVal);
            }
            catch(IOException e)
            {
                mLog.error("SystemProperties - exception while creating app folder [" + folder + "]", e);
            }
        }

        return retVal;
    }

    public void logCurrentSettings()
    {
        if(mPropertiesPath == null)
        {
            mLog.info("SystemProperties - no properties file loaded - using defaults");
        }
    }

    /**
     * Loads the properties file into this properties set
     */
    public void load(Path propertiesPath)
    {
        if(propertiesPath != null)
        {
            mPropertiesPath = propertiesPath;

            if(Files.exists(mPropertiesPath))
            {
                try(InputStream in = new FileInputStream(propertiesPath.toString()))
                {
                    mProperties.load(in);
                }
                catch(IOException ioe)
                {
                    mLog.error("Error loading system properties file", ioe.getMessage());
                }
            }
        }

        mLog.info("SystemProperties - loaded [" + propertiesPath.toString() + "]");
    }

    /**
     * Creates the application name, suitable for display in the title bar of the gui
     */
    public String getApplicationName()
    {
        if(mApplicationName == null)
        {
            StringBuilder sb = new StringBuilder();

            sb.append("sdrtrunk");

            Manifest manifest = getManifest(SDRTrunk.class);

            if(manifest != null)
            {
                String version = manifest.getMainAttributes().getValue(MANIFEST_VERSION);
                String timestamp = manifest.getMainAttributes().getValue(BUILD_TIMESTAMP);

                if(version != null)
                {
                    if(version.contains("nightly") && timestamp != null)
                    {
                        sb.append( " nightly - ").append(timestamp);
                    }
                    else
                    {
                        sb.append(" v");
                        sb.append(version);
                    }
                }
            }

            mApplicationName = sb.toString();
        }

        return mApplicationName;
    }

    /**
     * Returns the value of the property, or null if the property doesn't exist
     */
    private String get(String key)
    {
        return mProperties.getProperty(key);
    }

    /**
     * Returns the value of the property, or the defaultValue if the
     * property doesn't exist
     */
    public String get(String key, String defaultValue)
    {
        String value = get(key);

        if(value != null)
        {
            return value;
        }

        set(key, defaultValue);

        return defaultValue;
    }

    /**
     * Returns the value of the property, or the defaultValue if the
     * property doesn't exist
     */
    public boolean get(String key, boolean defaultValue)
    {
        String value = get(key);

        if(value != null)
        {
            try
            {
                boolean stored = Boolean.parseBoolean(value);

                return stored;
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the stored value
            }
        }

        set(key, String.valueOf(defaultValue));

        return defaultValue;
    }

    /**
     * Returns the value of the property, or the defaultValue if the
     * property doesn't exist
     */
    public int get(String key, int defaultValue)
    {
        String value = get(key);

        if(value != null)
        {
            try
            {
                int stored = Integer.parseInt(value);

                return stored;
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the stored value
            }
        }

        set(key, String.valueOf(defaultValue));

        return defaultValue;
    }

    /**
     * Sets (overrides) the property key with the new value
     */
    public void set(String key, String value)
    {
        mProperties.setProperty(key, value);

        save();
    }

    /**
     * Sets (overrides) the property key with the new boolean value
     */
    public void set(String key, boolean value)
    {
        set(key, String.valueOf(value));
    }

    /**
     * Sets (overrides) the property key with the new integer value
     */
    public void set(String key, int value)
    {
        set(key, String.valueOf(value));
    }

    /**
     * Gets the named color property, storing the default value in properties file if it doesn't exist.
     *
     * @param key identifying the color property
     * @param defaultColor to use for creating the property if it doesn't exist
     * @return named color
     */
    public Color get(String key, Color defaultColor)
    {
        return new Color(get(key, defaultColor.getRGB()), true);
    }

    /**
     * Creates a new color instance with the RGB values from color argument and translucency (alpha) argument
     * @param color containing RGB values
     * @param alpha desired translucency/alpha value
     * @return new color with adjusted alpha value
     */
    public static Color getTranslucent(Color color, int alpha)
    {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public class SavePropertiesTask implements Runnable
    {
        @Override
        public void run()
        {
            if(mSavePending.compareAndSet(true, false))
            {
                Path propsPath = getApplicationRootPath().resolve(PROPERTIES_FILENAME);

                try(OutputStream out = new FileOutputStream(propsPath.toString()))
                {
                    String comments = "SDRTrunk - SDR Trunking Decoder Application Settings";
                    mProperties.store(out, comments);
                }
                catch(IOException ioe)
                {
                    mLog.error("Error saving system properties file [" + propsPath.toString() + "]", ioe);
                }
            }
        }
    }
}

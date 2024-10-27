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

package io.github.dsheirer.audio.codec.mbe;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.AbstractAudioModule;
import io.github.dsheirer.audio.squelch.ISquelchStateListener;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import jmbe.iface.IAudioCodec;
import jmbe.iface.IAudioCodecLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base audio module for protocols that use the JMBE audio codec.
 */
public abstract class JmbeAudioModule extends AbstractAudioModule implements Listener<IMessage>, IMessageListener,
    ISquelchStateListener
{
    private static final Logger mLog = LoggerFactory.getLogger(JmbeAudioModule.class);
    private static final String JMBE_AUDIO_LIBRARY = "JMBE";
    private static final List<String> mLibraryLoadStatusLogged = new ArrayList<>();
    private IAudioCodec mAudioCodec;
    private final UserPreferences mUserPreferences;
    private static Class sLoadedJmbeAudioConverterClass;

    public JmbeAudioModule(UserPreferences userPreferences, AliasList aliasList, int timeslot)
    {
        super(aliasList, timeslot, DEFAULT_SEGMENT_AUDIO_SAMPLE_LENGTH);
        mUserPreferences = userPreferences;
        MyEventBus.getGlobalEventBus().register(this);
        loadConverter();
    }

    @Override
    protected void closeAudioSegment()
    {
        super.closeAudioSegment();

        //Reset the audio codec to clear any leftover frame data from the previous call.
        if(mAudioCodec != null)
        {
            mAudioCodec.reset();
        }
    }

    @Override
    public void dispose()
    {
        super.dispose();
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    public IAudioCodec getAudioCodec()
    {
        return mAudioCodec;
    }

    /**
     * Indicates that the JMBE audio library has been loaded and a suitable audio codec is usable (ie non-null)
     */
    protected boolean hasAudioCodec()
    {
        return getAudioCodec() != null;
    }

    @Override
    public Listener<IMessage> getMessageListener()
    {
        return this;
    }

    /**
     * Receives notifications that the JMBE library preference has been updated via the Guava event bus
     *
     * @param preferenceType that was updated
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.JMBE_LIBRARY)
        {
            mLibraryLoadStatusLogged.clear();
            loadConverter();
        }
    }

    /**
     * Name of the CODEC to use from the JMBE library
     */
    protected abstract String getCodecName();

    /**
     * Loads JMBE audio converter library class and then instantiates new converter instances from the loaded class.
     */
    protected void loadConverter()
    {
        IAudioCodec audioConverter = null;

        if(sLoadedJmbeAudioConverterClass == null)
        {
            Path path = mUserPreferences.getJmbeLibraryPreference().getPathJmbeLibrary();

            if(path != null)
            {
                if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                {
                    mLog.info("Loading JMBE library from [" + path + "]");
                }

                try
                {
                    URLClassLoader childClassLoader = new URLClassLoader(new URL[]{path.toUri().toURL()},
                            this.getClass().getClassLoader());

                    sLoadedJmbeAudioConverterClass = Class.forName("jmbe.JMBEAudioLibrary", true, childClassLoader);
                }
                catch(IllegalArgumentException iae)
                {
                    if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY + getCodecName()))
                    {
                        mLog.error("Couldn't load JMBE audio conversion library - " + iae.getMessage());
                        mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY + getCodecName());
                    }
                }
                catch(MalformedURLException mue)
                {
                    if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                    {
                        mLog.error("Couldn't load JMBE audio conversion library from path [" + path + "]");
                        mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                    }
                }
                catch(ClassNotFoundException e1)
                {
                    if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                    {
                        mLog.error("Couldn't load JMBE audio conversion library - class not found");
                        mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                    }
                }
            }
        }

        if(sLoadedJmbeAudioConverterClass != null)
        {
            try
            {
                Object instance = sLoadedJmbeAudioConverterClass.getDeclaredConstructor().newInstance();

                if(instance instanceof IAudioCodecLibrary library)
                {
                    if((library.getMajorVersion() == 1 && library.getMinorVersion() >= 0 &&
                            library.getBuildVersion() >= 0) || library.getMajorVersion() >= 1)
                    {
                        audioConverter = library.getAudioConverter(getCodecName());

                        if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                        {
                            mLog.info("JMBE audio conversion library loaded: " + library.getVersion());
                            mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                        }
                    }
                    else
                    {
                        if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                        {
                            mLog.warn("JMBE library version 1.0.0 or higher is required - found: " + library.getVersion());
                            mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                        }
                    }
                }
                else
                {
                    if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                    {
                        mLog.info("JMBE audio conversion library NOT FOUND");
                        mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                    }
                }
            }
            catch(InvocationTargetException ite)
            {
                if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                {
                    mLog.error("Couldn't load JMBE audio conversion library - invocation target exception", ite);
                    mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                }
            }
            catch(InstantiationException e1)
            {
                if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                {
                    mLog.error("Couldn't load JMBE audio conversion library - instantiation exception", e1);
                    mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                }
            }
            catch(IllegalAccessException e1)
            {
                if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                {
                    mLog.error("Couldn't load JMBE audio conversion library - security restrictions");
                    mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                }
            }
            catch(NoSuchMethodException nsme)
            {
                if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                {
                    mLog.error("Couldn't load JMBE audio conversion library - no such method exception");
                    mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                }
            }
        }
        else
        {
            if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
            {
                mLog.warn("JMBE audio library path is NOT SET in your User Preferences.");
                mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
            }
        }

        mAudioCodec = audioConverter;
    }
}

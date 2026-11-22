/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.sample.Listener;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jmbe.frame.ICorrectedFrame;
import jmbe.iface.IAudioCodec;
import jmbe.iface.IAudioCodecLibrary;
import jmbe.iface.IAudioCodecLibraryV2;
import jmbe.iface.IAudioCodecLibraryV3;
import jmbe.iface.IAudioCodecV2;
import jmbe.iface.IAudioCodecV3;
import jmbe.iface.IAudioWithMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base audio module for protocols that use the JMBE audio codec.
 */
public abstract class JmbeAudioModule extends AbstractAudioModule implements Listener<IMessage>, IMessageListener,
    ISquelchStateListener
{
    private static final Logger LOG = LoggerFactory.getLogger(JmbeAudioModule.class);
    private static final String JMBE_AUDIO_LIBRARY = "JMBE";

    /**
     * JMBE library entry point class names, newest first.  The loader uses the newest version that the library
     * provides, falling back to older versions for libraries created by older JMBE releases.
     */
    private static final List<String> JMBE_LIBRARY_CLASS_NAMES = List.of("jmbe.JMBEAudioLibraryV3",
        "jmbe.JMBEAudioLibraryV2", "jmbe.JMBEAudioLibrary");
    private static final List<String> mLibraryLoadStatusLogged = new ArrayList<>();
    private IAudioCodec mAudioCodec;
    private final Settings mSettings;
    private static Class sLoadedJmbeAudioConverterClass;

    public JmbeAudioModule(Settings settings, AliasList aliasList, int timeslot)
    {
        super(aliasList, timeslot, DEFAULT_MAX_SEGMENT_LENGTH_MILLIS);
        mSettings = settings;
        MyEventBus.getGlobalEventBus().register(this);
        loadConverter();
    }

    /**
     * List of features supported by the JMBE audio library.  Note: features are only supported in Version 2.0+
     * @return list of features or an empty list if the current library version doesn't support features.
     */
    public List<String> getFeatures()
    {
        if(mAudioCodec instanceof IAudioCodecV2 v2)
        {
            return v2.getFeatures();
        }

        return Collections.emptyList();
    }

    /**
     * Sets the JMBE feature
     * @param name of the feature
     * @param value for the feature
     */
    public void setFeature(String name, Object value)
    {
        if(mAudioCodec instanceof IAudioCodecV2 v2)
        {
            v2.setFeature(name, value);
        }
        else
        {
            LOG.info("Feature [" + name + "] is not supported by the current JMBE audio codec library version");
        }
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

    /**
     * Indicates if the loaded audio codec can synthesize audio from a voice frame that sdrtrunk has already error
     * corrected.  Only JMBE version 3 libraries support this.  Version 1 and 2 libraries only accept the raw voice
     * frame bytes and perform the error detection and correction themselves.
     *
     * Callers can use this to skip creating corrected frames when the codec can't use them.
     */
    protected boolean isCorrectedFrameSupported()
    {
        return mAudioCodec instanceof IAudioCodecV3;
    }

    /**
     * Synthesizes audio for the voice frame.  When the codec supports it (JMBE version 3), the audio is synthesized
     * from the corrected frame so the codec doesn't repeat the error detection and correction.  Otherwise, the raw
     * frame bytes are passed to the codec (JMBE version 1 and 2).
     *
     * @param frame bytes for the voice frame as transmitted
     * @param correctedFrame for the same voice frame, or null to let the codec perform the error correction
     * @return audio samples
     */
    protected float[] getAudio(byte[] frame, ICorrectedFrame correctedFrame)
    {
        if(correctedFrame != null && mAudioCodec instanceof IAudioCodecV3 v3)
        {
            return v3.getAudio(correctedFrame);
        }

        return mAudioCodec.getAudio(frame);
    }

    /**
     * Synthesizes audio with optional metadata (tones, DTMF, etc.) for the voice frame.  When the codec supports it
     * (JMBE version 3), the audio is synthesized from the corrected frame so the codec doesn't repeat the error
     * detection and correction.  Otherwise, the raw frame bytes are passed to the codec (JMBE version 1 and 2).
     *
     * @param frame bytes for the voice frame as transmitted
     * @param correctedFrame for the same voice frame, or null to let the codec perform the error correction
     * @return audio samples with optional metadata
     */
    protected IAudioWithMetadata getAudioWithMetadata(byte[] frame, ICorrectedFrame correctedFrame)
    {
        if(correctedFrame != null && mAudioCodec instanceof IAudioCodecV3 v3)
        {
            return v3.getAudioWithMetadata(correctedFrame);
        }

        return mAudioCodec.getAudioWithMetadata(frame);
    }

    @Override
    public Listener<IMessage> getMessageListener()
    {
        return this;
    }

    /**
     * Receives notifications that the JMBE library preference has been updated via the Guava event bus
     *
     * @param settingsType that was updated
     */
    @Subscribe
    public void settingsUpdated(SettingsType settingsType)
    {
        if(settingsType == SettingsType.JMBE_LIBRARY)
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
            Path path = mSettings.getJmbeLibrarySettings().getPathJmbeLibrary();

            if(path != null)
            {
                if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                {
                    LOG.info("Loading JMBE library from [" + path + "]");
                }

                try
                {
                    URLClassLoader childClassLoader = new URLClassLoader(new URL[]{path.toUri().toURL()},
                            this.getClass().getClassLoader());

                    for(String className: JMBE_LIBRARY_CLASS_NAMES)
                    {
                        try
                        {
                            sLoadedJmbeAudioConverterClass = Class.forName(className, true, childClassLoader);
                            break;
                        }
                        catch(ClassNotFoundException notFound)
                        {
                            //Library doesn't contain this version of the library class - try the next older version
                        }
                    }

                    if(sLoadedJmbeAudioConverterClass == null && !mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                    {
                        LOG.error("Couldn't load JMBE audio conversion library from [" + path +
                            "] - none of the library classes " + JMBE_LIBRARY_CLASS_NAMES + " were found");
                        mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                    }
                }
                catch(IllegalArgumentException iae)
                {
                    if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY + getCodecName()))
                    {
                        LOG.error("Couldn't load JMBE audio conversion library - " + iae.getMessage());
                        mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY + getCodecName());
                    }
                }
                catch(MalformedURLException mue)
                {
                    if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                    {
                        LOG.error("Couldn't load JMBE audio conversion library from path [" + path + "]");
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
                        if(library instanceof IAudioCodecLibraryV3 libraryV3)
                        {
                            audioConverter = libraryV3.getAudioConverterV3(getCodecName());
                        }
                        else if(library instanceof IAudioCodecLibraryV2 libraryV2)
                        {
                            audioConverter = libraryV2.getAudioConverterV2(getCodecName());
                        }
                        else
                        {
                            audioConverter = library.getAudioConverter(getCodecName());
                        }

                        String codecStatusKey = JMBE_AUDIO_LIBRARY + ":codec:" + getCodecName();
                        if(!mLibraryLoadStatusLogged.contains(codecStatusKey))
                        {
                            LOG.info("JMBE codec [" + getCodecName() + "] class [" +
                                audioConverter.getClass().getName() + "] features [" +
                                (audioConverter instanceof IAudioCodecV2 v2 ?
                                    v2.getFeatures() : Collections.emptyList()) + "]");
                            mLibraryLoadStatusLogged.add(codecStatusKey);
                        }

                        if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                        {
                            LOG.info("JMBE audio conversion library loaded: " + library.getVersion());
                            mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                        }
                    }
                    else
                    {
                        if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                        {
                            LOG.warn("JMBE library version 1.0.0 or higher is required - found: " + library.getVersion());
                            mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                        }
                    }
                }
                else
                {
                    if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                    {
                        LOG.info("JMBE audio conversion library NOT FOUND");
                        mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                    }
                }
            }
            catch(InvocationTargetException ite)
            {
                if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                {
                    LOG.error("Couldn't load JMBE audio conversion library - invocation target exception", ite);
                    mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                }
            }
            catch(InstantiationException e1)
            {
                if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                {
                    LOG.error("Couldn't load JMBE audio conversion library - instantiation exception", e1);
                    mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                }
            }
            catch(IllegalAccessException e1)
            {
                if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                {
                    LOG.error("Couldn't load JMBE audio conversion library - security restrictions");
                    mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                }
            }
            catch(NoSuchMethodException nsme)
            {
                if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
                {
                    LOG.error("Couldn't load JMBE audio conversion library - no such method exception");
                    mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
                }
            }
        }
        else
        {
            if(!mLibraryLoadStatusLogged.contains(JMBE_AUDIO_LIBRARY))
            {
                LOG.warn("JMBE audio library path is NOT SET in your Settings.");
                mLibraryLoadStatusLogged.add(JMBE_AUDIO_LIBRARY);
            }
        }

        mAudioCodec = audioConverter;
    }
}

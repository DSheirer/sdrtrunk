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

package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.mixer.MixerManager;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.MultiFrequencyTunerChannelSource;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.HotplugCallback;
import org.usb4java.HotplugCallbackHandle;
import org.usb4java.LibUsb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tuner manager provides access to tuners using USB, recording, sound-card and system-daemon accessible devices. This
 * manager also supports hot-plug detection and black-listing of discovered tuners so that they can be used with other
 * software applications.
 */
public class TunerManager implements IDiscoveredTunerStatusListener
{
    private static final Logger mLog = LoggerFactory.getLogger(TunerManager.class);
    private static final int MAXIMUM_USB_2_DATA_RATE = 480000000;

    private UserPreferences mUserPreferences;
    private DiscoveredTunerModel mDiscoveredTunerModel = new DiscoveredTunerModel();
    private TunerConfigurationManager mTunerConfigurationManager;
    private HotplugEventSupport mHotplugEventSupport = new HotplugEventSupport();
    private Context mLibUsbApplicationContext = new Context();
    private boolean mLibUsbInitialized = false;

    /**
     * Constructs an instance
     * @param userPreferences for preferences
     */
    public TunerManager(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mTunerConfigurationManager = new TunerConfigurationManager(userPreferences);
    }

    /**
     * Discovered tuner model
     */
    public DiscoveredTunerModel getDiscoveredTunerModel()
    {
        return mDiscoveredTunerModel;
    }

    /**
     * Starts the tuner manager and loads all available tuners
     */
    public void start()
    {
        mLog.info("LibUSB API Version: " + LibUsb.getApiVersion());
        mLog.info("LibUSB Version: " + LibUsb.getVersion());

        int status = LibUsb.init(mLibUsbApplicationContext);

        if(status == LibUsb.SUCCESS)
        {
            mLibUsbInitialized = true;
//            LibUsb.setOption(mLibUsbApplicationContext, LibUsb.OPTION_LOG_LEVEL, LibUsb.LOG_LEVEL_DEBUG);
            mHotplugEventSupport.start();

            discoverUSBTuners();
        }

        discoverSdrPlayTuners();
        discoverSoundCardTuners();
        discoverRecordingTuners();
    }

    /**
     * Stops or shuts down the tuner manager and releases all devices
     */
    public void stop()
    {
        //Stop all tuners
        mDiscoveredTunerModel.releaseDiscoveredTuners();

        //Shutdown LibUsb
        if(mLibUsbInitialized)
        {
            mHotplugEventSupport.stop();
            LibUsb.exit(mLibUsbApplicationContext);
            mLibUsbInitialized = false;
        }
    }

    /**
     * Requests to save the current state of the tuner configurations
     */
    public void saveConfigurations()
    {
        mTunerConfigurationManager.saveConfigurations();
    }

    /**
     * Discover or rediscovers USB tuners
     */
    private void discoverUSBTuners()
    {
        if(mLibUsbInitialized)
        {
            try
            {
                DeviceList deviceList = new DeviceList();
                int deviceCount = LibUsb.getDeviceList(mLibUsbApplicationContext, deviceList);

                if(deviceCount >= 0)
                {
                    for(Device device: deviceList)
                    {
                        addUsbTuner(device);
                    }
                }

                LibUsb.freeDeviceList(deviceList, true);
            }
            catch(Exception e)
            {
                mLog.error("LibUsb - error during USB device discovery", e);
            }
        }
    }

    /**
     * Determines if the USB device is a supported tuner and add if it has not already been added/discovered.
     * @param device to inspect and optionally add as a discovered tuner
     */
    private void addUsbTuner(Device device)
    {
        int bus = LibUsb.getBusNumber(device);
        int port = LibUsb.getPortNumber(device);

        DeviceDescriptor deviceDescriptor = new DeviceDescriptor();
        int status = LibUsb.getDeviceDescriptor(device, deviceDescriptor);

        if(status == LibUsb.SUCCESS)
        {
            TunerClass tunerClass = TunerClass.lookup(deviceDescriptor.idVendor(), deviceDescriptor.idProduct());

            if(tunerClass.isSupportedUsbTuner() && !mDiscoveredTunerModel.hasUsbTuner(bus, port))
            {
                mLog.info("Adding USB Bus [" + bus + "] Port [" + port + "] ID [" +
                    String.format("%04X", deviceDescriptor.idVendor()) + ":" +
                    String.format("%04X", deviceDescriptor.idProduct()) + "] Tuner Class [" + tunerClass + "]");
                ChannelizerType channelizerType = mUserPreferences.getTunerPreference().getChannelizerType();
                DiscoveredUSBTuner discoveredUSBTuner = new DiscoveredUSBTuner(tunerClass, bus, port, channelizerType);
                discoveredUSBTuner.addTunerStatusListener(this);

                //Set the tuner to disabled if the user has previously blacklisted the tuner
                if(mTunerConfigurationManager.isDisabled(discoveredUSBTuner))
                {
                    discoveredUSBTuner.setEnabled(false);
                }
                else
                {
                    //Attempt to start the discovered tuner so we can determine the tuner type
                    discoveredUSBTuner.start();

                    if(discoveredUSBTuner.hasTuner())
                    {
                        TunerType tunerType = discoveredUSBTuner.getTuner().getTunerType();

                        TunerConfiguration tunerConfiguration = mTunerConfigurationManager
                                .getTunerConfiguration(tunerType, discoveredUSBTuner.getId());

                        if(tunerConfiguration != null)
                        {
                            mLog.info("Applying tuner configuration to " + discoveredUSBTuner.getId());
                            discoveredUSBTuner.setTunerConfiguration(tunerConfiguration);
                            mTunerConfigurationManager.saveConfigurations();
                        }
                    }
                }

                mDiscoveredTunerModel.addDiscoveredTuner(discoveredUSBTuner);
                mLog.info("Tuner Added: " + discoveredUSBTuner);
            }
        }
        else
        {
            mLog.error("LibUsb - unable to get device descriptor for device on bus [" + bus +
                    "] port [" + port + "] - status [" + status + "] - " + LibUsb.errorName(status));
        }
    }

    /**
     * Stops and removes the USB tuner if it is currently discovered.
     * @param bus usb for the tuner
     * @param port usb for the tuner
     */
    private void removeUsbTuner(int bus, int port)
    {
        mDiscoveredTunerModel.removeUsbTuner(bus, port);
    }

    /**
     * Discover sound card tuners like the Funcube Dongle (Pro)
     */
    private void discoverSoundCardTuners()
    {

    }

    /**
     * Discover SDRPlay RSP tuners
     */
    private void discoverSdrPlayTuners()
    {
        //placeholder ...
    }

    /**
     * Discover recording based tuners
     */
    private void discoverRecordingTuners()
    {
        //placeholder
    }

    /**
     * Handles tuner status change events.  Events are sent to the tuner configuration manager so that it can save
     * configuration updates and events are also monitored to detect when a user changes the tuner state of a tuner
     * so that we can auto-start the tuner and apply a tuner configuration.
     *
     * @param discoveredTuner that has a status change.
     * @param previous tuner status
     * @param current tuner status
     */
    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        mTunerConfigurationManager.tunerStatusUpdated(discoveredTuner, previous, current);

        if(previous != TunerStatus.ENABLED && current == TunerStatus.ENABLED)
        {
            discoveredTuner.start();

            if(discoveredTuner.hasTuner())
            {
                TunerType tunerType = discoveredTuner.getTuner().getTunerType();

                TunerConfiguration tunerConfiguration = mTunerConfigurationManager
                        .getTunerConfiguration(tunerType, discoveredTuner.getId());

                if(tunerConfiguration != null)
                {
                    discoveredTuner.setTunerConfiguration(tunerConfiguration);
                    mTunerConfigurationManager.saveConfigurations();
                }
            }
        }
    }

    /**
     * Find a tuner that matches the name argument
     *
     * @param preferredTunerName of the tuner
     * @return named tuner or null
     */
    public DiscoveredTuner getDiscoveredTuner(String preferredTunerName)
    {
        if(preferredTunerName != null)
        {
            for(DiscoveredTuner discoveredTuner : getDiscoveredTunerModel().getAvailableTuners())
            {
                if(discoveredTuner.isAvailable() &&
                   discoveredTuner.getTuner().getPreferredName().equalsIgnoreCase(preferredTunerName))
                {
                    return discoveredTuner;
                }
            }
        }

        return null;
    }

    /**
     * Sorted list of preferred tuner names
     */
    public List<String> getPreferredTunerNames()
    {
        List<String> preferredNames = new ArrayList<>();
        List<DiscoveredTuner> availableTuners = getAvailableTuners();

        for(DiscoveredTuner discoveredTuner: availableTuners)
        {
            String preferredName = discoveredTuner.getTuner().getPreferredName();

            if(!preferredNames.contains(preferredName))
            {
                preferredNames.add(preferredName);
            }
        }

        Collections.sort(preferredNames);

        return preferredNames;
    }

    /**
     * Tuners currently available for use
     */
    public List<DiscoveredTuner> getAvailableTuners()
    {
        return mDiscoveredTunerModel.getAvailableTuners();
    }

    public Source getSource(SourceConfiguration config, ChannelSpecification channelSpecification) throws SourceException
    {
        Source retVal = null;

        switch(config.getSourceType())
        {
            case MIXER:
                retVal = MixerManager.getSource(config);
                break;
            case TUNER:
                if(config instanceof SourceConfigTuner)
                {
                    SourceConfigTuner sourceConfigTuner = (SourceConfigTuner)config;
                    TunerChannel tunerChannel = sourceConfigTuner.getTunerChannel(channelSpecification.getBandwidth());
                    String preferredTuner = sourceConfigTuner.getPreferredTuner();
                    retVal = getSource(tunerChannel, channelSpecification, preferredTuner);
                }
                break;
            case TUNER_MULTIPLE_FREQUENCIES:
                if(config instanceof SourceConfigTunerMultipleFrequency)
                {
                    SourceConfigTunerMultipleFrequency sourceConfigTuner = (SourceConfigTunerMultipleFrequency)config;
                    TunerChannel tunerChannel = sourceConfigTuner.getTunerChannel(channelSpecification.getBandwidth());
                    String preferredTuner = sourceConfigTuner.getPreferredTuner();

                    Source source = getSource(tunerChannel, channelSpecification, preferredTuner);

                    if(source instanceof TunerChannelSource)
                    {
                        retVal = new MultiFrequencyTunerChannelSource(this, (TunerChannelSource)source,
                                sourceConfigTuner.getFrequencies(), channelSpecification, sourceConfigTuner.getPreferredTuner());
                    }
                }
                break;
            default:
                break;
        }

        return retVal;
    }

    /**
     * Iterates current available tuners to get a tuner channel source for the specified frequency and bandwidth
     *
     * Returns null if no tuner can source the channel
     */
    public Source getSource(TunerChannel tunerChannel, ChannelSpecification channelSpecification, String preferredTuner)
    {
        TunerChannelSource source = null;

        if(tunerChannel != null && channelSpecification != null)
        {
            DiscoveredTuner discoveredTuner;

            if(preferredTuner != null)
            {
                discoveredTuner = getDiscoveredTuner(preferredTuner);

                if(discoveredTuner != null)
                {
                    try
                    {
                        source = discoveredTuner.getTuner().getChannelSourceManager().getSource(tunerChannel, channelSpecification);

                        if(source != null)
                        {
                            return source;
                        }
                    }
                    catch(Exception e)
                    {
                        //Fall through to logger below
                    }
                }

                mLog.info("Unable to source channel [" + tunerChannel.getFrequency() + "] from preferred tuner [" +
                        preferredTuner + "] - searching for another tuner");
            }

            Iterator<DiscoveredTuner> it = mDiscoveredTunerModel.getAvailableTuners().iterator();

            while(it.hasNext() && source == null)
            {
                discoveredTuner = it.next();

                if(discoveredTuner.hasTuner())
                {
                    try
                    {
                        source = discoveredTuner.getTuner().getChannelSourceManager().getSource(tunerChannel, channelSpecification);
                    }
                    catch(Exception e)
                    {
                        mLog.error("Error obtaining channel from tuner [" + discoveredTuner.getTuner().getPreferredName() + "]", e);
                    }
                }
            }
        }

        return source;
    }

    /**
     * USB hotplug event listener to register, unregister and detect when USB devices are added or removed.
     *
     * Note: hotplug is not supported on all platforms.
     */
    public class HotplugEventSupport implements HotplugCallback
    {
        private static final int HOTPLUG_CONTINUE_EVENT_SUPPORT = 0;
        private HotplugCallbackHandle mHotplugCallbackHandle;
        private ScheduledFuture<?> mEventProcessorFuture;

        /**
         * LibUsb hotplug event notification
         * @param context used for hotplug event registration
         * @param device that arrived or left
         * @param event value, arrived event or left event
         * @param userData not used
         * @return
         */
        @Override
        public int processEvent(Context context, Device device, int event, Object userData)
        {
            int bus = LibUsb.getBusNumber(device);
            int port = LibUsb.getPortNumber(device);

            switch(event)
            {
                case LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED:
                    addUsbTuner(device);
                    break;
                case LibUsb.HOTPLUG_EVENT_DEVICE_LEFT:
                    removeUsbTuner(bus, port);
                    break;
            }

            return HOTPLUG_CONTINUE_EVENT_SUPPORT;
        }

        /**
         * Registers for hotplug event notifications, if the capability is supported on this platform and starts an
         * event processing timer to process hotplug events.
         */
        public void start()
        {
            if(LibUsb.hasCapability(LibUsb.CAP_HAS_HOTPLUG))
            {
                mHotplugCallbackHandle = new HotplugCallbackHandle();
                int events = LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED | LibUsb.HOTPLUG_EVENT_DEVICE_LEFT;
                int flags = LibUsb.HOTPLUG_ENUMERATE;
                int vendorId = LibUsb.HOTPLUG_MATCH_ANY;
                int productId = LibUsb.HOTPLUG_MATCH_ANY;
                int deviceClass = LibUsb.HOTPLUG_MATCH_ANY;

                int status = LibUsb.hotplugRegisterCallback(mLibUsbApplicationContext, events, flags, vendorId,
                        productId, deviceClass, this, "sdrtrunk hotplug support", mHotplugCallbackHandle);

                if(status == LibUsb.SUCCESS)
                {
                    Runnable eventHandler = () -> LibUsb.handleEvents(mLibUsbApplicationContext);
                    mEventProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(eventHandler,
                            0, 100, TimeUnit.MILLISECONDS);
                }
                else
                {
                    mHotplugCallbackHandle = null;
                    mLog.info("LibUsb - unable to register device hotplug listener - " + LibUsb.errorName(status));
                }
            }
            else
            {
                mLog.info("LibUsb Hotplug event notification Is Not Supported on this platform.");
            }
        }

        /**
         * Unregisters from hotplug event notifications and stops the event processing timer
         */
        public void stop()
        {
            if(mHotplugCallbackHandle != null)
            {
                LibUsb.hotplugDeregisterCallback(mLibUsbApplicationContext, mHotplugCallbackHandle);
                mHotplugCallbackHandle = null;
            }

            if(mEventProcessorFuture != null)
            {
                mEventProcessorFuture.cancel(true);
                mEventProcessorFuture = null;
            }
        }
    }

    public static void main(String[] args)
    {
        mLog.info("Starting ...");
        UserPreferences userPreferences = new UserPreferences();
        TunerManager tunerManager = new TunerManager(userPreferences);
        tunerManager.start();

        while(true);

//        mLog.info("Finished!");
    }
}

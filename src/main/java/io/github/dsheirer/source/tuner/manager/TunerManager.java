/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import io.github.dsheirer.gui.preference.tuner.RspDuoSelectionMode;
import io.github.dsheirer.libusb.LibUsb;
import io.github.dsheirer.libusb.UsbContext;
import io.github.dsheirer.libusb.UsbDeviceInfo;
import io.github.dsheirer.libusb.UsbException;
import io.github.dsheirer.libusb.UsbHotplug;
import io.github.dsheirer.libusb.UsbHotplugListener;
import io.github.dsheirer.libusb.UsbLogLevel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.sdrplay.SDRplay;
import io.github.dsheirer.sdrplay.SDRplayException;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.mixer.MixerManager;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.MultiFrequencyTunerChannelSource;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.network.NetworkTunerConfiguration;
import io.github.dsheirer.source.tuner.recording.RecordingTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.DiscoveredRspDuoTuner1;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tuner manager provides access to tuners using USB, recording, sound-card and system-daemon accessible devices. This
 * manager also supports hot-plug detection and black-listing of discovered tuners so that they can be used with other
 * software applications.
 */
public class TunerManager implements IDiscoveredTunerStatusListener
{
    private static final Logger mLog = LoggerFactory.getLogger(TunerManager.class);
    private final UserPreferences mUserPreferences;
    private final DiscoveredTunerModel mDiscoveredTunerModel;
    private final TunerConfigurationManager mTunerConfigurationManager;
    /**
     * libusb error code for an unspecified error (LIBUSB_ERROR_OTHER)
     */
    private static final int LIBUSB_ERROR_OTHER = -99;
    private final HotplugEventSupport mHotplugEventSupport = new HotplugEventSupport();
    private boolean mLibUsbAvailable = false;
    private SDRplay mSDRplay;

    /**
     * Constructs an instance
     * @param userPreferences for preferences
     */
    public TunerManager(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mTunerConfigurationManager = new TunerConfigurationManager(userPreferences);
        mDiscoveredTunerModel = new DiscoveredTunerModel(mTunerConfigurationManager);
    }

    /**
     * Discovered tuner model
     */
    public DiscoveredTunerModel getDiscoveredTunerModel()
    {
        return mDiscoveredTunerModel;
    }

    /**
     * Tuner configuration manager
     */
    public TunerConfigurationManager getTunerConfigurationManager()
    {
        return mTunerConfigurationManager;
    }

    /**
     * Starts the tuner manager and loads all available tuners
     */
    public void start()
    {
        mLog.info("Discovering tuners ...");

        try
        {
            //Loads the libusb library that jlibusb bundles, which the USB tuner libraries share
            mLog.info("LibUsb Version: " + LibUsb.version() + " - hotplug detection: " +
                    (LibUsb.hasHotplug() ? "native" : "polling"));
            mLibUsbAvailable = true;
        }
        catch(LinkageError | RuntimeException e)
        {
            String os = System.getProperty("os.name");
            String version = System.getProperty("os.version");
            mLog.error("Unable to load the libusb library for communication with USB tuner(s) on os [" + os +
                    "] version [" + version + "].  To use a different libusb build, start sdrtrunk with " +
                    "-Djlibusb.library.path=<directory containing libusb>", e);
        }

        if(mLibUsbAvailable)
        {
            discoverUSBTuners();
            mHotplugEventSupport.start();
            discoverSdrPlayTuners();
        }

        discoverRecordingTuners();
        discoverNetworkTuners();
    }

    /**
     * Stops or shuts down the tuner manager and releases all devices
     */
    public void stop()
    {
        //Stop hotplug detection first so that no tuner is added or removed while the tuners are released
        mHotplugEventSupport.stop();

        //Stop all tuners
        mDiscoveredTunerModel.releaseDiscoveredTuners();

        //Shutdown SDRplay API instance
        closeSDRplay();
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
        List<DiscoveredUSBTuner> discoveredUSBTuners = new ArrayList<>();

        try(UsbContext usb = UsbContext.create())
        {
            List<UsbDeviceInfo> devices = getDeviceList(usb);

            if(devices != null)
            {
                mLog.info("LibUsb - discovered [" + devices.size() + "] potential usb devices");

                for(UsbDeviceInfo device: devices)
                {
                    DiscoveredUSBTuner discoveredUSBTuner = createDiscoveredTuner(device);

                    if(discoveredUSBTuner != null)
                    {
                        mLog.info("Discovered tuner at USB Bus [" + discoveredUSBTuner.getBus() + "] Port [" +
                                discoveredUSBTuner.getPortAddress() + "] Tuner Class [" +
                                discoveredUSBTuner.getTunerClass() + "]");
                        discoveredUSBTuners.add(discoveredUSBTuner);
                    }
                }
            }
        }
        catch(UsbException | RuntimeException e)
        {
            mLog.error("LibUsb - error during USB device discovery", e);
        }

        for(DiscoveredUSBTuner discoveredUSBTuner: discoveredUSBTuners)
        {
            addUsbTuner(discoveredUSBTuner);
        }
    }

    /**
     * Lists the USB devices.  If listing fails with error -99, retries once with libusb warning logging turned on so
     * that the log shows what caused it.
     * @param usb context to list devices from
     * @return devices, or null if they couldn't be listed
     */
    private List<UsbDeviceInfo> getDeviceList(UsbContext usb)
    {
        try
        {
            return usb.devices();
        }
        catch(UsbException e)
        {
            if(e.code() != LIBUSB_ERROR_OTHER)
            {
                mLog.error("LibUsb - error during get device list - " + e.getMessage());
                return null;
            }
        }

        try
        {
            mLog.info("LibUsb reported error -99 during get device list - increasing logging level and retrying");
            usb.setLogLevel(UsbLogLevel.WARNING);
            List<UsbDeviceInfo> devices = usb.devices();
            mLog.info("LibUsb - get device list count [" + devices.size() + "] during retry");
            return devices;
        }
        catch(UsbException e)
        {
            mLog.info("LibUsb - get device list reported error during retry - " + e.getMessage());
            return null;
        }
        finally
        {
            try
            {
                usb.setLogLevel(UsbLogLevel.NONE);
            }
            catch(UsbException e)
            {
                //Logging level only - nothing else to do
            }

            mLog.info("LibUsb - get device list reattempt complete - if additional logging was generated please notify developer");
        }
    }

    /**
     * Identifies the USB tuner class of a device.
     * @param device to identify
     * @return tuner class, or null for a root hub or a device that isn't a supported tuner
     */
    private static TunerClass getSupportedTunerClass(UsbDeviceInfo device)
    {
        //A root hub has no port numbers
        if(device.portNumbers().isEmpty())
        {
            return null;
        }

        TunerClass tunerClass = TunerClass.lookup((short)device.vendorId(), (short)device.productId());
        return tunerClass.isSupportedUsbTuner() ? tunerClass : null;
    }

    /**
     * Creates a discovered tuner for a USB device.
     * @param device to create a tuner for
     * @return discovered tuner, or null if the device isn't a supported tuner
     */
    private DiscoveredUSBTuner createDiscoveredTuner(UsbDeviceInfo device)
    {
        TunerClass tunerClass = getSupportedTunerClass(device);

        if(tunerClass == null)
        {
            return null;
        }

        ChannelizerType channelizerType = mUserPreferences.getTunerPreference().getChannelizerType();
        return new DiscoveredUSBTuner(tunerClass, device.busNumber(), UsbPortAddress.of(device), channelizerType);
    }

    /**
     * Determines if the USB device is a supported tuner and add if it has not already been added/discovered.
     * @param discoveredUSBTuner to add
     */
    private void addUsbTuner(DiscoveredUSBTuner discoveredUSBTuner)
    {
        if(!mDiscoveredTunerModel.hasUsbTuner(discoveredUSBTuner.getBus(), discoveredUSBTuner.getPortAddress()))
        {
            startAndConfigureTuner(discoveredUSBTuner);
        }
    }

    /**
     * Stops and removes the USB tuner if it is currently discovered.
     * @param bus usb for the tuner
     * @param portAddress usb for the tuner
     */
    private DiscoveredTuner removeUsbTuner(int bus, String portAddress)
    {
        return mDiscoveredTunerModel.removeUsbTuner(bus, portAddress);
    }

    /**
     * Starts, configures and adds the tuner to the tuner model.
     * @param discoveredTuner to add and configure
     */
    private void startAndConfigureTuner(DiscoveredTuner discoveredTuner)
    {
        discoveredTuner.addTunerStatusListener(this);

        //Set the tuner to disabled if the user has previously blacklisted the tuner
        if(mTunerConfigurationManager.isDisabled(discoveredTuner))
        {
            discoveredTuner.setEnabled(false);
            mLog.info("Tuner: " + discoveredTuner + " - Added / Disabled");
        }
        else
        {
            mLog.info("Tuner: " + discoveredTuner + " - Added / Starting ...");
            //Attempt to start the discovered tuner and determine the tuner type
            tunerStatusUpdated(discoveredTuner, TunerStatus.DISABLED, TunerStatus.ENABLED);
        }

        mDiscoveredTunerModel.addDiscoveredTuner(discoveredTuner);
    }

    /**
     * Discover SDRPlay RSP tuners
     */
    private void discoverSdrPlayTuners()
    {
        ChannelizerType channelizerType = mUserPreferences.getTunerPreference().getChannelizerType();
        RspDuoSelectionMode duoSelectionMode = mUserPreferences.getTunerPreference().getRspDuoTunerMode();

        if(!SDRplay.isInstalled())
        {
            mLog.info("SDRplay API is not installed - RSP tuners are not available");
            return;
        }

        //Note: this session stays open while RSP tuners are in use.  Each RSP tuner opens its own session and the
        //sessions share a single connection to the SDRplay API service, which closes when the last session closes.
        try
        {
            mSDRplay = SDRplay.open();
            mLog.info("SDRplay API version [" + mSDRplay.apiVersion() + "] loaded from [" + mSDRplay.location() + "]");
        }
        catch(SDRplayException se)
        {
            mLog.error("Couldn't open SDRplay API - " + se.getMessage());
            mSDRplay = null;
        }

        if(mSDRplay != null)
        {
            try
            {
                List<DeviceInfo> deviceInfos = mSDRplay.devices();

                mLog.info("Discovered [" + deviceInfos.size() + "] RSP devices from SDRplay API");

                if(deviceInfos.isEmpty())
                {
                    closeSDRplay();
                    return;
                }

                for(DeviceInfo deviceInfo: deviceInfos)
                {
                    List<DiscoveredRspTuner> tuners = TunerFactory.getRspTuners(deviceInfo, channelizerType, duoSelectionMode);

                    for(DiscoveredRspTuner tuner: tuners)
                    {
                        startAndConfigureTuner(tuner);
                    }
                }
            }
            catch(SDRplayException se)
            {
                mLog.info("Unable to get list of devices from SDRplay API - " + se.getMessage());
            }
        }
    }

    /**
     * Closes the SDRplay API discovery session.
     */
    private void closeSDRplay()
    {
        if(mSDRplay != null)
        {
            try
            {
                mSDRplay.close();
            }
            catch(SDRplayException se)
            {
                mLog.warn("Error closing SDRplay API - " + se.getMessage());
            }

            mSDRplay = null;
        }
    }

    /**
     * Discover recording based tuners
     */
    private void discoverRecordingTuners()
    {
        List<TunerConfiguration> tunerConfigurations = getTunerConfigurationManager().getTunerConfigurations(TunerType.RECORDING);

        if(tunerConfigurations.size() > 0)
        {
            mLog.info("Discovered [" + tunerConfigurations.size() + "] recording tuners");
        }

        for(TunerConfiguration tunerConfiguration: tunerConfigurations)
        {
            if(tunerConfiguration instanceof RecordingTunerConfiguration recordingTunerConfiguration)
            {
                DiscoveredRecordingTuner discoveredRecordingTuner =
                        new DiscoveredRecordingTuner(mUserPreferences, recordingTunerConfiguration);

                discoveredRecordingTuner.addTunerStatusListener(this);
                discoveredRecordingTuner.setEnabled(false);
                mLog.info("Tuner Added: " + discoveredRecordingTuner);
                mDiscoveredTunerModel.addDiscoveredTuner(discoveredRecordingTuner);
            }
        }
    }

    /**
     * Discover network (e.g. rtl_tcp) based tuners
     */
    private void discoverNetworkTuners()
    {
        List<TunerConfiguration> tunerConfigurations = getTunerConfigurationManager().getTunerConfigurations(TunerType.RTL_TCP);

        if(tunerConfigurations.size() > 0)
        {
            mLog.info("Discovered [" + tunerConfigurations.size() + "] network tuners");
        }

        for(TunerConfiguration tunerConfiguration: tunerConfigurations)
        {
            if(tunerConfiguration instanceof NetworkTunerConfiguration networkTunerConfiguration)
            {
                DiscoveredNetworkTuner discoveredNetworkTuner =
                        new DiscoveredNetworkTuner(mUserPreferences, networkTunerConfiguration);

                discoveredNetworkTuner.addTunerStatusListener(this);
                discoveredNetworkTuner.setEnabled(false);
                mLog.info("Tuner Added: " + discoveredNetworkTuner);
                mDiscoveredTunerModel.addDiscoveredTuner(discoveredNetworkTuner);
            }
        }
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
        if(current == TunerStatus.ENABLED)
        {
            discoveredTuner.start();
        }

        //Special handling for RSPduo to auto-update enabled state for slave device when configured for master/slave operation
        if(discoveredTuner instanceof DiscoveredRspDuoTuner1 rspDuoTuner1 && rspDuoTuner1.getDeviceSelectionMode().isMasterMode())
        {
            String id = rspDuoTuner1.getId();
            id = id.replace(DiscoveredRspDuoTuner1.RSP_DUO_ID_PREFIX + "1", DiscoveredRspDuoTuner1.RSP_DUO_ID_PREFIX + "2");
            DiscoveredTuner rspDuoTuner2 = getDiscoveredTunerModel().getDiscoveredTuner(id);

            if(rspDuoTuner2 != null)
            {
                if(previous == TunerStatus.ENABLED && current == TunerStatus.DISABLED)
                {
                    rspDuoTuner2.setEnabled(false);
                }
                else if(previous == TunerStatus.DISABLED && current == TunerStatus.ENABLED)
                {
                    rspDuoTuner2.setEnabled(true);
                    rspDuoTuner2.start();
                }
            }

            //Notify tuner configuration manager to apply tuner configurations & update disabled tuner states
            mTunerConfigurationManager.tunerStatusUpdated(rspDuoTuner1, previous, current);

            if(rspDuoTuner2 != null)
            {
                mTunerConfigurationManager.tunerStatusUpdated(rspDuoTuner2, previous, current);
            }
        }
        else
        {
            //Notify tuner configuration manager to apply tuner configuration
            mTunerConfigurationManager.tunerStatusUpdated(discoveredTuner, previous, current);
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

    /**
     * Obtain a channel (sample stream) source from one of the tuners.
     * @param config for the source
     * @param channelSpecification required by the decoder
     * @param thread name to apply for the channel's scheduled executor
     * @return a configured source or null if there aren't any tuners that can provide the source.
     * @throws SourceException if there is an issue.
     */
    public Source getSource(SourceConfiguration config, ChannelSpecification channelSpecification,
                            String threadName) throws SourceException
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
                    retVal = getSource(tunerChannel, channelSpecification, preferredTuner, threadName +
                            " " + tunerChannel.getFrequency());
                }
                break;
            case TUNER_MULTIPLE_FREQUENCIES:
                if(config instanceof SourceConfigTunerMultipleFrequency)
                {
                    SourceConfigTunerMultipleFrequency sourceConfigTuner = (SourceConfigTunerMultipleFrequency)config;
                    TunerChannel tunerChannel = sourceConfigTuner.getTunerChannel(channelSpecification.getBandwidth());
                    String preferredTuner = sourceConfigTuner.getPreferredTuner();

                    Source source = getSource(tunerChannel, channelSpecification, preferredTuner, threadName);

                    if(source instanceof TunerChannelSource)
                    {
                        retVal = new MultiFrequencyTunerChannelSource(this, (TunerChannelSource)source,
                                sourceConfigTuner.getFrequencies(), channelSpecification,
                                sourceConfigTuner.getPreferredTuner(), threadName + " MULTI FREQ");
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
     * @param tunerChannel requested
     * @param channelSpecification from the decoder
     * @param preferredTuner name
     * @param threadName to assign to the channel's thread pool
     *
     * Returns null if no tuner can source the channel
     */
    public Source getSource(TunerChannel tunerChannel, ChannelSpecification channelSpecification, String preferredTuner,
                            String threadName)
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
                        source = discoveredTuner.getTuner().getChannelSourceManager().getSource(tunerChannel,
                                channelSpecification, threadName);

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
                        source = discoveredTuner.getTuner().getChannelSourceManager().getSource(tunerChannel,
                                channelSpecification, threadName);
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
     * USB hotplug support to detect when USB tuners are added or removed.  jlibusb reports changes as they happen
     * where libusb supports hotplug (Linux and macOS) and polls the USB device list elsewhere (Windows).
     *
     * Tuners already connected when detection starts are reported as arrivals too.  Those were added during discovery
     * at startup and are ignored here because the tuner model already has a tuner at that bus and port.
     */
    public class HotplugEventSupport implements UsbHotplugListener
    {
        private UsbHotplug mUsbHotplug;

        /**
         * Starts hotplug detection of supported USB tuners.
         */
        public void start()
        {
            try
            {
                mUsbHotplug = UsbHotplug.start(device -> getSupportedTunerClass(device) != null, this);
                mLog.info("LibUsb - started USB hotplug detection (" +
                        (mUsbHotplug.mode() == UsbHotplug.Mode.NATIVE ? "native" : "polling") + ")");
            }
            catch(UsbException | RuntimeException e)
            {
                mLog.error("LibUsb - unable to start USB hotplug detection", e);
                mUsbHotplug = null;
            }
        }

        /**
         * Stops hotplug detection.  Waits for an arrival or removal that is being processed to finish.
         */
        public void stop()
        {
            if(mUsbHotplug != null)
            {
                mUsbHotplug.close();
                mUsbHotplug = null;
            }
        }

        /**
         * A supported USB tuner was connected.  Called on the jlibusb hotplug dispatch thread.
         * @param device that arrived
         */
        @Override
        public void arrived(UsbDeviceInfo device)
        {
            try
            {
                DiscoveredUSBTuner discoveredUSBTuner = createDiscoveredTuner(device);

                if(discoveredUSBTuner == null ||
                   mDiscoveredTunerModel.hasUsbTuner(discoveredUSBTuner.getBus(), discoveredUSBTuner.getPortAddress()))
                {
                    return;
                }

                mLog.info("Tuner plug-in detected at USB Bus [" + discoveredUSBTuner.getBus() + "] Port [" +
                        discoveredUSBTuner.getPortAddress() + "] Tuner Class [" + discoveredUSBTuner.getTunerClass() + "]");

                if(discoveredUSBTuner.getTunerClass().isFuncubeTuner())
                {
                    //Funcube tuners take a few moments to init the sound card interface.  Delay adding
                    //the tuner so that it can be started correctly.
                    ThreadPool.SCHEDULED.schedule(() -> addUsbTuner(discoveredUSBTuner), 2, TimeUnit.SECONDS);
                }
                else
                {
                    addUsbTuner(discoveredUSBTuner);
                }
            }
            catch(Exception e)
            {
                mLog.error("Error adding USB tuner after plug-in at USB Bus [" + device.busNumber() + "] Port [" +
                        UsbPortAddress.of(device) + "]", e);
            }
        }

        /**
         * A supported USB tuner was disconnected.  Called on the jlibusb hotplug dispatch thread.
         * @param device that left
         */
        @Override
        public void left(UsbDeviceInfo device)
        {
            try
            {
                DiscoveredTuner removed = removeUsbTuner(device.busNumber(), UsbPortAddress.of(device));

                if(removed != null)
                {
                    mLog.info("Tuner Unplugged: " + removed.getId());
                }
            }
            catch(Exception e)
            {
                mLog.error("Error removing USB tuner after unplug at USB Bus [" + device.busNumber() + "] Port [" +
                        UsbPortAddress.of(device) + "]", e);
            }
        }
    }
}

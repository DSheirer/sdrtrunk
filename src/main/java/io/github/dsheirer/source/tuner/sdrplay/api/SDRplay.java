/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay.api;

import io.github.dsheirer.source.tuner.sdrplay.api.callback.CallbackFunctions;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.IDeviceEventListener;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.IStreamListener;
import io.github.dsheirer.source.tuner.sdrplay.api.device.Device;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceFactory;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceInfo;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceType;
import io.github.dsheirer.source.tuner.sdrplay.api.device.IDeviceStruct;
import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;
import io.github.dsheirer.source.tuner.sdrplay.api.error.DebugLevel;
import io.github.dsheirer.source.tuner.sdrplay.api.error.ErrorInformation;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.composite.CompositeParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.composite.CompositeParametersFactory;
import io.github.dsheirer.source.tuner.sdrplay.api.util.Flag;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DeviceT;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_ErrorInfoT;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_h;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SDRplay API wrapper.
 *
 * Note: the jextract auto-generated RuntimeHelper contains a static block that attempts to load the sdrplay_api
 * library.  This approach fails because the API is installed to a non-default location.  Comment out the library
 * load code in the RuntimeHelper and we'll directly load the library.
 *
 * //        System.loadLibrary("libsdrplay_api");
 */
public class SDRplay
{
    public static final String SDRPLAY_API_LIBRARY_NAME = "sdrplay_api";
    public static final String SDRPLAY_API_PATH_LINUX = "/usr/local/lib/libsdrplay_api.so";
    public static final String SDRPLAY_API_PATH_MAC_OS = "/usr/local/lib/libsdrplay_api.dylib";
    public static final String SDRPLAY_API_PATH_WINDOWS = System.getenv("ProgramFiles") +
            "\\SDRplay\\API\\" + (System.getProperty("sun.arch.data.model").contentEquals("64") ? "x64" : "x86") +
            "\\" + SDRPLAY_API_LIBRARY_NAME;
    public static final String JAVA_LIBRARY_PATH_KEY = "java.library.path";

    private static final Logger mLog = LoggerFactory.getLogger(SDRplay.class);

    /**
     * Foreign memory allocation resource scope
     */
    private final MemorySession mGlobalMemorySession = MemorySession.global();

    /**
     * Indicates if libsdrplay_api.xx library was found and loaded.
     */
    private boolean mSdrplayLibraryLoaded;

    /**
     * Indicates if the library is available, meaning that the host system library was loaded AND it supports the
     * correct API version
     */
    private boolean mAvailable;

    /**
     * Map of (reusable) callback functions for each device.  Key value is the device handle memory address
     */
    private final Map<MemoryAddress, CallbackFunctions> mDeviceCallbackFunctionsMap = new HashMap<>();

    /**
     * Detected version of the API installed on the local system.
     */
    private Version mVersion;

    /**
     * Controls logging of library load status so that it only gets logged once.  Set to false once initial logging complete
     */
    private static boolean sLibraryLoadStatusLogging = true;

    /**
     * Constructs an instance of the SDRPLay API
     */
    public SDRplay()
    {
        mSdrplayLibraryLoaded = loadLibrary();

        if(mSdrplayLibraryLoaded)
        {
            Status openStatus = open();
            if(sLibraryLoadStatusLogging)
            {
                mLog.info("API library - open status: " + openStatus);
            }
            mAvailable = openStatus.success() && getVersion().isSupported();
        }
        else
        {
            if(sLibraryLoadStatusLogging)
            {
                mLog.info("API library was not loaded");
            }
            mAvailable = false;
        }

        if(isAvailable())
        {
            if(sLibraryLoadStatusLogging)
            {
                mLog.info("API library v" + getVersion() + " - loaded");
            }
        }
        else
        {
            if(sLibraryLoadStatusLogging)
            {
                mLog.info("API library is not available.");
            }
        }

        sLibraryLoadStatusLogging = false;
    }

    /**
     * Global Memory Session for this API instance
     */
    private MemorySession getGlobalMemorySession()
    {
        return mGlobalMemorySession;
    }

    /**
     * Attempts to load the SDRPlay API library from the local system.
     *
     * @return true if library was loaded successfully.
     */
    private boolean loadLibrary()
    {
        try
        {
            String libraryPath = getSDRplayLibraryPath();
            if(sLibraryLoadStatusLogging)
            {
                mLog.info("Loading API Library from default install path: " + libraryPath);
            }
            System.loadLibrary(SDRPLAY_API_LIBRARY_NAME);
            return true;
        }
        catch(Throwable t)
        {
            mLog.error("Unable to load SDRplay API library from default install path.  Loading from java system library path");

            try
            {
                System.loadLibrary(SDRPLAY_API_LIBRARY_NAME);
                return true;
            }
            catch(Throwable t2)
            {
                String name = System.mapLibraryName(SDRPLAY_API_LIBRARY_NAME);

                if(sLibraryLoadStatusLogging)
                {
                    mLog.warn("SDRPlay API library not found/installed on this system.  Ensure the API is installed either " +
                            "in the default install location or the install location is included in the " +
                            "'java.library.path' JVM property contains path to the library file [" + name +
                            "].  Current library path property contents: " + System.getProperty(JAVA_LIBRARY_PATH_KEY));
                }
            }
        }

        return false;
    }

    /**
     * List of devices available via the API
     * @return list of device infos.
     * @throws SDRPlayException if there is an error
     */
    public List<DeviceInfo> getDeviceInfos() throws SDRPlayException
    {
        return DeviceFactory.parseDeviceInfos(getDeviceStructures());
    }

    /**
     * List of device structures for devices available from the API
     * @return list of device structures
     * @throws SDRPlayException if there is an error
     */
    public List<IDeviceStruct> getDeviceStructures() throws SDRPlayException
    {
        List<IDeviceStruct> deviceStructs = new ArrayList<>();

        //Get a version-correct array of DeviceT structures
        MemorySegment devicesArray = DeviceFactory.createDeviceArray(getVersion(), getGlobalMemorySession());

        MemorySegment deviceCount = getGlobalMemorySession().allocate(ValueLayout.JAVA_INT, 0);

        Status status = Status.fromValue(sdrplay_api_h.sdrplay_api_GetDevices(devicesArray, deviceCount,
                sdrplay_api_h.SDRPLAY_MAX_DEVICES()));

        if(status.success())
        {
            int count = deviceCount.get(ValueLayout.JAVA_INT, 0);
            deviceStructs.addAll(DeviceFactory.parseDeviceStructs(getVersion(), devicesArray, count));
        }
        else
        {
            mLog.error("Couldn't load RSP devices from API.  Status: " + status);
        }

        return deviceStructs;
    }

    /**
     * Find an RSP device descriptor by serial number.
     * @param serialNumber to search for
     * @return matching device descriptor, or null.
     * @throws SDRPlayException if there is an error parsing the device structures
     */
    public DeviceInfo getDevice(String serialNumber) throws SDRPlayException
    {
        for(DeviceInfo deviceInfo: getDeviceInfos())
        {
            if(deviceInfo.getSerialNumber().equals(serialNumber))
            {
                return deviceInfo;
            }
        }

        return null;
    }

    /**
     * Obtains the device that matches the device info argument.
     * @param deviceInfo to match
     * @return non-null device
     * @throws SDRPlayException
     */
    public Device getDevice(DeviceInfo deviceInfo) throws SDRPlayException
    {
        Device device = null;

        if(isAvailable())
        {
            List<IDeviceStruct> deviceStructs = getDeviceStructures();

            for(IDeviceStruct deviceStruct: deviceStructs)
            {
                if(deviceInfo.matches(deviceStruct))
                {
                    device = DeviceFactory.createDevice(this, deviceStruct);
                    break;
                }
            }
        }

        if(device == null)
        {
            throw new SDRPlayException("Unable to find RSP device");
        }

        return device;
    }


    /**
     * Finds the first device that matches the specified device type.
     *
     * @param deviceType to find
     * @return the specified device type or null.
     */
    public DeviceInfo getDeviceInfo(DeviceType deviceType) throws SDRPlayException
    {
        for(DeviceInfo deviceInfo : getDeviceInfos())
        {
            if(deviceInfo.getDeviceType() == deviceType)
            {
                return deviceInfo;
            }
        }

        return null;
    }

    /**
     * Selects the device for exclusive use.  This method is invoked by the device instance.
     *
     * @param memorySegment of the device in foreign memory
     * @throws SDRPlayException if the device argument was not created by this API instance or if unable to lock or
     * unlock the device API for exclusive use, or if unable to select the device.
     */
    public void select(MemorySegment memorySegment) throws SDRPlayException
    {
        lockDeviceApi();
        Status selectStatus = Status.fromValue(sdrplay_api_h.sdrplay_api_SelectDevice(memorySegment));
        unlockDeviceApi();

        if(selectStatus.fail())
        {
            throw new SDRPlayException("Unable to select the device", selectStatus);
        }
    }

    /**
     * Releases the device from exclusive use.  This method is invoked by the device instance.
     *
     * @param memorySegment of the device in foreign memory
     * @throws SDRPlayException if the device argument was not created by this API instance or if unable to release
     * the device
     */
    public void release(MemorySegment memorySegment) throws SDRPlayException
    {
        Status status = Status.fromValue(sdrplay_api_h.sdrplay_api_ReleaseDevice(memorySegment));

        if(status.fail())
        {
            mLog.info("API call to release device failed, however device is effectively released now.");
        }
    }

    /**
     * Retrieves the initial composite parameters for each device.  This should only be invoked once, on
     * startup, for each device.  Changes made to the device parameters should invoke update() method to apply changes.
     *
     * @param deviceType to load parameters
     * @param deviceHandle to device
     * @return constructed device composite paramaters
     */
    public CompositeParameters getCompositeParameters(DeviceType deviceType, MemoryAddress deviceHandle) throws SDRPlayException
    {
        //Allocate a pointer that the api will fill with the memory address of the device parameters in memory.
        MemorySegment pointer = getGlobalMemorySession().allocate(ValueLayout.ADDRESS);
        Status status = Status.fromValue(sdrplay_api_h.sdrplay_api_GetDeviceParams(deviceHandle, pointer));

        if(status.success())
        {
            //Get the memory address from the pointer's memory segment to where the structure is located
            MemoryAddress memoryAddress = pointer.get(ValueLayout.ADDRESS, 0);

            //The structure's memory is already allocated ... wrap a memory segment around it
            MemorySegment memorySegment = sdrplay_api_DeviceT.ofAddress(memoryAddress, mGlobalMemorySession);
            return CompositeParametersFactory.create(deviceType, memorySegment, mGlobalMemorySession);
        }
        else
        {
            throw new SDRPlayException("Error retrieving device composite parameters", status);
        }
    }

    /**
     * Initializes a device for use.
     *
     * @param deviceHandle to the device
     * @param callbackFunctions to receive stream data from A and (optionally) B channels and events.
     * @throws SDRPlayException if the device is not selected of if unable to init the device
     */
    private void init(MemoryAddress deviceHandle, MemorySegment callbackFunctions) throws SDRPlayException
    {
        //Since we don't need/use the callback context ... setup as a pointer to the callback functions
        MemorySegment contextPointer = getGlobalMemorySession().allocate(ValueLayout.ADDRESS, callbackFunctions);
        Status status = Status.fromValue(sdrplay_api_h.sdrplay_api_Init(deviceHandle, callbackFunctions, contextPointer));

        if(!status.success())
        {
            throw new SDRPlayException("Error while initializing device", status);
        }
    }

    /**
     * Initializes a device for single-tuner use or for dual-tuner use as stream A
     *
     * @param device to initialize
     * @param deviceHandle to the device
     * @param eventListener to receive events for this device
     * @param streamListener to receive samples for stream A
     * @throws SDRPlayException if the device is not selected of if unable to init the device
     */
    public void initA(Device device, MemoryAddress deviceHandle, IDeviceEventListener eventListener,
                      IStreamListener streamListener) throws SDRPlayException
    {
        CallbackFunctions callbackFunctions = mDeviceCallbackFunctionsMap.get(deviceHandle);

        if(callbackFunctions == null)
        {
            callbackFunctions = new CallbackFunctions(getGlobalMemorySession(), eventListener, streamListener,
                    device.getStreamCallbackListener());
            mDeviceCallbackFunctionsMap.put(deviceHandle, callbackFunctions);
        }
        else
        {
            callbackFunctions.setDeviceEventListener(eventListener);
            callbackFunctions.setStreamAListener(streamListener);
        }

        init(deviceHandle, callbackFunctions.getCallbackFunctionsMemorySegment());
    }

    /**
     * Initializes a device for dual-tuner use as stream B
     *
     * @param device to initialize
     * @param deviceHandle to the device
     * @param eventListener to receive events for this device
     * @param streamListener to receive samples for stream B
     * @throws SDRPlayException if the device is not selected of if unable to init the device
     */
    public void initB(Device device, MemoryAddress deviceHandle, IDeviceEventListener eventListener,
                      IStreamListener streamListener) throws SDRPlayException
    {
        CallbackFunctions callbackFunctions = mDeviceCallbackFunctionsMap.get(deviceHandle);

        if(callbackFunctions == null)
        {
            callbackFunctions = new CallbackFunctions(getGlobalMemorySession(), eventListener, streamListener, streamListener,
                    device.getStreamCallbackListener());
            mDeviceCallbackFunctionsMap.put(deviceHandle, callbackFunctions);
        }
        else
        {
            callbackFunctions.setDeviceEventListener(eventListener);
            callbackFunctions.setStreamAListener(streamListener);
        }

        init(deviceHandle, callbackFunctions.getCallbackFunctionsMemorySegment());
    }

    /**
     * Un-Initializes a device from use.
     *
     * @param deviceHandle to the device
     * @throws SDRPlayException if error during uninit or if device is not selected
     */
    public void uninit(MemoryAddress deviceHandle) throws SDRPlayException
    {
        Status status = Status.fromValue(sdrplay_api_h.sdrplay_api_Uninit(deviceHandle));

        if(status.fail() && status != Status.NOT_INITIALIZED)
        {
            throw new SDRPlayException("Error while un-initializing device", status);
        }
    }

    /**
     * Applies updates made to the device parameters.  The device parameter that was updated is specified in the
     * update reason.
     * <p>
     * Note: this method is synchronized to prevent multiple threads from attempting to send update requests
     * concurrently, which will cause a failed request.
     *
     * @param device to update
     * @param deviceHandle for the device
     * @param tunerSelect identifies which tuner to apply the updates
     * @param updateReasons identifying what was updated
     * @throws SDRPlayException if the device is not selected, or if unable to update the device parameters
     */
    public synchronized void update(Device device, MemoryAddress deviceHandle, TunerSelect tunerSelect,
                                    UpdateReason... updateReasons) throws SDRPlayException
    {
        int reasons = UpdateReason.getReasons(updateReasons);
        int extendedReasons = UpdateReason.getExtendedReasons(updateReasons);

        Status status = Status.fromValue(sdrplay_api_h.sdrplay_api_Update(deviceHandle, tunerSelect.getValue(),
                reasons, extendedReasons));

        if(status.fail())
        {
            throw new SDRPlayUpdateException(status, Arrays.stream(updateReasons).toList());
        }
    }

    /**
     * Retrieve error information for the last error for the specified device.
     *
     * @param deviceSegment for the device
     * @return error information
     */
    private ErrorInformation getLastError(MemorySegment deviceSegment)
    {
        MemoryAddress errorAddress = sdrplay_api_h.sdrplay_api_GetLastError(deviceSegment);
        MemorySegment errorSegment = sdrplay_api_ErrorInfoT.ofAddress(errorAddress, mGlobalMemorySession);
        return new ErrorInformation(errorSegment);
    }

    /**
     * Sets the debug level logging for the specified device
     *
     * @param deviceHandle for the device
     * @param debugLevel to set
     * @throws SDRPlayException if the device is not selected or if unable to set/change the debug level.
     */
    public void setDebugLevel(MemoryAddress deviceHandle, DebugLevel debugLevel) throws SDRPlayException
    {
        Status status = Status.UNKNOWN;

        if(getVersion() == Version.V3_07)
        {
            //V3.07 used a debug level argument
            status = Status.fromValue(sdrplay_api_h.sdrplay_api_DebugEnable(deviceHandle, debugLevel.getValue()));
        }
        else if(getVersion().gte(Version.V3_08))
        {
            //V3.08+ uses a 0:1 flag to enable debug logging.  The method signature didn't change -- still takes an integer
            boolean enable = debugLevel != DebugLevel.DISABLE;
            status = Status.fromValue(sdrplay_api_h.sdrplay_api_DebugEnable(deviceHandle, Flag.of(enable)));
        }

        if(status.fail())
        {
            throw new SDRPlayException("Unable to set debug level", status);
        }
    }

    /**
     * Indicates if the SDRplay API is available and that the API library has been located and loaded for use and
     * it the API version is supported by this jsdrplay library.
     */
    public boolean isAvailable()
    {
        return mAvailable;
    }

    /**
     * Opens the API service.  MUST be invoked before accessing any of the API functions/methods.
     */
    private Status open()
    {
        return Status.fromValue(sdrplay_api_h.sdrplay_api_Open());
    }

    /**
     * Closes the API service.  MUST be invoked before shutdown, after all SDRPlay API operations are completed.
     *
     * Note: when using multiple instances of this class, only invoke close() on a single instance.  With linux API
     * version 3.07, if you invoked close() on one instance, then all of the other instances become unusable for
     * performing device operations (e.g. release(), etc).  This may be an artifact of the way that the Java
     * Foreign Function support is implemented, but not sure.  dls 1-Jan-2023
     */
    public Status close()
    {
        if(mSdrplayLibraryLoaded)
        {
            Status closeStatus;

            try
            {
                closeStatus = Status.fromValue(sdrplay_api_h.sdrplay_api_Close());
            }
            catch(Exception e)
            {
                closeStatus = Status.FAIL;
                mLog.error("Error closing SDRPlay API");
            }

            mSdrplayLibraryLoaded = false;
            mAvailable = false;
            return closeStatus;
        }
        else
        {
            return Status.API_UNAVAILABLE;
        }
    }

    /**
     * Identifies the API version.
     * Note: if the library is not found or loaded, or if the API version is not a supported version, this method
     * returns UNKNOWN
     *
     * @return version.
     */
    public Version getVersion()
    {
        if(mVersion == null)
        {
            if(mSdrplayLibraryLoaded)
            {
                MemorySegment apiVersion = getGlobalMemorySession().allocate(ValueLayout.JAVA_FLOAT, 0);
                Status status = Status.fromValue(sdrplay_api_h.sdrplay_api_ApiVersion(apiVersion));
                if(status.success())
                {
                    float version = apiVersion.get(ValueLayout.JAVA_FLOAT, 0);
                    mVersion = Version.fromValue(version);
                }
            }

            if(mVersion == null)
            {
                mVersion = Version.UNKNOWN;
            }
        }

        return mVersion;
    }

    /**
     * Attempts to lock the API for exclusive use of the current application. Once locked, no other applications
     * will be able to use the API. Typically used to lock the API prior to calling sdrplay_api_GetDevices() to
     * ensure only one application can select a given device. After completing device selection using
     * sdrplay_api_SelectDevice(), sdrplay_api_UnlockDeviceApi() can be used to release the API. May also
     * be used prior to calling sdrplay_api_ReleaseDevice() if it is necessary to reselect the same device.
     */
    private void lockDeviceApi() throws SDRPlayException
    {
        if(isAvailable())
        {
            Status status = Status.fromValue(sdrplay_api_h.sdrplay_api_LockDeviceApi());
            if(status.fail())
            {
                throw new SDRPlayException("Unable to lock Device API for exclusive use", status);
            }
        }
        else
        {
            throw new SDRPlayException("API is unavailable", Status.API_UNAVAILABLE);
        }
    }

    /**
     * Unlocks the device from exclusive access.
     *
     * @throws SDRPlayException if unable to unlock the Device API
     */
    private void unlockDeviceApi() throws SDRPlayException
    {
        if(isAvailable())
        {
            Status status = Status.fromValue(sdrplay_api_h.sdrplay_api_UnlockDeviceApi());

            if(status.fail())
            {
                throw new SDRPlayException("Unable to unlock Device API", status);
            }
        }
        else
        {
            throw new SDRPlayException("API is unavailable", Status.API_UNAVAILABLE);
        }
    }

    /**
     * Identifies the java library path for the sdrplay api library at runtime.
     */
    public static String getSDRplayLibraryPath()
    {
        if(SystemUtils.IS_OS_WINDOWS)
        {
            return SDRPLAY_API_PATH_WINDOWS;
        }
        else if(SystemUtils.IS_OS_LINUX)
        {
            return SDRPLAY_API_PATH_LINUX;
        }
        else if(SystemUtils.IS_OS_MAC_OSX)
        {
            return SDRPLAY_API_PATH_MAC_OS;
        }

        mLog.error("Unrecognized operating system.  Cannot identify sdrplay api library path");
        return "";
    }

    public static void main(String[] args)
    {
        /**
         * Note: on windows, add the following environment variable to the IntelliJ launcher:
         * -Djava.library.path="C:\Program Files\SDRplay\API\x64"
         *
         * Alternately, we can add this location to the Windows PATH environment variable and
         * Java will look there for the library, without having to specify it as a launcher
         * option.  This would allow for users that have installed the API to an alternate location.
         */
        SDRplay sdrplay = new SDRplay();
        Status status = sdrplay.open();
        mLog.info("Open Status: " + status);

        try
        {
            for(DeviceInfo deviceInfo: sdrplay.getDeviceInfos())
            {
                mLog.info("Found: " + deviceInfo);
            }
        }
        catch(SDRPlayException se)
        {
            mLog.info("Error", se);
        }

        Status closeStatus = sdrplay.close();
        mLog.info("Close Status: " + closeStatus);
    }
}

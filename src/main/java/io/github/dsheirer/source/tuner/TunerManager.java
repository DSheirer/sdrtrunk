/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.source.tuner;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.mixer.MixerManager;
import io.github.dsheirer.source.tuner.airspy.AirspyTuner;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerController;
import io.github.dsheirer.source.tuner.fcd.FCDTuner;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController;
import io.github.dsheirer.source.tuner.fcd.proplusV2.FCD2TunerController;
import io.github.dsheirer.source.tuner.hackrf.HackRFTuner;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController;
import io.github.dsheirer.source.tuner.rtl.RTL2832Tuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerController;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TTunerController;
import io.github.dsheirer.source.tuner.usb.USBMasterProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;

import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TunerManager
{
    private final static Logger mLog = LoggerFactory.getLogger(TunerManager.class);
    private static final int MAXIMUM_USB_2_DATA_RATE = 480000000;

    private TunerModel mTunerModel;
    private UserPreferences mUserPreferences;
    private Map<Integer,List<Tuner>> mUSBBusTunerMap = new TreeMap<>();

    /**
     * Application-wide LibUSB timeout processor for transfer buffers.  All classes that need to use USB transfer
     * buffers can register with this processor and the processor will auto-start and auto-stop while USB transfer
     * processors are registered.
     */
    public static final USBMasterProcessor LIBUSB_TRANSFER_PROCESSOR;

    static
    {
        LIBUSB_TRANSFER_PROCESSOR = new USBMasterProcessor();
    }

    public TunerManager(TunerModel tunerModel, UserPreferences userPreferences)
    {
        mTunerModel = tunerModel;
        mUserPreferences = userPreferences;

        initTuners();
        validateUSBBusTransferRates();
    }

    /**
     * Performs cleanup of USB related issues
     */
    public void dispose()
    {
        mLog.info("Shutting down LibUsb timeout processor ...");

        LIBUSB_TRANSFER_PROCESSOR.shutdown();

        mLog.info("Shutting down LibUsb ...");
        try
        {
            LibUsb.exit(null);
        }
        catch(Exception e)
        {
            mLog.error("Error shutting down LibUsb", e);
        }
    }

    /**
     * Loads all USB tuners and USB/Mixer tuner devices
     */
    private void initTuners()
    {
        DeviceList deviceList = new DeviceList();

        int result = LibUsb.init(null);

        if(result != LibUsb.SUCCESS)
        {
            mLog.error("unable to initialize libusb [" +
                LibUsb.errorName(result) + "]");
        }
        else
        {
            mLog.info("LibUSB API Version: " + LibUsb.getApiVersion());
            mLog.info("LibUSB Version: " + LibUsb.getVersion());

            result = LibUsb.getDeviceList(null, deviceList);

            if(result < 0)
            {
                mLog.error("unable to get device list from libusb [" + result + " / " +
                    LibUsb.errorName(result) + "]");
            }
            else
            {
                mLog.info("discovered [" + result + "] attached USB devices");
            }
        }

        for(Device device : deviceList)
        {
            DeviceDescriptor descriptor = new DeviceDescriptor();

            result = LibUsb.getDeviceDescriptor(device, descriptor);

            if(result != LibUsb.SUCCESS)
            {
                mLog.error("unable to read device descriptor [" +
                    LibUsb.errorName(result) + "]");
            }
            else
            {
                TunerInitStatus status = initTuner(device, descriptor);

                StringBuilder sb = new StringBuilder();

                int bus = LibUsb.getBusNumber(device);
                sb.append("USB Bus [").append(bus).append("]");
                sb.append(" Device [");
                sb.append(String.format("%04X", descriptor.idVendor()));
                sb.append(":");
                sb.append(String.format("%04X", descriptor.idProduct()));
                sb.append("]");

                if(status.isLoaded())
                {
                    Tuner tuner = status.getTuner();

                    try
                    {
                        mTunerModel.addTuner(tuner);
                        sb.append(" LOADED: ");
                        sb.append(tuner.toString());
                        sb.append(" Max Rate:").append(tuner.getMaximumUSBBitsPerSecond()).append(" bps");
                        updateUSBBusTunerMap(bus, tuner);
                    }
                    catch(Exception e)
                    {
                        sb.append(" NOT LOADED: ");
                        sb.append(status.getInfo());
                        sb.append(" Error:").append(e.getMessage());
                    }
                }
                else
                {
                    sb.append(" ").append(getDeviceClass(descriptor.bDeviceClass()));
                }

                mLog.info(sb.toString());
            }
        }

        LibUsb.freeDeviceList(deviceList, true);
    }

    private static String getDeviceClass(byte deviceClass)
    {
        switch(deviceClass)
        {
            case 0:
                return "Unknown Device - Class 0";
            case 2:
                return "Communications Device";
            case 3:
                return "HID Device";
            case 5:
                return "Physical Device";
            case 6:
                return "Still Imaging Device";
            case 7:
                return "Printer Device";
            case 8:
                return "Mass Storage Device";
            case 9:
                return "Hub Device";
            case 0xA:
                return "Communications Device";
            case 0xB:
                return "Smart Card Device";
            case 0xD:
                return "Content Security Device";
            case 0xE:
                return "Video Device";
            case 0xF:
                return "Personal Healthcare Device";
            case 0x10:
                return "Audio/Video Device";
            case (byte)0xDC:
                return "Diagnostic Device";
            case (byte)0xE0:
                return "Wireless Controller Device";
            case (byte)0xEF:
                return "Miscellaneous Device";
            default:
                return "Unknown Device - Class " + deviceClass;
        }
    }

    private void updateUSBBusTunerMap(int bus, Tuner tuner)
    {
        if(mUSBBusTunerMap.containsKey(bus))
        {
            mUSBBusTunerMap.get(bus).add(tuner);
        }
        else
        {
            List<Tuner> tuners = new ArrayList<>();
            tuners.add(tuner);
            mUSBBusTunerMap.put(bus, tuners);
        }
    }

    private void validateUSBBusTransferRates()
    {
        mLog.info("-------------------------------------------------------------");
        mLog.info("USB Bus - Potential Maximum Data Rates");

        boolean warning = false;

        List<Integer> buses = new ArrayList(mUSBBusTunerMap.keySet());
        Collections.sort(buses);
        for(Integer bus: buses)
        {
            int totalBPS = 0;

            for(Tuner tuner: mUSBBusTunerMap.get(bus))
            {
                totalBPS += tuner.getMaximumUSBBitsPerSecond();
            }

            if(totalBPS > (MAXIMUM_USB_2_DATA_RATE * .7))
            {
                warning = true;
            }

            mLog.info("USB Bus [" + bus + "] Rate [" + totalBPS + "] bits per second" + (warning ? " *** WARNING ***" : ""));
        }

        if(warning)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(
                "\n\t\t******************************************************************************************************\n" +
                "\t\t* WARNING: the combined data rates of tuner(s) on USB bus(es) above can potentially saturate or exceed\n" +
                "\t\t* the maximum USB 2.0 data rate of 480 Mbps.  This can cause data loss and severely impact decoding\n" +
                "\t\t* performance resulting in missed calls and stuttering audio.  Recommend moving tuner(s) to other\n" +
                "\t\t* USB ports/buses to better balance data rates across available USB buses.\n" +
                "\t\t******************************************************************************************************");
            mLog.warn(sb.toString());
        }
    }

    private TunerInitStatus initTuner(Device device, DeviceDescriptor descriptor)
    {
        if(device != null && descriptor != null)
        {
            TunerClass tunerClass = TunerClass.valueOf(descriptor.idVendor(), descriptor.idProduct());

            switch(tunerClass)
            {
                case AIRSPY:
                    return initAirspyTuner(device, descriptor);
                case ETTUS_USRP_B100:
                    return initEttusB100Tuner(device, descriptor);
                case FUNCUBE_DONGLE_PRO:
                    return initFuncubeProTuner(device, descriptor);
                case FUNCUBE_DONGLE_PRO_PLUS:
                    return initFuncubeProPlusTuner(device, descriptor);
                case HACKRF_ONE:
                case HACKRF_JAWBREAKER:
                case RAD1O:
                    return initHackRFTuner(device, descriptor);
                case COMPRO_VIDEOMATE_U620F:
                case COMPRO_VIDEOMATE_U650F:
                case COMPRO_VIDEOMATE_U680F:
                case GENERIC_2832:
                case GENERIC_2838:
                case DEXATEK_5217_DVBT:
                case DEXATEK_DIGIVOX_MINI_II_REV3:
                case DEXATEK_LOGILINK_VG002A:
                case GIGABYTE_GTU7300:
                case GTEK_T803:
                case LIFEVIEW_LV5T_DELUXE:
                case MYGICA_TD312:
                case PEAK_102569AGPK:
                case PROLECTRIX_DV107669:
                case SVEON_STV20:
                case TERRATEC_CINERGY_T_REV1:
                case TERRATEC_CINERGY_T_REV3:
                case TERRATEC_NOXON_REV1_B3:
                case TERRATEC_NOXON_REV1_B4:
                case TERRATEC_NOXON_REV1_B7:
                case TERRATEC_NOXON_REV1_C6:
                case TERRATEC_NOXON_REV2:
                case TERRATEC_T_STICK_PLUS:
                case TWINTECH_UT40:
                case ZAAPA_ZTMINDVBZP:
                    return initRTL2832Tuner(tunerClass, device, descriptor);
                case UNKNOWN:
                default:
                    break;
            }
        }

        return new TunerInitStatus(null, "Unknown Device");
    }

    /**
     * Releases all tuners in preparation for shutdown
     */
    public void releaseTuners()
    {
        mLog.info("Releasing [" + mTunerModel.getTuners().size() + "] tuner(s) ...");
        Tuner tuner = mTunerModel.getTuner(0);

        while(tuner != null)
        {
            mTunerModel.removeTuner(tuner);
            tuner.dispose();
            tuner = mTunerModel.getTuner(0);
        }
    }

    private TunerInitStatus initAirspyTuner(Device device, DeviceDescriptor descriptor)
    {
        try
        {
            AirspyTunerController airspyController = new AirspyTunerController(device);

            airspyController.init();

            AirspyTuner tuner = new AirspyTuner(airspyController, mUserPreferences);

            return new TunerInitStatus(tuner, "LOADED");
        }
        catch(SourceException se)
        {
            mLog.error("couldn't construct Airspy controller/tuner", se);

            return new TunerInitStatus(null,
                "error constructing Airspy tuner controller");
        }
    }


    private TunerInitStatus initEttusB100Tuner(Device device, DeviceDescriptor descriptor)
    {
        return new TunerInitStatus(null, "Ettus B100 tuner not currently supported");
    }

    private TunerInitStatus initFuncubeProTuner(Device device, DeviceDescriptor descriptor)
    {
        String reason = "NOT LOADED";

        TargetDataLine tdl = MixerManager.getTunerTargetDataLine(MixerTunerType.FUNCUBE_DONGLE_PRO);

        if(tdl != null)
        {
            FCD1TunerController controller = new FCD1TunerController(tdl, device, descriptor);

            try
            {
                controller.init();
                FCDTuner tuner = new FCDTuner(controller, mUserPreferences);
                return new TunerInitStatus(tuner, "LOADED");
            }
            catch(SourceException e)
            {
                mLog.error("couldn't load funcube dongle pro tuner", e);
                reason = "error during initialization - " + e.getLocalizedMessage();
            }
        }
        else
        {
            reason = "couldn't find matching mixer dataline";
        }

        return new TunerInitStatus(null, "Funcube Dongle Pro tuner not loaded - " + reason);
    }

    private TunerInitStatus initFuncubeProPlusTuner(Device device, DeviceDescriptor descriptor)
    {
        String reason = "NOT LOADED";

        TargetDataLine dataline = MixerManager.getTunerTargetDataLine(MixerTunerType.FUNCUBE_DONGLE_PRO_PLUS);

        if(dataline != null)
        {
            FCD2TunerController controller = new FCD2TunerController(dataline, device, descriptor);

            try
            {
                controller.init();
                FCDTuner tuner = new FCDTuner(controller, mUserPreferences);
                return new TunerInitStatus(tuner, "LOADED");
            }
            catch(SourceException e)
            {
                mLog.error("couldn't load funcube dongle pro plus tuner", e);
                reason = "error during initialization - " + e.getLocalizedMessage();
            }
        }
        else
        {
            reason = "couldn't find matching mixer dataline";
        }

        return new TunerInitStatus(null, "Funcube Dongle Pro tuner not loaded - " + reason);
    }

    private TunerInitStatus initHackRFTuner(Device device, DeviceDescriptor descriptor)
    {
        try
        {
            HackRFTunerController hackRFController = new HackRFTunerController(device, descriptor);

            hackRFController.init();

            HackRFTuner tuner = new HackRFTuner(hackRFController, mUserPreferences);

            return new TunerInitStatus(tuner, "LOADED");
        }
        catch(SourceException se)
        {
            mLog.error("couldn't construct HackRF controller/tuner", se);

            return new TunerInitStatus(null, "error constructing HackRF tuner controller");
        }
    }

    private TunerInitStatus initRTL2832Tuner(TunerClass tunerClass, Device device, DeviceDescriptor deviceDescriptor)
    {
        String reason = "NOT LOADED";

        TunerType tunerType = tunerClass.getTunerType();

        if(tunerType == TunerType.RTL2832_VARIOUS)
        {
            try
            {
                tunerType = RTL2832TunerController.identifyTunerType(device);
            }
            catch(SourceException e)
            {
                mLog.error("couldn't determine RTL2832 tuner type", e);
                tunerType = TunerType.UNKNOWN;
            }
        }

        switch(tunerType)
        {
            case ELONICS_E4000:
                try
                {
                    E4KTunerController controller = new E4KTunerController(device, deviceDescriptor);

                    controller.init();

                    RTL2832Tuner rtlTuner = new RTL2832Tuner(tunerClass, controller, mUserPreferences);

                    return new TunerInitStatus(rtlTuner, "LOADED");
                }
                catch(SourceException se)
                {
                    return new TunerInitStatus(null, "Error constructing E4K tuner controller - " +
                        se.getLocalizedMessage());
                }
            case RAFAELMICRO_R820T:
                try
                {
                    R820TTunerController controller = new R820TTunerController(device, deviceDescriptor);

                    controller.init();

                    RTL2832Tuner rtlTuner = new RTL2832Tuner(tunerClass, controller, mUserPreferences);

                    return new TunerInitStatus(rtlTuner, "LOADED");
                }
                catch(SourceException se)
                {
                    mLog.error("error constructing tuner", se);

                    return new TunerInitStatus(null, "Error constructing R820T "
                        + "tuner controller - " + se.getLocalizedMessage());
                }
            case FITIPOWER_FC0012:
            case FITIPOWER_FC0013:
            case RAFAELMICRO_R828D:
            case UNKNOWN:
            default:
                reason = "SDRTRunk doesn't currently support RTL2832 Dongle with [" + tunerType.toString() +
                    "] tuner for tuner class[" + tunerClass.toString() + "]";

                mLog.info(reason);
                break;
        }

        return new TunerInitStatus(null, reason);
    }

    public class TunerInitStatus
    {
        private Tuner mTuner;
        private String mInfo;

        public TunerInitStatus(Tuner tuner, String info)
        {
            mTuner = tuner;
            mInfo = info;
        }

        public Tuner getTuner()
        {
            return mTuner;
        }

        public String getInfo()
        {
            return mInfo;
        }

        public boolean isLoaded()
        {
            return mTuner != null;
        }
    }
}

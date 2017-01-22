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
package source.tuner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import source.SourceException;
import source.mixer.MixerManager;
import source.tuner.airspy.AirspyTuner;
import source.tuner.airspy.AirspyTunerController;
import source.tuner.fcd.FCDTuner;
import source.tuner.fcd.proV1.FCD1TunerController;
import source.tuner.fcd.proplusV2.FCD2TunerController;
import source.tuner.hackrf.HackRFTuner;
import source.tuner.hackrf.HackRFTunerController;
import source.tuner.rtl.RTL2832Tuner;
import source.tuner.rtl.RTL2832TunerController;
import source.tuner.rtl.e4k.E4KTunerController;
import source.tuner.rtl.r820t.R820TTunerController;

import java.util.Collection;

public class TunerManager
{
    private final static Logger mLog =
        LoggerFactory.getLogger(TunerManager.class);

    private MixerManager mMixerManager;
    private TunerModel mTunerModel;

    public TunerManager(MixerManager mixerManager, TunerModel tunerModel)
    {
        mMixerManager = mixerManager;
        mTunerModel = tunerModel;

        initTuners();
    }

    /**
     * Performs cleanup of USB related issues
     */
    public void dispose()
    {
        LibUsb.exit(null);
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

                sb.append("usb device [");
                sb.append(String.format("%04X", descriptor.idVendor()));
                sb.append(":");
                sb.append(String.format("%04X", descriptor.idProduct()));

                if(status.isLoaded())
                {
                    Tuner tuner = status.getTuner();

                    try
                    {
                        mTunerModel.addTuner(tuner);
                        sb.append("] LOADED: ");
                        sb.append(tuner.toString());
                    }
                    catch(Exception e)
                    {
                        sb.append("] NOT LOADED: ");
                        sb.append(status.getInfo());
                        sb.append(" Error:" + e.getMessage());
                    }
                }
                else
                {
                    sb.append("] NOT LOADED: ");
                    sb.append(status.getInfo());
                }

                mLog.info(sb.toString());
            }
        }

        LibUsb.freeDeviceList(deviceList, true);
    }

    private TunerInitStatus initTuner(Device device,
                                      DeviceDescriptor descriptor)
    {
        if(device != null && descriptor != null)
        {
            TunerClass tunerClass = TunerClass.valueOf(descriptor.idVendor(),
                descriptor.idProduct());

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

    private TunerInitStatus initAirspyTuner(Device device,
                                            DeviceDescriptor descriptor)
    {
        try
        {
            AirspyTunerController airspyController = new AirspyTunerController(device);

            airspyController.init();

            AirspyTuner tuner = new AirspyTuner(airspyController);

            return new TunerInitStatus(tuner, "LOADED");
        }
        catch(SourceException se)
        {
            mLog.error("couldn't construct Airspy controller/tuner", se);

            return new TunerInitStatus(null,
                "error constructing Airspy tuner controller");
        }
    }


    private TunerInitStatus initEttusB100Tuner(Device device,
                                               DeviceDescriptor descriptor)
    {
        return new TunerInitStatus(null, "Ettus B100 tuner not currently "
            + "supported");
    }

    private TunerInitStatus initFuncubeProTuner(Device device,
                                                DeviceDescriptor descriptor)
    {
        String reason = "NOT LOADED";

        MixerTunerDataLine dataline = getMixerTunerDataLine(
            TunerClass.FUNCUBE_DONGLE_PRO.getTunerType());

        if(dataline != null)
        {
            FCD1TunerController controller =
                new FCD1TunerController(device, descriptor);

            try
            {
                controller.init();

                FCDTuner tuner =
                    new FCDTuner(dataline, controller);

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

        return new TunerInitStatus(null, "Funcube Dongle Pro tuner not "
            + "loaded - " + reason);
    }

    private TunerInitStatus initFuncubeProPlusTuner(Device device,
                                                    DeviceDescriptor descriptor)
    {
        String reason = "NOT LOADED";

        MixerTunerDataLine dataline = getMixerTunerDataLine(
            TunerClass.FUNCUBE_DONGLE_PRO_PLUS.getTunerType());

        if(dataline != null)
        {
            FCD2TunerController controller =
                new FCD2TunerController(device, descriptor);

            try
            {
                controller.init();

                FCDTuner tuner =
                    new FCDTuner(dataline, controller);

                return new TunerInitStatus(tuner, "LOADED");
            }
            catch(SourceException e)
            {
                mLog.error("couldn't load funcube dongle pro plus tuner", e);

                reason = "error during initialization - " +
                    e.getLocalizedMessage();
            }
        }
        else
        {
            reason = "couldn't find matching mixer dataline";
        }

        return new TunerInitStatus(null, "Funcube Dongle Pro tuner not "
            + "loaded - " + reason);
    }

    private TunerInitStatus initHackRFTuner(Device device,
                                            DeviceDescriptor descriptor)
    {
        try
        {
            HackRFTunerController hackRFController = new HackRFTunerController(device, descriptor);

            hackRFController.init();

            HackRFTuner tuner = new HackRFTuner(hackRFController);

            return new TunerInitStatus(tuner, "LOADED");
        }
        catch(SourceException se)
        {
            mLog.error("couldn't construct HackRF controller/tuner", se);

            return new TunerInitStatus(null,
                "error constructing HackRF tuner controller");
        }
    }

    private TunerInitStatus initRTL2832Tuner(TunerClass tunerClass,
                                             Device device,
                                             DeviceDescriptor deviceDescriptor)
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

                    RTL2832Tuner rtlTuner = new RTL2832Tuner(tunerClass, controller);

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

                    RTL2832Tuner rtlTuner = new RTL2832Tuner(tunerClass, controller);

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
                reason = "SDRTRunk doesn't currently support RTL2832 "
                    + "Dongle with [" + tunerType.toString() +
                    "] tuner for tuner class[" + tunerClass.toString() + "]";
                break;
        }

        return new TunerInitStatus(null, reason);
    }

    /**
     * Gets the first tuner mixer dataline that corresponds to the tuner class.
     *
     * Note: this method is not currently able to align multiple tuner mixer
     * data lines of the same tuner type.  If you have multiple Funcube Dongle
     * tuners of the same TYPE, there is no guarantee that you will get the
     * correct mixer.
     *
     * @param tunerClass
     * @return
     */
    private MixerTunerDataLine getMixerTunerDataLine(TunerType tunerClass)
    {
        Collection<MixerTunerDataLine> datalines =
            mMixerManager.getMixerTunerDataLines();

        for(MixerTunerDataLine mixerTDL : datalines)
        {
            if(mixerTDL.getMixerTunerType().getTunerClass() == tunerClass)
            {
                return mixerTDL;
            }
        }

        return null;
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

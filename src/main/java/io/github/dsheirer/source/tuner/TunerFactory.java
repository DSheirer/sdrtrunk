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

package io.github.dsheirer.source.tuner;

import io.github.dsheirer.gui.preference.tuner.RspDuoSelectionMode;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.sdrplay.RspModel;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.mixer.MixerManager;
import io.github.dsheirer.source.tuner.airspy.AirspyTuner;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerConfiguration;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerController;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerEditor;
import io.github.dsheirer.source.tuner.airspy.hf.AirspyHfTuner;
import io.github.dsheirer.source.tuner.airspy.hf.AirspyHfTunerConfiguration;
import io.github.dsheirer.source.tuner.airspy.hf.AirspyHfTunerController;
import io.github.dsheirer.source.tuner.airspy.hf.AirspyHfTunerEditor;
import io.github.dsheirer.source.tuner.bladerf.BladeRfTuner;
import io.github.dsheirer.source.tuner.bladerf.BladeRfTunerConfiguration;
import io.github.dsheirer.source.tuner.bladerf.BladeRfTunerController;
import io.github.dsheirer.source.tuner.bladerf.BladeRfTunerEditor;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.fcd.FCDTuner;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerConfiguration;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerEditor;
import io.github.dsheirer.source.tuner.fcd.proplusV2.FCD2TunerConfiguration;
import io.github.dsheirer.source.tuner.fcd.proplusV2.FCD2TunerController;
import io.github.dsheirer.source.tuner.fcd.proplusV2.FCD2TunerEditor;
import io.github.dsheirer.source.tuner.hackrf.HackRFTuner;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerConfiguration;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerEditor;
import io.github.dsheirer.source.tuner.hydrasdr.HydraSdrTuner;
import io.github.dsheirer.source.tuner.hydrasdr.HydraSdrTunerConfiguration;
import io.github.dsheirer.source.tuner.hydrasdr.HydraSdrTunerController;
import io.github.dsheirer.source.tuner.hydrasdr.HydraSdrTunerEditor;
import io.github.dsheirer.source.tuner.limesdr.LimeSdrTuner;
import io.github.dsheirer.source.tuner.limesdr.LimeSdrTunerConfiguration;
import io.github.dsheirer.source.tuner.limesdr.LimeSdrTunerController;
import io.github.dsheirer.source.tuner.limesdr.LimeSdrTunerEditor;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.recording.RecordingTunerConfiguration;
import io.github.dsheirer.source.tuner.recording.RecordingTunerEditor;
import io.github.dsheirer.source.tuner.rtl.RTL2832Tuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerEditor;
import io.github.dsheirer.source.tuner.rtl.RTL2832UnknownTunerEditor;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc0012.FC0012TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc0013.FC0013TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc2580.FC2580TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r8x.r820t.R820TTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r8x.r828d.R828DTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspTuner;
import io.github.dsheirer.source.tuner.sdrplay.DeviceSelectionMode;
import io.github.dsheirer.source.tuner.sdrplay.RspException;
import io.github.dsheirer.source.tuner.sdrplay.rsp1.ControlRsp1;
import io.github.dsheirer.source.tuner.sdrplay.rsp1.DiscoveredRsp1Tuner;
import io.github.dsheirer.source.tuner.sdrplay.rsp1.IControlRsp1;
import io.github.dsheirer.source.tuner.sdrplay.rsp1.Rsp1TunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rsp1.Rsp1TunerController;
import io.github.dsheirer.source.tuner.sdrplay.rsp1.Rsp1TunerEditor;
import io.github.dsheirer.source.tuner.sdrplay.rsp1a.ControlRsp1a;
import io.github.dsheirer.source.tuner.sdrplay.rsp1a.DiscoveredRsp1aTuner;
import io.github.dsheirer.source.tuner.sdrplay.rsp1a.IControlRsp1a;
import io.github.dsheirer.source.tuner.sdrplay.rsp1a.Rsp1aTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rsp1a.Rsp1aTunerController;
import io.github.dsheirer.source.tuner.sdrplay.rsp1a.Rsp1aTunerEditor;
import io.github.dsheirer.source.tuner.sdrplay.rsp1b.ControlRsp1b;
import io.github.dsheirer.source.tuner.sdrplay.rsp1b.DiscoveredRsp1bTuner;
import io.github.dsheirer.source.tuner.sdrplay.rsp1b.IControlRsp1b;
import io.github.dsheirer.source.tuner.sdrplay.rsp1b.Rsp1bTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rsp1b.Rsp1bTunerController;
import io.github.dsheirer.source.tuner.sdrplay.rsp1b.Rsp1bTunerEditor;
import io.github.dsheirer.source.tuner.sdrplay.rsp2.ControlRsp2;
import io.github.dsheirer.source.tuner.sdrplay.rsp2.DiscoveredRsp2Tuner;
import io.github.dsheirer.source.tuner.sdrplay.rsp2.IControlRsp2;
import io.github.dsheirer.source.tuner.sdrplay.rsp2.Rsp2TunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rsp2.Rsp2TunerController;
import io.github.dsheirer.source.tuner.sdrplay.rsp2.Rsp2TunerEditor;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.ControlRspDuoTuner1Master;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.ControlRspDuoTuner1Single;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.ControlRspDuoTuner2Single;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.ControlRspDuoTuner2Slave;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.DiscoveredRspDuoTuner1;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.DiscoveredRspDuoTuner2;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.IControlRspDuoTuner1;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.IControlRspDuoTuner2;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.MasterSlaveBridge;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.RspDuoTuner1Configuration;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.RspDuoTuner1Controller;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.RspDuoTuner1Editor;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.RspDuoTuner2Configuration;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.RspDuoTuner2Controller;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.RspDuoTuner2Editor;
import io.github.dsheirer.source.tuner.sdrplay.rspDx.ControlRspDx;
import io.github.dsheirer.source.tuner.sdrplay.rspDx.DiscoveredRspDxTuner;
import io.github.dsheirer.source.tuner.sdrplay.rspDx.IControlRspDx;
import io.github.dsheirer.source.tuner.sdrplay.rspDx.RspDxTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rspDx.RspDxTunerController;
import io.github.dsheirer.source.tuner.sdrplay.rspDx.RspDxTunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.TargetDataLine;

/**
 * Factory for creating tuners and tuner configurations
 */
public class TunerFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(TunerFactory.class);

    /**
     * Creates one or two (e.g. RSPduo) Discovered RSP tuner instances for the RSP device.
     * @param deviceInfo describing the RSP device
     * @param channelizerType to use with the device
     * @param selectionMode to specify how to configure an RSPduo dual-tuner device.
     * @return zero or more discovered RSP tuners.
     */
    public static List<DiscoveredRspTuner> getRspTuners(DeviceInfo deviceInfo, ChannelizerType channelizerType, RspDuoSelectionMode selectionMode)
    {
        List<DiscoveredRspTuner> tuners = new ArrayList<>();

        switch(deviceInfo.model())
        {
            case RSP1:
                tuners.add(new DiscoveredRsp1Tuner(deviceInfo, channelizerType));
                break;
            case RSP1A:
                tuners.add(new DiscoveredRsp1aTuner(deviceInfo, channelizerType));
                break;
            case RSP1B:
                tuners.add(new DiscoveredRsp1bTuner(deviceInfo, channelizerType));
                break;
            case RSP2:
                tuners.add(new DiscoveredRsp2Tuner(deviceInfo, channelizerType));
                break;
            case RSPDX:
            case RSPDX_R2:
                tuners.add(new DiscoveredRspDxTuner(deviceInfo, channelizerType));
                break;
            case RSPDUO:
                switch(selectionMode)
                {
                    case SINGLE_1:
                        tuners.add(new DiscoveredRspDuoTuner1(deviceInfo, channelizerType));
                        break;
                    case SINGLE_2:
                        tuners.add(new DiscoveredRspDuoTuner2(deviceInfo, channelizerType));
                        break;
                    case DUAL:
                        MasterSlaveBridge bridge = new MasterSlaveBridge();
                        tuners.add(new DiscoveredRspDuoTuner1(deviceInfo, channelizerType, bridge));
                        tuners.add(new DiscoveredRspDuoTuner2(deviceInfo, channelizerType, bridge));
                        break;
                }
                break;
            default:
                mLog.warn("SDRPlay API returned an unknown tuner type [" + deviceInfo.model() +
                        "] with serial number [" + deviceInfo.serialNumber() + "]");
        }

        return tuners;
    }

    /**
     * Creates an RSP tuner instance for the RSP device described by the device info object.  The device is selected
     * from the SDRplay API when the tuner is started.
     * @param deviceInfo to match
     * @param selectionMode for the device (RSPduo single tuner 1 or 2, or single tuner 1 for all others).
     * @param channelizerType for the tuner
     * @param tunerErrorListener to monitor the tuner's status
     * @return constructed RSP tuner instance
     * @throws RspException if the device is not supported
     */
    public static RspTuner getRspTuner(DeviceInfo deviceInfo, DeviceSelectionMode selectionMode,
                                       ChannelizerType channelizerType, ITunerErrorListener tunerErrorListener)
            throws RspException
    {
        switch(deviceInfo.model())
        {
            case RSP1:
                Rsp1TunerController rsp1TunerController = new Rsp1TunerController(new ControlRsp1(deviceInfo), tunerErrorListener);
                return new RspTuner(rsp1TunerController, tunerErrorListener, channelizerType);
            case RSP1A:
                Rsp1aTunerController rsp1aTunerController = new Rsp1aTunerController(new ControlRsp1a(deviceInfo), tunerErrorListener);
                return new RspTuner(rsp1aTunerController, tunerErrorListener, channelizerType);
            case RSP1B:
                Rsp1bTunerController rsp1bTunerController = new Rsp1bTunerController(new ControlRsp1b(deviceInfo), tunerErrorListener);
                return new RspTuner(rsp1bTunerController, tunerErrorListener, channelizerType);
            case RSP2:
                Rsp2TunerController rsp2TunerController = new Rsp2TunerController(new ControlRsp2(deviceInfo), tunerErrorListener);
                return new RspTuner(rsp2TunerController, tunerErrorListener, channelizerType);
            case RSPDX:
            case RSPDX_R2:
                RspDxTunerController rspDxTunerController = new RspDxTunerController(new ControlRspDx(deviceInfo), tunerErrorListener);
                return new RspTuner(rspDxTunerController, tunerErrorListener, channelizerType);
            case RSPDUO:
                switch(selectionMode)
                {
                    case SINGLE_TUNER_1:
                        IControlRspDuoTuner1 controlRspDuoTuner1 = new ControlRspDuoTuner1Single(deviceInfo);
                        RspDuoTuner1Controller rspDuoTuner1Controller = new RspDuoTuner1Controller(controlRspDuoTuner1, tunerErrorListener);
                        return new RspTuner(rspDuoTuner1Controller, tunerErrorListener, channelizerType);
                    case SINGLE_TUNER_2:
                        IControlRspDuoTuner2 controlRspDuoTuner2 = new ControlRspDuoTuner2Single(deviceInfo);
                        RspDuoTuner2Controller rspDuoTuner2Controller = new RspDuoTuner2Controller(controlRspDuoTuner2, tunerErrorListener);
                        return new RspTuner(rspDuoTuner2Controller, tunerErrorListener, channelizerType);
                    default:
                        throw new RspException("This method only supports RSPduo single tuner configurations");
                }
            default:
                mLog.warn("Unrecognized SDRplay RSP Device Type: " + deviceInfo.model() + " SER#: " + deviceInfo.serialNumber());
                throw new RspException("Unsupported RSP device: " + deviceInfo);
        }
    }

    /**
     * Creates an RSPduo master (tuner 1) or slave (tuner 2) tuner instance for the device described by the device
     * info object.  The device is selected from the SDRplay API when the tuner is started.
     * @param deviceInfo to match
     * @param selectionMode master tuner 1 or slave tuner 2
     * @param channelizerType for the tuner
     * @param tunerErrorListener to monitor the tuner's status
     * @param bridge to link master/slave instances together
     * @return constructed RSP tuner instance
     * @throws RspException if the device is not an RSPduo or the selection mode is not master or slave
     */
    public static RspTuner getRspDuoTuner(DeviceInfo deviceInfo, DeviceSelectionMode selectionMode,
                                          ChannelizerType channelizerType, ITunerErrorListener tunerErrorListener,
                                          MasterSlaveBridge bridge) throws RspException
    {
        if(deviceInfo.model() != RspModel.RSPDUO)
        {
            throw new RspException("Device is not an RSPduo: " + deviceInfo);
        }

        switch(selectionMode)
        {
            case MASTER_TUNER_1:
                IControlRspDuoTuner1 controlRspDuoTuner1 = new ControlRspDuoTuner1Master(deviceInfo, bridge);
                RspDuoTuner1Controller rspDuoTuner1Controller = new RspDuoTuner1Controller(controlRspDuoTuner1, tunerErrorListener);
                return new RspTuner(rspDuoTuner1Controller, tunerErrorListener, channelizerType);
            case SLAVE_TUNER_2:
                IControlRspDuoTuner2 controlRspDuoTuner2 = new ControlRspDuoTuner2Slave(deviceInfo, bridge);
                RspDuoTuner2Controller rspDuoTuner2Controller = new RspDuoTuner2Controller(controlRspDuoTuner2, tunerErrorListener);
                return new RspTuner(rspDuoTuner2Controller, tunerErrorListener, channelizerType);
            default:
                throw new RspException("This method only supports RSPduo master/slave configurations");
        }
    }

    /**
     * Create a USB tuner
     * @param tunerClass to instantiate
     * @param portAddress usb
     * @param bus usb
     * @param tunerErrorListener to listen for errors from the tuner
     * @param channelizerType for the tuner
     * @return instantiated tuner
     * @throws SourceException if the tuner class is unrecognized
     */
    public static Tuner getUsbTuner(TunerClass tunerClass, String portAddress, int bus, ITunerErrorListener tunerErrorListener,
                                    ChannelizerType channelizerType) throws SourceException
    {
        switch(tunerClass)
        {
            case AIRSPY:
                return new AirspyTuner(new AirspyTunerController(bus, portAddress, tunerErrorListener), tunerErrorListener, channelizerType);
            case AIRSPY_HF:
                return new AirspyHfTuner(new AirspyHfTunerController(bus, portAddress, tunerErrorListener), tunerErrorListener, channelizerType);
            case BLADERF:
                return new BladeRfTuner(new BladeRfTunerController(bus, portAddress, tunerErrorListener), tunerErrorListener, channelizerType);
            case FUNCUBE_DONGLE_PRO:
                TargetDataLine tdl1 = MixerManager.getTunerTargetDataLine(MixerTunerType.FUNCUBE_DONGLE_PRO);
                if(tdl1 != null)
                {
                    FCD1TunerController controller = new FCD1TunerController(tdl1, bus, portAddress, tunerErrorListener);
                    return new FCDTuner(controller, tunerErrorListener);
                }
                throw new SourceException("Unable to find matching tuner sound card mixer");
            case FUNCUBE_DONGLE_PRO_PLUS:
                TargetDataLine tdl2 = MixerManager.getTunerTargetDataLine(MixerTunerType.FUNCUBE_DONGLE_PRO_PLUS);
                if(tdl2 != null)
                {
                    FCD2TunerController controller = new FCD2TunerController(tdl2, bus, portAddress, tunerErrorListener);
                    return new FCDTuner(controller, tunerErrorListener);
                }
                throw new SourceException("Unable to find matching tuner sound card mixer");
            case HACKRF:
                return new HackRFTuner(new HackRFTunerController(bus, portAddress, tunerErrorListener), tunerErrorListener, channelizerType);
            case HYDRASDR:
                return new HydraSdrTuner(new HydraSdrTunerController(bus, portAddress, tunerErrorListener), tunerErrorListener, channelizerType);
            case LIMESDR:
                return new LimeSdrTuner(new LimeSdrTunerController(bus, portAddress, tunerErrorListener), tunerErrorListener, channelizerType);
            case RTL2832:
                return new RTL2832Tuner(new RTL2832TunerController(bus, portAddress, tunerErrorListener), tunerErrorListener, channelizerType);
            default:
                throw new SourceException("Unrecognized tuner class [" + tunerClass + "]");
        }
    }

    /**
     * Creates a tuner configuration for the specified tuner type, unique ID and name
     */
    public static TunerConfiguration getTunerConfiguration(TunerType type, String uniqueID)
    {
        switch(type)
        {
            case AIRSPY_HF_PLUS:
                return new AirspyHfTunerConfiguration(uniqueID);
            case AIRSPY_R820T:
                return new AirspyTunerConfiguration(uniqueID);
            case BLADERF:
                return new BladeRfTunerConfiguration(uniqueID);
            case ELONICS_E4000:
                return new E4KTunerConfiguration(uniqueID);
            case FITIPOWER_FC0012:
                return new FC0012TunerConfiguration(uniqueID);
            case FITIPOWER_FC0013:
                return new FC0013TunerConfiguration(uniqueID);
            case FCI_FC2580:
                return new FC2580TunerConfiguration(uniqueID);
            case FUNCUBE_DONGLE_PRO:
                return new FCD1TunerConfiguration(uniqueID);
            case FUNCUBE_DONGLE_PRO_PLUS:
                return new FCD2TunerConfiguration(uniqueID);
            case HACKRF_JAWBREAKER:
            case HACKRF_ONE:
            case HACKRF_PRO:
            case HACKRF_RAD1O:
                return new HackRFTunerConfiguration(uniqueID);
            case HYDRASDR_R828D:
                return new HydraSdrTunerConfiguration(uniqueID);
            case LIMESDR:
                return new LimeSdrTunerConfiguration(uniqueID);
            case RAFAELMICRO_R820T:
                return new R820TTunerConfiguration(uniqueID);
            case RAFAELMICRO_R828D:
                return new R828DTunerConfiguration(uniqueID);
            case RECORDING:
                return RecordingTunerConfiguration.create();
            case RSP_1:
                return new Rsp1TunerConfiguration(uniqueID);
            case RSP_1A:
                return new Rsp1aTunerConfiguration(uniqueID);
            case RSP_1B:
                return new Rsp1bTunerConfiguration(uniqueID);
            case RSP_2:
                return new Rsp2TunerConfiguration(uniqueID);
            case RSP_DUO_1:
                return new RspDuoTuner1Configuration(uniqueID);
            case RSP_DUO_2:
                return new RspDuoTuner2Configuration(uniqueID);
            case RSP_DX:
                return new RspDxTunerConfiguration(uniqueID);
            default:
                throw new IllegalArgumentException("Unrecognized tuner type [" + type.name() + "]");
        }
    }

    /**
     * Creates a tuner editor gui for the specified tuner
     */
    public static TunerEditor getEditor(UserPreferences userPreferences, DiscoveredTuner discoveredTuner,
                                        TunerManager tunerManager)
    {
        switch(discoveredTuner.getTunerClass())
        {
            case AIRSPY:
                return new AirspyTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case AIRSPY_HF:
                return new AirspyHfTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case FUNCUBE_DONGLE_PRO:
                return new FCD1TunerEditor(userPreferences, tunerManager, discoveredTuner);
            case FUNCUBE_DONGLE_PRO_PLUS:
                return new FCD2TunerEditor(userPreferences, tunerManager, discoveredTuner);
            case BLADERF:
                return new BladeRfTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case HACKRF:
                return new HackRFTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case HYDRASDR:
                return new HydraSdrTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case LIMESDR:
                return new LimeSdrTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case RSP:
                if(discoveredTuner instanceof DiscoveredRspTuner discoveredRspTuner)
                {
                    switch(discoveredRspTuner.getModel())
                    {
                        case RSP1:
                            return new Rsp1TunerEditor(userPreferences, tunerManager, discoveredRspTuner);
                        case RSP1A:
                            return new Rsp1aTunerEditor(userPreferences, tunerManager, discoveredRspTuner);
                        case RSP1B:
                            return new Rsp1bTunerEditor(userPreferences, tunerManager, discoveredRspTuner);
                        case RSP2:
                            return new Rsp2TunerEditor(userPreferences, tunerManager, discoveredRspTuner);
                        case RSPDX:
                        case RSPDX_R2:
                            return new RspDxTunerEditor(userPreferences, tunerManager, discoveredRspTuner);
                        case RSPDUO:
                            if(discoveredRspTuner instanceof DiscoveredRspDuoTuner1 duoTuner1)
                            {
                                return new RspDuoTuner1Editor(userPreferences, tunerManager, duoTuner1);
                            }
                            else if(discoveredRspTuner instanceof DiscoveredRspDuoTuner2 duoTuner2)
                            {
                                return new RspDuoTuner2Editor(userPreferences, tunerManager, duoTuner2);
                            }
                            else
                            {
                                throw new IllegalArgumentException("Unrecognized RSPduo device type:" +
                                        discoveredRspTuner.getClass());
                            }
                        case UNKNOWN:
                            throw new IllegalArgumentException("Unrecognized RSP device type: " +
                                    discoveredRspTuner.getModel());
                    }
                }
                throw new IllegalArgumentException("Unrecognized discovered RSP tuner class: " +
                        discoveredTuner.getClass());
            case RECORDING_TUNER:
                return new RecordingTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case RTL2832:
                if(discoveredTuner.hasTuner())
                {
                    switch(discoveredTuner.getTuner().getTunerType())
                    {
                        case ELONICS_E4000:
                        case FCI_FC2580:
                        case FITIPOWER_FC0012:
                        case FITIPOWER_FC0013:
                        case RAFAELMICRO_R820T:
                        case RAFAELMICRO_R828D:
                            return new RTL2832TunerEditor(userPreferences, tunerManager, discoveredTuner);
                    }
                }
                return new RTL2832UnknownTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case TEST_TUNER:
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("Unsupported tuner class: " + discoveredTuner.getTunerClass());
        }
    }
}

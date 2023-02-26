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
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.dmr.audio.DMRCallSequenceRecorder;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.DMRBurst;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.DMRMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.data.packet.DMRPacketMessage;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.LRRPPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.Identity;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.Token;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.record.binary.BinaryReader;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DMR Sync Detector and Message Framer.  Includes capability to detect PLL out-of-phase lock errors
 * and issue phase corrections.
 */
public class DMRMessageFramer implements Listener<Dibit>, IDMRBurstDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRMessageFramer.class);

    private static final double DMR_BIT_RATE = 9600.0;

    /**
     * Provides sync detection and burst framing
     */
    private DMRBurstFramer mBurstFramer;

    /**
     * External listener for messages produced by this framer
     */
    private Listener<IMessage> mMessageListener;

    /**
     * External listener for sync state information
     */
    private ISyncDetectListener mSyncDetectListener;

    /**
     * Tracks current time for use with framed bursts/messages.  This value is updated externally from sample buffer
     * timestamps and internally as each framed message is received or sync loss is processed.
     */
    private long mCurrentTime = System.currentTimeMillis();

    /**
     * Constructs an instance
     *
     * @param phaseLockedLoop to receive PLL phase lock error corrections that are identified by the DMRBurstFramer
     * when known phase misaligned sync patterns are detected.
     */
    public DMRMessageFramer(IPhaseLockedLoop phaseLockedLoop)
    {
        mBurstFramer = new DMRBurstFramer(this, phaseLockedLoop);
    }

    /**
     * Constructs an instance.
     */
    public DMRMessageFramer()
    {
        this(null);
    }

    /**
     * Registers a sync detect listener to be notified each time a sync pattern and NID are detected.
     */
    public void setSyncDetectListener(ISyncDetectListener syncDetectListener)
    {
        mSyncDetectListener = syncDetectListener;
    }

    /**
     * Current timestamp or timestamp of incoming message buffers that is continuously updated to as
     * close as possible to the bits processed for the expected baud rate.
     *
     * @return current time
     */
    private long getTimestamp()
    {
        return mCurrentTime;
    }

    /**
     * Sets the current time.  This should be invoked by an incoming message buffer stream.
     *
     * @param currentTime
     */
    public void setCurrentTime(long currentTime)
    {
        mCurrentTime = currentTime;
    }

    /**
     * Updates the current timestamp based on the number of bits processed versus the bit rate per second
     * in order to keep an accurate running timestamp to use for timestamped message creation.
     *
     * @param bitsProcessed thus far
     */
    private void updateBitsProcessed(int bitsProcessed)
    {
        if(bitsProcessed > 0)
        {
            mCurrentTime += (long)((double)bitsProcessed / DMR_BIT_RATE * 1000.0);
        }
    }

    /**
     * Registers the listener for messages produced by this message framer
     *
     * @param messageListener to receive framed and decoded messages
     */
    public void setListener(Listener<IMessage> messageListener)
    {
        mMessageListener = messageListener;
    }

    /**
     * Primary method for streaming decoded symbol dibits for message framing.
     *
     * @param dibit to process
     */
    @Override
    public void receive(Dibit dibit)
    {
        mBurstFramer.receive(dibit);
    }

    /**
     * Primary method for streaming decoded symbol byte arrays.
     *
     * @param buffer to process into a stream of dibits for processing.
     */
    public void receive(ByteBuffer buffer)
    {
        for(byte value : buffer.array())
        {
            for(int x = 0; x <= 3; x++)
            {
                receive(Dibit.parse(value, x));
            }
        }
    }

    /**
     * DMR burst detection processing.  Processes DMR burst/messages from external burst framer/producer.
     *
     * @param message bits for the burst/message and any bit errors detected in the sync pattern
     * @param syncPattern that was detected for the burst
     */
    @Override
    public void burstDetected(CorrectedBinaryMessage message, DMRSyncPattern syncPattern, int timeslot)
    {
        //Each burst is 288 bits
        updateBitsProcessed(288);

        //This allows the frequency error monitor to be notified when we're in sync
        if(mSyncDetectListener != null)
        {
            mSyncDetectListener.syncDetected(0);
        }

        CACH cach = null;

        if(syncPattern.hasCACH())
        {
            cach = CACH.getCACH(message);
        }

        if(mMessageListener != null)
        {
            DMRMessage dmrMessage = DMRMessageFactory.create(syncPattern, message, cach, getTimestamp(), timeslot);

            if(dmrMessage != null)
            {
                mMessageListener.receive(dmrMessage);
            }
        }
    }

    /**
     * Processes a sync loss to track how many bits were processed and update message listeners.
     *
     * @param bitsProcessed since the last sync detect
     */
    @Override
    public void syncLost(int bitsProcessed, int timeslot)
    {
        updateBitsProcessed(bitsProcessed);

        dispatchSyncLoss(bitsProcessed, timeslot);

        if(mSyncDetectListener != null)
        {
            mSyncDetectListener.syncLost(bitsProcessed);
        }
    }

    /**
     * Creates a sync loss message and dispatches it to the message listener
     *
     * @param bitsProcessed without a sync detection
     * @param timeslot that lost the sync
     */
    private void dispatchSyncLoss(int bitsProcessed, int timeslot)
    {
        if(bitsProcessed > 0 && mMessageListener != null)
        {
            mMessageListener.receive(new SyncLossMessage(getTimestamp(), bitsProcessed, Protocol.DMR, timeslot));
        }
    }

    public static class MessageListener implements Listener<IMessage>
    {
        private boolean mHasDMRData = false;
        public int mTS0Count = 0;
        public int mTS1Count = 0;

        @Override
        public void receive(IMessage message)
        {
            if(message instanceof DMRBurst && ((DMRBurst)message).hasCACH())
            {
                CACH cach = ((DMRBurst)message).getCACH();
                mLog.info(cach.toString() + " TS:" + message.getTimeslot() + " " + message.toString());
            }
            else
            {
                mLog.info("     TS:" + message.getTimeslot() + " " + message.toString());
            }

            if(message instanceof DMRPacketMessage dpm)
            {
                if(dpm.getPacket() instanceof LRRPPacket lrrp)
                {
                    mLog.info("LRRP: " + lrrp.toString());
                    mLog.info("MESSAGE: " + lrrp.getMessage().toHexString());
                }
            }

//            if(message instanceof UDTHeader)
//            {
//                mLog.info("TS:" + message.getTimeslot() + " " + message.toString());
//            }

//            if(message instanceof UnknownCSBKMessage && message.isValid())
//            {
//                mLog.info("TS:" + ((DMRMessage)message).getTimeslot() + " " + message.toString());
//            }
        }

        public boolean hasData()
        {
            return mHasDMRData;
        }

        public void reset()
        {
            mHasDMRData = true;
        }
    }

    public static class LrrpProcessor implements Listener<IMessage>
    {
        private Map<Integer, List<LRRPPacket>> mLrrpMap = new HashMap<>();

        public LrrpProcessor()
        {

        }

        @Override
        public void receive(IMessage message)
        {
            if(message instanceof DMRPacketMessage dpm)
            {
                if(dpm.getPacket() instanceof LRRPPacket lrrp)
                {
                    process(lrrp);
                }
            }
        }

        public void process(LRRPPacket lrrpPacket)
        {
            Identity identity = getIdentity(lrrpPacket);

            if(identity != null)
            {
                List<LRRPPacket> packets = mLrrpMap.get(identity.getID());

                if(packets == null)
                {
                    packets = new ArrayList<>();
                }

                packets.add(lrrpPacket);

                mLrrpMap.put(identity.getID(), packets);
            }
        }

        public void log()
        {
            mLog.info("*************** DUMPING LRRP PACKETS****************************");
            List<Integer> ids = new ArrayList(mLrrpMap.keySet());
            Collections.sort(ids);

            for(Integer id: ids)
            {
                mLog.info("");
                mLog.info("###################### ID: " + id + " ###################################");
                List<LRRPPacket> packets = mLrrpMap.get(id);

                for(LRRPPacket lrrp: packets)
                {
                    mLog.info("PACKET: " + lrrp.toString());
                    mLog.info("MESSAGE: " + lrrp.getMessage().toHexString());
                }
            }
        }

        private static Identity getIdentity(LRRPPacket packet)
        {
            for(Token token: packet.getTokens())
            {
                if(token instanceof Identity)
                {
                    return (Identity)token;
                }
            }

            return null;
        }
    }

    public static void main(String[] args)
    {
        String path = "/media/denny/Lexar/Recordings/DMR/";

        //Con+ Traffic + Voice
//        String file = path + "20200513_143340_9600BPS_DMR_SaiaNet_Onondaga_Control.bits"; //Enh GPS Revert Window Annce
//        String file = path + "20200514_062135_9600BPS_DMR_SaiaNet_Onondaga_LCN_3_Control.bits"; //Neighbor only
//        String file = path + "20200514_063507_9600BPS_DMR_SaiaNet_Onondaga_LCN_3_Control.bits"; //GPS Window Grant 2579
//        String file = path + "20200514_064224_9600BPS_DMR_SaiaNet_Onondaga_LCN_3_Control.bits"; //GPS Window Grant 5056035
//        String file = path + "20200514_131623_9600BPS_DMR_SaiaNet_Onondaga_LCN_3_Control.bits"; //GPS Grant: 5074193
        String file = path + "20200514_133947_9600BPS_DMR_SaiaNet_Onondaga_LCN_4.bits"; //<<<<<<<------ Basic Encryption
//        String file = path + "20200514_142249_9600BPS_DMR_SaiaNet_Onondaga_LCN_4.bits";
//        String file = path + "20200514_144534_9600BPS_DMR_SaiaNet_Onondaga_LCN_3_Control.bits"; //Con+ Control w/GPS Window Announce

        //Con+ Multi-Site with Network Frequency Map - This file is really General Motors Con+ - Tarrant County site
//        String file = path + "20200716_235004_9600BPS_DMR_Azle_Communications_(Tier_3)_Dallas_Control.bits";

        //Cap-Max Tier III CC
//        String file = path + "20200710_053632_9600BPS_DMR_Niles_Radio_Coconino_Control.bits";

        //Cap+ Multi-Site 1 - Traffic LCN 2
//        String file = path + "20200716_210133_9600BPS_DMR_Aerowave_Technologies_Dallas_LCN_2.bits";
//        String file = path + "20200716_212309_9600BPS_DMR_Aerowave_Technologies_Dallas_LCN_2.bits";

        //Cap+ Multi-Site Enhanced GPS Channel
//        String file = path + "20200714_224018_9600BPS_DMR_Farmers_Electric_Cooperative_Hunt_LCN_3.bits"; //This may have PLL mis-align issues

        //Hytera Short Data = Proprietary (encrypted)
//        String file = path + "20200716_222839_9600BPS_DMR_SystemUnk_SiteUnk_Unk.bits"; //Hytera short data packets
//        String file = path + "20200716_223551_9600BPS_DMR_SystemUnk_SiteUnk_Unk.bits";

        //Motorola GPS Revert channel
//        String file = path + "20200730_085433_9600BPS_DMR_Otsego_County_Road_Commission_Alpine_Center_Road_Commission.bits";

        //Hytera XPT (pseudo trunk) beaconing and voice
//        String file = path + "20200729_161450_9600BPS_DMR_JPJ_Communications_(DMR)_Madison_Control.bits"; //PLL lock oscillating
//        String file = path + "20200729_162200_9600BPS_DMR_JPJ_Communications_(DMR)_Madison_Control.bits";
//        String file = path + "Hytera XPT/20200816_161126_9600BPS_DMR_JPJ_Communications_(DMR)_Madison_Control.bits"; //analzing this <<<<<<<<<<<<<<<<<<<<<<<<<<

        //Hytera Tier III - CSBKO 40
//        String file = path + "20200716_234510_9600BPS_DMR_Azle_Communications_(Tier_3)_Dallas_Control.bits";
//        String file = path + "20200812_061410_9600BPS_DMR_Azle_Communications_(Tier_3)_Dallas_Control.bits";
//        String file = path + "20200716_234510_9600BPS_DMR_Azle_Communications_(Tier_3)_Dallas_Control.bits"; //MBC-channel grant

        //UDT Header And Blocks
//        String file = path + "20200730_090634_9600BPS_DMR_Otsego_County_Road_Commission_Alpine_Center_Road_Commission.bits";

        //Cap+ BP Scrambling
//        String file = path + "20200829_065610_9600BPS_DMR_Albany_Medical_Center_Albany_LCN_3.bits";

//        String file = "/home/denny/SDRTrunk/recordings/20200927_054837_9600BPS_DMR_Blair_Communications_(Capacity_Plus)_Dallas_Control_9.bits";

        DecodeConfigDMR config = new DecodeConfigDMR();

        Broadcaster<IMessage> messageBroadcaster = new Broadcaster<>();
        MessageListener messageListener = new MessageListener();
        messageBroadcaster.addListener(messageListener);

        LrrpProcessor lrrpProcessor = new LrrpProcessor();
        messageBroadcaster.addListener(lrrpProcessor);

        DMRCallSequenceRecorder ambeRecorder = new DMRCallSequenceRecorder(new UserPreferences(), 123456789l,
                "Denny System", "Denny Site");
        messageBroadcaster.addListener(ambeRecorder);

        boolean multi = false;

        if(multi)
        {
            try
            {
                DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path), "*.bits");

                stream.forEach(new Consumer<Path>()
                {
                    @Override
                    public void accept(Path path)
                    {
                        mLog.info("Processing: " + path.toString());
                        DMRMessageFramer messageFramer = new DMRMessageFramer(null);
                        DMRMessageProcessor messageProcessor = new DMRMessageProcessor(config);
                        messageFramer.setListener(messageProcessor);
                        messageProcessor.setMessageListener(messageBroadcaster);

                        try(BinaryReader reader = new BinaryReader(path, 200))
                        {
                            while(reader.hasNext())
                            {
                                ByteBuffer buffer = reader.next();
                                messageFramer.receive(buffer);
                            }
                        }
                        catch(Exception ioe)
                        {
                            ioe.printStackTrace();
                        }

                        if(!messageListener.hasData())
                        {
//                            mLog.info("Has Data: " + listener.hasData() + " File:" + path.toString());
//                            try
//                            {
//                                Files.delete(path);
//                            }
//                            catch(IOException ioe)
//                            {
//                                ioe.printStackTrace();
//                            }
                        }

                        messageListener.reset();

                        System.out.println("TS0 VOICE:" + messageListener.mTS0Count + " TS1 VOICE:" + messageListener.mTS1Count);
                        messageListener.mTS0Count = 0;
                        messageListener.mTS1Count = 0;
                    }
                });
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }

            lrrpProcessor.log();
        }
        else
        {
            DMRMessageFramer messageFramer = new DMRMessageFramer(null);
            DMRMessageProcessor messageProcessor = new DMRMessageProcessor(config);
            messageFramer.setListener(messageProcessor);
            messageProcessor.setMessageListener(messageBroadcaster);

            try(BinaryReader reader = new BinaryReader(Path.of(file), 200))
            {
                while(reader.hasNext())
                {
                    ByteBuffer buffer = reader.next();
                    messageFramer.receive(buffer);
                }
            }
            catch(Exception ioe)
            {
                ioe.printStackTrace();
            }

            System.out.println("TS0 VOICE:" + messageListener.mTS0Count + " TS1 VOICE:" + messageListener.mTS1Count);
            messageListener.mTS0Count = 0;
            messageListener.mTS1Count = 0;
        }
    }
}

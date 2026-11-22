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
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.c4fm.C4FMMessageFramer;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.log.ApplicationLog;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageProviderModule;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.audio.P25P2AudioModule;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.AudioRecordingManager;
import io.github.dsheirer.record.binary.BinaryReader;
import io.github.dsheirer.sample.Listener;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P25 Phase 2 message framer.  Receives symbol decisions (dibits) and sync detection notifications from the symbol
 * processor and frames super frame fragments using the super frame detector.
 */
public class P25P2MessageFramer extends C4FMMessageFramer
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2MessageFramer.class);
    private final P25P2SuperFrameDetector mSuperFrameDetector = new P25P2SuperFrameDetector();
    private boolean mRunning = false;

    /**
     * Constructs an instance
     */
    public P25P2MessageFramer()
    {
    }

    /**
     * Sets or updates the scramble parameters for the current channel
     * @param scrambleParameters to apply
     */
    public void setScrambleParameters(ScrambleParameters scrambleParameters)
    {
        mSuperFrameDetector.setScrambleParameters(scrambleParameters);
    }

    /**
     * Registers the listener for messages produced by this message framer
     *
     * @param messageListener to receive framed and decoded messages
     */
    public void setListener(Listener<IMessage> messageListener)
    {
        mSuperFrameDetector.setListener(message -> {
            if(mRunning)
            {
                messageListener.receive(message);
            }
        });
    }

    /**
     * Sets or updates the current dibit stream time from an incoming sample buffer.
     * @param timestamp to use as a reference timestamp.
     */
    public void setTimestamp(long timestamp)
    {
        mSuperFrameDetector.setTimestamp(timestamp);
    }

    /**
     * Starts this framer dispatching messages
     */
    public void start()
    {
        mRunning = true;
    }

    /**
     * Stops this framer from dispatching messages
     */
    public void stop()
    {
        mRunning = false;
    }

    /**
     * Notification from the symbol processor that a sync pattern was detected, where the final sync dibit is the most
     * recently processed dibit.
     */
    @Override
    public void syncDetected()
    {
        mSuperFrameDetector.syncDetected();
    }

    /**
     * Primary method for streaming decoded symbol dibits for message framing.
     *
     * @param dibit to process
     * @return true if a valid super frame fragment was produced.
     */
    @Override
    public boolean process(Dibit dibit)
    {
        return mSuperFrameDetector.process(dibit);
    }

    /**
     * Primary method for streaming decoded symbol dibits and soft symbols for message framing.
     *
     * @param dibit to process
     * @param softSymbol for the dibit (radians), for soft decision decoding
     * @return true if a valid super frame fragment was produced.
     */
    @Override
    public boolean process(Dibit dibit, float softSymbol)
    {
        return mSuperFrameDetector.process(dibit, softSymbol);
    }

    /**
     * Processes the dibit and detects sync patterns from the dibit stream, for use when processing a stream of dibits
     * without a symbol processor (ie replaying a recorded bit stream).
     *
     * @param dibit to process
     */
    public void processWithHardSyncDetect(Dibit dibit)
    {
        mSuperFrameDetector.processWithHardSyncDetect(dibit);
    }

    /**
     * Processes the decoded symbol byte array using hard sync detection.
     *
     * @param buffer to process into a stream of dibits for processing.
     */
    public void receive(ByteBuffer buffer)
    {
        for(byte value : buffer.array())
        {
            for(int x = 0; x <= 3; x++)
            {
                processWithHardSyncDetect(Dibit.parse(value, x));
            }
        }
    }

    public static void main(String[] args)
    {
        UserPreferences userPreferences = new UserPreferences();
        userPreferences.getJmbeLibraryPreference().setPathJmbeLibrary(Path.of("/home/denny/JMBE/jmbe-1.0.3.jar"));
        ApplicationLog applicationLog = new ApplicationLog(userPreferences);
        applicationLog.start();

//        Path directory = Paths.get("/home/denny/SDRTrunk/recordings");
        Path directory = Paths.get("/home/denny/temp/Harris P25-2 Logs/bits");
//        Path directory = Paths.get("/media/denny/500G1EXT4/PBITRecordings");

//        ScrambleParameters scrambleParameters = new ScrambleParameters(781824, 1186, 1189);  //Nugent
        ScrambleParameters scrambleParameters = new ScrambleParameters(123654, 813, 10);  //Hills
//        ScrambleParameters scrambleParameters = new ScrambleParameters(1, 972, 972));
//        ScrambleParameters scrambleParameters = new ScrambleParameters(781824, 686, 677); //CNYICC - Rome

//        Channel channel = new Channel("Phase 2 Test", Channel.ChannelType.STANDARD);
        Channel channel = new Channel("Phase 2 Test", Channel.ChannelType.TRAFFIC);

        channel.setDecodeConfiguration(new DecodeConfigP25Phase2());
        AliasList aliasList = new AliasList("Test Alias List");
//        Alias alias = new Alias("TG Range 1-65535");
//        alias.addAliasID(new Record());
//        alias.addAliasID(new TalkgroupRange(Protocol.APCO25, 1, 65535));
//        alias.addAliasID(new Talkgroup(Protocol.APCO25, 11857));
//        alias.addAliasID(new Talkgroup(Protocol.APCO25, 12601));
//        aliasList.addAlias(alias);
        AudioRecordingManager recordingManager = new AudioRecordingManager(userPreferences);
        recordingManager.start();
        ProcessingChain processingChain = new ProcessingChain(channel, new AliasModel());

        processingChain.addAudioSegmentListener(recordingManager);
        P25TrafficChannelManager trafficChannelManager = new P25TrafficChannelManager(channel);
        PatchGroupManager patchGroupManager = new PatchGroupManager();
        processingChain.addModule(new P25P2DecoderState(channel, P25P2Message.TIMESLOT_1, trafficChannelManager,
                patchGroupManager));
        processingChain.addModule(new P25P2DecoderState(channel, P25P2Message.TIMESLOT_2, trafficChannelManager,
                patchGroupManager));
        processingChain.addModule(new P25P2AudioModule(userPreferences, P25P2Message.TIMESLOT_1, aliasList));
        processingChain.addModule(new P25P2AudioModule(userPreferences, P25P2Message.TIMESLOT_2, aliasList));
        MessageProviderModule messageProviderModule = new MessageProviderModule();
        processingChain.addModule(messageProviderModule);

        try(OutputStream logOutput = Files.newOutputStream(directory.resolve("log.txt")))
        {
            try
            {
//                DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*042102*TRAFFIC.bits");
//                DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*042102*.bits");
                DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*214601*Hills*TRAFFIC.bits");

                stream.forEach(new Consumer<Path>()
                               {
                                   @Override
                                   public void accept(Path path)
                                   {
                                       mLog.debug("\n\nProcessing:" + path.toString() + "\n\n");
                                       try
                                       {
                                           logOutput.write(("Processing:" + path.toString() + "\n").getBytes());
                                       }
                                       catch(IOException ioe)
                                       {
                                           mLog.error("Error", ioe);
                                       }

                                       processingChain.start();

                                       P25P2MessageFramer messageFramer = new P25P2MessageFramer();
                                       messageFramer.start();
                                       messageFramer.setScrambleParameters(scrambleParameters);
                                       P25P2MessageProcessor messageProcessor = new P25P2MessageProcessor();
                                       messageFramer.setListener(messageProcessor);
                                       messageProcessor.setMessageListener(new Listener<IMessage>()
                                       {
                                           @Override
                                           public void receive(IMessage message)
                                           {
                                               messageProviderModule.receive(message);
                                               try
                                               {
                                                   logOutput.write(message.toString().getBytes());
                                                   logOutput.write("\n".getBytes());
                                               }
                                               catch(IOException ioe)
                                               {
                                                   mLog.error("Error", ioe);
                                               }

                                               mLog.debug(message.toString());
                                           }
                                       });

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

                                       mLog.debug("**STOPPING PROCESSING CHAIN**");

                                       processingChain.stop();

                                       mLog.debug("\n============================================================================================================================\n");
                                   }
                               }
                );
            }
            catch(IOException ioe)
            {
                mLog.error("Error", ioe);
            }
        }
        catch(IOException ioe)
        {
            mLog.error("Error", ioe);
        }

        recordingManager.stop();
    }
}

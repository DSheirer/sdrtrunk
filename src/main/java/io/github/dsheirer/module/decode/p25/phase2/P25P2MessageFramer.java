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
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
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
 * P25 Sync Detector and Message Framer.  Includes capability to detect PLL out-of-phase lock errors
 * and issue phase corrections.
 */
public class P25P2MessageFramer implements Listener<Dibit>
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2MessageFramer.class);
    private P25P2SuperFrameDetector mSuperFrameDetector;

    /**
     * Constructs an instance
     * @param phaseLockedLoop for use with the super frame detector
     */
    public P25P2MessageFramer(IPhaseLockedLoop phaseLockedLoop)
    {
        mSuperFrameDetector = new P25P2SuperFrameDetector(phaseLockedLoop);
    }

    /**
     * Sets or updates the scramble parameters for the current channel
     * @param scrambleParameters
     */
    public void setScrambleParameters(ScrambleParameters scrambleParameters)
    {
        if(mSuperFrameDetector != null)
        {
            mSuperFrameDetector.setScrambleParameters(scrambleParameters);
        }
    }

    /**
     * Sets the sample rate for the sync detector
     */
    public void setSampleRate(double sampleRate)
    {
        mSuperFrameDetector.setSampleRate(sampleRate);
    }

    /**
     * Registers a sync detect listener to be notified each time sync is detected or lost.
     *
     * Note: this actually registers the listener on the enclosed super frame detector which has access to the
     * actual sync pattern detector instance.
     */
    public void setSyncDetectListener(ISyncDetectListener listener)
    {
        mSuperFrameDetector.setSyncDetectListener(listener);
    }

    /**
     * Registers the listener for messages produced by this message framer
     *
     * @param messageListener to receive framed and decoded messages
     */
    public void setListener(Listener<IMessage> messageListener)
    {
        mSuperFrameDetector.setListener(messageListener);
    }

    /**
     * Primary method for streaming decoded symbol dibits for message framing.
     *
     * @param dibit to process
     */
    @Override
    public void receive(Dibit dibit)
    {
        mSuperFrameDetector.receive(dibit);
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

                                       P25P2MessageFramer messageFramer = new P25P2MessageFramer(null);
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

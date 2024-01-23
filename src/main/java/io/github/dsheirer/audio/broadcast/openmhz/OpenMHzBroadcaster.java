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

 package io.github.dsheirer.audio.broadcast.openmhz;

import com.google.common.net.HttpHeaders;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferenceDecoder;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.ConfigurationLongIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.util.ThreadPool;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;



 /**
  * Audio broadcaster to push completed audio recordings to the OpenMHz call upload API.
  *
  */
 public class OpenMHzBroadcaster extends AbstractAudioBroadcaster<OpenMHzConfiguration>
 {
     private final static Logger mLog = LoggerFactory.getLogger(OpenMHzBroadcaster.class);

     private static final String ENCODING_TYPE_MP3 = "mp3";
     private static final String MULTIPART_TYPE = "multipart";
     private static final String DEFAULT_SUBTYPE = "form-data";
     private static final String MULTIPART_FORM_DATA = MULTIPART_TYPE + "/" + DEFAULT_SUBTYPE;
     private static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
     private Queue<AudioRecording> mAudioRecordingQueue = new LinkedTransferQueue<>();
     private ScheduledFuture<?> mAudioRecordingProcessorFuture;
     private HttpClient mHttpClient = HttpClient.newBuilder()
         .version(HttpClient.Version.HTTP_2)
         .followRedirects(HttpClient.Redirect.NORMAL)
         .connectTimeout(Duration.ofSeconds(20))
         .build();
     private long mLastConnectionAttempt;
     private long mConnectionAttemptInterval = 5000; //Every 5 seconds
     @Resource
     private AliasModel mAliasModel;

     /**
      * Constructs an instance of the broadcaster
      * @param config to use
      */
     public OpenMHzBroadcaster(OpenMHzConfiguration config)
     {
         super(config);
     }

     /**
      * Starts the audio recording processor thread
      */
     @Override
     public void start()
     {
         setBroadcastState(BroadcastState.CONNECTING);
         String response = testConnection(getBroadcastConfiguration());
         mLastConnectionAttempt = System.currentTimeMillis();

         if(response == "OK")
         {
             setBroadcastState(BroadcastState.CONNECTED);
         }
         else if (response == "No Response")
         {
            setBroadcastState(BroadcastState.NO_SERVER);
            mLog.error("Error connecting to OpenMHz server [Server not found or not reachable]");
         }
         else
         {
             mLog.error("Error connecting to OpenMHz server on startup [" + response + "]");
             setBroadcastState(BroadcastState.ERROR);
         }

         if(mAudioRecordingProcessorFuture == null)
         {
             mAudioRecordingProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(new AudioRecordingProcessor(),
                 0, 500, TimeUnit.MILLISECONDS);
         }
     }

     /**
      * Stops the audio recording processor thread
      */
     @Override
     public void stop()
     {
         if(mAudioRecordingProcessorFuture != null)
         {
             mAudioRecordingProcessorFuture.cancel(true);
             mAudioRecordingProcessorFuture = null;
             dispose();
             setBroadcastState(BroadcastState.DISCONNECTED);
         }
     }

     /**
      * Prepares for disposal
      */
     @Override
     public void dispose()
     {
         AudioRecording audioRecording = mAudioRecordingQueue.poll();

         while(audioRecording != null)
         {
             audioRecording.removePendingReplay();
             audioRecording = mAudioRecordingQueue.poll();
         }
     }

     /**
      * Indicates if this broadcaster continues to have successful connections to and transactions with the remote
      * server.  If there is a connectivity or other issue, the broadcast state is set to temporary error and
      * the audio processor thread will persistently invoke this method to attempt a reconnect.
      */
     private boolean connected()
     {
         if(getBroadcastState() != BroadcastState.CONNECTED &&
             (System.currentTimeMillis() - mLastConnectionAttempt > mConnectionAttemptInterval))
         {
             setBroadcastState(BroadcastState.CONNECTING);

             String response = testConnection(getBroadcastConfiguration());
             mLastConnectionAttempt = System.currentTimeMillis();

             if(response == "OK")
             {
                 setBroadcastState(BroadcastState.CONNECTED);
             }
             else if (response == "No Response")
             {
                setBroadcastState(BroadcastState.NO_SERVER);
                mLog.error("Error reconnecting to OpenMHz server [Server not found or not reachable]");
             }
             else
             {
                 setBroadcastState(BroadcastState.ERROR);
                 mLog.error("Error reconnecting to OpenMHz server [" + response + "]");
             }

         }

         return getBroadcastState() == BroadcastState.CONNECTED;
     }

     @Override
     public int getAudioQueueSize()
     {
         return mAudioRecordingQueue.size();
     }

     @Override
     public void receive(AudioRecording audioRecording)
     {
         mAudioRecordingQueue.offer(audioRecording);
         broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_QUEUE_CHANGE));
     }

     /**
      * Indicates if the audio recording is non-null and not too old, meaning that the age of the recording has not
      * exceeded the max age value indicated in the broadcast configuration.  Audio recordings that are too old will be
      * deleted to ensure that the in-memory queue size doesn't blow up.
      * @param audioRecording to test
      * @return true if the recording is valid
      */
     private boolean isValid(AudioRecording audioRecording)
     {
         return audioRecording != null && System.currentTimeMillis() - audioRecording.getStartTime() <=
             getBroadcastConfiguration().getMaximumRecordingAge();
     }

     /**
      * Processes any enqueued audio recordings. This method employs asynchronous
      * interaction with the server, so multiple audio recording uploads can occur simultaneously.
      */
     private void processRecordingQueue()
     {


         while(connected() && !mAudioRecordingQueue.isEmpty())
         {
             final AudioRecording audioRecording = mAudioRecordingQueue.poll();
             broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_QUEUE_CHANGE));

             if(isValid(audioRecording) && audioRecording.getRecordingLength() > 0)
             {
                 int durationSeconds = (int)(audioRecording.getRecordingLength() / 1E3f);
                 long timestampSeconds = (int)(audioRecording.getStartTime() / 1E3);
                 String talkgroup = getTo(audioRecording);
                 String radioId = getFrom(audioRecording);
                 Long frequency = getFrequency(audioRecording);
                 String patches = getPatches(audioRecording);
                 String talkgroupLabel = getTalkgroupLabel(audioRecording);
                 String talkgroupGroup = getTalkgroupGroup(audioRecording);
                 String systemLabel = getSystemLabel(audioRecording);

                 try
                 {
                     byte[] audioBytes = null;

                     try
                     {
                         audioBytes = Files.readAllBytes(audioRecording.getPath());
                     }
                     catch(IOException e)
                     {
                         mLog.error("OpenMHz - audio recording file not found - ignoring upload");
                     }

                     if(audioBytes != null)
                     {
                        String uri = getBroadcastConfiguration().getHost() + "/" + getBroadcastConfiguration().getSystemName() + "/upload";

                         OpenMHzBuilder bodyBuilder = new OpenMHzBuilder();
                             bodyBuilder
                             .addFile(audioBytes)
                             .addPart(FormField.FREQ, frequency)
                             .addPart(FormField.START_TIME, timestampSeconds)
                             .addPart(FormField.STOP_TIME, timestampSeconds)
                             .addPart(FormField.CALL_LENGTH, durationSeconds)
                             .addPart(FormField.TALKGROUP_NUM, talkgroup)
                             .addPart(FormField.EMERGENCY, 0)
                             .addPart(FormField.API_KEY, getBroadcastConfiguration().getApiKey())
                             // For future use if OpenMHz supports patch information
                             //.addPart(FormField.PATCHES, patches)
                             .addPart(FormField.SOURCE_LIST, "[{ \"pos\": 0.00, \"src\": " + radioId + "}]");

                         HttpRequest fileRequest = HttpRequest.newBuilder()
                             .uri(URI.create(uri))
                             .header(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + bodyBuilder.getBoundary())
                             .header(HttpHeaders.USER_AGENT, "sdrtrunk")
                             .POST(bodyBuilder.build())
                             .build();

                         mHttpClient.sendAsync(fileRequest, HttpResponse.BodyHandlers.ofString())
                             .whenComplete((fileResponse, throwable1) -> {
                                 if(throwable1 != null || fileResponse.statusCode() != 200)
                                 {
                                     if(throwable1 instanceof IOException || throwable1 instanceof CompletionException)
                                     {
                                         //We get socket reset exceptions occasionally when the remote server doesn't
                                         //fully read our request and immediately responds.
                                         setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                                         mLog.error("OpenMHz API file upload fail [" +
                                             fileResponse.statusCode() + "] response [" +
                                             fileResponse.body() + "]");
                                     }
                                     else
                                     {
                                         setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                                         mLog.error("OpenMHz API file upload fail [" +
                                             fileResponse.statusCode() + "] response [" +
                                             fileResponse.body() + "]");
                                     }

                                     incrementErrorAudioCount();
                                     broadcast(new BroadcastEvent(OpenMHzBroadcaster.this,
                                         BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
                                 }
                                 else
                                 {

                                     if(fileResponse.statusCode() == 200)
                                     {
                                         incrementStreamedAudioCount();
                                         broadcast(new BroadcastEvent(OpenMHzBroadcaster.this,
                                             BroadcastEvent.Event.BROADCASTER_STREAMED_COUNT_CHANGE)); 
                                         audioRecording.removePendingReplay(); 
                                     }
                                     else
                                     {
                                         setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                                         mLog.error("OpenMHz API file upload fail [" +
                                             fileResponse.statusCode() + "] response [" +
                                             fileResponse.body() + "]");
                                     }


                                 }

                             });
                     }
                     else
                     {
                         //Register an error for the file not found exception
                         mLog.error("OpenMHz API - upload file not found [" +
                             audioRecording.getPath().toString() + "]");
                         incrementErrorAudioCount();
                         broadcast(new BroadcastEvent(OpenMHzBroadcaster.this,
                             BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
                         audioRecording.removePendingReplay();
                     }
                 }
                 catch(Exception e)
                 {
                     mLog.error("Unknown Error", e);
                     setBroadcastState(BroadcastState.ERROR);
                     incrementErrorAudioCount();
                     broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
                     audioRecording.removePendingReplay();
                 }
             }
         }

         //If we're not connected and there are recordings in the queue, check the recording at the head of the queue
         // and start age-off once the recordings become too old.  The recordings should be time ordered in the queue.
         AudioRecording audioRecording = mAudioRecordingQueue.peek();

         while(audioRecording != null)
         {
             if(isValid(audioRecording))
             {
                 return;
             }
             else
             {
                 //Remove the recording from the queue, remove a replay, and peek at the next recording in the queue
                 mAudioRecordingQueue.poll();
                 audioRecording.removePendingReplay();
                 incrementAgedOffAudioCount();
                 broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_AGED_OFF_COUNT_CHANGE));
                 audioRecording = mAudioRecordingQueue.peek();
             }
         }
     }

     /**
      * Creates a frequency value from the audio recording identifier collection.
      */
     private static Long getFrequency(AudioRecording audioRecording)
     {
         Identifier identifier = audioRecording.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION,
             Form.CHANNEL_FREQUENCY, Role.ANY);

         if(identifier instanceof ConfigurationLongIdentifier)
         {
             Long value = ((ConfigurationLongIdentifier)identifier).getValue();

             if(value != null)
             {
                 return value;
             }
         }

         return Long.valueOf(0);
     }

     /**
      * Creates a formatted string with the FROM identifier or uses a default of zero(0)
      */
     private static String getFrom(AudioRecording audioRecording)
     {
         for(Identifier identifier: audioRecording.getIdentifierCollection().getIdentifiers(Role.FROM))
         {
             if(identifier instanceof RadioIdentifier)
             {
                 return ((RadioIdentifier)identifier).getValue().toString();
             }
         }

         return "0";
     }

     /**
      * Creates a formatted string with the TO identifiers or uses a default of zero (0)
      * 
      */
     private static String getTo(AudioRecording audioRecording)
     {
         Identifier identifier = audioRecording.getIdentifierCollection().getToIdentifier();

         if(identifier instanceof PatchGroupIdentifier patchGroupIdentifier)
         {
             return patchGroupIdentifier.getValue().getPatchGroup().getValue().toString();
         }
         else if(identifier instanceof TalkgroupIdentifier talkgroupIdentifier)
         {
             return String.valueOf(RadioReferenceDecoder.convertToRadioReferenceTalkgroup(talkgroupIdentifier.getValue(),
                     talkgroupIdentifier.getProtocol()));
         }
         else if(identifier instanceof RadioIdentifier radioIdentifier)
         {
             return radioIdentifier.getValue().toString();
         }

         return "0";
     }

     /**
      * Creates a formatted string with the Talkgroup Label from the Audio Recording Alias
      * If this is a PatchGroup we return only the first label as the primary talkgroup label.
      * 
      */
     private String getTalkgroupLabel(AudioRecording audioRecording)
     {

         AliasList aliasList = mAliasModel.getAliasList(audioRecording.getIdentifierCollection());
         Identifier identifier = audioRecording.getIdentifierCollection().getToIdentifier();

         StringBuilder sb = new StringBuilder();
         if(identifier != null)
         {
             List<Alias> aliases = aliasList.getAliases(identifier);
             if(!aliases.isEmpty())
             {
                 sb.append(aliases.get(0));
             }

         }

         return sb.toString();
     }

     /**
      * Creates a formatted string with the Talkgroup Group from the Audio Recording Alias
      * If this is a PatchGroup we return only the first group as the primary talkgroup group.
      * 
      */
     private String getTalkgroupGroup(AudioRecording audioRecording)
     {

         AliasList aliasList = mAliasModel.getAliasList(audioRecording.getIdentifierCollection());
         Identifier identifier = audioRecording.getIdentifierCollection().getToIdentifier();

         StringBuilder sb = new StringBuilder();
         if(identifier != null)
         {
             List<Alias> aliases = aliasList.getAliases(identifier);
             if(!aliases.isEmpty())
             {
                 sb.append(aliases.get(0).getGroup());
             }
         }

         return sb.toString();
     }

     /**
      * Creates a formatted string with the System Label from the Audio Recording Alias
      * If this is a PatchGroup we return only the first sytem as the primary talkgroup system.
      * 
      */
     private String getSystemLabel(AudioRecording audioRecording)
     {
         List<Identifier> systems = audioRecording.getIdentifierCollection().getIdentifiers(Form.SYSTEM);

         StringBuilder sb = new StringBuilder();
         if(!systems.isEmpty())
         {
            sb.append(systems.get(0));
         }

         return sb.toString();
     }

    /**
     * Creates a formatted string with the patched talkgroups
     */
    public static String getPatches(AudioRecording audioRecording)
    {
        Identifier identifier = audioRecording.getIdentifierCollection().getToIdentifier();

        if(identifier instanceof PatchGroupIdentifier patchGroupIdentifier)
        {
            PatchGroup patchGroup = patchGroupIdentifier.getValue();

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            
            sb.append(patchGroup.getPatchGroup().getValue().toString());
            
            for(TalkgroupIdentifier patched: patchGroup.getPatchedTalkgroupIdentifiers())
            {
                sb.append(",").append(patched.getValue());
            }

            for(RadioIdentifier patched: patchGroup.getPatchedRadioIdentifiers())
            {
                sb.append(",").append(patched.getValue());
            }
            
            sb.append("]");
            return sb.toString();
        }

        return "[]";
    }

     /**
      * Tests both the connection and configuration against the OpenMHz Call API service
      * @param configuration containing API key and system id
      * @return error string or null if test is successful
      */
     public static String testConnection(OpenMHzConfiguration configuration)
     {
         String uri = configuration.getHost() + "/" + configuration.getSystemName() + "/authorize";

         HttpClient httpClient = HttpClient.newBuilder()
             .version(HttpClient.Version.HTTP_1_1)
             .followRedirects(HttpClient.Redirect.NORMAL)
             .connectTimeout(Duration.ofSeconds(20))
             .build();

         HttpRequest request = HttpRequest.newBuilder()
             .uri(URI.create(uri))
             .header(HttpHeaders.CONTENT_TYPE, APPLICATION_FORM_URLENCODED)
             .header(HttpHeaders.USER_AGENT, "sdrtrunk")
             .POST(HttpRequest.BodyPublishers.ofString("api_key="+configuration.getApiKey()))
             .build();

         HttpResponse.BodyHandler<String> responseHandler = HttpResponse.BodyHandlers.ofString();

         try
         {
            HttpResponse<String> response = httpClient.send(request, responseHandler);

             if (response.statusCode() == 200)
             {
                return "OK";
             }
             else if(response.statusCode() == 403)
             {
                return "Invalid API Key";
             }
             else if(response.statusCode() == 500)
             {
                return "Invalid System Name";
             }

             return "No Response";
         }
         catch(Exception e)
         {
            Throwable throwableCause = e.getCause();

            if(throwableCause instanceof ConnectException)
            {
                return "No Response";
            }

            mLog.error("Exception connecting to OpenMHz server [" + e.toString() + "]");
            return "Uknown Exception";
         }
     }

     public class AudioRecordingProcessor implements Runnable
     {
         @Override
         public void run()
         {
             processRecordingQueue();
         }
     }

     public static void main(String[] args)
     {
         mLog.debug("Starting ...");

         OpenMHzConfiguration config = new OpenMHzConfiguration();
         config.setHost("https://api.OpenMHz.com/call-upload-dev");
         config.setApiKey("c33aae37-8572-11ea-bd8b-0ecc8ab9ccec");
         config.setSystemName("systemx");

         String response = testConnection(config);
         mLog.error("Response: " + response);

         if(response == "OK")
         {
             mLog.debug("Test Successful!");
         }
         else
         {
             if(response.contains("Invalid API Key"))
             {
                 mLog.error("Invalid API Key");
             }
             else if(response.contains("Invalid System Name"))
             {
                 mLog.error("Invalid System Name");
             }
             else
             {
                 mLog.debug("Response: " + response);
             }
         }

         mLog.debug("Finished!");
     }
 }
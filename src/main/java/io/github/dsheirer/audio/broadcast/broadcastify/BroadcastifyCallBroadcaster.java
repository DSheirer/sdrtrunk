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

package io.github.dsheirer.audio.broadcast.broadcastify;

import com.google.common.net.HttpHeaders;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferenceDecoder;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.ConfigurationLongIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.util.ThreadPool;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio broadcaster to push completed audio recordings to the Broadcastify call push API.
 *
 * Note: this is not the same as the Broadcastify Feeds (ie streaming) service
 */
public class BroadcastifyCallBroadcaster extends AbstractAudioBroadcaster<BroadcastifyCallConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(BroadcastifyCallBroadcaster.class);

    private static final String ENCODING_TYPE_MP3 = "mp3";
    private static final String MULTIPART_TYPE = "multipart";
    private static final String DEFAULT_SUBTYPE = "form-data";
    private static final String MULTIPART_FORM_DATA = MULTIPART_TYPE + "/" + DEFAULT_SUBTYPE;
    private ScheduledFuture<?> mBroadcastifyTestFuture;
    private Queue<AudioRecording> mAudioRecordingQueue = new LinkedTransferQueue<>();
    private ScheduledFuture<?> mAudioRecordingProcessorFuture;
    private HttpClient mHttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build();
    private long mLastConnectionAttempt;
    private long mConnectionAttemptInterval = 5000; //Every 5 seconds
    final private AliasModel mAliasModel;

    /**
     * Constructs an instance of the broadcaster
     * @param config to use
     * @param aliasModel for access to aliases
     */
    public BroadcastifyCallBroadcaster(BroadcastifyCallConfiguration config, InputAudioFormat inputAudioFormat,
                                       MP3Setting mp3Setting, AliasModel aliasModel)
    {
        super(config);
        mAliasModel = aliasModel;
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

        if(response != null && response.toLowerCase().startsWith("ok"))
        {
            setBroadcastState(BroadcastState.CONNECTED);
        }
        else
        {
            mLog.error("Error connecting to Broadcastify calls server on startup [" + response + "]");
            setBroadcastState(BroadcastState.ERROR);
        }

        if(mBroadcastifyTestFuture == null && getBroadcastConfiguration().isTestEnabled())
        {
            // Test periodically so we don't get marked offline due to radio inactivity
            mBroadcastifyTestFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(new BroadcastifyCallTest(), getBroadcastConfiguration().getTestInterval(), getBroadcastConfiguration().getTestInterval(), TimeUnit.MINUTES);
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
        if(mBroadcastifyTestFuture != null)
        {
            mBroadcastifyTestFuture.cancel(true);
            mBroadcastifyTestFuture = null;
        }
        if(mAudioRecordingProcessorFuture != null)
        {
            mAudioRecordingProcessorFuture.cancel(true);
            mAudioRecordingProcessorFuture = null;
            dispose();
            setBroadcastState(BroadcastState.DISCONNECTED);
        }
    }

    public class BroadcastifyCallTest implements Runnable
    {
        @Override
        public void run()
        {
            String response = testConnection(getBroadcastConfiguration());
            if(response != null && response.toLowerCase().startsWith("ok"))
            {
                mLog.info("Broadcastify Calls keep-alive success");
            }
            else
            {
                mLog.info("Broadcastify Calls keep-alive failure");
            }
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

            if(response != null && response.toLowerCase().startsWith("ok"))
            {
                setBroadcastState(BroadcastState.CONNECTED);
            }
            else
            {
                setBroadcastState(BroadcastState.ERROR);
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
     * Processes any enqueued audio recordings.  The broadcastify calls API uses a two-step process that includes
     * requesting an upload URL and then uploading the audio recording to that URL.  This method employs asynchronous
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
                float durationSeconds = (float)(audioRecording.getRecordingLength() / 1E3f);
                long timestampSeconds = (int)(audioRecording.getStartTime() / 1E3);
                String talkgroup = getTo(audioRecording);
                String radioId = getFrom(audioRecording);
                float frequency = getFrequency(audioRecording);

                BroadcastifyCallBuilder bodyBuilder = new BroadcastifyCallBuilder();
                bodyBuilder.addPart(FormField.API_KEY, getBroadcastConfiguration().getApiKey())
                    .addPart(FormField.SYSTEM_ID, getBroadcastConfiguration().getSystemID())
                    .addPart(FormField.CALL_DURATION, durationSeconds)
                    .addPart(FormField.TIMESTAMP, timestampSeconds)
                    .addPart(FormField.TALKGROUP_ID, talkgroup)
                    .addPart(FormField.RADIO_ID, radioId)
                    .addPart(FormField.FREQUENCY, frequency)
                    .addPart(FormField.ENCODING, ENCODING_TYPE_MP3);

                try
                {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getBroadcastConfiguration().getHost()))
                        .header(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + bodyBuilder.getBoundary())
                        .header(HttpHeaders.USER_AGENT, "sdrtrunk")
                        .header(HttpHeaders.ACCEPT, "*/*")
                        .POST(bodyBuilder.build())
                        .build();

                    mHttpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .whenComplete((stringHttpResponse, throwable) -> {
                            if(throwable != null || stringHttpResponse.statusCode() != 200)
                            {
                                if(throwable instanceof IOException || throwable instanceof CompletionException)
                                {
                                    //We get socket reset exceptions occasionally when the remote server doesn't
                                    //fully read our request and immediately responds.
                                }
                                else
                                {
                                    mLog.error("Error while sending upload URL request" + throwable.getLocalizedMessage());
                                    setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                                }
                                incrementErrorAudioCount();
                                broadcast(new BroadcastEvent(BroadcastifyCallBroadcaster.this,
                                    BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
                            }
                            else
                            {
                                String urlResponse = stringHttpResponse.body();

                                if(urlResponse.startsWith("0 "))
                                {
                                    HttpRequest.BodyPublisher filePublisher = null;

                                    try
                                    {
                                        filePublisher = HttpRequest.BodyPublishers.ofFile(audioRecording.getPath());
                                    }
                                    catch(FileNotFoundException fnfe)
                                    {
                                        mLog.error("Broadcastify calls API - audio recording file not found - ignoring upload");
                                    }

                                    if(filePublisher != null)
                                    {
                                        HttpRequest fileRequest = HttpRequest.newBuilder()
                                            .uri(URI.create(urlResponse.substring(2)))
                                            .header(HttpHeaders.USER_AGENT, "sdrtrunk")
                                            .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                                            .PUT(filePublisher)
                                            .build();

                                        mHttpClient.sendAsync(fileRequest, HttpResponse.BodyHandlers.ofString())
                                            .whenComplete((fileResponse, throwable1) -> {
                                                if(throwable1 != null || fileResponse.statusCode() != 200)
                                                {
                                                    if(throwable1 instanceof IOException || throwable1 instanceof CompletionException)
                                                    {
                                                        //We get socket reset exceptions occasionally when the remote server doesn't
                                                        //fully read our request and immediately responds.
                                                    }
                                                    else
                                                    {
                                                        setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                                                        mLog.error("Broadcastify calls API file upload fail [" +
                                                            fileResponse.statusCode() + "] response [" +
                                                            fileResponse.body() + "]");
                                                    }

                                                    incrementErrorAudioCount();
                                                    broadcast(new BroadcastEvent(BroadcastifyCallBroadcaster.this,
                                                        BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
                                                }
                                                else
                                                {
                                                    incrementStreamedAudioCount();
                                                    broadcast(new BroadcastEvent(BroadcastifyCallBroadcaster.this,
                                                        BroadcastEvent.Event.BROADCASTER_STREAMED_COUNT_CHANGE));
                                                }

                                                audioRecording.removePendingReplay();
                                            });
                                    }
                                    else
                                    {
                                        //Register an error for the file not found exception
                                        mLog.error("Broadcastify calls API - upload file not found [" +
                                            audioRecording.getPath().toString() + "]");
                                        incrementErrorAudioCount();
                                        broadcast(new BroadcastEvent(BroadcastifyCallBroadcaster.this,
                                            BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
                                        audioRecording.removePendingReplay();
                                    }
                                }
                                else if(urlResponse.startsWith("1 SKIPPED"))
                                {
                                    //Broadcastify is telling us to skip audio upload - someone already uploaded it
                                    audioRecording.removePendingReplay();
                                }
                                else
                                {
                                    mLog.error("Broadcastify calls API upload URL request failed [" + urlResponse + "]");
                                    setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                                    incrementErrorAudioCount();
                                    broadcast(new BroadcastEvent(BroadcastifyCallBroadcaster.this,
                                        BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
                                    audioRecording.removePendingReplay();
                                }
                            }
                        });
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
    private static float getFrequency(AudioRecording audioRecording)
    {
        Identifier identifier = audioRecording.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION,
            Form.CHANNEL_FREQUENCY, Role.ANY);

        if(identifier instanceof ConfigurationLongIdentifier)
        {
            Long value = ((ConfigurationLongIdentifier)identifier).getValue();

            if(value != null)
            {
                return value / 1E6f;
            }
        }

        return 0.0f;
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
     */
    private String getTo(AudioRecording audioRecording)
    {
        Identifier identifier = audioRecording.getIdentifierCollection().getToIdentifier();

        //Alias the TO value when the user specifies a 'Stream As Talkgroup'
        if(identifier != null)
        {
            AliasListConfigurationIdentifier config = audioRecording.getIdentifierCollection().getAliasListConfiguration();

            if(config != null)
            {
                AliasList aliasList = mAliasModel.getAliasList(config.getValue());

                if(aliasList != null)
                {
                    List<Alias> aliases = aliasList.getAliases(identifier);

                    for(Alias a: aliases)
                    {
                        if(a.getStreamTalkgroupAlias() != null)
                        {
                            return String.valueOf(a.getStreamTalkgroupAlias().getValue());
                        }
                    }
                }
            }
        }

        if(identifier instanceof PatchGroupIdentifier patchGroupIdentifier)
        {
            return format(patchGroupIdentifier);
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
     * Formats a patch group
     */
    public static String format(PatchGroupIdentifier patchGroupIdentifier)
    {
        PatchGroup patchGroup = patchGroupIdentifier.getValue();

        StringBuilder sb = new StringBuilder();
        sb.append(patchGroup.getPatchGroup().getValue().toString());
        for(TalkgroupIdentifier patched: patchGroup.getPatchedTalkgroupIdentifiers())
        {
            sb.append(",").append(patched.getValue());
        }
        for(RadioIdentifier patched: patchGroup.getPatchedRadioIdentifiers())
        {
            sb.append(",").append(patched.getValue());
        }

        return sb.toString();
    }

    /**
     * Tests both the connection and configuration against the Broadcastify Call API service
     * @param configuration containing API key and system id
     * @return error string or null if test is successful
     */
    public static String testConnection(BroadcastifyCallConfiguration configuration)
    {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

        BroadcastifyCallBuilder bodyBuilder = new BroadcastifyCallBuilder();
        bodyBuilder.addPart(FormField.API_KEY, configuration.getApiKey())
            .addPart(FormField.SYSTEM_ID, configuration.getSystemID())
            .addPart(FormField.TEST, 1);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(configuration.getHost()))
            .header(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + bodyBuilder.getBoundary())
            .header(HttpHeaders.USER_AGENT, "sdrtrunk")
            .header(HttpHeaders.ACCEPT, "*/*")
            .POST(bodyBuilder.build())
            .build();

        HttpResponse.BodyHandler<String> responseHandler = HttpResponse.BodyHandlers.ofString();

        try
        {
            HttpResponse<String> response = httpClient.send(request, responseHandler);
            String responseBody = response.body();
            return (responseBody != null ? responseBody : "(no response)") + " Status Code:" + response.statusCode();
        }
        catch(Exception e)
        {
            return e.getLocalizedMessage();
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

        BroadcastifyCallConfiguration config = new BroadcastifyCallConfiguration();
        config.setHost("https://api.broadcastify.com/call-upload-dev");
        config.setApiKey("c33aae37-8572-11ea-bd8b-0ecc8ab9ccec");
        config.setSystemID(11);

        String response = testConnection(config);

        if(response == null)
        {
            mLog.debug("Test Successful!");
        }
        else
        {
            if(response.contains("1 Invalid-API-Key"))
            {
                mLog.error("Invalid API Key");
            }
            else if(response.contains("1 API-Key-Access-Denied"))
            {
                mLog.error("System ID not valid for API Key");
            }
            else
            {
                mLog.debug("Response: " + response);
            }
        }

        mLog.debug("Finished!");
    }
}

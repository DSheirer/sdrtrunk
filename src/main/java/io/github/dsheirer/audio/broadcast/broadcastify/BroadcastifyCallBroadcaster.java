/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    private static final String API_ENDPOINT = BroadcastifyCallConfiguration.DEV_ENDPOINT;
//    private static final String API_ENDPOINT = BroadcastifyCallConfiguration.PRODUCTION_ENDPOINT;
    private Queue<AudioRecording> mAudioRecordingQueue = new LinkedTransferQueue<>();
    private ScheduledFuture<?> mAudioRecordingProcessorFuture;
    private HttpClient mHttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build();
    private long mLastConnectionAttempt;
    private long mConnectionAttemptInterval = 5000; //Every 5 seconds

    /**
     * Constructs an instance of the broadcaster
     * @param config to use
     * @param aliasModel for access to aliases
     */
    public BroadcastifyCallBroadcaster(BroadcastifyCallConfiguration config, AliasModel aliasModel)
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

        if(response != null)
        {
            mLog.error("Error connecting to Broadcastify calls server on startup [" + response + "]");
            setBroadcastState(BroadcastState.ERROR);
        }
        else
        {
            setBroadcastState(BroadcastState.CONNECTED);
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

            if(response == null)
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
                                if(throwable instanceof IOException)
                                {
                                    //We get socket reset exceptions occasionally when the remote server doesn't
                                    //fully read our request and immediately responds.
                                }
                                else
                                {
                                    mLog.error("Error while sending upload URL request", throwable);
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
                                                    if(throwable1 instanceof IOException)
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
    private static String getTo(AudioRecording audioRecording)
    {
        List<Identifier> toIdentifiers = audioRecording.getIdentifierCollection().getIdentifiers(Role.TO);

        if(toIdentifiers.size() >= 1)
        {
            Identifier to = toIdentifiers.get(0);

            if(to instanceof TalkgroupIdentifier)
            {
                TalkgroupIdentifier talkgroupIdentifier = (TalkgroupIdentifier)to;
                return String.valueOf(RadioReferenceDecoder.convertToRadioReferenceTalkgroup(talkgroupIdentifier.getValue(),
                    talkgroupIdentifier.getProtocol()));
            }
            else if(to instanceof PatchGroupIdentifier)
            {
                PatchGroup patchGroup = ((PatchGroupIdentifier)to).getValue();

                StringBuilder sb = new StringBuilder();
                sb.append(patchGroup.getPatchGroup().getValue().toString());
                for(TalkgroupIdentifier patched: patchGroup.getPatchedGroupIdentifiers())
                {
                    sb.append(",").append(patched.getValue());
                }

                return sb.toString();
            }
            else if(to instanceof RadioIdentifier)
            {
                return ((RadioIdentifier)to).getValue().toString();
            }
        }

        return "0";
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
            .addPart(FormField.CALL_DURATION, 0)
            .addPart(FormField.TIMESTAMP, (int)(System.currentTimeMillis() / 1E3))
            .addPart(FormField.TALKGROUP_ID, 999)
            .addPart(FormField.RADIO_ID, 999999)
            .addPart(FormField.FREQUENCY, 450.00000)
            .addPart(FormField.ENCODING, ENCODING_TYPE_MP3);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_ENDPOINT))
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
            if(response.statusCode() == 200 && responseBody != null && responseBody.startsWith("0 "))
            {
                //success!
                return null;
            }
            else
            {
                return responseBody;
            }
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

        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

        int ts = (int)(System.currentTimeMillis() / 1E3);

        BroadcastifyCallBuilder builder = new BroadcastifyCallBuilder();
        builder.addPart(FormField.API_KEY, BroadcastifyCallConfiguration.SDRTRUNK_DEV_API_KEY)
                .addPart(FormField.SYSTEM_ID, 11)
                .addPart(FormField.CALL_DURATION, 3.0)
                .addPart(FormField.TIMESTAMP, 1234568)
                .addPart(FormField.TALKGROUP_ID, 1234)
                .addPart(FormField.RADIO_ID, 123456)
                .addPart(FormField.FREQUENCY, 450.12345)
                .addPart(FormField.ENCODING, "mp3");

        HttpRequest.BodyPublisher body = builder.build();

        if(body != null)
        {
            try
            {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BroadcastifyCallConfiguration.DEV_ENDPOINT))
                    .header(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + builder.getBoundary())
                    .header(HttpHeaders.USER_AGENT, "sdrtrunk")
                    .header(HttpHeaders.ACCEPT, "*/*")
                    .POST(body).build();

                HttpResponse<String> response = null;

                mLog.info("Submitting request for URL");
                try
                {
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                }
                catch(Exception ioe)
                {
                    mLog.error("Error", ioe);
                }

                if(response != null)
                {
                    String url = response.body().toString();

                    mLog.info("URL Requested - Response: " + url);
                    if(response.statusCode() == 200)
                    {
                        if(url != null && url.startsWith("0 "))
                        {
                            String file = "/home/denny/SDRTrunk/recordings/20200426_160130CNYICC_Onondaga_TRAFFIC__TO_1_FROM_911005.mp3";
                            Path path = Path.of(file);

                            HttpRequest fileRequest = HttpRequest.newBuilder()
                                .uri(URI.create(url.substring(2)))
                                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                                .POST(HttpRequest.BodyPublishers.ofFile(path))
                                .build();

                            mLog.info("Submitting file upload");
                            httpClient.sendAsync(fileRequest, HttpResponse.BodyHandlers.ofString())
                                .whenComplete((stringHttpResponse, throwable) -> {
                                    mLog.debug("Upload complete");
                                    if(throwable != null)
                                    {
                                        mLog.info("Upload Complete:" + stringHttpResponse.body());
                                    }
                                    else
                                    {
                                        throwable.printStackTrace();
                                    }
                                });
                        }
                        else
                        {
                            mLog.error("Got HTTP 200 OK, but invalid response: " + url);
                        }
                    }
                    else
                    {
                        mLog.info("Error response:" + url);
                    }
                }
            }
            catch(Exception e)
            {
                mLog.debug("Error", e);
            }
        }

        while(true);

//        mLog.debug("Finished!");
    }
}

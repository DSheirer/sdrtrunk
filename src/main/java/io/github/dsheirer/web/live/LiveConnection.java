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

package io.github.dsheirer.web.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.auth.Principal;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One client WebSocket connection: its caller, its subscriptions and its outbound queue.
 *
 * Messages leave one at a time: the next one is sent when the previous send completes, so a slow client builds a
 * queue here rather than in the network layer.  Messages that carry the latest state of one thing (a channel
 * timeslot, a tuner, an active call) replace a waiting message for the same thing, so a slow client receives the
 * newest state rather than a backlog.  When the queue still overflows, the connection closes with code 4008 and the
 * client reloads over REST.
 */
class LiveConnection
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveConnection.class);
    static final int MAX_QUEUED = 5000;
    static final int MAX_SUBSCRIPTIONS = 32;
    static final int CLOSE_TOO_SLOW = 4008;
    static final int CLOSE_UNAUTHORIZED = 4001;
    private static final AtomicLong UNIQUE = new AtomicLong();

    private final String mId;
    private final MessageSink mSink;
    private final ObjectMapper mMapper;
    private final Map<Integer,Subscription> mSubscriptions = new ConcurrentHashMap<>();
    private final LinkedHashMap<Object,Outbound> mQueue = new LinkedHashMap<>();
    private volatile Principal mPrincipal;
    private boolean mSending;
    private boolean mInSend;
    private boolean mCompletedInSend;
    private volatile boolean mClosed;

    LiveConnection(String id, Principal principal, MessageSink sink, ObjectMapper mapper)
    {
        mId = id;
        mPrincipal = principal;
        mSink = sink;
        mMapper = mapper;
    }

    String getId()
    {
        return mId;
    }

    Principal getPrincipal()
    {
        return mPrincipal;
    }

    void setPrincipal(Principal principal)
    {
        mPrincipal = principal;
    }

    Map<Integer,Subscription> getSubscriptions()
    {
        return mSubscriptions;
    }

    boolean isClosed()
    {
        return mClosed;
    }

    /**
     * Queues a data message for a subscription.
     * @param subscription that the message belongs to
     * @param coalesceKey identifies the thing whose latest state this message carries, so a newer message replaces a
     * waiting one, or null when every message must be delivered
     * @param data JSON text of the message data
     */
    void sendData(Subscription subscription, Object coalesceKey, String data)
    {
        Object key = coalesceKey != null ? List.of(subscription.getId(), coalesceKey) : UNIQUE.incrementAndGet();
        enqueue(key, new Outbound(subscription, data));
    }

    /**
     * Queues a binary message (spectrum or audio frame) for a subscription.
     * @param coalesceKey identifies a frame that a newer one replaces while waiting (spectrum), or null to deliver
     * every frame (audio)
     */
    void sendBinary(Subscription subscription, Object coalesceKey, byte[] data)
    {
        Object key = coalesceKey != null ? List.of(subscription.getId(), coalesceKey) : UNIQUE.incrementAndGet();
        enqueue(key, new Outbound(data));
    }

    /**
     * Count of messages waiting to be sent
     */
    int getQueueSize()
    {
        synchronized(mQueue)
        {
            return mQueue.size();
        }
    }

    /**
     * Queues a control message: subscribed, unsubscribed, error, pong.
     */
    void sendControl(Map<String,Object> message)
    {
        try
        {
            enqueue(UNIQUE.incrementAndGet(), new Outbound(null, mMapper.writeValueAsString(message)));
        }
        catch(Exception e)
        {
            LOGGER.error("Unable to serialize a WebSocket control message", e);
        }
    }

    /**
     * Sends an error for a subscription (or 0 for the connection).
     */
    void sendError(int sub, ApiException error)
    {
        Map<String,Object> message = new LinkedHashMap<>();
        message.put("op", "error");
        message.put("sub", sub);
        message.put("status", error.getStatus());
        message.put("title", error.getTitle());

        if(error.getDetail() != null)
        {
            message.put("detail", error.getDetail());
        }

        if(!error.getErrors().isEmpty())
        {
            message.put("errors", error.getErrors());
        }

        sendControl(message);
    }

    /**
     * Closes the connection.
     */
    void close(int code, String reason)
    {
        if(!mClosed)
        {
            mClosed = true;

            synchronized(mQueue)
            {
                mQueue.clear();
            }

            try
            {
                mSink.close(code, reason);
            }
            catch(Exception e)
            {
                LOGGER.debug("Error closing WebSocket " + mId, e);
            }
        }
    }

    /**
     * Marks the connection closed after the client closed it.
     */
    void closed()
    {
        mClosed = true;

        synchronized(mQueue)
        {
            mQueue.clear();
        }
    }

    /**
     * Parses a client message as JSON.
     */
    JsonNode parse(String text)
    {
        try
        {
            return mMapper.readTree(text);
        }
        catch(Exception e)
        {
            throw ApiException.badRequest("The message isn't valid JSON");
        }
    }

    private void enqueue(Object key, Outbound message)
    {
        if(mClosed)
        {
            return;
        }

        boolean overflow;

        synchronized(mQueue)
        {
            //Replacing keeps the waiting message's place in the queue, so states stay in order across things
            mQueue.put(key, message);
            overflow = mQueue.size() > MAX_QUEUED;
        }

        if(overflow)
        {
            LOGGER.warn("Web API WebSocket client [" + mPrincipal.username() + "] isn't keeping up - closing");
            close(CLOSE_TOO_SLOW, "Client too slow; reload over REST and subscribe again");
            return;
        }

        sendNext();
    }

    /**
     * Sends queued messages one at a time.  A send that completes at once (the socket was writable) continues this
     * loop instead of recursing, so a long queue doesn't grow the stack.
     */
    private void sendNext()
    {
        while(true)
        {
            Outbound next;
            String text;

            synchronized(mQueue)
            {
                if(mSending || mClosed || mQueue.isEmpty())
                {
                    return;
                }

                Iterator<Outbound> it = mQueue.values().iterator();
                next = it.next();
                it.remove();
                text = next.mBinary == null ? next.render() : null;
                mSending = true;
                mInSend = true;
                mCompletedInSend = false;
            }

            try
            {
                java.util.function.Consumer<Throwable> failed = error -> {
                    LOGGER.debug("WebSocket send failed for " + mId + " - " + error.getMessage());
                    closed();
                };

                if(next.mBinary != null)
                {
                    mSink.sendBinary(next.mBinary, this::sent, failed);
                }
                else
                {
                    mSink.send(text, this::sent, failed);
                }
            }
            catch(Exception e)
            {
                LOGGER.debug("WebSocket send failed for " + mId + " - " + e.getMessage());
                closed();
                return;
            }

            synchronized(mQueue)
            {
                mInSend = false;

                if(!mCompletedInSend)
                {
                    //Completes later; the callback continues with the next message
                    return;
                }
            }
        }
    }

    private void sent()
    {
        synchronized(mQueue)
        {
            mSending = false;

            if(mInSend)
            {
                mCompletedInSend = true;
                return;
            }
        }

        sendNext();
    }

    /**
     * Queued message.  The envelope and the sequence number are added when it is sent, so a replaced message doesn't
     * leave a gap in the sequence.
     */
    private static final class Outbound
    {
        private final Subscription mSubscription;
        private final String mData;
        private final byte[] mBinary;

        Outbound(Subscription subscription, String data)
        {
            mSubscription = subscription;
            mData = data;
            mBinary = null;
        }

        Outbound(byte[] binary)
        {
            mSubscription = null;
            mData = null;
            mBinary = binary;
        }

        String render()
        {
            if(mSubscription == null)
            {
                return mData;
            }

            return "{\"op\":\"data\",\"sub\":" + mSubscription.getId() + ",\"topic\":\"" +
                mSubscription.getTopic().getName() + "\",\"seq\":" + mSubscription.nextSequence() + ",\"data\":" +
                mData + "}";
        }
    }
}

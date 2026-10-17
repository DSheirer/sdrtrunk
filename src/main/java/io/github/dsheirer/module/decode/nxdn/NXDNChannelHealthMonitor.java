/*
 * *****************************************************************************
 * Copyright (C) 2026 Dennis Sheirer
 * ****************************************************************************
 */
package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Audio;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.heartbeat.Heartbeat;
import io.github.dsheirer.source.heartbeat.IHeartbeatListener;
import io.github.dsheirer.util.ThreadPool;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches NXDN source heartbeats independently of decoded call activity. An idle channel continues to receive source
 * heartbeats, so the monitor only requests recovery when the processing path itself has stopped moving.
 */
public class NXDNChannelHealthMonitor extends Module implements IHeartbeatListener, IMessageListener, Listener<IMessage>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NXDNChannelHealthMonitor.class);
    static final long HEARTBEAT_TIMEOUT_MILLISECONDS = 30_000L;
    private static final long CHECK_INTERVAL_MILLISECONDS = 5_000L;
    private final Channel mChannel;
    private final AtomicLong mLastHeartbeat = new AtomicLong();
    private final AtomicLong mLastValidMessage = new AtomicLong();
    private final AtomicLong mLastAudio = new AtomicLong();
    private final AtomicBoolean mRestartRequested = new AtomicBoolean();
    private final Listener<Heartbeat> mHeartbeatListener = heartbeat -> mLastHeartbeat.set(System.currentTimeMillis());
    private ScheduledFuture<?> mMonitorTask;

    public NXDNChannelHealthMonitor(Channel channel)
    {
        mChannel = channel;
    }

    @Override
    public Listener<Heartbeat> getHeartbeatListener()
    {
        return mHeartbeatListener;
    }

    @Override
    public Listener<IMessage> getMessageListener()
    {
        return this;
    }

    @Override
    public void receive(IMessage message)
    {
        if(message instanceof NXDNMessage nxdn && nxdn.isValid())
        {
            mLastValidMessage.set(nxdn.getTimestamp());

            if(nxdn instanceof Audio)
            {
                mLastAudio.set(nxdn.getTimestamp());
            }
        }
    }

    @Override
    public void reset()
    {
        long now = System.currentTimeMillis();
        mLastHeartbeat.set(now);
        mLastValidMessage.set(0);
        mLastAudio.set(0);
        mRestartRequested.set(false);
    }

    @Override
    public void start()
    {
        reset();
        mMonitorTask = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::checkHealth,
            HEARTBEAT_TIMEOUT_MILLISECONDS, CHECK_INTERVAL_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop()
    {
        if(mMonitorTask != null)
        {
            mMonitorTask.cancel(false);
            mMonitorTask = null;
        }
    }

    private void checkHealth()
    {
        long now = System.currentTimeMillis();
        long heartbeatAge = now - mLastHeartbeat.get();

        if(mChannel != null && mChannel.isProcessing() && heartbeatAge > HEARTBEAT_TIMEOUT_MILLISECONDS &&
            mRestartRequested.compareAndSet(false, true))
        {
            String diagnostics = "lastHeartbeatAgeMs=" + heartbeatAge + ", lastValidMessage=" +
                mLastValidMessage.get() + ", lastAudio=" + mLastAudio.get();
            LOGGER.error("NXDN channel [{}] processing heartbeat stopped; requesting controlled restart ({})",
                mChannel.getName(), diagnostics);

            if(hasInterModuleEventBus())
            {
                getInterModuleEventBus().post(new NXDNChannelRestartRequest(mChannel,
                    "NXDN source heartbeat timed out", diagnostics));
            }
        }
    }
}

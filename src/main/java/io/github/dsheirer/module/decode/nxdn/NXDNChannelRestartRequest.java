/*
 * *****************************************************************************
 * Copyright (C) 2026 Dennis Sheirer
 * ****************************************************************************
 */
package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.ModuleEventBusMessage;

/**
 * Requests a controlled restart after an NXDN processing chain stops receiving source heartbeats.
 */
public class NXDNChannelRestartRequest extends ModuleEventBusMessage
{
    private final Channel mChannel;
    private final String mReason;
    private final String mDiagnostics;

    public NXDNChannelRestartRequest(Channel channel, String reason, String diagnostics)
    {
        mChannel = channel;
        mReason = reason;
        mDiagnostics = diagnostics;
    }

    public Channel getChannel()
    {
        return mChannel;
    }

    public String getReason()
    {
        return mReason;
    }

    public String getDiagnostics()
    {
        return mDiagnostics;
    }
}

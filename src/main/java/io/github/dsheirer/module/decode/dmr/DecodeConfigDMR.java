package io.github.dsheirer.module.decode.dmr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.DMRDecoder;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

public class DecodeConfigDMR extends DecodeConfiguration {
    private DMRDecoder.Modulation mModulation = DMRDecoder.Modulation.C4FM;

    private int mCallTimeout = 1;
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT;
    private boolean mIgnoreDataCalls = true;

    public DecodeConfigDMR()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.DMR;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "modulation")
    public DMRDecoder.Modulation getModulation()
    {
        return mModulation;
    }

    public void setModulation(DMRDecoder.Modulation modulation)
    {
        mModulation = modulation;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "ignore_data_calls")
    public boolean getIgnoreDataCalls()
    {
        return mIgnoreDataCalls;
    }

    public void setIgnoreDataCalls(boolean ignore)
    {
        mIgnoreDataCalls = ignore;
    }

    /**
     * Note: this field is now deprecated.
     *
     * @return
     */
    @JsonIgnore
    @Deprecated
    public int getCallTimeout()
    {
        return mCallTimeout;
    }

    /**
     * Sets the call timeout value in seconds ( 10 - 600 );
     *
     * @param timeout
     */
    @Deprecated
    public void setCallTimeout(int timeout)
    {
    }


    @JacksonXmlProperty(isAttribute = true, localName = "traffic_channel_pool_size")
    public int getTrafficChannelPoolSize()
    {
        return mTrafficChannelPoolSize;
    }

    /**
     * Sets the traffic channel pool size which is the maximum number of
     * simultaneous traffic channels that can be allocated.
     *
     * This limits the maximum calls so that busy systems won't cause more
     * traffic channels to be allocated than the decoder/software/host computer
     * can support.
     */
    public void setTrafficChannelPoolSize(int size)
    {
        mTrafficChannelPoolSize = size;
    }

    /**
     * Source channel specification for this decoder
     */
    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        return new ChannelSpecification(50000.0, 12500, 5750.0, 6500.0);
    }
}

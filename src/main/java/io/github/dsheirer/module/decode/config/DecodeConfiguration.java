/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.module.decode.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.controller.config.Configuration;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.am.DecodeConfigAM;
import io.github.dsheirer.module.decode.ltrnet.DecodeConfigLTRNet;
import io.github.dsheirer.module.decode.ltrstandard.DecodeConfigLTRStandard;
import io.github.dsheirer.module.decode.mpt1327.DecodeConfigMPT1327;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.p25.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.passport.DecodeConfigPassport;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DecodeConfigAM.class, name = "decodeConfigAM"),
    @JsonSubTypes.Type(value = DecodeConfigLTRNet.class, name = "decodeConfigLTRNet"),
    @JsonSubTypes.Type(value = DecodeConfigLTRStandard.class, name = "decodeConfigLTRStandard"),
    @JsonSubTypes.Type(value = DecodeConfigMPT1327.class, name = "decodeConfigMPT1327"),
    @JsonSubTypes.Type(value = DecodeConfigNBFM.class, name = "decodeConfigNBFM"),
    @JsonSubTypes.Type(value = DecodeConfigP25Phase1.class, name = "decodeConfigP25Phase1"),
    @JsonSubTypes.Type(value = DecodeConfigPassport.class, name = "decodeConfigPassport")
})
@JacksonXmlRootElement(localName = "decode_configuration")
public abstract class DecodeConfiguration extends Configuration
{
    public static final int DEFAULT_CALL_TIMEOUT_SECONDS = 45;
    public static final int CALL_TIMEOUT_MINIMUM = 1;
    public static final int CALL_TIMEOUT_MAXIMUM = 300; //5 minutes

    public static final int TRAFFIC_CHANNEL_LIMIT_DEFAULT = 3;
    public static final int TRAFFIC_CHANNEL_LIMIT_MINIMUM = 0;
    public static final int TRAFFIC_CHANNEL_LIMIT_MAXIMUM = 30;

    public static final int DEFAULT_AFC_MAX_CORRECTION = 1000;

    private boolean mAFCEnabled = true;
    private int mAFCMaxCorrection = DEFAULT_AFC_MAX_CORRECTION;

    public DecodeConfiguration()
    {
    }

    @JsonIgnore
    public abstract DecoderType getDecoderType();

    @JacksonXmlProperty(isAttribute = true, localName = "afc")
    public boolean getAFC()
    {
        return mAFCEnabled;
    }

    @JsonIgnore
    public boolean isAFCEnabled()
    {
        return mAFCEnabled;
    }

    public void setAFC(boolean enabled)
    {
        mAFCEnabled = enabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "AFCMaximumCorrection")
    public int getAFCMaximumCorrection()
    {
        return mAFCMaxCorrection;
    }

    public void setAFCMaximumCorrection(int max)
    {
        mAFCMaxCorrection = max;
    }

    public boolean supportsAFC()
    {
        return true;
    }
}

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package io.github.dsheirer.source.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.SourceType;
import io.github.dsheirer.source.tuner.TunerChannel;
import io.github.dsheirer.source.tuner.TunerChannel.Type;

import java.text.DecimalFormat;

@JsonSubTypes.Type(value = SourceConfigTuner.class, name = "sourceConfigTuner")
public class SourceConfigTuner extends SourceConfiguration
{
    private static DecimalFormat sFORMAT = new DecimalFormat("0.00000");

    private long mFrequency = 0;
    private int mBandwidth = 12500;

    public SourceConfigTuner()
    {
        super(SourceType.TUNER);
    }

    public SourceConfigTuner(TunerChannel tunerChannel)
    {
        this();

        mFrequency = tunerChannel.getFrequency();
        mBandwidth = tunerChannel.getBandwidth();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "frequency")
    public long getFrequency()
    {
        return mFrequency;
    }

    public void setFrequency(long frequency)
    {
        mFrequency = frequency;
    }

    @JsonIgnore
    @Override
    public String getDescription()
    {
        return sFORMAT.format((double)mFrequency / 1000000.0d) + " MHz";
    }

    @JsonIgnore
    public TunerChannel getTunerChannel()
    {
        return new TunerChannel(Type.LOCKED, mFrequency, mBandwidth);
    }
}

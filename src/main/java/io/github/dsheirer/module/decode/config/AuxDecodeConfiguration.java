/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.controller.config.Configuration;
import io.github.dsheirer.module.decode.DecoderType;

import java.util.ArrayList;
import java.util.List;

@JsonSubTypes.Type(value = AuxDecodeConfiguration.class, name = "auxDecodeConfiguration")
public class AuxDecodeConfiguration extends Configuration
{
    private List<DecoderType> mAuxDecoders = new ArrayList<DecoderType>();

    public AuxDecodeConfiguration()
    {
    }

    @JacksonXmlProperty(isAttribute = false, localName = "aux_decoder")
    public List<DecoderType> getAuxDecoders()
    {
        return mAuxDecoders;
    }

    public void setAuxDecoders(List<DecoderType> decoders)
    {
        mAuxDecoders = decoders;
    }

    public void addAuxDecoder(DecoderType decoder)
    {
        mAuxDecoders.add(decoder);
    }

    public void removeAuxDecoder(DecoderType decoder)
    {
        mAuxDecoders.remove(decoder);
    }

    public void clearAuxDecoders()
    {
        mAuxDecoders.clear();
    }
}

/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for converting MBE call sequences (*.mbe) to PCM wave audio format
 */
public class MBECallSequenceConverter
{
    private final static Logger mLog = LoggerFactory.getLogger(MBECallSequenceConverter.class);

    public static Path convert(Path input, Path output) throws IOException
    {
        InputStream inputStream = Files.newInputStream(input);
        ObjectMapper mapper = new ObjectMapper();
        MBECallSequence sequence = mapper.readValue(inputStream, MBECallSequence.class);
        return convert(sequence);
    }

    public static Path convert(MBECallSequence callSequence)
    {
        return null;
    }
}

/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package record.mp3;

import audio.broadcast.BroadcastModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

public class MP3FileInspector
{
    private final static Logger mLog = LoggerFactory.getLogger(MP3FileInspector.class);

    private Path mPath;

    public MP3FileInspector(Path path)
    {
        mPath = path;
    }

    public void inspect()
    {
        try
        {
            byte[] audio = Files.readAllBytes(mPath);

            mLog.debug("File:" + mPath.toAbsolutePath().toString() + " Length:" + audio.length + " Residual:" + audio.length % 144);

//            mLog.debug(format(Arrays.copyOfRange(audio, 0, 20)));
//            for(int x = 0; x < audio.length - 3; x++)
//            {
//                if(audio[x] == (byte)0xFF && ((audio[x + 1] & 0xE0) == 0xE0))
//                {
//                    mLog.debug("\tFrame: " + x + "\t" + format(Arrays.copyOfRange(audio, x, x + 4)));
//
//                    x += 143;
//                }
//            }

        }
        catch(IOException ioe)
        {
            mLog.error("Error reading temporary audio stream recording [" + mPath.toString() +
                "] - skipping recording");
        }
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        Path path = SystemProperties.getInstance().getApplicationFolder(BroadcastModel.TEMPORARY_STREAM_DIRECTORY);

        if(path != null && Files.isDirectory(path))
        {
            StringBuilder sb = new StringBuilder();
            sb.append(BroadcastModel.TEMPORARY_STREAM_FILE_SUFFIX);
            sb.append("*.*");

            try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path, sb.toString()))
            {
                directoryStream.forEach(new Consumer<Path>()
                {
                    @Override
                    public void accept(Path path)
                    {
                        MP3FileInspector inspector = new MP3FileInspector(path);
                        inspector.inspect();
                    }
                });
            }
            catch(IOException ioe)
            {
                mLog.error("Error discovering orphaned temporary stream recording files", ioe);
            }
        }



        mLog.debug("Finished");
    }

    private String format(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();

        for(byte b: bytes)
        {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }
}

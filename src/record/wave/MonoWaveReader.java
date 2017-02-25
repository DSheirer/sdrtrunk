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
package record.wave;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.ConversionUtils;
import sample.Listener;
import sample.real.RealBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.regex.Pattern;

public class MonoWaveReader implements AutoCloseable
{
	private final static Logger mLog = LoggerFactory.getLogger( MonoWaveReader.class);

	private static final Pattern FILENAME_PATTERN = Pattern.compile( "(.*_)(\\d+)(\\.wav)" );

	private Path mPath;
    private boolean mRealTime;
	private InputStream mInputStream;
    private int mDataByteSize = 0;
    private Listener<RealBuffer> mListener;
    private byte[] mDataBuffer = new byte[8000];

    /**
     * Wave file reader for PCM 8kHz, 16-bit little-endian format.
     *
     * @param path of the wave file
     * @param realTime to force replay to real-time.  Note: you should run this reader on a separate thread if you
     * choose real time since this reader will sleep the calling thread as needed to effect real-time playback.
     */
	public MonoWaveReader(Path path, boolean realTime) throws IOException
	{
        Validate.isTrue(path != null);
		mPath = path;
        mRealTime = realTime;

		open();
	}

	/**
	 * Opens the file
	 */
	private void open() throws IOException
	{
		if(!Files.exists(mPath))
		{
			throw new IOException("File not found");
		}

		mInputStream = Files.newInputStream(mPath, StandardOpenOption.READ);

        //Check for RIFF header
        byte[] buffer = new byte[4];
        mInputStream.read(buffer);

        if(!Arrays.equals(buffer, WaveUtils.RIFF_CHUNK))
        {
            throw new IOException("File is not .wav format - missing RIFF chunk");
        }

        //Get file size
        mInputStream.read(buffer);
        int fileSize = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get();

        //Check for WAVE format
        mInputStream.read(buffer);

        if(!Arrays.equals(buffer, WaveUtils.WAV_FORMAT))
        {
            throw new IOException("File is not .wav format - missing WAVE format");
        }

        //Check for format chunk
        mInputStream.read(buffer);

        if(!Arrays.equals(buffer, WaveUtils.CHUNK_FORMAT))
        {
            throw new IOException("File is not .wav format - missing format chunk");
        }

        //Get chunk size
        mInputStream.read(buffer);
        int chunkSize = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get();

        //Get format
        mInputStream.read(buffer);

        ShortBuffer shortBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short format = shortBuffer.get();
        if(format != WaveUtils.PCM_FORMAT)
        {
            throw new IOException("File format not supported - expecting PCM format");
        }

        //Get number of channels
        short channels = shortBuffer.get();
        if(channels != 1)
        {
            throw new IOException("Unsupported channel count - mono audio only");
        }

        //Get samples per second
        mInputStream.read(buffer);
        int sampleRate = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get();

        //Get bytes per second
        mInputStream.read(buffer);
        int bytesPerSecond = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get();

        mInputStream.read(buffer);

        //Get frame size
        shortBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short frameSize = shortBuffer.get();
        if(frameSize != 2)
        {
            throw new IOException("PCM frame size not supported - expecting 2 bytes per frame");
        }

        //Get bits per sample
        short bitsPerSample = shortBuffer.get();
        if(bitsPerSample != 16)
        {
            throw new IOException("PCM sample size not supported - expecting 16 bits per sample");
        }

        mInputStream.read(buffer);

        if(!Arrays.equals(buffer, WaveUtils.CHUNK_DATA))
        {
            throw new IOException("Unexpected chunk - expecting data chunk");
        }

        //Get data chunk size
        mInputStream.read(buffer);
        mDataByteSize = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get();
    }

    public void read() throws IOException
    {
        long start = System.currentTimeMillis();
        long samplesRead = 0;

        int totalBytesRead = 0;

        while(totalBytesRead < mDataByteSize)
        {
            int bytesRead = mInputStream.read(mDataBuffer);

            totalBytesRead += bytesRead;

            float[] samples = ConversionUtils.convertFromSigned16BitSamples(mDataBuffer);

            if(bytesRead != mDataBuffer.length)
            {
                samples = Arrays.copyOf(samples, bytesRead / 2);
            }

            if(mRealTime)
            {
                samplesRead += samples.length;

                long actualElapsed = System.currentTimeMillis() - start;

                long audioElapsed = (long)(((double)samplesRead / 8000.0) * 1000.0);

                if(audioElapsed > actualElapsed)
                {
                    try
                    {
                        Thread.sleep(audioElapsed - actualElapsed);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }

            if(mListener != null)
            {
                mListener.receive(new RealBuffer(samples));
            }
        }

        close();
    }

    public void setListener(Listener<RealBuffer> listener)
    {
        mListener = listener;
    }

	/**
	 * Closes the file
	 */
	public void close() throws IOException
	{
	    if(mInputStream != null)
        {
            mInputStream.close();
        }
	}

	public static void main(String[] args)
    {
        Path path = Paths.get("/home/denny/Music/PCM.wav");
        mLog.debug("Opening: " + path.toString());

        try
        {
            MonoWaveReader reader = new MonoWaveReader(path, true);
            reader.setListener(new Listener<RealBuffer>()
            {
                @Override
                public void receive(RealBuffer realBuffer)
                {
                    mLog.debug("Received buffer");
                }
            });
            reader.read();
        }
        catch(IOException e)
        {
            mLog.error("Error", e);
        }

        mLog.debug("Finished");
    }
}

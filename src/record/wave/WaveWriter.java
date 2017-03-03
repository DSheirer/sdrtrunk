/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioFormat;

public class WaveWriter implements AutoCloseable 
{
	private static final Pattern FILENAME_PATTERN = Pattern.compile( "(.*_)(\\d+)(\\.wav)" );
	public static final long MAX_WAVE_SIZE = 2l * (long)Integer.MAX_VALUE;
	
	private AudioFormat mAudioFormat;
	private int mFileRolloverCounter = 1;
	private long mMaxSize;
	private Path mFile;
	private FileChannel mFileChannel;
	
	/**
	 * Constructs a new wave writer that is open with a complete header, ready
	 * for writing buffers of PCM sample data.
	 * 
	 * Each time the maximum file size is reached, a new file is created with a 
	 * series suffix appended to the file name.
	 * 
	 * @param format - audio format (channels, sample size, sample rate)
	 * @param file - wave file to write
	 * @param maxSize - maximum file size ( range: 1 - 4,294,967,294 bytes )
	 * @throws IOException - if there are any IO issues
	 */
	public WaveWriter( AudioFormat format, Path file, long maxSize ) throws IOException
	{
		Validate.isTrue(format != null);
		Validate.isTrue( file != null );
		
		mAudioFormat = format;
		mFile = file;
		
		if( 0 < maxSize && maxSize <= MAX_WAVE_SIZE )
		{
			mMaxSize = maxSize;
		}
		else
		{
			mMaxSize = MAX_WAVE_SIZE;
		}
		
		open();
	}
	
	/**
	 * Constructs a new wave writer that is open with a complete header, ready
	 * for writing buffers of PCM sample data.  The maximum file size is limited
	 * to the max size specified in the wave file format: max unsigned integer
	 * 
	 * @param format - audio format (channels, sample size, sample rate)
	 * @param file - wave file to write
	 * @throws IOException - if there are any IO issues
	 */
	public WaveWriter( AudioFormat format, Path file ) throws IOException
	{
		this( format, file, Integer.MAX_VALUE * 2 );
	}
	
	/**
	 * Opens the file and writes a wave header.
	 */
	private void open() throws IOException
	{
		int version = 2;
		
		while( Files.exists( mFile ) )
		{
			mFile = Paths.get( mFile.toFile().getAbsolutePath().replace( ".wav", "_" + version + ".wav" ) );
			version++;
		}
		
		mFileChannel = (FileChannel.open( mFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW ) );

		ByteBuffer header = WaveUtils.getWaveHeader( mAudioFormat );
		
		header.flip();

		while( header.hasRemaining() )
		{
			mFileChannel.write( header );
		}
	}

	/**
	 * Closes the file
	 */
	public void close() throws IOException
	{
		mFileChannel.force( true );
		mFileChannel.close();
		mFileChannel = null;
	}

    @Override
    protected void finalize() throws IOException
    {
        mFileChannel.force( true );
        mFileChannel.close();
        mFileChannel = null;
    }

    /**
	 * Writes the buffer contents to the file.  Assumes that the buffer is full 
	 * and the first byte of data is at position 0.
	 */
	public void write( ByteBuffer buffer ) throws IOException
	{
		buffer.position( 0 );

		/* Write the full buffer if there is room, respecting the max file size */
		if( mFileChannel.size() + buffer.capacity() < mMaxSize )
		{
			while( buffer.hasRemaining() )
			{
				mFileChannel.write( buffer );
			}
			
			updateWaveFileSize();
		}
		else
		{
			/* Split the buffer to finish filling the current file and then put
			 * the leftover into a new file */
			int remaining = (int)( mMaxSize - mFileChannel.size() );

			/* Ensure we write full frames to fill up the remaining size */
			remaining -= (int)( remaining % mAudioFormat.getFrameSize() );
			
			byte[] bytes = buffer.array();
			
			ByteBuffer current = ByteBuffer.wrap( Arrays.copyOf( bytes, remaining ) );

			ByteBuffer next = ByteBuffer.wrap( Arrays.copyOfRange( bytes, 
					remaining, bytes.length ) );

			while( current.hasRemaining() )
			{
				mFileChannel.write( current );
			}
			
			updateWaveFileSize();
			
			rollover();

			while( next.hasRemaining() )
			{
				mFileChannel.write( next );
			}
			
			updateWaveFileSize();
		}
	}

	/**
	 * Closes out the current file, appends an incremented sequence number to 
	 * the file name and opens up a new file.
	 */
	private void rollover() throws IOException
	{
		close();
		
		mFileRolloverCounter++;
		
		updateFileName();
		
		open();
	}
	
	/**
	 * Updates the overall and the chunk2 sizes
	 */
	private void updateWaveFileSize() throws IOException
	{
		/* Update overall wave size (total size - 8 bytes) */
		ByteBuffer buffer = getUnsignedIntegerBuffer( mFileChannel.size() - 8 );

		mFileChannel.write( buffer, 4 );

		ByteBuffer buffer2 = getUnsignedIntegerBuffer( mFileChannel.size() - 44 );
		
		mFileChannel.write( buffer2, 40 );
	}

	/**
	 * Creates a little-endian 4-byte buffer containing an unsigned 32-bit 
	 * integer value derived from the 4 least significant bytes of the argument.
	 * 
	 * The buffer's position is set to 0 to prepare it for writing to a channel.
	 */
	protected static ByteBuffer getUnsignedIntegerBuffer( long size )
	{
		ByteBuffer buffer = ByteBuffer.allocate( 4 );
		
		buffer.put( (byte)( size & 0xFFl ) );
		buffer.put( (byte)( Long.rotateRight( size & 0xFF00l, 8 ) ) );
		buffer.put( (byte)( Long.rotateRight( size & 0xFF0000l, 16 ) ) );
		
		/* This side-steps an issue with right shifting a signed long by 32 
		 * where it produces an error value.  Instead, we right shift in two steps. */
		buffer.put( (byte)Long.rotateRight( 
				Long.rotateRight( size & 0xFF000000l, 16 ), 8 ) );
		
		buffer.position( 0 );
		
		return buffer;
	}
	
	public static String toString( ByteBuffer buffer )
	{
		StringBuilder sb = new StringBuilder();
		
		byte[] bytes = buffer.array();
		
		for( byte b: bytes )
		{
			sb.append(String.format("%02X ", b));
			sb.append( " " );
		}
		
		return sb.toString();
	}
	
	/**
	 * Updates the current file name with the rollover counter series suffix
	 */
	private void updateFileName()
	{
		String filename = mFile.toString();

		if( mFileRolloverCounter == 2 )
		{
			filename = filename.replace( ".wav", "_2.wav" );
		}
		else
		{
			Matcher m = FILENAME_PATTERN.matcher( filename );

			if( m.find() )
			{
				StringBuilder sb = new StringBuilder();
				sb.append( m.group( 1 ) );
				sb.append( mFileRolloverCounter );
				sb.append( m.group( 3 ) );
				
				filename = sb.toString();
			}
		}
		
		mFile = Paths.get( filename );
	}
}

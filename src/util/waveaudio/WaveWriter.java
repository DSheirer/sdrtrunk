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
package util.waveaudio;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;

import log.Log;

public class WaveWriter
{
	private static final int sTOTAL_BYTE_LIMIT = Integer.MAX_VALUE;
	private BufferedOutputStream mOutputStream;
	private boolean mRunning = false;
	private AudioFormat mAudioFormat;
	private String mFileName;
	private int mTotalByteCount = 0;
	private int mFileRolloverCounter = 0;
	
	public WaveWriter( String fileName, AudioFormat format )
	{
		mAudioFormat = format;
		mFileName = fileName;
	}
	
	public void start() throws IOException
	{
		if( !mRunning )
		{
			openFile();
            
            mRunning = true;
		}
	}
	
	public void stop() throws IOException
	{
		if( mRunning )
		{
			closeFile();
			
			//Increment file rollover count, in case we re-start the recorder
			mFileRolloverCounter++;
			
			mRunning = false;
		}
	}
	
	public void write( ByteBuffer buffer ) throws IOException
	{
		checkFileRollover( buffer.capacity() );
		
		if( mRunning && mOutputStream != null )
		{
			//Write the data
			mOutputStream.write( buffer.array() );
			mTotalByteCount += buffer.capacity();
		}
	}
	
	protected void checkFileRollover( int byteCount ) throws IOException
	{
		if( mTotalByteCount + byteCount >= sTOTAL_BYTE_LIMIT )
		{
			closeFile();
			
			mFileRolloverCounter++;
			
			openFile();
		}
	}
	
	private void closeFile() throws IOException
	{
		mOutputStream.flush();
		mOutputStream.close();
		
		String filename = mFileName;

		//If this is a rollover file, append the rollover count to the filename
		if( mFileRolloverCounter > 0 )
		{
			filename += mFileRolloverCounter;
		}
		
		//Correct the total count.  Subtract 8 bytes from the total, to account
		//for the ChunkID and ChunkSize bytes, at the beginning of the file
		correctWAVEFileByteCount( filename, ( mTotalByteCount ) );
	}

	private void correctWAVEFileByteCount( String fileName, int newByteCount ) throws IOException
	{
		RandomAccessFile raf = new RandomAccessFile( fileName, "rw" );

		//Get byte array for newByteCount minus header (8 bytes)
		byte[] newTotalSize = new byte[ 4 ];
		WaveUtils.loadDouble( newTotalSize, ( newByteCount - 8 ), 0, 4 );

		//Seek to byte position 4 and write the 4 byte length value
		raf.seek( 4 );
		raf.write( newTotalSize );

		//Get byte array for newByteCount minus all headers (44 bytes)
		byte[] newDataSize = new byte[ 4 ];
		WaveUtils.loadDouble( newDataSize, ( newByteCount - 44 ), 0, 4 );

		//Seek to byte position 4 and write the 4 byte length value
		raf.seek( 40 );
		raf.write( newDataSize );

		//debug
		Log.info( "Closing recording [" + mFileName + "] amending file byte size to:" + newByteCount );
		
		raf.close();
	}
	
	private void openFile() throws IOException
	{
		mTotalByteCount = 0;
		
		String filename = mFileName;
		
		if( mFileRolloverCounter > 0 )
		{
			filename += mFileRolloverCounter;
		}
		
        mOutputStream = new BufferedOutputStream( new FileOutputStream( filename ) );
        
        //Write the RIFF descriptor
        byte[] riff = WaveUtils.getRIFFChunkDescriptor( 
        								mAudioFormat.getSampleRate(), 10 );
        mOutputStream.write( riff );
        mTotalByteCount += riff.length;
        
        //Write the WAVE descriptor
        byte[] wave = WaveUtils.getWAVEFormatDescriptor( 
							    		mAudioFormat.getSampleRate(), 
										mAudioFormat.getChannels(), 
										mAudioFormat.getSampleSizeInBits() );
        mOutputStream.write( wave );
        mTotalByteCount += wave.length;

		//Write the data chunk header - for 10 seconds of audio
		byte[] header = WaveUtils.getDataChunkHeader( 
				(int)(10 * mAudioFormat.getSampleRate() ), mAudioFormat.getChannels() );
		mOutputStream.write( header );
		mTotalByteCount += header.length;
	}
}

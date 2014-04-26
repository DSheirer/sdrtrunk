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


public class WaveUtils
{
	/**
	 * Returns a byte array representing a wave file RIFF chunk descriptor,
	 * for mono channel, signed 16-bit audio sample recording.
	 * 
	 * @param sampleRate - audio sample rate
	 * 
	 * @param duration - recording duration in seconds.  Note: this is a
	 * temporary value, since you won't know the exact duration until after the
	 * recording is complete.  Your code should overwrite this initial value
	 * with the correct value, upon completion of the recording
	 */
	public static byte[] getRIFFChunkDescriptor( float sampleRate, int duration )
	{
		//Byte array large enough to hold the descriptor
		byte[] buffer = new byte[ 12 ];

		//Load "RIFF" into the descriptor (4-bytes)
		loadDouble( buffer, 0x46464952, 0, 4 );

		//Initial total size value ... will be overwritten later (4-bytes)
		//Note: assumes that sample rate is an integer value
		int totalSize = 36 + ( (int)sampleRate * duration ); 
		
		loadDouble( buffer, totalSize, 4, 4);

		//Load "WAVE" into the descriptor (4-bytes)
		loadDouble( buffer, 0x45564157, 8, 4);
		
		return buffer;
	}

	/**
	 * WAVE format descriptor
	 * @param sampleRate 
	 */
	public static byte[] getWAVEFormatDescriptor( float sampleRate,
												  int numberOfChannels,
												  int bitsPerSample )
	{
		int bytesPerSample = bitsPerSample / 8;
		
		//Byte array large enough to hold the descriptor
		byte[] buffer = new byte[ 24 ];

		//SubChunk1ID = "fmt "
		loadDouble( buffer, 0x20746D66, 0, 4);

		//SubChunk1Size = 16
		loadDouble( buffer, 16, 4, 4);
		
		//AudioFormat = PCM ( 1 = not compressed )
		loadDouble( buffer, 1, 8, 2 );
		
		//NumberOfChannels
		loadDouble( buffer, numberOfChannels, 10, 2);

		//SampleRate
		//Note: assumes sample rate is an integer value
		loadDouble( buffer, (int)sampleRate,	12, 4 );

		//ByteRate ( sampleRate * numberOfChannels * bytesPerSample )
		loadDouble( buffer, 
					( (int)sampleRate * numberOfChannels * bytesPerSample ), 
					16, 
					4 );

		//BlockAlign ( numberOfChannels * bytesPerSample )
		loadDouble( buffer, ( numberOfChannels * bytesPerSample ), 20, 2 );	

		//BitsPerSample
		loadDouble( buffer, bitsPerSample, 22, 2 );
		
		return buffer;
	}

	/**
	 * Returns formatted data chunk header, based on the following values:
	 * 
	 * @param numberSamples - number of samples that will be in the chunk
	 * @param numberChannels - number of channels
	 * @param bitsPerSample - bits per sample
	 * @return
	 */
	public static byte[] getDataChunkHeader( int numberBytes, 
											 int numberChannels )
	{
		//Byte array large enough to hold the descriptor
		byte[] buffer = new byte[ 24 ];

		//SubChunk2ID = "data"
		loadDouble( buffer, 0x61746164, 0, 4 );

		//SubChunk2Size
		loadDouble( buffer, ( numberBytes * numberChannels ), 4, 4 );
		
		return buffer;
	}
	
	/**
	 * Loads valueToLoad into the byte buffer at start location
	 * @param buffer - byte array
	 * @param valueToLoad
	 * @param start
	 * @param numberOfBytes
	 */
	protected static void loadDouble ( byte[] buffer, 
									   long valueToLoad, 
									   int start, 
									   int numberOfBytes )
	{
		if( !( ( start + numberOfBytes ) <= buffer.length ) )
		{
			throw new IllegalArgumentException( "Byte array buffer is not "
					+ "large enough to load value:" + valueToLoad );
		}
		
		for ( int x = 0; x < numberOfBytes; x++ )
		{
			buffer[ start ] = (byte) ( valueToLoad & 0xFF);
			
			valueToLoad >>= 8;
			
			start++;
		}
	}
	
}

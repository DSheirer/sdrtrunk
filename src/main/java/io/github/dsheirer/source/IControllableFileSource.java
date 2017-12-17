package io.github.dsheirer.source;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Defines a controllable source that can be manually controlled for stepping
 * through the file.
 */
public interface IControllableFileSource
{
	/**
	 * Opens the file source
	 */
	public void open() throws IOException, UnsupportedAudioFileException;
	
	public void close() throws IOException;
	
	public File getFile();
	
	public void next( int frames ) throws IOException;
	
	public void next( int frames, boolean broadcast ) throws IOException;
	
	public long getFrameCount() throws IOException;
	
	public void setListener( IFrameLocationListener listener );
	
	public void removeListener( IFrameLocationListener listener );
}

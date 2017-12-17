package io.github.dsheirer.audio;

public class AudioException extends Exception
{
	private static final long serialVersionUID = 1L;

	public AudioException( String description )
	{
		super( description );
	}
	
	public AudioException( String description, Throwable throwable )
	{
		super( description, throwable );
	}
}

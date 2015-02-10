package controller.channel;

public class ChannelValidationException extends Exception
{
	private static final long serialVersionUID = 1L;

	public ChannelValidationException( String validationMessage )
	{
		super( validationMessage );
	}
}

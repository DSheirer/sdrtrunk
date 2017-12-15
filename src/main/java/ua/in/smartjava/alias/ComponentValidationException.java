package ua.in.smartjava.alias;

public class ComponentValidationException extends Exception
{
	private static final long serialVersionUID = 1L;

	/**
	 * Component editor contents validation error
	 */
	public ComponentValidationException( String message )
	{
		super( message );
	}
}

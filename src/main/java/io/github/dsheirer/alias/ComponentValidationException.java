package io.github.dsheirer.alias;

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

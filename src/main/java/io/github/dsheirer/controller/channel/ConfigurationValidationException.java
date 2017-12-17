package io.github.dsheirer.controller.channel;

import java.awt.*;

public class ConfigurationValidationException extends Exception
{
	private static final long serialVersionUID = 1L;

	private Component mComponent;
	
	public ConfigurationValidationException( Component component, 
											 String validationMessage )
	{
		super( validationMessage );
		
		mComponent = component;
	}
	
	public Component getComponent()
	{
		return mComponent;
	}
}

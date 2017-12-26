package io.github.dsheirer.alias.action.beep;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.action.AliasActionType;
import io.github.dsheirer.alias.action.RecurringAction;
import io.github.dsheirer.message.Message;

public class BeepAction extends RecurringAction
{

	@JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
	@Override
	public AliasActionType getType()
	{
		return AliasActionType.BEEP;
	}

	@Override
	public void performAction(Alias alias, Message message )
	{
		System.out.println( "\007" );
	}

	@Override
	public String toString()
	{
		return "Beep";
	}
}

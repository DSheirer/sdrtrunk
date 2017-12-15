package ua.in.smartjava.alias.action.beep;

import ua.in.smartjava.message.Message;
import ua.in.smartjava.alias.Alias;
import ua.in.smartjava.alias.action.AliasActionType;
import ua.in.smartjava.alias.action.RecurringAction;

public class BeepAction extends RecurringAction
{

	@Override
	public AliasActionType getType()
	{
		return AliasActionType.BEEP;
	}

	@Override
	public void performAction( Alias alias, Message message )
	{
		System.out.println( "\007" );
	}

	@Override
	public String toString()
	{
		return "Beep";
	}
}

package alias.action;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import message.Message;
import sample.Listener;
import alias.Alias;
import controller.ResourceManager;
import decode.p25.P25Decoder;

/**
 * Manages all alias action events.  Each received message is interrogated for
 * any alias entries and then each alias is interrogated for any alias actions.
 * 
 * Each alias action is executed and provided a copy of the source message so 
 * that the contents of the message can be used as part of the action.
 */
public class AliasActionManager implements Listener<Message>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( AliasActionManager.class );

	private ResourceManager mResourceManager;
	
	public AliasActionManager( ResourceManager resourceManager )
	{
		mResourceManager = resourceManager;
	}
	
	@Override
	public void receive( Message message )
	{
		List<Alias> aliases = message.getAliases();
		
		if( aliases != null )
		{
			for( Alias alias: aliases )
			{
				if( alias.hasActions() )
				{
					List<AliasAction> actions = alias.getAction();
					
					for( AliasAction action: actions )
					{
						/* Provide access to the thread pool manager in case the
						 * action needs to setup a timer, and provide the original
						 * message to be used as part of the action (e.g. sending
						 * the message as a text message to a cell phone */
						action.execute( mResourceManager.getThreadPoolManager(), 
								alias, message );
					}
				}
			}
		}
	}
}

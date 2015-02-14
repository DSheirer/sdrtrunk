package alias.action;

import message.Message;
import alias.Alias;
import controller.ThreadPoolManager;

/**
 * Alias action defines an action to execute when an alias is detected active.
 */
public abstract class AliasAction
{
	public AliasAction()
	{
	}
	
	/**
	 * Task to execute when an alias action is defined.  The message argument is
	 * the original message containing one or more aliases that have an alias
	 * action attached.  The alias argument is the parent alias containing the
	 * alias action.
	 */
	public abstract void execute( ThreadPoolManager threadPoolManager,
								  Alias alias,
								  Message message );

	/**
	 * Dismiss a persistent alias action
	 */
	public abstract void dismiss( boolean reset );
}

/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package alias.action;

import alias.Alias;
import alias.action.beep.BeepAction;
import alias.action.clip.ClipAction;
import alias.action.script.ScriptAction;
import message.Message;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Alias action defines an action to execute when an alias is detected active.
 */
@XmlSeeAlso( { BeepAction.class, ClipAction.class, ScriptAction.class } )
@XmlRootElement( name = "action" )
public abstract class AliasAction
{
	public AliasAction()
	{
	}

	@XmlTransient
	public abstract AliasActionType getType();
	
	/**
	 * Task to execute when an alias action is defined.  The message argument is
	 * the original message containing one or more aliases that have an alias
	 * action attached.  The alias argument is the parent alias containing the
	 * alias action.
	 */
	public abstract void execute( ScheduledExecutorService scheduledExecutorService, Alias alias, Message message );

	/**
	 * Dismiss a persistent alias action
	 */
	public abstract void dismiss( boolean reset );
}

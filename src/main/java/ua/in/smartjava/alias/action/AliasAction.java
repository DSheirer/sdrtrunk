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
package ua.in.smartjava.alias.action;

import ua.in.smartjava.alias.Alias;
import ua.in.smartjava.alias.action.beep.BeepAction;
import ua.in.smartjava.alias.action.clip.ClipAction;
import ua.in.smartjava.alias.action.script.ScriptAction;
import ua.in.smartjava.message.Message;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Alias action defines an action to execute when an ua.in.smartjava.alias is detected active.
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
	 * Task to execute when an ua.in.smartjava.alias action is defined.  The ua.in.smartjava.message argument is
	 * the original ua.in.smartjava.message containing one or more aliases that have an ua.in.smartjava.alias
	 * action attached.  The ua.in.smartjava.alias argument is the parent ua.in.smartjava.alias containing the
	 * ua.in.smartjava.alias action.
	 */
	public abstract void execute( ScheduledExecutorService scheduledExecutorService, Alias alias, Message message );

	/**
	 * Dismiss a persistent ua.in.smartjava.alias action
	 */
	public abstract void dismiss( boolean reset );
}

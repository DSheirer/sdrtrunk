/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package alias;

import alias.action.AliasAction;
import alias.action.AliasActionType;
import alias.action.beep.BeepAction;
import alias.action.beep.BeepActionEditor;
import alias.action.clip.ClipAction;
import alias.action.clip.ClipActionEditor;
import alias.action.script.ScriptAction;
import alias.action.script.ScriptActionEditor;
import alias.id.AliasID;
import alias.id.AliasIDType;
import alias.id.broadcast.BroadcastChannel;
import alias.id.broadcast.BroadcastChannelEditor;
import alias.id.esn.ESNEditor;
import alias.id.esn.Esn;
import alias.id.fleetsync.FleetsyncID;
import alias.id.fleetsync.FleetsyncIDEditor;
import alias.id.lojack.LoJackFunctionAndID;
import alias.id.lojack.LoJackIDEditor;
import alias.id.mdc.MDC1200ID;
import alias.id.mdc.MDC1200IDEditor;
import alias.id.mobileID.MINEditor;
import alias.id.mobileID.Min;
import alias.id.mpt1327.MPT1327ID;
import alias.id.mpt1327.MPT1327IDEditor;
import alias.id.nonrecordable.NonRecordable;
import alias.id.nonrecordable.NonRecordableEditor;
import alias.id.priority.Priority;
import alias.id.priority.PriorityEditor;
import alias.id.siteID.SiteID;
import alias.id.siteID.SiteIDEditor;
import alias.id.status.StatusID;
import alias.id.status.StatusIDEditor;
import alias.id.talkgroup.TalkgroupID;
import alias.id.talkgroup.TalkgroupIDEditor;
import alias.id.uniqueID.UniqueID;
import alias.id.uniqueID.UniqueIDEditor;
import audio.broadcast.BroadcastModel;
import gui.editor.Editor;
import gui.editor.EmptyEditor;

public class AliasFactory
{
	public static AliasID copyOf( AliasID id )
	{
		switch( id.getType() )
		{
			case BROADCAST_CHANNEL:
				BroadcastChannel originalBroadcast = (BroadcastChannel)id;
				BroadcastChannel copyBroadcast = new BroadcastChannel();
				copyBroadcast.setChannelName(originalBroadcast.getChannelName());
				return copyBroadcast;
			case ESN:
				Esn originalESN = (Esn)id;
				Esn copyESN = new Esn();
				copyESN.setEsn( originalESN.getEsn() );
				return copyESN;
			case FLEETSYNC:
				FleetsyncID originalFleetsyncID = (FleetsyncID)id;
				FleetsyncID copyFleetsyncID = new FleetsyncID();
				copyFleetsyncID.setIdent( originalFleetsyncID.getIdent() );
				return copyFleetsyncID;
			case LTR_NET_UID:
				UniqueID originalUniqueID = (UniqueID)id;
				UniqueID copyUniqueID = new UniqueID();
				copyUniqueID.setUid( originalUniqueID.getUid() );
				return copyUniqueID;
			case LOJACK:
				LoJackFunctionAndID originalLoJackFunctionAndID = (LoJackFunctionAndID)id;
				LoJackFunctionAndID copyLoJackFunctionAndID = new LoJackFunctionAndID();
				copyLoJackFunctionAndID.setFunction( originalLoJackFunctionAndID.getFunction() );
				copyLoJackFunctionAndID.setID( originalLoJackFunctionAndID.getID() );
				return copyLoJackFunctionAndID;
			case MDC1200:
				MDC1200ID originalMDC1200ID = (MDC1200ID)id;
				MDC1200ID copyMDC1200ID = new MDC1200ID();
				copyMDC1200ID.setIdent( originalMDC1200ID.getIdent() );
				return copyMDC1200ID;
			case MIN:
				Min originalMin = (Min)id;
				Min copyMin = new Min();
				copyMin.setMin( originalMin.getMin() );
				return copyMin;
			case MPT1327:
				MPT1327ID originalMPT1327ID = (MPT1327ID)id;
				MPT1327ID copyMPT1327ID = new MPT1327ID();
				copyMPT1327ID.setIdent( originalMPT1327ID.getIdent() );
				return copyMPT1327ID;
			case NON_RECORDABLE:
				return new NonRecordable();
			case PRIORITY:
				Priority originalPriority = (Priority)id;
				Priority copyPriority = new Priority();
				copyPriority.setPriority( originalPriority.getPriority() );
				return copyPriority;
			case SITE:
				SiteID originalSiteID = (SiteID)id;
				SiteID copySiteID = new SiteID();
				copySiteID.setSite( originalSiteID.getSite() );
				return copySiteID;
			case TALKGROUP:
				TalkgroupID originalTalkgroupID = (TalkgroupID)id;
				TalkgroupID copyTalkgroupID = new TalkgroupID();
				copyTalkgroupID.setTalkgroup( originalTalkgroupID.getTalkgroup() );
				return copyTalkgroupID;
			case STATUS:
				StatusID originalStatusID = (StatusID)id;
				StatusID copyStatusID = new StatusID();
				copyStatusID.setStatus( originalStatusID.getStatus() );
				return copyStatusID;
			default:
		}

		return null;
	}
	
	public static AliasAction copyOf( AliasAction action )
	{
		if( action instanceof BeepAction )
		{
			return new BeepAction();
		}
		else if( action instanceof ClipAction )
		{
			ClipAction originalClip = (ClipAction)action;
			ClipAction copyClip = new ClipAction();
			copyClip.setInterval( originalClip.getInterval() );
			copyClip.setPath( originalClip.getPath() );
			copyClip.setPeriod( originalClip.getPeriod() );
			return copyClip;
		}
		else if( action instanceof ScriptAction )
		{
			ScriptAction originalScript = (ScriptAction)action;
			ScriptAction copyScript = new ScriptAction();
			copyScript.setInterval( originalScript.getInterval() );
			copyScript.setPeriod( originalScript.getPeriod() );
			copyScript.setScript( originalScript.getScript() );
			return copyScript;
		}
		
		return null;
	}
	
	public static Alias copyOf( Alias original )
	{
		Alias copy = new Alias( original.getName() );
		copy.setList( original.getList() );
		copy.setGroup( original.getGroup() );
		copy.setColor( original.getColor() );
		copy.setIconName( original.getIconName() );
		
		for( AliasID id: original.getId() )
		{
			AliasID copyID = copyOf( id );
			
			if( copyID != null )
			{
				copy.addAliasID( copyID );
			}
		}

		for( AliasAction action: original.getAction() )
		{
			AliasAction copyAction = copyOf( action );
			
			if( copyAction != null )
			{
				copy.addAliasAction( copyAction );
			}
		}
		
		return copy;
	}

	public static Editor<AliasID> getEditor( AliasID aliasID, BroadcastModel broadcastModel )
	{
		if( aliasID != null )
		{
			switch( aliasID.getType() )
			{
				case BROADCAST_CHANNEL:
					return new BroadcastChannelEditor(aliasID, broadcastModel);
				case ESN:
					return new ESNEditor( aliasID );
				case FLEETSYNC:
					return new FleetsyncIDEditor( aliasID );
				case LTR_NET_UID:
					return new UniqueIDEditor( aliasID );
				case LOJACK:
					return new LoJackIDEditor( aliasID );
				case MDC1200:
					return new MDC1200IDEditor( aliasID );
				case MIN:
					return new MINEditor( aliasID );
				case MPT1327:
					return new MPT1327IDEditor( aliasID );
				case NON_RECORDABLE:
					return new NonRecordableEditor( aliasID );
				case PRIORITY:
					return new PriorityEditor( aliasID );
				case SITE:
					return new SiteIDEditor( aliasID );
				case STATUS:
					return new StatusIDEditor( aliasID );
				case TALKGROUP:
					return new TalkgroupIDEditor( aliasID );
				default:
					break;
			}
		}
		
		return new EmptyEditor<AliasID>();
	}
	
	public static Editor<AliasAction> getEditor( AliasAction aliasAction )
	{
		if( aliasAction != null )
		{
			switch( aliasAction.getType() )
			{
				case BEEP:
					return new BeepActionEditor( aliasAction );
				case CLIP:
					return new ClipActionEditor( aliasAction );
				case SCRIPT:
					return new ScriptActionEditor( aliasAction );
				default:
					break;
			}
		}
		
		return new EmptyEditor<AliasAction>();
	}
	
	public static AliasID getAliasID( AliasIDType type )
	{
		switch( type )
		{
			case BROADCAST_CHANNEL:
				return new BroadcastChannel();
			case ESN:
				return new Esn();
			case FLEETSYNC:
				return new FleetsyncID();
			case LTR_NET_UID:
				return new UniqueID();
			case LOJACK:
				return new LoJackFunctionAndID();
			case MDC1200:
				return new MDC1200ID();
			case MIN:
				return new Min();
			case MPT1327:
				return new MPT1327ID();
			case NON_RECORDABLE:
				return new NonRecordable();
			case PRIORITY:
				return new Priority();
			case SITE:
				return new SiteID();
			case STATUS:
				return new StatusID();
			case TALKGROUP:
				return new TalkgroupID();
			default:
				throw new IllegalArgumentException( "Unrecognized Alias ID type: " + type );
		}
	}
	
	public static AliasAction getAliasAction( AliasActionType type )
	{
		switch( type )
		{
			case BEEP:
				return new BeepAction();
			case CLIP:
				return new ClipAction();
			case SCRIPT:
				return new ScriptAction();
			default:
				throw new IllegalArgumentException( "Unrecognized Alias Action type: " + type );
		}
	}
	
}

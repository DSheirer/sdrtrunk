package alias;

import alias.action.AliasAction;
import alias.action.beep.BeepAction;
import alias.action.clip.ClipAction;
import alias.action.script.ScriptAction;
import alias.id.esn.Esn;
import alias.id.fleetsync.FleetsyncID;
import alias.id.lojack.LoJackFunctionAndID;
import alias.id.mdc.MDC1200ID;
import alias.id.mobileID.Min;
import alias.id.mpt1327.MPT1327ID;
import alias.id.siteID.SiteID;
import alias.id.talkgroup.TalkgroupID;
import alias.id.uniqueID.UniqueID;
import alias.priority.Priority;
import alias.record.NonRecordable;

public class AliasFactory
{
	public static Alias copyOf( Alias original )
	{
		Alias copy = new Alias( original.getName() );
		copy.setList( original.getList() );
		copy.setGroup( original.getGroup() );
		copy.setColor( original.getColor() );
		copy.setIconName( original.getIconName() );
		
		for( AliasID id: original.getId() )
		{
			switch( id.getType() )
			{
				case ESN:
					Esn originalESN = (Esn)id;
					Esn copyESN = new Esn();
					copyESN.setEsn( originalESN.getEsn() );
					copy.addAliasID( copyESN );
					break;
				case Fleetsync:
					FleetsyncID originalFleetsyncID = (FleetsyncID)id;
					FleetsyncID copyFleetsyncID = new FleetsyncID();
					copyFleetsyncID.setIdent( originalFleetsyncID.getIdent() );
					copy.addAliasID( copyFleetsyncID );
					break;
				case LTRNetUID:
					UniqueID originalUniqueID = (UniqueID)id;
					UniqueID copyUniqueID = new UniqueID();
					copyUniqueID.setUid( originalUniqueID.getUid() );
					copy.addAliasID( copyUniqueID );
					break;
				case LoJack:
					LoJackFunctionAndID originalLoJackFunctionAndID = (LoJackFunctionAndID)id;
					LoJackFunctionAndID copyLoJackFunctionAndID = new LoJackFunctionAndID();
					copyLoJackFunctionAndID.setFunction( originalLoJackFunctionAndID.getFunction() );
					copyLoJackFunctionAndID.setID( originalLoJackFunctionAndID.getID() );
					copy.addAliasID( copyLoJackFunctionAndID );
					break;
				case MDC1200:
					MDC1200ID originalMDC1200ID = (MDC1200ID)id;
					MDC1200ID copyMDC1200ID = new MDC1200ID();
					copyMDC1200ID.setIdent( originalMDC1200ID.getIdent() );
					copy.addAliasID( copyMDC1200ID );
					break;
				case MIN:
					Min originalMin = (Min)id;
					Min copyMin = new Min();
					copyMin.setMin( originalMin.getMin() );
					copy.addAliasID( copyMin );
					break;
				case MPT1327:
					MPT1327ID originalMPT1327ID = (MPT1327ID)id;
					MPT1327ID copyMPT1327ID = new MPT1327ID();
					copyMPT1327ID.setIdent( originalMPT1327ID.getIdent() );
					copy.addAliasID( copyMPT1327ID );
					break;
				case NonRecordable:
					copy.addAliasID( new NonRecordable() );
					break;
				case Priority:
					Priority originalPriority = (Priority)id;
					Priority copyPriority = new Priority();
					copyPriority.setPriority( originalPriority.getPriority() );
					copy.addAliasID( copyPriority );
					break;
				case Site:
					SiteID originalSiteID = (SiteID)id;
					SiteID copySiteID = new SiteID();
					copySiteID.setSite( originalSiteID.getSite() );
					copy.addAliasID( copySiteID );
					break;
				case Talkgroup:
					TalkgroupID originalTalkgroupID = (TalkgroupID)id;
					TalkgroupID copyTalkgroupID = new TalkgroupID();
					copyTalkgroupID.setTalkgroup( originalTalkgroupID.getTalkgroup() );
					copy.addAliasID( copyTalkgroupID );
					break;
				case Status:
				default:
					throw new IllegalArgumentException( "Unrecognized alias "
							+ "ID type: " + id.getType() );
			}
		}

		for( AliasAction action: original.getAction() )
		{
			if( action instanceof BeepAction )
			{
				copy.addAliasAction( new BeepAction() );
			}
			else if( action instanceof ClipAction )
			{
				ClipAction originalClip = (ClipAction)action;
				ClipAction copyClip = new ClipAction();
				copyClip.setInterval( originalClip.getInterval() );
				copyClip.setPath( originalClip.getPath() );
				copyClip.setPeriod( originalClip.getPeriod() );
				copy.addAliasAction( copyClip );
			}
			else if( action instanceof ScriptAction )
			{
				ScriptAction originalScript = (ScriptAction)action;
				ScriptAction copyScript = new ScriptAction();
				copyScript.setInterval( originalScript.getInterval() );
				copyScript.setPeriod( originalScript.getPeriod() );
				copyScript.setScript( originalScript.getScript() );
				copy.addAliasAction( copyScript );
			}
			else
			{
				throw new IllegalArgumentException( "Unrecognized alias "
						+ "action: " + action.getClass() );
			}
		}
		
		return copy;
	}

}

/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.alias;

import io.github.dsheirer.alias.action.AliasAction;
import io.github.dsheirer.alias.action.AliasActionType;
import io.github.dsheirer.alias.action.beep.BeepAction;
import io.github.dsheirer.alias.action.beep.BeepActionEditor;
import io.github.dsheirer.alias.action.clip.ClipAction;
import io.github.dsheirer.alias.action.clip.ClipActionEditor;
import io.github.dsheirer.alias.action.script.ScriptAction;
import io.github.dsheirer.alias.action.script.ScriptActionEditor;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannelEditor;
import io.github.dsheirer.alias.id.esn.ESNEditor;
import io.github.dsheirer.alias.id.esn.Esn;
import io.github.dsheirer.alias.id.legacy.fleetsync.FleetsyncID;
import io.github.dsheirer.alias.id.legacy.fleetsync.FleetsyncIDEditor;
import io.github.dsheirer.alias.id.legacy.mdc.MDC1200ID;
import io.github.dsheirer.alias.id.legacy.mdc.MDC1200IDEditor;
import io.github.dsheirer.alias.id.legacy.mobileID.MINEditor;
import io.github.dsheirer.alias.id.legacy.mobileID.Min;
import io.github.dsheirer.alias.id.legacy.mpt1327.MPT1327ID;
import io.github.dsheirer.alias.id.legacy.mpt1327.MPT1327IDEditor;
import io.github.dsheirer.alias.id.legacy.nonrecordable.NonRecordable;
import io.github.dsheirer.alias.id.legacy.nonrecordable.NonRecordableEditor;
import io.github.dsheirer.alias.id.legacy.siteID.SiteID;
import io.github.dsheirer.alias.id.legacy.siteID.SiteIDEditor;
import io.github.dsheirer.alias.id.legacy.talkgroup.LegacyTalkgroupID;
import io.github.dsheirer.alias.id.legacy.talkgroup.LegacyTalkgroupIDEditor;
import io.github.dsheirer.alias.id.legacy.uniqueID.UniqueID;
import io.github.dsheirer.alias.id.legacy.uniqueID.UniqueIDEditor;
import io.github.dsheirer.alias.id.lojack.LoJackFunctionAndID;
import io.github.dsheirer.alias.id.lojack.LoJackIDEditor;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.priority.PriorityEditor;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioEditor;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.radio.RadioRangeEditor;
import io.github.dsheirer.alias.id.record.Record;
import io.github.dsheirer.alias.id.record.RecordEditor;
import io.github.dsheirer.alias.id.status.StatusID;
import io.github.dsheirer.alias.id.status.StatusIDEditor;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupEditor;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRangeEditor;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.gui.editor.EmptyEditor;

public class AliasFactory
{
    public static AliasID copyOf(AliasID id)
    {
        switch(id.getType())
        {
            case BROADCAST_CHANNEL:
                BroadcastChannel originalBroadcast = (BroadcastChannel)id;
                BroadcastChannel copyBroadcast = new BroadcastChannel();
                copyBroadcast.setChannelName(originalBroadcast.getChannelName());
                return copyBroadcast;
            case ESN:
                Esn originalESN = (Esn)id;
                Esn copyESN = new Esn();
                copyESN.setEsn(originalESN.getEsn());
                return copyESN;
            case LTR_NET_UID:
                UniqueID originalUniqueID = (UniqueID)id;
                UniqueID copyUniqueID = new UniqueID();
                copyUniqueID.setUid(originalUniqueID.getUid());
                return copyUniqueID;
            case LOJACK:
                LoJackFunctionAndID originalLoJackFunctionAndID = (LoJackFunctionAndID)id;
                LoJackFunctionAndID copyLoJackFunctionAndID = new LoJackFunctionAndID();
                copyLoJackFunctionAndID.setFunction(originalLoJackFunctionAndID.getFunction());
                copyLoJackFunctionAndID.setID(originalLoJackFunctionAndID.getID());
                return copyLoJackFunctionAndID;
            case MIN:
                Min originalMin = (Min)id;
                Min copyMin = new Min();
                copyMin.setMin(originalMin.getMin());
                return copyMin;
            case PRIORITY:
                Priority originalPriority = (Priority)id;
                Priority copyPriority = new Priority();
                copyPriority.setPriority(originalPriority.getPriority());
                return copyPriority;
            case RADIO_ID:
                Radio originalRadio = (Radio)id;
                Radio copyRadio = new Radio(originalRadio.getProtocol(), originalRadio.getValue());
                return copyRadio;
            case RADIO_ID_RANGE:
                RadioRange originalRadioRange = (RadioRange)id;
                RadioRange copyRadioRange = new RadioRange(originalRadioRange.getProtocol(), originalRadioRange.getMinRadio(),
                    originalRadioRange.getMaxRadio());
                return copyRadioRange;
            case RECORD:
                return new Record();
            case SITE:
                SiteID originalSiteID = (SiteID)id;
                SiteID copySiteID = new SiteID();
                copySiteID.setSite(originalSiteID.getSite());
                return copySiteID;
            case STATUS:
                StatusID originalStatusID = (StatusID)id;
                StatusID copyStatusID = new StatusID();
                copyStatusID.setStatus(originalStatusID.getStatus());
                return copyStatusID;
            case TALKGROUP:
                Talkgroup originalTalkgroup = (Talkgroup)id;
                Talkgroup copyTalkgroup = new Talkgroup(originalTalkgroup.getProtocol(), originalTalkgroup.getValue());
                return copyTalkgroup;
            case TALKGROUP_RANGE:
                TalkgroupRange originalRange = (TalkgroupRange)id;
                TalkgroupRange copyRange = new TalkgroupRange(originalRange.getProtocol(), originalRange.getMinTalkgroup(),
                    originalRange.getMaxTalkgroup());
                return copyRange;

            //Legacy identifiers ... not supported
            case FLEETSYNC:
            case LEGACY_TALKGROUP:
            case MDC1200:
            case MPT1327:
            case NON_RECORDABLE:
            default:
        }

        return null;
    }

    public static AliasAction copyOf(AliasAction action)
    {
        if(action instanceof BeepAction)
        {
            return new BeepAction();
        }
        else if(action instanceof ClipAction)
        {
            ClipAction originalClip = (ClipAction)action;
            ClipAction copyClip = new ClipAction();
            copyClip.setInterval(originalClip.getInterval());
            copyClip.setPath(originalClip.getPath());
            copyClip.setPeriod(originalClip.getPeriod());
            return copyClip;
        }
        else if(action instanceof ScriptAction)
        {
            ScriptAction originalScript = (ScriptAction)action;
            ScriptAction copyScript = new ScriptAction();
            copyScript.setInterval(originalScript.getInterval());
            copyScript.setPeriod(originalScript.getPeriod());
            copyScript.setScript(originalScript.getScript());
            return copyScript;
        }

        return null;
    }

    public static Alias copyOf(Alias original)
    {
        Alias copy = new Alias(original.getName());
        copy.setList(original.getList());
        copy.setGroup(original.getGroup());
        copy.setColor(original.getColor());
        copy.setIconName(original.getIconName());

        for(AliasID id : original.getId())
        {
            AliasID copyID = copyOf(id);

            if(copyID != null)
            {
                copy.addAliasID(copyID);
            }
        }

        for(AliasAction action : original.getAction())
        {
            AliasAction copyAction = copyOf(action);

            if(copyAction != null)
            {
                copy.addAliasAction(copyAction);
            }
        }

        return copy;
    }

    public static Editor<AliasID> getEditor(AliasID aliasID, BroadcastModel broadcastModel)
    {
        if(aliasID != null)
        {
            switch(aliasID.getType())
            {
                case BROADCAST_CHANNEL:
                    return new BroadcastChannelEditor(aliasID, broadcastModel);
                case ESN:
                    return new ESNEditor(aliasID);
                case LTR_NET_UID:
                    return new UniqueIDEditor(aliasID);
                case LOJACK:
                    return new LoJackIDEditor(aliasID);
                case MIN:
                    return new MINEditor(aliasID);
                case PRIORITY:
                    return new PriorityEditor(aliasID);
                case RADIO_ID:
                    return new RadioEditor(aliasID);
                case RADIO_ID_RANGE:
                    return new RadioRangeEditor(aliasID);
                case RECORD:
                    return new RecordEditor(aliasID);
                case SITE:
                    return new SiteIDEditor(aliasID);
                case STATUS:
                    return new StatusIDEditor(aliasID);
                case TALKGROUP:
                    return new TalkgroupEditor(aliasID);
                case TALKGROUP_RANGE:
                    return new TalkgroupRangeEditor(aliasID);

                case FLEETSYNC:
                    return new FleetsyncIDEditor(aliasID);
                case LEGACY_TALKGROUP:
                    return new LegacyTalkgroupIDEditor(aliasID);
                case MDC1200:
                    return new MDC1200IDEditor(aliasID);
                case MPT1327:
                    return new MPT1327IDEditor(aliasID);
                case NON_RECORDABLE:
                    return new NonRecordableEditor(aliasID);
                default:
                    break;
            }
        }

        return new EmptyEditor<AliasID>();
    }

    public static Editor<AliasAction> getEditor(AliasAction aliasAction)
    {
        if(aliasAction != null)
        {
            switch(aliasAction.getType())
            {
                case BEEP:
                    return new BeepActionEditor(aliasAction);
                case CLIP:
                    return new ClipActionEditor(aliasAction);
                case SCRIPT:
                    return new ScriptActionEditor(aliasAction);
                default:
                    break;
            }
        }

        return new EmptyEditor<AliasAction>();
    }

    public static AliasID getAliasID(AliasIDType type)
    {
        switch(type)
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
            case RADIO_ID:
                return new Radio();
            case RADIO_ID_RANGE:
                return new RadioRange();
            case RECORD:
                return new Record();
            case TALKGROUP:
                return new Talkgroup();
            case TALKGROUP_RANGE:
                return new TalkgroupRange();
            case SITE:
                return new SiteID();
            case STATUS:
                return new StatusID();
            case LEGACY_TALKGROUP:
                return new LegacyTalkgroupID();
            default:
                throw new IllegalArgumentException("Unrecognized Alias ID type: " + type);
        }
    }

    public static AliasAction getAliasAction(AliasActionType type)
    {
        switch(type)
        {
            case BEEP:
                return new BeepAction();
            case CLIP:
                return new ClipAction();
            case SCRIPT:
                return new ScriptAction();
            default:
                throw new IllegalArgumentException("Unrecognized Alias Action type: " + type);
        }
    }

}

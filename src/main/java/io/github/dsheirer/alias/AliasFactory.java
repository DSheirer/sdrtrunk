/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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
import io.github.dsheirer.alias.action.clip.ClipAction;
import io.github.dsheirer.alias.action.script.ScriptAction;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.esn.Esn;
import io.github.dsheirer.alias.id.legacy.fleetsync.FleetsyncID;
import io.github.dsheirer.alias.id.legacy.mdc.MDC1200ID;
import io.github.dsheirer.alias.id.legacy.mobileID.Min;
import io.github.dsheirer.alias.id.legacy.mpt1327.MPT1327ID;
import io.github.dsheirer.alias.id.legacy.nonrecordable.NonRecordable;
import io.github.dsheirer.alias.id.legacy.siteID.SiteID;
import io.github.dsheirer.alias.id.legacy.talkgroup.LegacyTalkgroupID;
import io.github.dsheirer.alias.id.legacy.uniqueID.UniqueID;
import io.github.dsheirer.alias.id.lojack.LoJackFunctionAndID;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.record.Record;
import io.github.dsheirer.alias.id.status.UnitStatusID;
import io.github.dsheirer.alias.id.status.UserStatusID;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.alias.id.tone.TonesID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliasFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(AliasFactory.class);

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
                copyRadio.setOverlap(originalRadio.overlapProperty().get());
                return copyRadio;
            case RADIO_ID_RANGE:
                RadioRange originalRadioRange = (RadioRange)id;
                RadioRange copyRadioRange = new RadioRange(originalRadioRange.getProtocol(), originalRadioRange.getMinRadio(),
                    originalRadioRange.getMaxRadio());
                copyRadioRange.setOverlap(originalRadioRange.overlapProperty().get());
                return copyRadioRange;
            case RECORD:
                return new Record();
            case SITE:
                SiteID originalSiteID = (SiteID)id;
                SiteID copySiteID = new SiteID();
                copySiteID.setSite(originalSiteID.getSite());
                return copySiteID;
            case STATUS:
                UserStatusID originalUserStatusID = (UserStatusID)id;
                UserStatusID copyUserStatusID = new UserStatusID();
                copyUserStatusID.setStatus(originalUserStatusID.getStatus());
                copyUserStatusID.setOverlap(originalUserStatusID.overlapProperty().get());
                return copyUserStatusID;
            case TALKGROUP:
                Talkgroup originalTalkgroup = (Talkgroup)id;
                Talkgroup copyTalkgroup = new Talkgroup(originalTalkgroup.getProtocol(), originalTalkgroup.getValue());
                copyTalkgroup.setOverlap(originalTalkgroup.overlapProperty().get());
                return copyTalkgroup;
            case TALKGROUP_RANGE:
                TalkgroupRange originalRange = (TalkgroupRange)id;
                TalkgroupRange copyRange = new TalkgroupRange(originalRange.getProtocol(), originalRange.getMinTalkgroup(),
                    originalRange.getMaxTalkgroup());
                copyRange.setOverlap(originalRange.overlapProperty().get());
                return copyRange;
            case TONES:
                TonesID originalTones = (TonesID)id;
                TonesID copyTones = new TonesID();
                copyTones.setToneSequence(originalTones.getToneSequence().copyOf());
                return copyTones;
            case UNIT_STATUS:
                UnitStatusID originalUnitStatus = (UnitStatusID)id;
                UnitStatusID copyUnitStatusID = new UnitStatusID();
                copyUnitStatusID.setStatus(originalUnitStatus.getStatus());
                copyUnitStatusID.setOverlap(originalUnitStatus.overlapProperty().get());
                return copyUnitStatusID;

            //Legacy identifiers ... not supported
            case FLEETSYNC:
            case LEGACY_TALKGROUP:
            case LTR_NET_UID:
            case MDC1200:
            case MPT1327:
            case NON_RECORDABLE:
                return null;
            default:
                mLog.warn("Unrecognized Alias ID Type [" + id.getType() + "] cannot make copy of instance");
                break;
        }

        return null;
    }

    public static AliasAction copyOf(AliasAction action)
    {
        if(action instanceof BeepAction)
        {
            BeepAction original = (BeepAction)action;
            BeepAction copyBeep = new BeepAction();
            copyBeep.setInterval(original.getInterval());
            copyBeep.setPeriod(original.getPeriod());
            return copyBeep;
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
        copy.setAliasListName(original.getAliasListName());
        copy.setGroup(original.getGroup());
        copy.setColor(original.getColor());
        copy.setIconName(original.getIconName());

        for(AliasID id : original.getAliasIdentifiers())
        {
            AliasID copyID = copyOf(id);

            if(copyID != null)
            {
                copy.addAliasID(copyID);
            }
        }

        for(AliasAction action : original.getAliasActions())
        {
            AliasAction copyAction = copyOf(action);

            if(copyAction != null)
            {
                copy.addAliasAction(copyAction);
            }
        }

        return copy;
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
                return new UserStatusID();
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

/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * ****************************************************************************
 */
package io.github.dsheirer.alias;

import io.github.dsheirer.alias.action.AliasAction;
import io.github.dsheirer.alias.action.beep.BeepAction;
import io.github.dsheirer.alias.action.clip.ClipAction;
import io.github.dsheirer.alias.action.script.ScriptAction;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.dcs.Dcs;
import io.github.dsheirer.alias.id.esn.Esn;
import io.github.dsheirer.alias.id.legacy.mobileID.Min;
import io.github.dsheirer.alias.id.legacy.siteID.SiteID;
import io.github.dsheirer.alias.id.lojack.LoJackFunctionAndID;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.record.Record;
import io.github.dsheirer.alias.id.status.UnitStatusID;
import io.github.dsheirer.alias.id.status.UserStatusID;
import io.github.dsheirer.alias.id.talkgroup.P25FullyQualifiedTalkgroup;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.alias.id.tone.TonesID;
import java.util.ArrayList;
import java.util.List;
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
            case DCS:
                Dcs originalDcs = (Dcs)id;
                Dcs copyDcs = new Dcs();
                copyDcs.setDCSCode(originalDcs.getDCSCode());
                return copyDcs;
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
            case P25_FULLY_QUALIFIED_RADIO_ID:
                P25FullyQualifiedRadio originalP25 = (P25FullyQualifiedRadio) id;
                P25FullyQualifiedRadio copyP25 = new P25FullyQualifiedRadio(originalP25.getWacn(),
                        originalP25.getSystem(), originalP25.getValue());
                copyP25.setOverlap(originalP25.overlapProperty().get());
                return copyP25;
            case P25_FULLY_QUALIFIED_TALKGROUP:
                P25FullyQualifiedTalkgroup originalFqt = (P25FullyQualifiedTalkgroup) id;
                P25FullyQualifiedTalkgroup copyFqt = new P25FullyQualifiedTalkgroup(originalFqt.getWacn(),
                        originalFqt.getSystem(), originalFqt.getValue());
                copyFqt.setOverlap(originalFqt.overlapProperty().get());
                return copyFqt;
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

    public static Alias shallowCopyOf(Alias original)
    {
        Alias copy = new Alias(original.getName());
        copy.setAliasListName(original.getAliasListName());
        copy.setGroup(original.getGroup());
        copy.setColor(original.getColor());
        copy.setIconName(original.getIconName());
        copy.setRecordable(original.isRecordable());
        return copy;
    }

    public static List<AliasID> copyAliasIDs(Alias original)
    {
        List<AliasID> aliasIDS = new ArrayList<>();

        for(AliasID id : original.getAliasIdentifiers())
        {
            aliasIDS.add(copyOf(id));
        }

        return aliasIDS;
    }

    public static List<AliasAction> copyAliasActions(Alias original)
    {
        List<AliasAction> actions = new ArrayList<>();

        for(AliasAction aliasAction: original.getAliasActions())
        {
            actions.add(copyOf(aliasAction));
        }

        return actions;
    }

    public static Alias copyOf(Alias original)
    {
        Alias copy = shallowCopyOf(original);

        List<AliasID> aliasIDS = copyAliasIDs(original);

        for(AliasID aliasID: aliasIDS)
        {
            copy.addAliasID(aliasID);
        }

        List<AliasAction> actions = copyAliasActions(original);

        for(AliasAction action: actions)
        {
            copy.addAliasAction(action);
        }

        return copy;
    }
}

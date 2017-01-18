/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package playlist.version1;

import alias.Alias;
import alias.action.AliasAction;
import alias.action.beep.BeepAction;
import alias.action.clip.ClipAction;
import alias.action.script.ScriptAction;
import alias.id.AliasID;
import alias.id.esn.Esn;
import alias.id.fleetsync.FleetsyncID;
import alias.id.lojack.LoJackFunctionAndID;
import alias.id.mdc.MDC1200ID;
import alias.id.mobileID.Min;
import alias.id.mpt1327.MPT1327ID;
import alias.id.nonrecordable.NonRecordable;
import alias.id.priority.Priority;
import alias.id.siteID.SiteID;
import alias.id.status.StatusID;
import alias.id.talkgroup.TalkgroupID;
import alias.id.uniqueID.UniqueID;
import module.decode.lj1200.LJ1200Message.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@XmlSeeAlso({Alias.class,

			   /* Alias ID's */
    AliasID.class,
    FleetsyncID.class,
    Esn.class,
    Group.class,
    LoJackFunctionAndID.class,
    MDC1200ID.class,
    Min.class,
    MPT1327ID.class,
    UniqueID.class,
    SiteID.class,
    StatusID.class,
    TalkgroupID.class,
			   
			   /* Alias Attributes */
    NonRecordable.class,
    Priority.class,
			   
			   /* Alias Actions */
    AliasAction.class,
    BeepAction.class,
    ClipAction.class,
    ScriptAction.class})

@Deprecated
@XmlRootElement(name = "alias_list")
public class AliasListOld implements Comparable<AliasListOld>
{
    private final static Logger mLog = LoggerFactory.getLogger(AliasListOld.class);

    private String mName;
    private ArrayList<Group> mGroups = new ArrayList<Group>();

    private Map<String,Alias> mESN = new HashMap<>();
    private Map<String,Alias> mFleetsync = new HashMap<>();
    private Map<LoJackFunctionAndID,Alias> mLoJack = new HashMap<>();
    private Map<String,Alias> mMDC1200 = new HashMap<>();
    private Map<String,Alias> mMobileID = new HashMap<>();
    private Map<String,Alias> mMPT1327 = new HashMap<>();
    private Map<String,Alias> mSiteID = new HashMap<>();
    private Map<Integer,Alias> mStatus = new HashMap<>();
    private Map<String,Alias> mTalkgroup = new HashMap<>();
    private Map<Integer,Alias> mUniqueID = new HashMap<>();

    private boolean mESNWildcard = false;
    private boolean mMobileIDWildcard = false;
    private boolean mFleetsyncWildcard = false;
    private boolean mMDC1200Wildcard = false;
    private boolean mMPT1327Wildcard = false;
    private boolean mSiteWildcard = false;
    private boolean mTalkgroupWildcard = false;

    public AliasListOld()
    {
        this(null);
    }

    public AliasListOld(String name)
    {
        mName = name;
        update();
    }

    /**
     * Load/Reload all lookup hashmaps
     */
    public void update()
    {
        //Clear hashmaps
        mESN.clear();
        mFleetsync.clear();
        mLoJack.clear();
        mMDC1200.clear();
        mMobileID.clear();
        mMPT1327.clear();
        mSiteID.clear();
        mStatus.clear();
        mTalkgroup.clear();
        mUniqueID.clear();

        for(Group group : mGroups)
        {
            for(Alias alias : group.getAlias())
            {
                for(AliasID id : alias.getId())
                {
                    switch(id.getType())
                    {
                        case ESN:
                            Esn esn = (Esn) id;
                            if(esn.getEsn().contains("*"))
                            {
                                mESNWildcard = true;

                                mESN.put(fixWildcard(esn.getEsn()), alias);
                            }
                            else
                            {
                                mESN.put(esn.getEsn(), alias);
                            }
                            break;
                        case FLEETSYNC:
                            FleetsyncID fs = (FleetsyncID) id;

                            if(fs.getIdent().contains("*"))
                            {
                                mFleetsyncWildcard = true;

                                mFleetsync.put(fixWildcard(fs.getIdent()), alias);
                            }
                            else
                            {
                                mFleetsync.put(fs.getIdent(), alias);
                            }
                            break;
                        case LOJACK:
                            mLoJack.put((LoJackFunctionAndID) id, alias);
                            break;
                        case MDC1200:
                            MDC1200ID mdc = (MDC1200ID) id;

                            if(mdc.getIdent().contains("*"))
                            {
                                mMDC1200Wildcard = true;

                                //Replace (*) wildcard with regex wildcard (.)
                                mMDC1200.put(fixWildcard(mdc.getIdent()), alias);
                            }
                            else
                            {
                                mMDC1200.put(mdc.getIdent(), alias);
                            }
                            break;
                        case MPT1327:
                            MPT1327ID mpt = (MPT1327ID) id;

                            String ident = mpt.getIdent();

                            if(ident != null)
                            {
                                if(ident.contains("*"))
                                {
                                    mMPT1327Wildcard = true;

                                    //Replace (*) wildcard with regex wildcard (.)
                                    mMPT1327.put(fixWildcard(ident), alias);
                                }
                                else
                                {
                                    mMPT1327.put(ident, alias);
                                }
                            }
                            break;
                        case MIN:
                            Min min = (Min) id;

                            if(min.getMin().contains("*"))
                            {
                                mMobileIDWildcard = true;
                                mMobileID.put(fixWildcard(min.getMin()), alias);
                            }
                            else
                            {
                                mMobileID.put(min.getMin(), alias);
                            }
                            break;
                        case LTR_NET_UID:
                            UniqueID uid = (UniqueID) id;

                            mUniqueID.put(uid.getUid(), alias);
                            break;
                        case SITE:
                            SiteID siteID = (SiteID) id;

                            if(siteID.getSite().contains("*"))
                            {
                                mSiteWildcard = true;
                                mSiteID.put(fixWildcard(siteID.getSite()), alias);
                            }
                            else
                            {
                                mSiteID.put(siteID.getSite(), alias);
                            }

                            break;
                        case STATUS:
                            mStatus.put(((StatusID) id).getStatus(), alias);
                            break;
                        case TALKGROUP:
                            TalkgroupID tgid = (TalkgroupID) id;

                            if(tgid.getTalkgroup().contains("*"))
                            {
                                mTalkgroupWildcard = true;

                                //Replace (*) wildcard with regex wildcard (.)
                                mTalkgroup.put(fixWildcard(tgid.getTalkgroup()), alias);
                            }
                            else
                            {
                                mTalkgroup.put(tgid.getTalkgroup(), alias);
                            }
                            break;
                        case NON_RECORDABLE:
                        case PRIORITY:
                            //We don't maintain lookups for these items
                            break;
                        default:
                            mLog.warn("Unrecognized Alias ID Type:" + id.getType().name());
                            break;
                    }
                }
            }
        }
    }

    /**
     * Converts user wildcard character (*) to regex single character wildcard (.)
     * but ignores a regex multi-character wildcard (.*)
     */
    private String fixWildcard(String value)
    {
        if(value.contains("*") && !value.contains(".*"))
        {
            return value.replace("*", ".");
        }

        return value;
    }

    public Alias getSiteID(String siteID)
    {
        if(siteID != null)
        {
            if(mSiteWildcard)
            {
                for(String regex : mSiteID.keySet())
                {
                    if(siteID.matches(regex))
                    {
                        return mSiteID.get(regex);
                    }
                }
            }
            else
            {
                return mSiteID.get(siteID);
            }
        }

        return null;
    }

    public Alias getStatus(int status)
    {
        return mStatus.get(status);
    }

    public Alias getUniqueID(int uniqueID)
    {
        return mUniqueID.get(uniqueID);
    }

    public Alias getESNAlias(String esn)
    {
        if(esn != null)
        {
            if(mESNWildcard)
            {
                for(String regex : mESN.keySet())
                {
                    if(esn.matches(regex))
                    {
                        return mESN.get(regex);
                    }
                }
            }
            else
            {
                return mESN.get(esn);
            }
        }

        return null;
    }


    public Alias getFleetsyncAlias(String ident)
    {
        if(ident != null)
        {
            if(mFleetsyncWildcard)
            {
                for(String regex : mFleetsync.keySet())
                {
                    if(ident.matches(regex))
                    {
                        return mFleetsync.get(regex);
                    }
                }
            }
            else
            {
                return mFleetsync.get(ident);
            }
        }

        return null;
    }

    public Alias getLoJackAlias(Function function, String id)
    {
        if(id != null)
        {
            for(LoJackFunctionAndID lojack : mLoJack.keySet())
            {
                if(lojack.matches(function, id))
                {
                    return mLoJack.get(lojack);
                }
            }
        }

        return null;
    }

    public Alias getMDC1200Alias(String ident)
    {
        if(ident != null)
        {
            if(mMDC1200Wildcard)
            {
                for(String regex : mMDC1200.keySet())
                {
                    if(ident.matches(regex))
                    {
                        return mMDC1200.get(regex);
                    }
                }
            }
            else
            {
                return mMDC1200.get(ident);
            }
        }

        return null;
    }

    public Alias getMPT1327Alias(String ident)
    {
        if(ident != null)
        {
            if(mMPT1327Wildcard)
            {
                for(String regex : mMPT1327.keySet())
                {
                    if(ident.matches(regex))
                    {
                        return mMPT1327.get(regex);
                    }
                }
            }
            else
            {
                return mMPT1327.get(ident);
            }
        }

        return null;
    }

    public Group getGroup(Alias alias)
    {
        if(alias != null)
        {
            for(Group group : mGroups)
            {
                if(group.contains(alias))
                {
                    return group;
                }
            }
        }

        return null;
    }

    public Alias getMobileIDNumberAlias(String ident)
    {
        if(ident != null)
        {
            if(mMobileIDWildcard)
            {
                for(String regex : mMobileID.keySet())
                {
                    if(ident.matches(regex))
                    {
                        return mMobileID.get(regex);
                    }
                }
            }
            else
            {
                return mMobileID.get(ident);
            }
        }

        return null;
    }

    public Alias getTalkgroupAlias(String tgid)
    {
        if(tgid != null)
        {
            if(mTalkgroupWildcard)
            {
                for(String regex : mTalkgroup.keySet())
                {
                    if(tgid.matches(regex))
                    {
                        return mTalkgroup.get(regex);
                    }
                }
            }
            else
            {
                return mTalkgroup.get(tgid);
            }
        }

        return null;
    }

    public String toString()
    {
        return mName;
    }

    @XmlAttribute
    public String getName()
    {
        return mName;
    }

    public void setName(String name)
    {
        mName = name;
    }

    public ArrayList<Group> getGroup()
    {
        return mGroups;
    }

    public void setGroup(ArrayList<Group> groups)
    {
        mGroups = groups;
    }

    public void addGroup(Group group)
    {
        mGroups.add(group);
    }

    public void removeGroup(Group group)
    {
        mGroups.remove(group);
    }

    @Override
    public int compareTo(AliasListOld otherAliasList)
    {
        return getName().compareTo(otherAliasList.getName());
    }
}

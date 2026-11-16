/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.edacs.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.edacs.EDACSMessageType;
import io.github.dsheirer.module.decode.edacs.identifier.EDACSRadio;
import io.github.dsheirer.module.decode.edacs.identifier.EDACSTalkgroup;
import io.github.dsheirer.protocol.Protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EDACS control channel message with all fields extracted from the
 * 28-bit data payload(s) per the DSD-FME {@code edacs-fme.c} dispatch
 * table.
 */
public class EDACSMessage extends Message
{
    private EDACSMessageType mMessageType;
    private CorrectedBinaryMessage mData;
    private CorrectedBinaryMessage mData2;
    private String mDetails;

    /** Talkgroup for GROUP_CALL, LOGIN, ALL_CALL */
    private int mGroup;
    /** Source radio for GROUP_CALL, LOGIN, INDIVIDUAL_CALL, ALL_CALL (20-bit) */
    private int mSource;
    /** Target radio for INDIVIDUAL_CALL (20-bit) */
    private int mTarget;
    /** Logical Channel Number (0-25) */
    private int mLCN;
    /** True if the call is digital (ProVoice) instead of analog FM */
    private boolean mDigital;
    /** True if the message is an update to an existing call (vs assignment) */
    private boolean mUpdate;
    /** True if transmission-trunked (vs message-trunked) */
    private boolean mTxTrunking;
    /** True if emergency priority */
    private boolean mEmergency;

    /** Site ID for SYSTEM_INFO/EXTENDED_ADDRESSING */
    private int mSiteId;
    /** Area code for EXTENDED_ADDRESSING */
    private int mArea;
    /** System ID for SYSTEM_INFO */
    private int mSystemId;
    /** CC LCN for SYSTEM_INFO */
    private int mCcLcn;

    /** Adjacent site fields */
    private int mAdjLcn;
    private int mAdjIndex;
    private int mAdjSiteId;

    /** Dynamic regroup plan bitmap fields */
    private int mBank1;
    private int mResident1;
    private int mActive1;
    private int mBank2;
    private int mResident2;
    private int mActive2;

    /** Dynamic regroup detail fields */
    private int mRegroupOption;
    private int mRegroupSsn;

    public EDACSMessage(EDACSMessageType messageType, CorrectedBinaryMessage data, long timestamp)
    {
        this(messageType, data, null, timestamp);
    }

    public EDACSMessage(EDACSMessageType messageType, CorrectedBinaryMessage data, CorrectedBinaryMessage data2,
                        long timestamp)
    {
        super(timestamp);
        mMessageType = messageType;
        mData = data;
        mData2 = data2;
    }

    public EDACSMessageType getMessageType()
    {
        return mMessageType;
    }

    public void setMessageType(EDACSMessageType type)
    {
        mMessageType = type;
    }

    public CorrectedBinaryMessage getData()
    {
        return mData;
    }

    public CorrectedBinaryMessage getData2()
    {
        return mData2;
    }

    public int getGroup() { return mGroup; }
    public void setGroup(int group) { mGroup = group; }

    public int getSource() { return mSource; }
    public void setSource(int source) { mSource = source; }

    public int getTarget() { return mTarget; }
    public void setTarget(int target) { mTarget = target; }

    /** Dynamic regroup detail option field: 7 for an active patch, 0 when a patch is deleted */
    public int getRegroupOption() { return mRegroupOption; }
    public void setRegroupOption(int option) { mRegroupOption = option; }

    /** Dynamic regroup detail SSN field, which differs per site for the same supergroup */
    public int getRegroupSsn() { return mRegroupSsn; }
    public void setRegroupSsn(int ssn) { mRegroupSsn = ssn; }

    /**
     * Indicates if this dynamic regroup detail deletes the patch: DSD-FME treats a message whose target is the
     * supergroup itself as a delete.
     */
    public boolean isRegroupDelete()
    {
        return mMessageType == EDACSMessageType.DYNAMIC_REGROUP_DETAIL && mTarget == mGroup;
    }

    public int getLCN() { return mLCN; }
    public void setLCN(int lcn) { mLCN = lcn; }

    public boolean isDigital() { return mDigital; }
    public void setDigital(boolean digital) { mDigital = digital; }

    public boolean isUpdate() { return mUpdate; }
    public void setUpdate(boolean update) { mUpdate = update; }

    public boolean isTxTrunking() { return mTxTrunking; }
    public void setTxTrunking(boolean txTrunking) { mTxTrunking = txTrunking; }

    public boolean isEmergency() { return mEmergency; }
    public void setEmergency(boolean emergency) { mEmergency = emergency; }

    public int getSiteId() { return mSiteId; }
    public void setSiteId(int siteId) { mSiteId = siteId; }

    public int getArea() { return mArea; }
    public void setArea(int area) { mArea = area; }

    public int getSystemId() { return mSystemId; }
    public void setSystemId(int systemId) { mSystemId = systemId; }

    public int getCcLcn() { return mCcLcn; }
    public void setCcLcn(int ccLcn) { mCcLcn = ccLcn; }

    public int getAdjLcn() { return mAdjLcn; }
    public void setAdjLcn(int adjLcn) { mAdjLcn = adjLcn; }

    public int getAdjIndex() { return mAdjIndex; }
    public void setAdjIndex(int adjIndex) { mAdjIndex = adjIndex; }

    public int getAdjSiteId() { return mAdjSiteId; }
    public void setAdjSiteId(int adjSiteId) { mAdjSiteId = adjSiteId; }

    public int getBank1() { return mBank1; }
    public void setBank1(int v) { mBank1 = v; }
    public int getResident1() { return mResident1; }
    public void setResident1(int v) { mResident1 = v; }
    public int getActive1() { return mActive1; }
    public void setActive1(int v) { mActive1 = v; }
    public int getBank2() { return mBank2; }
    public void setBank2(int v) { mBank2 = v; }
    public int getResident2() { return mResident2; }
    public void setResident2(int v) { mResident2 = v; }
    public int getActive2() { return mActive2; }
    public void setActive2(int v) { mActive2 = v; }

    public void setDetails(String details) { mDetails = details; }
    public String getDetails() { return mDetails; }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.EDACS;
    }

    @Override
    public boolean isValid()
    {
        return mData != null && mData.getCorrectedBitCount() >= 0;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        List<Identifier> ids = new ArrayList<>();

        switch(mMessageType)
        {
            case GROUP_CALL:
            case DIGITAL_GROUP_CALL:
            case ANALOG_GROUP_CALL:
            case TDMA_GROUP_CALL:
            case ALL_CALL:
            case LOGIN:
                ids.add(new EDACSTalkgroup(mGroup, Role.TO));
                if(mSource > 0)
                {
                    ids.add(new EDACSRadio(mSource, Role.FROM));
                }
                break;
            case INDIVIDUAL_CALL:
                if(mTarget > 0)
                {
                    ids.add(new EDACSRadio(mTarget, Role.TO));
                }
                if(mSource > 0)
                {
                    ids.add(new EDACSRadio(mSource, Role.FROM));
                }
                break;
            default:
                // Non-call messages (system info, adjacent site, dynamic
                // regroup, etc.) have no per-call identifiers.
                break;
        }

        return ids;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("EDACS ").append(mMessageType);
        if(isValid())
        {
            sb.append(" BCH:PASS");
        }
        else
        {
            sb.append(" BCH:FAIL");
        }
        if(mDetails != null)
        {
            sb.append(" ").append(mDetails);
        }
        return sb.toString();
    }
}

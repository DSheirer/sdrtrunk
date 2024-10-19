/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.harris.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.VendorOSPMessage;
import io.github.dsheirer.module.decode.p25.reference.RegroupOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * L3Harris Group Regroup Explicit Encryption Command.
 * <p>
 * This OSP is used to create, remove or update a Supergroup address associated with one member WGID or WUID with
 * explicit encryption parameters.
 */
public class L3HarrisGroupRegroupExplicitEncryptionCommand extends VendorOSPMessage
{
    private static final IntField REGROUP_OPTIONS = IntField.length8(OCTET_2_BIT_16);
    private static final IntField SUPERGROUP_ADDRESS = IntField.length16(OCTET_3_BIT_24);
    private static final IntField ENCRYPTION_KEY_ID = IntField.length16(OCTET_5_BIT_40);
    private static final IntField TARGET_RADIO_ADDRESS = IntField.length24(OCTET_7_BIT_56);
    private static final IntField TARGET_TALKGROUP_ADDRESS = IntField.length16(OCTET_8_BIT_64);

    private TalkgroupIdentifier mSuperGroupIdentifier;
    private APCO25PatchGroup mPatchGroup;
    private Identifier mPatchedIdentifier;
    private List<Identifier> mIdentifiers;
    private RegroupOptions mRegroupOptions;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public L3HarrisGroupRegroupExplicitEncryptionCommand(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    /**
     * Options for this regrouping that indicate the regrouping actions and targets.
     * @return regroup options
     */
    public RegroupOptions getRegroupOptions()
    {
        if(mRegroupOptions == null)
        {
            mRegroupOptions = new RegroupOptions(getInt(REGROUP_OPTIONS));
        }

        return mRegroupOptions;
    }

    /**
     * Encryption key ID to use for the patch group.  Note: this only applies for patched talkgroups and the value is
     * zero if the target address is a radio identifier.
     * @return encryption key identifier
     */
    public int getEncryptionKeyId()
    {
        return getInt(ENCRYPTION_KEY_ID);
    }

    /**
     * Indicates if there is a specified encryption key to use for the patch or simulselect communications.
     * @return true if key is specified.
     */
    public boolean hasEncryptionKeyId()
    {
        return getEncryptionKeyId() > 0;
    }

    /**
     * APCO25 Patch Group or Simul-Select (e.g. Harris) loaded with the target address specified in this message.
     * @return patch group
     */
    public APCO25PatchGroup getPatchGroup()
    {
        if(mPatchGroup == null)
        {
            PatchGroup patchGroup = new PatchGroup(getSuperGroup(), getRegroupOptions().getSupergroupSequenceNumber());

            if(getTargetAddress() instanceof APCO25Talkgroup talkgroup)
            {
                patchGroup.addPatchedTalkgroup(talkgroup);
            }
            else if(getTargetAddress() instanceof APCO25RadioIdentifier radio)
            {
                patchGroup.addPatchedRadio(radio);
            }

            mPatchGroup = APCO25PatchGroup.create(patchGroup);
        }

        return mPatchGroup;
    }

    /**
     * Target address for the regrouping action.
     * @return target talkgroup or radio address.
     */
    public Identifier getTargetAddress()
    {
        if(mPatchedIdentifier == null)
        {
            if(getRegroupOptions().isTalkgroupAddress())
            {
                mPatchedIdentifier = APCO25Talkgroup.create(getInt(TARGET_TALKGROUP_ADDRESS));
            }
            else
            {
                mPatchedIdentifier = APCO25RadioIdentifier.createTo(getInt(TARGET_RADIO_ADDRESS));
            }
        }

        return mPatchedIdentifier;
    }

    /**
     * Talkgroup identifier for the super group aka patch group
     *
     * @return patch group identifier
     */
    public TalkgroupIdentifier getSuperGroup()
    {
        if(mSuperGroupIdentifier == null)
        {
            mSuperGroupIdentifier = APCO25Talkgroup.create(getMessage().getInt(SUPERGROUP_ADDRESS));
        }

        return mSuperGroupIdentifier;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());

        if(getRegroupOptions().isActivate())
        {
            sb.append(" ACTIVATE");
            sb.append(getRegroupOptions().isPatch() ? " TWO-WAY PATCH" : " ONE-WAY SIMUL-SELECT");
            sb.append(" SUPERGROUP:").append(getSuperGroup());
            sb.append(" V").append(getRegroupOptions().getSupergroupSequenceNumber());

            sb.append(" INCLUDE");
            sb.append(getRegroupOptions().isTalkgroupAddress() ? " TALKGROUP:" : " RADIO:");
            sb.append(getTargetAddress());
            if(hasEncryptionKeyId())
            {
                sb.append(" USE ENCRYPTION KEY:").append(getEncryptionKeyId());
            }

        }
        else
        {
            sb.append(" DEACTIVATE SUPERGROUP:").append(getSuperGroup());
            sb.append(" V").append(getRegroupOptions().getSupergroupSequenceNumber());
        }

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getPatchGroup());
        }

        return mIdentifiers;
    }
}

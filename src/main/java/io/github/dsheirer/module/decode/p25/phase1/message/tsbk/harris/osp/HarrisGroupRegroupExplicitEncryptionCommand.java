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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.harris.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.RegroupOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Harris Group Regroup Action and Explicit Encryption Command.
 * <p>
 * This OSP is used to create, remove or update a Supergroup address associated with one member WGID or WUID with
 * explicit encryption parameters.
 *
 * See: https://forums.radioreference.com/threads/duke-energy-p25-system.411183/page-28#post-3908078
 */
public class HarrisGroupRegroupExplicitEncryptionCommand extends OSPMessage
{
    private static final int[] REGROUP_OPTIONS = new int[]{16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] SUPERGROUP_ADDRESS = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] ENCRYPTION_KEY_ID = new int[]{40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] TARGET_ADDRESS = new int[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62,
            63, 64, 65, 66, 67, 68, 69, 70, 71};

    private TalkgroupIdentifier mSuperGroupIdentifier;
    private APCO25PatchGroup mPatchGroup;
    private Identifier mPatchedIdentifier;
    private List<Identifier> mIdentifiers;
    private RegroupOptions mRegroupOptions;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public HarrisGroupRegroupExplicitEncryptionCommand(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
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
            mRegroupOptions = new RegroupOptions(getMessage().getInt(REGROUP_OPTIONS));
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
        return getMessage().getInt(ENCRYPTION_KEY_ID);
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
                mPatchedIdentifier = APCO25Talkgroup.create(getMessage().getInt(TARGET_ADDRESS));
            }
            else
            {
                mPatchedIdentifier = APCO25RadioIdentifier.createTo(getMessage().getInt(TARGET_ADDRESS));
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

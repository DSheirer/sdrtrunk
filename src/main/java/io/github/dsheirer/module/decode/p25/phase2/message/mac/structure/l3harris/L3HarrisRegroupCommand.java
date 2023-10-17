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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import io.github.dsheirer.module.decode.p25.reference.RegroupOptions;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * L3Harris Group Regroup Explicit Encryption Command.  Provides super group activate/deactivate for dynamic regrouping
 * of talkgroups or groups of individual radios and includes optional encryption algorithm and key parameters when the
 * regrouping targets talkgroups (not radios).
 *
 * See: https://forums.radioreference.com/threads/duke-energy-p25-system.411183/page-28#post-3908078
 */
public class L3HarrisRegroupCommand extends MacStructure
{
    private static final int[] VENDOR = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] LENGTH = {18, 19, 20, 21, 22, 23};
    private static final int[] REGROUP_OPTIONS = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] SUPERGROUP = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] KEY_ID = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};

    //Variation 1: Radio identifiers only.
    private static final int[] RADIO_1 = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82,
            83, 84, 85, 86, 87};
    private static final int[] RADIO_2 = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105,
            106, 107, 108, 109, 110, 111};
    private static final int[] RADIO_3 = {112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126,
            127, 128, 129, 130, 131, 132, 133, 134, 135};

    //Variation 2: Talkgroups and Algorithm ID
    private static final int[] ALGORITHM_ID = {64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] TALKGROUP_1 = {72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] TALKGROUP_2 = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};
    private static final int[] TALKGROUP_3 = {104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119};
    private static final int[] TALKGROUP_4 = {120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135};

    private RegroupOptions mRegroupOptions;
    private PatchGroupIdentifier mPatchGroupIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public L3HarrisRegroupCommand(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(getVendor() == Vendor.HARRIS)
        {
            sb.append("L3HARRIS ");
        }
        else
        {
            sb.append("WARNING: UNKNOWN VENDOR:").append(getVendor());
        }

        if(getRegroupOptions().isActivate())
        {
            sb.append(" ACTIVATE");
            sb.append(getRegroupOptions().isPatch() ? " TWO-WAY PATCH" : " ONE-WAY SIMUL-SELECT");
            sb.append(" SUPERGROUP:").append(getPatchGroup().getValue().getPatchGroup().getValue());
            sb.append(" V").append(getRegroupOptions().getSupergroupSequenceNumber());

            sb.append(" V").append(getRegroupOptions().getSupergroupSequenceNumber());

            if(getRegroupOptions().isActivate())
            {
                if(getRegroupOptions().isTalkgroupAddress())
                {
                    sb.append(" INCLUDE TALKGROUPS:");
                    sb.append(getPatchGroup().getValue().getPatchedTalkgroupIdentifiers().stream()
                            .map(tg -> tg.getValue().toString()).collect(Collectors.joining(",")));

                    Encryption encryption = getEncryptionAlgorithm();

                    if(encryption != Encryption.UNENCRYPTED)
                    {
                        sb.append(" USE ENCRYPTION:").append(encryption.name());
                        sb.append(" KEY:").append(getKeyId());
                    }
                }
                else
                {
                    sb.append(" INCLUDE RADIOS:");
                    sb.append(getPatchGroup().getValue().getPatchedRadioIdentifiers().stream()
                            .map(radio -> radio.getValue().toString()).collect(Collectors.joining(",")));
                }
            }
        }
        else
        {
            sb.append(" DEACTIVATE SUPERGROUP:").append(getPatchGroup().getValue().getPatchGroup().getValue());
        }

        sb.append(" MSG LENGTH:").append(getLength());

        return sb.toString();
    }

    /**
     * Vendor ID.  This should be L3Harris unless another vendor is also using this Opcode.
     */
    public Vendor getVendor()
    {
        return Vendor.fromValue(getMessage().getInt(VENDOR, getOffset()));
    }

    /**
     * Message length.  Note: I'm unsure if this is the count of identifiers or if this is the quantity of bytes.
     * @return length.
     */
    public int getLength()
    {
        return getMessage().getInt(LENGTH, getOffset());
    }

    /**
     * Regrouping options for this patch group (ie activate, deactivate, patch group version, etc.
     * @return patch group options.
     */
    public RegroupOptions getRegroupOptions()
    {
        if(mRegroupOptions == null)
        {
            mRegroupOptions = new RegroupOptions(getMessage().getInt(REGROUP_OPTIONS, getOffset()));
        }

        return mRegroupOptions;
    }

    /**
     * Encryption key ID.  Note: only available when the regroup options indicates this is a talkgroup supergroup.
     * @return key ID.
     */
    public int getKeyId()
    {
        return getMessage().getInt(KEY_ID, getOffset());
    }

    /**
     * Encryption algorithm for talkgroup super-group.
     */
    public Encryption getEncryptionAlgorithm()
    {
        return Encryption.fromValue(getMessage().getInt(ALGORITHM_ID, getOffset()));
    }

    /**
     * Supergroup/Patch group referenced by this message including any patched talkgroups or individual radio identifiers.
     * @return patch group.
     */
    public PatchGroupIdentifier getPatchGroup()
    {
        if(mPatchGroupIdentifier == null)
        {
            TalkgroupIdentifier patchGroupId = APCO25Talkgroup.create(getMessage().getInt(SUPERGROUP, getOffset()));
            PatchGroup patchGroup = new PatchGroup(patchGroupId, getRegroupOptions().getSupergroupSequenceNumber());

            if(getRegroupOptions().isTalkgroupAddress())
            {
                int talkgroup1 = getMessage().getInt(TALKGROUP_1, getOffset());

                if(talkgroup1 > 0)
                {
                    patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(talkgroup1));

                    int talkgroup2 = getMessage().getInt(TALKGROUP_2, getOffset());

                    if(talkgroup2 > 0)
                    {
                        patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(talkgroup2));

                        int talkgroup3 = getMessage().getInt(TALKGROUP_3, getOffset());

                        if(talkgroup3 > 0)
                        {
                            patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(talkgroup3));

                            int talkgroup4 = getMessage().getInt(TALKGROUP_4, getOffset());

                            if(talkgroup4 > 0)
                            {
                                patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(talkgroup4));
                            }
                        }
                    }
                }
            }
            else
            {
                int radio1 = getMessage().getInt(RADIO_1, getOffset());

                if(radio1 > 0)
                {
                    patchGroup.addPatchedRadio(APCO25RadioIdentifier.createTo(radio1));

                    int radio2 = getMessage().getInt(RADIO_2, getOffset());

                    if(radio2 > 0)
                    {
                        patchGroup.addPatchedRadio(APCO25RadioIdentifier.createTo(radio2));

                        int radio3 = getMessage().getInt(RADIO_3, getOffset());

                        if(radio3 > 0)
                        {
                            patchGroup.addPatchedRadio(APCO25RadioIdentifier.createTo(radio3));
                        }
                    }
                }
            }

            mPatchGroupIdentifier = APCO25PatchGroup.create(patchGroup);
        }

        return mPatchGroupIdentifier;
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

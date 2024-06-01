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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import io.github.dsheirer.module.decode.p25.reference.RegroupOptions;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * L3Harris Group Regroup Explicit Encryption Command.
 */
public class L3HarrisGroupRegroupExplicitEncryptionCommand extends MacStructureVendor
{
    private static final IntField GRG_OPTIONS = IntField.length8(OCTET_4_BIT_24);
    private static final IntField SUPERGROUP_ADDRESS = IntField.length16(OCTET_5_BIT_32);
    private static final IntField KEY_ID = IntField.length16(OCTET_7_BIT_48);

    private static final IntField RADIO_1 = IntField.length24(OCTET_9_BIT_64);
    private static final IntField RADIO_2 = IntField.length24(OCTET_12_BIT_88);
    private static final IntField RADIO_3 = IntField.length24(OCTET_15_BIT_112);

    private static final IntField ALGORITHM_ID = IntField.length8(OCTET_9_BIT_64);
    private static final IntField TALKGROUP_1 = IntField.length16(OCTET_10_BIT_72);
    private static final IntField TALKGROUP_2 = IntField.length16(OCTET_12_BIT_88);
    private static final IntField TALKGROUP_3 = IntField.length16(OCTET_14_BIT_104);
    private static final IntField TALKGROUP_4 = IntField.length16(OCTET_16_BIT_120);

    private RegroupOptions mRegroupOptions;
    private PatchGroupIdentifier mPatchGroupIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public L3HarrisGroupRegroupExplicitEncryptionCommand(CorrectedBinaryMessage message, int offset)
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
            sb.append("L3HARRIS");
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

            if(getRegroupOptions().isActivate())
            {
                if(getRegroupOptions().isTalkgroupAddress())
                {
                    sb.append(" INCLUDE TALKGROUPS:");
                    sb.append(getPatchGroup().getValue().getPatchedTalkgroupIdentifiers().stream()
                            .map(tg -> tg.getValue().toString()).collect(Collectors.joining(",")));
                }
                else
                {
                    sb.append(" INCLUDE RADIOS:");
                    sb.append(getPatchGroup().getValue().getPatchedRadioIdentifiers().stream()
                            .map(radio -> radio.getValue().toString()).collect(Collectors.joining(",")));
                }

                int key = getKeyId();

                if(key > 0)
                {
                    sb.append(" USE ENCRYPTION:").append(getEncryptionAlgorithm());
                    sb.append(" KEY:").append(key);
                }
            }
        }
        else
        {
            sb.append(" DEACTIVATE SUPERGROUP:").append(getPatchGroup().getValue().getPatchGroup().getValue());
            sb.append(" V").append(getRegroupOptions().getSupergroupSequenceNumber());
        }

        return sb.toString();
    }

    /**
     * Regrouping options for this patch group (ie activate, deactivate, patch group version, etc.
     * @return patch group options.
     */
    public RegroupOptions getRegroupOptions()
    {
        if(mRegroupOptions == null)
        {
            mRegroupOptions = new RegroupOptions(getInt(GRG_OPTIONS));
        }

        return mRegroupOptions;
    }

    /**
     * Encryption key ID.  Note: only available when the regroup options indicates this is a talkgroup supergroup.
     * @return key ID.
     */
    public int getKeyId()
    {
        return getInt(KEY_ID);
    }

    /**
     * Encryption algorithm for talkgroup super-group.
     */
    public Encryption getEncryptionAlgorithm()
    {
        if(getRegroupOptions().isTalkgroupAddress())
        {
            return Encryption.fromValue(getMessage().getInt(ALGORITHM_ID, getOffset()));
        }
        else
        {
            //The radio version of the message does not specify an algorithm.
            return Encryption.UNKNOWN;
        }
    }

    /**
     * Supergroup/Patch group referenced by this message including any patched talkgroups or individual radio identifiers.
     * @return patch group.
     */
    public PatchGroupIdentifier getPatchGroup()
    {
        if(mPatchGroupIdentifier == null)
        {
            int octetLength = getLength();

            TalkgroupIdentifier patchGroupId = APCO25Talkgroup.create(getInt(SUPERGROUP_ADDRESS));
            PatchGroup patchGroup = new PatchGroup(patchGroupId, getRegroupOptions().getSupergroupSequenceNumber());

            if(getRegroupOptions().isTalkgroupAddress())
            {
                if(octetLength >= 11)
                {
                    int talkgroup1 = getInt(TALKGROUP_1);

                    if(talkgroup1 > 0)
                    {
                        patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(talkgroup1));

                        if(octetLength >= 13)
                        {
                            int talkgroup2 = getMessage().getInt(TALKGROUP_2, getOffset());

                            if(talkgroup2 > 0)
                            {
                                patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(talkgroup2));

                                if(octetLength >= 15)
                                {
                                    int talkgroup3 = getMessage().getInt(TALKGROUP_3, getOffset());

                                    if(talkgroup3 > 0)
                                    {
                                        patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(talkgroup3));

                                        if(octetLength >= 17)
                                        {
                                            int talkgroup4 = getMessage().getInt(TALKGROUP_4, getOffset());

                                            if(talkgroup4 > 0)
                                            {
                                                patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(talkgroup4));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                if(octetLength >= 11)
                {
                    int radio1 = getInt(RADIO_1);

                    if(radio1 > 0)
                    {
                        patchGroup.addPatchedRadio(APCO25RadioIdentifier.createTo(radio1));

                        if(octetLength >= 14)
                        {
                            int radio2 = getInt(RADIO_2);

                            if(radio2 > 0)
                            {
                                patchGroup.addPatchedRadio(APCO25RadioIdentifier.createTo(radio2));

                                if(octetLength >= 17)
                                {
                                    int radio3 = getInt(RADIO_3);

                                    if(radio3 > 0)
                                    {
                                        patchGroup.addPatchedRadio(APCO25RadioIdentifier.createTo(radio3));
                                    }
                                }
                            }
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

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

package io.github.dsheirer.module.decode.nxdn.layer2;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageFactory;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;

/**
 * Assembles SACCH fragments into a Layer 3 message
 */
public class SACCHAssembler
{
    private SACCHFragment mFragment1;
    private SACCHFragment mFragment2;
    private SACCHFragment mFragment3;
    private SACCHFragment mFragment4;

    /**
     * Processes the SACCH fragment sequence to reassemble the embedded layer 3 message
     * @param fragment to process
     * @return assembled layer 3 message or null.
     */
    public NXDNLayer3Message process(SACCHFragment fragment)
    {
        if(fragment.isValid())
        {
            switch(fragment.getStructure())
            {
                case SACCH_1_OF_4:
                    mFragment1 = fragment;
                    break;
                case SACCH_2_OF_4:
                    mFragment2 = fragment;
                    break;
                case SACCH_3_OF_4:
                    mFragment3 = fragment;
                    break;
                case SACCH_4_OF_4_LAST_OR_SINGLE:
                    mFragment4 = fragment;
                    return assemble();
            }
        }

        return null;
    }

    /**
     * Assembles the fragments into a complete message
     * @return
     */
    private NXDNLayer3Message assemble()
    {
        NXDNLayer3Message layer3 = null;

        if(mFragment1 != null && mFragment2 != null && mFragment3 != null && mFragment4 != null && mFragment4.isSuperFrame())
        {
            CorrectedBinaryMessage message = new CorrectedBinaryMessage(72);
            message.load(0, mFragment1.getFragment());
            message.load(18, mFragment2.getFragment());
            message.load(36, mFragment3.getFragment());
            message.load(54, mFragment4.getFragment());
            NXDNMessageType type = mFragment4.getLICH().isOutbound() ? NXDNMessageType.getTrafficOutbound(message) :
                    NXDNMessageType.getTrafficInbound(message);
            layer3 = NXDNMessageFactory.get(type, message, mFragment4.getTimestamp(), mFragment4.getRAN(), mFragment4.getLICH());
        }
        else if(mFragment4 != null && !mFragment4.isSuperFrame())
        {
            CorrectedBinaryMessage message = new CorrectedBinaryMessage(18);
            message.load(0, mFragment4.getFragment());
            NXDNMessageType type = mFragment4.getLICH().isOutbound() ? NXDNMessageType.getTrafficOutbound(message) :
                    NXDNMessageType.getTrafficInbound(message);
            layer3 = NXDNMessageFactory.get(type, message, mFragment4.getTimestamp(), mFragment4.getRAN(), mFragment4.getLICH());
        }

        mFragment1 = null;
        mFragment2 = null;
        mFragment3 = null;
        mFragment4 = null;

        return layer3;
    }
}

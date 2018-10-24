/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.message.lc.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;

import java.util.Collections;
import java.util.List;

public class MotorolaOpcode_0 extends LinkControlWord
{
    public static final int[] TARGET_ADDRESS = {536, 537, 538, 539, 540, 541, 546,
        547, 548, 549, 550, 551, 556, 557, 558, 559, 560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] SOURCE_ADDRESS = {720, 721, 722, 723, 724, 725, 730,
        731, 732, 733, 734, 735, 740, 741, 742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    public MotorolaOpcode_0(CorrectedBinaryMessage message)
    {
        super(message);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" ***FIX ME ******  FROM:" + getSourceAddress());

        sb.append(" TO:" + getTargetAddress());

        return sb.toString();
    }

    public String getTargetAddress()
    {
        return getMessage().getHex(TARGET_ADDRESS, 6);
    }

    public String getSourceAddress()
    {
        return getMessage().getHex(SOURCE_ADDRESS, 6);
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
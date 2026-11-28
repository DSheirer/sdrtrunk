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
package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.config.AccessCodeFilter;
import io.github.dsheirer.module.decode.config.AccessCodeGate;
import io.github.dsheirer.module.decode.nxdn.layer2.RFChannel;
import io.github.dsheirer.module.decode.nxdn.layer2.SACCHFragment;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;

/**
 * Applies a Radio Access Number (RAN) filter to an NXDN message stream.
 *
 * The RAN is trusted from a SACCH fragment that passes its CRC, and from a control channel (RCCH) message that passes
 * its CAC CRC, since the RAN is covered by those checks.  Audio and FACCH1 messages on traffic channels carry the RAN
 * of the frame's SACCH, which may have failed its CRC, so they follow the decision for the most recent trusted RAN.
 * The SACCH fragment is dispatched ahead of the audio and FACCH1 messages from the same frame, so the decision is
 * current when they arrive.
 */
public class NXDNRanGate extends AccessCodeGate
{
    /**
     * Constructs an instance
     * @param filter to apply, or null to pass all messages
     */
    public NXDNRanGate(AccessCodeFilter filter)
    {
        super(filter);
    }

    @Override
    protected Integer getTrustedAccessCode(IMessage message)
    {
        if(message instanceof SACCHFragment fragment && fragment.isValid())
        {
            return fragment.getRAN();
        }
        else if(message instanceof NXDNLayer3Message layer3 && layer3.isValid() && layer3.getLICH() != null &&
                layer3.getLICH().getRFChannel() == RFChannel.RCCH)
        {
            return layer3.getRAN();
        }

        return null;
    }
}

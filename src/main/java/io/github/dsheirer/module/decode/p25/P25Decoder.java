/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;

public abstract class P25Decoder extends Decoder implements ISourceEventListener, ISourceEventProvider, Instrumentable
{
    private P25MessageProcessor mMessageProcessor;
    private AliasList mAliasList;

    public P25Decoder(AliasList aliasList)
    {
        mAliasList = aliasList;
        mMessageProcessor = new P25MessageProcessor(mAliasList);
        mMessageProcessor.setMessageListener(getMessageListener());
    }

    protected AliasList getAliasList()
    {
        return mAliasList;
    }

    public void dispose()
    {
        super.dispose();

        mMessageProcessor.dispose();
        mMessageProcessor = null;
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE1;
    }

    protected P25MessageProcessor getMessageProcessor()
    {
        return mMessageProcessor;
    }

    public abstract Modulation getModulation();

    public enum Modulation
    {
        CQPSK("Simulcast (LSM)", "LSM"),
        C4FM("Normal (C4FM)", "C4FM");

        private String mLabel;
        private String mShortLabel;

        private Modulation(String label, String shortLabel)
        {
            mLabel = label;
            mShortLabel = shortLabel;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String getShortLabel()
        {
            return mShortLabel;
        }

        public String toString()
        {
            return getLabel();
        }
    }
}

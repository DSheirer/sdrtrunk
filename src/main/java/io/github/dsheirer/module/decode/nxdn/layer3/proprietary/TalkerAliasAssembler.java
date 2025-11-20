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

package io.github.dsheirer.module.decode.nxdn.layer3.proprietary;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assembles a talker alias from four fragments.
 */
public class TalkerAliasAssembler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TalkerAliasAssembler.class);
    private TalkerAlias mFragment1;
    private TalkerAlias mFragment2;
    private TalkerAlias mFragment3;
    private TalkerAlias mFragment4;
    private Encoding mEncoding;

    /**
     * Constructs an instance
     * @param encoding to use for talker alias values.
     */
    public TalkerAliasAssembler(Encoding encoding)
    {
        mEncoding = encoding;
    }

    /**
     * Process the talker alias fragment.
     * @param fragment to process
     * @return optional completed talker alias
     */
    public TalkerAliasComplete process(TalkerAlias fragment)
    {
        switch(fragment.getFragmentNumber())
        {
            case 1:
                reset();
                mFragment1 = fragment;
                break;
            case 2:
                mFragment2 = fragment;
                break;
            case 3:
                mFragment3 = fragment;
                break;
            case 4:
                mFragment4 = fragment;
                break;
            default:
                LOGGER.info("Unexpected NXDN Talker Alias Fragment sequence # - please notify developer: " + fragment);
        }

        if(mFragment1 != null && mFragment2 != null && mFragment3 != null && mFragment4 != null)
        {
            CorrectedBinaryMessage message = new CorrectedBinaryMessage(128);
            message.load(0, mFragment1.getFragment());
            message.load(32, mFragment2.getFragment());
            message.load(64, mFragment3.getFragment());
            message.load(96, mFragment4.getFragment());
            String alias = new String(message.getSubMessage(0, 112).getBytes(), mEncoding.getCharset()).trim();
            return new TalkerAliasComplete(alias, mFragment1.getTimestamp(), mFragment1.getRAN(), mFragment1.getLICH());
        }

        return null;
    }

    /**
     * Resets the assembler and clears any captured fragments.
     */
    public void reset()
    {
        mFragment1 = null;
        mFragment2 = null;
        mFragment3 = null;
        mFragment4 = null;
    }
}

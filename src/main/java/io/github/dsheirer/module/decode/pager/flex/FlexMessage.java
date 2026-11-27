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

package io.github.dsheirer.module.decode.pager.flex;

import io.github.dsheirer.module.decode.pager.PagerMessage;
import io.github.dsheirer.module.decode.pager.PagerMessageType;
import io.github.dsheirer.protocol.Protocol;

/**
 * FLEX paging message.
 */
public class FlexMessage extends PagerMessage
{
    private final FlexMode mMode;
    private final int mCycle;
    private final int mFrame;
    private final char mPhase;
    private final boolean mLongAddress;
    private final boolean mGroupAddress;
    private final Fragment mFragment;

    /**
     * Constructs an instance
     * @param timestamp of the frame
     * @param capcode of the pager
     * @param messageType of the content
     * @param content decoded
     * @param mode of the transmission
     * @param cycle number (0-14) from the frame information word
     * @param frame number (0-127) from the frame information word
     * @param phase (A-D) carrying the page
     * @param longAddress true if the capcode was sent as a two-word long address
     * @param groupAddress true if the capcode is a dynamic group address
     * @param fragment status for alphanumeric pages
     * @param errorCount of uncorrectable message words
     */
    public FlexMessage(long timestamp, long capcode, PagerMessageType messageType, String content, FlexMode mode,
                       int cycle, int frame, char phase, boolean longAddress, boolean groupAddress, Fragment fragment,
                       int errorCount)
    {
        super(timestamp, capcode, messageType, content, mode.getBitRate(), errorCount);
        mMode = mode;
        mCycle = cycle;
        mFrame = frame;
        mPhase = phase;
        mLongAddress = longAddress;
        mGroupAddress = groupAddress;
        mFragment = fragment;
    }

    public FlexMode getMode()
    {
        return mMode;
    }

    public int getCycle()
    {
        return mCycle;
    }

    public int getFrame()
    {
        return mFrame;
    }

    public char getPhase()
    {
        return mPhase;
    }

    public boolean isLongAddress()
    {
        return mLongAddress;
    }

    public boolean isGroupAddress()
    {
        return mGroupAddress;
    }

    /**
     * Fragment status of an alphanumeric page.  Long alphanumeric pages are sent as fragments across frames.
     */
    public Fragment getFragment()
    {
        return mFragment;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.FLEX;
    }

    @Override
    protected String getProtocolDetails()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MODE:").append(mMode.getBitRate()).append("/").append(mMode.getLevels());
        sb.append(" CYCLE:").append(mCycle).append(" FRAME:").append(mFrame).append(" PHASE:").append(mPhase);

        if(mLongAddress)
        {
            sb.append(" LONG");
        }

        if(mGroupAddress)
        {
            sb.append(" GROUP");
        }

        if(mFragment != Fragment.NONE && mFragment != Fragment.COMPLETE)
        {
            sb.append(" ").append(mFragment.getLabel());
        }

        return sb.toString();
    }

    /**
     * Alphanumeric page fragment status
     */
    public enum Fragment
    {
        NONE("", "Not Fragmented"),
        COMPLETE("COMPLETE", "Complete"),
        MORE_FOLLOWS("FRAGMENT-MORE", "Fragment, More Follows"),
        LAST("FRAGMENT-LAST", "Last Fragment");

        private final String mLabel;
        private final String mDescription;

        Fragment(String label, String description)
        {
            mLabel = label;
            mDescription = description;
        }

        public String getLabel()
        {
            return mLabel;
        }

        @Override
        public String toString()
        {
            return mDescription;
        }
    }
}

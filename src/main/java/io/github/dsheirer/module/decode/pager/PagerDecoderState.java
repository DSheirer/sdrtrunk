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

package io.github.dsheirer.module.decode.pager;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.ActivitySummary;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pager decoder state.
 *
 * Each decoded page is broadcast as a PAGE decode event with the capcode as the TO identifier and the page content in
 * the event details.  This decoder state does not broadcast channel state changes: the NBFM decoder (noise squelch)
 * controls the channel state.
 */
public class PagerDecoderState extends DecoderState
{
    private static final int MAX_SUMMARY_CAPCODES = 25;
    private static final int MAX_SUMMARY_CONTENT_LENGTH = 40;

    private final Map<String,Integer> mFormatCounts = new LinkedHashMap<>();
    private final Map<String,CapcodeActivity> mCapcodeActivity = new LinkedHashMap<>();
    private int mPageCount;
    private int mIncompletePageCount;

    /**
     * Constructs an instance
     */
    public PagerDecoderState()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.PAGER;
    }

    @Override
    public void init()
    {
    }

    @Override
    public void receive(IMessage message)
    {
        if(message instanceof PagerMessage page && page.isValid())
        {
            updateActivity(page);

            getIdentifierCollection().update(page.getIdentifiers());

            DecodeEvent event = DecodeEvent.builder(DecodeEventType.PAGE, page.getTimestamp())
                .channel(getCurrentChannel())
                .details(getEventDetails(page))
                .identifiers(getIdentifierCollection().copyOf())
                .protocol(page.getProtocol())
                .build();
            broadcast(event);

            getIdentifierCollection().remove(IdentifierClass.USER);
        }
    }

    /**
     * Decode event details for the page: the content type and content.
     */
    private static String getEventDetails(PagerMessage page)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(page.getProtocol()).append(" ").append(page.getBitRate()).append(" ");
        sb.append(page.getMessageType().getShortLabel());

        if(page.hasContent())
        {
            sb.append(": ").append(page.getContent());
        }

        if(!page.isComplete())
        {
            sb.append(" (ERRORS)");
        }

        return sb.toString();
    }

    /**
     * Updates the activity summary statistics.
     */
    private void updateActivity(PagerMessage page)
    {
        synchronized(mCapcodeActivity)
        {
            mPageCount++;

            if(!page.isComplete())
            {
                mIncompletePageCount++;
            }

            mFormatCounts.merge(page.getProtocol() + " " + page.getBitRate(), 1, Integer::sum);

            String key = page.getProtocol() + ":" + page.getCapcode();
            CapcodeActivity activity = mCapcodeActivity.computeIfAbsent(key,
                k -> new CapcodeActivity(page.getProtocol().toString(), page.getCapcode()));
            activity.update(page);
        }
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        if(event.getEvent() == DecoderStateEvent.Event.REQUEST_RESET)
        {
            resetState();
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        resetState();
    }

    @Override
    public String getActivitySummary()
    {
        ActivitySummary summary = new ActivitySummary("Pager");

        synchronized(mCapcodeActivity)
        {
            summary.section("Pages");
            summary.field("Total", mPageCount);
            summary.field("With Errors", mIncompletePageCount);

            if(!mFormatCounts.isEmpty())
            {
                summary.section("Formats");
                ActivitySummary.Table formats = summary.table("Format", "Pages");

                for(Map.Entry<String,Integer> entry: mFormatCounts.entrySet())
                {
                    formats.row(entry.getKey(), entry.getValue());
                }
            }

            if(!mCapcodeActivity.isEmpty())
            {
                summary.section("Capcodes" + (mCapcodeActivity.size() > MAX_SUMMARY_CAPCODES ?
                    " (Top " + MAX_SUMMARY_CAPCODES + " of " + mCapcodeActivity.size() + ")" : ""));
                ActivitySummary.Table capcodes = summary.table("Protocol", "Capcode", "Pages", "Last Page");

                List<CapcodeActivity> activities = new ArrayList<>(mCapcodeActivity.values());
                activities.sort(Comparator.comparingInt(CapcodeActivity::getCount).reversed());

                for(CapcodeActivity activity: activities.subList(0, Math.min(MAX_SUMMARY_CAPCODES, activities.size())))
                {
                    capcodes.row(activity.getProtocol(), activity.getCapcode(), activity.getCount(),
                        activity.getLastContent());
                }
            }
        }

        return summary.toString();
    }

    /**
     * Page activity for a capcode
     */
    private static class CapcodeActivity
    {
        private final String mProtocol;
        private final long mCapcode;
        private int mCount;
        private String mLastContent = "";

        CapcodeActivity(String protocol, long capcode)
        {
            mProtocol = protocol;
            mCapcode = capcode;
        }

        void update(PagerMessage page)
        {
            mCount++;
            String content = page.hasContent() ? page.getContent() : page.getMessageType().toString();
            mLastContent = content.length() > MAX_SUMMARY_CONTENT_LENGTH ?
                content.substring(0, MAX_SUMMARY_CONTENT_LENGTH) + "..." : content;
        }

        String getProtocol()
        {
            return mProtocol;
        }

        long getCapcode()
        {
            return mCapcode;
        }

        int getCount()
        {
            return mCount;
        }

        String getLastContent()
        {
            return mLastContent;
        }
    }
}

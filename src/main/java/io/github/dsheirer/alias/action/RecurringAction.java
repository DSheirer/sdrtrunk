/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.alias.action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.util.ThreadPool;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class RecurringAction extends AliasAction
{
    @JsonIgnore
    protected AtomicBoolean mRunning = new AtomicBoolean(false);
    @JsonIgnore
    private ScheduledFuture<?> mPerpetualAction;

    protected Interval mInterval = Interval.ONCE;
    protected int mPeriod = 5;

    public abstract void performAction(Alias alias, IMessage message);

    @Override
    public void execute(Alias alias, IMessage message)
    {
        if(mRunning.compareAndSet(false, true))
        {
            switch(mInterval)
            {
                case ONCE:
                    performThreadedAction(alias, message);
                    /* Don't reset */
                    break;
                case DELAYED_RESET:
                    performThreadedAction(alias, message);
                    ThreadPool.SCHEDULED.schedule(new ResetTask(), mPeriod, TimeUnit.SECONDS);
                    break;
                case UNTIL_DISMISSED:
                    mPerpetualAction = ThreadPool.SCHEDULED.scheduleAtFixedRate(
                        new PerformActionTask(alias, message), 0, mPeriod, TimeUnit.SECONDS);

                    StringBuilder sb = new StringBuilder();
                    sb.append("<html><div width='250'>Alias [");
                    sb.append(alias.getName());
                    sb.append("] is active in message [");
                    sb.append(message.toString());
                    sb.append("]</div></html>");

                    final String text = sb.toString();

                    EventQueue.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, text,
                            "Alias Alert", JOptionPane.INFORMATION_MESSAGE);

                        dismiss(false);

                        ThreadPool.SCHEDULED.schedule(new ResetTask(), 15, TimeUnit.SECONDS);
                    });
                    break;
                default:
            }
        }
    }

    /**
     * Spawns the performAction() event into a new thread so that it doesn't
     * delay any decoder actions.
     */
    private void performThreadedAction(final Alias alias, final IMessage message)
    {
        ThreadPool.CACHED.execute(() -> performAction(alias, message));
    }

    @Override
    public void dismiss(boolean reset)
    {
        if(mPerpetualAction != null)
        {
            mPerpetualAction.cancel(true);

            mPerpetualAction = null;
        }

        if(reset)
        {
            mRunning.set(false);
        }
    }

    @JacksonXmlProperty(isAttribute = true, localName = "period")
    public int getPeriod()
    {
        return mPeriod;
    }

    public void setPeriod(int period)
    {
        mPeriod = period;
        updateValueProperty();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "interval")
    public Interval getInterval()
    {
        return mInterval;
    }

    public void setInterval(Interval interval)
    {
        mInterval = interval;
        mRunning.set(false);
        updateValueProperty();
    }

    public enum Interval
    {
        ONCE("Once"),
        DELAYED_RESET("Once - Reset After Delay"),
        UNTIL_DISMISSED("Until Dismissed");

        private String mLabel;

        Interval(String label)
        {
            mLabel = label;
        }

        public String toString()
        {
            return mLabel;
        }
    }

    public class PerformActionTask implements Runnable
    {
        private Alias mAlias;
        private IMessage mMessage;

        public PerformActionTask(Alias alias, IMessage message)
        {
            mAlias = alias;
            mMessage = message;
        }

        @Override
        public void run()
        {
            /* Don't use the performThreadedAction() method, since this is
             * already running in a separate thread */
            performAction(mAlias, mMessage);
        }
    }

    public class ResetTask implements Runnable
    {
        @Override
        public void run()
        {
            mRunning.set(false);
        }
    }
}

/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package channel.metadata;

import alias.Alias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeMonitor<T>
{
    private final static Logger mLog = LoggerFactory.getLogger(AttributeMonitor.class);

    private Attribute mAttribute;
    private Listener<AttributeChangeRequest> mListener;
    private boolean mHeuristicsEnabled = true;
    private Map<T,Integer> mOccurrenceCounts = new HashMap<>();
    private T mCurrentValue;
    private List<T> mIllegalValues = new ArrayList<>();

    public AttributeMonitor(Attribute attribute, Listener<AttributeChangeRequest> listener)
    {
        mAttribute = attribute;
        mListener = listener;
    }

    /**
     * Processes the value and emits a change request if the value needs to be changed.  If heuristics are enabled,
     * an internal mapping of values to occurrence counts is updated and a change is emitted once a value that is
     * different from the current value, has a higher occurrence count than the current value.  If heuristics are
     * disabled, then a change is emitted at each change in the value.
     * @param t
     *
     * @return true if the value was changed after processing
     */
    public boolean process(T t)
    {
        boolean changed = false;

        if(t != null && !mIllegalValues.contains(t))
        {
            if(mHeuristicsEnabled)
            {
                if(mOccurrenceCounts.containsKey(t))
                {
                    mOccurrenceCounts.put(t, mOccurrenceCounts.get(t) + 1);
                }
                else
                {
                    mOccurrenceCounts.put(t, 1);
                }
            }

            if(mCurrentValue == null)
            {
                mCurrentValue = t;
                changed = true;
            }
            else if(!mCurrentValue.equals(t))
            {
                if(mHeuristicsEnabled)
                {
                    int count = mOccurrenceCounts.get(mCurrentValue);

                    for(Map.Entry<T,Integer> entry: mOccurrenceCounts.entrySet())
                    {
                        if(entry.getValue() > count)
                        {
                            count = entry.getValue();
                            mCurrentValue = entry.getKey();
                            changed = true;
                        }
                    }
                }
                else
                {
                    mCurrentValue = t;
                    changed = true;
                }
            }

            if(changed && mListener != null)
            {
                mListener.receive(new AttributeChangeRequest(mAttribute, getValue(), getAlias()));
            }
        }

        return changed;
    }

    /**
     * Alias for the current value.  This class does not return an alias, but subclasses can return an alias that
     * is appropriate for the monitored attribute value.
     *
     * @return an optional alias for the current value
     */
    protected Alias getAlias()
    {
        return null;
    }

    /**
     * Resets the attribute value to null and resets occurrence heuristics.
     */
    public void reset()
    {
        mCurrentValue = null;
        mOccurrenceCounts.clear();
    }

    /**
     * Adds a value to the internal list of illegal values.  The monitor will not allow the attribute to be changed to
     * any values contained in this list.
     *
     * @param illegalValue to prevent for the monitored alias
     */
    public void addIllegalValue(T illegalValue)
    {
        mIllegalValues.add(illegalValue);
    }

    /**
     * Returns the current value or null if the monitor is newly started or reset.
     */
    public T getValue()
    {
        return mCurrentValue;
    }

    /**
     * Indicates if the current value is non-null
     */
    public boolean hasValue()
    {
        return mCurrentValue != null;
    }

    public static void main(String[] args)
    {
        AttributeMonitor<String> monitor = new AttributeMonitor<>(Attribute.PRIMARY_ADDRESS_TO, new Listener<AttributeChangeRequest>()
        {
            @Override
            public void receive(AttributeChangeRequest attributeChangeRequest)
            {
                mLog.debug("Attribute Changed - value:" + attributeChangeRequest.getValue());
            }
        });
        monitor.addIllegalValue("0000");
        monitor.addIllegalValue("000000");

        monitor.process("ABCD");
        monitor.process("EFGH");
        monitor.process("0000");
        monitor.process("0000");
        monitor.process("ABCD");
        monitor.reset();

        monitor.process("EFGH");
        monitor.process("ABCD");
        monitor.process("ABCD");
        monitor.process("EFGH");
        monitor.process("EFGH");
        monitor.process("000000");
        monitor.process("000000");
        monitor.process("000000");
        monitor.process("000000");
    }
}

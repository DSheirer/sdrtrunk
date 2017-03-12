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
package instrument;

import controller.channel.Channel.ChannelType;
import instrument.tap.Tap;
import instrument.tap.TapGroup;
import module.Module;
import module.ProcessingChain;
import source.Source;

import java.util.ArrayList;
import java.util.List;

public class InstrumentableProcessingChain extends ProcessingChain implements Instrumentable
{
    public InstrumentableProcessingChain()
    {
        super(ChannelType.STANDARD);
    }

    public void setSource(Source source) throws IllegalStateException
    {
        mSource = source;
    }

    @Override
    public List<TapGroup> getTapGroups()
    {
        List<TapGroup> groups = new ArrayList<>();

        for(Module module : getModules())
        {
            if(module instanceof Instrumentable)
            {
                groups.addAll(((Instrumentable)module).getTapGroups());
            }
        }

        return groups;
    }

    @Override
    public void registerTap(Tap tap)
    {
        for(Module module : getModules())
        {
            if(module instanceof Instrumentable)
            {
                ((Instrumentable)module).registerTap(tap);
            }
        }
    }

    @Override
    public void unregisterTap(Tap tap)
    {
        for(Module module : getModules())
        {
            if(module instanceof Instrumentable)
            {
                ((Instrumentable)module).unregisterTap(tap);
            }
        }
    }
}

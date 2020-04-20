/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.*;

public class TSBKMessageFilterSet extends FilterSet<IMessage>
{
    public TSBKMessageFilterSet()
    {
        super("TSBK Trunking Signalling Block");

        addFilter(new StandardOpcodeFilter());
    }

//	@Override
//    public boolean passes( Message message )
//    {
//		if( mEnabled && canProcess( message ) )
//		{
//			TSBKMessage tsbk = (TSBKMessage) message;
//
//			switch( tsbk.getVendor() )
//			{
//				case STANDARD:
//					return 
//			}
//			Opcode opcode = tsbk.getOpcode();
//			
//			return mFilterElements.get( opcode ).isEnabled();
//		}
//
//	    return false;
//    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof TSBKMessage;
    }

    public class StandardOpcodeFilter extends Filter<IMessage>
    {
        private Map<Opcode, FilterElement<Opcode>> mStandardElements = new EnumMap<>(Opcode.class);

        public StandardOpcodeFilter()
        {
            super("Standard Opcode Filter");

            for(Opcode opcode : Opcode.values())
            {
                if(opcode != Opcode.OSP_UNKNOWN)
                {
                    mStandardElements.put(opcode, new FilterElement<Opcode>(opcode));
                }
            }
        }

        @Override
        public boolean passes(IMessage message)
        {
            if(mEnabled && canProcess(message))
            {
                Opcode opcode = ((TSBKMessage) message).getOpcode();

                if(mStandardElements.containsKey(opcode))
                {
                    return mStandardElements.get(opcode).isEnabled();
                }
            }

            return false;
        }

        @Override
        public boolean canProcess(IMessage message)
        {
            return ((TSBKMessage) message).getVendor() == Vendor.STANDARD;
        }

        @Override
        public List<FilterElement<?>> getFilterElements()
        {
            return new ArrayList<FilterElement<?>>(mStandardElements.values());
        }
    }
}

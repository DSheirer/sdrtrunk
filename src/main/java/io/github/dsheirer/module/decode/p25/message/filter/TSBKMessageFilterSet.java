package io.github.dsheirer.module.decode.p25.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        private HashMap<Opcode, FilterElement<Opcode>> mStandardElements =
                new HashMap<Opcode, FilterElement<Opcode>>();

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

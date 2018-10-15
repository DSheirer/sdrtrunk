package io.github.dsheirer.module.decode.p25.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.MotorolaOpcode;
import io.github.dsheirer.module.decode.p25.message.tsbk.vendor.VendorOpcode;
import io.github.dsheirer.module.decode.p25.reference.Opcode;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TSBKMessageFilterSet extends FilterSet<IMessage>
{
    public TSBKMessageFilterSet()
    {
        super("TSBK Trunking Signalling Block");

        addFilter(new StandardOpcodeFilter());
        addFilter(new MotorolaOpcodeFilter());
        addFilter(new VendorOpcodeFilter());
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
                if(opcode != Opcode.UNKNOWN)
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

    public class MotorolaOpcodeFilter extends Filter<IMessage>
    {
        private Map<MotorolaOpcode, FilterElement<MotorolaOpcode>> mMotorolaElements = new HashMap<MotorolaOpcode, FilterElement<MotorolaOpcode>>();

        public MotorolaOpcodeFilter()
        {
            super("Motorola Opcode Filter");

            for(MotorolaOpcode opcode : MotorolaOpcode.values())
            {
                if(opcode != MotorolaOpcode.UNKNOWN)
                {
                    mMotorolaElements.put(opcode, new FilterElement<MotorolaOpcode>(opcode));
                }
            }
        }

        @Override
        public boolean passes(IMessage message)
        {
            if(mEnabled && canProcess(message))
            {
                MotorolaOpcode opcode = ((TSBKMessage) message).getMotorolaOpcode();

                if(mMotorolaElements.containsKey(opcode))
                {
                    return mMotorolaElements.get(opcode).isEnabled();
                }
            }

            return false;
        }

        @Override
        public boolean canProcess(IMessage message)
        {
            return ((TSBKMessage) message).getVendor() == Vendor.MOTOROLA;
        }

        @Override
        public List<FilterElement<?>> getFilterElements()
        {
            return new ArrayList<FilterElement<?>>(mMotorolaElements.values());
        }
    }

    public class VendorOpcodeFilter extends Filter<IMessage>
    {
        private Map<VendorOpcode, FilterElement<VendorOpcode>> mVendorElements = new HashMap<VendorOpcode, FilterElement<VendorOpcode>>();

        public VendorOpcodeFilter()
        {
            super("Vendor Opcode Filter");

            for(VendorOpcode opcode : VendorOpcode.values())
            {
                if(opcode != VendorOpcode.UNKNOWN)
                {
                    mVendorElements.put(opcode, new FilterElement<VendorOpcode>(opcode));
                }
            }
        }

        @Override
        public boolean passes(IMessage message)
        {
            if(mEnabled && canProcess(message))
            {
                MotorolaOpcode opcode = ((TSBKMessage) message).getMotorolaOpcode();

                if(mVendorElements.containsKey(opcode))
                {
                    return mVendorElements.get(opcode).isEnabled();
                }
            }

            return false;
        }

        @Override
        public boolean canProcess(IMessage message)
        {
            Vendor vendor = ((TSBKMessage) message).getVendor();

            return vendor != Vendor.STANDARD && vendor != Vendor.MOTOROLA;
        }

        @Override
        public List<FilterElement<?>> getFilterElements()
        {
            return new ArrayList<FilterElement<?>>(mVendorElements.values());
        }
    }
}

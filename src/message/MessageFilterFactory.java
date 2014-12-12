package message;

import java.util.ArrayList;
import java.util.List;

import controller.channel.Channel;
import decode.DecoderType;
import decode.config.AuxDecodeConfiguration;
import decode.fleetsync2.FleetsyncMessageFilter;
import decode.lj1200.LJ1200MessageFilter;
import decode.ltrnet.LTRNetMessageFilter;
import decode.ltrstandard.LTRStandardMessageFilter;
import decode.mdc1200.MDCMessageFilter;
import decode.mpt1327.MPT1327MessageFilter;
import decode.p25.message.filter.P25MessageFilterSet;
import decode.passport.PassportMessageFilter;
import filter.AllPassFilter;
import filter.FilterSet;
import filter.IFilter;

/**
 * Constructs message filters for each of the decoder types
 */
public class MessageFilterFactory
{
	/**
	 * Assembles a filter set containing filters for the primary channel 
	 * decoder and each of the auxiliary decoders 
	 */
	public static FilterSet<Message> getMessageFilter( Channel channel )
	{
		FilterSet<Message> filterSet = new FilterSet<Message>( 
				"Message Filter: " + channel.getChannelDisplayName() );
		
		filterSet.addFilters( getMessageFilters( 
				channel.getDecodeConfiguration().getDecoderType() ) );

		AuxDecodeConfiguration auxConfig = channel.getAuxDecodeConfiguration();
		
		for( DecoderType decoderType: auxConfig.getAuxDecoders() )
		{
			filterSet.addFilters( getMessageFilters( decoderType ) ); 
		}

		/* If we don't have any filters, add an ALL-PASS filter */
		if( filterSet.getFilters().isEmpty() )
		{
			filterSet.addFilter( new AllPassFilter<Message>() );
		}

		return filterSet;
	}
	
	/**
	 * Returns a set of IMessageFilter objects (FilterSets or Filters) that
	 * can process all of the messages produced by the specified decoder type.
	 */
	public static List<IFilter<Message>> getMessageFilters( DecoderType decoder )
	{
		ArrayList<IFilter<Message>> filters = new ArrayList<IFilter<Message>>();

		switch( decoder )
		{
			case ACARS:
				break;
			case AIS:
				break;
			case AM:
				break;
			case APRS:
				break;
			case DMR:
				break;
			case FLEETSYNC2:
				filters.add( new FleetsyncMessageFilter() );
				break;
			case LJ_1200:
				filters.add( new LJ1200MessageFilter() );
				break;
			case LTR_NET:
				filters.add( new LTRNetMessageFilter() );
				break;
			case LTR_STANDARD:
				filters.add( new LTRStandardMessageFilter() );
				break;
			case MDC1200:
				filters.add( new MDCMessageFilter() );
				break;
			case MOTOROLA_TYPE_2:
				break;
			case MPT1327:
				filters.add( new MPT1327MessageFilter() );
				break;
			case NBFM:
				break;
			case NXDN:
				break;
			case P25_PHASE1:
			case P25_PHASE2:
				filters.add( new P25MessageFilterSet() );
				break;
			case PASSPORT:
				filters.add( new PassportMessageFilter() );
				break;
			default:
				break;
		}
		
		return filters;
	}
}

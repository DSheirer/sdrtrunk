package decode.passport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import message.Message;
import message.MessageType;
import filter.Filter;
import filter.FilterElement;

public class PassportMessageFilter extends Filter<Message>
{
	private HashMap<MessageType, FilterElement<MessageType>> mElements = 
			new HashMap<MessageType, FilterElement<MessageType>>();
	
	public PassportMessageFilter()
	{
		super( "Passport Message Filter" );

		mElements.put( MessageType.CA_STRT, 
				new FilterElement<MessageType>( MessageType.CA_STRT ) );
		mElements.put( MessageType.CA_ENDD, 
				new FilterElement<MessageType>( MessageType.CA_ENDD ) );
		mElements.put( MessageType.SY_IDLE, 
				new FilterElement<MessageType>( MessageType.SY_IDLE ) );
		mElements.put( MessageType.ID_TGAS, 
				new FilterElement<MessageType>( MessageType.ID_TGAS ) );
		mElements.put( MessageType.ID_ESNH, 
				new FilterElement<MessageType>( MessageType.ID_ESNH ) );
		mElements.put( MessageType.CA_PAGE, 
				new FilterElement<MessageType>( MessageType.CA_PAGE ) );
		mElements.put( MessageType.ID_RDIO, 
				new FilterElement<MessageType>( MessageType.ID_RDIO ) );
		mElements.put( MessageType.DA_STRT, 
				new FilterElement<MessageType>( MessageType.DA_STRT ) );
		mElements.put( MessageType.RA_REGI, 
				new FilterElement<MessageType>( MessageType.RA_REGI ) );
	}
	
	@Override
    public boolean passes( Message message )
    {
		if( mEnabled && canProcess( message ) )
		{
			PassportMessage passport = (PassportMessage)message;
			
			if( mElements.containsKey( passport.getMessageType() ) )
			{
				return mElements.get( passport.getMessageType() ).isEnabled();
			}
		}
		
	    return false;
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof PassportMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
	    return new ArrayList<FilterElement<?>>( mElements.values() );
    }
}

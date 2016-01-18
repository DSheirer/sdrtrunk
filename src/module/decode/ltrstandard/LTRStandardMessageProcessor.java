/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package module.decode.ltrstandard;

import message.Message;
import message.MessageDirection;
import module.decode.ltrstandard.message.CallEndMessage;
import module.decode.ltrstandard.message.CallMessage;
import module.decode.ltrstandard.message.IdleMessage;
import module.decode.ltrstandard.message.LTRStandardMessage;
import module.decode.ltrstandard.message.UnknownMessage;
import sample.Listener;
import alias.AliasList;
import bits.BinaryMessage;
import edac.CRC;
import edac.CRCLTR;

public class LTRStandardMessageProcessor implements Listener<BinaryMessage>
{
	private MessageDirection mDirection;
	private AliasList mAliasList;
	private Listener<Message> mMessageListener;
	
	/**
	 * Processes raw binary messages and converts them to the correct message class
	 * 
	 * @param direction - inbound (ISW) or outbound (OSW)
	 * @param list - alias list
	 */
	public LTRStandardMessageProcessor( MessageDirection direction, AliasList list )
	{
		mDirection = direction;
		mAliasList = list;
	}
	
	@Override
    public void receive( BinaryMessage binaryMessage )
    {
		if( mMessageListener != null )
		{
			//Inbound Status Word (ISW) is a bit-flipped version of the Outbound
			//(OSW), so flip the bits and process it as an OSW
			if( mDirection == MessageDirection.ISW )
			{
				binaryMessage.flip( 0, 40 );
			}
			
			CRC crc = CRCLTR.check( binaryMessage, mDirection );
			
			if( crc != CRC.FAILED_CRC && 
				crc != CRC.FAILED_PARITY )
			{
				LTRStandardMessage message;

				int channel = binaryMessage.getInt( LTRStandardMessage.CHANNEL );
				int home = binaryMessage.getInt( LTRStandardMessage.HOME_REPEATER );
				int free = binaryMessage.getInt( LTRStandardMessage.FREE );
				int group = binaryMessage.getInt( LTRStandardMessage.GROUP );

				if( isValidChannel( channel ) &&
					isValidChannel( home ) &&
					isValidChannel( free ) )
				{
					if( channel == free && group == 255 )
					{
						message = new IdleMessage( binaryMessage, mDirection, mAliasList, crc );
					}
					else
					{
						message = new CallMessage( binaryMessage, mDirection, mAliasList, crc );
					}
				}
				else if( channel == 31 && 
						 isValidChannel( home ) && 
						 isValidChannel( free ) )
				{
					message = new CallEndMessage( binaryMessage, mDirection, mAliasList, crc );
				}
				else
				{
					message = new UnknownMessage( binaryMessage, mDirection, mAliasList, crc );
				}
				
				mMessageListener.receive( message );
			}
		}
    }
	
	/**
	 * Checks the channel (LCN) number to ensure it is in the range 1 -20
	 */
	private boolean isValidChannel( int channel )
	{
		return ( 1 <= channel && channel <= 20 );
	}
	
    public void setMessageListener( Listener<Message> listener )
    {
		mMessageListener = listener;
    }

    public void removeMessageListener()
    {
		mMessageListener = null;
    }
}

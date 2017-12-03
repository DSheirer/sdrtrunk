/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package source.tuner.airspy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.tuner.Tuner;
import source.tuner.TunerClass;
import source.tuner.TunerType;
import source.tuner.airspy.AirspyTunerController.BoardID;

public class AirspyTuner extends Tuner
{
	private final static Logger mLog = LoggerFactory.getLogger( AirspyTuner.class );

	public AirspyTuner( AirspyTunerController controller )
	{
		super( "Airspy " + controller.getDeviceInfo().getSerialNumber(), controller );
	}
	
	public AirspyTunerController getController()
	{
		return (AirspyTunerController)getTunerController();
	}

	@Override
    public String getUniqueID()
    {
		try
		{
			return getController().getDeviceInfo().getSerialNumber();
		}
		catch( Exception e )
		{
			mLog.error( "error getting serial number", e );
		}
		
		return BoardID.AIRSPY.getLabel();
    }

	@Override
	public TunerClass getTunerClass()
	{
		return TunerClass.AIRSPY;
	}

	@Override
	public TunerType getTunerType()
	{
		return TunerType.AIRSPY_R820T;
	}

	@Override
	public double getSampleSize()
	{
		return 13.0;
	}
}

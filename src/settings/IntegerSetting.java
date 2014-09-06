package settings;

import javax.xml.bind.annotation.XmlAttribute;

public class IntegerSetting extends Setting
{
	private int mValue;
	
	public IntegerSetting()
	{
	}

	public IntegerSetting( String name, int value )
	{
		super( name );
		
		mValue = value;
	}
	
	@XmlAttribute
	public int getValue()
	{
		return mValue;
	}
	
	public void setValue( int value )
	{
		mValue = value;
	}
}

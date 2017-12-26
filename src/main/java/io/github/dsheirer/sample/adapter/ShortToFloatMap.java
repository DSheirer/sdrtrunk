package io.github.dsheirer.sample.adapter;

public class ShortToFloatMap
{
	private static float[] MAP = new float[ 65536 ];
	
	static
	{
		for( int x = 0; x < 65536; x++ )
		{
			MAP[ x] = (float)( x - 32768 ) / 32768.0f;
		}
	}

	public float get( short value )
	{
		return MAP[ 32768 + value ];
	}
}

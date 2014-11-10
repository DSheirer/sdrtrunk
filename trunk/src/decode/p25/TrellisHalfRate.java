package decode.p25;

import java.util.ArrayList;

import bits.BitSetBuffer;

public class TrellisHalfRate
{
	private ArrayList<ConstellationNode> mConstellationNodes = 
				new ArrayList<ConstellationNode>();

	private static final int[][] CONSTELLATION_COSTS = 
		{ { 0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4 },
		  { 1,0,2,1,2,1,3,2,2,1,3,2,3,2,4,3 },
		  { 1,2,0,1,2,3,1,2,2,3,1,2,3,4,2,3 },
		  { 2,1,1,0,3,2,2,1,3,2,2,1,4,3,3,2 },
		  { 1,2,2,3,0,1,1,2,2,3,3,4,1,2,2,3 },
		  { 2,1,3,2,1,0,2,1,3,2,4,3,2,1,3,2 },
		  { 2,3,1,2,1,2,0,1,3,4,2,3,2,3,1,2 },
		  { 3,2,2,1,2,1,1,0,4,3,3,2,3,2,2,1 },
		  { 1,2,2,3,2,3,3,4,0,1,1,2,1,2,2,3 },
		  { 2,1,3,2,3,2,4,3,1,0,2,1,2,1,3,2 },
		  { 2,3,1,2,3,4,2,3,1,2,0,1,2,3,1,2 },
		  { 3,2,2,1,4,3,3,2,2,1,1,0,3,2,2,1 },
		  { 2,3,3,4,1,2,2,3,1,2,2,3,0,1,1,2 },
		  { 3,2,4,3,2,1,3,2,2,1,3,2,1,0,2,1 },
		  { 3,4,2,3,2,3,1,2,2,3,1,2,1,2,0,1 },
		  { 4,3,3,2,3,2,2,1,3,2,2,1,2,1,1,0 } };
		
	public TrellisHalfRate()
	{
		/* Create 49 x 4-bit constellation nodes to handle 196-bit blocks */
		ConstellationNode previous = null;
		
		for( int x = 0; x < 49; x++ )
		{
			ConstellationNode node = new ConstellationNode();

			if( previous != null )
			{
				previous.connect( node );
			}
			
			previous = node;
			
			mConstellationNodes.add( node );
		}
	}
	
	public void dispose()
	{
		for( ConstellationNode node: mConstellationNodes )
		{
			node.dispose();
		}
		
		mConstellationNodes.clear();
	}

	public void decode( BitSetBuffer message, int start, int end )
	{
		/* load each of the nodes with deinterleaved constellations */
		for( int index = 0; index < 49; index++ )
		{
			Constellation c = getConstellation( message, index * 4 );
			
			mConstellationNodes.get( index ).setConstellation( c );
		}

		/* test to see if constellations are correct - otherwise correct them */
		ConstellationNode firstNode = mConstellationNodes.get( 0 );

		if( !firstNode.startsWith( Dibit.D0 ) || !firstNode.isCorrect() )
		{
			firstNode.correctTo( Dibit.D0 );
		}

		/* clear constellations from original message */
		message.clear( start, end );

		/* replace with decoded values from the nodes */
		for( int index = 0; index < 49; index++ )
		{
			ConstellationNode node = mConstellationNodes.get( index );
			
			if( node.firstBit() )
			{
				message.set( start + ( index * 2 ) );
			}
			if( node.secondBit() )
			{
				message.set( start + ( index * 2 ) + 1 );
			}
		}
	}
	
	private Constellation getConstellation( BitSetBuffer message, int index )
	{
		int constellation = 0;
		
		for( int x = 0; x < 4; x++ )
		{
			if( message.get( index + x ) )
			{
				constellation += ( 1 << ( 3 - x ) );
			}
		}
		
		return Constellation.fromValue( constellation );
	}
	
	public class ConstellationNode
	{
		private ConstellationNode mConnectedNode;
		private Constellation mConstellation;
		private boolean mCorrect;
		
		public ConstellationNode()
		{
		}
		
		public void dispose()
		{
			mConnectedNode = null;
		}
		
		public boolean startsWith( Dibit dibit )
		{
			return mConstellation.getLeft() == dibit;
		}
		
		public boolean firstBit()
		{
			return mConstellation.getRight().firstBit();
		}
		
		public boolean secondBit()
		{
			return mConstellation.getRight().secondBit();
		}
		
		/**
		 * Executes a correction down the line of connected nodes.  Only nodes
		 * with the mCorrect flag set to false will be corrected.
		 * 
		 * Note: Assumes that the starting node's value is 0.  Initiate the 
		 * corrective sequence by invoking this method with Dibit.D0 on the
		 * first node.
		 * 
		 * @param dibit to use for the left side.
		 */
		public void correctTo( Dibit dibit )
		{
			if( mCorrect && mConstellation.getLeft() == dibit )
			{
				return;
			}
			
			if( isCurrentConnectionCorrect() )
			{
				mConstellation = Constellation.
						fromDibits( dibit, mConstellation.getRight() );

				mCorrect = true;
				
				if( mConnectedNode != null )
				{
					mConnectedNode.correctTo( mConstellation.getRight() );
				}
			}
			else
			{
				Constellation cheapest = mConstellation;
				
				int cost = 100; //arbitrary
				
				for( Dibit d: Dibit.values() )
				{
					Constellation test = Constellation.fromDibits( dibit, d );
					
					int testCost = mConstellation.costTo( test ) + 
								   mConnectedNode.costTo( d );

					if( testCost < cost )
					{
						cost = testCost;
						cheapest = test;
					}
				}

				mConstellation = cheapest;
				
				mConnectedNode.correctTo( mConstellation.getRight() );
				
				mCorrect = true;
			}
		}

		/**
		 * Calculates the cost (hamming distance) of using the argument as the
		 * left side dibit for the current node, and recursively finding the
		 * cheapest corresponding right dibit.
		 * 
		 * @param leftTest
		 * @return
		 */
		public int costTo( Dibit leftTest )
		{
			if( isCurrentConnectionCorrect() )
			{
				Constellation c = Constellation.
						fromDibits( leftTest, mConstellation.getRight() );
				
				return mConstellation.costTo( c );
			}
			else
			{
				int cheapestCost = 100; //arbitrary
				
				for( Dibit d: Dibit.values() )
				{
					Constellation c = Constellation.fromDibits( leftTest, d );
					
					int cost = mConnectedNode.costTo( d ) + 
							   mConstellation.costTo( c );
					
					if( cost < cheapestCost )
					{
						cheapestCost = cost;
					}
				}
				
				return cheapestCost;
			}
		}

		/**
		 * Indicates if the immediate connection is correct
		 */
		public boolean isCurrentConnectionCorrect()
		{
			return ( mConnectedNode == null || 
					 mConstellation.getRight() == mConnectedNode.getLeft() );
		}
		
		/**
		 * Executes a recursive call to all nodes to the right, setting the 
		 * mCorrect flag on all nodes to true, if the node's connection to the
		 * right node is correct and all nodes to the right are correct.  Or,
		 * false if this node's connection, or any node's connection to the
		 * right is incorrect.
		 * 
		 * @return - true - all node connections to the right are correct
		 * 			 false - one or more nodes to the right are incorrect
		 */
		public boolean isCorrect()
		{
			if( mCorrect )
			{
				return mCorrect;
			}
			
			if( mConnectedNode == null )
			{
				mCorrect = true;
			}
			else
			{
				mCorrect = mConnectedNode.isCorrect() &&
					mConstellation.getRight() == mConnectedNode.getLeft();
			}

			return mCorrect;
		}
		
		public Dibit getLeft()
		{
			return mConstellation.getLeft();
		}
		
		public void setConstellation( Constellation constellation )
		{
			mConstellation = constellation;
			mCorrect = false;
		}
		
		public void connect( ConstellationNode node )
		{
			mConnectedNode = node;
		}
	}
	
	public enum Constellation
	{
		C0( Dibit.D1, Dibit.D1, 0 ),
		C1( Dibit.D0, Dibit.D2, 1 ),
		C2( Dibit.D0, Dibit.D0, 2 ),
		C3( Dibit.D1, Dibit.D3, 3 ),
		C4( Dibit.D2, Dibit.D3, 4 ),
		C5( Dibit.D3, Dibit.D0, 5 ),
		C6( Dibit.D3, Dibit.D2, 6 ),
		C7( Dibit.D2, Dibit.D1, 7 ),
		C8( Dibit.D3, Dibit.D3, 8 ),
		C9( Dibit.D2, Dibit.D0, 9 ),
		CA( Dibit.D2, Dibit.D2, 10 ),
		CB( Dibit.D3, Dibit.D1, 11 ),
		CC( Dibit.D0, Dibit.D1, 12 ),
		CD( Dibit.D1, Dibit.D2, 13 ),
		CE( Dibit.D1, Dibit.D0, 14 ),
		CF( Dibit.D0, Dibit.D3, 15 );
		
		private Dibit mLeftDibit;
		private Dibit mRightDibit;
		private int mValue;
		
		private Constellation( Dibit leftDibit, Dibit rightDibit, int value )
		{
			mLeftDibit = leftDibit;
			mRightDibit = rightDibit;
			mValue = value;
		}
		
		public Dibit getLeft()
		{
			return mLeftDibit;
		}
		
		public Dibit getRight()
		{
			return mRightDibit;
		}
		
		public int getValue()
		{
			return mValue;
		}
		
		public static Constellation fromValue( int value )
		{
			if( 0 <= value && value <= 15 )
			{
				return Constellation.values()[ value ];
			}
			
			return null;
		}

		/**
		 * Returns the cost or hamming distance to the other constellation using
		 * the values from the constellation costs table
		 */
		public int costTo( Constellation other )
		{
			return CONSTELLATION_COSTS[ getValue() ][ other.getValue() ];
		}
		
		public static Constellation fromDibits( Dibit left, Dibit right )
		{
			switch( left )
			{
				case D0:
					switch( right )
					{
						case D0:
							return C2;
						case D1:
							return CC;
						case D2:
							return C1;
						case D3:
							return CF;
						default:
					}
				case D1:
					switch( right )
					{
						case D0:
							return CE;
						case D1:
							return C0;
						case D2:
							return CD;
						case D3:
							return C3;
						default:
					}
				case D2:
					switch( right )
					{
						case D0:
							return C9;
						case D1:
							return C7;
						case D2:
							return CA;
						case D3:
							return C4;
						default:
					}
				case D3:
					switch( right )
					{
						case D0:
							return C5;
						case D1:
							return CB;
						case D2:
							return C6;
						case D3:
							return C8;
						default:
					}
				default:
			}

			/* we should never get to here */
			return C0;
		}
	}
	
	public enum Dibit
	{
		D0( false, false ), 
		D1( false, true ), 
		D2( true, false ), 
		D3( true, true );
		
		private boolean mFirstBit;
		private boolean mSecondBit;

		private Dibit( boolean firstBit, boolean secondBit )
		{
			mFirstBit = firstBit;
			mSecondBit = secondBit;
		}
		
		public boolean firstBit()
		{
			return mFirstBit;
		}
		
		public boolean secondBit()
		{
			return mSecondBit;
		}
	}
	
	/**
	 * Test harness
	 */
	public static void main( String[] args )
	{
		String original = "0010101110101010001011001100111011001000100000101101001000100000110010111010100100100111100100100010000000001110001110010010110000010110001000101101101001110010001001011100001101011110110110011011";
//		String original = "0010111110111110001000100010001011000000000000001101101010101001110011011001001000101100000011100010001000101100001101010010001000011010011100001101100111001110000101111101010001101010101001111110";
//		String original =   "0010001011000000110101111110001000100010001000100010001000100010001000100010001000100010001000100010001000100010001000100010001000100010001000100010001000100010111110111110110000110101001011110101";
		String witherrors = "0010001011000000110101100110001000100010001100100011001000100010001000100010001000100010001000100010001000100010001000100010001000100010001000100010001000100010111110111110110000110101001011110101";
		//Injected errors:                          xx                  x       x

		BitSetBuffer bufferOriginal = new BitSetBuffer( 196 );
		BitSetBuffer bufferWithErrors = new BitSetBuffer( 196 );
		
		try
		{
			for( int x = 0; x < 196; x++ )
			{
				if( witherrors.substring( x, x + 1 ).contentEquals( "0" ) )
				{
					bufferWithErrors.add( false );
				}
				else
				{
					bufferWithErrors.add( true );
				}

				if( original.substring( x, x + 1 ).contentEquals( "0" ) )
				{
					bufferOriginal.add( false );
				}
				else
				{
					bufferOriginal.add( true );
				}
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}

		System.out.println( "    ORIGINAL: " + original );
		System.out.println( " WITH ERRORS: " + witherrors );
		
		TrellisHalfRate t = new TrellisHalfRate();
		
		t.decode( bufferOriginal, 0, 196 );
		System.out.println( "ORIG DECODED: " + bufferOriginal.toString() );

		t.decode( bufferWithErrors, 0, 196 );
		System.out.println( "ERRS DECODED: " + bufferWithErrors.toString() );
		
	}
}

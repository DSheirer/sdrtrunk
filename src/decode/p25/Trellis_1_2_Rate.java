package decode.p25;

import java.util.ArrayList;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bits.BinaryMessage;
import dsp.symbol.Dibit;

public class Trellis_1_2_Rate
{
	public final static int MAX_ERROR_THRESHOLD = 7;
	
	private final static Logger mLog = 
			LoggerFactory.getLogger( Trellis_1_2_Rate.class );

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
		
	public Trellis_1_2_Rate()
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
	
	private String constellationsToString( String label )
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( label );

		for( ConstellationNode node: mConstellationNodes )
		{
			sb.append( "  " + node.getConstellation().name() );
		}
		
		return sb.toString();
	}

	private String inputsToString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "  INPUTS: " );

		for( ConstellationNode node: mConstellationNodes )
		{
			sb.append( "  " );
			sb.append( ( node.getConstellation().getInput().getBit1() ? "1" : "0" ) );
			sb.append( ( node.getConstellation().getInput().getBit2() ? "1" : "0" ) );
		}
		
		return sb.toString();
	}

	public boolean decode( BinaryMessage message, int start, int end )
	{
		/* load each of the nodes with de-interleaved constellations */
		for( int index = 0; index < 49; index++ )
		{
			Constellation c = getConstellation( message, start + index * 4 );

			mConstellationNodes.get( index ).setConstellation( c );
		}

		/* test to see if constellations are correct - otherwise correct them */
		ConstellationNode firstNode = mConstellationNodes.get( 0 );

		int errorCount = firstNode.getErrorCount();

		if( errorCount > 0 )
		{
			if( errorCount < MAX_ERROR_THRESHOLD )
			{
				/* repair the errors */
				firstNode.correctTo( Dibit.D00_PLUS_1 );
			}
			else
			{
				return false;
			}
		}
		
		/* clear constellations from original message */
		message.clear( start, end );

		/* replace with decoded values from the constellation nodes */
		for( int index = 0; index < 49; index++ )
		{
			ConstellationNode node = mConstellationNodes.get( index );
			
			Dibit input = node.getInputDibit();
			
			if( input.getBit1() )
			{
				message.set( start + ( index * 2 ) );
			}
			if( input.getBit2() )
			{
				message.set( start + ( index * 2 ) + 1 );
			}
		}
		
		return true;
	}
	
	private Constellation getConstellation( BinaryMessage message, int index )
	{
		int transmittedValue = 0;
		
		for( int x = 0; x < 4; x++ )
		{
			if( message.get( index + x ) )
			{
				transmittedValue += ( 1 << ( 3 - x ) );
			}
		}
		
		return Constellation.fromTransmittedValue( transmittedValue );
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
		
		public Constellation getConstellation()
		{
			return mConstellation;
		}
		
		public Dibit getInputDibit()
		{
			return mConstellation.getInput();
		}
		
		public boolean hasStateDibit( Dibit dibit )
		{
			return mConstellation.getState() == dibit;
		}
		
		/**
		 * Executes a correction down the line of connected nodes.  Only nodes
		 * with the mCorrect flag set to false will be corrected.
		 * 
		 * Note: Assumes that the starting node's value is 0.  Initiate the 
		 * corrective sequence by invoking this method with Dibit.D0 on the
		 * first node.
		 * 
		 * @param stateDibit to use for the left side.
		 */
		public void correctTo( Dibit stateDibit )
		{
			if( mCorrect && mConstellation.getState() == stateDibit )
			{
				return;
			}
			
			if( isCurrentConnectionCorrect() )
			{
				mConstellation = Constellation.fromStateAndInputDibits( stateDibit, 
						mConstellation.getInput() );

				mCorrect = true;
				
				if( mConnectedNode != null )
				{
					/* the next node's state was this node's input */
					mConnectedNode.correctTo( mConstellation.getInput() );
				}
			}
			else
			{
				Constellation cheapestConstellation = mConstellation;
				
				int cheapestCost = 100; //arbitrary
				
				for( Dibit testInput: Dibit.values() )
				{
					Constellation testConstellation = Constellation
							.fromStateAndInputDibits( stateDibit, testInput );
					
					int testCost = mConstellation.costTo( testConstellation ) + 
								   mConnectedNode.costTo( testInput );

					if( testCost < cheapestCost )
					{
						cheapestCost = testCost;
						cheapestConstellation = testConstellation;
					}
				}

				mConstellation = cheapestConstellation;
				
				mConnectedNode.correctTo( mConstellation.getInput() );
				
				mCorrect = true;
			}
		}

		/**
		 * Calculates the cost (hamming distance) of using the argument as the
		 * state dibit for the current node, and recursively finding the
		 * cheapest corresponding input dibit.
		 * 
		 * @param stateTest
		 * @return
		 */
		public int costTo( Dibit stateTest )
		{
			if( isCurrentConnectionCorrect() )
			{
				Constellation c = Constellation.fromStateAndInputDibits( 
						stateTest, mConstellation.getInput() );
				
				return mConstellation.costTo( c );
			}
			else
			{
				int cheapestCost = 100; //arbitrary
				
				for( Dibit inputTest: Dibit.values() )
				{
					Constellation constellationTest = 
						Constellation.fromStateAndInputDibits( stateTest, inputTest );
					
					int cost = mConnectedNode.costTo( inputTest ) + 
							   mConstellation.costTo( constellationTest );
					
					if( cost < cheapestCost )
					{
						cheapestCost = cost;
					}
				}
				
				return cheapestCost;
			}
		}

		/**
		 * Indicates if the immediate connection to the right is correct
		 */
		public boolean isCurrentConnectionCorrect()
		{
			return ( mConnectedNode == null || 
					 mConstellation.getInput() == mConnectedNode.getStateDibit() );
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
		public int getErrorCount()
		{
			if( mConnectedNode == null )
			{
				mCorrect = true;
				
				return 0;
			}
			
			mCorrect = mConstellation.getInput() == mConnectedNode.getStateDibit();

			return mConnectedNode.getErrorCount() + ( mCorrect ? 0 : 1 );
		}
		
		public Dibit getStateDibit()
		{
			return mConstellation.getState();
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
		/*  State (previous),  Input (next),      Transmit 1,        Transmit 2   */
		CB( Dibit.D01_PLUS_3,  Dibit.D01_PLUS_3,  Dibit.D00_PLUS_1,  Dibit.D00_PLUS_1,   0 ),
		CC( Dibit.D00_PLUS_1,  Dibit.D10_MINUS_1, Dibit.D00_PLUS_1,  Dibit.D01_PLUS_3,   1 ),
		C0( Dibit.D00_PLUS_1,  Dibit.D00_PLUS_1,  Dibit.D00_PLUS_1,  Dibit.D10_MINUS_1,  2 ),
		C7( Dibit.D01_PLUS_3,  Dibit.D11_MINUS_3, Dibit.D00_PLUS_1,  Dibit.D11_MINUS_3,  3 ),
		CE( Dibit.D10_MINUS_1, Dibit.D11_MINUS_3, Dibit.D01_PLUS_3,  Dibit.D00_PLUS_1,   4 ),
		C9( Dibit.D11_MINUS_3, Dibit.D00_PLUS_1,  Dibit.D01_PLUS_3,  Dibit.D01_PLUS_3,   5 ),
		C5( Dibit.D11_MINUS_3, Dibit.D10_MINUS_1, Dibit.D01_PLUS_3,  Dibit.D10_MINUS_1,  6 ),
		C2( Dibit.D10_MINUS_1, Dibit.D01_PLUS_3,  Dibit.D01_PLUS_3,  Dibit.D11_MINUS_3,  7 ),
		CA( Dibit.D11_MINUS_3, Dibit.D11_MINUS_3, Dibit.D10_MINUS_1, Dibit.D00_PLUS_1,   8 ),
		CD( Dibit.D10_MINUS_1, Dibit.D00_PLUS_1,  Dibit.D10_MINUS_1, Dibit.D01_PLUS_3,   9 ),
		C1( Dibit.D10_MINUS_1, Dibit.D10_MINUS_1, Dibit.D10_MINUS_1, Dibit.D10_MINUS_1, 10 ),
		C6( Dibit.D11_MINUS_3, Dibit.D01_PLUS_3,  Dibit.D10_MINUS_1, Dibit.D11_MINUS_3, 11 ),
		CF( Dibit.D00_PLUS_1,  Dibit.D01_PLUS_3,  Dibit.D11_MINUS_3, Dibit.D00_PLUS_1,  12 ),
		C8( Dibit.D01_PLUS_3,  Dibit.D10_MINUS_1, Dibit.D11_MINUS_3, Dibit.D01_PLUS_3,  13 ),
		C4( Dibit.D01_PLUS_3,  Dibit.D00_PLUS_1,  Dibit.D11_MINUS_3, Dibit.D10_MINUS_1, 14 ),
		C3( Dibit.D00_PLUS_1,  Dibit.D11_MINUS_3, Dibit.D11_MINUS_3, Dibit.D11_MINUS_3, 15 );
		
		private Dibit mStateDibit;
		private Dibit mInputDibit;
		private Dibit mTransmitDibit1;
		private Dibit mTransmitDibit2;
		private int mTransmittedValue;
		
		private Constellation( Dibit stateDibit, 
							   Dibit inputDibit, 
							   Dibit transmitDibit1,
							   Dibit transmitDibit2,
							   int transmittedValue )
		{
			mStateDibit = stateDibit;
			mInputDibit = inputDibit;
			mTransmitDibit1 = transmitDibit1;
			mTransmitDibit2 = transmitDibit2;
			mTransmittedValue = transmittedValue;
		}
		
		public Dibit getState()
		{
			return mStateDibit;
		}
		
		public Dibit getInput()
		{
			return mInputDibit;
		}
		
		public Dibit getTransmitDibit1()
		{
			return mTransmitDibit1;
		}
		
		public Dibit getTransmitDibit2()
		{
			return mTransmitDibit2;
		}
		
		public int getTransmittedValue()
		{
			return mTransmittedValue;
		}
		
		public static Constellation fromTransmittedValue( int value )
		{
			if( 0 <= value && value <= 15 )
			{
				return values()[ value ];
			}
			
			return null;
		}

		public static Constellation fromTransmittedDibits( Dibit left, Dibit right )
		{
			return fromTransmittedValue( left.getHighValue() + right.getLowValue() );
		}

		public static Constellation fromStateAndInputDibits( Dibit state, Dibit input )
		{
			switch( state )
			{
				case D00_PLUS_1:
					switch( input )
					{
						case D00_PLUS_1:
							return C0;
						case D01_PLUS_3:
							return CF;
						case D10_MINUS_1:
							return CC;
						case D11_MINUS_3:
							return C3;
					}
					break;
				case D01_PLUS_3:
					switch( input )
					{
						case D00_PLUS_1:
							return C4;
						case D01_PLUS_3:
							return CB;
						case D10_MINUS_1:
							return C8;
						case D11_MINUS_3:
							return C7;
					}
					break;
				case D10_MINUS_1:
					switch( input )
					{
						case D00_PLUS_1:
							return CD;
						case D01_PLUS_3:
							return C2;
						case D10_MINUS_1:
							return C1;
						case D11_MINUS_3:
							return CE;
					}
					break;
				case D11_MINUS_3:
					switch( input )
					{
						case D00_PLUS_1:
							return C9;
						case D01_PLUS_3:
							return C6;
						case D10_MINUS_1:
							return C5;
						case D11_MINUS_3:
							return CA;
					}
					break;
			}

			/* Should never get to here */
			return C0;
		}

		/**
		 * Returns the cost or hamming distance to the other constellation using
		 * the values from the constellation costs table
		 */
		public int costTo( Constellation other )
		{
			return CONSTELLATION_COSTS[ getTransmittedValue() ][ other.getTransmittedValue() ];
		}
		
	}
	
	/**
	 * Test harness
	 */
	public static void main( String[] args )
	{
		Random random = new Random();
		
		BinaryMessage buffer = new BinaryMessage( 196 );
		
		try
		{
			for( int x = 0; x < 196; x++ )
			{
				int number = random.nextInt( 2 );
				
				if( number == 1 )
				{
					buffer.add( false );
				}
				else
				{
					buffer.add( true );
				}
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}

		mLog.debug( "  BUFFER: " + buffer.toString() );
		
		Trellis_1_2_Rate t = new Trellis_1_2_Rate();
		
		t.decode( buffer, 0, 196 );
		mLog.debug( " DECODED: " + buffer.toString() );
		mLog.debug( "Finished!" );
	}
}

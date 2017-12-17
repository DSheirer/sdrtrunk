package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.bits.BinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.dsheirer.dsp.symbol.Dibit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Trellis_3_4_Rate
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( Trellis_3_4_Rate.class );

	/* Hamming distance (bit match count) between constellation pairs */
	private static final int[][] CONSTELLATION_METRICS = 
		{ { 4,3,3,2,3,2,2,1,3,2,2,1,2,1,1,0 },
		  { 3,4,2,3,2,3,1,2,2,3,1,2,1,2,0,1 },
		  { 3,2,4,3,2,1,3,2,2,1,3,2,1,0,2,1 },
		  { 2,3,3,4,1,2,2,3,1,2,2,3,0,1,1,2 },
		  { 3,2,2,1,4,3,3,2,2,1,1,0,3,2,2,1 },
		  { 2,3,1,2,3,4,2,3,1,2,0,1,2,3,1,2 },
		  { 2,1,3,2,3,2,4,3,1,0,2,1,2,1,3,2 },
		  { 1,2,2,3,2,3,3,4,0,1,1,2,1,2,2,3 },
		  { 3,2,2,1,2,1,1,0,4,3,3,2,3,2,2,1 },
		  { 2,3,1,2,1,2,0,1,3,4,2,3,2,3,1,2 },
		  { 2,1,3,2,1,0,2,1,3,2,4,3,2,1,3,2 },
		  { 1,2,2,3,0,1,1,2,2,3,3,4,1,2,2,3 },
		  { 2,1,1,0,3,2,2,1,3,2,2,1,4,3,3,2 },
		  { 1,2,0,1,2,3,1,2,2,3,1,2,3,4,2,3 },
		  { 1,0,2,1,2,1,3,2,2,1,3,2,3,2,4,3 },
		  { 0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4 } };
	
	/* Even valued state tribits can only produce even valued constellations
	 * and odd valued state tribits can only produce odd constellations. */
	public static Con[] EVEN_CONSTELLATIONS = new Con[] { Con.C0,Con.C2,Con.C4,
		Con.C6,Con.C8,Con.CA,Con.CC,Con.CE };
	public static Con[] ODD_CONSTELLATIONS = new Con[] { Con.C1,Con.C3,Con.C5,
		Con.C7,Con.C9,Con.CB,Con.CD,Con.CF };

	/* Constellation and state tribit lookup map to find the input tribit */
	public static HashMap<Con,Tribit[]> INPUT_FROM_CONSTELLATION_MAP;
	
	private ArrayList<Con> mTransmittedConstellations = new ArrayList<Con>();
	private ArrayList<Path> mSurvivorPaths = new ArrayList<Path>();
	private ArrayList<Path> mNewPaths = new ArrayList<Path>();
	private PathMetrics mPathMetrics = new PathMetrics();

	/**
	 * Implements the Viterbi algorithm to decode 3/4 rate trellis encoded 196-bit
	 * packet data messages.
	 */
	public Trellis_3_4_Rate()
	{
		createConstellationToTribitMap();
	}

	/**
	 * Creates a lookup map for state to input tribit values for a given
	 * constellation.  Input tribits are contained in the array using the state
	 * tribit's value as the lookup index.  Null values indicate illegal state
	 * and input combinations for the specified constellation.
	 */
	private void createConstellationToTribitMap()
	{
		INPUT_FROM_CONSTELLATION_MAP = new HashMap<Con,Tribit[]>();
		
		INPUT_FROM_CONSTELLATION_MAP.put( Con.CB, new Tribit[] { null, null, Tribit.T5, Tribit.T3, Tribit.T1, Tribit.T7, null, null } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.CC, new Tribit[] { Tribit.T3, Tribit.T1, null, null, null, null, Tribit.T7, Tribit.T5 } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.C0, new Tribit[] { Tribit.T0, Tribit.T6, null, null, null, null, Tribit.T4, Tribit.T2 } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.C7, new Tribit[] { null, null, Tribit.T6, Tribit.T4, Tribit.T2, Tribit.T0, null, null } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.CE, new Tribit[] { Tribit.T7, Tribit.T5, null, null, null, null, Tribit.T3, Tribit.T1 } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.C9, new Tribit[] { null, null, Tribit.T1, Tribit.T7, Tribit.T5, Tribit.T3, null, null } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.C5, new Tribit[] { null, null, Tribit.T2, Tribit.T0, Tribit.T6, Tribit.T4, null, null } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.C2, new Tribit[] { Tribit.T4, Tribit.T2, null, null, null, null, Tribit.T0, Tribit.T6 } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.CA, new Tribit[] { Tribit.T5, Tribit.T3, null, null, null, null, Tribit.T1, Tribit.T7 } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.CD, new Tribit[] { null, null, Tribit.T3, Tribit.T1, Tribit.T7, Tribit.T5, null, null } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.C1, new Tribit[] { null, null, Tribit.T0, Tribit.T6, Tribit.T4, Tribit.T2, null, null } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.C6, new Tribit[] { Tribit.T6, Tribit.T4, null, null, null, null, Tribit.T2, Tribit.T0 } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.CF, new Tribit[] { null, null, Tribit.T7, Tribit.T5, Tribit.T3, Tribit.T1, null, null } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.C8, new Tribit[] { Tribit.T1, Tribit.T7, null, null, null, null, Tribit.T5, Tribit.T3 } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.C4, new Tribit[] { Tribit.T2, Tribit.T0, null, null, null, null, Tribit.T6, Tribit.T4 } );
		INPUT_FROM_CONSTELLATION_MAP.put( Con.C3, new Tribit[] { null, null, Tribit.T4, Tribit.T2, Tribit.T0, Tribit.T6, null, null } );;
	}
	
	/**
	 * Member object cleanup prior to deleting
	 */
	public void dispose()
	{
		reset();
	}
	
	/**
	 * Decodes a 196-bit 3/4 rate convolutional encoded message located between
	 * start and end indexes and returns the decoded 144-bit message overlayed
	 * upon the original message with the remaining 52 bits cleared to zero.
	 * 
	 * @return - original message with decoded message bits..
	 */
	public boolean decode( BinaryMessage message, int start, int end )
	{
		reset();
		
		/* Load and decode each of the transmitted constellations */
		for( int index = 0; index < 49; index++ )
		{
			Con c = getConstellation( message, start + index * 4 );
			
			add( c );
		}

		/* The final decoded survivor path should be at path metrics tribit 0 */
		Path path = mPathMetrics.getPath( Tribit.T0 );

		/* We should have a path with 50 nodes, counting the starting 000 node
		 * and the flushing 000 node, otherwise there was an error */
		if( path != null && path.getNodes().size() == 50 )
		{
			/* Clear the original message bits */
			message.clear( start, end );

			List<Node> nodes = path.getNodes();
			
			/* Load each of the nodes' state tribit values into the original message */
			for( int x = 1; x < 50; x++ )
			{
				message.load( start + ( ( x - 1 ) * 3 ), 3, 
						nodes.get( x ).getState().getValue() );
			}
			
			return true;
		}

		return false;
	}
	
	/**
	 * Resets the decoder before decoding a new data packet.
	 */
	private void reset()
	{
		mTransmittedConstellations.clear();
		mNewPaths.clear();
		
		mPathMetrics.reset();
		
		mSurvivorPaths.clear();
		
		/* Tribit 000 is the only legal start point */
		mSurvivorPaths.add( new Path( new Node( 0, Tribit.T0, Con.C0 ) ) );
	}

	private void add( Con con )
	{
		mTransmittedConstellations.add( con );

		/* Add in any newly created survivor paths */
		mSurvivorPaths.addAll( mNewPaths );
		mNewPaths.clear();

		/* Set a culling threshold for any path metrics 3 below the best path */
		int survivorThreshold = mPathMetrics.getSurvivorThreshold();
		
		/* Reset path metrics */
		mPathMetrics.reset();
		
		/* Add constellation to each survivor path.  If a survivor path metric
		 * falls below the culling threshold, remove it */
		Iterator<Path> it = mSurvivorPaths.iterator();

		Path path;
		
		while( it.hasNext() )
		{
			path = it.next();

			if( path.isDead() || path.getPathMetric() < survivorThreshold )
			{
				it.remove();
			}
			else
			{
				path.add( con );
			}
		}
	}
	
	private Con getConstellation( BinaryMessage message, int index )
	{
		int value = message.getInt( index, index + 3 );
		return Con.fromTransmittedValue( value );
	}

	/**
	 * Path metrics maintains a map of the current best path metrics that end
	 * at each of the 8 valid tribit values.  Each new path is evaluated against 
	 * the current best path for retention (survivor) or disposal (dead).  A 
	 * best metric is maintained to evaluate the Walking Dead for disposal.  
	 * Walking dead is a path lagging the best path metric by more than 3.
	 */
	public class PathMetrics
	{
		private HashMap<Tribit,Path> mMetrics = new HashMap<Tribit,Path>();
		
		private int mBestMetric = 0;
		
		public PathMetrics()
		{
		}
		
		public int getSurvivorThreshold()
		{
			return mBestMetric - 3;
		}
		
		/**
		 * Removes all path metrics
		 */
		public void reset()
		{
			mMetrics.clear();
			mBestMetric = 0;
		}
		
		public int getMetric( Tribit tribit )
		{
			if( mMetrics.containsKey( tribit ) )
			{
				return mMetrics.get( tribit ).getPathMetric();
			}
			
			return 0;
		}
		
		public Path getPath( Tribit tribit )
		{
			return mMetrics.get( tribit );
		}
		
		/**
		 * Evaluates the path's metric against the current best path metric for
		 * the final state tribit of the path.  If the path's metric is higher
		 * than the current best path metric, the current path is marked for
		 * termination and the new path is promoted to best path.  If the new
		 * path's metric is less than the best path metric, then the new path 
		 * metric is marked for termination.  If the metrics are equal, the new
		 * path is allowed to survive.
		 * 
		 * @param path/metric to evaluate
		 */
		public void evalute( Path path )
		{
			Node lastNode = path.getLastNode();

			Path bestPath = mMetrics.get( lastNode.getState() );
			
			if( bestPath == null )
			{
				mMetrics.put( lastNode.getState(), path );
			}
			else
			{
				if( path.getPathMetric() < bestPath.getPathMetric() )
				{
					path.setSurvivor( false );
				}
				else if( path.getPathMetric() > bestPath.getPathMetric() )
				{
					mMetrics.put( lastNode.getState(), path );

					bestPath.setSurvivor( false );
				}
				
				/* Equal metric paths are allowed to survive */
			}
			
			/* Capture the best path metric */
			if( path.getPathMetric() > mBestMetric )
			{
				mBestMetric = path.getPathMetric();
			}
		}
	}
	
	/**
	 * Path is a sequence of nodes where the starting node always contains 
	 * tribit 0 and constellation 0.  Path maintains a running path metric 
	 * derived from the sum of each of the node's branch metrics.
	 */
	public class Path
	{
		private boolean mSurvivor = true;
		private int mPathMetric = 0;
		private ArrayList<Node> mNodes = new ArrayList<Node>();
		
		public Path( Node first )
		{
			mNodes.add( first );
			
			mPathMetric = first.getBranchMetric();
			
			mPathMetrics.evalute( this );
		}
		
		public Path( ArrayList<Node> nodes, int metric )
		{
			mNodes = nodes;
			mPathMetric = metric;
		}
		
		public List<Node> getNodes()
		{
			return mNodes;
		}
		
		public int getBitErrorCount()
		{
			return ( getLastNode().getIndex() * 4 ) - mPathMetric;
		}
		
		public void setSurvivor( boolean survivor )
		{
			mSurvivor = survivor;
		}
		
		public boolean isSurvivor()
		{
			return mSurvivor;
		}

		public boolean isDead()
		{
			return !mSurvivor;
		}
		
		@SuppressWarnings( "unchecked" )
		public Path copyOf()
		{
			return new Path( (ArrayList<Node>)mNodes.clone(), mPathMetric );
		}

		/**
		 * Adds the constellation and returns any new paths created as a result
		 * of the constellation being detected as an errant constellation.
		 */
		public void add( Con con )
		{
			Node current = getLastNode();
			
			Tribit input = INPUT_FROM_CONSTELLATION_MAP
					.get( con )[ current.getState().getValue() ];

			/* A non-null input tribit from the lookup table is valid, otherwise
			 * create 8 alternate branches from the original path to explore 
			 * where the error occurred. */
			if( input != null )
			{
				add( new Node( mNodes.size(), input, con ) );
			}
			else
			{
				/* Create new paths for each of the valid constellations that
				 * correspond to the parity of the current state tribit */
				Con[] constellations = current.getState().getType() == 
						Type.EVEN ? EVEN_CONSTELLATIONS : ODD_CONSTELLATIONS;
				
				for( int x = 0; x < 8; x++ )
				{
					/* Get the input tribit corresponding to the state tribit
					 * and constellation combination */
					Tribit tribit = INPUT_FROM_CONSTELLATION_MAP
						.get( constellations[ x ] )[ current.getState().getValue() ];

					Node candidate = new Node( mNodes.size(), tribit, constellations[ x ] );

					/* If the current path metric plus the candidate node's 
					 * branch metric is equal or better than the current best 
					 * metric for the given tribit, add it as a potential path, 
					 * otherwise discard it */
					if( this.getPathMetric() + candidate.getBranchMetric() >= 
								mPathMetrics.getMetric( tribit ) )
					{
						/* If this is the final constellation of the set, simply 
						 * add it to the existing path ... */
						if( x == 7 )
						{
							add( candidate );
						}
						/* ... otherwise create a copy of the current path */
						else
						{
							Path path = this.copyOf();
							
							path.add( candidate );
							
							mNewPaths.add( path );
						}
					}
				}
			}
		}
		
		public void add( Node node )
		{
			mNodes.add( node );
			
			mPathMetric += node.getBranchMetric();
			
			mPathMetrics.evalute( this );
		}
		
		public int getPathMetric()
		{
			return mPathMetric;
		}
		
		public Node getLastNode()
		{
			return mNodes.get( mNodes.size() - 1 );
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();

			sb.append( "PATH [" );
			sb.append( mNodes.size() );
			sb.append( "-" );
			sb.append( mPathMetric );
			sb.append( "] " );
			
			for( Node node: mNodes )
			{
				sb.append( node.getState().name() );
				sb.append( "/" );
				sb.append( node.getConstellation().name() );
				sb.append( " " );
			}
			
			sb.append( mSurvivor ? "SURVIVOR" : "DEAD" );
			
			return sb.toString();
		}
	}

	/**
	 * Trellis node containing the constellation used to produce the node and
	 * the input tribit for the constellation.
	 */
	public class Node
	{
		private int mIndex;
		private int mBranchMetric;
		private Tribit mTribit;
		private Con mConstellation;
		
		public Node( int index, Tribit tribit, Con constellation )
		{
			mIndex = index;
			mTribit = tribit;
			mConstellation = constellation;

			if( mIndex > 0 )
			{
				Con transmitted = mTransmittedConstellations.get( index - 1 );
				
				if( transmitted == null )
				{
					throw new IllegalArgumentException( "Cannot calculate "
						+ "metric - transmitted constellation symbol is null" );
				}
				
				mBranchMetric = transmitted.getMetricTo( constellation );
			}
		}
		
		public int getIndex()
		{
			return mIndex;
		}
		
		public Tribit getState()
		{
			return mTribit;
		}
		
		public Con getConstellation()
		{
			return mConstellation;
		}
		
		public int getBranchMetric()
		{
			return mBranchMetric;
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append( "Node " );
			sb.append( mIndex );
			sb.append( " Metric:"  );
			sb.append( mBranchMetric );
			sb.append( " Con:" );
			sb.append( mConstellation.name() );
			sb.append( " State:" );
			if( mTribit == null )
			{
				sb.append( "null" );
			}
			else
			{
				sb.append( mTribit.name() );
			}
			
			return sb.toString();
		}
	}
	
	/**
	 * Constellation Type.  Constellations are classified as even or add based
	 * on the the composition/comparison of the transmitted (representative) 
	 * dibits:
	 * 
	 * 	EVEN: - first dibit differs from second dibit in exactly one bit position
	 * 
	 *   ODD: - first dibit differs from second dibit in zero or two bit positions
	 *   	    following three patterns:
	 *          1) repeat (10 10), mirror (01 10), or invert(00 11) 
	 */
	public enum Type { EVEN, ODD };
	
	/**
	 * Constellations, ordered by transmitted value.  Transmitted value is the
	 * value of the dibit pair that represents the constellation when transmitted.
	 */
	public enum Con
	{
		CB( 0 ),
		CC( 1 ),
		C0( 2 ),
		C7( 3 ),
		CE( 4 ),
		C9( 5 ),
		C5( 6 ),
		C2( 7 ),
		CA( 8 ),
		CD( 9 ),
		C1( 10 ),
		C6( 11 ),
		CF( 12 ),
		C8( 13 ),
		C4( 14 ),
		C3( 15 );
		
		private int mTransmittedValue;
		
		private Con( int transmittedValue ) 
		{
			mTransmittedValue = transmittedValue;
		}
		
		public int getTransmittedValue()
		{
			return mTransmittedValue;
		}
		
		public static Con fromTransmittedValue( int value )
		{
			if( 0 <= value && value <= 15 )
			{
				return values()[ value ];
			}
			
			return null;
		}

		public static Con fromTransmittedDibits( Dibit left, Dibit right )
		{
			return fromTransmittedValue( left.getHighValue() + right.getLowValue() );
		}

		/**
		 * Returns the metric or hamming distance to the other constellation 
		 * using the values from the constellation costs table.  A perfect match
		 * has a metric of 4 and a complete miss has a value of 0.
		 */
		public int getMetricTo( Con other )
		{
			return CONSTELLATION_METRICS[ getTransmittedValue() ][ other.getTransmittedValue() ];
		}
		
	}
	
	/**
	 * Tribit.  Used to define both the state and the input bit sequences that
	 * produce the transmitted constellation bit sequences.  Each tribit is 
	 * either even or odd valued producing a direct correlation to the types
	 * of valid constellations containing the tribit as the state tribit.
	 */
	public enum Tribit
	{
		T0( 0, Type.EVEN ),
		T1( 1, Type.EVEN ),
		T2( 2, Type.ODD ),
		T3( 3, Type.ODD ),
		T4( 4, Type.ODD ),
		T5( 5, Type.ODD ),
		T6( 6, Type.EVEN ),
		T7( 7, Type.EVEN );
		
		private int mValue;
		private Type mType;
		
		private Tribit( int value, Type type )
		{
			mValue = value;
			mType = type;
		}

		public Type getType()
		{
			return mType;
		}
		
		public int getValue()
		{
			return mValue;
		}
	}
	
	/**
	 * Test harness
	 */
	public static void main( String[] args )
	{
		String raw = "12:58:51.635 DEBUG dsp.fsk.P25MessageFramer - AFTER  DEINTERLEAVE: 00100110000011001010010101111101000101010110100111111100101011000111011011000110000000000000000000000111111101011000001000001111000110000000000010101011110110010000000001011011000000010100011110110001000010100100011111000000000100000000000000000000010000000000001100000000000000011010101010101010101010100010001010000110010111111101101000001010000010100000101000001010000010100000101000001010000010100000101000001010000010100000101000001010000010100000010111001001000011100101011010101010101001010011";
		
		BinaryMessage message = BinaryMessage.load( raw );
		
		mLog.debug( "MSG: " + message.toString() );
		
		Trellis_3_4_Rate t = new Trellis_3_4_Rate();
		t.decode( message, 0, 196 );
		
		mLog.debug( "DEC: " + message.toString() );
		
		mLog.debug( "Finished!" );
	}

	
}

package edac;

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
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
 *     
 *     --------------------------------------------------------------------
 *     Based on Ed Fuentetaja's DSD RS decoder, May 2014, at:
 *     https://github.com/szechyjs/dsd/blob/master/ReedSolomon.hpp
 *     --------------------------------------------------------------------
 *     Adapted from Mr. Simon Rockliff's version at: http://www.eccpage.com/rs.c
 *     
 *     Simon Rockliff, University of Adelaide   21/9/89
 *     26/6/91 Slight modifications to remove a compiler dependent bug which 
 *     hadn't previously surfaced. A few extra comments added for clarity.
 *     Appears to all work fine, ready for posting to net!
 *     
 *     Notice
 *     --------
 *     This program may be freely modified and/or given to whoever wants it.
 *     A condition of such distribution is that the author's contribution be
 *     acknowledged by his name being left in the comments heading the program,
 *     however no responsibility is accepted for any financial or other loss which
 *     may result from some unforseen errors or malfunctioning of the program
 *     during use.
 *     Simon Rockliff, 26th June 1991
 ******************************************************************************/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Berlekemp Massey decoder for 63-bit primitive RS/BCH block codes
 */
public class BerlekempMassey_63
{
	public final static Logger mLog = LoggerFactory.getLogger( BerlekempMassey_63.class );

	/* Golay field size GF( 2 ** MM ) */
	private static final int MM = 6;

	/* Codeword Length: NN = 2 ** MM - 1 */
	private static final int NN = 63;
	
	/* Hamming distance between codewords: NN - KK + 1 = 2 * TT + 1 */
	private static int KK;

	/* Maximum number of errors that can be corrected */
	int TT;

	int[] alpha_to;
	int[] index_of;
	int[] gg;
	
	public BerlekempMassey_63( int tt )
    {
		TT = tt;
		KK = NN - 2 * TT;
		
        alpha_to = new int[ NN + 1 ];
        index_of = new int[ NN + 1 ];
        gg       = new int[ NN - KK + 1 ];

        /* P25 generator polynomial */
        int generator_polinomial[] = { 1, 1, 0, 0, 0, 0, 1 };

        generate_gf( generator_polinomial );

        gen_poly();
    }

	/**
	 * Generates the Golay Field.
	 * 
	 * Generates a GF( 2**mm ) from the irreducible polynomial 
	 * p(X) in pp[0]..pp[mm]
	 * 
	 * Lookup tables:  
	 * 		index_of[] = polynomial form   
	 * 		alpha_to[] = contains j=alpha**i;
	 * 
	 * Polynomial form -> Index form  index_of[j=alpha**i] = i
	 * 
	 * alpha_to = 2 is the primitive element of GF( 2**mm )
	 * 
	 * @param generator_polinomial
	 */
	private void generate_gf( int[] generator_polinomial )
	{
		int i;
		int mask = 1;

        alpha_to[ MM ] = 0;
        
        for( i = 0; i < MM; i++ ) 
        {
            alpha_to[ i ] = mask;
            index_of[ alpha_to[ i ] ] = i;
            
            if( generator_polinomial[ i ] != 0 )
            {
                alpha_to[ MM ] ^= mask;
            }
            
            mask <<= 1;
        }
        
        index_of[ alpha_to[ MM ] ] = MM;
        
        mask >>= 1;
            
        for ( i = MM + 1; i < NN; i++ ) 
        {
            if( alpha_to[ i - 1 ] >= mask )
            {
                alpha_to[ i ] = alpha_to[ MM ] ^ ( ( alpha_to[ i - 1 ] ^ mask ) << 1 );
            }
            else
            {
                alpha_to[ i ] = alpha_to[ i - 1 ] << 1;
            }
            
            index_of[ alpha_to[ i ] ] = i;
        }
        
        index_of[ 0 ] = -1;
    }

    /**
     * Generates the polynomial for a TT-error correction code.
     * 
     * Length NN = ( 2 ** MM -1 ) Reed Solomon code from the product of 
     * (X+alpha**i), i=1..2*tt 
     */
	private void gen_poly()
    {
        int i, j;

        gg[ 0 ] = 2; /* primitive element alpha = 2  for GF(2**mm)  */
        gg[ 1 ] = 1; /* g(x) = (X+alpha) initially */
        for( i = 2; i <= NN - KK; i++ ) 
        {
            gg[ i ] = 1;
            
            for( j = i - 1; j > 0; j-- )
            {
                if( gg[ j ] != 0 )
                {
                    gg[ j ] = gg[ j - 1 ] ^ alpha_to[ ( index_of[ gg[ j ] ] + i ) % NN ];
                }
                else
                {
                    gg[ j ] = gg[ j - 1 ];
                }
            }
            
            /* gg[0] can never be zero */
            gg[ 0 ] = alpha_to[ ( index_of[ gg[ 0 ] ] + i ) % NN ];
        }
        
        /* convert gg[] to index form for quicker encoding */
        for( i = 0; i <= NN - KK; i++ )
        {
            gg[ i ] = index_of[ gg[ i ] ];
        }
    }

	/**
	 * Decodes 
	 * @param input
	 * @param recd
	 * @return
	 */
    /* assume we have received bits grouped into mm-bit symbols in recd[i],
    i=0..(nn-1),  and recd[i] is polynomial form.
    We first compute the 2*tt syndromes by substituting alpha**i into rec(X) and
    evaluating, storing the syndromes in s[i], i=1..2tt (leave s[0] zero) .
    Then we use the Berlekamp iteration to find the error location polynomial
    elp[i].   If the degree of the elp is >tt, we cannot correct all the errors
    and hence just put out the information symbols uncorrected. If the degree of
    elp is <=tt, we substitute alpha**i , i=1..n into the elp to get the roots,
    hence the inverse roots, the error location numbers. If the number of errors
    located does not equal the degree of the elp, we have more than tt errors
    and cannot correct them.  Otherwise, we then solve for the error value at
    the error location and correct the error.  The procedure is that found in
    Lin and Costello. For the cases where the number of errors is known to be too
    large to correct, the information symbols as received are output (the
    advantage of systematic encoding is that hopefully some of the information
    symbols will be okay and that if we are in luck, the errors are in the
    parity part of the transmitted codeword).  Of course, these insoluble cases
    can be returned as error flags to the calling routine if desired.   */
    public boolean decode( final int[] input, int[] recd ) //input, output
    {
    	int u, q;
        int[][] elp = new int[ NN - KK + 2 ][ NN - KK ];
        int[] d = new int[ NN - KK + 2 ]; 
        int[] l = new int[ NN - KK + 2 ]; 
        int[] u_lu = new int[ NN - KK + 2 ];
        int[] s = new int[ NN - KK + 1 ];
        int count = 0; 
        boolean syn_error = false;
        int[] root = new int[ TT ];
        int[] loc = new int[ TT ];
        int[] z = new int[ TT + 1 ];
        int[] err = new int[ NN ];
        int[] reg = new int[ TT + 1 ];

        boolean irrecoverable_error = false;

    	/* put recd[i] into index form (ie as powers of alpha) */
        for( int i = 0; i < NN; i++ )
        {
        	recd[ i ] = index_of[ input[ i ] ]; 
        }
        
        /* first form the syndromes */
        for( int i = 1; i <= NN - KK; i++ ) 
        {
            s[ i ] = 0;
            
            for( int j = 0; j < NN; j++ )
            {
                if( recd[ j ] != -1 )
                {
                	/* recd[j] in index form */
                	s[ i ] ^= alpha_to[ ( recd[ j ] + i * j ) % NN ];
                }
            }
            
            /* convert syndrome from polynomial form to index form  */
            if( s[ i ] != 0 )
            {
            	/* set flag if non-zero syndrome => error */            	
                syn_error = true; 
            }
            
            s[ i ] = index_of[ s[ i ] ];
        }
        
        if( syn_error ) /* if errors, try and correct */
        {
            /* compute the error location polynomial via the Berlekamp iterative algorithm,
             following the terminology of Lin and Costello :   d[u] is the 'mu'th
             discrepancy, where u='mu'+1 and 'mu' (the Greek letter!) is the step number
             ranging from -1 to 2*tt (see L&C),  l[u] is the
             degree of the elp at that step, and u_l[u] is the difference between the
             step number and the degree of the elp.
             */
        	
            /* initialise table entries */
            d[ 0 ] = 0; /* index form */
            d[ 1 ] = s[ 1 ]; /* index form */
            elp[ 0 ][ 0 ] = 0; /* index form */
            elp[ 1 ][ 0 ] = 1; /* polynomial form */
            
            for( int i = 1; i < NN - KK; i++ ) 
            {
                elp[ 0 ][ i ] = -1; /* index form */
                elp[ 1 ][ i ] = 0; /* polynomial form */
            }
            
            l[ 0 ] = 0;
            l[ 1 ] = 0;
            u_lu[ 0 ] = -1;
            u_lu[ 1 ] = 0;
            u = 0;

            do 
            {
                u++;
                
                if( d[ u ] == -1 ) 
                {
                    l[ u + 1 ] = l[ u ];
                    
                    for( int i = 0; i <= l[ u ]; i++ ) 
                    {
                        elp[ u + 1 ][ i ] = elp[ u ][ i ];
                        elp[ u ][ i ] = index_of[ elp[ u ][ i ] ];
                    }
                } 
                else
                /* search for words with greatest u_lu[q] for which d[q]!=0 */
                {
                    q = u - 1;
                    
                    while( ( d[ q ] == -1 ) && ( q > 0 ) )
                    {
                        q--;
                    }
                    
                    /* have found first non-zero d[q]  */
                    if( q > 0 ) 
                    {
                    	int j = q;
                    	
                        do 
                        {
                            j--;
                            
                            if( ( d[ j ] != -1 ) && ( u_lu[ q ] < u_lu[ j ] ) )
                            {
                                q = j;
                            }
                        } 
                        while( j > 0 );
                    };

                    /* have now found q such that d[u]!=0 and u_lu[q] is maximum */
                    /* store degree of new elp polynomial */
                    if( l[ u ] > l[ q ] + u - q )
                    {
                        l[ u + 1 ] = l[ u ];
                    }
                    else
                    {
                        l[ u + 1 ] = l[ q ] + u - q;
                    }

                    /* form new elp(x) */
                    for( int i = 0; i < NN - KK; i++ )
                    {
                        elp[ u + 1 ][ i ] = 0;
                    }                    	
                    
                    for( int i = 0; i <= l[q]; i++ )
                    {
                        if( elp[ q ][ i ] != -1 )
                        {
                            elp[ u + 1 ][ i + u - q ] = 
                        		alpha_to[ ( d[ u ] + NN - d[ q ]
                                    + elp[ q ][ i ]) % NN ];
                        }
                    }
                    for( int i = 0; i <= l[u]; i++ ) 
                    {
                        elp[ u + 1 ][ i ] ^= elp[ u ][ i ];
                        elp[ u ][ i ] = index_of[ elp[ u ][ i ] ]; /*convert old elp value to index*/
                    }
                }
                
                u_lu[ u + 1 ] = u - l[ u + 1 ];

                /* form (u+1)th discrepancy */
                if( u < NN - KK ) /* no discrepancy computed on last iteration */
                {
                    if ( s[ u + 1 ] != -1 )
                    {
                        d[ u + 1 ] = alpha_to[ s[ u + 1 ] ];
                    }
                    else
                    {
                        d[ u + 1 ] = 0;
                    }
                    for( int i = 1; i <= l[ u + 1 ]; i++ )
                    {
                        if( ( s[ u + 1 - i ] != -1 ) && ( elp[ u + 1 ][ i]  != 0 ) )
                        {
                            d[ u + 1 ] ^= alpha_to[ ( s[ u + 1 - i ]
                                    + index_of[ elp[ u + 1 ][ i ] ] ) % NN ];
                        }
                    }
                    
                    d[ u + 1 ] = index_of[ d[ u + 1 ] ]; /* put d[u+1] into index form */
                }
            } 
            while( ( u < NN - KK ) && ( l[ u + 1 ] <= TT) );

            u++;
            
            if( l[ u ] <= TT ) /* can correct error */
            {
                /* put elp into index form */
            	for( int i = 0; i <= l[u]; i++ )
            	{
                	elp[ u ][ i ] = index_of[ elp[ u ][ i ] ];
            	}

                /* find roots of the error location polynomial */
            	for( int i = 1; i <= l[u]; i++ )
            	{
                    reg[ i ] = elp[ u ][ i ];
            	}
            	
                count = 0;
                
                for( int i = 1; i <= NN; i++ ) 
                {
                    q = 1;
                    
                    for( int j = 1; j <= l[u]; j++ )
                    {
                        if( reg[ j ] != -1 ) 
                        {
                            reg[ j ] = ( reg[ j ] + j ) % NN;
                            q ^= alpha_to[ reg[ j ] ];
                        };
                    }
                    
                    if( q == 0 ) /* store root and error location number indices */
                    {
                        root[ count ] = i;
                        loc[ count ] = NN - i;
                        count++;
                    };
                };

                if( count == l[ u ] ) /* no. roots = degree of elp hence <= tt errors */
                {
                    /* form polynomial z(x) */
                	for( int i = 1; i <= l[ u ]; i++ ) /* Z[0] = 1 always - do not need */
                    {
                        if( ( s[ i ] != -1 ) && ( elp[ u ][ i ] != -1 ) )
                        {
                            z[ i ] = alpha_to[ s[ i ] ] ^ alpha_to[ elp[ u ][ i ] ];
                        }
                        else if( ( s[ i ] != -1 ) && ( elp[ u ][ i ] == -1 ) )
                        {
                            z[ i ] = alpha_to[ s[ i ] ];
                        }
                        else if( ( s[ i ] == -1 ) && ( elp[ u ][ i ] != -1 ) )
                        {
                            z[ i ] = alpha_to[ elp[ u ][ i ] ];
                        }
                        else
                        {
                            z[ i ] = 0;
                        }
                        
                        for( int j = 1; j < i; j++ )
                        {
                            if( ( s[ j ] != -1 ) && ( elp[ u ][ i - j ] != -1 ) )
                            {
                                z[ i ] ^= alpha_to[ ( elp[ u ][ i - j ] + s[ j ] ) % NN ];
                            }
                        }
                        
                        z[ i ] = index_of[ z[ i ] ]; /* put into index form */
                    };

                    /* evaluate errors at locations given by error location numbers loc[i] */
                    for( int i = 0; i < NN; i++ ) 
                    {
                        err[ i ] = 0;
                        
                        if( recd[ i ] != -1 ) /* convert recd[] to polynomial form */
                        {
                            recd[ i ] = alpha_to[ recd[ i ] ];
                        }
                        else
                        {
                            recd[ i ] = 0;
                        }
                    }
                    
                    for( int i = 0; i < l[ u ]; i++ ) /* compute numerator of error term first */
                    {
                        err[ loc[ i ] ] = 1; /* accounts for z[0] */
                        
                        for( int j = 1; j <= l[ u ]; j++ )
                        {
                            if( z[ j ] != -1 )
                            {
                                err[ loc[ i ] ] ^= alpha_to[ ( z[ j ] + j * root[ i ] ) % NN ];
                            }
                        }
                        
                        if( err[ loc[ i ] ] != 0 ) 
                        {
                            err[ loc[ i ] ] = index_of[ err[ loc[ i ] ] ];
                            
                            q = 0; /* form denominator of error term */
                            
                            for (int j = 0; j < l[u]; j++)
                            {
                                if (j != i)
                                {
                                    q += index_of[1 ^ alpha_to[(loc[j] + root[i]) % NN]];
                                }
                            }
                            
                            q = q % NN;
                            err[loc[i]] = alpha_to[(err[loc[i]] - q + NN) % NN];
                            recd[loc[i]] ^= err[loc[i]]; /*recd[i] must be in polynomial form */
                        }
                    }
                } 
                else 
                {
                    /* no. roots != degree of elp => >tt errors and cannot solve */
                    irrecoverable_error = true;
                }

            } 
            else 
            {
                /* elp has degree >tt hence cannot solve */
                irrecoverable_error = true;
            }
        } 
        else 
        {
            /* no non-zero syndromes => no errors: output received codeword */
        	for (int i = 0; i < NN; i++)
        	{
                if (recd[i] != -1) /* convert recd[] to polynomial form */
                {
                    recd[i] = alpha_to[recd[i]];
                }
                else
                {
                    recd[i] = 0;
                }
        	}
        }

        if( irrecoverable_error ) 
        {
        	for (int i = 0; i < NN; i++) /* could return error flag if desired */
        	{
                if (recd[i] != -1) /* convert recd[] to polynomial form */
                {
                    recd[i] = alpha_to[recd[i]];
                }
                else
                {
                    recd[i] = 0; /* just output received codeword as is */
                }
        	}
        }

        return irrecoverable_error;
    }
}

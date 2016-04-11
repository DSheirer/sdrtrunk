
/**************************************************************************
 * Parks-McClellan algorithm for FIR filter design (C version)
 *-------------------------------------------------
 *  Copyright (C) 1995  Jake Janovetz (janovetz@coewl.cen.uiuc.edu)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *************************************************************************/
package filter;

public class Remez 
{


  /* Converted to Java by Iain A Robin, June 1998 */
	
  /* changes by Hanns Holger Rutz, Jan 2010:
   * 	needed to make method 'remez' public
   */


  public static final int BANDPASS       = 1;
  public static final int DIFFERENTIATOR = 2;
  public static final int HILBERT        = 3;

  public static final int NEGATIVE       = 0;
  public static final int POSITIVE       = 1;

  public static final int GRIDDENSITY    = 16;
  public static final int MAXITERATIONS  = 40;

  double pi = Math.PI;
  double pi2 = 2.0*pi;

  int numtaps;

  public Remez() 
  {
  }

  /*******************
   * createDenseGrid
   *=================
   * Creates the dense grid of frequencies from the specified bands.
   * Also creates the Desired Frequency Response function (d[]) and
   * the Weight function (w[]) on that dense grid
   *
   *
   * INPUT:
   * ------
   * int      r        - 1/2 the number of filter coefficients
   * int      numtaps  - Number of taps in the resulting filter
   * int      numband  - Number of bands in user specification
   * double[] bands    - User-specified band edges [2*numband]
   * double[] des      - Desired response per band [numband]
   * double[] weight   - Weight per band [numband]
   * int      symmetry - Symmetry of filter - used for grid check
   *
   * OUTPUT:
   * -------
   * int    gridSize   - Number of elements in the dense frequency grid
   * double[] grid     - Frequencies (0 to 0.5) on the dense grid [gridSize]
   * double[] d        - Desired response on the dense grid [gridSize]
   * double[] w        - Weight function on the dense grid [gridSize]
   *******************/

  void createDenseGrid(int r, int numtaps, int numband, double bands[],
                       double des[], double weight[], int gridSize,
                       double grid[], double d[], double w[],
                       int symmetry) {
    double lowf, highf;
    double delf = 0.5/(GRIDDENSITY*r);

    // For differentiator, hilbert,
    //  symmetry is odd and grid[0] = max(delf, band[0])
    if ((symmetry == NEGATIVE) && (delf > bands[0]))
      bands[0] = delf;

    int j=0;
    int k;
    for (int band=0; band < numband; band++) {
      grid[j] = bands[2*band];
      lowf = bands[2*band];
      highf = bands[2*band + 1];
      k = (int)Math.round((highf - lowf)/delf);
      for (int i=0; i<k; i++) {
        d[j] = des[band];
        w[j] = weight[band];
        grid[j] = lowf;
        lowf += delf;
        j++;
      }
      grid[j-1] = highf;
    }

    // Similar to above, if odd symmetry, last grid point can't be .5
    // - but, if there are even taps, leave the last grid point at .5
    if ((symmetry == NEGATIVE) &&
        (grid[gridSize-1] > (0.5 - delf)) && ((numtaps % 2) != 0)) {
          grid[gridSize-1] = 0.5-delf;
    }
  }

  /********************
   * initialGuess
   *==============
   * Places Extremal Frequencies evenly throughout the dense grid.
   *
   *
   * INPUT:
   * ------
   * int r        - 1/2 the number of filter coefficients
   * int gridSize - Number of elements in the dense frequency grid
   *
   * OUTPUT:
   * -------
   * int ext[]    - Extremal indexes to dense frequency grid [r+1]
   ********************/

  void initialGuess(int r, int ext[], int gridSize) {
    for (int i=0; i<=r; i++) ext[i] = i * (gridSize-1) / r;
  }


  /***********************
   * calcParms
   *===========
   *
   *
   * INPUT:
   * ------
   * int      r    - 1/2 the number of filter coefficients
   * int[]    ext  - Extremal indexes to dense frequency grid [r+1]
   * double[] grid - Frequencies (0 to 0.5) on the dense grid [gridSize]
   * double[] d    - Desired response on the dense grid [gridSize]
   * double[] w    - Weight function on the dense grid [gridSize]
   *
   * OUTPUT:
   * -------
   * double[] ad   - 'b' in Oppenheim & Schafer [r+1]
   * double[] x    - [r+1]
   * double[] y    - 'C' in Oppenheim & Schafer [r+1]
   ***********************/

  void calcParms(int r, int ext[], double grid[], double d[], double w[],
                  double ad[], double x[], double y[]) {
    double sign, xi, delta, denom, numer;

    // Find x[]
    for (int i=0; i<=r; i++) x[i] = Math.cos(pi2 * grid[ext[i]]);

    // Calculate ad[]  - Oppenheim & Schafer eq 7.132
    int ld = (r-1)/15 + 1;         // Skips around to avoid round errors
    for (int i=0; i<=r; i++) {
      denom = 1.0;
      xi = x[i];
      for (int j=0; j<ld; j++) {
        for (int k=j; k<=r; k+=ld)
          if (k != i) denom *= 2.0*(xi - x[k]);
       }
       if (Math.abs(denom)<0.00001) denom = 0.00001;
       ad[i] = 1.0/denom;
    }

    // Calculate delta  - Oppenheim & Schafer eq 7.131
    numer = denom = 0;
    sign = 1;
    for (int i=0; i<=r; i++) {
      numer += ad[i] * d[ext[i]];
      denom += sign * ad[i]/w[ext[i]];
      sign = -sign;
    }
    delta = numer/denom;
    sign = 1;

    // Calculate y[]  - Oppenheim & Schafer eq 7.133b
    for (int i=0; i<=r; i++) {
      y[i] = d[ext[i]] - sign * delta/w[ext[i]];
      sign = -sign;
    }
  }


  /*********************
   * computeA
   *==========
   * Using values calculated in CalcParms, ComputeA calculates the
   * actual filter response at a given frequency (freq).  Uses
   * eq 7.133a from Oppenheim & Schafer.
   *
   *
   * INPUT:
   * ------
   * double   freq - Frequency (0 to 0.5) at which to calculate A
   * int      r    - 1/2 the number of filter coefficients
   * double[] ad   - 'b' in Oppenheim & Schafer [r+1]
   * double[] x    - [r+1]
   * double[] y    - 'C' in Oppenheim & Schafer [r+1]
   *
   * OUTPUT:
   * -------
   * Returns double value of A[freq]
   *********************/

  double computeA(double freq, int r, double ad[], double x[], double y[]) {

    double c;
    double numer = 0;
    double denom = 0;
    double xc = Math.cos(pi2 * freq);
    for (int i=0; i<=r; i++) {
      c = xc - x[i];
      if (Math.abs(c) < 1.0e-7) {
        numer = y[i];
        denom = 1;
        break;
      }
      c = ad[i]/c;
      denom += c;
      numer += c*y[i];
    }
    return numer/denom;
  }


  /************************
   * calcError
   *===========
   * Calculates the Error function from the desired frequency response
   * on the dense grid (d[]), the weight function on the dense grid (w[]),
   * and the present response calculation (A[])
   *
   *
   * INPUT:
   * ------
   * int      r        - 1/2 the number of filter coefficients
   * double[] ad       - [r+1]
   * double[] x        - [r+1]
   * double[] y        - [r+1]
   * int      gridSize - Number of elements in the dense frequency grid
   * double[] grid     - Frequencies on the dense grid [gridSize]
   * double[] d        - Desired response on the dense grid [gridSize]
   * double[] w        - Weight function on the desnse grid [gridSize]
   *
   * OUTPUT:
   * -------
   * double[] e        - Error function on dense grid [gridSize]
   ************************/

  void calcError(int r, double ad[], double x[], double y[],
                   int gridSize, double grid[],
                   double d[], double w[], double e[]) {
    double A;
    for (int i=0; i<gridSize; i++) {
      A = computeA(grid[i], r, ad, x, y);
      e[i] = w[i] * (d[i] - A);
    }
  }

  /************************
   * search
   *========
   * Searches for the maxima/minima of the error curve.  If more than
   * r+1 extrema are found, it uses the following heuristic (thanks
   * Chris Hanson):
   * 1) Adjacent non-alternating extrema deleted first.
   * 2) If there are more than one excess extrema, delete the
   *    one with the smallest error.  This will create a non-alternation
   *    condition that is fixed by 1).
   * 3) If there is exactly one excess extremum, delete the smaller
   *    of the first/last extremum
   *
   *
   * INPUT:
   * ------
   * int      r        - 1/2 the number of filter coefficients
   * int[]    ext      - Indexes to grid[] of extremal frequencies [r+1]
   * int      gridSize - Number of elements in the dense frequency grid
   * double[] e        - Array of error values.  [gridSize]
   *
   * OUTPUT:
   * -------
   * int[]    ext      - New indexes to extremal frequencies [r+1]
   ************************/

  void search(int r, int ext[], int gridSize, double e[]) {
    boolean up, alt;
    int[] foundExt = new int[gridSize];  /* Array of found extremals */
    int k = 0;
    // Check for extremum at 0.
    if (((e[0]>0.0) && (e[0]>e[1])) || ((e[0]<0.0) && (e[0]<e[1])))
      foundExt[k++] = 0;

    // Check for extrema inside dense grid
    for (int i=1; i<gridSize-1; i++) {
      if (((e[i]>=e[i-1]) && (e[i]>e[i+1]) && (e[i]>0.0)) ||
          ((e[i]<=e[i-1]) && (e[i]<e[i+1]) && (e[i]<0.0)))
            foundExt[k++] = i;
    }

    // Check for extremum at 0.5
    int j = gridSize-1;
    if (((e[j]>0.0) && (e[j]>e[j-1])) || ((e[j]<0.0) && (e[j]<e[j-1])))
      foundExt[k++] = j;

    // Remove extra extremals
    int extra = k - (r+1);
    int l;
    while (extra > 0) {
      up = e[foundExt[0]] > 0.0;
      // up = true -->  first one is a maximum
      // up = false --> first one is a minimum
      l=0;
      alt = true;
      for (j=1; j<k; j++) {
        if (Math.abs(e[foundExt[j]]) < Math.abs(e[foundExt[l]]))
          l = j;               // new smallest error.
        if (up && (e[foundExt[j]] < 0.0))
            up = false;             // switch to a minima
        else if (!up && (e[foundExt[j]] > 0.0))
            up = true;             // switch to a maxima
        else {
          alt = false;
          break;              // Ooops, found two non-alternating
        }                     // extrema.  Delete smallest of them
      }  // if the loop finishes, all extrema are alternating

      // If there's only one extremal and all are alternating,
      // delete the smallest of the first/last extremals.
      if (alt && (extra == 1)) {
        if (Math.abs(e[foundExt[k-1]]) < Math.abs(e[foundExt[0]]))
          l = foundExt[k-1];   // Delete last extremal
        else
          l = foundExt[0];     // Delete first extremal
      }

      for (j=l; j<k; j++)      // Loop that does the deletion
        foundExt[j] = foundExt[j+1];
      k--;
      extra--;
    }
    //  Copy found extremals to ext[]
    for (int i=0; i<=r; i++) ext[i] = foundExt[i];
  }


  /*********************
   * freqSample
   *============
   * Simple frequency sampling algorithm to determine the impulse
   * response h[] from A's found in ComputeA
   *
   *
   * INPUT:
   * ------
   * int      N        - Number of filter coefficients
   * double[] A        - Sample points of desired response [N/2]
   * int      symmetry - Symmetry of desired filter
   *
   * OUTPUT:
   * -------
   * double[] h        - Impulse Response of final filter [N]
   *********************/
  double[] freqSample(double A[], int symm) {
    double x, val;
    int N = numtaps;
    double M = (N-1.0)/2.0;
    double[] h = new double[N];
    if (symm == POSITIVE) {
      if (N%2 != 0) {
         for (int n=0; n<N; n++) {
           val = A[0];
           x = pi2 * (n - M)/N;
           for (int k=1; k<=M; k++) val += 2.0 * A[k] * Math.cos(x*k);
           h[n] = val/N;
         }
      }
      else {
        for (int n=0; n<N; n++) {
          val = A[0];
          x = pi2 * (n - M)/N;
          for (int k=1; k<=(N/2-1); k++) val += 2.0 * A[k] * Math.cos(x*k);
          h[n] = val/N;
        }
      }
    }
    else {
      if (N%2 != 0) {
        for (int n=0; n<N; n++) {
          val = 0;
          x = pi2 * (n - M)/N;
          for (int k=1; k<=M; k++) val += 2.0 * A[k] * Math.sin(x*k);
          h[n] = val/N;
        }
      }
      else {
        for (int n=0; n<N; n++) {
          val = A[N/2] * Math.sin(pi * (n - M));
          x = pi2 * (n - M)/N;
          for (int k=1; k<=(N/2-1); k++) val += 2.0 * A[k] * Math.sin(x*k);
          h[n] = val/N;
        }
      }
    }
    return h;
  }

  /*******************
   * isDone
   *========
   * Checks to see if the error function is small enough to consider
   * the result to have converged.
   *
   * INPUT:
   * ------
   * int      r   - 1/2 the number of filter coeffiecients
   * int[]    ext - Indexes to extremal frequencies [r+1]
   * double[] e   - Error function on the dense grid [gridSize]
   *
   * OUTPUT:
   * -------
   * Returns true if the result converged
   * Returns false if the result has not converged
   ********************/

  boolean isDone(int r, int ext[], double e[]) {

    double min, max, current;
    min = max = Math.abs(e[ext[0]]);
    for (int i=1; i<=r; i++){
      current = Math.abs(e[ext[i]]);
      if (current < min) min = current;
      if (current > max) max = current;
    }
    if (((max-min)/max) < 0.0001) return true;
    return false;
  }

  /********************
   * remez
   *=======
   * Calculates the optimal (in the Chebyshev/minimax sense)
   * FIR filter impulse response given a set of band edges,
   * the desired reponse on those bands, and the weight given to
   * the error in those bands.
   *
   * INPUT:
   * ------
   * int      numtaps - Number of filter coefficients
   * int      numband - Number of bands in filter specification
   * double[] bands   - User-specified band edges [2 * numband]
   * double[] des     - User-specified band responses [numband]
   * double[] weight  - User-specified error weights [numband]
   * int      type    - Type of filter
   *
   * OUTPUT:
   * -------
   * double[] h       - Impulse response of final filter [numtaps]
   ********************/

  public double[] remez(int n,
               double bands[], double des[], double weight[], int type) {

    double c;
    numtaps = n;
    int symmetry = (type == BANDPASS)? POSITIVE : NEGATIVE;
    int r = numtaps/2;   // number of extrema
    if ((numtaps%2 != 0) && (symmetry == POSITIVE)) r++;

    // Predict dense grid size in advance for array sizes
    int gridSize = 0;
    int numband = des.length;
    for (int i=0; i<numband; i++) {
      gridSize += (int)Math.round(2*r*GRIDDENSITY*(bands[2*i+1] - bands[2*i]));
    }
    if (symmetry == NEGATIVE) gridSize--;

    double[] grid = new double[gridSize];
    double[] d    = new double[gridSize];
    double[] w    = new double[gridSize];
    double[] e    = new double[gridSize];
    double[] x    = new double[r+1];
    double[] y    = new double[r+1];
    double[] ad   = new double[r+1];
    double[] taps = new double[r+1];
    int[]    ext  = new int[r+1];

    // Create dense frequency grid
    createDenseGrid(r, numtaps, numband, bands, des, weight,
                     gridSize, grid, d, w, symmetry);
    initialGuess(r, ext, gridSize);

    // For Differentiator: (fix grid)
    if (type == DIFFERENTIATOR) {
      for (int i=0; i<gridSize; i++) {
        if (d[i] > 0.0001) w[i] /= grid[i];
      }
    }

    // For odd or Negative symmetry filters, alter the
    // d[] and w[] according to Parks McClellan
    if (symmetry == POSITIVE) {
      if (numtaps % 2 == 0) {
        for (int i=0; i<gridSize; i++) {
          c = Math.cos(pi * grid[i]);
          d[i] /= c;
          w[i] *= c;
        }
      }
    }
    else {
      if (numtaps % 2 != 0) {
        for (int i=0; i<gridSize; i++) {
          c = Math.sin(pi2 * grid[i]);
          d[i] /= c;
          w[i] *= c;
        }
      }
      else {
        for (int i=0; i<gridSize; i++) {
          c = Math.sin(pi * grid[i]);
          d[i] /= c;
          w[i] *= c;
        }
      }
    }

    // Perform the Remez Exchange algorithm
    int iter;
    for (iter=0; iter<MAXITERATIONS; iter++) {
      calcParms(r, ext, grid, d, w, ad, x, y);
      calcError(r, ad, x, y, gridSize, grid, d, w, e);
      search(r, ext, gridSize, e);
      if (isDone(r, ext, e)) break;
    }
    if (iter == MAXITERATIONS) {
      System.out.println("Reached maximum iteration count.\nResults may be bad.\n");
    }

    calcParms(r, ext, grid, d, w, ad, x, y);

    // Find the 'taps' of the filter for use with Frequency
    // Sampling.  If odd or Negative symmetry, fix the taps
    // according to Parks McClellan
    for (int i=0; i<=numtaps/2; i++) {
      if (symmetry == POSITIVE) {
        if (numtaps%2 != 0) c = 1;
        else c = Math.cos(pi * (double)i/numtaps);
      }
      else {
        if (numtaps%2 != 0) c = Math.sin(pi2 * (double)i/numtaps);
        else c = Math.sin(pi * (double)i/numtaps);
      }
      taps[i] = computeA((double)i/numtaps, r, ad, x, y)*c;
    }
    // Frequency sampling design with calculated taps
    return freqSample(taps, symmetry);

  }

}

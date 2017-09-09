/*
 * Created on Oct 31, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jmt.engine.random;

import jmt.common.exception.IncorrectDistributionParameterException;
import Jama.Matrix;

/**
 * @author casale
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MAP extends AbstractDistribution implements Distribution {

	private Matrix D[];
	private Matrix iD0;
	private Matrix P;
	private Matrix pie;
	private Matrix jumpCDF;
	private Matrix hT; // holdingtimes

	private int nBatches;
	private int nStates;
	public int curState;

	public MAP() {
		curState = -1;
		nBatches = 1;
		nStates = 2;
		iD0 = null;
		P = null;
		pie = null;
		hT = new Matrix( 1, nStates );
		D = new Matrix[ nBatches + 1 ];
		for( int k = 0; k <= nBatches; k++ )
			D[ k ] = new Matrix( nStates, nStates );
	}

	public int getCurState() {
		return curState;
	}

	public void setCurState(int newState) {
		curState = newState;
	}

	public double getRate( int batch, int fromState, int toState )
	{
		return D[ batch ].get( fromState - 1, toState - 1 );
	}

	public void setRate( int batch, int fromState, int toState, double rate )
	{
		D[ batch ].set( fromState - 1, toState - 1, rate );
		curState = -1;
		if( batch >= 1 || fromState != toState ) normalize();
	}

	public int getOrder()
	{
		return nStates;
	}

	public double getMeanHoldingTime( int state )
	{
		return ( double ) hT.get( 0, state - 1 );
	}

	public void scale( double meanRate )
	{
		double EX = mean();
		for( int i = 1; i <= nStates; i++ )
			for( int j = 1; j <= nStates; j++ )
			{
				setRate( 0, i, j, getRate( 0, i, j ) * EX * meanRate );
				setRate( 1, i, j, getRate( 1, i, j ) * EX * meanRate );
			}
	}

	public void normalize()
	{
		double cumulative = 0.0;
		for( int j = 1; j <= nStates; j++ )
		{
			cumulative = 0.0;
			for( int i = 0; i <= nBatches; i++ )
				for( int k = 1; k <= nStates; k++ )
					if( i >= 1 || j != k ) // skip diagonal of D[0]
						cumulative += getRate( i, j, k );
			setRate( 0, j, j, -cumulative );
			hT.set( 0, j - 1, -1.0 / getRate( 0, j, j ) );

		}
		iD0 = null;
		P = null;
		jumpCDF = null;
		curState = -1;
	}

	public Matrix getInverse()
	{
		if( iD0 == null ) iD0 = D[ 0 ].inverse().uminus();
		return iD0;
	}

	public Matrix getEmbedded()
	{
		if( P == null ) P = getInverse().times( D[ 1 ] );
		return P;
	}

	public double pdf(double x, Parameter p) { //other implementation may use p.check()
		return 0.0;
	}

	public double cdf(double x, Parameter p) { //other implementation may use p.check()
		return 0.0;
	}

	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		return mean();
	}

	public double  mean() {
		Matrix e = new Matrix( nStates, 1 );
		for( int i = 0; i < nStates; i++ )
		{
			e.set( i, 0, 1.0 );
		}
		return getPie().times( getInverse().times( e ) ).get( 0, 0 );
	}

	public Matrix getPie()
	{
		if( pie == null )
		{
			Matrix I = new Matrix( nStates, nStates );
			for( int i = 0; i < nStates; i++ )
			{
				I.set( i, i, 1.0 );
			}

			Matrix Q = getEmbedded().transpose().minus( I );
			Matrix b = new Matrix( nStates, 1 );
			b.set( nStates - 1, 0, 1 );
			for( int i = 0; i < nStates; i++ )
			{
				Q.set( nStates - 1, i, 1.0 );
			}

			pie = Q.solve( b ).transpose();
		}

		return pie;
	}

	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		return 0.0;
	}

	public void print()
	{
		System.out.println( nStates );
		System.out.println( nBatches );
		System.out.println( " " );
		for( int i = 0; i <= nBatches; i++ )
			D[ i ].print( nBatches, 16 );
	}

	/* initialize state according to getPie() interval-stationary vector */
	public void initState()
	{
		double r = engine.nextDouble();
		curState = 0;
		while( r > getPie().getMatrix( 0, 0, 0, curState ).normInf() )
		{
			curState++;
		}
	}

	/* jump to next state */
	public boolean nextState()
	{
		double r = engine.nextDouble();
		int newState;

		if( jumpCDF == null )
		{
			jumpCDF = new Matrix( nStates, ( nBatches + 1 ) * nStates );
			for( int i = 0; i < nStates; i++ )
			{

				jumpCDF.setMatrix( i, i, 0, nStates - 1, D[ 0 ].getMatrix( i, i, 0,
						nStates - 1 ).times( getMeanHoldingTime( 1 + i ) ) );

				jumpCDF.setMatrix( i, i, nStates, ( nBatches + 1 ) * nStates - 1, D[ 1 ]
						.getMatrix( i, i, 0, nStates - 1 ).times(
								getMeanHoldingTime( 1 + i ) ) );
				jumpCDF.set( i, i, 0.0 );
			}
		}

		Matrix row = jumpCDF.getMatrix( curState, curState, 0, ( nBatches + 1 )
				* nStates - 1 );

		double rowCumSum = row.get( 0, 0 );
		newState = 0;
		while( r > rowCumSum )
		{
			newState++;
			if( newState == ( nBatches + 1 ) * nStates - 1 )
			{
				break;
			}
			rowCumSum = rowCumSum + row.get( 0, newState );
		}
		curState = newState % nStates;
		if( newState > nStates - 1 )
		{
			return false; // hidden transition?
		}
		else
		{
			return true; // hidden transition?
		}

	}

	/**
	 * it returns the new random number.
	 * This method is used to obtain from the distribution the next number distributed
	 * according to the distribution parameter.
	 *
	 * @param p parameter of the MAP (MAPPar class).
	 * @throws IncorrectDistributionParameterException
	 * @return double with the next random number of this distribution.
	 */


	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		{
			if (p.check()) {
				if( curState == -1 )
				{
					initState();
				}
				double sample = 0.0;

				do
				{
					double r = engine.nextDouble();

					sample = sample
							+ ( -( getMeanHoldingTime( curState + 1 ) ) * Math.log( 1.0 - r ) );
				}
				while( nextState() );

				return sample;
			} else {
				throw new IncorrectDistributionParameterException(
						"Incorrect parameterization of MAP distribution.");
			}
		}

	}
}
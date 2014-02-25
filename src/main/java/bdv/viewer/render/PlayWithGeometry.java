package bdv.viewer.render;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.Interpolation;

public class PlayWithGeometry
{
	public static void scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval,  final Interpolation interpolation, final RandomAccess< ? > cellsRandomAccess )
	{
		new Prefetcher().scan( sourceToScreen, cellDimensions, dimensions, screenInterval, interpolation, cellsRandomAccess );
	}

	static class Prefetcher
	{
		private final double[] xStep = new double[ 3 ];

		private final double[] offsetNeg = new double[ 3 ];

		private final double[] offsetPos = new double[ 3 ];

		private static final double eps = 0.0000001;

		public void scan( final AffineTransform3D sourceToScreen, final int[] cellDimensions, final long[] dimensions, final Dimensions screenInterval, final Interpolation interpolation, final RandomAccess< ? > cellsRandomAccess )
		{
			final RealPoint pSource = new RealPoint( 3 );
			final RealPoint pScreen = new RealPoint( 3 );
			final int[] minCell = new int[ 3 ];
			final int[] maxCell = new int[ 3 ];
			final int w = ( int ) screenInterval.dimension( 0 );
			final int h = ( int ) screenInterval.dimension( 1 );

			for ( int d = 0; d < 3; ++d )
				maxCell[ d ] = ( int ) ( ( dimensions[ d ] - 1 ) / cellDimensions[ d ] );

			// compute bounding box
			final RealPoint[] screenCorners = new RealPoint[ 4 ];
			screenCorners[ 0 ] = new RealPoint( 0, 0, 0 );
			screenCorners[ 1 ] = new RealPoint( w, 0, 0 );
			screenCorners[ 2 ] = new RealPoint( w, h, 0 );
			screenCorners[ 3 ] = new RealPoint( 0, h, 0 );
			final RealPoint sourceCorner = new RealPoint( 3 );
			final double[] bbMin = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
			final double[] bbMax = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
			for ( int i = 0; i < 4; ++i )
			{
				sourceToScreen.applyInverse( sourceCorner, screenCorners[ i ] );
				for ( int d = 0; d < 3; ++d )
				{
					final double p = sourceCorner.getDoublePosition( d );
					if ( p < bbMin[ d ] )
						bbMin[ d ] = p;
					if ( p > bbMax[ d ] )
						bbMax[ d ] = p;
				}
			}
			for ( int d = 0; d < 3; ++d )
			{
				minCell[ d ] = Math.min( maxCell[ d ], Math.max( ( int ) bbMin[ d ] / cellDimensions[ d ] - 1, 0 ) );
				maxCell[ d ] = Math.max( 0, Math.min( ( int ) bbMax[ d ] / cellDimensions[ d ] + 1, maxCell[ d ] ) );
			}

			checkProtoCell( cellDimensions, sourceToScreen, interpolation );
			getStep( cellDimensions, sourceToScreen );

			pSource.setPosition( ( minCell[ 2 ] - 1 ) * cellDimensions[ 2 ], 2 );
			for ( cellsRandomAccess.setPosition( minCell[ 2 ], 2 ); cellsRandomAccess.getIntPosition( 2 ) <= maxCell[ 2 ]; cellsRandomAccess.fwd( 2 ) )
			{
				pSource.move( cellDimensions[ 2 ], 2 );
				pSource.setPosition( ( minCell[ 1 ] - 1 ) * cellDimensions[ 1 ], 1 );
				for ( cellsRandomAccess.setPosition( minCell[ 1 ], 1 ); cellsRandomAccess.getIntPosition( 1 ) <= maxCell[ 1 ]; cellsRandomAccess.fwd( 1 ) )
				{
					pSource.move( cellDimensions[ 1 ], 1 );

					// find first and last cell that hits z
					pSource.setPosition( minCell[ 0 ] * cellDimensions[ 0 ], 0 );
					sourceToScreen.apply( pSource, pScreen );
					final double z0 = pScreen.getDoublePosition( 2 );
					int nStart = 0;
					int nStop = 0;
					if ( xStep[ 2 ] > eps )
					{
						nStart = minCell[ 0 ] + Math.max( 0, ( int ) Math.ceil( - ( z0 + offsetPos[ 2 ] ) / xStep[ 2 ] ) );
						if ( nStart > maxCell[ 0 ] )
							continue;
						nStop = Math.min( maxCell[ 0 ], minCell[ 0 ] + ( int ) Math.floor( - ( z0 + offsetNeg[ 2 ] ) / xStep[ 2 ] ) );
						if ( nStop < minCell[ 0 ] )
							continue;
					}
					else if ( xStep[ 2 ] < - eps )
					{
						nStart = minCell[ 0 ] + Math.max( 0, ( int ) Math.ceil( - ( z0 + offsetNeg[ 2 ] ) / xStep[ 2 ] ) );
						if ( nStart > maxCell[ 0 ] )
							continue;
						nStop = Math.min( maxCell[ 0 ], minCell[ 0 ] + ( int ) Math.floor( - ( z0 + offsetPos[ 2 ] ) / xStep[ 2 ] ) );
						if ( nStop < minCell[ 0 ] )
							continue;
					}
					else
					{
						if ( z0 + offsetNeg[ 2 ] > 0 || z0 + offsetPos[ 2 ] < 0 )
							continue;
						nStart = minCell[ 0 ];
						nStop = maxCell[ 0 ];
					}

					pSource.setPosition( nStart * cellDimensions[ 0 ], 0 );
					for ( cellsRandomAccess.setPosition( nStart, 0 ); cellsRandomAccess.getIntPosition( 0 ) <= nStop; cellsRandomAccess.fwd( 0 ) )
					{
						sourceToScreen.apply( pSource, pScreen );
						final double x = pScreen.getDoublePosition( 0 );
						final double y = pScreen.getDoublePosition( 1 );
						if (    ( x + offsetPos[ 0 ] >= 0 ) &&
								( x + offsetNeg[ 0 ] < w ) &&
								( y + offsetPos[ 1 ] >= 0 ) &&
								( y + offsetNeg[ 1 ] < h ) )
						{
							cellsRandomAccess.get();
						}
						pSource.move( cellDimensions[ 0 ], 0 );
					}
				}
			}
		}

		void getStep( final int[] cellStep, final AffineTransform3D sourceToScreen )
		{
			final RealPoint p0 = new RealPoint( 3 );
			final RealPoint p1 = new RealPoint( 3 );
			p1.setPosition( cellStep[ 0 ], 0 );
			final RealPoint s0 = new RealPoint( 3 );
			final RealPoint s1 = new RealPoint( 3 );
			sourceToScreen.apply( p0, s0 );
			sourceToScreen.apply( p1, s1 );
			for ( int d = 0; d < 3; ++d )
				xStep[ d ] = s1.getDoublePosition( d ) - s0.getDoublePosition( d );
		}

		void checkProtoCell( final int[] cellDims, final AffineTransform3D sourceToScreen, final Interpolation interpolation )
		{
			final RealPoint pSource = new RealPoint( 3 );
			final RealPoint pScreenAnchor = new RealPoint( 3 );
			sourceToScreen.apply( pSource, pScreenAnchor );

			final RealPoint[] pScreen = new RealPoint[ 8 ];
			final double[] cellMin = new double[ 3 ];
			final double[] cellSize = new double[] { cellDims[ 0 ], cellDims[ 1 ], cellDims[ 2 ] };
			if ( interpolation == Interpolation.NEARESTNEIGHBOR )
			{
				for ( int d = 0; d < 3; ++d )
				{
					cellMin[ d ] -= 0.5;
					cellSize[ d ] -= 0.5;
				}
			}
			else // Interpolation.NLINEAR
			{
				for ( int d = 0; d < 3; ++d )
					cellMin[ d ] -= 1;
			}
			int i = 0;
			for ( int z = 0; z < 2; ++z )
			{
				pSource.setPosition( ( z == 0 ) ? cellMin[ 2 ] : cellSize[ 2 ], 2 );
				for ( int y = 0; y < 2; ++y )
				{
					pSource.setPosition( ( y == 0 ) ? cellMin[ 1 ] : cellSize[ 1 ], 1 );
					for ( int x = 0; x < 2; ++x )
					{
						pSource.setPosition( ( x == 0 ) ? cellMin[ 0 ] : cellSize[ 0 ], 0 );
						pScreen[ i ] = new RealPoint( 3 );
						sourceToScreen.apply( pSource, pScreen[ i++ ] );
					}
				}
			}

			for ( int d = 0; d < 3; ++d )
			{
				double min = pScreen[ 0 ].getDoublePosition( d );
				double max = pScreen[ 0 ].getDoublePosition( d );
				for ( i = 1; i < 8; ++i )
				{
					final double p = pScreen[ i ].getDoublePosition( d );
					if ( p < min )
						min = p;
					if ( p > max )
						max = p;
				}
				offsetNeg[ d ] = min - pScreenAnchor.getDoublePosition( d );
				offsetPos[ d ] = max - pScreenAnchor.getDoublePosition( d );
			}
		}
	}
}
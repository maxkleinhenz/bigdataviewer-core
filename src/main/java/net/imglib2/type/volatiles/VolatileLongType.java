/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.type.volatiles;

import net.imglib2.img.NativeImg;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.LongAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileLongAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.type.numeric.integer.LongType;

public class VolatileLongType extends AbstractVolatileNativeRealType< LongType, VolatileLongType >
{
	final protected NativeImg< ?, ? extends VolatileLongAccess > img;

	private static class WrappedLongType extends LongType
	{
		public WrappedLongType( final NativeImg< ?, ? extends LongAccess > img )
		{
			super( img );
		}

		public WrappedLongType( final LongAccess access )
		{
			super( access );
		}

		public void setAccess( final LongAccess access )
		{
			dataAccess = access;
		}
	}

	// this is the constructor if you want it to read from an array
	public VolatileLongType( final NativeImg< ?, ? extends VolatileLongAccess > img )
	{
		super( new WrappedLongType( img ), false );
		this.img = img;
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileLongType( final VolatileLongAccess access )
	{
		super( new WrappedLongType( access ), access.isValid() );
		this.img = null;
	}

	// this is the constructor if you want it to be a variable
	public VolatileLongType( final long value )
	{
		this( new VolatileLongArray( 1, true ) );
		set( value );
	}

	// this is the constructor if you want it to be a variable
	public VolatileLongType()
	{
		this( 0 );
	}

	public void set( final long value )
	{
		get().set( value );
	}

	@Override
	public void updateContainer( final Object c )
	{
		final VolatileLongAccess a = img.update( c );
		( ( WrappedLongType ) t ).setAccess( a );
		setValid( a.isValid() );
	}

	@Override
	public NativeImg< VolatileLongType, ? extends VolatileLongAccess > createSuitableNativeImg( final NativeImgFactory< VolatileLongType > storageFactory, final long[] dim )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public VolatileLongType duplicateTypeOnSameNativeImg()
	{
		return new VolatileLongType( img );
	}

	@Override
	public VolatileLongType createVariable()
	{
		return new VolatileLongType();
	}

	@Override
	public VolatileLongType copy()
	{
		final VolatileLongType v = createVariable();
		v.set( this );
		return v;
	}
}

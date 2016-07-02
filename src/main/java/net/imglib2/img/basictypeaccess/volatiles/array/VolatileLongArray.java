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
package net.imglib2.img.basictypeaccess.volatiles.array;

import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileLongAccess;

/**
 * A {@link LongArray} with an {@link #isValid()} flag and that tracks
 * whether it was modified or not.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class VolatileLongArray extends AbstractVolatileArray< VolatileLongArray > implements VolatileLongAccess
{
	protected long[] data;

	public VolatileLongArray( final int numEntities, final boolean isValid )
	{
		super( isValid );
		this.data = new long[ numEntities ];
	}

	public VolatileLongArray( final long[] data, final boolean isValid )
	{
		super( isValid );
		this.data = data;
	}

	@Override
	public long getValue( final int index )
	{
		return data[ index ];
	}

	@Override
	public void setValue( final int index, final long value )
	{
		modified = true;
		data[ index ] = value;
	}

	@Override
	public VolatileLongArray createArray( final int numEntities )
	{
		return new VolatileLongArray( numEntities, true );
	}

	@Override
	public long[] getCurrentStorageArray()
	{
		return data;
	}
}

/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package bdv.tools.bookmarks.editor;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.tools.bookmarks.BookmarkTextOverlayAnimator;
import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.BookmarksCollectionChangedListener;
import bdv.tools.bookmarks.bookmark.Bookmark;
import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.tools.bookmarks.bookmark.KeyFrame;
import bdv.tools.bookmarks.bookmark.SimpleBookmark;
import bdv.util.Affine3DHelpers;
import bdv.viewer.TimePointListener;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.RotationAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.Point;
import net.imglib2.realtransform.AffineTransform3D;

public class BookmarksEditor
{
	static enum Mode
	{
		INACTIVE, SET, CREATE_DYNAMIC_BOOKMARK, RECALL_TRANSFORM, RECALL_ORIENTATION, ADD_KEYFRAME, RENAME, DELETE
	}

	private Mode mode = Mode.INACTIVE;

	private volatile boolean initialKey = false;

	private final ArrayList< String > inputMapsToBlock;

	private final ViewerPanel viewer;

	private final InputActionBindings bindings;

	private final ActionMap actionMap;

	private final InputMap inputMap;

	private BookmarkTextOverlayAnimator animator;

	private final Bookmarks bookmarks;

	private BookmarkRenameEditor bookmarkRenameEditor;

	public BookmarksEditor( final ViewerPanel viewer, final InputActionBindings inputActionBindings,
			final Bookmarks bookmarks )
	{
		this.viewer = viewer;
		bindings = inputActionBindings;
		inputMapsToBlock = new ArrayList<>( Arrays.asList( "bdv", "navigation" ) );
		this.bookmarks = bookmarks;

		final KeyStroke abortKey = KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 );
		final Action abortAction = new AbstractAction( "abort bookmark" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				abort();
			}

			private static final long serialVersionUID = 1L;
		};

		actionMap = new ActionMap();
		inputMap = new InputMap();
		actionMap.put( "abort bookmark", abortAction );
		inputMap.put( abortKey, "abort bookmark" );

		bindings.addActionMap( "bookmarks", actionMap );

		bookmarkRenameEditor = new BookmarkRenameEditor( viewer, inputActionBindings, bookmarks );
		bookmarkRenameEditor.addListener( new BookmarkRenameEditorListener()
		{

			@Override
			public void bookmarkRenameFinished( String oldKey, String newKey )
			{
				renameBookmark( oldKey, newKey );
				setActiveDynamicBookmark( bookmarks.get( newKey, DynamicBookmark.class ) );

				String message = String.format( "bookmark %s renamed to %s", oldKey, newKey );
				fadeOut( message, 1500 );
			}

			@Override
			public void bookmarkRenameAborted( String oldKey )
			{
				fadeOut( "rename bookmark aborted", 1500 );
			}
		} );

		viewer.getDisplay().addKeyListener( new KeyAdapter()
		{
			@Override
			public void keyTyped( final KeyEvent e )
			{
				if ( mode != Mode.INACTIVE )
				{
					if ( initialKey )
						initialKey = false;
					else
					{
						final String key = String.valueOf( e.getKeyChar() );
						switch ( mode )
						{
						case SET:
						{
							createSimpleBookmark( key );

							animator.fadeOut( "set bookmark: " + key, 500 );
							viewer.requestRepaint();
						}
							break;
						case CREATE_DYNAMIC_BOOKMARK:
						{
							createDynamicBookmark( key );

							animator.fadeOut( "create dynamic bookmark: " + key, 500 );
						}
							break;
						case RECALL_TRANSFORM:
						{
							recallTransformationOfBookmark( key );

							animator.fadeOut( "go to bookmark: " + key, 500 );
						}
							break;
						case RECALL_ORIENTATION:
						{
							final int currentTimepoint = viewer.getState().getCurrentTimepoint();
							final double cX = viewer.getDisplay().getWidth() / 2.0;
							final double cY = viewer.getDisplay().getHeight() / 2.0;

							// set dynamic bookmark if the key is associated
							// with a dynamic bookmark,
							// otherwise it will set null
							setActiveDynamicBookmark( bookmarks.get( key, DynamicBookmark.class ) );

							final AffineTransform3D targetTransform = bookmarks.getTransform( key, currentTimepoint, cX,
									cY );
							if ( targetTransform != null )
							{
								final AffineTransform3D c = new AffineTransform3D();
								viewer.getState().getViewerTransform( c );
								final Point p = new Point( 2 );
								viewer.getMouseCoordinates( p );
								final double[] qTarget = new double[ 4 ];
								Affine3DHelpers.extractRotation( targetTransform, qTarget );
								viewer.setTransformAnimator( new RotationAnimator( c, p.getDoublePosition( 0 ),
										p.getDoublePosition( 1 ), qTarget, 300 ) );
							}
							animator.fadeOut( "go to bookmark orientation: " + key, 500 );
						}
							break;
						case RENAME:
						{
							if ( bookmarks.get( key ) == null )
							{
								fadeOut( "bookmark with the key " + key + " could not be found ", 1500 );
							}
							else
							{
								fadeIn( "rename bookmark " + key + " to: ", 500 );
								bookmarkRenameEditor.init( key );
							}
						}
							break;
						case DELETE:
						{
							if ( bookmarks.get( key ) == null )
							{
								fadeOut( "bookmark with the key " + key + " could not be found ", 1500 );
							}
							else
							{
								removeBookmark( key );
								fadeOut( "delete bookmark: " + key, 500 );
							}
						}
							break;
						default:
							break;
						}
						done();
					}
				}
			}
		} );

		viewer.addTimePointListener( new TimePointListener()
		{

			@Override
			public void timePointChanged( int timePointIndex )
			{
				DynamicBookmark dynamicBookmark = getActiveDynamicBookmark();
				if ( dynamicBookmark != null )
				{

					final double cX = viewer.getDisplay().getWidth() / 2.0;
					final double cY = viewer.getDisplay().getHeight() / 2.0;

					final AffineTransform3D targetTransform = dynamicBookmark.getInterpolatedTransform( timePointIndex,
							cX, cY );
					if ( targetTransform != null )
					{
						targetTransform.set( targetTransform.get( 0, 3 ) + cX, 0, 3 );
						targetTransform.set( targetTransform.get( 1, 3 ) + cY, 1, 3 );

						viewer.setCurrentViewerTransform( targetTransform );
					}
				}
			}
		} );

		viewer.getKeyFramePopupMenu().setBookmarksEditor( this );
	}

	public synchronized void abort()
	{
		if ( animator != null )
			animator.clear();
		done();
	}

	private synchronized void setActiveDynamicBookmark( DynamicBookmark bookmark )
	{
		viewer.setActiveBookmark( bookmark );
	}

	private synchronized DynamicBookmark getActiveDynamicBookmark()
	{
		Bookmark bookmark = viewer.getActiveBookmark();

		if ( bookmark instanceof DynamicBookmark ) { return ( DynamicBookmark ) bookmark; }

		return null;
	}

	private void fadeIn( String message, long duration )
	{
		if ( animator != null )
			animator.clear();
		animator = new BookmarkTextOverlayAnimator( viewer );
		viewer.addOverlayAnimator( animator );

		animator.fadeIn( message, duration );
	}

	private void fadeOut( String message, long duration )
	{
		if ( animator != null )
			animator.clear();
		animator = new BookmarkTextOverlayAnimator( viewer );
		viewer.addOverlayAnimator( animator );

		animator.fadeOut( message, duration );
	}

	private AffineTransform3D getViewTransformForBookmark()
	{
		final AffineTransform3D t = new AffineTransform3D();
		viewer.getState().getViewerTransform( t );
		final double cX = viewer.getDisplay().getWidth() / 2.0;
		final double cY = viewer.getDisplay().getHeight() / 2.0;
		t.set( t.get( 0, 3 ) - cX, 0, 3 );
		t.set( t.get( 1, 3 ) - cY, 1, 3 );

		return t;
	}

	protected synchronized void init( final Mode mode, final String message )
	{
		initialKey = true;
		this.mode = mode;
		bindings.addInputMap( "bookmarks", inputMap, inputMapsToBlock );
		fadeIn( message, 100 );
	}

	public synchronized void initSetBookmark()
	{
		init( Mode.SET, "set bookmark: " );
	}

	public synchronized void initCreateDynamicBookmark()
	{
		if ( areDynamicBookmarksEnabled() )
			init( Mode.CREATE_DYNAMIC_BOOKMARK, "create dynamic bookmark: " );
		else
			fadeOut( "dynamic bookmarks cannot be created", 1000 );
	}

	public synchronized void initGoToBookmark()
	{
		init( Mode.RECALL_TRANSFORM, "go to bookmark: " );
	}

	public synchronized void initGoToBookmarkRotation()
	{
		init( Mode.RECALL_ORIENTATION, "go to bookmark orientation: " );
	}

	public synchronized void initRenameBookmark()
	{
		init( Mode.RENAME, String.format( "bookmark key to rename: " ) );
	}

	public synchronized void initDeleteBookmark()
	{
		init( Mode.DELETE, String.format( "delete bookmark: " ) );
	}

	public synchronized void createSimpleBookmark( String key )
	{
		createSimpleBookmark( key, null, null );
	}

	public synchronized void createSimpleBookmark( String key, String title, String description )
	{
		final AffineTransform3D t = getViewTransformForBookmark();
		final SimpleBookmark bookmark = new SimpleBookmark( key, t );

		bookmark.setTitle( title );
		bookmark.setDescription( description );
		bookmarks.put( bookmark );

		setActiveDynamicBookmark( null );
	}

	public synchronized void createDynamicBookmark( String key )
	{
		createDynamicBookmark( key, null, null );
	}

	public synchronized void createDynamicBookmark( String key, String title, String description )
	{
		DynamicBookmark bookmark = new DynamicBookmark( key );
		bookmark.setTitle( title );
		bookmark.setDescription( description );
		bookmarks.put( bookmark );

		setActiveDynamicBookmark( bookmark );
	}

	public synchronized void recallTransformationOfBookmark( String key )
	{
		final int currentTimepoint = viewer.getState().getCurrentTimepoint();
		final double cX = viewer.getDisplay().getWidth() / 2.0;
		final double cY = viewer.getDisplay().getHeight() / 2.0;

		setActiveDynamicBookmark( bookmarks.get( key, DynamicBookmark.class ) );

		final AffineTransform3D targetTransform = bookmarks.getTransform( key, currentTimepoint, cX, cY );

		if ( targetTransform != null )
		{
			final AffineTransform3D viewTransform = new AffineTransform3D();
			viewer.getState().getViewerTransform( viewTransform );

			viewTransform.set( viewTransform.get( 0, 3 ) - cX, 0, 3 );
			viewTransform.set( viewTransform.get( 1, 3 ) - cY, 1, 3 );

			viewer.setTransformAnimator( new SimilarityTransformAnimator( viewTransform, targetTransform, cX, cY, 300 ) );
		}
	}

	public synchronized Bookmark removeBookmark( final String key )
	{
		final Bookmark removedBookmark = bookmarks.remove( key );
		if ( removedBookmark == null )
			return null;

		final Bookmark acitveBookmark = viewer.getActiveBookmark();
		if ( acitveBookmark != null && acitveBookmark.equals( removedBookmark ) )
		{
			viewer.setActiveBookmark( null );
		}

		return removedBookmark;
	}

	public synchronized boolean containsBookmark( final String key )
	{
		return bookmarks.containsKey( key );
	}

	public synchronized Bookmark renameBookmark( final String oldKey, final String newKey )
	{
		final String activeBookmarkKey = viewer.getActiveBookmark() == null ? null
				: viewer.getActiveBookmark().getKey();

		final Bookmark newBookmark = bookmarks.rename( oldKey, newKey );
		if ( newBookmark != null )
		{
			if ( oldKey.equals( activeBookmarkKey ) )
			{
				viewer.setActiveBookmark( newBookmark );
			}
			return newBookmark;
		}

		return null;
	}

	public synchronized void deselectBookmark()
	{
		setActiveDynamicBookmark( null );
	}

	public synchronized void addKeyframe()
	{
		final DynamicBookmark dynamicBookmark = getActiveDynamicBookmark();
		if ( dynamicBookmark != null )
		{

			final AffineTransform3D tranform = getViewTransformForBookmark();
			final int timepoint = viewer.getState().getCurrentTimepoint();

			final KeyFrame keyframe = new KeyFrame( timepoint, tranform );
			dynamicBookmark.add( keyframe );

			fadeOut( "key frame added to " + dynamicBookmark.getKey(), 1000 );
		}
		else
		{
			fadeOut( "no active dynamic bookmark", 1000 );
		}
	}

	public synchronized void setTransformationToKeyframe( final KeyFrame keyframe )
	{
		keyframe.setTransform( getViewTransformForBookmark() );
	}

	public synchronized void removeKeyframe()
	{
		final int timepoint = viewer.getState().getCurrentTimepoint();
		removeKeyframe( timepoint );
	}

	public synchronized void removeKeyframe( final int timepoint )
	{
		final KeyFrame keyframe = new KeyFrame( timepoint, null );
		removeKeyframe( keyframe );
	}

	public synchronized void removeKeyframe( final KeyFrame keyFrame )
	{
		final DynamicBookmark dynamicBookmark = getActiveDynamicBookmark();
		if ( dynamicBookmark != null )
		{
			final boolean removed = dynamicBookmark.remove( keyFrame );

			if ( removed )
			{
				fadeOut( "key frame removed", 1000 );
			}
			else
			{
				fadeOut( "no key frame at this timepoint", 1000 );
			}
		}
		else
		{
			fadeOut( "no active dynamic bookmark", 1000 );
		}
	}

	public synchronized boolean copyKeyFrame( final KeyFrame keyframe )
	{
		final DynamicBookmark dynamicBookmark = getActiveDynamicBookmark();
		if ( dynamicBookmark != null )
		{

			final int numTimepoint = this.viewer.getState().getNumTimepoints();
			final int timeOneLater = Math.min( numTimepoint, keyframe.getTimepoint() + 1 );
			final int timeOneEarlier = Math.max( 0, keyframe.getTimepoint() - 1 );

			final KeyFrame keyFrameOneLater = new KeyFrame( timeOneLater, keyframe.getTransform() );
			final KeyFrame keyFrameOneEalier = new KeyFrame( timeOneEarlier, keyframe.getTransform() );
			if ( !dynamicBookmark.getFrameSet().contains( keyFrameOneLater ) )
			{
				return dynamicBookmark.add( keyFrameOneLater );
			}
			else if ( !dynamicBookmark.getFrameSet().contains( keyFrameOneEalier ) )
			{
				return dynamicBookmark.add( keyFrameOneEalier );
			}
			else
			{
				fadeOut( "could not copy the keyframe", 1000 );
			}
		}
		else
		{
			fadeOut( "no active dynamic bookmark", 1000 );
		}

		return false;
	}

	public synchronized void nextKeyframe()
	{
		final DynamicBookmark dynamicBookmark = getActiveDynamicBookmark();
		if ( dynamicBookmark != null )
		{

			final int currentTimepoint = viewer.getState().getCurrentTimepoint();
			final KeyFrame nextKeyframe = dynamicBookmark.getNextKeyFrame( currentTimepoint );
			if ( nextKeyframe != null && nextKeyframe.getTimepoint() > currentTimepoint )
			{
				viewer.setTimepoint( nextKeyframe.getTimepoint() );
				fadeOut( "go to next key frame", 1000 );
			}
			else
			{
				fadeOut( "no next key frame available", 1000 );
			}
		}
	}

	public synchronized void previousKeyframe()
	{
		final DynamicBookmark dynamicBookmark = getActiveDynamicBookmark();
		if ( dynamicBookmark != null )
		{

			final int currentTimepoint = viewer.getState().getCurrentTimepoint();
			final KeyFrame previousKeyframe = dynamicBookmark.getPreviousKeyFrame( currentTimepoint );
			if ( previousKeyframe != null && previousKeyframe.getTimepoint() < currentTimepoint )
			{
				viewer.setTimepoint( previousKeyframe.getTimepoint() );
				fadeOut( "go to previous key frame", 1000 );
			}
			else
			{
				fadeOut( "no previous key frame available", 1000 );
			}
		}
	}

	/**
	 * Returns true when Dynamic Bookmarks can be created, otherwise false
	 * 
	 * @return whether Dynamic Bookmarks can be created or not
	 */
	public synchronized boolean areDynamicBookmarksEnabled()
	{
		return viewer.getState().getNumTimepoints() > 1;
	}

	public synchronized Collection< Bookmark > getAllBookmarks()
	{
		return this.bookmarks.getAll();
	}

	public synchronized void done()
	{
		mode = Mode.INACTIVE;
		initialKey = false;
		bindings.removeInputMap( "bookmarks" );
	}

	public synchronized void setInputMapsToBlock( final Collection< String > idsToBlock )
	{
		inputMapsToBlock.clear();
		inputMapsToBlock.addAll( idsToBlock );
	}

	public void addBookmarksCollectionChangedListener( BookmarksCollectionChangedListener listener )
	{
		this.bookmarks.addListener( listener );
	}

	public void removeBookmarksCollectionChangedListener( BookmarksCollectionChangedListener listener )
	{
		this.bookmarks.removeListener( listener );
	}
}

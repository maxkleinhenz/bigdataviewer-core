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
package bdv.viewer;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.BehaviourTransformEventHandler;
import bdv.cache.CacheControl;
import bdv.viewer.SaveOnCloseFunction.UserSaveChoice;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.util.GuiUtil;

/**
 * A {@link JFrame} containing a {@link ViewerPanel} and associated
 * {@link InputActionBindings}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class ViewerFrame extends JFrame
{
	private static final long serialVersionUID = 1L;

	protected final ViewerPanel viewer;

	private final InputActionBindings keybindings;

	private final TriggerBehaviourBindings triggerbindings;
	
	private SaveOnCloseFunction funcSaveOnClose = null;

	public ViewerFrame(
			final List< SourceAndConverter< ? > > sources,
			final int numTimepoints,
			final CacheControl cache )
	{
		this( sources, numTimepoints, cache, ViewerOptions.options() );
	}

	/**
	 *
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display.
	 * @param numTimepoints
	 *            number of available timepoints.
	 * @param cacheControl
	 *            handle to cache. This is used to control io timing.
	 * @param optional
	 *            optional parameters. See {@link ViewerOptions#options()}.
	 */
	public ViewerFrame(
			final List< SourceAndConverter< ? > > sources,
			final int numTimepoints,
			final CacheControl cacheControl,
			final ViewerOptions optional )
	{
//		super( "BigDataViewer", GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.ARGB_COLOR_MODEL ) );
		super( "BigDataViewer", GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL ) );
			
		keybindings = new InputActionBindings();
		triggerbindings = new TriggerBehaviourBindings();
		
		viewer = new ViewerPanel( sources, numTimepoints, cacheControl, optional, keybindings.getConcatenatedActionMap() );

		getRootPane().setDoubleBuffered( true );
		add( viewer, BorderLayout.CENTER );
		pack();
        
        setLocationRelativeTo(null);
		setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
        
		final WindowAdapter windowAdapter = new WindowAdapter()
		{
			@Override
			public void windowStateChanged( WindowEvent e )
			{
				SwingUtilities.invokeLater( () -> {
					getContentPane().revalidate();
				} );
			}

			@Override
			public void windowClosing( final WindowEvent e )
			{
				UserSaveChoice choice = UserSaveChoice.NO;
				if (null != funcSaveOnClose) {
					choice = funcSaveOnClose.invokeSaveOnClose();
				}
				
				if (choice != UserSaveChoice.CANCEL) {
					viewer.stop();
					viewer.stopPlayExecuter();
					dispose();
				}
			}
		};
        
		addWindowListener(windowAdapter);
        addWindowStateListener(windowAdapter);

		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		viewer.getDisplay().addHandler( mouseAndKeyHandler );

		final TransformEventHandler< ? > tfHandler = viewer.getDisplay().getTransformEventHandler();
		if ( tfHandler instanceof BehaviourTransformEventHandler )
			( ( BehaviourTransformEventHandler< ? > ) tfHandler ).install( triggerbindings );
	}

	public ViewerPanel getViewerPanel()
	{
		return viewer;
	}

	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}

	public TriggerBehaviourBindings getTriggerbindings()
	{
		return triggerbindings;
	}
	
	public void setSaveOnCloseFunction(SaveOnCloseFunction function) {
		this.funcSaveOnClose = function;
	}
	
}

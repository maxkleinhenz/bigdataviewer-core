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
package bdv.tools;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bdv.cache.CacheControl;
import bdv.export.ProgressWriter;
import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.util.Prefs;
import bdv.viewer.ViewerPanel;
import bdv.viewer.overlay.ScaleBarOverlayRenderer;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.state.ViewerState;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import java.awt.Component;
import javax.swing.Box;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class RecordMovieDialog extends JDialog implements OverlayRenderer
{
	private static final long serialVersionUID = 1L;

	private final ViewerPanel viewer;

	private final int maxTimepoint;

	private final ProgressWriter progressWriter;

	private final JTextField pathTextField;

	private final JSpinner spinnerMinTimepoint;

	private final JSpinner spinnerMaxTimepoint;

	private final JSpinner spinnerWidth;

	private final JSpinner spinnerHeight;
	
	private JProgressBar progressBar;
	
	private boolean isRecordThreadRunning;
	private JButton cancelButton;

	public RecordMovieDialog( final Frame owner, final ViewerPanel viewer, final ProgressWriter progressWriter )
	{
		super( owner, "record movie", false );
		this.viewer = viewer;
		maxTimepoint = viewer.getState().getNumTimepoints() - 1;
		this.progressWriter = progressWriter;

		final JPanel boxes = new JPanel();
		getContentPane().add( boxes, BorderLayout.NORTH );
		boxes.setLayout( new BoxLayout( boxes, BoxLayout.PAGE_AXIS ) );

		final JPanel saveAsPanel = new JPanel();
		saveAsPanel.setLayout( new BorderLayout( 0, 0 ) );
		boxes.add( saveAsPanel );

		saveAsPanel.add( new JLabel( "save to" ), BorderLayout.WEST );

		pathTextField = new JTextField( "./record/" );
		saveAsPanel.add( pathTextField, BorderLayout.CENTER );
		pathTextField.setColumns( 20 );

		final JButton browseButton = new JButton( "Browse" );
		saveAsPanel.add( browseButton, BorderLayout.EAST );

		final JPanel timepointsPanel = new JPanel();
		boxes.add( timepointsPanel );

		timepointsPanel.add( new JLabel( "timepoints from" ) );

		spinnerMinTimepoint = new JSpinner();
		spinnerMinTimepoint.setModel( new SpinnerNumberModel( 0, 0, maxTimepoint, 1 ) );
		timepointsPanel.add( spinnerMinTimepoint );

		timepointsPanel.add( new JLabel( "to" ) );

		spinnerMaxTimepoint = new JSpinner();
		spinnerMaxTimepoint.setModel( new SpinnerNumberModel( maxTimepoint, 0, maxTimepoint, 1 ) );
		timepointsPanel.add( spinnerMaxTimepoint );

		final JPanel widthPanel = new JPanel();
		boxes.add( widthPanel );
		widthPanel.add( new JLabel( "width" ) );
		spinnerWidth = new JSpinner();
		spinnerWidth.setModel( new SpinnerNumberModel( 800, 10, 5000, 1 ) );
		widthPanel.add( spinnerWidth );

		final JPanel heightPanel = new JPanel();
		boxes.add( heightPanel );
		heightPanel.add( new JLabel( "height" ) );
		spinnerHeight = new JSpinner();
		spinnerHeight.setModel( new SpinnerNumberModel( 600, 10, 5000, 1 ) );
		heightPanel.add( spinnerHeight );
		
		JPanel progressPanel = new JPanel();
		progressPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		boxes.add(progressPanel);
		GridBagLayout gbl_progressPanel = new GridBagLayout();
		gbl_progressPanel.columnWidths = new int[]{332, 0, 0};
		gbl_progressPanel.rowHeights = new int[]{19, 0};
		gbl_progressPanel.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		gbl_progressPanel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		progressPanel.setLayout(gbl_progressPanel);
		
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		GridBagConstraints gbc_progressBar = new GridBagConstraints();
		gbc_progressBar.fill = GridBagConstraints.HORIZONTAL;
		gbc_progressBar.insets = new Insets(0, 0, 0, 5);
		gbc_progressBar.gridx = 0;
		gbc_progressBar.gridy = 0;
		progressPanel.add(progressBar, gbc_progressBar);
		
		cancelButton = new JButton("Cancel");
		cancelButton.setEnabled(false);
		GridBagConstraints gbc_cancelButton = new GridBagConstraints();
		gbc_cancelButton.gridx = 1;
		gbc_cancelButton.gridy = 0;
		progressPanel.add(cancelButton, gbc_cancelButton);
		cancelButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				isRecordThreadRunning = false;
			}
		});

		final JPanel buttonsPanel = new JPanel();
		boxes.add( buttonsPanel );
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

		final JButton recordButton = new JButton( "Record" );
		buttonsPanel.add( recordButton );

		spinnerMinTimepoint.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int min = ( Integer ) spinnerMinTimepoint.getValue();
				final int max = ( Integer ) spinnerMaxTimepoint.getValue();
				if ( max < min )
					spinnerMaxTimepoint.setValue( min );
			}
		} );

		spinnerMaxTimepoint.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int min = ( Integer ) spinnerMinTimepoint.getValue();
				final int max = ( Integer ) spinnerMaxTimepoint.getValue();
				if (min > max)
					spinnerMinTimepoint.setValue( max );
			}
		} );

		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled( false );
		fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );

		browseButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				fileChooser.setSelectedFile( new File( pathTextField.getText() ) );
				final int returnVal = fileChooser.showSaveDialog( null );
				if ( returnVal == JFileChooser.APPROVE_OPTION )
				{
					final File file = fileChooser.getSelectedFile();
					pathTextField.setText( file.getAbsolutePath() );
				}
			}
		} );

		recordButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final String dirname = pathTextField.getText();
				final File dir = new File( dirname );
				if ( !dir.exists() )
					dir.mkdirs();
				if ( !dir.exists() || !dir.isDirectory() )
				{
					System.err.println( "Invalid export directory " + dirname );
					return;
				}
				final int minTimepointIndex = ( Integer ) spinnerMinTimepoint.getValue();
				final int maxTimepointIndex = ( Integer ) spinnerMaxTimepoint.getValue();
				final int width = ( Integer ) spinnerWidth.getValue();
				final int height = ( Integer ) spinnerHeight.getValue();
				new Thread()
				{
					@Override
					public void run()
					{
						try
						{			
							isRecordThreadRunning = true;
							recordButton.setEnabled( false );
							cancelButton.setEnabled(true);
							
							recordMovie( width, height, minTimepointIndex, maxTimepointIndex, dir );
							
							progressBar.setValue(0);
							recordButton.setEnabled( true );
							cancelButton.setEnabled(false);
							isRecordThreadRunning = false;
						}
						catch ( final Exception ex )
						{
							ex.printStackTrace();
						}
					}
				}.start();
			}
		} );
		

		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}

			private static final long serialVersionUID = 1L;
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		pack();
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}

	public void recordMovie( final int width, final int height, final int minTimepointIndex, final int maxTimepointIndex, final File dir ) throws IOException
	{
		final ViewerState renderState = viewer.getState();
		final int canvasW = viewer.getDisplay().getWidth();
		final int canvasH = viewer.getDisplay().getHeight();

		final ScaleBarOverlayRenderer scalebar = Prefs.showScaleBarInMovie() ? new ScaleBarOverlayRenderer() : null;

		class MyTarget implements RenderTarget
		{
			BufferedImage bi;

			@Override
			public BufferedImage setBufferedImage( final BufferedImage bufferedImage )
			{
				bi = bufferedImage;
				return null;
			}

			@Override
			public int getWidth()
			{
				return width;
			}

			@Override
			public int getHeight()
			{
				return height;
			}
		}
		final MyTarget target = new MyTarget();
		final MultiResolutionRenderer renderer = new MultiResolutionRenderer(
				target, new PainterThread( null ), new double[] { 1 }, 0, false, 1, null, false,
				viewer.getOptionValues().getAccumulateProjectorFactory(), new CacheControl.Dummy() );
		setProgress(0);
		for ( int timepoint = minTimepointIndex; timepoint <= maxTimepointIndex; ++timepoint )
		{
			// stop recording if requested
			if(!isRecordThreadRunning)
				break;
			
			final AffineTransform3D affine = getTransformation(renderState, canvasW, canvasH, timepoint);
			affine.scale( ( double ) width / canvasW );
			affine.set( affine.get( 0, 3 ) + width / 2, 0, 3 );
			affine.set( affine.get( 1, 3 ) + height / 2, 1, 3 );
			renderState.setViewerTransform( affine );
			
			renderState.setCurrentTimepoint( timepoint );
			renderer.requestRepaint();
			renderer.paint( renderState );

			if ( Prefs.showScaleBarInMovie() )
			{
				final Graphics2D g2 = target.bi.createGraphics();
				g2.setClip( 0, 0, width, height );
				scalebar.setViewerState( renderState );
				scalebar.paint( g2 );
			}

			ImageIO.write( target.bi, "png", new File( String.format( "%s/img-%03d.png", dir, timepoint ) ) );
			setProgress(( double ) (timepoint - minTimepointIndex + 1) / (maxTimepointIndex - minTimepointIndex + 1));
		}
	}
	
	private synchronized  void setProgress(double progress){
		progressWriter.setProgress( progress );
		progressBar.setValue((int) (progress * 100));
	}

	@Override
	public void drawOverlays( final Graphics g )
	{}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		spinnerWidth.setValue( width );
		spinnerHeight.setValue( height );
	}
	
	private AffineTransform3D getTransformation(final ViewerState renderState, final int canvasW, final int canvasH, final int currentTimepoint){
		
		if(renderState.getActiveBookmark() instanceof DynamicBookmark){
			final DynamicBookmark dynamicBookmark = (DynamicBookmark)renderState.getActiveBookmark();
			final AffineTransform3D affine = dynamicBookmark.getInterpolatedTransform(currentTimepoint, canvasW / 2, canvasH);
			return affine;
		}
		else{
			final AffineTransform3D affine = new AffineTransform3D();
			renderState.getViewerTransform( affine );
			affine.set( affine.get( 0, 3 ) - canvasW / 2, 0, 3 );
			affine.set( affine.get( 1, 3 ) - canvasH / 2, 1, 3 );
			return affine;
		}
	}
}

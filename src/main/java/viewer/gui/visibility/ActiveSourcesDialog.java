package viewer.gui.visibility;

import static viewer.render.DisplayMode.FUSED;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import viewer.VisibilityAndGrouping;
import viewer.VisibilityAndGrouping.Event;
import viewer.render.SourceGroup;

public class ActiveSourcesDialog extends JDialog
{
	private final VisibilityAndGrouping visibilityAndGrouping;

	public ActiveSourcesDialog( final Frame owner, final VisibilityAndGrouping visibilityAndGrouping )
	{
		super( owner, "visibility and grouping", false );

		this.visibilityAndGrouping = visibilityAndGrouping;

		final VisibilityPanel visibilityPanel = new VisibilityPanel( visibilityAndGrouping );
		visibilityAndGrouping.addUpdateListener( visibilityPanel );
		visibilityPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"visibility" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		getContentPane().add( visibilityPanel, BorderLayout.NORTH );

		final GroupingPanel groupingPanel = new GroupingPanel( visibilityAndGrouping );
		visibilityAndGrouping.addUpdateListener( groupingPanel );
		groupingPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"grouping" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		getContentPane().add( groupingPanel, BorderLayout.SOUTH );

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
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		pack();
		setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
	}

	public static class VisibilityPanel extends JPanel implements VisibilityAndGrouping.UpdateListener
	{
		private final VisibilityAndGrouping visibility;

		private final ArrayList< JRadioButton > currentButtons;

		private final ArrayList< JCheckBox > fusedBoxes;

		private final ArrayList< JCheckBox > visibleBoxes;

		public VisibilityPanel( final VisibilityAndGrouping visibilityAndGrouping )
		{
			super( new GridBagLayout() );
			this.visibility = visibilityAndGrouping;
			currentButtons = new ArrayList< JRadioButton >();
			fusedBoxes = new ArrayList< JCheckBox >();
			visibleBoxes = new ArrayList< JCheckBox >();

			final int numSources = visibilityAndGrouping.numSources();
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets( 0, 5, 0, 5 );

			// source names
			c.gridx = 0;
			c.gridy = 0;
			add( new JLabel( "source" ), c );
			c.anchor = GridBagConstraints.LINE_END;
			c.gridy = GridBagConstraints.RELATIVE;
			for ( int i = 0; i < numSources; ++i )
				add( new JLabel( visibilityAndGrouping.getSources().get( i ).getSpimSource().getName() ), c );

			// "current" radio-buttons
			c.anchor = GridBagConstraints.CENTER;
			c.gridx = 1;
			c.gridy = 0;
			add( new JLabel( "current" ), c );
			c.gridy = GridBagConstraints.RELATIVE;
			final ButtonGroup currentButtonGroup = new ButtonGroup();
			for ( int i = 0; i < numSources; ++i )
			{
				final JRadioButton b = new JRadioButton();
				final int sourceIndex = i;
				b.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						if ( b.isSelected() )
							visibility.setCurrentSource( sourceIndex );
					}
				} );
				currentButtons.add( b );
				currentButtonGroup.add( b );
				add( b, c );
			}

			// "active in fused" check-boxes
			c.gridx = 2;
			c.gridy = 0;
			add( new JLabel( "active in fused" ), c );
			c.gridy = GridBagConstraints.RELATIVE;
			for ( int i = 0; i < numSources; ++i )
			{
				final JCheckBox b = new JCheckBox();
				final int sourceIndex = i;
				b.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						visibility.setActive( sourceIndex, FUSED, b.isSelected() );
					}
				} );
				fusedBoxes.add( b );
				add( b, c );
			}

			// "currently visible" check-boxes
			c.gridx = 3;
			c.gridy = 0;
			add( new JLabel( "visible" ), c );
			c.gridy = GridBagConstraints.RELATIVE;
			for ( int i = 0; i < numSources; ++i )
			{
				final JCheckBox b = new JCheckBox();
				visibleBoxes.add( b );
				b.setEnabled( false );
				add( b, c );
			}

			update();
		}

		protected void update()
		{
			synchronized ( visibility )
			{
				final int n = visibility.numSources();
				currentButtons.get( visibility.getCurrentSource() ).setSelected( true );
				for ( int i = 0; i < n; ++i )
				{
					fusedBoxes.get( i ).setSelected( visibility.isActive( i, FUSED ) );
					visibleBoxes.get( i ).setSelected( visibility.isVisible( i ) );
				}
			}
		}

		@Override
		public void visibilityChanged( final Event e )
		{
			switch ( e.id )
			{
			case Event.ACTIVATE:
				if ( e.displayMode == FUSED )
					fusedBoxes.get( e.sourceIndex ).setSelected( true );
				if ( e.displayMode == visibility.getDisplayMode() )
					visibleBoxes.get( e.sourceIndex ).setSelected( true );
				break;
			case Event.DEACTIVATE:
				if ( e.displayMode == FUSED )
					fusedBoxes.get( e.sourceIndex ).setSelected( false );
				if ( e.displayMode == visibility.getDisplayMode() )
					visibleBoxes.get( e.sourceIndex ).setSelected( false );
				break;
			case Event.MAKE_CURRENT:
				currentButtons.get( e.sourceIndex ).setSelected( true );
				break;
			case Event.DISPLAY_MODE_CHANGED:
				update();
			default:
			}
		}
	}

	public static class GroupingPanel extends JPanel implements VisibilityAndGrouping.UpdateListener
	{
		private final VisibilityAndGrouping visibility;

		private final ArrayList< JTextField > nameFields;

		private final ArrayList< JCheckBox > assignBoxes;

		private final JCheckBox groupingBox;

		private final int numSources;

		private final int numGroups;

		public GroupingPanel( final VisibilityAndGrouping visibilityAndGrouping )
		{
			super( new GridBagLayout() );
			this.visibility = visibilityAndGrouping;
			nameFields = new ArrayList< JTextField >();
			assignBoxes = new ArrayList< JCheckBox >();
			numSources = visibilityAndGrouping.numSources();
			numGroups = visibilityAndGrouping.numGroups();

			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets( 0, 5, 0, 5 );

			final List< SourceGroup > groups = visibility.getSourceGroups();

			// source shortcuts
			// TODO: shortcut "names" should not be hard-coded here!
			c.gridx = 0;
			c.gridy = 0;
			add( new JLabel( "shortcut" ), c );
			c.anchor = GridBagConstraints.LINE_END;
			c.gridy = GridBagConstraints.RELATIVE;
			final int nShortcuts = Math.min( numGroups, 10 );
			for ( int i = 0; i < nShortcuts; ++i )
				add( new JLabel( Integer.toString( i == 10 ? 0 : i + 1 ) ), c );

			// source names
			c.gridx = 1;
			c.gridy = 0;
			c.anchor = GridBagConstraints.CENTER;
			add( new JLabel( "group name" ), c );
			c.anchor = GridBagConstraints.LINE_END;
			c.gridy = GridBagConstraints.RELATIVE;
			for ( int g = 0; g < numGroups; ++g )
			{
				final SourceGroup group = groups.get( g );
				final JTextField tf = new JTextField( group.getName(), 10 );
				tf.getDocument().addDocumentListener( new DocumentListener()
				{
					private void doit()
					{
						group.setName( tf.getText() );
					}

					@Override
					public void removeUpdate( final DocumentEvent e )
					{
						doit();
					}

					@Override
					public void insertUpdate( final DocumentEvent e )
					{
						doit();
					}

					@Override
					public void changedUpdate( final DocumentEvent e )
					{
						doit();
					}
				} );
				nameFields.add( tf );
				add( tf, c );
			}

			// setup-to-group assignments
			c.gridx = 2;
			c.gridy = 0;
			c.gridwidth = numSources;
			c.anchor = GridBagConstraints.CENTER;
			add( new JLabel( "assigned sources" ), c );
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.LINE_END;
			for ( int s = 0; s < numSources; ++s )
			{
				final int sourceIndex = s;
				c.gridx = sourceIndex + 2;
				for ( int g = 0; g < numGroups; ++g )
				{
					final SourceGroup group = groups.get( g );
					c.gridy = g + 1;
					final JCheckBox b = new JCheckBox();
					b.addActionListener( new ActionListener()
					{
						@Override
						public void actionPerformed( final ActionEvent e )
						{
							if ( b.isSelected() )
								group.addSource( sourceIndex );
							else
								group.removeSource( sourceIndex );
						}
					} );
					assignBoxes.add( b );
					add( b, c );
				}
			}

			final JPanel panel = new JPanel();
			panel.setLayout(  new BoxLayout( panel, BoxLayout.LINE_AXIS ) );
			groupingBox = new JCheckBox();
			groupingBox.setSelected( visibility.isGroupingEnabled() );
			groupingBox.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					visibility.setGroupingEnabled( groupingBox.isSelected() );
				}
			} );
			panel.add( groupingBox );
			panel.add( new JLabel("enable grouping") );

			c.gridx = 0;
			c.gridy = numGroups + 1;
			c.gridwidth = 2 + numSources;
			c.anchor = GridBagConstraints.CENTER;
			add( panel, c );

			update();
		}

		protected void update()
		{
			final List< SourceGroup > groups = visibility.getSourceGroups();
			for ( int i = 0; i < numGroups; ++i )
				nameFields.get( i ).setText( groups.get( i ).getName() );

			for ( int s = 0; s < numSources; ++s )
				for ( int g = 0; g < numGroups; ++g )
					assignBoxes.get( s * numGroups + g ).setSelected( groups.get( g ).getSourceIds().contains( s ) );
		}

		@Override
		public void visibilityChanged( final Event e )
		{
			switch ( e.id )
			{
			case Event.GROUPING_ENABLED_CHANGED:
				groupingBox.setSelected( visibility.isGroupingEnabled() );
			default:
			}
		}
	}

}

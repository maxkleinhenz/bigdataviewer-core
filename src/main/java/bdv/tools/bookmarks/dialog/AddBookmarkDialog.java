package bdv.tools.bookmarks.dialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bdv.MaxLengthTextDocument;
import bdv.tools.bookmarks.editor.BookmarksEditor;

public class AddBookmarkDialog extends JDialog
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final JRadioButton simpleBookmarkOption;

	private final JRadioButton dynamicBookmarkOption;

	private final BookmarksEditor bookmarksEditor;

	private JTextField titleField;

	private JTextArea descriptionArea;

	private JTextField keyField;

	public AddBookmarkDialog( final Frame owner, final BookmarksEditor bookmarksEditor )
	{
		super( owner, "Add bookmark", true );
		setResizable( false );
		setPreferredSize( new Dimension( 250, 300 ) );
		setLocationRelativeTo( owner );

		this.bookmarksEditor = bookmarksEditor;
		getContentPane().setLayout( new BoxLayout( getContentPane(), BoxLayout.Y_AXIS ) );

		JPanel bookmarkPanel = new JPanel();
		bookmarkPanel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		GridBagLayout gbl_bookmarkPanel = new GridBagLayout();
		gbl_bookmarkPanel.columnWidths = new int[] { 190, 1, 0 };
		gbl_bookmarkPanel.rowHeights = new int[] { 0, 0, 0, 50 };
		gbl_bookmarkPanel.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gbl_bookmarkPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0 };

		GridBagConstraints gbc_bookmarkPanel = new GridBagConstraints();
		gbc_bookmarkPanel.anchor = GridBagConstraints.NORTH;
		gbc_bookmarkPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_bookmarkPanel.insets = new Insets( 5, 5, 5, 5 );
		gbc_bookmarkPanel.gridx = 0;
		gbc_bookmarkPanel.gridy = 0;
		getContentPane().add( bookmarkPanel, gbc_bookmarkPanel );

		bookmarkPanel.setLayout( gbl_bookmarkPanel );

		JLabel lblTitle = new JLabel( "Title" );
		GridBagConstraints gbc_lblTitle = new GridBagConstraints();
		gbc_lblTitle.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblTitle.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblTitle.gridx = 0;
		gbc_lblTitle.gridy = 0;
		bookmarkPanel.add( lblTitle, gbc_lblTitle );

		JLabel keyLabel = new JLabel( "Key*" );
		GridBagConstraints gbc_keyLabel = new GridBagConstraints();
		gbc_keyLabel.anchor = GridBagConstraints.WEST;
		gbc_keyLabel.insets = new Insets( 0, 0, 5, 0 );
		gbc_keyLabel.gridx = 1;
		gbc_keyLabel.gridy = 0;
		gbc_keyLabel.weightx = 1;
		bookmarkPanel.add( keyLabel, gbc_keyLabel );

		titleField = new JTextField();
		lblTitle.setLabelFor( titleField );
		titleField.setText( ( String ) null );
		titleField.setColumns( 10 );
		GridBagConstraints gbc_titleField = new GridBagConstraints();
		gbc_titleField.fill = GridBagConstraints.HORIZONTAL;
		gbc_titleField.insets = new Insets( 0, 0, 5, 5 );
		gbc_titleField.gridx = 0;
		gbc_titleField.gridy = 1;
		gbc_titleField.weightx = 1;
		bookmarkPanel.add( titleField, gbc_titleField );
		titleField.getDocument().addDocumentListener( new DocumentListener()
		{

			@Override
			public void changedUpdate( DocumentEvent arg0 )
			{
				setKeyFieldText();
			}

			@Override
			public void insertUpdate( DocumentEvent arg0 )
			{
				setKeyFieldText();
			}

			@Override
			public void removeUpdate( DocumentEvent arg0 )
			{
				setKeyFieldText();
			}

			private void setKeyFieldText()
			{
				if ( !titleField.getText().isEmpty() )
				{
					keyField.setText( titleField.getText().substring( 0, 1 ) );
				}
			}

		} );

		keyField = new JTextField();
		GridBagConstraints gbc_keyField = new GridBagConstraints();
		gbc_keyField.insets = new Insets( 0, 0, 5, 0 );
		gbc_keyField.fill = GridBagConstraints.HORIZONTAL;
		gbc_keyField.gridx = 1;
		gbc_keyField.gridy = 1;
		bookmarkPanel.add( keyField, gbc_keyField );
		keyField.setColumns( 10 );
		keyField.setDocument( new MaxLengthTextDocument( 1 ) );

		JLabel lblDescription = new JLabel( "Description" );
		GridBagConstraints gbc_lblDescription = new GridBagConstraints();
		gbc_lblDescription.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblDescription.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblDescription.gridx = 0;
		gbc_lblDescription.gridy = 2;
		bookmarkPanel.add( lblDescription, gbc_lblDescription );

		JScrollPane descriptionScrollPane = new JScrollPane();
		GridBagConstraints gbc_descriptionScrollPane = new GridBagConstraints();
		gbc_descriptionScrollPane.gridwidth = 2;
		gbc_descriptionScrollPane.insets = new Insets( 0, 0, 5, 0 );
		gbc_descriptionScrollPane.fill = GridBagConstraints.BOTH;
		gbc_descriptionScrollPane.gridx = 0;
		gbc_descriptionScrollPane.gridy = 3;
		bookmarkPanel.add( descriptionScrollPane, gbc_descriptionScrollPane );

		descriptionArea = new JTextArea();
		descriptionArea.setLineWrap( true );
		descriptionArea.setFocusTraversalKeys( KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null );
		descriptionArea.setFocusTraversalKeys( KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null );
		descriptionScrollPane.setViewportView( descriptionArea );

		JLabel typeLabel = new JLabel( "Type" );
		GridBagConstraints gbc_typeLabel = new GridBagConstraints();
		gbc_typeLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_typeLabel.insets = new Insets( 0, 0, 5, 5 );
		gbc_typeLabel.gridx = 0;
		gbc_typeLabel.gridy = 4;
		bookmarkPanel.add( typeLabel, gbc_typeLabel );

		ButtonGroup group = new ButtonGroup();

		simpleBookmarkOption = new JRadioButton( "Simple bookmark" );
		simpleBookmarkOption.setSelected( true );
		GridBagConstraints gbc_simpleBookmarkOption = new GridBagConstraints();
		gbc_simpleBookmarkOption.fill = GridBagConstraints.HORIZONTAL;
		gbc_simpleBookmarkOption.insets = new Insets( 0, 0, 5, 5 );
		gbc_simpleBookmarkOption.gridx = 0;
		gbc_simpleBookmarkOption.gridy = 5;
		bookmarkPanel.add( simpleBookmarkOption, gbc_simpleBookmarkOption );
		group.add( simpleBookmarkOption );

		dynamicBookmarkOption = new JRadioButton( "Dynamic bookmark" );
		GridBagConstraints gbc_dynamicBookmarkOption = new GridBagConstraints();
		gbc_dynamicBookmarkOption.fill = GridBagConstraints.HORIZONTAL;
		gbc_dynamicBookmarkOption.insets = new Insets( 0, 0, 0, 5 );
		gbc_dynamicBookmarkOption.gridx = 0;
		gbc_dynamicBookmarkOption.gridy = 6;

		if ( bookmarksEditor.areDynamicBookmarksEnabled() )
		{
			bookmarkPanel.add( dynamicBookmarkOption, gbc_dynamicBookmarkOption );
			group.add( dynamicBookmarkOption );
		}

		JPanel buttonPanel = new JPanel();
		getContentPane().add( buttonPanel );
		buttonPanel.setLayout( new FlowLayout( FlowLayout.RIGHT, 5, 5 ) );

		JButton cancelButton = new JButton( "Cancel" );
		buttonPanel.add( cancelButton );
		cancelButton.addActionListener( new ActionListener()
		{

			@Override
			public void actionPerformed( ActionEvent e )
			{
				dispose();
			}
		} );

		JButton addButton = new JButton( "Add" );
		buttonPanel.add( addButton );
		addButton.addActionListener( new AbstractAction()
		{
			@Override
			public void actionPerformed( ActionEvent arg0 )
			{
				addBookmark();
			}
		} );

		addComponentListener( new ComponentAdapter()
		{

			@Override
			public void componentShown( ComponentEvent e )
			{
				reset();
			}
		} );

		setKeyBinding();
		this.getRootPane().setDefaultButton( addButton );

		pack();
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}

	private void setKeyBinding()
	{
		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );

		// close dialog
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

	}

	private boolean isInputValid()
	{

		String key = keyField.getText();

		if ( key.length() == 0 )
		{
			return false;
		}
		else if ( key.length() > 1 )
		{
			JOptionPane.showMessageDialog( this, "Please enter a key with only 1 character.", "Key is too long",
					JOptionPane.INFORMATION_MESSAGE );
			return false;
		}

		if ( bookmarksEditor.containsBookmark( key ) )
		{

			String message = "The key '" + key
					+ "' is already given to another bookmark. Please choose a different key.";

			JOptionPane.showMessageDialog( this, message, "Key is already in use", JOptionPane.INFORMATION_MESSAGE );
			return false;
		}

		return true;
	}

	private void addBookmark()
	{
		if ( !isInputValid() )
			return;

		if ( simpleBookmarkOption.isSelected() )
		{
			bookmarksEditor.createSimpleBookmark( keyField.getText(), titleField.getText(), descriptionArea.getText() );
		}
		else if ( dynamicBookmarkOption.isSelected() )
		{
			bookmarksEditor.createDynamicBookmark( keyField.getText(), titleField.getText(), descriptionArea.getText() );
		}

		dispose();
	}

	private void reset()
	{
		titleField.setText( "" );
		descriptionArea.setText( "" );
		keyField.setText( "" );
		simpleBookmarkOption.setSelected( true );
	}

}

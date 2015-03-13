import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import java.util.*;
import java.net.URL;
import javax.imageio.ImageIO;

public class AsmEdit extends JFrame
{
	// fields
	private JTextComponent editArea;
	private File openedFile;
	private boolean fileDirty;
	private UndoManager undoManager;
	private VirtualMachine vm;
	private FileNameExtensionFilter fileFilter;
	
	private final static String FileTypeName = "Assembly Code File";
	private final static String FileExt = "assem";
	
	private AsmDocument doc;
	private JTable table;
	
	public static void main( String[] args )
	{
		AsmEdit w = new AsmEdit();
		w.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
		w.setVisible( true );
	}
	
	protected JButton makeToolbarButton( String imageName, String actionCommand, String toolTipText, String altText )
	{
		//Look for the image.
		String imgLocation = "images/" + imageName + ".png";
		
		BufferedImage img = null;
		Image im = null;
		try
		{
			img = ImageIO.read( new File( imgLocation ) );
			im = img.getScaledInstance( 24, 24, Image.SCALE_FAST );
		}
		catch( IOException e )
		{
		}
		
		//Create and initialize the button.
		JButton button = new JButton();
		button.setActionCommand( actionCommand );
		button.setToolTipText( toolTipText );

		// TODO: Setup this next line properly so the toolbar buttons can be used
		//button.addActionListener( this );

		//image found
		if( img != null )
		{
			button.setIcon( new ImageIcon( im ) );
		}
		//no image found
		else
		{
			button.setText( altText );
			System.err.println( "Resource not found: " + imgLocation );
		}

		return button;
	}
    
	// constructor
	public AsmEdit()
	{
		super( "Assembly Editor" );
		setOpenedFile( null );
		
		undoManager = new UndoManager();
		
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosing( WindowEvent e )
				{
					tryExit();
				}
			}
		);
		
		fileFilter = new FileNameExtensionFilter( FileTypeName, FileExt );
        
        JToolBar toolBar = new JToolBar( "Default Toolbar" );
		toolBar.add( makeToolbarButton( "icon-new", "btnNew", "New File", "New" ) );
		toolBar.add( makeToolbarButton( "icon-open", "btnOpen", "Open File", "Open" ) );
		toolBar.add( makeToolbarButton( "icon-save", "btnSave", "Save File", "Save" ) );
		toolBar.add( makeToolbarButton( "icon-run", "btnRun", "Run Program", "Run" ) );
		toolBar.add( makeToolbarButton( "icon-play", "btnContinue", "Continue Program", "Continue" ) );
		toolBar.add( makeToolbarButton( "icon-next", "btnNext", "Step Program", "Step" ) );

		vm = new VirtualMachine( System.out, System.in );

		// Temporary test for breakpoints (not working yet)
		ArrayList<Integer> bps = new ArrayList<Integer>();
		bps.add( 3 );
		bps.add( 5 );
		vm.setBreakpoints( bps );
		
		// Setup keyword highlighting
		// This is really unfinished, needs a lot of work!!! I dislike how this is setup right now
		java.util.List<String> opcodes = vm.getOpcodeStrings();
		final StyleContext cont = StyleContext.getDefaultStyleContext();
		final AttributeSet attrOpcode = cont.addAttribute( cont.getEmptySet(), StyleConstants.Foreground, Color.BLUE );
		final AttributeSet attrPlain = cont.addAttribute( cont.getEmptySet(), StyleConstants.Foreground, Color.BLACK );
		final AttributeSet attrDebugBackground = cont.addAttribute( cont.getEmptySet(), StyleConstants.Background, Color.RED );
		final AttributeSet attrPlainBg = cont.addAttribute( cont.getEmptySet(), StyleConstants.Background, Color.WHITE );

		ArrayList<java.util.List<String> > keywordSet = new ArrayList<java.util.List<String> >();
		keywordSet.add( opcodes );
		ArrayList<AttributeSet> attrs = new ArrayList<AttributeSet>();
		attrs.add( attrOpcode );

		doc = new AsmDocument( attrPlain, keywordSet, attrs, attrDebugBackground, attrPlainBg );

		JPanel gui = new JPanel( new BorderLayout() );

		// Create the asm edit area
		editArea = new JTextPane( doc );
		editArea.getDocument().addUndoableEditListener( undoManager );
		editArea.getDocument().addDocumentListener(
			new DocumentListener()
			{
				public void removeUpdate( DocumentEvent e )
				{
					setFileDirty( true );
				}
				
				public void insertUpdate( DocumentEvent e )
				{
					setFileDirty( true );
				}
				
				public void changedUpdate( DocumentEvent e )
				{
				}
			}
		);

		JScrollPane scrollEditArea = new JScrollPane( editArea );
		
		JTextArea txtOutput = new JTextArea();
		JScrollPane scrollOutput = new JScrollPane( txtOutput );

		// This contains the two main panels for the left side of the program
		JSplitPane splitEditOutput = new JSplitPane( JSplitPane.VERTICAL_SPLIT, scrollEditArea, scrollOutput );

		// Create the debug area panel
		JPanel debugPanel = new JPanel( new BorderLayout() );

		// Memory table display
		DefaultTableModel model = new DefaultTableModel( new String[0][2], new String[]{ "Address", "Value" } );
		table = new JTable( model );
		JScrollPane memScrollPane = new JScrollPane(table);
		
		// TODO: Add other debug displays here (stack)
		debugPanel.add( memScrollPane );

		// Split pane that separates the edit area from the debug area
		JSplitPane splitEditDebug = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, splitEditOutput, debugPanel );
		gui.add( splitEditDebug );

		// TODO: Re-enable the toolbar once we write the code to hook up the buttons
		//gui.add( toolBar, BorderLayout.PAGE_START );
		setContentPane( gui );
		pack();
		setSize( 600, 600 );
		splitEditOutput.setDividerLocation( 0.90 );
		splitEditDebug.setDividerLocation( 0.80 );
		
		// Set the virtual machine's output stream to go to the output text area
		// By default the virtual machine will print to System.out but we will redirect that to the output text area
		// TODO: Do this in the constructor instead of right here, there is no reason not to
		vm.setOutput( new PrintStream( new TextAreaOutputStream( txtOutput ) ) );
        
        // Menu Bar	
		JMenuBar menuBar = new JMenuBar();
		
		// File Menu
		JMenu fileMenu = new JMenu( "File" );
		JMenuItem newItem = new JMenuItem( "New" );
		newItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
		newItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					tryNew();
				}
			}
		);
		
		JMenuItem openItem = new JMenuItem( "Open" );
		openItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
		openItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					tryOpen();
				}
			}
		);
		
		JMenuItem saveItem = new JMenuItem( "Save" );
		saveItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
		saveItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					save( false );
				}
			}
		);
		
		JMenuItem saveAsItem = new JMenuItem( "Save As" );
		saveAsItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | Event.SHIFT_MASK ) );
		saveAsItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					save( true );
				}
			}
		);
		
		JMenuItem exitItem = new JMenuItem( "Exit" );
		exitItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					tryExit();
				}
			}
		);
		
		fileMenu.add( newItem );
		fileMenu.add( openItem );
		fileMenu.add( saveItem );
		fileMenu.add( saveAsItem );
		fileMenu.add( exitItem );
		
		menuBar.add( fileMenu );
		
		// Edit Menu
		JMenu editMenu = new JMenu( "Edit" );
		
		JMenuItem undoItem = new JMenuItem( "Undo" );
		undoItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
		undoItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					try
					{
						undoManager.undo();
					}
					catch( CannotUndoException ex )
					{
					}
				}
			}
		);
		
		JMenuItem redoItem = new JMenuItem( "Redo" );
		redoItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | Event.SHIFT_MASK ) );
		redoItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					try
					{
						undoManager.redo();
					}
					catch( CannotRedoException ex )
					{
					}
				}
			}
		);
		
		JMenuItem cutItem = new JMenuItem( "Cut" );
		cutItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
		cutItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					editArea.cut();
				}
			}
		);
		
		JMenuItem copyItem = new JMenuItem( "Copy" );
		copyItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
		copyItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					editArea.copy();
				}
			}
		);
		
		JMenuItem pasteItem = new JMenuItem( "Paste" );
		pasteItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
		pasteItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					editArea.paste();
				}
			}
		);
		
		JMenuItem runItem = new JMenuItem( "Run" );
		runItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) );
		runItem.addActionListener(
			new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					run();
				}
			}
		);
		
		editMenu.add( undoItem );
		editMenu.add( redoItem );
		editMenu.addSeparator();
		editMenu.add( cutItem );
		editMenu.add( copyItem );
		editMenu.add( pasteItem );
		editMenu.add( runItem );
		
		menuBar.add( editMenu );
		
		setJMenuBar( menuBar );

		// TODO: Remove these debug lines
		/*
		tryOpen();

		for( Integer i : bps )
		{
			doc.addBreakpoint( i );
		}
		*/
	}
	
	// mutators
	private void setOpenedFile( File value )
	{
		openedFile = value;
		
		if( openedFile != null )
		{
			setTitle( (fileDirty ? "*" : "") + openedFile.getName() );
		}
		else
		{
			setTitle( (fileDirty ? "*" : "") + "new" );
		}
	}
	
	private void setFileDirty( boolean value )
	{
		fileDirty = value;
		setOpenedFile( openedFile );
	}
	
	// methods
	private boolean save( boolean forceSaveDialog )
	{
		File writeFile = null;
		boolean hasFile = false;
		boolean success = false;
		
		if( openedFile == null || forceSaveDialog )
		{
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter( fileFilter );
			
			if( openedFile != null )
			{
				// only runs when forceSaveDialog
				fc.setSelectedFile( openedFile );
			}
			
			int returnVal = fc.showSaveDialog( AsmEdit.this );
			
			hasFile = (returnVal == JFileChooser.APPROVE_OPTION);
			writeFile = fc.getSelectedFile();
		}
		else
		{
			hasFile = true;
			writeFile = openedFile;
		}
		
		if( hasFile )
		{
			try
			{
				BufferedWriter w = new BufferedWriter( new FileWriter( writeFile ) );
				
				w.write( editArea.getText() );
				
				w.close();
				
				setOpenedFile( writeFile );
				setFileDirty( false );
				success = true;
			}
			catch( FileNotFoundException ex )
			{
			}
			catch( IOException ex )
			{
			}
		}
		
		return success;
	}
	
	private boolean trySave()
	{
		boolean returnValue = true;
		
		if( fileDirty )
		{
			int result = JOptionPane.showConfirmDialog( AsmEdit.this, "Do you want to save your file?", "Save", JOptionPane.YES_NO_CANCEL_OPTION );

			if( result == JOptionPane.YES_OPTION )
			{
				// user wants to save
				boolean saveSuccessful = save( false );
				
				if( !saveSuccessful )
				{
					returnValue = false;
				}
			}
			else if( result == JOptionPane.NO_OPTION )
			{
				// user doesnt want to save
			}
			else
			{
				// user is cancelling
				returnValue = false;
			}
		}
		
		return returnValue;
	}
	
	private void tryNew()
	{
		if( trySave() )
		{
			editArea.setText( "" );
			setOpenedFile( null );
			setFileDirty( false );
			undoManager.discardAllEdits();
		}
	}
	
	private void tryOpen()
	{
		if( trySave() )
		{
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter( fileFilter );
		
			int returnVal = fc.showOpenDialog( AsmEdit.this );
		
			if( returnVal == JFileChooser.APPROVE_OPTION )
			{
				//JOptionPane.showMessageDialog( MenuTest.this, "You chose file: " + fc.getSelectedFile().getName() );
				try
				{
					BufferedReader r = new BufferedReader( new FileReader( fc.getSelectedFile() ) );
				
					StringBuilder sb = new StringBuilder();
					String line = r.readLine();
				
					while( line != null )
					{
						sb.append( line );
						line = r.readLine();
						if( line != null )
						{
							sb.append( '\n' );
						}
					}
					
					r.close();
				
					editArea.setText( sb.toString() );
					setOpenedFile( fc.getSelectedFile() );
					
					// cleanup program state
					setFileDirty( false );
					undoManager.discardAllEdits();
				}
				catch( FileNotFoundException ex )
				{
				}
				catch( IOException ex )
				{
				}
			}
		}
	}
	
	private void tryExit()
	{
		if( trySave() )
		{
			System.exit(0);
		}
	}
	
	private void run()
	{
		BufferedReader r = new BufferedReader( new StringReader( editArea.getText() ) );
		vm.compile( r );

		int[] memory = vm.getMemory();
		DefaultTableModel model = (DefaultTableModel)table.getModel();

		for( int i = 0; i < memory.length; ++i )
		{
			model.addRow( new Object[]{ i, memory[i] } );
		}

		vm.reset();
		vm.run( true );
	}
}
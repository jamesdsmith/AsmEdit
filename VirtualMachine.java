import java.util.*;
import java.io.*;

public class VirtualMachine
{
	private int[] memory;	
	private ListStack<Integer> stack;
	private int pc;
	private int ir;
	private Scanner kb;
	private Hashtable<String,Integer> opcodeTable;
	private PrintStream stdout;
	private InputStream stdin;
	private boolean halted;
	private List<Integer> breakpoints;
	
	public VirtualMachine( PrintStream out, InputStream in )
	{
		stack = new ListStack<Integer>();
		stdout = out;
		stdin = in;
		kb = new Scanner( stdin );
		pc = 0;
		halted = false;

		opcodeTable = new Hashtable<String,Integer>();
		opcodeTable.put( "HALT", OPCODE_HALT );
		opcodeTable.put( "CONST", OPCODE_CONST );
		opcodeTable.put( "LOAD", OPCODE_LOAD );
		opcodeTable.put( "STO", OPCODE_STO );
		opcodeTable.put( "ADD", OPCODE_ADD );
		opcodeTable.put( "SUB", OPCODE_SUB );
		opcodeTable.put( "MUL", OPCODE_MUL );
		opcodeTable.put( "DIV", OPCODE_DIV );
		opcodeTable.put( "EQL", OPCODE_EQL );
		opcodeTable.put( "LSS", OPCODE_LSS );
		opcodeTable.put( "GTR", OPCODE_GTR );
		opcodeTable.put( "JMP", OPCODE_JMP );
		opcodeTable.put( "FJMP", OPCODE_FJMP );
		opcodeTable.put( "READ", OPCODE_READ );
		opcodeTable.put( "WRITE", OPCODE_WRITE );
		opcodeTable.put( "CALL", OPCODE_CALL );
		opcodeTable.put( "RET", OPCODE_RET );
		
		//compile( new BufferedReader( new FileReader( new File( "program.assem" ) ) ) );
	}
	
	public void run( boolean withDebug )
	{
		if( !withDebug )
		{
			while( true && !halted )
			{
				ir = memory[pc];
				pc++;
				decodeAndExecute();
			}
		}
		else
		{
			while( true && !halted )
			{
				ir = memory[pc];
				pc++;
				decodeAndExecute();

				// TODO: This is kind of slow so eventually we should just keep track of what the next breakpoint should be
				if( breakpoints.contains( pc ) )
				{
					//pause();
					//System.out.println( "Breakpoint hit at " + pc );
				}
			}
		}

	}
	
	public void reset()
	{
		halted = false;
		pc = 0;
	}

	public void pause()
	{
		halted = true;
	}

	public void play()
	{
		halted = false;
	}

	public void setBreakpoints( List<Integer> newBreakpoints )
	{
		breakpoints = newBreakpoints;
	}
	
	private void decodeAndExecute()
	{
		int x, y, a;
		switch( ir )
		{
			case OPCODE_HALT:
				halted = true;
				break;
				
			case OPCODE_CONST:
				stack.push( memory[pc] );
				pc++;
				break;
			
			case OPCODE_LOAD:
				a = memory[pc];
				stack.push( memory[a] );
				pc++;
				break;
			
			case OPCODE_STO:
				a = memory[pc];
				memory[a] = stack.pop();
				pc++;
				break;
				
			case OPCODE_ADD:
				y = stack.pop();
				x = stack.pop();
				stack.push( x + y );
				break;
				
			case OPCODE_SUB:
				y = stack.pop();
				x = stack.pop();
				stack.push( x - y );
				break;
			
			case OPCODE_MUL:
				y = stack.pop();
				x = stack.pop();
				stack.push( x * y );
				break;
			
			case OPCODE_DIV:
				y = stack.pop();
				x = stack.pop();
				stack.push( x / y );
				break;
			
			case OPCODE_EQL:
				y = stack.pop();
				x = stack.pop();
				if( x == y )
				{
					stack.push( 1 );
				}
				else
				{
					stack.push( 0 );
				}
				break;
			
			case OPCODE_LSS:
				y = stack.pop();
				x = stack.pop();
				if( x < y )
				{
					stack.push( 1 );
				}
				else
				{
					stack.push( 0 );
				}
				break;
			
			case OPCODE_GTR:
				y = stack.pop();
				x = stack.pop();
				if( x > y )
				{
					stack.push( 1 );
				}
				else
				{
					stack.push( 0 );
				}
				break;
			
			case OPCODE_JMP:
				a = memory[pc];
				pc = a;
				break;
			
			case OPCODE_FJMP:
				a = memory[pc];
				if( stack.pop() == 0 )
				{
					pc = a;
				}
				else
				{
					pc++;
				}
				break;
			
			case OPCODE_READ:
				stack.push( kb.nextInt() );
				break;
				
			case OPCODE_WRITE:
				stdout.println( stack.pop() );
				break;
			
			case OPCODE_CALL:
				a = memory[pc];
				stack.push( pc + 1 );
				pc = a;
				break;
			
			case OPCODE_RET:
				pc = stack.pop();
				break;			
				
			default:
				stdout.println( "Unrecognized opcode " + ir + ", terminating..." );
				System.exit( -1 );
				break;
		}
	}
	
	public void compile( BufferedReader r )
	{
		try
		{
			String line = r.readLine();
			
			// data structures used to build the program
			List<Integer> program = new ArrayList<Integer>();
			Hashtable<String, List<Integer>> symbols = new Hashtable<String,List<Integer>>();
			Hashtable<String, Integer> adresses = new Hashtable<String,Integer>();
			
			// we will read through the whole file, building the bytecode as we go and saving
			// the locations of addresses to fill in at the end
			while( line != null )
			{
				// trim leading/trailing whitespace, and split the line up into words at spaces
				String[] tokens = line.trim().split(" ");
				
				if( opcodeTable.containsKey( tokens[0] ) )
				{
					// this is an instruction, fetch the opcode from the table and add it to our program
					int opcode = opcodeTable.get( tokens[0] );
					program.add( opcode );
					
					// there is extra work for opcodes that have parameters
					switch( opcode )
					{
						case OPCODE_CONST:
							// tokens[1] must contain an integer
							program.add( Integer.parseInt( tokens[1] ) );
							break;
							
						case OPCODE_LOAD:
						case OPCODE_STO:
						case OPCODE_JMP:
						case OPCODE_FJMP:
						case OPCODE_CALL:
							// these opcodes have addresses as parameters, so we will save
							// the index that needs the address and fill it in later when
							// we parse the symbol table
							if( !symbols.containsKey( tokens[1] ) )
							{
								symbols.put( tokens[1], new ArrayList<Integer>() );
							}
							symbols.get( tokens[1] ).add( program.size() );
							// add empty placeholder value, will be replaces with an address later
							program.add( -1 );
							break;
					}
				}
				else if( tokens[0].charAt( tokens[0].length() - 1 ) == ':' )
				{
					// labels need to have colons after them
					String label = tokens[0].substring( 0, tokens[0].length() - 1 );
					
					if( !adresses.containsKey( label ) )
					{
						// this label points to the current address in the program, note that we
						// dont actually add anything to the program list here. Labels point to 
						// addresses that contain instructions, they are not instructions themselves
						adresses.put( label, program.size() );
					}
					else
					{
						// two adresses of the same name in one program. bit of a problem
						stdout.println( "Detected multiple uses of label " + label + ", only one instance allowed" );
					}
				}
				else
				{
					stdout.println( "Unrecognized symbol " + tokens[0] + " when compiling" );
				}
				
				line = r.readLine();
			}
			
			// iterate over the symbols that we have found, if it is a symbol that doesnt have an address
			// associated with it, then it is a variable and we will need to allocate space in the program
			// to store it. Then, fill in the addresses in the program
			Enumeration e = symbols.keys();
			while( e.hasMoreElements() )
			{
				String key = (String)e.nextElement();
				if( !adresses.containsKey( key ) )
				{
					adresses.put( key, program.size() );
					// default variables to this value:
					program.add( 0 );
				}
				
				// iterate over all the cells where this symbol was used in the program, and put its actual
				// memory address there instead of the placeholder we put earlier.
				int address = adresses.get( key );
				for( int i : symbols.get( key ) )
				{
					program.set( i, address );
				}
			}
			
			// build the memory from the program list
			memory = new int[program.size()];
			for( int i = 0; i < memory.length; ++i )
			{
				memory[i] = program.get( i );
			}

			r.close();
			
			// Uncommenting these lines can be useful in debugging the assembler. It can also provide
			// a little insight into the assembly process
			//stdout.println( program );
			//stdout.println( labels );
			//stdout.println( symbols );
		}
		catch( FileNotFoundException ex )
		{
			stdout.println( ex.getMessage() );
		}
		catch( IOException ex )
		{
			stdout.println( ex.getMessage() );
		}
	}
	
	public List<String> getOpcodeStrings()
	{
		List<String> ret = new ArrayList<String>();
		for( String key : opcodeTable.keySet() )
		{
			ret.add( key );
		}
		return ret;
	}
	
	public void setOutput( PrintStream out )
	{
		stdout = out;
	}
	
	public void setInput( InputStream in )
	{
		stdin = in;
	}

	public int[] getMemory()
	{
		return memory;
	}

	public static final int OPCODE_HALT  = 0;
	public static final int OPCODE_CONST = 8;
	public static final int OPCODE_LOAD  = 9;
	public static final int OPCODE_STO   = 10;
	public static final int OPCODE_ADD   = 11;
	public static final int OPCODE_SUB   = 12;
	public static final int OPCODE_MUL   = 13;
	public static final int OPCODE_DIV   = 14;
	public static final int OPCODE_EQL   = 15;
	public static final int OPCODE_LSS   = 16;
	public static final int OPCODE_GTR   = 17;
	public static final int OPCODE_JMP   = 18;
	public static final int OPCODE_FJMP  = 19;
	public static final int OPCODE_READ  = 20;
	public static final int OPCODE_WRITE = 21;
	public static final int OPCODE_CALL  = 22;
	public static final int OPCODE_RET   = 23;
	
	public static void main( String[] args )
	{
		VirtualMachine vm = new VirtualMachine( System.out, System.in );
		vm.run( false );
	}
}
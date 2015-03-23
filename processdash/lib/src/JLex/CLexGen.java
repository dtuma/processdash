
package JLex;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/***************************************************************
  Class: CLexGen
  **************************************************************/
class CLexGen 
{
  /***************************************************************
    Member Variables
    **************************************************************/
  private java.io.Reader m_instream; /* JLex specification file. */
  private java.io.PrintWriter m_outstream; /* Lexical analyzer source file. */

  private CInput m_input; /* Input buffer class. */

  private Hashtable m_tokens; /* Hashtable that maps characters to their 
				 corresponding lexical code for
				 the internal lexical analyzer. */
  private CSpec m_spec; /* Spec class holds information
			   about the generated lexer. */
  private boolean m_init_flag; /* Flag set to true only upon 
				  successful initialization. */

  private CMakeNfa m_makeNfa; /* NFA machine generator module. */
  private CNfa2Dfa m_nfa2dfa; /* NFA to DFA machine (transition table) 
				 conversion module. */
  private CMinimize m_minimize; /* Transition table compressor. */
  private CSimplifyNfa m_simplifyNfa; /* NFA simplifier using char classes */
  private CEmit m_emit; /* Output module that emits source code
			   into the generated lexer file. */


  /********************************************************
    Constants
    *******************************************************/
  private static final boolean ERROR = false;
  private static final boolean NOT_ERROR = true;
  private static final int BUFFER_SIZE = 1024;

  /********************************************************
    Constants: Token Types
    *******************************************************/
  static final int EOS = 1;
  static final int ANY = 2;
  static final int AT_BOL = 3;
  static final int AT_EOL = 4;
  static final int CCL_END = 5;
  static final int CCL_START = 6;
  static final int CLOSE_CURLY = 7;
  static final int CLOSE_PAREN = 8;
  static final int CLOSURE = 9;
  static final int DASH = 10;
  static final int END_OF_INPUT = 11;
  static final int L = 12;
  static final int OPEN_CURLY = 13;
  static final int OPEN_PAREN = 14;
  static final int OPTIONAL = 15;
  static final int OR = 16;
  static final int PLUS_CLOSE = 17;

  /***************************************************************
    Function: CLexGen
    **************************************************************/
  CLexGen 
    (
     String filename
     )
      throws java.io.FileNotFoundException, java.io.IOException
      {
          this(filename, filename+".java");
      }

  CLexGen 
    (
     String filename,
     String outputFilename
     )
      throws java.io.FileNotFoundException, java.io.IOException
      {
	/* Successful initialization flag. */
	m_init_flag = false;
	
	/* Open input stream. */
	m_instream = new java.io.FileReader(filename);
	if (null == m_instream)
	  {
	    System.out.println("Error: Unable to open input file "
			       + filename + ".");
	    return;
	  }

	/* Open output stream. */
    File file = new File(outputFilename);
	m_outstream 
	  = new java.io.PrintWriter(new java.io.BufferedWriter(
		new java.io.FileWriter(file)));
	if (null == m_outstream)
	  {
	    System.out.println("Error: Unable to open output file "
			       + outputFilename);
	    return;
	  }

	/* Create input buffer class. */
	m_input = new CInput(m_instream);

	/* Initialize character hash table. */
	m_tokens = new Hashtable();
	m_tokens.put(new Character('$'),new Integer(AT_EOL));
	m_tokens.put(new Character('('),new Integer(OPEN_PAREN));
	m_tokens.put(new Character(')'),new Integer(CLOSE_PAREN));
	m_tokens.put(new Character('*'),new Integer(CLOSURE));
	m_tokens.put(new Character('+'),new Integer(PLUS_CLOSE));
	m_tokens.put(new Character('-'),new Integer(DASH));
	m_tokens.put(new Character('.'),new Integer(ANY));
	m_tokens.put(new Character('?'),new Integer(OPTIONAL));
	m_tokens.put(new Character('['),new Integer(CCL_START));
	m_tokens.put(new Character(']'),new Integer(CCL_END));
	m_tokens.put(new Character('^'),new Integer(AT_BOL));
	m_tokens.put(new Character('{'),new Integer(OPEN_CURLY));
	m_tokens.put(new Character('|'),new Integer(OR));
	m_tokens.put(new Character('}'),new Integer(CLOSE_CURLY));
      
	/* Initialize spec structure. */
	m_spec = new CSpec(this);
	
	/* Nfa to dfa converter. */
	m_nfa2dfa = new CNfa2Dfa();
	m_minimize = new CMinimize();
	m_makeNfa = new CMakeNfa();
	m_simplifyNfa = new CSimplifyNfa();

	m_emit = new CEmit(file.getParentFile());

	/* Successful initialization flag. */
	m_init_flag = true;
      }

  /***************************************************************
    Function: generate
    Description: 
    **************************************************************/
  void generate
    (
     )
      throws java.io.IOException, java.io.FileNotFoundException
      {
	if (false == m_init_flag)
	  {
	    CError.parse_error(CError.E_INIT,0);
	  }

	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(null != this);
	    CUtility.ASSERT(null != m_outstream);
	    CUtility.ASSERT(null != m_input);
	    CUtility.ASSERT(null != m_tokens);
	    CUtility.ASSERT(null != m_spec);
	    CUtility.ASSERT(m_init_flag);
	  }

	/*m_emit.emit_imports(m_spec,m_outstream);*/

	if (m_spec.m_verbose)
	  {
	    System.out.println("Processing first section -- user code.");
	  }
	userCode();
	if (m_input.m_eof_reached)
	  {
	    CError.parse_error(CError.E_EOF,m_input.m_line_number);
	  }

	if (m_spec.m_verbose)
	  {
	    System.out.println("Processing second section -- " 
			       + "JLex declarations.");
	  }
	userDeclare();
	if (m_input.m_eof_reached)
	  {
	    CError.parse_error(CError.E_EOF,m_input.m_line_number);
	  }

	if (m_spec.m_verbose)
	  {
	    System.out.println("Processing third section -- lexical rules.");
	  }
	userRules();
 	if (CUtility.DO_DEBUG)
	  {
	    print_header();
	  }

	if (m_spec.m_verbose)
	  {
	    System.out.println("Outputting lexical analyzer code.");
	  }
	m_emit.emit(m_spec,m_outstream);

	if (m_spec.m_verbose && true == CUtility.OLD_DUMP_DEBUG)
	  {
	    details();
	  }
	
	m_outstream.close();
        m_input.close();
      }

  /***************************************************************
    Function: userCode
    Description: Process first section of specification,
    echoing it into output file.
    **************************************************************/
  private void userCode
    (
     )
      throws java.io.IOException
      {
	int count = 0;

	if (false == m_init_flag)
	  {
	    CError.parse_error(CError.E_INIT,0);
	  }

	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(null != this);
	    CUtility.ASSERT(null != m_outstream);
	    CUtility.ASSERT(null != m_input);
	    CUtility.ASSERT(null != m_tokens);
	    CUtility.ASSERT(null != m_spec);
	  }

	if (m_input.m_eof_reached)
	  {
	    CError.parse_error(CError.E_EOF,0);
	  }

	while (true)
	  {
	    if (m_input.getLine())
	      {
		/* Eof reached. */
		CError.parse_error(CError.E_EOF,0);
	      }
	    
	    if (2 <= m_input.m_line_read 
		&& '%' == m_input.m_line[0]
		&& '%' == m_input.m_line[1])
	      {
		/* Discard remainder of line. */
		m_input.m_line_index = m_input.m_line_read;
		return;
	      }

	    m_outstream.print(new String(m_input.m_line,0,
					      m_input.m_line_read));
	  }
      }

  /***************************************************************
    Function: getName
    **************************************************************/
  private char[] getName
    (
     )
      {
	char buffer[];
	int elem;

	/* Skip white space. */
	while (m_input.m_line_index < m_input.m_line_read
	       && true == CUtility.isspace(m_input.m_line[m_input.m_line_index]))
	  {
	    ++m_input.m_line_index;
	  }

	/* No name? */
	if (m_input.m_line_index >= m_input.m_line_read)
	  {
	    CError.parse_error(CError.E_DIRECT,0);
	  }

	/* Determine length. */
	elem = m_input.m_line_index;
	while (elem < m_input.m_line_read
	       && false == CUtility.isnewline(m_input.m_line[elem]))
	  {
	    ++elem;
	  } 

	/* Allocate non-terminated buffer of exact length. */
	buffer = new char[elem - m_input.m_line_index];
	
	/* Copy. */
	elem = 0;
	while (m_input.m_line_index < m_input.m_line_read
	       && false == CUtility.isnewline(m_input.m_line[m_input.m_line_index]))
	  {
	    buffer[elem] = m_input.m_line[m_input.m_line_index];
	    ++elem;
	    ++m_input.m_line_index;
	  }

	return buffer;
      }

  private final int CLASS_CODE = 0;
  private final int INIT_CODE = 1;
  private final int EOF_CODE = 2;
  private final int INIT_THROW_CODE = 3;
  private final int YYLEX_THROW_CODE = 4;
  private final int EOF_THROW_CODE = 5;
  private final int EOF_VALUE_CODE = 6;

  /***************************************************************
    Function: packCode
    Description:
    **************************************************************/
  private char[] packCode
    (
     char start_dir[],
     char end_dir[],
     char prev_code[],
     int prev_read,
     int specified
     )
      throws java.io.IOException
      {
	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(INIT_CODE == specified 
			    || CLASS_CODE == specified
			    || EOF_CODE == specified
			    || EOF_VALUE_CODE == specified
			    || INIT_THROW_CODE == specified
			    || YYLEX_THROW_CODE == specified
			    || EOF_THROW_CODE == specified);
	  }

	if (0 != CUtility.charncmp(m_input.m_line,
				   0,
				   start_dir,
				   0,
				   start_dir.length - 1))
	  {
	    CError.parse_error(CError.E_INTERNAL,0);
	  }
	
	if (null == prev_code)
	  {
	    prev_code = new char[BUFFER_SIZE];
	    prev_read = 0;
	  }
	
	if (prev_read >= prev_code.length)
	  {
	    prev_code = CUtility.doubleSize(prev_code);
	  }
	
	m_input.m_line_index = start_dir.length - 1;
	while (true)
	  {
	    while (m_input.m_line_index >= m_input.m_line_read)
	      {
		if (m_input.getLine())
		  {
		    CError.parse_error(CError.E_EOF,m_input.m_line_number);
		  }
		
		if (0 == CUtility.charncmp(m_input.m_line,
					   0,
					   end_dir,
					   0,
					   end_dir.length - 1))
		  {
		    m_input.m_line_index = end_dir.length - 1;
		    
		    switch (specified)
		      {
		      case CLASS_CODE:
			m_spec.m_class_read = prev_read;
			break;
			
		      case INIT_CODE:
			m_spec.m_init_read = prev_read;
			break;
			
		      case EOF_CODE:
			m_spec.m_eof_read = prev_read;
			break;

		      case EOF_VALUE_CODE:
			m_spec.m_eof_value_read = prev_read;
			break;

		      case INIT_THROW_CODE:
			m_spec.m_init_throw_read = prev_read;
			break;

		      case YYLEX_THROW_CODE:
			m_spec.m_yylex_throw_read = prev_read;
			break;
			
		      case EOF_THROW_CODE:
			m_spec.m_eof_throw_read = prev_read;
			break;
			
		      default:
			CError.parse_error(CError.E_INTERNAL,m_input.m_line_number);
			break;
		      }

		    return prev_code;
		  }
	      }

	    while (m_input.m_line_index < m_input.m_line_read)
	      {
		prev_code[prev_read] = m_input.m_line[m_input.m_line_index];
		++prev_read;
		++m_input.m_line_index;

		if (prev_read >= prev_code.length)
		  {
		    prev_code = CUtility.doubleSize(prev_code);
		  }
	      }
	  }
      }

  /***************************************************************
    Member Variables: JLex directives.
    **************************************************************/
  private char m_state_dir[] = { 
    '%', 's', 't', 
    'a', 't', 'e',
    '\0'
    };
  
  private char m_char_dir[] = { 
    '%', 'c', 'h',
    'a', 'r',
    '\0'
    };

  private char m_line_dir[] = { 
    '%', 'l', 'i',
    'n', 'e',
    '\0'
    };

  private char m_cup_dir[] = { 
    '%', 'c', 'u',
    'p', 
    '\0'
    };

  private char m_class_dir[] = { 
    '%', 'c', 'l', 
    'a', 's', 's',
    '\0'
    };

  private char m_implements_dir[] = { 
    '%', 'i', 'm', 'p', 'l', 'e', 'm', 'e', 'n', 't', 's', 
    '\0'
    };

  private char m_function_dir[] = { 
    '%', 'f', 'u',
    'n', 'c', 't',
    'i', 'o', 'n',
    '\0'
    };

  private char m_type_dir[] = { 
    '%', 't', 'y',
    'p', 'e',
    '\0'
    };

  private char m_integer_dir[] = { 
    '%', 'i', 'n',
    't', 'e', 'g', 
    'e', 'r',
    '\0'
    };

  private char m_intwrap_dir[] = { 
    '%', 'i', 'n',
    't', 'w', 'r', 
    'a', 'p',
    '\0'
    };

  private char m_full_dir[] = { 
    '%', 'f', 'u', 
    'l', 'l',
    '\0'
    };

  private char m_unicode_dir[] = { 
    '%', 'u', 'n', 
    'i', 'c', 'o',
    'd', 'e',
    '\0'
    };

  private char m_ignorecase_dir[] = {
    '%', 'i', 'g',
    'n', 'o', 'r',
    'e', 'c', 'a', 
    's', 'e',
    '\0'
    };

  private char m_notunix_dir[] = { 
    '%', 'n', 'o',
    't', 'u', 'n', 
    'i', 'x',
    '\0'
    };

  private char m_init_code_dir[] = { 
    '%', 'i', 'n', 
    'i', 't', '{',
    '\0'
    };

  private char m_init_code_end_dir[] = { 
    '%', 'i', 'n', 
    'i', 't', '}',
    '\0'
    };

  private char m_init_throw_code_dir[] = { 
    '%', 'i', 'n', 
    'i', 't', 't',
    'h', 'r', 'o',
    'w', '{',
    '\0'
    };

  private char m_init_throw_code_end_dir[] = { 
    '%', 'i', 'n', 
    'i', 't', 't',
    'h', 'r', 'o',
    'w', '}',
    '\0'
    };

  private char m_yylex_throw_code_dir[] = { 
    '%', 'y', 'y', 'l', 
    'e', 'x', 't',
    'h', 'r', 'o',
    'w', '{',
    '\0'
    };

  private char m_yylex_throw_code_end_dir[] = { 
    '%', 'y', 'y', 'l', 
    'e', 'x', 't',
    'h', 'r', 'o',
    'w', '}',
    '\0'
    };

  private char m_eof_code_dir[] = { 
    '%', 'e', 'o', 
    'f', '{',
    '\0'
    };

  private char m_eof_code_end_dir[] = { 
    '%', 'e', 'o', 
    'f', '}',
    '\0'
    };

  private char m_eof_value_code_dir[] = { 
    '%', 'e', 'o', 
    'f', 'v', 'a', 
    'l', '{',
    '\0'
    };

  private char m_eof_value_code_end_dir[] = { 
    '%', 'e', 'o', 
    'f', 'v', 'a',
    'l', '}',
    '\0'
    };

  private char m_eof_throw_code_dir[] = { 
    '%', 'e', 'o', 
    'f', 't', 'h',
    'r', 'o', 'w',
    '{',
    '\0'
    };

  private char m_eof_throw_code_end_dir[] = { 
    '%', 'e', 'o', 
    'f', 't', 'h',
    'r', 'o', 'w',
    '}',
    '\0'
    };

  private char m_class_code_dir[] = { 
    '%', '{',
    '\0'
    };

  private char m_class_code_end_dir[] = { 
    '%', '}',
    '\0'
    };

  private char m_yyeof_dir[] = { 
    '%', 'y', 'y',
    'e', 'o', 'f',
    '\0'
    };
  
  private char m_public_dir[] = { 
    '%', 'p', 'u',
    'b', 'l', 'i', 
    'c', '\0'
    };
  
  /***************************************************************
    Function: userDeclare
    Description:
    **************************************************************/
  private void userDeclare
    (
     )
      throws java.io.IOException
	{
	  int elem;
	  
	  if (CUtility.DEBUG)
	    {
	      CUtility.ASSERT(null != this);
	      CUtility.ASSERT(null != m_outstream);
	      CUtility.ASSERT(null != m_input);
	      CUtility.ASSERT(null != m_tokens);
	      CUtility.ASSERT(null != m_spec);
	    }

	  if (m_input.m_eof_reached)
	    {
	      /* End-of-file. */
	      CError.parse_error(CError.E_EOF,
				 m_input.m_line_number);
	    }

	  while (false == m_input.getLine())
	    {
	      /* Look for double percent. */
	      if (2 <= m_input.m_line_read 
		  && '%' == m_input.m_line[0] 
		  && '%' == m_input.m_line[1])
		{
		  /* Mess around with line. */
		  m_input.m_line_read -= 2;
		  System.arraycopy(m_input.m_line, 2,
				   m_input.m_line, 0, m_input.m_line_read);

		  m_input.m_pushback_line = true;
		  /* Check for and discard empty line. */
		  if (0 == m_input.m_line_read 
		      || '\n' == m_input.m_line[0])
		    {
		      m_input.m_pushback_line = false;
		    }

		  return;
		}

	      if (0 == m_input.m_line_read)
		{
		  continue;
		}

	      if ('%' == m_input.m_line[0])
		{
		  /* Special lex declarations. */
		  if (1 >= m_input.m_line_read)
		    {
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      continue;
		    }

		  switch (m_input.m_line[1])
		    {
		    case '{':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_class_code_dir,
						 0,
						 m_class_code_dir.length - 1))
			{
			  m_spec.m_class_code = packCode(m_class_code_dir,
							 m_class_code_end_dir,
							 m_spec.m_class_code,
							 m_spec.m_class_read,
							 CLASS_CODE);
			  break;
			}
	      
		      /* Bad directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;

		    case 'c':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_char_dir,
						 0,
						 m_char_dir.length - 1))
			{
			  /* Set line counting to ON. */
			  m_input.m_line_index = m_char_dir.length;
			  m_spec.m_count_chars = true;
			  break;
			}	
		      else if (0 == CUtility.charncmp(m_input.m_line,
						      0,
						      m_class_dir, 
						      0,
						      m_class_dir.length - 1))
			{
			  m_input.m_line_index = m_class_dir.length;
			  m_spec.m_class_name = getName();
			  break;
			}
		      else if (0 == CUtility.charncmp(m_input.m_line,
						      0,
						      m_cup_dir,
						      0,
						      m_cup_dir.length - 1))
			{
			  /* Set Java CUP compatibility to ON. */
			  m_input.m_line_index = m_cup_dir.length;
			  m_spec.m_cup_compatible = true;
			  // this is what %cup does: [CSA, 27-Jul-1999]
			  m_spec.m_implements_name =
			      "java_cup.runtime.Scanner".toCharArray();
			  m_spec.m_function_name =
			      "next_token".toCharArray();
			  m_spec.m_type_name =
			      "java_cup.runtime.Symbol".toCharArray();
			  break;
			}
	      
		      /* Bad directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;
		      
		    case 'e':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_eof_code_dir,
						 0,
						 m_eof_code_dir.length - 1))
			{
			  m_spec.m_eof_code = packCode(m_eof_code_dir,
						       m_eof_code_end_dir,
						       m_spec.m_eof_code,
						       m_spec.m_eof_read,
						       EOF_CODE);
			  break;
			}
		      else if (0 == CUtility.charncmp(m_input.m_line,
						      0,
						      m_eof_value_code_dir,
						      0,
						      m_eof_value_code_dir.length - 1))
			{
			  m_spec.m_eof_value_code = packCode(m_eof_value_code_dir,
							     m_eof_value_code_end_dir,
							     m_spec.m_eof_value_code,
							     m_spec.m_eof_value_read,
							     EOF_VALUE_CODE);
			  break;
			}
		      else if (0 == CUtility.charncmp(m_input.m_line,
						      0,
						      m_eof_throw_code_dir,
						      0,
						      m_eof_throw_code_dir.length - 1))
			{
			  m_spec.m_eof_throw_code = packCode(m_eof_throw_code_dir,
						       m_eof_throw_code_end_dir,
						       m_spec.m_eof_throw_code,
						       m_spec.m_eof_throw_read,
						       EOF_THROW_CODE);
			  break;
			}
	      
		      /* Bad directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;

		    case 'f':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_function_dir,
						 0,
						 m_function_dir.length - 1))
			{
			  /* Set line counting to ON. */
			  m_input.m_line_index = m_function_dir.length;
			  m_spec.m_function_name = getName();
			  break;
			}
		      else if (0 == CUtility.charncmp(m_input.m_line,
						      0,
						      m_full_dir,
						      0,
						      m_full_dir.length - 1))
			{
			  m_input.m_line_index = m_full_dir.length;
			  m_spec.m_dtrans_ncols = CUtility.MAX_EIGHT_BIT + 1;
			  break;
			}

		      /* Bad directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;

		    case 'i':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_integer_dir,
						 0,
						 m_integer_dir.length - 1))
			{
			  /* Set line counting to ON. */
			  m_input.m_line_index = m_integer_dir.length;
			  m_spec.m_integer_type = true;
			  break;
			}
		      else if (0 == CUtility.charncmp(m_input.m_line,
						      0,
						      m_intwrap_dir,
						      0,
						      m_intwrap_dir.length - 1))
			{
			  /* Set line counting to ON. */
			  m_input.m_line_index = m_integer_dir.length;
			  m_spec.m_intwrap_type = true;
			  break;
			}
		      else if (0 == CUtility.charncmp(m_input.m_line,
						      0,
						      m_init_code_dir,
						      0,
						      m_init_code_dir.length - 1))
			{
			  m_spec.m_init_code = packCode(m_init_code_dir,
							m_init_code_end_dir,
							m_spec.m_init_code,
							m_spec.m_init_read,
							INIT_CODE);
			  break;
			}
		      else if (0 == CUtility.charncmp(m_input.m_line,
						      0,
						      m_init_throw_code_dir,
						      0,
						      m_init_throw_code_dir.length - 1))
			{
			  m_spec.m_init_throw_code = packCode(m_init_throw_code_dir,
						       m_init_throw_code_end_dir,
						       m_spec.m_init_throw_code,
						       m_spec.m_init_throw_read,
						       INIT_THROW_CODE);
			  break;
			}
		      else if (0 == CUtility.charncmp(m_input.m_line,
						      0,
						      m_implements_dir, 
						      0,
						      m_implements_dir.length - 1))
			{
			  m_input.m_line_index = m_implements_dir.length;
			  m_spec.m_implements_name = getName();
			  break;
			}
		      else if (0 == CUtility.charncmp(m_input.m_line,
						      0,
						      m_ignorecase_dir,
						      0,
						      m_ignorecase_dir.length-1))
			{
			  /* Set m_ignorecase to ON. */
			  m_input.m_line_index = m_ignorecase_dir.length;
			  m_spec.m_ignorecase = true;
			  break;
			}

		      /* Bad directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;

		    case 'l':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_line_dir,
						 0,
						 m_line_dir.length - 1))
			{
			  /* Set line counting to ON. */
			  m_input.m_line_index = m_line_dir.length;
			  m_spec.m_count_lines = true;
			  break;
			}

		      /* Bad directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;

		    case 'n':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_notunix_dir,
						 0,
						 m_notunix_dir.length - 1))
			{
			  /* Set line counting to ON. */
			  m_input.m_line_index = m_notunix_dir.length;
			  m_spec.m_unix = false;
			  break;
			}

		      /* Bad directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;

		    case 'p':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_public_dir,
						 0,
						 m_public_dir.length - 1))
			{
			  /* Set public flag. */
			  m_input.m_line_index = m_public_dir.length;
			  m_spec.m_public = true;
			  break;
			}

		      /* Bad directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;

		    case 's':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_state_dir,
						 0,
						 m_state_dir.length - 1))
			{
			  /* Recognize state list. */
			  m_input.m_line_index = m_state_dir.length;
			  saveStates();
			  break;
			}

		      /* Undefined directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;
		     
		    case 't':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_type_dir,
						 0,
						 m_type_dir.length - 1))
			{
			  /* Set Java CUP compatibility to ON. */
			  m_input.m_line_index = m_type_dir.length;
			  m_spec.m_type_name = getName();
			  break;
			}

		      /* Undefined directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;

		    case 'u':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_unicode_dir,
						 0,
						 m_unicode_dir.length - 1))
			{
			  m_input.m_line_index = m_unicode_dir.length;
			  m_spec.m_dtrans_ncols= CUtility.MAX_SIXTEEN_BIT + 1;
			  break;
			}

		      /* Bad directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;

		    case 'y':
		      if (0 == CUtility.charncmp(m_input.m_line,
						 0,
						 m_yyeof_dir,
						 0,
						 m_yyeof_dir.length - 1))
			{
			  m_input.m_line_index = m_yyeof_dir.length;
			  m_spec.m_yyeof = true;
			  break;
			} else if (0 == CUtility.charncmp(m_input.m_line,
							  0,
							  m_yylex_throw_code_dir,
							  0,
							  m_yylex_throw_code_dir.length - 1))
			{
			  m_spec.m_yylex_throw_code = packCode(m_yylex_throw_code_dir,
							       m_yylex_throw_code_end_dir,
						       m_spec.m_yylex_throw_code,
						       m_spec.m_yylex_throw_read,
						       YYLEX_THROW_CODE);
			  break;
			}


		      /* Bad directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;

		    default:
		      /* Undefined directive. */
		      CError.parse_error(CError.E_DIRECT,
					 m_input.m_line_number);
		      break;
		    }
		}
	      else
		{
		  /* Regular expression macro. */
		  m_input.m_line_index = 0;
		  saveMacro();
		}

	      if (CUtility.OLD_DEBUG)
		{
		  System.out.println("Line number " 
				     + m_input.m_line_number + ":"); 
		  System.out.print(new String(m_input.m_line,
					      0,m_input.m_line_read));
		}
	    }
	}
	 
  /***************************************************************
    Function: userRules
    Description: Processes third section of JLex 
    specification and creates minimized transition table.
    **************************************************************/
  private void userRules
    (
     )
      throws java.io.IOException
      {
	int code;

	if (false == m_init_flag)
	  {
	    CError.parse_error(CError.E_INIT,0);
	  }

	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(null != this);
	    CUtility.ASSERT(null != m_outstream);
	    CUtility.ASSERT(null != m_input);
	    CUtility.ASSERT(null != m_tokens);
	    CUtility.ASSERT(null != m_spec);
	  }

	/* UNDONE: Need to handle states preceding rules. */
	
	if (m_spec.m_verbose)
	  {
	    System.out.println("Creating NFA machine representation.");
	  }
	m_makeNfa.allocate_BOL_EOF(m_spec);
	m_makeNfa.thompson(this,m_spec,m_input);
	
	m_simplifyNfa.simplify(m_spec);

	/*print_nfa();*/

	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(END_OF_INPUT == m_spec.m_current_token);
	  }

	if (m_spec.m_verbose)
	  {
	    System.out.println("Creating DFA transition table.");
	  }
	m_nfa2dfa.make_dfa(this,m_spec);

	if (CUtility.FOODEBUG) {
	  print_header();
	}

	if (m_spec.m_verbose)
	  {
	    System.out.println("Minimizing DFA transition table.");
	  }
	m_minimize.min_dfa(m_spec);
      }

  /***************************************************************
    Function: printccl
    Description: Debugging routine that outputs readable form
    of character class.
    **************************************************************/
  private void printccl
    (
     CSet set
     )
      {
	int i;
	
	System.out.print(" [");
	for (i = 0; i < m_spec.m_dtrans_ncols; ++i)
	  {
	    if (set.contains(i))
	      {
		System.out.print(interp_int(i));
	      }
	  }
	System.out.print(']');
      }

  /***************************************************************
    Function: plab
    Description:
    **************************************************************/
  private String plab
    (
     CNfa state
     )
      {
	int index;
	
	if (null == state)
	  {
	    return (new String("--"));
	  }

	index = m_spec.m_nfa_states.indexOf(state);
	
	return ((new Integer(index)).toString());
      }

  /***************************************************************
    Function: interp_int
    Description:
    **************************************************************/
  private String interp_int
    (
     int i
     )
      {
	switch (i)
	  {
	  case (int) '\b':
	    return (new String("\\b"));

	  case (int) '\t':
	    return (new String("\\t"));

	  case (int) '\n':
	    return (new String("\\n"));

	  case (int) '\f':
	    return (new String("\\f"));

	  case (int) '\r':
	    return (new String("\\r"));
	    
	  case (int) ' ':
	    return (new String("\\ "));
	    
	  default:
	    return ((new Character((char) i)).toString());
	  }
      }

  /***************************************************************
    Function: print_nfa
    Description:
    **************************************************************/
  void print_nfa
    (
     )
      {
	int elem;
	CNfa nfa;
	int size;
	Enumeration states;
	Integer index;
	int i;
	int j;
	int vsize;
	String state;
     
	System.out.println("--------------------- NFA -----------------------");
	
	size = m_spec.m_nfa_states.size();
	for (elem = 0; elem < size; ++elem)
	  {
	    nfa = (CNfa) m_spec.m_nfa_states.elementAt(elem);
	    
	    System.out.print("Nfa state " + plab(nfa) + ": ");
	    
	    if (null == nfa.m_next)
	      {
		System.out.print("(TERMINAL)");
	      }
	    else
	      {
		System.out.print("--> " + plab(nfa.m_next));
		System.out.print("--> " + plab(nfa.m_next2));
		
		switch (nfa.m_edge)
		  {
		  case CNfa.CCL:
		    printccl(nfa.m_set);
		    break;

		  case CNfa.EPSILON:
		    System.out.print(" EPSILON ");
		    break; 
		    
		  default:
		    System.out.print(" " + interp_int(nfa.m_edge));
		    break;
		  }
	      }

	    if (0 == elem)
	      {
		System.out.print(" (START STATE)");
	      }
	    
	    if (null != nfa.m_accept)
	      {
		System.out.print(" accepting " 
				 + ((0 != (nfa.m_anchor & CSpec.START)) ? "^" : "")
				 + "<" 
				 + (new String(nfa.m_accept.m_action,0,
					       nfa.m_accept.m_action_read))
				 + ">"
				 + ((0 != (nfa.m_anchor & CSpec.END)) ? "$" : ""));
	      }

	    System.out.println();
	  }

	states = m_spec.m_states.keys();
	while (states.hasMoreElements())
	  {
	    state = (String) states.nextElement();
	    index = (Integer) m_spec.m_states.get(state);

	    if (CUtility.DEBUG)
	      {
		CUtility.ASSERT(null != state);
		CUtility.ASSERT(null != index);
	      }

	    System.out.println("State \"" + state 
			       + "\" has identifying index " 
			       + index.toString() + ".");
	    System.out.print("\tStart states of matching rules: ");
	    
	    i = index.intValue();
	    vsize = m_spec.m_state_rules[i].size();
	    
	    for (j = 0; j < vsize; ++j)
	      {
		nfa = (CNfa) m_spec.m_state_rules[i].elementAt(j);

		System.out.print(m_spec.m_nfa_states.indexOf(nfa) + " ");
	      }

	    System.out.println();
	  }

	System.out.println("-------------------- NFA ----------------------");
      }

  /***************************************************************
    Function: getStates
    Description: Parses the state area of a rule,
    from the beginning of a line.
    < state1, state2 ... > regular_expression { action }
    Returns null on only EOF.  Returns all_states, 
    initialied properly to correspond to all states,
    if no states are found.
    Special Notes: This function treats commas as optional
    and permits states to be spread over multiple lines.
    **************************************************************/
  private SparseBitSet all_states = null;
  SparseBitSet getStates
    (
     )
      throws java.io.IOException
      {
	int start_state;
	int count_state;
	SparseBitSet states;
	String name;
	Integer index;
	int i;
	int size;
	
	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(null != this);
	    CUtility.ASSERT(null != m_outstream);
	    CUtility.ASSERT(null != m_input);
	    CUtility.ASSERT(null != m_tokens);
	    CUtility.ASSERT(null != m_spec);
	  }

	states = null;

	/* Skip white space. */
	while (CUtility.isspace(m_input.m_line[m_input.m_line_index]))
	  {
	    ++m_input.m_line_index;
    
	    while (m_input.m_line_index >= m_input.m_line_read)
	      {
		/* Must just be an empty line. */
		if (m_input.getLine())
		  {
		    /* EOF found. */
		    return null;
		  }
	      }
	  }

	/* Look for states. */
	if ('<' == m_input.m_line[m_input.m_line_index])
	  {
	    ++m_input.m_line_index;
	   
	    states = new SparseBitSet();

	    /* Parse states. */
	    while (true)
	      {
		/* We may have reached the end of the line. */
		while (m_input.m_line_index >= m_input.m_line_read)
		  {
		    if (m_input.getLine())
		      {
			/* EOF found. */
			CError.parse_error(CError.E_EOF,m_input.m_line_number);
			return states;
		      }
		  }

		while (true)
		  {
		    /* Skip white space. */
		    while (CUtility.isspace(m_input.m_line[m_input.m_line_index]))
		      {
			++m_input.m_line_index;
			
			while (m_input.m_line_index >= m_input.m_line_read)
			  {
			    if (m_input.getLine())
			      {
				/* EOF found. */
				CError.parse_error(CError.E_EOF,m_input.m_line_number);
				return states;
			      }
			  }
		      }
		    
		    if (',' != m_input.m_line[m_input.m_line_index])
		      {
			break;
		      }

		    ++m_input.m_line_index;
		  }

		if ('>' == m_input.m_line[m_input.m_line_index])
		  {
		    ++m_input.m_line_index;
		    if (m_input.m_line_index < m_input.m_line_read)
		      {
			m_advance_stop = true;
		      }
		    return states;
		  }

		/* Read in state name. */
		start_state = m_input.m_line_index;
		while (false == CUtility.isspace(m_input.m_line[m_input.m_line_index])
		       && ',' != m_input.m_line[m_input.m_line_index]
		       && '>' != m_input.m_line[m_input.m_line_index])
		  {
		    ++m_input.m_line_index;

		    if (m_input.m_line_index >= m_input.m_line_read)
		      {
			/* End of line means end of state name. */
			break;
		      }
		  }
		count_state = m_input.m_line_index - start_state;

		/* Save name after checking definition. */
		name = new String(m_input.m_line,
				  start_state,
				  count_state);
		index = (Integer) m_spec.m_states.get(name);
		if (null == index)
		  {
		    /* Uninitialized state. */
		    System.out.println("Uninitialized State Name: " + name);
		    CError.parse_error(CError.E_STATE,m_input.m_line_number);
		  }
		states.set(index.intValue());
	      }
	  }
	
	if (null == all_states)
	  {
	    all_states = new SparseBitSet();

	    size = m_spec.m_states.size();
	    for (i = 0; i < size; ++i)
	      {
		all_states.set(i);
	      }
	  }
	
	if (m_input.m_line_index < m_input.m_line_read)
	  {
	    m_advance_stop = true;
	  }
	return all_states;
      }

  /********************************************************
    Function: expandMacro
    Description: Returns false on error, true otherwise. 
    *******************************************************/
  private boolean expandMacro
    (
     )
      {
	int elem;
	int start_macro;
	int end_macro;
	int start_name;
	int count_name;
	String def;
	int def_elem;
	String name;
	char replace[];
	int rep_elem;

	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(null != this);
	    CUtility.ASSERT(null != m_outstream);
	    CUtility.ASSERT(null != m_input);
	    CUtility.ASSERT(null != m_tokens);
	    CUtility.ASSERT(null != m_spec);
	  }

	/* Check for macro. */
	if ('{' != m_input.m_line[m_input.m_line_index])
	  {
	    CError.parse_error(CError.E_INTERNAL,m_input.m_line_number);
	    return ERROR;
	  }
	
	start_macro = m_input.m_line_index;
	elem = m_input.m_line_index + 1;
	if (elem >= m_input.m_line_read)
	  {
	    CError.impos("Unfinished macro name");
	    return ERROR;
	  }
	
	/* Get macro name. */
	start_name = elem;
	while ('}' != m_input.m_line[elem])
	  {
	    ++elem;
	    if (elem >= m_input.m_line_read)
	      {
		CError.impos("Unfinished macro name at line " 
			     + m_input.m_line_number);
		return ERROR;
	      }
	  }
	count_name = elem - start_name;
	end_macro = elem;

	/* Check macro name. */
	if (0 == count_name)
	  {
	    CError.impos("Nonexistent macro name");
	    return ERROR;
	  }

	/* Debug checks. */
	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(0 < count_name);
	  }

	/* Retrieve macro definition. */
	name = new String(m_input.m_line,start_name,count_name);
	def = (String) m_spec.m_macros.get(name);
	if (null == def)
	  {
	    /*CError.impos("Undefined macro \"" + name + "\".");*/
	    System.out.println("Error: Undefined macro \"" + name + "\".");
	    CError.parse_error(CError.E_NOMAC, m_input.m_line_number);
	    return ERROR;
	  }
	if (CUtility.OLD_DUMP_DEBUG)
	  {
	    System.out.println("expanded escape: " + def);
	  }
		
	/* Replace macro in new buffer,
	   beginning by copying first part of line buffer. */
	replace = new char[m_input.m_line.length];
	for (rep_elem = 0; rep_elem < start_macro; ++rep_elem)
	  {
	    replace[rep_elem] = m_input.m_line[rep_elem];

	    if (CUtility.DEBUG)
	      {
		CUtility.ASSERT(rep_elem < replace.length);
	      }
	  }
	
	/* Copy macro definition. */
	if (rep_elem >= replace.length)
	  {
	    replace = CUtility.doubleSize(replace);
	  }
	for (def_elem = 0; def_elem < def.length(); ++def_elem)
	  {
	    replace[rep_elem] = def.charAt(def_elem);
	    
	    ++rep_elem;
	    if (rep_elem >= replace.length)
	      {
		replace = CUtility.doubleSize(replace);
	      }
	  }

	/* Copy last part of line. */
       	if (rep_elem >= replace.length)
	  {
	    replace = CUtility.doubleSize(replace);
	  }
	for (elem = end_macro + 1; elem < m_input.m_line_read; ++elem)
	  {
	    replace[rep_elem] = m_input.m_line[elem];
	    
	    ++rep_elem;
	    if (rep_elem >= replace.length)
	      {
		replace = CUtility.doubleSize(replace);
	      }
	  } 
	
	/* Replace buffer. */
	m_input.m_line = replace;
	m_input.m_line_read = rep_elem;
	
	if (CUtility.OLD_DEBUG)
	  {
	    System.out.println(new String(m_input.m_line,0,m_input.m_line_read));
	  }
	return NOT_ERROR;
      }

  /***************************************************************
    Function: saveMacro
    Description: Saves macro definition of form:
    macro_name = macro_definition
    **************************************************************/
  private void saveMacro
    (
     )
      {
	int elem;
	int start_name;
	int count_name;
	int start_def;
	int count_def;
	boolean saw_escape;
	boolean in_quote;
	boolean in_ccl;

	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(null != this);
	    CUtility.ASSERT(null != m_outstream);
	    CUtility.ASSERT(null != m_input);
	    CUtility.ASSERT(null != m_tokens);
	    CUtility.ASSERT(null != m_spec);
	  }

	/* Macro declarations are of the following form:
	   macro_name macro_definition */

	elem = 0;
	
	/* Skip white space preceding macro name. */
	while (CUtility.isspace(m_input.m_line[elem]))
	  {
	    ++elem;
	    if (elem >= m_input.m_line_read)
	      {
		/* End of line has been reached,
		   and line was found to be empty. */
		return;
	      }
	  }

	/* Read macro name. */
	start_name = elem;
	while (false == CUtility.isspace(m_input.m_line[elem])
	       && '=' != m_input.m_line[elem])
	  {
	    ++elem;
	    if (elem >= m_input.m_line_read)
	      {
		/* Macro name but no associated definition. */
		CError.parse_error(CError.E_MACDEF,m_input.m_line_number);
	      }
	  }
	count_name = elem - start_name;

	/* Check macro name. */
	if (0 == count_name) 
	  {
	    /* Nonexistent macro name. */
	    CError.parse_error(CError.E_MACDEF,m_input.m_line_number);
	  }

	/* Skip white space between name and definition. */
	while (CUtility.isspace(m_input.m_line[elem]))
	  {
	    ++elem;
	    if (elem >= m_input.m_line_read)
	      {
		/* Macro name but no associated definition. */
		CError.parse_error(CError.E_MACDEF,m_input.m_line_number);
	      }
	  }

	if ('=' == m_input.m_line[elem])
	  {
	    ++elem;
	    if (elem >= m_input.m_line_read)
	      {
		/* Macro name but no associated definition. */
		CError.parse_error(CError.E_MACDEF,m_input.m_line_number);
	      }
	  }
	else /* macro definition without = */
		CError.parse_error(CError.E_MACDEF,m_input.m_line_number);

	/* Skip white space between name and definition. */
	while (CUtility.isspace(m_input.m_line[elem]))
	  {
	    ++elem;
	    if (elem >= m_input.m_line_read)
	      {
		/* Macro name but no associated definition. */
		CError.parse_error(CError.E_MACDEF,m_input.m_line_number);
	      }
	  }

	/* Read macro definition. */
	start_def = elem;
	in_quote = false;
	in_ccl = false;
	saw_escape = false;
	while (false == CUtility.isspace(m_input.m_line[elem])
	       || true == in_quote
	       || true == in_ccl
	       || true == saw_escape)
	  {
	    if ('\"' == m_input.m_line[elem] && false == saw_escape)
	      {
		in_quote = !in_quote;
	      }
	    
	    if ('\\' == m_input.m_line[elem] && false == saw_escape)
	      {
		saw_escape = true;
	      }
	    else
	      {
		saw_escape = false;
	      }
	    if (false == saw_escape && false == in_quote) { // CSA, 24-jul-99
	      if ('[' == m_input.m_line[elem] && false == in_ccl)
		in_ccl = true;
	      if (']' == m_input.m_line[elem] && true == in_ccl)
		in_ccl = false;
	    }

	    ++elem;
	    if (elem >= m_input.m_line_read)
	      {
		/* End of line. */
		break;
	      }
	  }
	count_def = elem - start_def;
	  
	/* Check macro definition. */
	if (0 == count_def) 
	  {
	    /* Nonexistent macro name. */
	    CError.parse_error(CError.E_MACDEF,m_input.m_line_number);
	  }

	/* Debug checks. */
	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(0 < count_def);
	    CUtility.ASSERT(0 < count_name);
	    CUtility.ASSERT(null != m_spec.m_macros);
	  }

	if (CUtility.OLD_DEBUG)
	  {
	    System.out.println("macro name \""
			       + new String(m_input.m_line,start_name,count_name)
			       + "\".");
	    System.out.println("macro definition \""
			       + new String(m_input.m_line,start_def,count_def)
			       + "\".");
	  }

	/* Add macro name and definition to table. */
	m_spec.m_macros.put(new String(m_input.m_line,start_name,count_name),
			    new String(m_input.m_line,start_def,count_def));
      }

  /***************************************************************
    Function: saveStates
    Description: Takes state declaration and makes entries
    for them in state hashtable in CSpec structure.
    State declaration should be of the form:
    %state name0[, name1, name2 ...]
    (But commas are actually optional as long as there is 
    white space in between them.)
    **************************************************************/
  private void saveStates
    (
     )
      {
	int start_state;
	int count_state;

	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(null != this);
	    CUtility.ASSERT(null != m_outstream);
	    CUtility.ASSERT(null != m_input);
	    CUtility.ASSERT(null != m_tokens);
	    CUtility.ASSERT(null != m_spec);
	  }

	/* EOF found? */
	if (m_input.m_eof_reached)
	  {
	    return;
	  }

	/* Debug checks. */
	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT('%' == m_input.m_line[0]);
	    CUtility.ASSERT('s' == m_input.m_line[1]);
	    CUtility.ASSERT(m_input.m_line_index <= m_input.m_line_read);
	    CUtility.ASSERT(0 <= m_input.m_line_index);
	    CUtility.ASSERT(0 <= m_input.m_line_read);
	  }

	/* Blank line?  No states? */
	if (m_input.m_line_index >= m_input.m_line_read)
	  {
	    return;
	  }

	while (m_input.m_line_index < m_input.m_line_read)
	  {
	    if (CUtility.OLD_DEBUG)
	      {
		System.out.println("line read " + m_input.m_line_read 
				   + "\tline index = " + m_input.m_line_index);
	      }

	    /* Skip white space. */
	    while (CUtility.isspace(m_input.m_line[m_input.m_line_index]))
	      {
		++m_input.m_line_index;
		if (m_input.m_line_index >= m_input.m_line_read)
		  {
		    /* No more states to be found. */
		    return;
		  }
	      }
	    
	    /* Look for state name. */
	    start_state = m_input.m_line_index;
	    while (false == CUtility.isspace(m_input.m_line[m_input.m_line_index])
		   && ',' != m_input.m_line[m_input.m_line_index])
	      {
		++m_input.m_line_index;
		if (m_input.m_line_index >= m_input.m_line_read)
		  {
		    /* End of line and end of state name. */
		    break;
		  }
	      }
	    count_state = m_input.m_line_index - start_state;

	    if (CUtility.OLD_DEBUG)
	      {
		System.out.println("State name \"" 
				   + new String(m_input.m_line,start_state,count_state)
				   + "\".");
		System.out.println("Integer index \"" 
				   + m_spec.m_states.size()
				   + "\".");
	      }

	    /* Enter new state name, along with unique index. */
	    m_spec.m_states.put(new String(m_input.m_line,start_state,count_state),
				new Integer(m_spec.m_states.size()));
	    
	    /* Skip comma. */
	    if (',' == m_input.m_line[m_input.m_line_index])
	      {
		++m_input.m_line_index;
		if (m_input.m_line_index >= m_input.m_line_read)
		  {
		    /* End of line. */
		    return;
		  }
	      }
	  }
      }

  /********************************************************
    Function: expandEscape
    Description: Takes escape sequence and returns
    corresponding character code.
    *******************************************************/
  private char expandEscape
    (
     )
      {
	char r;
	
	/* Debug checks. */
	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(m_input.m_line_index < m_input.m_line_read);
	    CUtility.ASSERT(0 < m_input.m_line_read);
	    CUtility.ASSERT(0 <= m_input.m_line_index);
	  }

	if ('\\' != m_input.m_line[m_input.m_line_index])
	  {
	    ++m_input.m_line_index;
	    return m_input.m_line[m_input.m_line_index - 1];
	  }
	else
	  {
	    boolean unicode_escape = false;
	    ++m_input.m_line_index;
	    switch (m_input.m_line[m_input.m_line_index])
	      {
	      case 'b':
		++m_input.m_line_index;
		return '\b';

	      case 't':
		++m_input.m_line_index;
		return '\t';

	      case 'n':
		++m_input.m_line_index;
		return '\n';

	      case 'f':
		++m_input.m_line_index;
		return '\f';

	      case 'r':
		++m_input.m_line_index;
		return '\r';

	      case '^':
		++m_input.m_line_index;
		r=Character.toUpperCase(m_input.m_line[m_input.m_line_index]);
		if (r<'@' || r>'Z') // non-fatal
		    CError.parse_error(CError.E_BADCTRL,m_input.m_line_number);
		r = (char) (r - '@');
		++m_input.m_line_index;
		return r;

	      case 'u':
		unicode_escape = true;
	      case 'x':
		++m_input.m_line_index;
		r = 0;
		for (int i=0; i<(unicode_escape?4:2); i++)
		  if (CUtility.ishexdigit(m_input.m_line[m_input.m_line_index]))
		    {
		      r = (char) (r << 4);
		      r = (char) (r | CUtility.hex2bin(m_input.m_line[m_input.m_line_index]));
		      ++m_input.m_line_index;
		    }
		  else break;
		
		return r;
		
	      default:
		if (false == CUtility.isoctdigit(m_input.m_line[m_input.m_line_index]))
		  {
		    r = m_input.m_line[m_input.m_line_index];
		    ++m_input.m_line_index;
		  }
		else
		  {
		    r = 0;
		    for (int i=0; i<3; i++)
		      if (CUtility.isoctdigit(m_input.m_line[m_input.m_line_index]))
			{
			  r = (char) (r << 3);
			  r = (char) (r | CUtility.oct2bin(m_input.m_line[m_input.m_line_index]));
			  ++m_input.m_line_index;
			}
		      else break;
		  }
		return r;
	      }
	  }
      }
	
  /********************************************************
    Function: packAccept
    Description: Packages and returns CAccept 
    for action next in input stream.
    *******************************************************/
  CAccept packAccept
    (
     )
      throws java.io.IOException
      {
	CAccept accept;
	char action[];
	int action_index;
	int brackets;
	boolean insinglequotes;
	boolean indoublequotes;
	boolean instarcomment;
	boolean inslashcomment;
	boolean escaped;
	boolean slashed;

	action = new char[BUFFER_SIZE];
	action_index = 0;

	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(null != this);
	    CUtility.ASSERT(null != m_outstream);
	    CUtility.ASSERT(null != m_input);
	    CUtility.ASSERT(null != m_tokens);
	    CUtility.ASSERT(null != m_spec);
	  }

	/* Get a new line, if needed. */
	while (m_input.m_line_index >= m_input.m_line_read)
	  {
	    if (m_input.getLine())
	      {
		CError.parse_error(CError.E_EOF,m_input.m_line_number);
		return null;
	      }
	  }
	
	/* Look for beginning of action. */
	while (CUtility.isspace(m_input.m_line[m_input.m_line_index]))
	  {
	    ++m_input.m_line_index;
	    
	    /* Get a new line, if needed. */
	    while (m_input.m_line_index >= m_input.m_line_read)
	      {
		if (m_input.getLine())
		  {
		    CError.parse_error(CError.E_EOF,m_input.m_line_number);
		    return null;
		  }
	      }
	  }
	
	/* Look for brackets. */
	if ('{' != m_input.m_line[m_input.m_line_index])
	  {
	    CError.parse_error(CError.E_BRACE,m_input.m_line_number); 
	  }
	
	/* Copy new line into action buffer. */
	brackets = 0;
	insinglequotes = indoublequotes = inslashcomment = instarcomment =
	escaped  = slashed = false;
	while (true)
	  {
	    action[action_index] = m_input.m_line[m_input.m_line_index];

	    /* Look for quotes. */
	    if ((insinglequotes || indoublequotes) && escaped)
		escaped=false; // only protects one char, but this is enough.
	    else if ((insinglequotes || indoublequotes) &&
		     '\\' == m_input.m_line[m_input.m_line_index])
		escaped=true;
	    else if (!(insinglequotes || inslashcomment || instarcomment) &&
		     '\"' == m_input.m_line[m_input.m_line_index])
		indoublequotes=!indoublequotes; // unescaped double quote.
	    else if (!(indoublequotes || inslashcomment || instarcomment) &&
		     '\'' == m_input.m_line[m_input.m_line_index])
		insinglequotes=!insinglequotes; // unescaped single quote.
	    /* Look for comments. */
	    if (instarcomment) { // inside "/*" comment; look for "*/"
		if (slashed && '/' == m_input.m_line[m_input.m_line_index])
		    instarcomment = slashed = false;
		else // note that inside a star comment, slashed means starred
		    slashed = ('*' == m_input.m_line[m_input.m_line_index]);
	    } else if (!inslashcomment && !insinglequotes && !indoublequotes) {
	        // not in comment, look for /* or //
		inslashcomment = 
		    (slashed && '/' == m_input.m_line[m_input.m_line_index]);
		instarcomment =
		    (slashed && '*' == m_input.m_line[m_input.m_line_index]);
		slashed = ('/' == m_input.m_line[m_input.m_line_index]);
	    }

	    /* Look for brackets. */
	    if (!insinglequotes && !indoublequotes &&
		!instarcomment && !inslashcomment) {
	      if ('{' == m_input.m_line[m_input.m_line_index])
		{
		  ++brackets;
		}
	      else if ('}' == m_input.m_line[m_input.m_line_index])
		{
		  --brackets;
		
		  if (0 == brackets)
		    {
		      ++action_index;
		      ++m_input.m_line_index;

		      break;
		    }
		}
	    }
	    
	    ++action_index;
	    /* Double the buffer size, if needed. */
	    if (action_index >= action.length)
	      {
		action = CUtility.doubleSize(action);
	      }

	    ++m_input.m_line_index;
	    /* Get a new line, if needed. */
	    while (m_input.m_line_index >= m_input.m_line_read)
	      {
		inslashcomment = slashed = false;
		if (insinglequotes || indoublequotes) { // non-fatal
		    CError.parse_error(CError.E_NEWLINE,m_input.m_line_number);
		    insinglequotes = indoublequotes = false;
		}
		if (m_input.getLine())
		  {
		    CError.parse_error(CError.E_SYNTAX,m_input.m_line_number);
		    return null;
		  }
	      }
	  }
	    
	accept = new CAccept(action,action_index,m_input.m_line_number);

	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(null != accept);
	  }

	if (CUtility.DESCENT_DEBUG)
	  {
	    System.out.print("Accepting action:");
	    System.out.println(new String(accept.m_action,0,accept.m_action_read));
	  }

	return accept;
      }

  /********************************************************
    Function: advance
    Description: Returns code for next token.
    *******************************************************/
  private boolean m_advance_stop = false;
  int advance
    (
     )
      throws java.io.IOException
      {
	boolean saw_escape = false;
	Integer code;
	
	/*if (m_input.m_line_index > m_input.m_line_read) {
	  System.out.println("m_input.m_line_index = " + m_input.m_line_index);
	  System.out.println("m_input.m_line_read = " + m_input.m_line_read);
	  CUtility.ASSERT(m_input.m_line_index <= m_input.m_line_read);
	}*/

	if (m_input.m_eof_reached)
	  {
	    /* EOF has already been reached,
	       so return appropriate code. */

	    m_spec.m_current_token = END_OF_INPUT;
	    m_spec.m_lexeme = '\0';
	    return m_spec.m_current_token;
	  }

	/* End of previous regular expression?
	   Refill line buffer? */
	if (EOS == m_spec.m_current_token
	    /* ADDED */
	    || m_input.m_line_index >= m_input.m_line_read)
	    /* ADDED */
	  {
	    if (m_spec.m_in_quote)
	      {
		CError.parse_error(CError.E_SYNTAX,m_input.m_line_number);
	      }
	    
	    while (true)
	      {
		if (false == m_advance_stop  
		    || m_input.m_line_index >= m_input.m_line_read)
		  {
		    if (m_input.getLine())
		      {
			/* EOF has already been reached,
			   so return appropriate code. */
			
			m_spec.m_current_token = END_OF_INPUT;
			m_spec.m_lexeme = '\0';
			return m_spec.m_current_token;
		      }
		    m_input.m_line_index = 0;
		  }
		else
		  {
		    m_advance_stop = false;
		  }

		while (m_input.m_line_index < m_input.m_line_read
		       && true == CUtility.isspace(m_input.m_line[m_input.m_line_index]))
		  {
		    ++m_input.m_line_index;
		  }
		
		if (m_input.m_line_index < m_input.m_line_read)
		  {
		    break;
		  }
	      }
	  }
	
	if (CUtility.DEBUG) {
	  CUtility.ASSERT(m_input.m_line_index <= m_input.m_line_read);
	}

	while (true)
	  {
	    if (false == m_spec.m_in_quote
		&& '{' == m_input.m_line[m_input.m_line_index])
	      {
		if (false == expandMacro())
		  {
		    break;
		  }
	       
		if (m_input.m_line_index >= m_input.m_line_read)
		  {
		    m_spec.m_current_token = EOS;
		    m_spec.m_lexeme = '\0';
		    return m_spec.m_current_token;
		  }
	      }
	    else if ('\"' == m_input.m_line[m_input.m_line_index])
	      {
		m_spec.m_in_quote = !m_spec.m_in_quote;
		++m_input.m_line_index;
		
		if (m_input.m_line_index >= m_input.m_line_read)
		  {
		    m_spec.m_current_token = EOS;
		    m_spec.m_lexeme = '\0';
		    return m_spec.m_current_token;
		  }
	      }
	    else
	      {
		break;
	      }
	  }

	if (m_input.m_line_index > m_input.m_line_read) {
	  System.out.println("m_input.m_line_index = " + m_input.m_line_index);
	  System.out.println("m_input.m_line_read = " + m_input.m_line_read);
	  CUtility.ASSERT(m_input.m_line_index <= m_input.m_line_read);
	}

	/* Look for backslash, and corresponding 
	   escape sequence. */
	if ('\\' == m_input.m_line[m_input.m_line_index])
	  {
	    saw_escape = true;
	  }
	else
	  {
	    saw_escape = false;
	  }

	if (false == m_spec.m_in_quote)
	  {
	    if (false == m_spec.m_in_ccl &&
		CUtility.isspace(m_input.m_line[m_input.m_line_index]))
	      {
		/* White space means the end of 
		   the current regular expression. */

		m_spec.m_current_token = EOS;
		m_spec.m_lexeme = '\0';
		return m_spec.m_current_token;
	      }

	    /* Process escape sequence, if needed. */
	    if (saw_escape)
	      {
		m_spec.m_lexeme = expandEscape();
	      }
	    else
	      {
		m_spec.m_lexeme = m_input.m_line[m_input.m_line_index];
		++m_input.m_line_index;
	      }
	  }
	else
	  {
	    if (saw_escape 
		&& (m_input.m_line_index + 1) < m_input.m_line_read
		&& '\"' == m_input.m_line[m_input.m_line_index + 1])
	      {
		m_spec.m_lexeme = '\"';
		m_input.m_line_index = m_input.m_line_index + 2;
	      }
	    else
	      {
		m_spec.m_lexeme = m_input.m_line[m_input.m_line_index];
		++m_input.m_line_index;
	      }
	  }
	
	code = (Integer) m_tokens.get(new Character(m_spec.m_lexeme));
	if (m_spec.m_in_quote || true == saw_escape)
	  {
	    m_spec.m_current_token = L;
	  }
	else
	  {
	    if (null == code)
	      {
		m_spec.m_current_token = L;
	      }
	    else
	      {
		m_spec.m_current_token = code.intValue();
	      }
	  }

	if (CCL_START == m_spec.m_current_token) m_spec.m_in_ccl = true;
	if (CCL_END   == m_spec.m_current_token) m_spec.m_in_ccl = false;

	if (CUtility.FOODEBUG)
	  {
	    System.out.println("Lexeme: " + m_spec.m_lexeme
			       + "\tToken: " + m_spec.m_current_token
			       + "\tIndex: " + m_input.m_line_index);
	  }

	return m_spec.m_current_token;
      }

  /***************************************************************
    Function: details
    Description: High level debugging routine.
    **************************************************************/
  private void details
    (
     )
      {
	Enumeration names;
	String name;
	String def;
	Enumeration states;
	String state;
	Integer index;
	int elem;
	int size;

	System.out.println();
	System.out.println("\t** Macros **");
	names = m_spec.m_macros.keys();
	while (names.hasMoreElements())
	  {
	    name = (String) names.nextElement();
	    def = (String) m_spec.m_macros.get(name);

	    if (CUtility.DEBUG)
	      {
		CUtility.ASSERT(null != name);
		CUtility.ASSERT(null != def);
	      }

	    System.out.println("Macro name \"" + name 
			       + "\" has definition \"" 
			       + def + "\".");
	  }

	System.out.println();
	System.out.println("\t** States **");
	states = m_spec.m_states.keys();
	while (states.hasMoreElements())
	  {
	    state = (String) states.nextElement();
	    index = (Integer) m_spec.m_states.get(state);

	    if (CUtility.DEBUG)
	      {
		CUtility.ASSERT(null != state);
		CUtility.ASSERT(null != index);
	      }

	    System.out.println("State \"" + state 
			       + "\" has identifying index " 
			       + index.toString() + ".");
	  }
	    
	System.out.println();
	System.out.println("\t** Character Counting **");
	if (false == m_spec.m_count_chars)
	  {
	    System.out.println("Character counting is off.");
	  }
	else
	  {
	    if (CUtility.DEBUG)
	      {
		CUtility.ASSERT(m_spec.m_count_lines);
	      }

	    System.out.println("Character counting is on.");
	  }

	System.out.println();
	System.out.println("\t** Line Counting **");
	if (false == m_spec.m_count_lines)
	  {
	    System.out.println("Line counting is off.");
	  }
	else
	  {
	    if (CUtility.DEBUG)
	      {
		CUtility.ASSERT(m_spec.m_count_lines);
	      }

	    System.out.println("Line counting is on.");
	  }

	System.out.println();
	System.out.println("\t** Operating System Specificity **");
	if (false == m_spec.m_unix)
	  {
	    System.out.println("Not generating UNIX-specific code.");
	    System.out.println("(This means that \"\\r\\n\" is a "
			       + "newline, rather than \"\\n\".)");
	  }
	else
	  {
	    System.out.println("Generating UNIX-specific code.");
	    System.out.println("(This means that \"\\n\" is a " 
			       + "newline, rather than \"\\r\\n\".)");
	  }

	System.out.println();
	System.out.println("\t** Java CUP Compatibility **");
	if (false == m_spec.m_cup_compatible)
	  {
	    System.out.println("Generating CUP compatible code.");
	    System.out.println("(Scanner implements "
			       + "java_cup.runtime.Scanner.)");
	  }
	else
	  {
	    System.out.println("Not generating CUP compatible code.");
	  }
	
	if (CUtility.FOODEBUG) {
	  if (null != m_spec.m_nfa_states && null != m_spec.m_nfa_start)
	    {
	      System.out.println();
	      System.out.println("\t** NFA machine **");
	      print_nfa();
	  }
	}

	if (null != m_spec.m_dtrans_vector)
	  {
	    System.out.println();
	    System.out.println("\t** DFA transition table **");
	    /*print_header();*/
	  }

	/*if (null != m_spec.m_accept_vector && null != m_spec.m_anchor_array)
	  {
	    System.out.println();
	    System.out.println("\t** Accept States and Anchor Vector **");
	    print_accept();
	  }*/
      }

  /***************************************************************
    function: print_set
    **************************************************************/
  void print_set
    (
     Vector nfa_set
     )
      {
	int size; 
	int elem;
	CNfa nfa;

	size = nfa_set.size();

	if (0 == size)
	  {
	    System.out.print("empty ");
	  }
	
	for (elem = 0; elem < size; ++elem)
	  {
	    nfa = (CNfa) nfa_set.elementAt(elem);
	    /*System.out.print(m_spec.m_nfa_states.indexOf(nfa) + " ");*/
	    System.out.print(nfa.m_label + " ");
	  }
      }

   /***************************************************************
     Function: print_header
     **************************************************************/
  private void print_header
    (
     )
      {
	Enumeration states;
	int i;
	int j;
	int chars_printed=0;
	CDTrans dtrans;
	int last_transition;
	String str;
	CAccept accept;
	String state;
	Integer index;

	System.out.println("/*---------------------- DFA -----------------------");
	
	states = m_spec.m_states.keys();
	while (states.hasMoreElements())
	  {
	    state = (String) states.nextElement();
	    index = (Integer) m_spec.m_states.get(state);

	    if (CUtility.DEBUG)
	      {
		CUtility.ASSERT(null != state);
		CUtility.ASSERT(null != index);
	      }

	    System.out.println("State \"" + state 
			       + "\" has identifying index " 
			       + index.toString() + ".");

	    i = index.intValue();
	    if (CDTrans.F != m_spec.m_state_dtrans[i])
	      {
		System.out.println("\tStart index in transition table: "
				   + m_spec.m_state_dtrans[i]);
	      }
	    else
	      {
		System.out.println("\tNo associated transition states.");
	      }
	  }

	for (i = 0; i < m_spec.m_dtrans_vector.size(); ++i)
	  {
	    dtrans = (CDTrans) m_spec.m_dtrans_vector.elementAt(i);

	    if (null == m_spec.m_accept_vector && null == m_spec.m_anchor_array)
	      {
		if (null == dtrans.m_accept)
		  {
		    System.out.print(" * State " + i + " [nonaccepting]");
		  }
		else
		  {
		    System.out.print(" * State " + i 
				     + " [accepting, line "
				     + dtrans.m_accept.m_line_number 
				     + " <"
				     + (new String(dtrans.m_accept.m_action,0,
						   dtrans.m_accept.m_action_read))
				     + ">]");
		    if (CSpec.NONE != dtrans.m_anchor)
		      {
			System.out.print(" Anchor: "
					 + ((0 != (dtrans.m_anchor & CSpec.START)) 
					    ? "start " : "")
					 + ((0 != (dtrans.m_anchor & CSpec.END)) 
					    ? "end " : ""));
		      }
		  }
	      }
	    else
	      {
		accept = (CAccept) m_spec.m_accept_vector.elementAt(i);

		if (null == accept)
		  {
		    System.out.print(" * State " + i + " [nonaccepting]");
		  }
		else
		  {
		    System.out.print(" * State " + i 
				     + " [accepting, line "
				     + accept.m_line_number 
				     + " <"
				     + (new String(accept.m_action,0,
						   accept.m_action_read))
				     + ">]");
		    if (CSpec.NONE != m_spec.m_anchor_array[i])
		      {
			System.out.print(" Anchor: "
					 + ((0 != (m_spec.m_anchor_array[i] & CSpec.START)) 
					    ? "start " : "")
					 + ((0 != (m_spec.m_anchor_array[i] & CSpec.END)) 
					    ? "end " : ""));
		      }
		  }
	      }

	    last_transition = -1;
	    for (j = 0; j < m_spec.m_dtrans_ncols; ++j)
	      {
		if (CDTrans.F != dtrans.m_dtrans[j])
		  {
		    if (last_transition != dtrans.m_dtrans[j])
		      {
			System.out.println();
			System.out.print(" *    goto " + dtrans.m_dtrans[j]
					 + " on ");
			chars_printed = 0;
		      }
		    
		    str = interp_int((int) j);
		    System.out.print(str);
				
		    chars_printed = chars_printed + str.length(); 
		    if (56 < chars_printed)
		      {
			System.out.println();
			System.out.print(" *             ");
			chars_printed = 0;
		      }
		    
		    last_transition = dtrans.m_dtrans[j];
		  }
	      }
	    System.out.println();
	  }
	System.out.println(" */");
	System.out.println();
      }
}

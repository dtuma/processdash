
package JLex;

/***************************************************************
  Imported Packages
  **************************************************************/
import java.util.Hashtable;
import java.util.Vector;


/***************************************************************
  Class: CSpec
 **************************************************************/
class CSpec
{
  /***************************************************************
    Member Variables
    **************************************************************/
    
  /* Lexical States. */
  Hashtable m_states; /* Hashtable taking state indices (Integer) 
			 to state name (String). */

  /* Regular Expression Macros. */ 
  Hashtable m_macros; /* Hashtable taking macro name (String)
				to corresponding char buffer that
				holds macro definition. */

  /* NFA Machine. */
  CNfa m_nfa_start; /* Start state of NFA machine. */
  Vector m_nfa_states; /* Vector of states, with index
				 corresponding to label. */
  
  Vector m_state_rules[]; /* An array of Vectors of Integers.
				    The ith Vector represents the lexical state
				    with index i.  The contents of the ith 
				    Vector are the indices of the NFA start
				    states that can be matched while in
				    the ith lexical state. */
				    

  int m_state_dtrans[];

  /* DFA Machine. */
  Vector m_dfa_states; /* Vector of states, with index
				 corresponding to label. */
  Hashtable m_dfa_sets; /* Hashtable taking set of NFA states
				  to corresponding DFA state, 
				  if the latter exists. */
  
  /* Accept States and Corresponding Anchors. */
  Vector m_accept_vector;
  int m_anchor_array[];

  /* Transition Table. */
  Vector m_dtrans_vector;
  int m_dtrans_ncols;
  int m_row_map[];
  int m_col_map[];

  /* Special pseudo-characters for beginning-of-line and end-of-file. */
  static final int NUM_PSEUDO=2;
  int BOL; // beginning-of-line
  int EOF; // end-of-line

  /** NFA character class minimization map. */
  int m_ccls_map[];

  /* Regular expression token variables. */
  int m_current_token;
  char m_lexeme;
  boolean m_in_quote;
  boolean m_in_ccl;

  /* Verbose execution flag. */
  boolean m_verbose;

  /* JLex directives flags. */
  boolean m_integer_type;
  boolean m_intwrap_type;
  boolean m_yyeof;
  boolean m_count_chars;
  boolean m_count_lines;
  boolean m_cup_compatible;
  boolean m_unix;
  boolean m_public;
  boolean m_ignorecase;

  char m_init_code[];
  int m_init_read;

  char m_init_throw_code[];
  int m_init_throw_read;

  char m_class_code[];
  int m_class_read;

  char m_eof_code[];
  int m_eof_read;

  char m_eof_value_code[];
  int m_eof_value_read;

  char m_eof_throw_code[];
  int m_eof_throw_read;

  char m_yylex_throw_code[];
  int m_yylex_throw_read;

  /* Class, function, type names. */
  char m_class_name[] = {          
    'Y', 'y', 'l', 
    'e', 'x' 
    };
  char m_implements_name[] = {};
  char m_function_name[] = {
    'y', 'y', 'l', 
    'e', 'x' 
    };
  char m_type_name[] = {
    'Y', 'y', 't', 
    'o', 'k', 'e',
    'n'
    };

  /* Lexical Generator. */
  private CLexGen m_lexGen;

  /***************************************************************
    Constants
    ***********************************************************/
  static final int NONE = 0;
  static final int START = 1;
  static final int END = 2;
  
  /***************************************************************
    Function: CSpec
    Description: Constructor.
    **************************************************************/
  CSpec
    (
     CLexGen lexGen
     )
      {
	m_lexGen = lexGen;

	/* Initialize regular expression token variables. */
	m_current_token = m_lexGen.EOS;
	m_lexeme = '\0';
	m_in_quote = false;
	m_in_ccl = false;

	/* Initialize hashtable for lexer states. */
	m_states = new Hashtable();
	m_states.put(new String("YYINITIAL"),new Integer(m_states.size()));

	/* Initialize hashtable for lexical macros. */
	m_macros = new Hashtable();

	/* Initialize variables for lexer options. */
	m_integer_type = false;
	m_intwrap_type = false;
	m_count_lines = false;
	m_count_chars = false;
	m_cup_compatible = false;
	m_unix = true;
        m_public = false;
	m_yyeof = false;
	m_ignorecase = false;

	/* Initialize variables for JLex runtime options. */
	m_verbose = true;

	m_nfa_start = null;
	m_nfa_states = new Vector();
	
	m_dfa_states = new Vector();
	m_dfa_sets = new Hashtable();

	m_dtrans_vector = new Vector();
	m_dtrans_ncols = CUtility.MAX_SEVEN_BIT + 1;
	m_row_map = null;
	m_col_map = null;

	m_accept_vector = null;
	m_anchor_array = null;

	m_init_code = null;
	m_init_read = 0;

	m_init_throw_code = null;
	m_init_throw_read = 0;

	m_yylex_throw_code = null;
	m_yylex_throw_read = 0;

	m_class_code = null;
	m_class_read = 0;

	m_eof_code = null;
	m_eof_read = 0;

	m_eof_value_code = null;
	m_eof_value_read = 0;

	m_eof_throw_code = null;
	m_eof_throw_read = 0;

	m_state_dtrans = null;

	m_state_rules = null;
      }
}

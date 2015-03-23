
package JLex;

import java.util.Vector;


/***************************************************************
  Class: CDfa
  **************************************************************/
class CDfa 
{
  /***************************************************************
    Member Variables
    ***********************************************************/
  int m_group;
  boolean m_mark;
  CAccept m_accept;
  int m_anchor;
  Vector m_nfa_set;
  SparseBitSet m_nfa_bit;
  int m_label;

  /***************************************************************
    Function: CDfa
    **************************************************************/
  CDfa
    (
     int label
     )
      {
	m_group = 0;
	m_mark = false;

	m_accept = null;
	m_anchor = CSpec.NONE;

	m_nfa_set = null;
	m_nfa_bit = null;

	m_label = label;
      }
}

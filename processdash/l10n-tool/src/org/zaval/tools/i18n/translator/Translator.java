/**
 *     Caption: Zaval Java Resource Editor
 *     $Revision$
 *     $Date$
 *
 *     @author:     Victor Krapivin
 *     @version:    1.3
 *
 * Zaval JRC Editor is a visual editor which allows you to manipulate
 * localization strings for all Java based software with appropriate
 * support embedded.
 *
 * For more info on this product read Zaval Java Resource Editor User's Guide
 * (It comes within this package).
 * The latest product version is always available from the product's homepage:
 * http://www.zaval.org/products/jrc-editor/
 * and from the SourceForge:
 * http://sourceforge.net/projects/zaval0002/
 *
 * Contacts:
 *   Support : support@zaval.org
 *   Change Requests : change-request@zaval.org
 *   Feedback : feedback@zaval.org
 *   Other : info@zaval.org
 *
 * Copyright (C) 2001-2002  Zaval Creative Engineering Group (http://www.zaval.org)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * (version 2) as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.zaval.tools.i18n.translator;

import java.io.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;

import org.zaval.io.*;
import org.zaval.awt.*;
import org.zaval.awt.peer.TreeNode;
import org.zaval.awt.dialog.*;
import org.zaval.util.SafeResourceBundle;

public class Translator
extends Frame
implements TranslatorConstants
{
    public static final String BUNDLE_NAME = "org.zaval.tools.i18n.translator.jrc-editor";
    public String BASE_LANGUAGE = "en";
    public String NATIVE_LANGUAGE = "en";

    private boolean advancedMode;

    private MessageBox2 closeDialog = null;
    private MessageBox2 delDialog = null;
    private MessageBox2 errDialog = null;
    private EmulatedTextField keyName = null;
    private IELabel keyLabel = null;
    private Button keyInsertButton = null;
    private Button keyDeleteButton = null;
    private GridBagLayout textLayout = null;
    private IELabel commLab = null;
    private IELabel keynLab = null;
    private GraphTree tree = null;
    private Panel textPanel = null;
    private Vector langStates = new Vector();

    private MenuItem newBundleMenu, openBundleMenu, openBundleMenuP,
         saveBundleMenu, saveAsBundleMenu,
         genMenu, parseMenu,
         saveXmlBundleMenu, saveUtfBundleMenu,
         loadXmlBundleMenu, loadUtfBundleMenu,
         saveXmlBundleMenuP, saveUtfBundleMenuP,
         loadXmlBundleMenuP, loadUtfBundleMenuP,
         closeMenu, exitMenu;
    private MenuItem newLangMenu;
    private Menu langMenu, fileMenu;
    private MenuItem delMenu, insMenu,  editCopyMenu, editCutMenu, editPasteMenu, editDeleteMenu;
    private Menu helpMenu;
    private MenuItem getHelpMenu, aboutMenu;    
    private MenuItem expandTreeMenu, collapseTreeMenu, expandNodeMenu, collapseNodeMenu;
    private MenuItem statisticsMenu;
    private CheckboxMenuItem showNullsMenu;

    private EmulatedTextField commField = null;
    private IELabel sbl1, sbl2;

    private ToolkitResolver imgres = null;
    private boolean exitInitiated = true;
    private boolean isDirty = false;
    private String wasSelectedKey = null;
//    private String SYS_DIR;
    private BundleManager bundle = new BundleManager();
    private Panel pane = new Panel();
    private Toolbar tool;

    private String [] CLOSE_BUTTONS = new String[3];
    private String [] YESNO_BUTTONS = new String[2];
    private String [] DELETE_BUTTONS = new String[3];

    private MenuItem[] tbar2menu;

    private static final int MAX_PICK_LENGTH = 40;
    private Vector pickList = new Vector(8);
    private int nullsCount = 0;
    private int notCompletedCount = 0;
    private Comparator translationNeededTester;

    private Vector tabOrder = new Vector();
    private String destZipFile;
    private String destDir;
    private ActionListener saveListener, helpListener;

    public Translator( SafeResourceBundle res )
    {
       boolean haveInternalTranslation = res.isDesiredLanguage();
       advancedMode = Boolean.getBoolean("advancedMode");
       if (!advancedMode)
           bundle.setBaseLang(BASE_LANGUAGE);
       NATIVE_LANGUAGE = Locale.getDefault().getLanguage();

       rcTable = res;

       CLOSE_BUTTONS[0] = RC( "dialog.button.yes" );
       CLOSE_BUTTONS[1] = RC( "dialog.button.no" );
       CLOSE_BUTTONS[2] = RC( "dialog.button.cancel" );

       YESNO_BUTTONS[0] = RC( "dialog.button.yes" );
       YESNO_BUTTONS[1] = RC( "dialog.button.no" );

       DELETE_BUTTONS[0] = RC( "dialog.button.delete.all" );
       DELETE_BUTTONS[1] = RC( "dialog.button.delete.this" );
       DELETE_BUTTONS[2] = RC( "dialog.button.cancel" );

       imgres = new ToolkitResolver();
       this.setLayout(new BorderLayout(0,0));
       add("Center", pane);

       tool = new Toolbar();
       tool.add(91,new IELabel(RC("menu.file") + ":"));
       if (advancedMode) {
           tool.add(0, new SpeedButton(imgres.getImage("new.gif",     this)));
           tool.add(1, new SpeedButton(imgres.getImage("load.gif",    this)));
       }
       tool.add(2, new SpeedButton(imgres.getImage("save.gif",    this)));
       if (advancedMode) {
           tool.add(3, new SpeedButton(imgres.getImage("saveas.gif",  this)));
           tool.add(92,new IELabel("+"));
           tool.add(4, new SpeedButton(imgres.getImage("deploy.gif",  this)));
           tool.add(5, new SpeedButton(imgres.getImage("import.gif",  this)));
           tool.add(93,new IELabel(RC("menu.edit") + ":"));
           tool.add(6, new SpeedButton(imgres.getImage("newlang.gif", this)));
           tool.add(7, new SpeedButton(imgres.getImage("del.gif",     this)));
       }
       tool.add(94,new IELabel(RC("menu.help") + ": "));
       tool.add(8, new SpeedButton(imgres.getImage("about.gif",   this)));
       add("North", tool);

       StatusBar panel3 = new StatusBar/*Panel*/();
       panel3.add(new StatusBarElement(sbl1 = new IELabel(),20));
       panel3.add(new StatusBarElement(sbl2 = new IELabel(),80));
       StatusBarElement se = new StatusBarStubbElement(new Panel(), 0, new Dimension(22, 19));
       se.setLayout(new FlowLayout(FlowLayout.LEFT, 1,2));
       se.setType(0);
       panel3.add(se);
       add("South", panel3);

       tree = new GraphTree();
       tree.setResolver(imgres);
       tree.setBackground(Color.white);
       pane.setLayout( new BorderLayout() );
       Panel mainPanel = new BorderedPanel(BorderedPanel.RAISED2/*SUNKEN*/);
       GridBagLayout gbl = new GridBagLayout();
       Panel keyPanel = new Panel( gbl );
       GridBagConstraints c = new GridBagConstraints();
       c.insets.top = 5;
       c.insets.bottom = 3;
       c.insets.left = 5;
       c.insets.right = 5;
       keyLabel = new IELabel( RC("tools.translator.label.key") );
       gbl.setConstraints( keyLabel, c );
       keyPanel.add( keyLabel );
       keyName = new EmulatedTextField();
       c.weightx = 1;
       c.fill = GridBagConstraints.BOTH;
       keyName.setBackground( Color.white );
       gbl.setConstraints( keyName, c );
       keyPanel.add( keyName );
       keyInsertButton = new Button( RC( "tools.translator.label.insert" ) );
       keyDeleteButton = new Button( RC( "tools.translator.label.delete" ) );
       c.weightx = 0;
       c.fill = GridBagConstraints.NONE;
       gbl.setConstraints( keyInsertButton, c );
       keyPanel.add( keyInsertButton );
       gbl.setConstraints( keyDeleteButton, c );
       keyPanel.add( keyDeleteButton );
       if (advancedMode)
           pane.add( keyPanel, "South" );
       pane.add( mainPanel, "Center" );
       ResizeLayout resizeLayout = new ResizeLayout();
       Resizer rss = new Resizer();
       textPanel = new Panel();
       SimpleScrollPanel scrPanel = new SimpleScrollPanel( textPanel );
       setBackground( Color.lightGray );
       mainPanel.setLayout( resizeLayout );
       mainPanel.add(tree);
       mainPanel.add(scrPanel);
       mainPanel.add(rss);
       textLayout = new GridBagLayout();
       textPanel.setLayout( textLayout );

       tabOrder.add(tree);
       tabOrder.add(keyName);
       tabOrder.add(keyInsertButton);
       tabOrder.add(keyDeleteButton);

       MenuBar menuBar = new MenuBar();
       fileMenu = new Menu( RC( "menu.file" ) );
       newBundleMenu = new MenuItem( RC( "tools.translator.menu.new.bundle" ) , new MenuShortcut(KeyEvent.VK_N) );
       openBundleMenu = new MenuItem( RC( "tools.translator.menu.open" ) , new MenuShortcut(KeyEvent.VK_O)  );
       saveBundleMenu = new MenuItem( RC( "tools.translator.menu.save" ) , new MenuShortcut(KeyEvent.VK_S)  );
       saveBundleMenu.disable();
       saveAsBundleMenu = new MenuItem( RC( "tools.translator.menu.saveas" ) );
       saveAsBundleMenu.disable();
       closeMenu = new MenuItem( RC( "tools.translator.menu.close" ) );
       closeMenu.disable();
       exitMenu = new MenuItem( RC( "menu.exit" ) );

       Menu editMenu = new Menu( RC( "menu.edit" ) );

       editCopyMenu = new MenuItem( RC( "tools.translator.menu.edit.copy" ) /* , new MenuShortcut(KeyEvent.VK_C) */ );
       editCutMenu = new MenuItem( RC( "tools.translator.menu.edit.cut" )  /* , new MenuShortcut(KeyEvent.VK_X) */ );
       editPasteMenu = new MenuItem( RC( "tools.translator.menu.edit.paste" ) /* , new MenuShortcut(KeyEvent.VK_V) */ );
       editDeleteMenu = new MenuItem( RC( "tools.translator.menu.edit.delete" ) );

       newLangMenu = new MenuItem( RC( "tools.translator.menu.new.lang" ) , new MenuShortcut(KeyEvent.VK_L)  );
       delMenu = new MenuItem(RC( "tools.translator.menu.delete") , new MenuShortcut(KeyEvent.VK_D)  );
       insMenu = new MenuItem(RC( "tools.translator.menu.insert") , new MenuShortcut(KeyEvent.VK_I)  );

       Menu treeMenu = new Menu( RC( "menu.tree" ) );
       expandNodeMenu = new MenuItem( RC( "tools.translator.menu.node.expand") );
       collapseNodeMenu = new MenuItem(RC( "tools.translator.menu.node.collapse") );
       expandTreeMenu = new MenuItem( RC( "tools.translator.menu.expand" ) );
       collapseTreeMenu = new MenuItem(RC( "tools.translator.menu.collapse" ));

       Menu viewMenu = new Menu( RC( "menu.options" ) );
       statisticsMenu = new MenuItem( RC( "tools.translator.menu.statistics" ) );
       showNullsMenu = new CheckboxMenuItem( RC( "tools.translator.menu.nulls" ), true );
       langMenu = new Menu( RC( "tools.translator.menu.showres" ) );
       langMenu.disable();

       helpMenu = new Menu( RC("menu.help") );
       getHelpMenu = new MenuItem(RC("menu.help"));
       aboutMenu = new MenuItem(RC("menu.about"));

       Menu toolMenu = new Menu( RC("tools.translator.menu.tools") );
       genMenu = new MenuItem( RC( "tools.translator.menu.generate" ) );
       genMenu.disable();
       parseMenu = new MenuItem( RC( "tools.translator.menu.parse" ) );
       saveXmlBundleMenu = new MenuItem( RC( "tools.translator.menu.save.xml" ) );
       saveUtfBundleMenu = new MenuItem( RC( "tools.translator.menu.save.utf") );
       loadXmlBundleMenu = new MenuItem( RC( "tools.translator.menu.load.xml" ) );
       loadUtfBundleMenu = new MenuItem( RC( "tools.translator.menu.load.utf" ) );

       saveXmlBundleMenuP= new MenuItem( RC( "tools.translator.menu.save.xml.part" ) );
       saveUtfBundleMenuP= new MenuItem( RC( "tools.translator.menu.save.utf.part") );
       loadXmlBundleMenuP= new MenuItem( RC( "tools.translator.menu.load.xml.part" ) );
       loadUtfBundleMenuP= new MenuItem( RC( "tools.translator.menu.load.utf.part" ) );
       openBundleMenuP   = new MenuItem( RC( "tools.translator.menu.load.part" ) );

       if (advancedMode) {
           fileMenu.add(newBundleMenu);
           fileMenu.add(openBundleMenu);
       }
       fileMenu.add(saveBundleMenu);
       if (advancedMode) {
           fileMenu.add(saveAsBundleMenu);
           fileMenu.add(closeMenu);
       }
       fileMenu.addSeparator();

       editMenu.add(newLangMenu);
       editMenu.addSeparator();
       editMenu.add(editCopyMenu);
       editMenu.add(editCutMenu);
       editMenu.add(editPasteMenu);
       editMenu.add(editDeleteMenu);
       if (advancedMode) {
           editMenu.addSeparator();
           editMenu.add(insMenu);
           editMenu.add(delMenu);
       }

       treeMenu.add(expandNodeMenu);
       treeMenu.add(collapseNodeMenu);
       treeMenu.addSeparator();
       treeMenu.add(expandTreeMenu);
       treeMenu.add(collapseTreeMenu);

       viewMenu.add( langMenu );
       viewMenu.add( showNullsMenu );
       viewMenu.add( statisticsMenu );

       toolMenu.add(loadXmlBundleMenu);
       toolMenu.add(loadUtfBundleMenu);
       toolMenu.add(saveXmlBundleMenu);
       toolMenu.add(saveUtfBundleMenu);
       toolMenu.addSeparator();
       toolMenu.add(openBundleMenuP);
       toolMenu.add(loadXmlBundleMenuP);
       toolMenu.add(loadUtfBundleMenuP);
       toolMenu.add(saveXmlBundleMenuP);
       toolMenu.add(saveUtfBundleMenuP);
       toolMenu.addSeparator();
       toolMenu.add(genMenu);
       toolMenu.add(parseMenu);

       
       helpMenu.add(aboutMenu);

       menuBar.add( fileMenu );
       menuBar.add( editMenu );
       menuBar.add( viewMenu );
       menuBar.add( treeMenu );
       if (advancedMode) menuBar.add( toolMenu );
       menuBar.add( helpMenu );
       setMenuBar( menuBar );

       delDialog = new MessageBox2( this );
       delDialog.setIcon( imgres.getImage("ogo.gif", delDialog));
       delDialog.setTitle( RC( "dialog.title.warning" ) );
       delDialog.setButtons( DELETE_BUTTONS );
       delDialog.addListener( this );

       closeDialog = new MessageBox2( this );
       closeDialog.setText( RC( "tools.translator.message.save" ) );
       closeDialog.setTitle( RC( "dialog.title.warning" ) );
       closeDialog.setIcon( imgres.getImage("ogo.gif", closeDialog));
       closeDialog.setButtons( CLOSE_BUTTONS );
       closeDialog.addListener( this );

       errDialog = new MessageBox2( this );
       errDialog.setText( "" );
       errDialog.setTitle( RC( "dialog.title.warning" ) );
       errDialog.setIcon( imgres.getImage("stop.gif", errDialog));
       String [] OK_BUT = { RC( "dialog.button.ok" ) };
       errDialog.setButtons( OK_BUT );

       MenuItem[] _tbar2menu = {
           newBundleMenu, openBundleMenu, saveBundleMenu, saveAsBundleMenu,
           genMenu, parseMenu, newLangMenu, delMenu, getHelpMenu
       };
       tbar2menu = _tbar2menu;

//       if (advancedMode || haveInternalTranslation)
//           onNewBundle();
//       else
//           loadInternalBundle();
    }

    public boolean handleEvent(Event e)
    {
       if (e.id == Event.WINDOW_DESTROY)
       {
          onClose();
          return true;
       }
       if ( e.target == tree )
       {
          if ( e.id == REMOVE_REQUIRED ) onDeleteKey();
          if ( e.target == tree && wasSelectedKey != tree.getSelectedText() )
             setTranslations();
          // actually, we should just list all events that tree can handle itself.
          if ( e.key != (int)'\t' && e.key != Event.F4 && e.key != Event.F10) return true;
       }
       return super.handleEvent(e);
    }

    public boolean keyDown( Event e, int key )
    {
       if ( e.target == keyName && key == Event.ENTER ){
         onInsertKey();
         return true;
       }
       if ( key == (int)'\t' ){
         moveFocus();
         return true;
       }
       if ( e.target instanceof Button && key == Event.ENTER ){
         action(e, null);
         return true;
       }
 // to be corrected if keyName wants to receive "Enter" from JTextField
       return false;
    }

    public boolean action(Event e, Object arg)
    {
       if ( e.target instanceof Toolbar ){
           int pos = Integer.parseInt((String)arg);
           if(pos < 0 || pos >= tbar2menu.length) return false;
           e.target = tbar2menu[pos];
       }

       if ( e.target == statisticsMenu ) onStatistics();

       if ( e.target == expandNodeMenu ) expand(tree.getSelectedNode());
       if ( e.target == collapseNodeMenu ) collapse(tree.getSelectedNode());
       if ( e.target == expandTreeMenu ) tree.expandAll(null);
       if ( e.target == collapseTreeMenu ) tree.collapseAll(null);

       if ( e.target == editCopyMenu){
          Component ccur = getFocusOwner();
          if(ccur instanceof EmulatedTextField){
             EmulatedTextField cur = (EmulatedTextField)ccur;
             cur.blCopy();
          }
          else if(ccur instanceof TextField){
             TextField cur = (TextField)ccur;
             java.awt.datatransfer.Clipboard c = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
             java.awt.datatransfer.StringSelection s2 = new java.awt.datatransfer.StringSelection(cur.getSelectedText());
             c.setContents(s2,s2);
          }
       }
       if ( e.target == editCutMenu){
          Component ccur = getFocusOwner();
          if(ccur instanceof EmulatedTextField){
             EmulatedTextField cur = (EmulatedTextField)ccur;
             cur.blCopy();
             cur.blDelete();
          }
          else if(ccur instanceof TextField){
             TextField cur = (TextField)ccur;
             java.awt.datatransfer.Clipboard c = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
             java.awt.datatransfer.StringSelection s2 = new java.awt.datatransfer.StringSelection(cur.getSelectedText());
             c.setContents(s2,s2);
             if(cur.getSelectedText().length() > 0)
                cur.setText(
                    cur.getText().substring(0, cur.getSelectionStart()) +
                    cur.getText().substring(cur.getSelectionEnd()));
          }
       }
       if ( e.target == editPasteMenu){
          Component ccur = getFocusOwner();
          if(ccur instanceof EmulatedTextField){
             EmulatedTextField cur = (EmulatedTextField)ccur;
             cur.blPaste();
          }
          else if(ccur instanceof TextField){
             TextField cur = (TextField)ccur;

             java.awt.datatransfer.Clipboard c = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
             java.awt.datatransfer.Transferable t = c.getContents("e");

             String nt = "";
             if(t.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor))
                try{
                   nt = (String)t.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
                }
                catch(Exception ex){
                }

             if(cur.getSelectedText().length() > 0)
                cur.setText(
                    cur.getText().substring(0, cur.getSelectionStart()) +
                    nt +
                    cur.getText().substring(cur.getSelectionEnd()));
             else
                cur.setText(
                    cur.getText().substring(0, cur.getCaretPosition()) +
                    nt +
                    cur.getText().substring(cur.getCaretPosition()));
          }
       }
       if ( e.target == editDeleteMenu){
          Component ccur = getFocusOwner();
          if(ccur instanceof EmulatedTextField){
             EmulatedTextField cur = (EmulatedTextField)ccur;
             cur.blDelete();
          }
          else if(ccur instanceof TextField){
             TextField cur = (TextField)ccur;
             if(cur.getSelectedText().length() > 0)
                cur.setText(
                    cur.getText().substring(0, cur.getSelectionStart()) +
                    cur.getText().substring(cur.getSelectionEnd()));
          }
       }

       if ( e.target == newLangMenu ) onNewResource();
       if ( e.target == insMenu ) onNewKey();
       if ( e.target == newBundleMenu ) onNewBundle();
       if ( e.target == closeMenu ){
          exitInitiated = false;
          onClose();
       }
       if ( e.target == openBundleMenu ) onOpen(false);
       if ( e.target == saveBundleMenu ) onSave();
       if ( e.target == saveAsBundleMenu ) onSaveAs();
       if ( e.target == exitMenu ) onClose();
       if ( e.target == keyInsertButton ) onInsertKey();
       if ( e.target == keyDeleteButton || e.target == delMenu) onDeleteKey();
       if ( e.target == genMenu) onGenCode();
       if ( e.target == parseMenu) onParseCode();
       if ( e.target == getHelpMenu) onHelp();
       if ( e.target == aboutMenu) onAbout();

       if ( e.target == loadXmlBundleMenu) onLoadXml(false);
       if ( e.target == saveXmlBundleMenu) onSaveXml(false);
       if ( e.target == loadUtfBundleMenu) onLoadUtf(false);
       if ( e.target == saveUtfBundleMenu) onSaveUtf(false);

       if ( e.target == openBundleMenuP ) onOpen(true);
       if ( e.target == loadXmlBundleMenuP) onLoadXml(true);
       if ( e.target == saveXmlBundleMenuP) onSaveXml(true);
       if ( e.target == loadUtfBundleMenuP) onLoadUtf(true);
       if ( e.target == saveUtfBundleMenuP) onSaveUtf(true);


       if ( e.target instanceof CheckboxMenuItem && e.target == showNullsMenu ) {
           setIndicators( tree.getRootNode() );
           tree.repaint();
       }
       if ( e.target instanceof CheckboxMenuItem )
           for ( int i = 0; i < langStates.size(); i++ ){
               LangState ls = getLangState(i);
               if ( e.target == ls.box ){
                   ls.hidden = !ls.hidden;
                   ls.tf.setVisible( !ls.hidden );
                   ls.label.setVisible( !ls.hidden );
                   setIndicators( tree.getRootNode() );
                   textPanel.invalidate();
                   validate();
               }
               ls.box.setState(!ls.hidden);
           }
       if ( e.target == closeDialog && e.arg instanceof Button ){
          if ( !((Button)e.arg).getLabel().equals( CLOSE_BUTTONS[2])){
             if ( ((Button)e.arg).getLabel().equals( CLOSE_BUTTONS[0])) onSave();
             if ( exitInitiated ) finish();
             else clear();
          }
          exitInitiated = true;
       }
       if ( e.target == delDialog && e.arg instanceof Button &&
             ((Button)e.arg).getLabel().equals( DELETE_BUTTONS[0])){
          String key = tree.getSelectedText();
          if ( key != null ){
             isDirty = true;
             TreeNode tn = tree.getNode( wasSelectedKey );
             if ( tn != null ) tn = tn.parent;
             tree.remove( key );
             adjustIndicator( tn );
             tree.repaint();

             bundle.getBundle().removeKeysBeginningWith(key);

             wasSelectedKey = null;
             setTranslations();
          }
       }
       if ( e.target == delDialog && e.arg instanceof Button &&
             ((Button)e.arg).getLabel().equals( DELETE_BUTTONS[1])){
          String key = tree.getSelectedText();
          if ( key != null ){
             isDirty = true;
             TreeNode tn = tree.getNode( wasSelectedKey );
             if(tree.enumChild(tn) == null || tree.enumChild(tn).length == 0){
               if ( tn != null ) tn = tn.parent;
               tree.remove( key );
               adjustIndicator( tn );
               tree.repaint();

               bundle.getBundle().removeKeysBeginningWith(key);
             }else{
               bundle.getBundle().removeKey(key);
             }

             wasSelectedKey = null;
             setTranslations();
          }
       }
       if ( e.target instanceof MenuItem ){
           String lbl = ((MenuItem)e.target).getLabel();
           for(int j = 0;j<pickList.size();++j){
              String path = (String)pickList.elementAt(j);
              String patz = stretchPath(path);
              if(patz.equals(lbl)){
                 clear();
                 readResources( path, false );
                 break;
              }
           }
       }
       sbl1.setText(
          " " + getVisLangCount() + "/" +
          bundle.getBundle().getLangCount() + ", " +
          bundle.getBundle().getItemCount() + " "
          );
       return true;
    }

    private void setTranslations()
    {
      String newKey = tree.getSelectedText();
      if ( wasSelectedKey != null ) {
        for ( int i = 0; i < langStates.size(); i++ ) {
          LangState ls = getLangState(i);
          if(ls.hidden) continue;
          String trans = ls.tf.getText();
          while (trans.endsWith("\r") ||
                 trans.endsWith("\n") ||
                 trans.endsWith("\t"))
              trans = trans.substring(0, trans.length()-1);

          BundleItem bi = bundle.getBundle().getItem(wasSelectedKey);
          if ( bi==null ) {
            // do not add the translation implicitely - disable
            // the language field
            ls.tf.setVisible( false );
            ls.label.setVisible( false );
            commField.setEnabled( false );
          } else {
            if (valueChanged(bi.getTranslation(ls.name), trans))
              isDirty = true ;
            bundle.getBundle().updateValue(wasSelectedKey,ls.name,trans);
          }
        }
        String comm = commField==null ? null : commField.getText();
        if ( comm!=null && comm.trim().length()==0 ) comm = null;
        if ( comm!=null ) {
          BundleItem bi = bundle.getBundle().getItem(wasSelectedKey);
          if ( bi!=null ) bi.setComment(comm);
        }
        adjustIndicator( tree.getNode( wasSelectedKey ) );
        setIndicators(tree.getNode( wasSelectedKey ));
        tree.repaint();
      }
      if ( newKey == null ) return;

      BundleItem bi = bundle.getBundle().getItem(newKey);
      for ( int i = 0; i < langStates.size(); i++ ) {
        LangState ls = getLangState(i);
        String ss = bi==null ? null : bi.getTranslation(ls.name);
        if(ss==null) ss = "";
        if ( bi == null ) {
          ls.tf.setVisible( false );
          ls.label.setVisible( false );
          commField.setEnabled( false );
        } else {
          ls.tf.setVisible( !ls.hidden );
          ls.label.setVisible( !ls.hidden );
          commField.setEnabled( true );
        }
        ls.tf.setText(ss);
      }
      String commText = bi == null ? " ** " + RC("tools.translator.message.noentry") + " **" : bi.getComment();
      if ( commField!=null ) commField.setText( commText==null ? "" : commText );
      keynLab.setText("Key: " + newKey);
      keynLab.repaint();
      sbl2.setText(newKey);
      adjustIndicator( tree.getNode( newKey ) );

      String startValue = "";
      wasSelectedKey = newKey;
      if ( wasSelectedKey != null ) startValue = wasSelectedKey + ".";
      keyName.setText( startValue );
      tree.repaint();
    }

    private boolean valueChanged(String a, String b) {
        a = (a == null ? "" : a.trim());
        b = (b == null ? "" : b.trim());
        return (!a.equals(b));
    }

    public String getValidKey()
    {
       String key = keyName.getText();
       if ( key == null ) return null;
       while( key.endsWith( "." ) ) key = key.substring( 0, key.length() - 1 );
       if ( key.length() <= 0 ) return null;
       String illegalChar = "";
       if ( key.indexOf( '=' ) >= 0 ) illegalChar = "=";
       if ( key.indexOf( '#' ) >= 0 ) illegalChar = "#";
       if ( illegalChar.length() == 0 ) return bundle.replace(key, "..", "");
       MessageBox2 mess = new MessageBox2( this );
       mess.setTitle( RC( "dialog.title.warning" ) );
       mess.setText( bundle.replace( RC( "tools.translator.message.illchar" ),
             "[%illchar%]", illegalChar ) );
       mess.setIcon(imgres.getImage("stop.gif", mess));
       mess.setButtons( RC( "dialog.button.ok" ) );
       mess.show();
       return null;
    }

    public void onInsertKey()
    {
       if ( bundle.getBundle().getLangCount() == 0 ) return;
       String key = getValidKey();
       if ( key != null ){
          addToTree( key );
          bundle.getBundle().addKey( key );
          commField.setText("");
          isDirty = true;
          tree.selectNodeAndOpen( key );
          tree.repaint();
          setTranslations();
          saveBundleMenu.enable();
          saveAsBundleMenu.enable();
          genMenu.enable();
          setIndicators(tree.getRootNode());
          isDirty = true;
       }
       syncToolbar();
    }

    public void onDeleteKey()
    {
       String key = tree.getSelectedText();
       if ( key == null ) return;
       delDialog.setText( bundle.replace( RC( "tools.translator.message.delkey" ), "[%key%]", key ) );
       delDialog.show();
    }

    public void onClose()
    {
       setTranslations();
       if ( isDirty ) closeDialog.show();
       else if ( exitInitiated ) finish();
       else{
          clear();
          exitInitiated = true;
       }
    }

    public void finish()
    {
       dispose();
//       System.exit( 0 );
    }

    public void onSave()
    {
       if(bundle.getBundle().getLangCount()==0) return;
       String fn = bundle.getBundle().getLanguage(0).getLangFile();
       if(fn==null){
          onSaveAs();
          return;
       }
       setTranslations();
       try{
          if (!advancedMode && destZipFile != null)
             bundle.storeAllInZip(destZipFile);
          else
             bundle.store(NATIVE_LANGUAGE, null, destDir);
       }
       catch(Exception e){
          infoException(e);
       }
       isDirty = false;
    }

    public void clear()
    {
       setTitle( null );
       if(keyName!=null) keyName.setText("");
       if(keynLab!=null) keynLab.setText("");
       wasSelectedKey = null;
       textPanel.removeAll();
       langMenu.removeAll();
       if ( tree.getRootNode() != null ) tree.remove( tree.getRootNode() );
       tree.repaint();
       textPanel.invalidate();
       validate();
       isDirty = false;
       bundle = new BundleManager();
       bundle.setSaveListener(saveListener);
       langStates = new Vector();

       closeMenu.disable();
       saveBundleMenu.disable();
       saveAsBundleMenu.disable();
       genMenu.disable();
       langMenu.disable();
    }

    private LangState getLangState(int idx)
    {
       return (LangState)langStates.elementAt(idx);
    }

    private int getVisLangCount()
    {
       int c = 0;
       for ( int i = 0; i < langStates.size(); i++ ){
          LangState ls = getLangState(i);
          if(!ls.hidden) ++c;
       }
       return c;
    }

    private void setAllIndicators()
    {
//       for ( int i = 0; i < langStates.size(); i++ ){
//          LangState ls = getLangState(i);
//          ls.hidden = false;
//          ls.box.setState(true);
//       }
       setIndicators( tree.getRootNode() );
    }

    private boolean setIndicators( TreeNode tn )
    {
       if ( tn == null ) return false;
       boolean res = setIndicators( tn.sibling );
       return setIndicator( tn, setIndicators( tn.child ) ) || res;
    }

    private boolean setIndicator( TreeNode tn, boolean childOn )
    {
       if ( tn == null ) return false;
       if ( getVisLangCount() < 2 ){
          tree.setIndicator( tn.getText(), null );
          return false;
       }
       if ( childOn ){
          tree.setIndicator( tn.getText(), WARN_IMAGE );
          return true;
       }
       boolean isAbs = false;
       boolean isPres = false;
       boolean needsNoTranslation = false;

       BundleItem bi = bundle.getBundle().getItem(tn.getText());
       if(bi==null || needsNoTranslation(bi)){
          tree.setIndicator( tn.getText(), null );
          return false;
       }
       for ( int i = 0; i < langStates.size(); i++ ){
          LangState ls = getLangState(i);
          if(ls.hidden) continue;
          String ts = bi.getTranslation(ls.name);
          if(ts!=null && ts.trim().length()==0) ts = null;
          isPres|= ts != null;
          if(ls.ignoreMissing) continue;
          isAbs |= ts == null;
       }
       tree.setIndicator( tn.getText(), null );
       if ( isAbs && isPres ){
          tree.setIndicator( tn.getText(), WARN_IMAGE );
          notCompletedCount++;
       }else{
         if(isAbs){
           nullsCount++;
           if(showNullsMenu.getState()){
             tree.setIndicator( tn.getText(), WARN_IMAGE );
             return true;
           }
         }
       }
       return isAbs && isPres;
    }

    private boolean needsNoTranslation(BundleItem bi) {
        if (translationNeededTester == null)
            return false;
        String key = bi.getId();
        String defaultTranslation = bi.getTranslation(BASE_LANGUAGE);
        return (translationNeededTester.compare(key, defaultTranslation) == 0);
    }

    private void adjustIndicator( TreeNode tn )
    {
       if ( tn == null ) return;
       setIndicator( tn, isSetInSiblings( tn.child ) );
       adjustIndicator( tn.parent );
    }

    private boolean isSetInSiblings( TreeNode tn )
    {
       if ( tn == null ) return false;
       if ( tn.getIndicator() != null ) return true;
       return isSetInSiblings( tn.sibling );
    }

    private void createNewFile(String filename)
    {
       try{
          FileOutputStream f = new FileOutputStream( filename );
          f.close();
       }
       catch( Exception e ) {
          infoException(e);
       }
    }

    private void onNewResource()
    {
       EditDialog ed = new EditDialog( this, RC( "tools.translator.label.newrestitle" ), true, this );
       ed.setLabelCaption( RC( "tools.translator.label.filesuff" ) );
       ed.setButtonsCaption( RC( "dialog.button.ok" ), CLOSE_BUTTONS[2] );
       Dimension d = ed.preferredSize();
       ed.resize( d );
       ed.toCenter();
       ed.show();
       String text = ed.getText();
       if ( text.length() <= 0 || !ed.isApply()) return;
       bundle.getBundle().addLanguage(text);
       syncLanguage(text);

       for ( int i = 0; i < langStates.size(); i++ ){
          LangState ls = getLangState(i);
          CheckboxMenuItem cmi = ls.box;
          boolean show = cmi.getState();
          ls.tf.setVisible( show );
          ls.hidden = !show;
          ls.label.setVisible( show );
       }
       setAllIndicators();
       textPanel.invalidate();
       validate();
    }

    public void onNewBundle()
    {
       clear();
       initControls();
       bundle.getBundle().addLanguage("en");
       bundle.getBundle().addKey("creationDate");
       bundle.getBundle().updateValue("creationDate", "en", (new Date()).toLocaleString());
       initData(false);
       setTitle(null);
       isDirty = false;
    }

    private SafeResourceBundle rcTable = null;
    private String RC (String key)
    {
       return rcTable.getString(key);
    }

    public void setTitle( String filename )
    {
       String add = "";
       if ( filename != null ) add = " [" + filename + "]";
       super.setTitle( "Zaval JRC Editor" + add );
       sbl2.setText(filename == null ? "" : filename);
    }

    private void join(BundleManager bundle2, boolean part)
    {
       if(part){
           BundleSet set = bundle2.getBundle();
           for (Iterator iter = set.iterator(); iter.hasNext();) {
			BundleItem bi = (BundleItem) iter.next();
              bundle.getBundle().addKey(bi.getId());
              Enumeration en= bi.getLanguages();
              while(en.hasMoreElements()){
                  String lang = (String)en.nextElement();
                  bundle.getBundle().addLanguage(lang);
                  bundle.getBundle().updateValue(bi.getId(), lang, bi.getTranslation(lang));
              }
           }
           
           bundle.addPrefixes(bundle2.getPrefixes());
       }
       else {
           bundle = bundle2;
           if (!advancedMode)
               bundle.getBundle().addLanguage(NATIVE_LANGUAGE);
           bundle.setSaveListener(saveListener);
       }
    }

    public void readResources( String fileName, boolean part )
    {
       setCursor( Cursor.WAIT_CURSOR );
       try{
          BundleManager bundle2 = new BundleManager(fileName);
          join(bundle2, part);
       }
       catch(Exception e){
          infoException(e);
       }
       if (!advancedMode)
           bundle.setBaseLang(BASE_LANGUAGE);

       if(!part) initControls();
       else wasSelectedKey = null;
       initData(part);
       setTitle( fileName );
       isDirty = false;
       addToPickList(fileName);
       setCursor( Cursor.DEFAULT_CURSOR );
    }
    
    public void readResources( String[] fileNames )
    {
        if (fileNames.length == 1) {
            readResources(fileNames[0], false);
        }
        else if (fileNames.length > 1) {
            setCursor( Cursor.WAIT_CURSOR );
            
            try{
                // Initializing the bundle with the first package to translate
                BundleManager bundle = new BundleManager(fileNames[0]);
                join(bundle, false);
                addToPickList(fileNames[0]);

                for (int i = 1; i < fileNames.length; ++i) {
                    // If there are any more packages, we create a BundleManager
                    //  for every one of them and merge them together.
                    bundle = new BundleManager(fileNames[i]);
                    join(bundle, true);
                    addToPickList(fileNames[i]);
                }
            }
            catch(Exception e){
                infoException(e);
            }
            if (!advancedMode)
                bundle.setBaseLang(BASE_LANGUAGE);

            initControls();
            initData(false);
            setTitle(fileNames[0]);
            isDirty = false;
            setCursor( Cursor.DEFAULT_CURSOR );
        }
    }

    private void initControls()
    {
       GridBagConstraints c = new GridBagConstraints();
       c.anchor = GridBagConstraints.NORTH;
       c.fill = GridBagConstraints.HORIZONTAL;
       c.insets.top = 3;
       c.insets.bottom = 15;
       c.insets.right = 0;
       c.insets.left = 10;
       c.gridwidth=c.gridheight=1;
       c.gridx=c.gridy=0;
       c.weightx = 0;
       c.weighty = 0;
       commLab = new IELabel( RC("tools.translator.label.comments") );
       textLayout.setConstraints( commLab, c );
       textPanel.add( commLab );

       c.gridwidth = GridBagConstraints.REMAINDER;
       c.insets.right = 5;
       c.insets.left = 0;
       c.gridx=1;
       commField = new EmulatedTextField();
       commField.setBackground( Color.lightGray );
       textLayout.setConstraints( commField, c );
       textPanel.add( commField );

       IELabel l = new IELabel( "" );
       c.weighty = 1;
       textLayout.setConstraints( l, c );
       textPanel.add( l );

       c.gridwidth = 2;
       c.gridy = 1;
       c.gridx = 0;
       c.insets.left = 10;
       c.insets.top = 0;
       c.insets.bottom = 5;
       c.weighty = 0;
       keynLab = new IELabel("");
       textLayout.setConstraints( keynLab, c );
       textPanel.add( keynLab );

       langMenu.enable();
    }

    private void syncLanguage(String lang)
    {
       LangItem lang2 = bundle.getBundle().getLanguage(lang);
       int i = bundle.getBundle().getLangIndex(lang);

       String langLab = lang2.getLangDescription();
       LangState ls = new LangState();
       ls.name = lang2.getLangId();
       ls.box = new CheckboxMenuItem( langLab, false );
       ls.box.setState( true );
       ls.label = new IELabel( langLab + ":", IELabel.LEFT );
       ls.tf = new TextAreaWrap();
       if (!advancedMode && BASE_LANGUAGE.equals(lang2.getLangId())) {
           ls.tf.getControl().setBackground( Color.lightGray );
           ls.tf.setEditable(false);
           ls.ignoreMissing = true;
           ls.hidden = false;
       } else {
           ls.tf.getControl().setBackground( Color.white );
           if (!advancedMode &&
               NATIVE_LANGUAGE != null &&
               !NATIVE_LANGUAGE.equals(lang2.getLangId())) {
               ls.hidden = true;
               ls.tf.setVisible( false );
               ls.label.setVisible( false );
               ls.box.setState( false );
           }
       }
       ls.tf.setLocale(new Locale(lang, ""));

       langStates.addElement(ls);
       langMenu.add( ls.box );

       GridBagConstraints c = new GridBagConstraints();
       c.anchor = GridBagConstraints.NORTH;
       c.fill = GridBagConstraints.HORIZONTAL;
       c.gridheight=1;
       c.gridy=0;
       c.insets.top = 3;
       c.insets.bottom = 3;
       c.gridwidth = 1;

       c.gridy = i + 2;
       c.gridx=0;
       c.weightx = 0;
       c.weighty = 0;
       c.insets.right = 0;
       c.insets.left = 10;
       textLayout.setConstraints( ls.label, c );

       textPanel.add( ls.label );
       c.gridwidth = 1;
       c.weightx = 1;
       c.gridx=1;
       c.insets.right = 5;
       c.insets.left = 0;
       textLayout.setConstraints( ls.tf.getControl(), c );
       textPanel.add( ls.tf.getControl() );
    }

    private boolean shouldShowLang(String lang) {
        return advancedMode ||
            lang == null ||
            lang.equals(BASE_LANGUAGE) ||
            lang.equals(NATIVE_LANGUAGE);
    }

    private void addToTree( String s )
    {
       TreeNode tnew = tree.getNode( s );
       if ( tnew != null ) return;
       int ind = getIndexOfLast(s, "" + KEY_SEPARATOR + KEY_SEPARATOR_2 );

       tnew = new TreeNode( s );
       if ( ind < 0 ){
          tnew.caption = s;
          tree.insertRoot( s );
       }
       else{
          String tname = s.substring( 0, ind );
          addToTree( tname );
          TreeNode ttpar = tree.getNode( tname );
          tnew.caption = s.substring( ind + 1 );
          tree.insert( tnew, ttpar, LevelTree.CHILD );
       }
       tree.setImages( tnew.getText(), OPEN_IMAGE, CLOSE_IMAGE );
    }

    // returns the index of the last character in s which is in the
    // string delims, but is not in a terminal sequence of delimiters.
    private int getIndexOfLast(String s, String delims) {
        int pos = s.length();
        if (pos < 2) return -1;
        while (pos-- > 0) {
            if (delims.indexOf(s.charAt(pos)) == -1)
                break; // we've found a character that isn't a delimiter.
        }
        while (pos-- > 0) {
            if (delims.indexOf(s.charAt(pos)) != -1)
                return pos;  // we've found a nonterminal delimiter char.
        }
        return -1;
    }

    public void onSaveAs()
    {
       FileDialog openFileDialog1 = new FileDialog(this, RC("tools.translator.label.saveastitle"), FileDialog.SAVE);
       String mask = "*" + RES_EXTENSION;
       openFileDialog1.setDirectory(".");
       String fn = bundle.getBundle().getLanguage(0).getLangFile();
       if(fn==null) fn = "autosaved";
       openFileDialog1.setFile( bundle.baseName(fn) + RES_EXTENSION );
       openFileDialog1.show();

       String filename = openFileDialog1.getFile();
       if( filename != null )
          try{
             filename = openFileDialog1.getDirectory() + filename;
             bundle.store( filename );
          }
          catch(Exception e){
             infoException(e);
          }
       addToPickList(filename);
       setTitle( filename );
       isDirty = false;
    }

    private void infoException(Exception e)
    {
       e.printStackTrace();
       try{
          ByteArrayOutputStream ba = new ByteArrayOutputStream();
          PrintStream p = new PrintStream(ba);
          e.printStackTrace(p);
          p.close();
          String msg = new String(ba.toByteArray(), 0);

          String hdr = e.getMessage()==null ? e.toString() : e.getMessage();
          hdr = hdr + "\n" + msg;
          errDialog.setText( hdr );
          errDialog.show();
       }
       catch(Exception z){
       }
    }

    private void onHelp() {
        if (helpListener != null) {
            try {
                helpListener.actionPerformed(new ActionEvent(this,
                        ActionEvent.ACTION_PERFORMED, "l10nTool"));
            } catch (Exception e) {
                // An exception would be thrown if the dashboard web
                //  server is not accessible.
                onAbout();
            }
        } else {
            onAbout();
        }
    }
    
    private void onAbout()
    {
       MessageBox2 aboutDialog = new MessageBox2( this );
       aboutDialog.setText( RC( "tools.translator.copyright" ) );
       aboutDialog.setTitle( RC( "dialog.title.info" ) );
       aboutDialog.setIcon( imgres.getImage("zaval_ce.gif", aboutDialog));
       String [] OK_BUT = { RC( "dialog.button.ok" ) };
       aboutDialog.setButtons( OK_BUT );
       aboutDialog.show();       
    }

    private void onStatistics()
    {
       MessageBox2 sDialog = new MessageBox2( this );
       String text;
       nullsCount = 0;
       notCompletedCount = 0;
       setIndicators(tree.getRootNode());
       text = RC( "tools.translator.label.statistics.lang" )+bundle.getBundle().getLangCount()+"\n";
       text = text+RC( "tools.translator.label.statistics.record" )+bundle.getBundle().getItemCount()+"\n";
       text = text+RC( "tools.translator.label.statistics.nulls" )+nullsCount+"\n";
       text = text+RC( "tools.translator.label.statistics.notcompleted" )+notCompletedCount;
       sDialog.setText( text );
       sDialog.setTitle( RC( "dialog.title.info" ) );
       String [] OK_BUT = { RC( "dialog.button.ok" ) };
       sDialog.setButtons( OK_BUT );
       sDialog.show();
    }

    private void onGenCode()
    {
       try{
          //
          FileDialog openFileDialog1 = new FileDialog(this, RC("tools.translator.label.saveastitle"), FileDialog.SAVE);
          String mask = "*.java";
          openFileDialog1.setDirectory(".");
          String fn = bundle.getBundle().getLangCount() == 0 ||
             bundle.getBundle().getLanguage(0).getLangFile()==null ? "Sample" :
             bundle.baseName(bundle.getBundle().getLanguage(0).getLangFile());
          fn = fn.substring(0,1).toUpperCase() + fn.substring(1);
          openFileDialog1.setFile(fn + "ResourceMapped.java");
          openFileDialog1.show();

          String filename = openFileDialog1.getFile();
          if( filename != null ){
             SrcGenerator srcgen = new SrcGenerator(
                 bundle.replace(filename, "\\", "/"));
             srcgen.perform(bundle.getBundle());
          }
       }
       catch(Exception e){
          infoException(e);
       }
    }

    private void onParseCode()
    {
       try{
          FileDialog openFileDialog1 = new FileDialog(this, RC("tools.translator.label.opentitle"), FileDialog.LOAD);
          String mask = "*.java";
          openFileDialog1.setDirectory(".");
          String fn = bundle.getBundle().getLangCount() == 0 ||
             bundle.getBundle().getLanguage(0).getLangFile()==null ? "Sample" :
             bundle.baseName(bundle.getBundle().getLanguage(0).getLangFile());
          fn = fn.substring(0,1).toUpperCase() + fn.substring(1);
          openFileDialog1.setFile("*.java");
          openFileDialog1.show();

          String filename = openFileDialog1.getDirectory() + openFileDialog1.getFile();
          if( openFileDialog1.getFile() != null ){
             filename = bundle.replace(filename, "\\", "/");
             JavaParser parser = new JavaParser(new FileInputStream(filename));
             Hashtable ask = parser.parse();
             // TODO: add directory

             clear();
             initControls();
             bundle.getBundle().addLanguage("en");
             String rlng = bundle.getBundle().getLanguage(0).getLangId();

             Enumeration en = ask.keys();
             while(en.hasMoreElements()){
                 String key = (String)en.nextElement();
                 bundle.getBundle().addKey(key);
                 bundle.getBundle().updateValue(key, rlng, (String)ask.get(key));
             }
             initData(false);
             setTitle( filename );
          }
       }
       catch(Exception e){
          infoException(e);
       }
    }

    private void initData(boolean part)
    {
        /* Initialize language set */
        for (int i = 0; i < bundle.getBundle().getLangCount(); ++i ){
           LangItem lang2 = bundle.getBundle().getLanguage(i);
           syncLanguage(lang2.getLangId());
        }

        /* Add all keys in tree view ... */
        tree.setValidating(false);
        for (Iterator iter = bundle.getBundle().iterator(); iter.hasNext();) {
		   BundleItem bi = (BundleItem) iter.next();
           addToTree(bi.getId());
        }
        /* ... and make all keys closed by default */
//        expand(tree.getRootNode());
        collapse(tree.getRootNode());

        /* ... find first key, open it and select */
        if(bundle.getBundle().getItemCount() > 0){
		   BundleItem bi = (BundleItem) bundle.getBundle().iterator().next();
           String id = bi.getId();
           tree.selectNodeAndOpen(id);
           wasSelectedKey = null;
           setTranslations();
        }
        tree.setValidating(true);

        tree.requestFocus();

        closeMenu.enable();
        saveBundleMenu.enable();
        saveAsBundleMenu.enable();
        genMenu.enable();

        setAllIndicators();
        textPanel.invalidate();
        validate();
        repaint();
        isDirty = true;
        syncToolbar();
        if(!part) loadPickList();
    }

    private void expand(TreeNode tn)
    {
       if(tn!=null){
           tree.expandAll(tn.getText());
       }
    }

    private void collapse(TreeNode tn)
    {
       if(tn!=null){
	 	  tree.collapseAll(tn.getText());
       }
    }

    private void syncToolbar()
    {
        for(int j=0;j<tbar2menu.length;++j){
            tool.setEnabled(j, tbar2menu[j].isEnabled());
        }
    }

    private void linkPickList()
    {
        if (advancedMode) {
            for(int j=0;j<pickList.size();++j){
               String patz = stretchPath((String)pickList.elementAt(j));
               MenuItem item = new MenuItem(patz);
               fileMenu.add(item);
            }
            if(pickList.size()>0) fileMenu.addSeparator();
        }
        fileMenu.add(exitMenu);
    }

    private void removePickList()
    {
       if(pickList.size()==0) return;

       int j, q;
       String s1 = stretchPath((String)pickList.elementAt(0));
       for(j=0;j<fileMenu.countItems();++j){
          String patz = fileMenu.getItem(j).getLabel();
          if(patz.equals(s1)) break;
       }
       pickList = new Vector();
       if(j>=fileMenu.countItems()) return;
       for(;j<fileMenu.countItems();) fileMenu.remove(j); //fileMenu.countItems()-1);
    }

    private String stretchPath(String name)
    {
       if(name.length()<MAX_PICK_LENGTH) return name;
       return name.substring(0,4) + "..." +
          name.substring(name.length() - Math.min(name.length() - 7, MAX_PICK_LENGTH - 7));
    }

    private void loadPickList()
    {
       removePickList();
       String path = "./jrc-editor.conf";
       try{
          InputIniFile ini = new InputIniFile(new FileInputStream(path));
          Hashtable tbl = ini.getTable();
          ini.close();

          Enumeration e = tbl.keys();
          while(e.hasMoreElements()){
             String key = (String)e.nextElement();
             String val = (String)tbl.get(key);
             if(!key.startsWith("picklist.")) continue;
             try{
                key = key.substring(key.indexOf('.') + 1);
                int pickLevel = Integer.parseInt(key);
                while(pickList.size()<=pickLevel) pickList.addElement(null);
                pickList.setElementAt(val, pickLevel);
             }
             catch(Exception error){
                error.printStackTrace();
                continue;
             }
          }
       }
       catch(Exception e1){
       }

       for(int j=0;j<pickList.size();++j){
          Object obj = pickList.elementAt(j);
          if(obj==null){
             pickList.removeElementAt(j);
             --j;
          }
       }
       linkPickList();
    }

    private void addToPickList(String name)
    {
        int j=0;
        if(name==null) return;
        for(;j<pickList.size();++j){
            String v1 = (String)pickList.elementAt(j);
            if(v1.equals(name)){
                pickList.removeElementAt(j);
                --j;
            }
        }

        pickList.insertElementAt(name,0);
        while(pickList.size()>=8) pickList.removeElementAt(7);
        saveIni();
    }

    private void saveIni()
    {
        try{
           String path = "./jrc-editor.conf";
           IniFile ini = new IniFile(path);
           for(int j=0;j<pickList.size();++j)
              ini.putString("picklist." + j, (String)pickList.elementAt(j));
           ini.close();
        }
        catch(Exception e){
        }
    }

    private String[] getLangSet()
    {
       LangDialog ed = new LangDialog( this, RC( "tools.translator.label.choosetitle" ), true, this );
       ed.setLabelCaption( RC( "tools.translator.label.chooselabel" ) );
       ed.setButtonsCaption( RC( "dialog.button.ok" ), CLOSE_BUTTONS[2] );

       LangItem[] lset = new LangItem[bundle.getBundle().getLangCount()];
       for (int i = 0; i < lset.length; ++i )
          lset[i] = bundle.getBundle().getLanguage(i);
       ed.setList(lset);

       Dimension d = ed.preferredSize();
       d.width *= 2;
       ed.resize( d );
       ed.toCenter();
       ed.show();
       String[] ask = ed.getList();
       if ( ask==null || ask.length <= 0 || !ed.isApply()) return null;
       for(int i = 0; i < ask.length; ++i)
          if(ask[i].indexOf(':') > 0)
             ask[i] = ask[i].substring(0, ask[i].indexOf(':')).trim();
       return ask;
    }

    public void onOpen(boolean part)
    {
       FileDialog openFileDialog1 = new FileDialog(this, RC( "tools.translator.label.opentitle" ), FileDialog.LOAD);
       openFileDialog1.setDirectory(".");
       String mask = "*" + JAR_EXTENSION + ";" + "*" + ZIP_EXTENSION
           + ";" + "*" + RES_EXTENSION;
       openFileDialog1.setFile( mask );
       openFileDialog1.show();

       String filename = openFileDialog1.getFile();
       if( filename != null ){
          if(!part) clear();
          readResources( openFileDialog1.getDirectory() + filename, part );
       }
    }

    public void onSaveXml(boolean part)
    {
       String[] parts = part ? getLangSet() : null;
       if(part && (parts==null || parts.length < 2)) return;
       FileDialog openFileDialog1 = new FileDialog(this, RC("tools.translator.label.saveastitle"), FileDialog.SAVE);
       String mask = "*.xml";
       openFileDialog1.setDirectory(".");
       String fn = bundle.getBundle().getLanguage(0).getLangFile();
       if(fn==null) fn = "autosaved";
       openFileDialog1.setFile( bundle.baseName(fn) + ".xml" );
       openFileDialog1.show();

       String filename = openFileDialog1.getFile();
       if( filename != null )
          try{
             filename = openFileDialog1.getDirectory() + filename;
             DataOutputStream out = new DataOutputStream(new FileOutputStream(filename));
             BundleSet set = bundle.getBundle();
             out.writeChar((char)0xFEFF);
             out.writeChars("<xml>\n");
             for (Iterator iter = set.iterator(); iter.hasNext();) {
				BundleItem bi = (BundleItem) iter.next();
                Enumeration en= bi.getLanguages();
                out.writeChars("\t<key name=\"" + bi.getId() + "\">\n");
                while(en.hasMoreElements()){
                    String lang = (String)en.nextElement();
                    if(part && !inArray(parts, lang)) continue;
                    out.writeChars("\t\t<value lang=\""+lang+"\">"+bi.getTranslation(lang)+"</value>\n");
                }
                out.writeChars("\t</key>\n");
             }
             out.writeChars("</xml>\n");
             out.close();
          }
          catch(Exception e){
             infoException(e);
          }
    }

    public void onSaveUtf(boolean part)
    {
       String[] parts = part ? getLangSet() : null;
       if(part && (parts==null || parts.length < 2)) return;

       for(int k=0;parts!=null && k<parts.length;++k)
          System.err.println("+++ [" + parts[k] + "]");

       FileDialog openFileDialog1 = new FileDialog(this, RC("tools.translator.label.saveastitle"), FileDialog.SAVE);
       String mask = "*.txt";
       openFileDialog1.setDirectory(".");
       String fn = bundle.getBundle().getLanguage(0).getLangFile();
       if(fn==null) fn = "autosaved";
       openFileDialog1.setFile( bundle.baseName(fn) + ".txt" );
       openFileDialog1.show();

       String filename = openFileDialog1.getFile();
       if( filename != null )
          try{
             filename = openFileDialog1.getDirectory() + filename;
             DataOutputStream out = new DataOutputStream(new FileOutputStream(filename));
             BundleSet set = bundle.getBundle();
             out.writeChar((char)0xFEFF);
             out.writeChars("#JRCE 1.3: do not modify this line\r\n\r\n");
			 for (Iterator iter = set.iterator(); iter.hasNext();) {
			    BundleItem bi = (BundleItem) iter.next();
                Enumeration en= bi.getLanguages();
                out.writeChars("KEY=\"" + bi.getId() + "\":\r\n");
                while(en.hasMoreElements()){
                    String lang = (String)en.nextElement();
                    if(part && !inArray(parts, lang)) continue;
                    out.writeChars("\t\""+lang+"\"=\""+bi.getTranslation(lang)+"\"\r\n");
                }
                out.writeChars("\r\n");
             }
             out.close();
          }
          catch(Exception e){
             infoException(e);
          }
    }

    private boolean inArray(String[] array, String lang)
    {
        for(int j=0;j<array.length;++j)
            if(array[j].equalsIgnoreCase(lang)) return true;
        return false;
    }

    /*
        Reading unicode (UCS16) file stream into memory
    */
    private String getBody(String file)
    throws IOException
    {
        char ch;
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        StringBuffer buf = new StringBuffer(in.available());

        try{
            in.readChar(); // skip UCS16 marker FEFF
            for(;;){
                ch=in.readChar();
                buf.append(ch);
            }
        }
        catch(EOFException eof){
        }
        return buf.toString();
    }

    private void fillTable(Hashtable tbl)
    {
        Enumeration en = tbl.keys();
        while(en.hasMoreElements()){
           String k = (String)en.nextElement();
           StringTokenizer st = new StringTokenizer(k, "!");
           String key = st.nextToken();
           if(!st.hasMoreTokens()) continue;
           String lang = st.nextToken();

           if(bundle.getBundle().getLanguage(lang)==null)
              bundle.getBundle().addLanguage(lang);

           bundle.getBundle().addKey(key);
           bundle.getBundle().updateValue(key, lang, (String)tbl.get(k));
        }
    }

    public void onLoadXml(boolean part)
    {
       FileDialog openFileDialog1 = new FileDialog(this, RC( "tools.translator.label.opentitle" ), FileDialog.LOAD);
       openFileDialog1.setDirectory(".");
       String mask = "*.xml";
       openFileDialog1.setFile( mask );
       openFileDialog1.show();

       String filename = openFileDialog1.getFile();
       if( filename != null ){
          if(!part) clear();
          if(!part) initControls();
          bundle.getBundle().addLanguage("en");

          try{
             filename = openFileDialog1.getDirectory() + filename;
             XmlReader xml = new XmlReader(getBody(filename));
             Hashtable tbl = xml.getTable();
             fillTable(tbl);
          }
          catch(Exception e){
             infoException(e);
          }
          initData(part);
          setTitle( filename );
       }
    }

    public void onLoadUtf(boolean part)
    {
       FileDialog openFileDialog1 = new FileDialog(this, RC( "tools.translator.label.opentitle" ), FileDialog.LOAD);
       openFileDialog1.setDirectory(".");
       String mask = "*.txt";
       openFileDialog1.setFile( mask );
       openFileDialog1.show();

       String filename = openFileDialog1.getFile();
       if( filename != null ){
          if(!part) clear();
          if(!part) initControls();
          bundle.getBundle().addLanguage("en");
          try{
             filename = openFileDialog1.getDirectory() + filename;
             UtfParser parser = new UtfParser(new StringReader(getBody(filename)));
             Hashtable tbl = parser.parse();
             fillTable(tbl);
          }
          catch(Exception e){
             infoException(e);
          }
          initData(part);
          setTitle( filename );
       }
    }

    private void onNewKey()
    {
       EditDialog ed = new EditDialog( this, RC( "tools.translator.label.newkeytitle" ), true, this );
       ed.setLabelCaption( RC( "tools.translator.label.insert" ) );
       ed.setButtonsCaption( RC( "dialog.button.ok" ), CLOSE_BUTTONS[2] );
       Dimension d = ed.preferredSize();
       ed.resize( d );
       ed.toCenter();
       ed.show();
       String text = ed.getText();
       if ( text.length() <= 0 || !ed.isApply() ) return;
       keyName.setText(text);
       onInsertKey();
    }

    private void infoError(String error)
    {
       errDialog.setText( error );
       errDialog.show();
    }

    private void moveFocus()
    {
        Window wnd = null;
        Component p = this;
        while(p!=null && !(p instanceof Window)) p = p.getParent();
        if(p==null) return;
        wnd = (Window)p;

        Component focused = wnd.getFocusOwner();
        int idx = tabOrder.indexOf(focused);
        if(idx >= 0){
            if(idx + 1 < tabOrder.size() ){
                Component c = (Component)tabOrder.elementAt(idx+1);
                c.requestFocus();
                return;
            }
        }
        int i;
        for ( i = 0; i < langStates.size(); i++ ) {
            LangState ls = getLangState(i);
            if(ls.hidden) continue;
            if(ls.tf.getControl()==focused) break;
        }

        if(i < langStates.size()){
            for ( ++i; i < langStates.size(); ++i ) {
                LangState ls = getLangState(i);
                if(ls.hidden) continue;
                ls.tf.requestFocus();
                return;
            }
            tree.requestFocus();
            return;
        }
        for ( i=0; i < langStates.size(); ++i ) {
            LangState ls = getLangState(i);
            if(ls.hidden) continue;
            ls.tf.requestFocus();
            return;
        }
        tree.requestFocus();
    }

    public void setDestDir(String destDir) {
        this.destDir = destDir;
    }

    public void setTranslationNeededTester(Comparator translationNeededTester) {
        this.translationNeededTester = translationNeededTester;
        setAllIndicators();
    }

    public void setSaveListener(ActionListener saveListener) {
        this.saveListener = saveListener;
        if (bundle != null)
            bundle.setSaveListener(saveListener);
    }

    public void setHelpListener(ActionListener helpListener) {
        this.helpListener = helpListener;
        if (helpListener != null) {
            helpMenu.insert(getHelpMenu, 0);
        }
    }

}

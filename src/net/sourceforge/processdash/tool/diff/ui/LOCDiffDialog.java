// Copyright (C) 2011-2018 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.diff.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.EventHandler;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.diff.engine.DiffEngine;
import net.sourceforge.processdash.tool.diff.engine.DiffEvent;
import net.sourceforge.processdash.tool.diff.engine.DiffListener;
import net.sourceforge.processdash.tool.diff.engine.FileAnalysisSet;
import net.sourceforge.processdash.tool.diff.engine.LanguageFilterSelector;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.ui.lib.SwingEventHandler;
import net.sourceforge.processdash.util.FileUtils;

public class LOCDiffDialog {


    public interface Panel {

        public String getId();

        public String getShortName();

        public Component getConfigPanel();

        public FileAnalysisSet getFileAnalysisSet(DiffEngine engine)
                throws PanelInvalidException;

    }

    public static class PanelInvalidException extends RuntimeException {
        private Object dialogMessage;
        public PanelInvalidException(Object dialogMessage) {
            this.dialogMessage = dialogMessage;
        }
        public void show(Component parent) {
            if (dialogMessage != null) {
                java.awt.Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(parent, dialogMessage,
                    resources.getString("Dialog.Error"),
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private List<Panel> panels;

    private LanguageFilterSelector languageFilterSelector;

    private ActionListener reportFileListener;

    private JFrame frame;

    private JButton compareButton, closeButton;

    private JTabbedPane tabPane;


    private static final String SELECTED_TAB_PREF = "selectedTabId";

    public static final Preferences PREFS = Preferences
            .userNodeForPackage(LOCDiffDialog.class);

    protected static Resources resources = Resources.getDashBundle("LOCDiff");

    public LOCDiffDialog(List<Panel> panels, LanguageFilterSelector lfs) {
        this(panels, lfs, null);
    }

    public LOCDiffDialog(List<Panel> panels, LanguageFilterSelector lfs,
            ActionListener reportFileListener) {
        this.panels = panels;
        this.languageFilterSelector = lfs;
        this.reportFileListener = reportFileListener;

        frame = new JFrame(resources.getString("Dialog.Generic_Window_Title"));
        DashboardIconFactory.setWindowIcon(frame);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                disposePanels();
            }
        });

        JPanel content = new JPanel(new BorderLayout(5, 10));
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        content.add(createTabPanel(panels), BorderLayout.CENTER);
        content.add(createButtonPanel(), BorderLayout.SOUTH);
        setupWindowKeyBindings(content);

        frame.getContentPane().add(content);
        frame.pack();
        frame.setVisible(true);
    }

    private Component createTabPanel(List<Panel> panels) {
        tabPane = new JTabbedPane();

        String prefTab = PREFS.get(SELECTED_TAB_PREF, "");

        for (Panel p : panels) {
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.add(p.getConfigPanel());
            wrap.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));

            tabPane.addTab(p.getShortName(), wrap);
            if (p.getId().equals(prefTab))
                tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
        }

        return tabPane;
    }

    private Component createButtonPanel() {
        compareButton = new JButton(resources.getString("Dialog.Count"));
        compareButton.addActionListener(EventHandler.create(
            ActionListener.class, this, "doCount"));

        closeButton = new JButton(resources.getString("Close"));
        closeButton.addActionListener(EventHandler.create(ActionListener.class,
            this, "closeDialog"));

        return BoxUtils.hbox(BoxUtils.GLUE, compareButton, BoxUtils.GLUE,
            closeButton, BoxUtils.GLUE);
    }

    private void setupWindowKeyBindings(JComponent c) {
        InputMap inputMap = c
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            "closeDialog");
        c.getActionMap().put("closeDialog", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }});

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            "doCount");
        c.getActionMap().put("doCount", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doCount();
            }});
    }

    private void disposePanels() {
        for (Panel p : panels) {
            if (p instanceof Closeable) {
                FileUtils.safelyClose((Closeable) p);
            }
        }
    }

    public void closeDialog() {
        frame.setVisible(false);
        frame.dispose();
    }

    public void doCount() {
        Panel panel = panels.get(tabPane.getSelectedIndex());
        PREFS.put(SELECTED_TAB_PREF, panel.getId());

        DiffEngine engine = new DiffEngine();
        engine.setLanguageFilterSelector(languageFilterSelector);

        FileAnalysisSet fileSet;
        try {
            fileSet = panel.getFileAnalysisSet(engine);
        } catch (PanelInvalidException pie) {
            pie.show(tabPane);
            return;
        }

        if (fileSet == null || engine.isAborted())
            return;

        ProgressDialog progressDialog = new ProgressDialog(engine);
        WorkerThread worker = new WorkerThread(progressDialog, engine, fileSet);
        worker.start();
        progressDialog.setVisible(true);
    }

    private class ProgressDialog extends JDialog implements DiffListener {
        private DiffEngine engine;
        private JLabel message;
        private JProgressBar progressBar;
        private int numFilesSoFar;
        public ProgressDialog(DiffEngine engine) {
            super(frame, resources.getString("Dialog.Counting"), true);

            this.engine = engine;
            engine.addDiffListener(SwingEventHandler.create(DiffListener.class,
                this));

            message = new JLabel(resources.getString("Dialog.Starting"));

            progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);

            JButton cancelButton = new JButton(resources.getString("Cancel"));
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ProgressDialog.this.engine.abort();
                    dispose();
                }});

            JPanel content = new JPanel(new BorderLayout(0, 5));
            content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            content.add(message, BorderLayout.NORTH);
            content.add(progressBar, BorderLayout.CENTER);
            content.add(BoxUtils.hbox(BoxUtils.GLUE, cancelButton,
                BoxUtils.GLUE), BorderLayout.SOUTH);
            getContentPane().add(content);

            pack();
            Dimension d = getSize();
            d.width = 400;
            setSize(d);
            setLocationRelativeTo(frame);
        }

        public void analysisStarting(DiffEvent e) throws IOException {
            int totalNumFiles = engine.getFilesToAnalyze().size();
            progressBar.setMaximum(totalNumFiles);
            progressBar.setIndeterminate(false);

            numFilesSoFar = 0;
        }
        public void fileAnalysisStarting(DiffEvent e) throws IOException {
            if (e.getFile() != null)
                message.setText(e.getFile().getFilename());
            progressBar.setValue(numFilesSoFar);
            numFilesSoFar++;
        }
        public void fileAnalysisFinished(DiffEvent e) throws IOException {}
        public void analysisFinished(DiffEvent e) throws IOException {}
    }


    private class WorkerThread extends Thread {

        private ProgressDialog progressDialog;
        private DiffEngine engine;
        private FileAnalysisSet fileSet;

        public WorkerThread(ProgressDialog progressDialog, DiffEngine engine,
                FileAnalysisSet fileSet) {
            this.progressDialog = progressDialog;
            this.engine = engine;
            this.fileSet = fileSet;
        }

        @Override
        public void run() {
            try {
                HtmlDiffReportWriter html = new HtmlDiffReportWriter();
                if (reportFileListener != null)
                    html.setLaunchBrowser(false);

                engine.addFilesToAnalyze(fileSet);
                engine.addDiffListener(html);
                engine.run();

                if (reportFileListener != null)
                    reportFileListener.actionPerformed(new ActionEvent(
                            LOCDiffDialog.this, 0, html.getReportFile()
                                    .getPath()));

            } catch (Exception e) {
                ExceptionDialog.show(progressDialog,
                    resources.getString("Dialog.Error"), //
                    resources.getString("Dialog.Unexpected_Error"), e);
            } finally {
                if (fileSet instanceof Closeable)
                    FileUtils.safelyClose((Closeable) fileSet);               
            }
            progressDialog.dispose();
        }
    }

}

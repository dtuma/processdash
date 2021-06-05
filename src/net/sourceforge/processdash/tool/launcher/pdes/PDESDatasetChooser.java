// Copyright (C) 2018-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.pdes;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.launcher.LaunchableDataset;
import net.sourceforge.processdash.tool.launcher.ui.LaunchableDatasetList;
import net.sourceforge.processdash.tool.perm.UserAccountFlag;
import net.sourceforge.processdash.tool.perm.WhoAmI;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.WrappingHtmlLabel;
import net.sourceforge.processdash.util.HttpException;
import net.sourceforge.processdash.util.StringUtils;

public class PDESDatasetChooser extends JPanel {

    private DefaultComboBoxModel serverUrls;

    private JComboBox serverSelector;

    private JScrollPane scrollPane;

    private LaunchableDatasetList list;

    private JComponent loadingLabel;

    private JComponent errorLabel;

    private Exception error;

    private static final Resources res = Resources
            .getDashBundle("Launcher.PDES");


    public PDESDatasetChooser(List<String> knownServers) {
        super(new BorderLayout(0, 3));
        add(new JOptionPaneTweaker.MakeResizable(), BorderLayout.WEST);

        // build a list of objects to represent our servers
        serverUrls = new DefaultComboBoxModel();
        for (String url : knownServers)
            serverUrls.addElement(new ServerUrl(url));
        serverUrls.addElement(new ServerUrl()); // "Other server..." item

        // create a combo box for selecting a server
        serverSelector = new JComboBox(serverUrls);
        serverSelector.setSelectedIndex(0);
        add(serverSelector, BorderLayout.NORTH);

        // create a component to display the list of datasets
        list = new LaunchableDatasetList();
        list.addMouseListener(new JOptionPaneClickHandler());

        // create a label to advise the user data is loading
        loadingLabel = new JLabel(res.getString("Loading"),
                SwingConstants.CENTER);
        Font f = loadingLabel.getFont();
        loadingLabel.setFont(f.deriveFont(Font.ITALIC, f.getSize2D() * 1.2f));

        // create a scroll pane to display data/messages
        scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(300, 240));
        add(scrollPane, BorderLayout.CENTER);

        // if we weren't given any known servers, prompt for one immediately
        maybePromptForNewServerUrl(null);

        if (getNumServers() == 1) {
            // if we only have one server in the list, load its datasets
            // synchronously. We do this so our caller can know if the chooser
            // should be displayed at all, or if they can jump directly to
            // launching a single dataset.
            new DatasetListLoader().run();

        } else if (getNumServers() > 1) {
            // if there is more than one server, start the load asynchronously
            refreshDatasetList();
        }

        // add a listener that will refresh the list when a new server is chosen
        serverSelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshDatasetList();
            }
        });
    }


    /**
     * @return the number of servers in this chooser's selection list
     */
    public int getNumServers() {
        return serverUrls.getSize() - 1;
    }


    /**
     * @return true if the user has clicked on items during startup that should
     *         be interpreted as canceling the choosing operation
     */
    public boolean userCancelledStartup() {
        // calculate the number of servers in our list
        int numServers = getNumServers();

        // if we don't know about any servers, and the user aborted the prompt
        // to enter one, consider the operation cancelled
        if (numServers == 0)
            return true;

        // abort if there is only one server, and the user cancelled the login
        if (numServers == 1 && error instanceof HttpException.Unauthorized)
            return true;

        return false;
    }


    /**
     * If we only found a single dataset for a single known server, return it.
     */
    public LaunchableDataset getSingleResult() {
        if (getNumServers() == 1 && list.getModel().getSize() == 1)
            return (LaunchableDataset) list.getModel().getElementAt(0);
        else
            return null;
    }


    /**
     * @return the dataset that has been selected by the user, or null if no
     *         dataset has been selected
     */
    public LaunchableDataset getSelectedDataset() {
        if (error != null)
            return null;
        else
            return (LaunchableDataset) list.getSelectedValue();
    }


    /**
     * Reload the list of datasets based on the current server selection
     */
    private void refreshDatasetList() {
        maybePromptForNewServerUrl(scrollPane);
        scrollPane.setViewportView(loadingLabel);
        list.setData(Collections.EMPTY_LIST);
        new DatasetListLoader().start();
    }


    /**
     * If the user has chosen the "Other Datasets..." item, prompt them to enter
     * a new server URL. If they do, add that URL to the list and select it.
     */
    private void maybePromptForNewServerUrl(Component parent) {
        // if the user has selected a real server, do nothing
        ServerUrl s = (ServerUrl) serverSelector.getSelectedItem();
        if (s != null && s.baseUrl != null)
            return;

        // ask the user to enter the URL of the new server.
        String newUrl = promptForServerUrl(parent);
        if (newUrl != null) {
            // create a new server, add it to the list, and select it
            serverUrls.insertElementAt(new ServerUrl(newUrl), 0);
            serverSelector.setSelectedIndex(0);

        } else if (getNumServers() > 0) {
            // if the user pressed cancel on the URL prompt, revert back to the
            // first server in the list (if one exists)
            serverSelector.setSelectedIndex(0);
        }
    }

    static String promptForServerUrl(Component parent) {
        // create user interface components
        String title = res.getString("Enter_Url.Title");
        String prompt = res.getString("Enter_Url.Message");
        JTextField url = new JTextField();
        Object[] message = new Object[] { prompt, url,
                new JOptionPaneTweaker.GrabFocus(url),
                new JOptionPaneTweaker.ToFront() };

        while (true) {
            // prompt the user to enter a URL
            url.selectAll();
            int userChoice = JOptionPane.showConfirmDialog(parent, message,
                title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (userChoice != JOptionPane.OK_OPTION)
                return null;

            // if the user did not enter a value, admonish them
            String urlText = url.getText().trim();
            if (urlText.length() == 0) {
                showErrorMessage(parent, "Error.Url_Missing");
                continue;
            }

            // check to see if they entered a valid URL
            String baseUrl = PDESUtil.getBaseUrl(urlText);
            try {
                if (baseUrl.startsWith("http")) {
                    // if they entered a full URL (including the protocol),
                    // try connecting to that URL
                    if (isPdesUrl(baseUrl, false))
                        return baseUrl;

                } else {
                    // if they only entered a hostname, try connecting via
                    // HTTPS first, then HTTP
                    String https = "https://" + baseUrl;
                    if (isPdesUrl(https, true))
                        return https;
                    String http = "http://" + baseUrl;
                    if (isPdesUrl(http, false))
                        return http;
                }

                // if we were able to connect without error, but the given
                // server isn't a PDES, display an appropriate message
                showErrorMessage(parent, "Error.Not_PDES");

            } catch (Exception e) {
                // if we encountered an error while attempting to open the URL,
                // display a message that the given URL could not be reached
                showErrorMessage(parent, "Error.Server_Unreachable");
            }
        }
    }

    private static boolean isPdesUrl(String baseUrl, boolean swallowException)
            throws Exception {
        try {
            // try connecting to a URL we expect to find on the PDES server
            String testUrl = baseUrl + "pub/lib/jacsblank.html";
            HttpURLConnection conn = (HttpURLConnection) new URL(testUrl)
                    .openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);

            // return true if the connection is successful
            return (conn.getResponseCode() == 200 //
                    && conn.getContentLength() > 0 //
                    && conn.getContentLength() < 256);

        } catch (Exception e) {
            if (swallowException)
                return false;
            else
                throw e;
        }
    }

    static void showErrorMessage(Component parent, String key) {
        Object message = new Object[] { res.getStrings(key),
                new JOptionPaneTweaker.ToFront() };
        JOptionPane.showMessageDialog(parent, message,
            res.getString("Error.Title"), JOptionPane.ERROR_MESSAGE);
    }



    /**
     * Item to display in the combo box. Holds a full URL, with an abbreviated
     * version to display to the user
     */
    private class ServerUrl {

        String baseUrl;

        String display;

        private ServerUrl() {
            this.baseUrl = null;
            this.display = res.getString("Other_Server");
        }

        private ServerUrl(String url) {
            this.baseUrl = url;

            // for the displayable text, strip the protocol from the front
            // and the final slash from the end
            int pos = url.indexOf("://");
            this.display = url.substring(pos + 3, url.length() - 1);
        }

        @Override
        public String toString() {
            return display;
        }
    }


    /**
     * Class to asynchronously load datasets from a server
     */
    private class DatasetListLoader extends Thread {

        public DatasetListLoader() {
            currentLoader = this;
            setDaemon(true);
        }

        @Override
        public void run() {
            Runnable result;
            try {
                ServerUrl server = (ServerUrl) serverSelector.getSelectedItem();
                String url = server.baseUrl;

                WhoAmI who = new WhoAmI(url + "DataBridge/INST-000");
                List<UserAccountFlag> userFlags = who.getUserAccountFlags();
                if (!userFlags.isEmpty()) {
                    result = new UserFlagErrorMessage(userFlags.get(0));

                } else {
                    List<LaunchableDataset> datasets = DatasetsApi
                            .myDatasets(url);
                    result = new DatasetListReady(url, datasets);
                }

            } catch (Exception e) {
                result = new DatasetLoadFailed(e);
            }

            if (this != currentLoader) {
                // if another loader has replaced us, abort
            } else if (Thread.currentThread() != this) {
                // if we are running synchronously, publish data immediately
                result.run();
            } else {
                // publish results on the event dispatch thread
                SwingUtilities.invokeLater(result);
            }
        }
    }

    private volatile DatasetListLoader currentLoader;

    /**
     * Publish a list of datasets that have been fetched for the current server
     */
    private class DatasetListReady implements Runnable {

        private String baseUrl;

        private List<LaunchableDataset> datasets;

        public DatasetListReady(String baseUrl,
                List<LaunchableDataset> datasets) {
            this.baseUrl = baseUrl;
            this.datasets = datasets;
        }


        @Override
        public void run() {
            PDESUtil.getPdesPrefs().put(PDESUtil.DEFAULT_SERVER_PREF, baseUrl);
            list.setData(datasets);
            scrollPane.setViewportView(list);
            error = null;
        }
    }

    /**
     * Display a message that a problem has been encountered
     */
    private class DatasetLoadFailed implements Runnable {

        private Exception error;

        public DatasetLoadFailed(Exception error) {
            this.error = error;
        }

        @Override
        public void run() {
            PDESDatasetChooser.this.error = this.error;
            if (error instanceof HttpException.Unauthorized) {
                scrollPane.setViewportView(new JLabel(" "));
                showErrorMessage(scrollPane, "Error.Unauthorized");
            } else {
                errorLabel = buildErrorLabel();
                scrollPane.setViewportView(errorLabel);
            }
        }

        private JComponent buildErrorLabel() {
            // retrieve localized text and build an HTML document
            String html = "<html>" + getMessageHtml() + "</html>";
            html = StringUtils.findAndReplace(html, "[[", "<a href='try'>");
            html = StringUtils.findAndReplace(html, "]]", "</a>");
            html = StringUtils.findAndReplace(html, "((", "<a href='info'>");
            html = StringUtils.findAndReplace(html, "))", "</a>");
            html = StringUtils.findAndReplace(html, "\n", "<br/>");

            // create a wrapping label to display the error message
            WrappingHtmlLabel label = new WrappingHtmlLabel(html);
            label.setFont(loadingLabel.getFont());
            label.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,
                Boolean.TRUE);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // when the user clicks on hyperlinks, refresh or display details
            label.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (EventType.ACTIVATED.equals(e.getEventType())) {
                        String href = e.getDescription();
                        if ("try".equals(href)) {
                            refreshDatasetList();
                        } else if ("info".equals(href)) {
                            ExceptionDialog.show(errorLabel,
                                res.getString("Error.Title"), error);
                        } else if (e.getURL() != null && href.startsWith("http")) {
                            try {
                                Desktop.getDesktop().browse(e.getURL().toURI());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Toolkit.getDefaultToolkit().beep();
                            }
                        }
                    }
                }
            });

            return label;
        }

        protected String getMessageHtml() {
            return res.getHTML("Error.Cannot_Connect");
        }

    }

    /**
     * Display a server-supplied error message about a user account flag
     */
    private class UserFlagErrorMessage extends DatasetLoadFailed {

        private UserAccountFlag flag;

        public UserFlagErrorMessage(UserAccountFlag flag) {
            super(new Exception(flag.getMsg()));
            this.flag = flag;
        }

        @Override
        protected String getMessageHtml() {
            return flag.getHtml() + "\n \n" + res.getHTML("Error.Retry");
        }

    }

}

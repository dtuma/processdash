// Copyright (C) 2014-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.impl;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.PasswordAuthentication;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.netbeans.api.keyring.Keyring;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.StringUtils;

public class HttpAuthenticator extends Authenticator {

    private enum State {
        Initial, Keyring, //
        UserEntry1, UserEntry2, UserEntry3, //
        Failed, Cancelled
    };


    private Preferences prefs;

    private Resources resources;

    private String title;

    private Component parentComponent;

    private State state;

    private float rememberMeDays;

    private String lastUrl;

    private long lastTimestamp;

    private String lastUsername;

    private char[] lastPassword;

    private boolean passwordDialogVisible;


    private static final String SETTING_PREFIX = //
            "net.sourceforge.processdash.HttpAuthenticator.";

    private static final String ENABLED_SETTING_NAME = SETTING_PREFIX
            + "enabled";

    private static final String REMEMBER_ME_SETTING_NAME = SETTING_PREFIX
            + "rememberMeDays";

    private static final String LAST_URL_SETTING_NAME = SETTING_PREFIX
            + "lastUrl";

    private static final String LAST_USERNAME_SETTING_NAME = SETTING_PREFIX
            + "lastUsername";

    private static final String LAST_USERNAME = "lastUsername";

    private static final String REMEMBER_ME_UNTIL = "rememberUntil";

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;


    private HttpAuthenticator(String title) {
        this.prefs = Preferences.userRoot().node(
                "net/sourceforge/processdash/userPrefs/authenticator");
        this.resources = Resources.getDashBundle("Authentication.Password");
        this.title = title;
        this.state = State.Initial;
        this.lastUrl = System.getProperty(LAST_URL_SETTING_NAME);
        this.lastUsername = System.getProperty(LAST_USERNAME_SETTING_NAME);

        if (CookieHandler.getDefault() == null)
            CookieHandler.setDefault(
                new CookieManager(null, CookiePolicy.ACCEPT_ALL));

        if (Keyring.isPersistent()) {
            try {
                this.rememberMeDays = Float.parseFloat(System
                        .getProperty(REMEMBER_ME_SETTING_NAME));
            } catch (Exception e) {
                this.rememberMeDays = 14;
            }
        }
    }


    @Override
    protected PasswordAuthentication getPasswordAuthentication() {

        // only handle "server" auth requests (no proxy support for now)
        if (getRequestorType() != RequestorType.SERVER)
            return null;

        // only handle Data Bridge requests (no support for non-PDES servers)
        if (getRequestingURL().toString().indexOf("/DataBridge/") == -1
                && getRequestingURL().toString().indexOf("/api/datasets") == -1
                && getRequestingURL().toString().indexOf("/api/v1/") == -1)
            return null;

        // find out what state we are presently in.
        determineCurrentState();

        // if we're in a failure state, return no auth data.
        if (state == State.Failed || state == State.Cancelled)
            return null;

        // possibly supply credentials that were stored in the keyring
        if (state == State.Keyring) {
            char[] password = getPasswordFromKeyring(lastUsername);
            if (password != null)
                return new PasswordAuthentication(lastUsername, password);
            else if (lastUsername != null && lastPassword != null)
                return new PasswordAuthentication(lastUsername, mask(lastPassword));
            else
                state = State.UserEntry1;
        }

        // create user interface components to prompt for username and password
        JTextField username = new JTextField(2);
        JPasswordField password = new JPasswordField(2);
        FocusAdapter selectAll = new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (e.getComponent() instanceof JTextField) {
                    ((JTextField) e.getComponent()).selectAll();
                }}};
        username.addFocusListener(selectAll);
        password.addFocusListener(selectAll);
        JComponent focus = username;
        if (StringUtils.hasValue(lastUsername)) {
            username.setText(lastUsername);
            focus = password;
        }
        JLabel usernameLabel = new JLabel(resources.getString("Username"),
                SwingConstants.RIGHT);
        JLabel passwordLabel = new JLabel(resources.getString("Password"),
                SwingConstants.RIGHT);
        Dimension d = usernameLabel.getPreferredSize();
        d.width = Math.max(d.width, passwordLabel.getPreferredSize().width);
        usernameLabel.setPreferredSize(d);
        passwordLabel.setPreferredSize(d);

        // if "remember me" support is enabled, create a checkbox
        JCheckBox rememberMe = null;
        if (rememberMeDays > 0) {
            rememberMe = new JCheckBox(
                    resources.getString("Remember_Me.Prompt"));
            rememberMe.setToolTipText(resources.format(
                "Remember_Me.Tooltip_FMT", rememberMeDays));
            Font f = rememberMe.getFont();
            f = f.deriveFont(f.getSize2D() * 0.8f);
            rememberMe.setFont(f);
        }

        // prompt the user for credentials
        final String title = (StringUtils.hasValue(this.title) ? this.title
                : resources.getString("Title"));
        String[] promptLines = resources.formatStrings("Prompt_FMT",
            getRequestingPrompt());
        Object[] prompt = new Object[promptLines.length];
        System.arraycopy(promptLines, 0, prompt, 0, prompt.length);
        // add a tooltip to the last line of the prompt, indicating the URL
        JLabel promptLabel = new JLabel(promptLines[prompt.length - 1]);
        promptLabel.setToolTipText(getEffectiveURL());
        prompt[prompt.length - 1] = promptLabel;
        final Object[] message = new Object[] {
                prompt,
                BoxUtils.vbox(5),
                BoxUtils.hbox(15, usernameLabel, 5, username),
                BoxUtils.hbox(15, passwordLabel, 5, password),
                BoxUtils.hbox(BoxUtils.GLUE, rememberMe),
                new JOptionPaneTweaker.GrabFocus(focus),
                new JOptionPaneTweaker.ToFront() };
        final int[] userChoice = new int[1];

        try {
            Runnable r = new Runnable() {
                public void run() {
                    passwordDialogVisible = true;
                    userChoice[0] = JOptionPane.showConfirmDialog(
                        parentComponent, message, title,
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                    passwordDialogVisible = false;
                }
            };
            if (SwingUtilities.isEventDispatchThread())
                r.run();
            else
                SwingUtilities.invokeAndWait(r);
        } catch (Exception e) {
        }

        // record metadata about this password request
        setLastValues(getEffectiveURL(), username.getText().trim());
        prefs.put(prefsKey(LAST_USERNAME), lastUsername);

        if (userChoice[0] == JOptionPane.OK_OPTION) {
            // if the user entered credentials, return them.
            if (rememberMe != null && rememberMe.isSelected())
                savePasswordToKeyring(lastUsername, password.getPassword());
            lastPassword = mask(password.getPassword());
            return new PasswordAuthentication(lastUsername,
                    password.getPassword());
        } else {
            // if the user cancelled the operation, abort.
            state = State.Cancelled;
            return null;
        }
    }

    private void determineCurrentState() {
        if (isRetry() == false) {
            // if this operation is not a retry, reset to initial state.
            if (lastUrl == null || !getBaseURL().equals(getBaseURL(lastUrl)))
                lastPassword = null;
            setLastValues(getEffectiveURL(),
                prefs.get(prefsKey(LAST_USERNAME), null));
            state = State.Initial;
        }

        // if we are not in a failure mode, increment the state by one position
        if (state.compareTo(State.Failed) < 0)
            state = State.values()[state.ordinal() + 1];
        else
            lastPassword = null;
    }

    private boolean isRetry() {
        if (lastUrl == null || !lastUrl.equals(getEffectiveURL()))
            return false;

        long retryDelay = System.currentTimeMillis() - lastTimestamp;
        return retryDelay < 15 * DateUtils.SECONDS;
    }

    private void setLastValues(String url, String username) {
        lastUrl = url;
        lastTimestamp = System.currentTimeMillis();
        lastUsername = username;
        if (lastUrl != null)
            System.setProperty(LAST_URL_SETTING_NAME, lastUrl);
        if (lastUsername != null)
            System.setProperty(LAST_USERNAME_SETTING_NAME, lastUsername);
    }

    private char[] getPasswordFromKeyring(String username) {
        // no username? abort
        if (!StringUtils.hasValue(username))
            return null;

        // get the timestamp when we should forget credentials
        String expirationTimeKey = prefsKey(REMEMBER_ME_UNTIL);
        long expirationTime = prefs.getLong(expirationTimeKey, -1);
        if (expirationTime == -1) {
            // if the timestamp is not present, no credentials are available
            return null;

        } else if (expirationTime < System.currentTimeMillis()) {
            // if the timestamp has expired, delete the stored credentials
            prefs.remove(expirationTimeKey);
            Keyring.delete(ringKey(username));
            return null;

        } else {
            // if the timestamp is still valid, retrieve the stored password
            return Keyring.read(ringKey(username));
        }
    }

    private void savePasswordToKeyring(String username, char[] password) {
        // record an expiration time when we should forget these credentials
        long expirationTime = System.currentTimeMillis()
                + (long) (rememberMeDays * DAY_MILLIS);
        prefs.putLong(prefsKey(REMEMBER_ME_UNTIL), expirationTime);

        // store the password into the keyring.
        Keyring.save(ringKey(username), password, null);
    }

    private String prefsKey(String suffix) {
        String result = getBaseURL();
        result = result.replace(':', ';');
        result = result.replace('/', ',');
        if (suffix != null)
            result = result + suffix;
        return result;
    }

    private String ringKey(String username) {
        return username + "@" + getBaseURL();
    }

    private char[] mask(char[] password) {
        // tweak the chars to make passwords less readable in heap dumps
        char[] result = new char[password.length];
        for (int i = result.length; i-- > 0;)
            result[i] = (char) (password[i] ^ 0x5555);
        return result;
    }

    private String getBaseURL() {
        return getBaseURL(getEffectiveURL());
    }

    private String getBaseURL(String result) {
        int baseEndPos = result.indexOf("/DataBridge");
        if (baseEndPos == -1)
            baseEndPos = result.indexOf("/api/");
        if (baseEndPos != -1)
            result = result.substring(0, baseEndPos + 1);
        return result;
    }

    private String getEffectiveURL() {
        // client code will make various requests to the data bridge using
        // different query parameters. For our purposes, these are all related.
        // strip the query portion from the URL to get the essence of the URL
        // we should use for comparison purposes.
        String url = getRequestingURL().toString();
        int queryPos = url.indexOf('?');
        if (queryPos != -1)
            url = url.substring(0, queryPos);
        return url;
    }


    public static void maybeInitialize(String title) {
        String setting = System.getProperty(ENABLED_SETTING_NAME);
        if (!"false".equals(setting))
            Authenticator.setDefault(INSTANCE = new HttpAuthenticator(title));
    }

    public static void setParentComponent(Component c) {
        if (INSTANCE != null) {
            INSTANCE.parentComponent = c;
            if (c instanceof Frame)
                INSTANCE.title = ((Frame) c).getTitle();
        }
    }

    /** @since 2.5.6 */
    public static boolean isPasswordDialogVisible() {
        return INSTANCE != null && INSTANCE.passwordDialogVisible;
    }

    /** @since 2.1.10 */
    public static String getLastUsername() {
        return (INSTANCE == null ? null : INSTANCE.lastUsername);
    }

    private static HttpAuthenticator INSTANCE = null;

}

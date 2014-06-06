// Copyright (C) 2014 Tuma Solutions, LLC
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

import java.awt.Dimension;
import java.awt.Font;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

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

    private State state;

    private float rememberMeDays;

    private String lastUrl;

    private long lastTimestamp;

    private String lastUsername;


    private static final String SETTING_PREFIX = //
            "net.sourceforge.processdash.HttpAuthenticator.";

    private static final String ENABLED_SETTING_NAME = SETTING_PREFIX
            + "enabled";

    private static final String REMEMBER_ME_SETTING_NAME = SETTING_PREFIX
            + "rememberMeDays";

    private static final String LAST_USERNAME = "lastUsername";

    private static final String REMEMBER_ME_UNTIL = "rememberUntil";

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;


    private HttpAuthenticator() {
        this.prefs = Preferences.userRoot().node(
                "net/sourceforge/processdash/userPrefs/authenticator");
        this.resources = Resources.getDashBundle("Authentication.Password");
        this.state = State.Initial;

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
            else
                state = State.UserEntry1;
        }

        // create user interface components to prompt for username and password
        JTextField username = new JTextField(2);
        JPasswordField password = new JPasswordField(2);
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
        String title = resources.getString("Title");
        Object[] message = new Object[] {
                resources.formatStrings("Prompt_FMT", getRequestingPrompt()),
                BoxUtils.vbox(5),
                BoxUtils.hbox(15, usernameLabel, 5, username),
                BoxUtils.hbox(15, passwordLabel, 5, password),
                BoxUtils.hbox(BoxUtils.GLUE, rememberMe),
                new JOptionPaneTweaker.GrabFocus(focus) };

        int userChoice = JOptionPane.showConfirmDialog(null, message, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // record metadata about this password request
        lastUrl = getEffectiveURL();
        lastTimestamp = System.currentTimeMillis();
        lastUsername = username.getText().trim();
        prefs.put(prefsKey(LAST_USERNAME), lastUsername);

        if (userChoice == JOptionPane.OK_OPTION) {
            // if the user entered credentials, return them.
            if (rememberMe != null && rememberMe.isSelected())
                savePasswordToKeyring(lastUsername, password.getPassword());
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
            lastUrl = null;
            lastTimestamp = -1;
            lastUsername = prefs.get(prefsKey(LAST_USERNAME), null);
            state = State.Initial;
        }

        // if we are not in a failure mode, increment the state by one position
        if (state.compareTo(State.Failed) < 0)
            state = State.values()[state.ordinal() + 1];
    }

    private boolean isRetry() {
        if (lastUrl == null || !lastUrl.equals(getEffectiveURL()))
            return false;

        long retryDelay = System.currentTimeMillis() - lastTimestamp;
        return retryDelay < 15 * DateUtils.SECONDS;
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

    private String getBaseURL() {
        String result = getEffectiveURL();
        int bridgePos = result.indexOf("DataBridge");
        if (bridgePos != -1)
            result = result.substring(0, bridgePos);
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


    public static void maybeInitialize() {
        if (Boolean.getBoolean(ENABLED_SETTING_NAME))
            Authenticator.setDefault(new HttpAuthenticator());
    }

}

//This class shared from the Giant Java Tree, http://www.gjt.org
//originally in package org.gjt.rm.gui;

package net.sourceforge.processdash.ui.lib;

import javax.swing.*;
import java.awt.*;
import java.net.*;
/**
 * Class <code>SplashScreen</code> displays splash screen that is used when
 * loading of the program takes some time
 * Usage:
 * SplashScreen ss = new SplashScreen("c:/images/image.jpg");
 * ss.setVisible(true);
 *
 * or if you want load an image from jar file using getClass().getResource(String)
 * SplashScreen ss = new SplashScreen(url);
 * ss.setVisible(true);
 *
 * to kill the splash screen use dispose() command:
 * ss.dispose();
 *
 * @author  Ruslan Makarov
 * @author  <a href="mailto:r_makarov@yahoo.com">r_makarov@yahoo.com<a>
 * @version 1.00, 02-04-2000
 *
 * Change log:
 * 02-03-2001: added timeout thread to kill the screen automatically after
 * a specified time
 */
public class SplashScreen extends JWindow {

    private static final int BORDERSIZE = 1;
    private ImageIcon ii;

    public SplashScreen(String imagePath) {
        ii = new ImageIcon(imagePath);
        createSplashScreen();
    }

    public SplashScreen(URL imageURL) {
        ii = new ImageIcon(imageURL);
        createSplashScreen();
    }

    private void createSplashScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBackground(Color.lightGray);
        int w = ii.getIconWidth()   + (BORDERSIZE * 2);
        int h = ii.getIconHeight()  + (BORDERSIZE * 2);
        int x = (screenSize.width - w) /2;
        int y = (screenSize.height - h) /2;
        setBounds(x, y, w, h);
    }

    public void paint(Graphics g) {
        g.drawImage(ii.getImage(), BORDERSIZE, BORDERSIZE,
                    ii.getIconWidth(), ii.getIconHeight(), this);
    }

    public static void main(String[] args) {
        SplashScreen ss = new SplashScreen("s:/images/49.gif");
        ss = new SplashScreen(ss.getClass().getResource("bill.jpg"));
        ss.setVisible(true);
    }

    private volatile boolean waitingForDelay = true;
    private volatile boolean waitingForOkay  = true;
    public synchronized void okayToDispose() {
        waitingForOkay = false;
        if (!waitingForDelay)
            notify();
    }
    private long delayMillis;

    public void displayFor(long millis) {
        setVisible(true);
        delayMillis = millis;
        Thread t = new Thread() {
                public void run() {
                    synchronized(SplashScreen.this) { try {
                        SplashScreen.this.wait(delayMillis);
                        waitingForDelay = false;
                        if (waitingForOkay)
                            SplashScreen.this.wait();
                    } catch (InterruptedException ie) {} }

                    SplashScreen.this.dispose();
                }
            };
        t.start();
    }
}

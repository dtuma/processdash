// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ui;

import java.applet.Applet;
import java.applet.AudioClip;
import java.net.URL;



/** This class provides simple access to sound files, and insulates
 * other classes from system-dependent problems.
 */
public class SoundClip {

    /** The sound clip itself. */
    private AudioClip clip = null;

    /** Create and initialize a sound clip that can be played later.
     *
     * @param url the URL of the sound clip file; typically obtained
     *   with Class.getResource().  <code>null</code> can be safely
     *   passed into this parameter; the resulting SoundClip object
     *   will be a no-op object, which does nothing when
     *   <code>play()</code> is called.
     */
    public SoundClip(URL url) {
        if (url != null)
            clip = Applet.newAudioClip(url);
    }

    /** Play a sound clip.
     */
    public void play() {
        if (clip != null)
            clip.play();
    }

}

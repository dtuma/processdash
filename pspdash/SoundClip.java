// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.net.URL;
import javax.sound.sampled.*;



/** This class provides simple access to sound files, and insulates
 * other classes from system-dependent problems.
 *
 * Possible future enhancement: make use of reflection to load and
 * play the sound clip, so platforms that do not have the
 * javax.sound.sampled package can still run.
 */
public class SoundClip {

    /** The sound clip itself. */
    private Clip clip = null;

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
            (new ClipLoader(url)).start();
    }

    /** Play a sound clip.
     */
    public void play() {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    /** On some systems, the logic for loading a sound clip may hang
     * forever.  to prevent this from hanging the dashboard forever,
     * we execute it in a separate thread.
     */
    private class ClipLoader extends Thread {
        URL url;
        public ClipLoader(URL u) { url = u; }
        public void run() {
            try {
                AudioInputStream soundFile =
                    AudioSystem.getAudioInputStream(url);
                AudioFormat soundFormat = soundFile.getFormat();
                int bufferSize = (int) (soundFile.getFrameLength() *
                                        soundFormat.getFrameSize());
                DataLine.Info info = new DataLine.Info
                    (Clip.class, soundFile.getFormat(), bufferSize);
                Clip result = (Clip) AudioSystem.getLine(info);
                result.open(soundFile); // may hang forever
                clip = result;
            } catch (Throwable t) {}
        }
    }
}

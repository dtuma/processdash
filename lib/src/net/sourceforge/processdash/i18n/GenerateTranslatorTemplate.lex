// -*- mode:java;  tab-width: 4  -*-
//
// This java file is automatically generated from the file
// "GenerateTranslatorTemplate.lex".  Do not edit the Java
// file directly; your edits will be overwritten.
//
// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Tuma Solutions, LLC
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
//
package PACKAGE;

import java.io.*;
import java.util.*;

%%

%{
    private Map dict = new HashMap();


    // This main() routine is provided for testing and convenience
    // purposes.  It is not used by the dashboard.
    public static void main(String[] args) {
        try {
            long start = System.currentTimeMillis();

            if (args.length > 1 && "-reader".equalsIgnoreCase(args[1]))
                readerCopy(args[0]);
            else
                defaultCopy(args[0]);

            long end = System.currentTimeMillis();
            long elapsed = end - start;
            System.err.println("took " +elapsed+ " ms.");

            Runtime r = Runtime.getRuntime();
            long mem = r.totalMemory() - r.freeMemory();
            System.err.println("used memory: "+mem);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void defaultCopy(String filename) throws IOException {
        CLASSNAME t = new CLASSNAME(new FileReader(filename));
        t.setOut(new OutputStreamWriter(System.out, "UTF-8"));
        while (t.next() != null)
            ;
    }

    private static void readerCopy(String filename) throws IOException {
        CLASSNAME t = new CLASSNAME();
        Reader r = t.translateStream(new FileReader(filename));
        Writer w = new OutputStreamWriter(System.out, "UTF-8");
        int c;
        while ((c = r.read()) != -1)
            w.write(c);
        w.flush();
    }


    public CLASSNAME(Map p) {
        dict = new HashMap(p);
    }

    private void setDictionary(Map p) {
        dict = new HashMap(p);
    }

    private void popChar() {
        yy_buffer_index--; yy_buffer_end--;
    }

    private Writer outstream;

    public void setOut(Writer out) {
        outstream = out;
    }


    private Boolean send() throws IOException {
        // this is phenomenally more efficient than writing yytext().
        // Since Strings must be immutable, yytext's call to new String()
        // must copy the yy_buffer character array.
        outstream.write(yy_buffer,
                        yy_buffer_start,
                        yy_buffer_end - yy_buffer_start);
        return Boolean.TRUE;
    }

    private Boolean send(String key) throws IOException {
        String replacement = (String) dict.get(key);
        if (replacement == null) {
            replacement = key.replace('_', ' ');
            dict.put(key, replacement);
        }
        outstream.write(replacement);
        return Boolean.TRUE;
    }

    public String translateString(String s) {
        if (s == null) return null;

        StringTranslator t = new StringTranslator(s, dict);
        return t.getResult();
    }

    private static final class StringTranslator {
        private CLASSNAME translator;
        private StringBuffer buf;
        private String result;
        private Reader r;
        public StringTranslator(String s, Map dict) {
            String ss = s + " ";
            buf = new StringBuffer();
            result = null;

            r = new StringReader(ss);
            translator = new CLASSNAME(r);
            translator.setDictionary(dict);
            StringWriter w = new StringWriter(ss.length());
            translator.setOut(w);
            buf = w.getBuffer();
        }
        public void run() {
            try {
                // grab locks for all the objects we'll need and hold them
                // for the duration of the copy operation, so we don't have
                // to release and acquire the locks for each character we
                // copy.
                synchronized (r) {
                    synchronized (buf) {
                        while (translator.next() != null)
                            ;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            result = buf.toString();
            result = result.substring(0, result.length()-1);
        }
        public String getResult() {
            if (result == null) run();
            return result;
        }
    }

    public Reader translateStream(Reader r) {
        return new TranslatingReader(r, dict);
    }

    private static final class TranslatingReader extends Reader {
        private CLASSNAME translator;
        private String sbuf = null;
        private char cbuf[] = null;
        private int next, rem;

        public TranslatingReader(Reader r, Map dict) {
            translator = new CLASSNAME(r);
            translator.setDictionary(dict);
            translator.setOut(new Writer() {
                    public void write(String str) {
                        cbuf = null;  sbuf = str; next = rem = 0;
                    }
                    public void write(char[] buf, int off, int len) {
                        sbuf = null;  cbuf = buf;  next = off;  rem = len;
                        if (len == 0) cbuf = null;
                    }
                    public void close() {}
                    public void flush() {}
                });
        }

        public void close() { translator = null; }
        public boolean markSupported() { return false; }

        public void mark(int readAheadLimit) throws IOException { unsupp(); }
        public void reset() throws IOException { unsupp(); }
        private void unsupp() throws IOException {
            throw new IOException("Not supported");
        }

        private boolean hasMoreChars() throws IOException {
            if (translator == null) return false;
            if (sbuf != null || cbuf != null) return true;
            if (translator.next() != null) return true;
            translator = null;
            return false;
        }

        public int read() throws IOException {
            if (!hasMoreChars()) return -1;

            int result;

            if (sbuf != null) {
                result = sbuf.charAt(next++);
                if (next == sbuf.length()) sbuf = null;
                return result;
            }

            if (cbuf != null) {
                result = cbuf[next++];
                if (--rem == 0) cbuf = null;
                return result;
            }

            return -1;
        }

        public int read(char[] cbuf) throws IOException {
            return read(cbuf, 0, cbuf.length);
        }

        public int read(char[] cbuf, int off, int len) throws IOException {
            if (translator == null) return -1;
            int read = 0, totalRead = 0;
            do {
                read = read1(cbuf, off, len);
                off += read;
                len -= read;
                totalRead += read;
            } while (len > 0 && read > 0);
            if (totalRead == 0 && translator == null) return -1;
            return totalRead;
        }

        private int read1(char[] buf, int off, int len) throws IOException {
            if (!hasMoreChars()) return 0;

            if (sbuf != null) {
                int available = sbuf.length() - next;
                if (available <= len) {
                    sbuf.getChars(next, next+available, buf, off);
                    sbuf = null;
                    return available;
                } else {
                    sbuf.getChars(next, next+len, buf, off);
                    next += len;
                    return len;
                }
            }

            if (cbuf != null) {
                if (rem <= len) {
                    System.arraycopy(cbuf, next, buf, off, rem);
                    cbuf = null;
                    return rem;
                } else {
                    System.arraycopy(cbuf, next, buf, off, len);
                    next += len;
                    rem -= len;
                    return len;
                }
            }

            return 0;
        }

        public boolean ready() {
            return (sbuf != null || cbuf != null);
        }
    }

%}

%init{
    if (dict == null) throw new NullPointerException();
%init}

%eof{
    try {
        outstream.flush();
    } catch (IOException ioe) {}
%eof}


%unicode
%ignorecase
%public
%class CLASSNAME
%implements TranslationEngine
%function next
%type Boolean
%state script, style, comment, tag

ANY=[\x00-\uffff]
WORD_CHAR=[A-Za-z]
NON_WORD_CHAR=[^A-Za-z<]
NON_WORD_CHAR_OR_TAG=[^A-Za-z]
WHITE_SPACE_CHAR=[\n\ \t\b\012]
SP=[\n\ \t\b\012]
IN_TAG=[^>]
SAFE_INNER=[^<>\-]

%%


<YYINITIAL> "<script"{IN_TAG}*">" { yybegin(script); return send(); }

<script> "</script"{IN_TAG}*">" { yybegin(YYINITIAL); return send(); }

<YYINITIAL> "<style"{IN_TAG}*">" { yybegin(style); return send(); }

<style> "</style"{IN_TAG}*">" { yybegin(YYINITIAL); return send(); }



<YYINITIAL> "<!--" { yybegin(comment); return send(); }

<comment> "-->" { yybegin(YYINITIAL); return send(); }


<YYINITIAL> "<"/?{WORD_CHAR}{IN_TAG}*">" { yybegin(YYINITIAL); return send(); }

<YYINITIAL> "<" { yybegin(tag); return send(); }

<tag> ">" { yybegin(YYINITIAL); return send(); }


<comment,script,style,tag> {ANY}|{SAFE_INNER}+ { return send(); }



<YYINITIAL> "REGEXP"{NON_WORD_CHAR_OR_TAG} { popChar(); return send("KEY"); }



<YYINITIAL> {WORD_CHAR}+|{NON_WORD_CHAR}+ { return send(); }

. { throw new IOException("Unmatched input: " + yytext()); }

// Copyright (C) 1997-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.engine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RedlinedDocument <T> {

    /**
     * A list of the lines of content in the document.
     * 
     * The zero item is a container for changes that appear before the first
     * line of content.  The remaining items starting with position 1 represent
     * the actual lines in the document.
     */
    protected List<Line> content;

    public RedlinedDocument(T[] base) {
        content = new ArrayList<Line>(base.length + 1);
        content.add(new Line(null, null));
        for (int i = 0; i < base.length; i++) {
            content.add(new Line(base[i], AccountingType.Base));
        }
    }

    public void applyChange(int documentPos, int numLinesToDelete,
            T[] newContent, int newPos, int numLinesToInsert,
            boolean trackChange) {

        if (numLinesToDelete > 0) {
            int fromIndex = documentPos + 1;
            int toIndex = fromIndex + numLinesToDelete;
            List<Line> deletedLines = content.subList(fromIndex, toIndex);

            content.get(documentPos).appendDeletedLines(deletedLines,
                trackChange);
            deletedLines.clear();
        }

        if (numLinesToInsert > 0) {
            AccountingType type = (trackChange ? AccountingType.Added : AccountingType.Base);
            List<Line> newLines = new ArrayList(numLinesToInsert);
            for (int i = 0;  i < numLinesToInsert;  i++) {
                T newContentObject = newContent[newPos + i];
                Line newLine = new Line(newContentObject, type);
                newLines.add(newLine);
            }
            content.addAll(documentPos + 1, newLines);
        }
    }

    public void updateContent(T[] newContent) {
        if (newContent.length != content.size() - 1)
            throw new IllegalArgumentException("Content size mismatch");

        for (int i = 0; i < newContent.length; i++) {
            Line line = content.get(i+1);
            if (line.content.equals(newContent[i]))
                line.content = newContent[i];
            else
                throw new IllegalArgumentException("Content text mismatch");
        }
    }

    public class Block {
        List<T> baseLines;
        List<T> deletedLines;
        List<T> addedLines;

        public List<DiffFragment> convertToFragments() {
            List<DiffFragment> result = new ArrayList<DiffFragment>(3);
            maybeAppendFragment(result, AccountingType.Base, baseLines);
            maybeAppendFragment(result, AccountingType.Deleted, deletedLines);
            maybeAppendFragment(result, AccountingType.Added, addedLines);
            return result;
        }

        private void maybeAppendFragment(List<DiffFragment> fragments,
                AccountingType type, List<T> lines) {
            if (lines != null && !lines.isEmpty())
                fragments.add(new DiffFragment(type, appendLines(lines)));
        }

        private String appendLines(List<T> lines) {
            StringBuilder b = new StringBuilder();
            for (T line : lines)
                b.append(line.toString()).append("\n");
            return b.toString();
        }

        private boolean addLine(Line l) {
            if (l.type == AccountingType.Base) {
                if (deletedLines != null || addedLines != null) {
                    return false;
                } else {
                    baseLines = addContent(baseLines, l);
                }
            } else if (l.type == AccountingType.Added) {
                addedLines = addContent(addedLines, l);
            }

            deletedLines = addAllContent(deletedLines, l.deletedContentAfter);

            return true;
        }

        private List<T> addContent(List<T> list, Line line) {
            if (list == null)
                list = new ArrayList<T>();
            list.add(line.content);
            return list;
        }

        private List<T> addAllContent(List<T> list, List<T> lines) {
            if (lines == null || lines.isEmpty())
                return list;
            if (list == null)
                list = new ArrayList<T>();
            list.addAll(lines);
            return list;
        }

        private boolean isEmpty() {
            return baseLines == null && deletedLines == null
                    && addedLines == null;
        }

    }

    public List<Block> getRedlineBlocks() {
        List<Block> result = new ArrayList();

        Block currentBlock = new Block();
        for (Line l : content) {
            if (currentBlock.addLine(l) == false) {
                result.add(currentBlock);
                currentBlock = new Block();
                currentBlock.addLine(l);
            }
        }
        if (!currentBlock.isEmpty())
            result.add(currentBlock);

        return result;
    }

    private class Line {

        public T content;

        public AccountingType type;

        public List<T> deletedContentAfter;

        public Line(T content, AccountingType type) {
            this.content = content;
            this.type = type;
        }

        public void appendDeletedLines(List<Line> deletedLines,
                boolean trackChange) {
            if (deletedContentAfter == null)
                deletedContentAfter = new LinkedList<T>();

            for (Line l : deletedLines) {
                if (trackChange && l.type != AccountingType.Added)
                    deletedContentAfter.add(l.content);
                if (l.deletedContentAfter != null)
                    deletedContentAfter.addAll(l.deletedContentAfter);
            }
        }

        public String toString() {
            return content.toString();
        }

    }
}

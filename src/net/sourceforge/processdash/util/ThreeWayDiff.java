// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ThreeWayDiff<T> {

    private T[] base;

    private T[] a;

    private T[] b;


    public ThreeWayDiff(T[] base, T[] a, T[] b) {
        this.base = base;
        this.a = a;
        this.b = b;
    }


    public static class ResultItem<T> {
        /** An item appearing in the merged result */
        public T item;

        /**
         * The position of the item in the base list. If the item was inserted,
         * by A or B, this will be -1
         */
        public int basePos;

        /**
         * The position of the item in list A. If the item was deleted by A, or
         * was inserted by B, this will be -1.
         */
        public int aPos;

        /**
         * The position of the item in list B. If the item was deleted by B, or
         * was inserted by A, this will be -1.
         */
        public int bPos;

        public boolean isUnchanged() {
            return basePos != -1 && aPos != -1 && bPos != -1;
        }

        public boolean isInserted() {
            return basePos == -1;
        }

        public boolean isInsertedByA() {
            return isInserted() && aPos != -1;
        }

        public boolean isInsertedByB() {
            return isInserted() && bPos != -1;
        }
        public boolean isDeleted() {
            return isDeletedByA() || isDeletedByB();
        }

        public boolean isDeletedByA() {
            return aPos == -1 && basePos != -1;
        }

        public boolean isDeletedByB() {
            return bPos == -1 && basePos != -1;
        }

        private ResultItem(T item, int basePos, int aPos, int bPos) {
            this.item = item;
            this.basePos = basePos;
            this.aPos = aPos;
            this.bPos = bPos;
        }

    }


    public ResultItem<T>[] getMergedResult() {
        Insertion[] insertedByA = new Insertion[base.length + 1];
        boolean[] deletedByA = new boolean[base.length];
        Insertion[] insertedByB = new Insertion[base.length + 1];
        boolean[] deletedByB = new boolean[base.length];

        Diff aDiff = new Diff(base, a);
        Diff.change aChanges = aDiff.diff_2(false);
        recordChanges(insertedByA, deletedByA, aChanges);

        Diff bDiff = new Diff(base, b);
        Diff.change bChanges = bDiff.diff_2(false);
        recordChanges(insertedByB, deletedByB, bChanges);

        List<ResultItem> result = new ArrayList<ResultItem>();
        InsertedItemFactory<T> aFactory = new InsertedItemFactoryA();
        InsertedItemFactory<T> bFactory = new InsertedItemFactoryB();
        int aPos = 0;
        int bPos = 0;

        for (int i = 0; i <= base.length; i++) {
            if (insertedByA[i] != null)
                aPos = addInsertions(result, insertedByA[i], aFactory);
            if (insertedByB[i] != null)
                bPos = addInsertions(result, insertedByB[i], bFactory);

            if (i < base.length) {
                ResultItem<T> item = new ResultItem<T>(base[i], i, -1, -1);
                if (deletedByA[i] == false)
                    item.aPos = aPos++;
                if (deletedByB[i] == false)
                    item.bPos = bPos++;
                result.add(item);
            }
        }

        return (ResultItem<T>[]) result.toArray(new ResultItem[result.size()]);
    }

    private void recordChanges(Insertion[] insertions, boolean[] deletions,
            Diff.change c) {
        while (c != null) {
            if (c.deleted > 0)
                Arrays.fill(deletions, c.line0, c.line0 + c.deleted, true);

            if (c.inserted > 0)
                insertions[c.line0 + c.deleted] = new Insertion(c.line1,
                        c.inserted);

            // go to the next change.
            c = c.link;
        }
    }

    private int addInsertions(List<ResultItem> result, Insertion insertion,
            InsertedItemFactory<T> factory) {
        for (int i = 0; i < insertion.itemCount; i++) {
            int dataPos = insertion.itemPos + i;
            result.add(factory.getItem(dataPos));
        }
        return insertion.itemPos + insertion.itemCount;
    }

    private static class Insertion {
        public int itemPos;

        public int itemCount;

        public Insertion(int itemPos, int itemCount) {
            this.itemPos = itemPos;
            this.itemCount = itemCount;
        }

    }

    private interface InsertedItemFactory<T> {
        ResultItem<T> getItem(int pos);
    }

    private class InsertedItemFactoryA implements InsertedItemFactory<T> {
        public ResultItem<T> getItem(int pos) {
            return new ResultItem<T>(a[pos], -1, pos, -1);
        }
    }

    private class InsertedItemFactoryB implements InsertedItemFactory<T> {
        public ResultItem<T> getItem(int pos) {
            return new ResultItem<T>(b[pos], -1, -1, pos);
        }
    }

}

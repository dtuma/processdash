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

package net.sourceforge.processdash.ui.lib.chart;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.jfree.data.general.PieDataset;

public class StandardDiscItemDistributor extends AbstractDiscItemDistributor {

    Double forcedAspectRatio = Double.valueOf(1.0);

    boolean round = true;

    public StandardDiscItemDistributor() {
        super();
    }

    public StandardDiscItemDistributor(PieDataset dataset) {
        super(dataset);
    }

    public Double getForcedAspectRatio() {
        return forcedAspectRatio;
    }

    public void setForcedAspectRatio(Double forcedAspectRatio) {
        this.forcedAspectRatio = forcedAspectRatio;
        invalidate();
    }

    public void setCircular(boolean circular) {
        if (circular) {
            setForcedAspectRatio(Double.valueOf(1.0));
            setRound(true);
        } else {
            setForcedAspectRatio(null);
        }
    }

    public boolean isRound() {
        return round;
    }

    public void setRound(boolean round) {
        this.round = round;
        invalidate();
    }

    @Override
    public void setDiscDataArea(Rectangle2D discDataArea) {
        if (forcedAspectRatio == null && this.discs != null) {
            this.discDataArea = discDataArea;
            translateDiscs(this.discs);
        } else {
            super.setDiscDataArea(discDataArea);
        }
    }

    @Override
    protected void distributeDiscs(DiscItemRecord[] discsToDistribute) {
        int numDiscs = countVisibleDisks(discsToDistribute);
        if (numDiscs == 0)
            return;

        double aspect = getAspect();
        double aspectAdjust = 1.0 / (aspect * aspect);

        DiscItemRecord[] discs = discsToDistribute.clone();
        boolean[] flags = new boolean[discs.length];
        Arrays.sort(discs, SIZE_COMPARATOR);

        double totalArea = 0;
        for (int i = 0; i < numDiscs; i++) {
            totalArea += discs[i].getR() * discs[i].getR();
        }

        discs[0].setX(0);
        discs[0].setY(0);
        Rectangle2D.Double bounds = discs[0].getBounds();

        Random rand = new Random(0);
        int iterCount = 350;// Math.min(50, 500000 / circles.length);
        double placedArea = 0;
        double maxHeight = -1;
        double maxWidth = -1;

        for (int i = 1; i < numDiscs; i++) {
            DiscItemRecord next = discs[i];
            double r = next.getR();
            double xMid = bounds.getCenterX();
            double yMid = bounds.getCenterY();

            double bestX = Double.NaN;
            double bestY = Double.NaN;
            double bestScore = Double.POSITIVE_INFINITY;
            boolean isCompressed = false;

            for (boolean useLimit = !round; Double.isNaN(bestX); useLimit = false) {
                double left, top, xWindow, yWindow;

                if (useLimit && maxHeight > 0) {
                    yWindow = maxHeight - next.getR() * 2;
                    top = yMid - yWindow / 2;
                } else {
                    yWindow = bounds.height + 2 * r;
                    top = bounds.y - r;
                }

                if (useLimit && maxWidth > 0) {
                    xWindow = maxWidth - next.getR() * 2;
                    left = xMid - xWindow / 2;
                } else {
                    xWindow = bounds.width + 2 * r;
                    left = bounds.x - r;
                }

                // find random best location
                tryLocation: for (int iter = 0; iter < iterCount; iter++) {
                    double x = left + xWindow * rand.nextDouble();
                    double y = top + yWindow * rand.nextDouble();
                    double dx = x - xMid;
                    double dy = y - yMid;
                    double score = dx * dx * aspectAdjust + dy * dy;
                    if (score > bestScore)
                        continue;

                    for (int j = 0; j < i; j++) {
                        double xDist = discs[j].getX() - x;
                        double yDist = discs[j].getY() - y;
                        double distSq = xDist * xDist + yDist * yDist;
                        double minDist = discs[j].getR() + r;
                        double minSq = minDist * minDist;
                        if (distSq < minSq)
                            continue tryLocation;
                    }

                    if (numDiscs < 300) {
                        next.setX(x);
                        next.setY(y);
                        compressToCenter(discs, flags, i, next, xMid, yMid);
                        isCompressed = true;
                        x = next.getX();
                        y = next.getY();
                        dx = x - xMid;
                        dy = y - yMid;
                        score = dx * dx * aspectAdjust + dy * dy;
                    }

                    if (score < bestScore) {
                        bestX = x;
                        bestY = y;
                        bestScore = score;
                    }

                    if (score * 2 < bestScore) {
                        double distance = Math.sqrt(score);
                        top = yMid - distance;
                        yWindow = distance * 2;
                        double xDistance = distance * aspect;
                        left = xMid - xDistance;
                        xWindow = xDistance * 2;
                    }
                }
            }

            next.setX(bestX);
            next.setY(bestY);

            // compression step
            if (!isCompressed)
                compressToCenter(discs, flags, i, next, xMid, yMid);

            Rectangle2D.union(bounds, next.getBounds(), bounds);

            placedArea += next.getR() * next.getR();
            if (placedArea > totalArea * 0.7) {
                maxHeight = bounds.getHeight();
                maxWidth = bounds.getWidth();
            }
        }

    }

    protected int countVisibleDisks(DiscItemRecord[] discs) {
        int result = 0;
        for (int i = 0; i < discs.length; i++) {
            if (discs[i].getR() > 0)
                result++;
        }
        return result;
    }

    protected double getAspect() {
        if (forcedAspectRatio != null)
            return forcedAspectRatio.doubleValue();
        else
            return discDataArea.getWidth() / discDataArea.getHeight();
    }

    private void compressToCenter(DiscItemRecord[] discs, boolean[] flags,
            int i, DiscItemRecord next, double xMid, double yMid) {
        double x = next.getX();
        double y = next.getY();
        double r = next.getR();
        double dx = x - xMid;
        double dy = y - yMid;
        for (int k = 0; k < i; k++)
            flags[k] = checkMightOverlap(next, xMid, yMid, dx, dy, r);

        double xDelta = -dx / 20;
        double yDelta = -dy / 20;
        double cutoff = Math.min(Math.min(Math.abs(dx), Math.abs(dy)), r) / 100;
        do {
            x += xDelta;
            y += yDelta;

            if (overlaps(discs, flags, i, x, y, r)) {
                x -= xDelta;
                y -= yDelta;
                xDelta *= 0.5;
                yDelta *= 0.5;

            } else if (Math.signum(x - xMid) == Math.signum(xDelta)) {
                xDelta *= -0.5;
                yDelta *= -0.5;
            }

        } while (Math.abs(xDelta) + Math.abs(yDelta) > cutoff);

        next.setX(x);
        next.setY(y);
    }

    private boolean checkMightOverlap(DiscItemRecord disc, double x0,
            double y0, double dx, double dy, double rr) {
        double slope = dy / dx;
        int dirX = 1;
        int dirY = (slope > 0 ? -1 : 1);
        int sign1 = getOverlapSign(disc, x0, y0, slope, rr, dirX, dirY);
        int sign2 = getOverlapSign(disc, x0, y0, slope, rr, -dirX, -dirY);
        return (sign1 != sign2);
    }

    private int getOverlapSign(DiscItemRecord disc, double x0, double y0,
            double slope, double rr, int dirX, int dirY) {
        double testX = disc.getX() + dirX * (rr + disc.getR());
        double testY = disc.getY() + dirY * (rr + disc.getR());
        double lineY = slope * (testX - x0) + y0;
        return (int) Math.signum(testY - lineY);
    }

    private static boolean overlaps(DiscItemRecord[] discs, boolean[] flags,
            int n, double x, double y, double r) {
        while (n-- > 0) {
            DiscItemRecord d = discs[n];
            if (flags != null && !flags[n])
                continue;
            double xDist = d.getX() - x;
            double yDist = d.getY() - y;
            double distSq = xDist * xDist + yDist * yDist;
            double minDist = d.getR() + r;
            double minSq = minDist * minDist;
            if (distSq < minSq)
                return true;
        }
        return false;
    }


    private static class SizeComparator implements Comparator<DiscItemRecord> {

        public int compare(DiscItemRecord c1, DiscItemRecord c2) {
            if (c1.getR() > c2.getR() || Double.isNaN(c2.getR()))
                return -1;
            else if (c1.getR() < c2.getR() || Double.isNaN(c1.getR()))
                return +1;
            else
                return 0;
        }

    }

    private static final Comparator<DiscItemRecord> SIZE_COMPARATOR = new SizeComparator();

}

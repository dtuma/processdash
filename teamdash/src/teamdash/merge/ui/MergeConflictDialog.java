// Copyright (C) 2012-2022 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.merge.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.WrappingHtmlLabel;

import teamdash.merge.MergeWarning;
import teamdash.merge.ModelType;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.TeamProject;
import teamdash.wbs.UndoList;
import teamdash.wbs.WBSModelMergeConflictNotificationFactory;
import teamdash.wbs.icons.WBSEditorIcon;

public class MergeConflictDialog implements DataModelSource {

    private TeamProject teamProject;

    private JPanel content;

    private JDialog frame;

    private NotificationList notificationList;

    private JScrollPane scrollPane;

    private Map<ModelType, MergeConflictHyperlinkHandler> hyperlinkHandlers;

    private Map<ModelType, DataTableModel> dataModels;

    static final Resources resources = Resources
            .getDashBundle("WBSEditor.Merge");

    public MergeConflictDialog(TeamProject teamProject) {
        this.teamProject = teamProject;

        content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
        content.setLayout(new BorderLayout(0, 10));
        content.add(new JLabel(resources.getString("Conflict_Window.Header")),
            BorderLayout.NORTH);

        notificationList = new NotificationList();

        scrollPane = new JScrollPane(notificationList,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        content.add(scrollPane, BorderLayout.CENTER);

        hyperlinkHandlers = new HashMap();
        dataModels = new HashMap();
    }

    private void makeFrame(Component parent) {
        Window parentWindow;
        if (parent instanceof Window)
            parentWindow = (Window) parent;
        else
            parentWindow = SwingUtilities.getWindowAncestor(parent);

        frame = new JDialog(parentWindow,
                resources.getString("Conflict_Window.Title"),
                ModalityType.MODELESS);
        WBSEditorIcon.setWindowIcon(frame);
        frame.getContentPane().add(content);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(500, 300);
    }

    public void setHyperlinkHandler(ModelType type,
            MergeConflictHyperlinkHandler handler) {
        hyperlinkHandlers.put(type, handler);
    }

    public void setDataModel(ModelType type, DataTableModel dataModel) {
        dataModels.put(type, dataModel);
    }

    public DataTableModel getDataModel(ModelType type) {
        return dataModels.get(type);
    }

    public void addNotifications(List<MergeConflictNotification> nn) {
        if (nn != null && !nn.isEmpty()) {
            WBSModelMergeConflictNotificationFactory.refineAll(nn, this);
            for (MergeConflictNotification n : nn)
                notificationList.addNotification(n);
        }
    }

    public boolean maybeShow(Component relativeTo) {
        boolean notificationsPresent = notificationList.getComponentCount() > 0;
        if (notificationsPresent) {
            if (frame == null)
                makeFrame(relativeTo);
            if (!frame.isVisible()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        notificationList.scrollRectToVisible(new Rectangle(0,
                                0, 1, 1));
                    }});
                frame.setLocationRelativeTo(relativeTo);
            }
            frame.setVisible(true);
        }
        return notificationsPresent;
    }


    /**
     * Scrollable panel to display conflict notification items
     */
    private class NotificationList extends JPanel implements Scrollable {

        private NotificationList() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        public void addNotification(MergeConflictNotification n) {
            for (int i = getComponentCount();  i-- > 0; ) {
                Object c = getComponent(i);
                if (c instanceof NotificationItem) {
                    NotificationItem oldItem = (NotificationItem) c;
                    if (oldItem.matches(n))
                        remove(i);
                }
            }

            add(new NotificationItem(n));
            updateAppearance();
        }

        protected void updateAppearance() {
            invalidate();
            doLayout();
            scrollPane.repaint();
        }

        public void removeNotificationItem(NotificationItem item) {
            remove(item);
            updateAppearance();

            if (getComponentCount() == 0)
                frame.setVisible(false);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(500, 400);
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect,
                int orientation, int direction) {
            return 75;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect,
                int orientation, int direction) {
            return (int) (visibleRect.getHeight() * 0.9);
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * An element representing a single conflict notification.  Displays the
     * textual description of the conflict, along with options for conflict
     * resolution.
     */
    private class NotificationItem extends WrappingHtmlLabel implements
            HyperlinkListener, ActionListener {

        private MergeConflictNotification notification;

        private NotificationItem(MergeConflictNotification notification) {
            super(notification.getAsHtml());

            this.notification = notification;

            setBackground(Color.white);
            setBorder(REGULAR_ITEM_BORDER);
            addHyperlinkListener(this);
        }

        public boolean matches(MergeConflictNotification n) {
            MergeWarning thisWarning = notification.getMergeWarning();
            MergeWarning thatWarning = n.getMergeWarning();
            return thisWarning.equals(thatWarning);
        }

        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URL u = e.getURL();
                if (u == null)
                    return;
                String type = u.getHost();
                String command = u.getPath().substring(1);
                if ("Opt".equals(type))
                    runUserOption(command);
                else
                    followItemHyperlink(type, command);
            }
        }

        private void runUserOption(String option) {
            MergeConflictHandler handler = notification.getUserOptions().get(
                option);
            if (handler != null) {
                try {
                    handler.handle(notification, teamProject);
                    UndoList.madeChange(getAssociatedTable(), option
                            + " editing conflict");
                } catch (Exception e) {
                    e.printStackTrace();
                    Toolkit.getDefaultToolkit().beep();
                }
            }

            removeThisItem();
        }

        private JTable getAssociatedTable() {
            DataTableModel model = dataModels.get(notification.getModelType());
            if (model != null) {
                for (Object l : model.getListeners(TableModelListener.class)) {
                    if (l instanceof JTable)
                        return (JTable) l;
                }
            }
            return null;
        }

        private void followItemHyperlink(String typeName, String command) {
            ModelType type = ModelType.valueOf(typeName);
            MergeConflictHyperlinkHandler handler = hyperlinkHandlers.get(type);
            try {
                if (handler != null && handler.displayHyperlinkedItem(command))
                    return;
            } catch (Exception e) {}
            Toolkit.getDefaultToolkit().beep();
        }

        boolean firstAnimationPass;
        Timer animationTimer;

        private void removeThisItem() {
            firstAnimationPass = true;
            animationTimer = new Timer(50, this);
            animationTimer.start();
        }

        public void actionPerformed(ActionEvent e) {
            Dimension d = getSize();
            if (d.height < 15) {
                notificationList.removeNotificationItem(this);
                animationTimer.stop();
                animationTimer = null;
            } else {
                if (firstAnimationPass) {
                    setBorder(DELETE_ANIMATION_BORDER);
                    firstAnimationPass = false;
                }
                d.height -= 15;
                setPreferredSize(d);
                setMaximumSize(d);
                notificationList.updateAppearance();
            }
        }

    }


    private static final Border REGULAR_ITEM_BORDER = BorderFactory
            .createMatteBorder(3, 9, 3, 9, Color.white);

    private static final Border DELETE_ANIMATION_BORDER =
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3, 9, 6, 9, Color.white),
            BorderFactory.createMatteBorder(0, 0, 2, 0, Color.gray));

}

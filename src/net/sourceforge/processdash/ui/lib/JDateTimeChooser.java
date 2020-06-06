// Copyright (C) 2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.BorderLayout;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.toedter.calendar.JCalendar;

public class JDateTimeChooser extends JPanel {

    private DateFormat timeFormat, timeParse;

    private JTextField timeField;

    private JRadioButton amButton, pmButton;

    private JCalendar dateChooser;


    public JDateTimeChooser() {
        this(new Date());
    }

    public JDateTimeChooser(Date dateTime) {
        setLayout(new BorderLayout(0, 10));

        timeFormat = new SimpleDateFormat("h:mm");
        timeParse = new SimpleDateFormat("h:mm a");
        timeField = new JTextField(5);
        timeField.setMaximumSize(timeField.getPreferredSize());
        timeField.setHorizontalAlignment(JTextField.CENTER);

        ButtonGroup g = new ButtonGroup();
        g.add(amButton = new JRadioButton(AM_PM_STRINGS[0]));
        g.add(pmButton = new JRadioButton(AM_PM_STRINGS[1]));
        add(BoxUtils.hbox(BoxUtils.GLUE, timeField, 10, amButton, 5, pmButton,
            BoxUtils.GLUE), BorderLayout.NORTH);

        dateChooser = new JCalendar();
        dateChooser.setWeekOfYearVisible(false);
        add(dateChooser, BorderLayout.CENTER);

        setDateTime(dateTime);
    }

    public void setDateTime(Date d) {
        timeField.setText(timeFormat.format(d));
        if (timeFormat.getCalendar().get(Calendar.HOUR_OF_DAY) < 12)
            amButton.setSelected(true);
        else
            pmButton.setSelected(true);
        dateChooser.setDate(d);
    }

    public Date getDateTime() {
        try {
            // build a complete time string and parse the time
            String time = timeField.getText().trim();
            if (time.indexOf(':') == -1)
                time = time + ":00";
            time = time + " " + AM_PM_STRINGS[amButton.isSelected() ? 0 : 1];
            timeParse.parse(time);

            // copy the parsed hour/minute into the date chooser's calendar
            Calendar srcCal = timeParse.getCalendar();
            Calendar destCal = dateChooser.getCalendar();
            destCal.set(Calendar.HOUR_OF_DAY, srcCal.get(Calendar.HOUR_OF_DAY));
            destCal.set(Calendar.MINUTE, srcCal.get(Calendar.MINUTE));
            destCal.set(Calendar.SECOND, 0);
        } catch (Exception e) {
            // if we encountered an unparseable time, just stick with the
            // time of day that's already in the date chooser's calendar
        }

        return dateChooser.getDate();
    }

    public JCalendar getJCalendar() {
        return dateChooser;
    }

    private static final String[] AM_PM_STRINGS = new DateFormatSymbols()
            .getAmPmStrings();

}

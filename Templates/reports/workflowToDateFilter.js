// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2016 Tuma Solutions, LLC
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

var WFilt = {

    init : function() {
        var filterClickEvent = WFilt.filterClick.bindAsEventListener(this);
        var clearFieldEvent = WFilt.clearField.bindAsEventListener(this);
        var matchNumbersFunc = WFilt.matchNumbers.bind(this);
        var filterOnEvent = WFilt.filterOn.bindAsEventListener(this);
        var filterOffEvent = WFilt.filterOff.bindAsEventListener(this);
        this.filterIDs = new Array();

        $A($("filterRow").getElementsByTagName("td")).each(function(td) {
            // only examine top-level TD elements (not TDs in nested tables)
            if (td.parentNode.id != "filterRow")
                return;

            // set click handlers for hyperlinks
            var links = td.getElementsByTagName("a");
            links[0].onclick = filterClickEvent;
            for (var i = 1; i < links.length; i++) {
                if (links[i].className == "clearButton")
                    links[i].onclick = clearFieldEvent;
            }

            // read and record the ID of this filter
            var inputs = td.getElementsByTagName("input");
            var filterID = inputs[0].value;
            WFilt.filterIDs.push(filterID);
            WFilt.cols[filterID] = matchNumbersFunc; // default match handler

            // pre-enable this filter if indicated by the HTML
            if (inputs[1].value)
                Element.addClassName(td, "filterEnabled");

            // add handlers for the filter enablement buttons
            inputs[inputs.length - 3].onclick = filterOnEvent;
            inputs[inputs.length - 2].onclick = filterOffEvent;
            inputs[inputs.length - 1].onclick = filterClickEvent;
        });

        this.rows = $A($("data").getElementsByTagName("tr"));
        this.rows.shift();

        this.cols["proj"] = this.matchSelectedValues.bind(this);
        this.cols["date"] = this.matchDates.bind(this);

        this.applyFilters();
    },

    filterClick : function(event) {
        var td = Event.findElement(event, "td");
        var wasActive = Element.hasClassName(td, "filterActive");
        $A($("filterRow").getElementsByTagName("td")).each(function(e) {
            Element.removeClassName(e, "filterActive");
        });
        if (wasActive == false)
            Element.addClassName(td, "filterActive");
        return false;
    },

    clearField : function(event) {
        var tr = Event.findElement(event, "tr");
        Form.getInputs(tr, "text")[0].value = "";
        return false;
    },

    filterOn : function(event) {
        var td = Event.findElement(event, "td");
        Element.removeClassName(td, "filterActive");
        Element.addClassName(td, "filterEnabled");
        td.getElementsByTagName("input")[1].value = "true";
        this.applyFilters();
    },

    filterOff : function(event) {
        var td = Event.findElement(event, "td");
        this.filterOffTd(td);
        this.applyFilters();
    },

    filterOffTd : function(td) {
        Element.removeClassName(td, "filterActive");
        Element.removeClassName(td, "filterEnabled");
        td.getElementsByTagName("input")[1].value = "";
    },

    applyFilters : function() {
        var filters = this.createFilterEvaluators();
        this.evaluateFilters(filters);
    },

    createFilterEvaluators : function() {
        var filters = {};
        for (var i = 0; i < this.filterIDs.length; i++) {
            var id = this.filterIDs[i];
            var td = $(id + "Filter");
            if (!td)
                continue;

            var enabled = Form.getInputs(td, "hidden", id + "Enabled")[0].value;
            if (!!enabled) {
                var generator = this.cols[id];
                var evaluator = generator(td, id);
                filters[id] = evaluator;
            } else {
                filters[id] = Prototype.K;
            }
        }
        return filters;
    },

    evaluateFilters : function(filters) {
        for (var i = 0; i < this.rows.length; i++) {
            var row = $(this.rows[i]);
            var rowID = row.id;
            row.removeClassName("filterExcluded");
            for (var j = 0; j < this.filterIDs.length; j++) {
                var id = this.filterIDs[j];
                var filter = filters[id];
                var td = $(rowID + "." + id);
                if (!filter || !td) {
                    // do nothing
                } else if (filter(td)) {
                    td.removeClassName("filterExcluded");
                } else {
                    td.addClassName("filterExcluded");
                    row.addClassName("filterExcluded");
                }
            }
        }
    },

    matchSelectedValues : function(filter, id) {
        // does the user want us to include or exclude selected values?
        var exclude = this.getRadioVal(filter, id + "Logic") != "include";

        // get the list of values the user has selected
        var values = Form.getInputs(filter, "checkbox", id + "Val").findAll(
                function(element) {
                    return element.checked;
                }).pluck("value");

        // build a function which can test to see if a given table cell
        // is included/excluded as appropriate
        return function(td) {
            var match = Form.getInputs(td, "hidden", "val").find(function(e) {
                return values.indexOf(e.value) != -1;
            });
            return !match == exclude;
        };
    },

    matchDates : function(filter, id) {
        // get the completion date boundaries
        var before = Form.getInputs(filter, "text", "dateBefore")[0].value
                .trim();
        var after = Form.getInputs(filter, "text", "dateAfter")[0].value.trim();

        // possibly disable the filter if no dates are selected
        if (!before && !after) {
            this.filterOffTd(filter);
            return Prototype.K;
        }

        // build a function which can test to see if a given table cell
        // is included/excluded as appropriate
        return function(td) {
            var val = Form.getInputs(td, "hidden", "val")[0].value;
            if (before && before <= val)
                return false;
            if (after && after >= val)
                return false;
            return true;
        };
    },

    matchNumbers : function(filter, id) {
        // get the numeric boundaries
        var min = Form.getInputs(filter, "text", id + "Min")[0].value.trim();
        var max = Form.getInputs(filter, "text", id + "Max")[0].value.trim();

        // possibly disable the filter if no values are selected
        if (!min && !max) {
            this.filterOffTd(filter);
            return Prototype.K;
        }
        var min_ = this.parseNum(min);
        var max_ = this.parseNum(max);

        // build a function which can test to see if a given table cell
        // is included/excluded as appropriate
        return function(td) {
            var val = WFilt.parseNum(td.innerHTML);
            if (min && val < min_)
                return false;
            if (max && max_ <= val)
                return false;
            return true;
        };
    },

    getRadioVal : function(form, name) {
        var radioElements = Form.getInputs(form, "radio", name);
        for (var i = 0; i < radioElements.length; i++) {
            if (radioElements[i].checked)
                return radioElements[i].value;
        }
    },

    parseNum : function(str) {
        if (this.commaNum) {
            str = str.replace(/\./g, "");
            str = str.replace(",", ".");
        } else {
            str = str.replace(/,/g, "");
        }
        str = str.replace(/\s/g, "");
        return parseFloat(str);
    },

    commaNum : false,

    rows : [],

    cols : {}

};

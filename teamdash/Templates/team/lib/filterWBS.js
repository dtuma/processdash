
var SILENT=true;

var wbsListField = document.getElementById("wbsIdList");

function setWbsIdList(listVal) {
    wbsListField.value = listVal;
    changeNotifyElem(wbsListField);
}

function toggleSelected(link) {
    var cell = link.parentNode;
    var checkbox = cell.getElementsByTagName('INPUT')[0];
    checkbox.checked = !checkbox.checked;
    updateSelected(checkbox);
}

function updateSelected(checkbox) {
    var newList = updateChecks(checkbox);
    setWbsIdList(newList);
}

function updateChecks(checkbox) {
    var checked = checkbox.checked;
    var row = checkbox.parentNode.parentNode;
    var selId = row.id;
    var rows = row.parentNode.getElementsByTagName('TR');

    var result = "\t";
    var rowId;
    var lastResultRowId = "none";

    for (var j = 0; j < rows.length; j++) {
        row = rows[j];
        rowId = row.id;
        checkbox = row.getElementsByTagName('INPUT')[0];


        if ((!checked && isAncestor(rowId, selId))) {
            // if this row is an ancestor of the selected row, and the
            // selected row has been unchecked, then we need to uncheck
            // this ancestor also.
            checkbox.checked = false;

        } else if (isAncestor(selId, rowId)) {
            // if the selected row is an ancestor of this row, then
            // we need to make the checked status match the selected row.
            checkbox.checked = checked;
        }

        if (checkbox.checked && !isAncestor(lastResultRowId, rowId)) {
            lastResultRowId = rowId;
            result = result + checkbox.name + "\t";
        }
    }

    if (result.length == 1) {
        return "none";
    } else {
        return result;
    }
}

function isAncestor(ancestor, target) {
    ancestor = ancestor + "-";
    var pos = target.indexOf(ancestor);
    return (pos == 0);
}

function initializeCheckboxes() {
    var selectedItems = wbsListField.value;
    if (selectedItems == "initializing") {
        self.setTimeout("initializeCheckboxes()", 100);
        return;
    }
    selectedItems = selectedItems.split("\t");

    var firstRow = document.getElementById('wbs');
    var rows = firstRow.parentNode.getElementsByTagName('TR');
    var checkbox;
    var wbsId;

    for (var j = 0; j < rows.length; j++) {
        checkbox = rows[j].getElementsByTagName('INPUT')[0];
        wbsId = checkbox.name;
        checkbox.checked = isMatch(wbsId, selectedItems);
    }
}

function isMatch(target, selectedItems) {
    var item;
    for (var i=0;  i < selectedItems.length;  i++) {
        item = selectedItems[i];
        if (target == item) return true;
        if (target.indexOf(item+"/") == 0) return true;
    }
    return false;
}

initializeCheckboxes();

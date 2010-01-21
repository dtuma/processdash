addEvent(window, "load", evts_tree_sort_init);

function evts_tree_sort_init() {
    // Find all tables with class needsTreeSortLinks and make them sortable
    if (!document.getElementsByTagName) return;
    tbls = document.getElementsByTagName("table");
    for (ti=0;ti<tbls.length;ti++) {
        thisTbl = tbls[ti];
        if (((' '+thisTbl.className+' ').indexOf("needsTreeSortLinks") != -1) && (thisTbl.id)) {
            evts_makeSortable(thisTbl);
        }
    }
}

function evts_makeSortable(table) {
    // find the "flat view" hyperlink corresponding to this table, or abort.
    var flatLink = document.getElementById(table.id + 'styleflat');
    if (!flatLink) return;
    var href = flatLink.href;
    var questPos = href.indexOf('?');
    if (questPos == -1) return;
    href = href.substr(0,questPos)+
        '?'+table.id+'_initialSort=###&'+href.substr(questPos+1);

    if (table.rows && table.rows.length > 0) {
        var firstRow = table.rows[0];
    }
    if (!firstRow) return;

    // We have a first row: assume it's the header, and make its contents clickable links
    for (var i=0;i<firstRow.cells.length;i++) {
        var cell = firstRow.cells[i];
        var txt = ts_getInnerText(cell);
        var cellHref = href.replace(/###/, ''+i);
        cell.innerHTML = '<a href="' + cellHref + '" class="sortheader">' +
        txt+'<span class="sortarrow"></span></a>';
    }
}

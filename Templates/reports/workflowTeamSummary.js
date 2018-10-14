
function toggleWorkflowSelector(elem) {
    var div = getWorkflowTeamSummaryDiv(elem);
    var filter = div.getElementsByClassName("workflowSelector")[0];
    if (Element.hasClassName(div, "expanded")) {
        Element.removeClassName(div, "expanded");
        Element.addClassName(div, "collapsed");
        Effect.BlindUp(filter, { duration: 0.5 });
    } else {
        Element.removeClassName(div, "collapsed");
        Element.addClassName(div, "expanded");
        Effect.BlindDown(filter, { duration: 0.5 });
    }
    return false;
}

function showSelectedWorkflow(elem) {
    var div = getWorkflowTeamSummaryDiv(elem);
    var content = div.getElementsByClassName("workflowContent")[0];
    content.innerHTML = "<h2 class='workflowTitle'>" + elem.innerHTML
        + "<img style='margin-left:10px' src='/Images/loading-16.gif'></h2>";
    var url = elem.href + "&includable";
    new Ajax.Updater(content, url);
    return false;
}

function getWorkflowTeamSummaryDiv(elem) {
    while (!Element.hasClassName(elem, "workflowTeamSummary") && elem.parentNode) {
        elem = elem.parentNode;
    }
    return elem;
}

function phaseChange(elem) {
    updatePhase(elem);
    updateTopArrow();
}

function updatePhase(elem) {
    var excludeFlag = (elem.value == "" ? "excluded" : "");
    var origVal = elem.parentNode.getElementsByTagName("input")[2].value;
    var modFlag = (elem.value == origVal ? "" : " modified");
    elem.parentNode.parentNode.className =  excludeFlag + modFlag;
}

function updateTopArrow() {
    var newClass = (oneMappingPresent() ? "" : "empty");
    document.getElementById("header").className = newClass;
}

function oneMappingPresent() {
    var phases = document.getElementsByTagName("select");
    for (var j = 0; j < phases.length; j++) {
    	if (phases[j].value != "") return true;
    }
    return false;
}

function matchAllPhases() {
    var options = document.getElementsByTagName("option");
    for (var j = 0; j < options.length; j++) {
    	if (options[j].className == "nameMatch") {
    	    options[j].parentNode.value = options[j].value;
    	    updatePhase(options[j].parentNode);
    	}
    }
    updateTopArrow();
}

function clearAllPhases() {
    var phases = document.getElementsByTagName("select");
    for (var j = 0; j < phases.length; j++) {
    	phases[j].value = "";
    	updatePhase(phases[j]);
    }
    updateTopArrow();
}

function revertAllPhases() {
    document.getElementsByTagName("form")[0].reset();
    var phases = document.getElementsByTagName("select");
    for (var j = 0; j < phases.length; j++) {
    	updatePhase(phases[j]);
    }
    updateTopArrow();
}

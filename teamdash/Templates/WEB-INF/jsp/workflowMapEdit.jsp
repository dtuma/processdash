<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
<title><c:out value="${resources['Edit.Title']}"/> - <c:out value="${editWorkflow.process}"/></title>
<link rel="stylesheet" type="text/css" href="workflowMapEdit.css">
<script type="text/javascript">
function phaseChange(elem) {
    var excludeFlag = (elem.value == "" ? "excluded" : "");
    var origVal = elem.parentNode.getElementsByTagName("input")[0].value;
    var modFlag = (elem.value == origVal ? "" : " modified");
    elem.parentNode.parentNode.className =  excludeFlag + modFlag;
}
function clearAllPhases() {
    var phases = document.getElementsByTagName("select");
    for (var j = 0; j < phases.length; j++) {
    	phases[j].value = "";
    	phaseChange(phases[j]);
    }
}
function revertAllPhases() {
    document.getElementsByTagName("form")[0].reset();
    var phases = document.getElementsByTagName("select");
    for (var j = 0; j < phases.length; j++) {
        phaseChange(phases[j]);
    }
}
</script>
</head>
<c:set var="bodyClass" value="${param.focus == 'edit' ? 'fwd' : 'hist'}"/>
<body class="${bodyClass}">

<h1><c:out value="${resources['Edit.Title']}"/></h1>

<form action="workflowMap" method="post">
<input type="hidden" name="edit" value="${param.edit}" />
<input type="hidden" name="target" value="${param.target}" />
<input type="hidden" name="focus" value="${param.focus}" />

<table border="0" cellpadding="0" cellspacing="0">

<tr>
<td colspan="3" class="edit workflow"><div class="workflow">
  <div class="projectName">&laquo;&nbsp;<c:out
          value="${editWorkflow.project}"/>&nbsp;&raquo;</div>
  <c:out value="${editWorkflow.process}"/>
</div>
</td>

<td class="spacer">
  <div class="horizontalLine"><div class="arrow"></div></div>
</td>

<td colspan="2" class="target workflow"><div class="workflow">
  <div class="projectName">&laquo;&nbsp;<c:out
          value="${targetWorkflow.project}"/>&nbsp;&raquo;</div>
  <c:out value="${targetWorkflow.process}"/>
</div>
</td>
</tr>

<tr>
<td class="hierIndent"></td>
<td class="hierLine"></td>
<td class="description">
   <div><c:out value="${resources['Edit.Import_Description']}"/></div>
</td>
<td class="spacer"></td>
<td class="mapLine"></td>
<td class="description">
   <div title="${resources['Edit.Export_Tooltip']}"><c:out
        value="${resources['Edit.Export_Description']}"/></div>
</td>
</tr>

<c:forEach var="editPhase" items="${editWorkflow.phases}" varStatus="stat">
<c:if test="${!stat.first}">
  <tr class="spacerRow"><td class="hierIndent"></td></tr>
</c:if>

<c:set var="phaseClass" value="${(empty editPhase.mapsTo) ? 'excluded' : ''}" />
<tr class="${phaseClass}">
<td class="hierIndent"></td>
<td class="hierLine"><div class="hierLine"><c:if
    test="${stat.last}"><div class="hierLastLine"></div></c:if></div></td>
<td class="edit phase">
  <c:out value="${editPhase.name}"/>
  <input type="hidden" name="phase" value="${stat.index}" />
  <input type="hidden" name="phase${stat.index}id" value="${editPhase.id}" />
</td>

<td class="spacer">
  <div class="horizontalLine"></div>
</td>
<td class="mapLine">
  <div class="horizontalLine"><div class="arrow"></div></div>
</td>

<td class="target phase">
  <div class="modWrapper"><div class="modFlag"
       title="${resources['Edit.Modified_Tooltip']}"></div></div>
  <select name="phase${stat.index}mapsTo" onchange="phaseChange(this)">
    <option value=""><c:out value="${resources['Edit.No_Phase']}"/></option>
    <c:forEach var="targetPhase" items="${targetWorkflow.phases}">
    <c:set var="sel"><c:if test="${targetPhase.id == editPhase.mapsTo}"
            >selected="selected"</c:if></c:set>
    <option value="${targetPhase.id}" ${sel}><c:out
            value="${targetPhase.name}"/></option>
    </c:forEach>
  </select>
  <input type="hidden" name="phase${stat.index}origMapsTo" value="${editPhase.mapsTo}">
</td>

</tr>
</c:forEach>

<tr class="buttons">
<td colspan="2"></td>
<td>
  <input type="button" value="${resources['Edit.Clear']}" onclick="clearAllPhases()">
  <input type="button" value="${resources['Edit.Revert']}" onclick="revertAllPhases()">
</td>
<td colspan="2"></td>
<td>
  <input type="submit" name="save" value="${resources['Save']}">
  <input type="submit" name="cancel" value="${resources['Cancel']}">
</td>
</tr>

</table>

</form>
</body>
</html>

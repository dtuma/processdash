<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<html>
<head>
<title>${resources.html['Workflow.Analysis.Title']} - <c:out value="${hist.workflowName}"/></title>
<link rel="stylesheet" type="text/css" href="/lib/jacs.css">
<script type="text/javascript" src="/lib/jacs.js"></script>
<script type="text/javascript" src="/lib/prototype.js"></script>
<link rel="stylesheet" type="text/css" href="/reports/workflowToDateFilter.css">
<script type="text/javascript" src="/reports/workflowToDateFilter.js"></script>
</head>
<body>

<h1>${resources.html['Workflow.Analysis.Title']} - <c:out value="${hist.workflowName}"/></h1>

<h2>${resources.html['Title']}</h2>

<form method="post" action="workflowToDate" onkeypress="return event.keyCode != 13;">
<input type="hidden" name="page" value="Filter" />
<input type="hidden" name="lastpage"  value="${param.lastpage}" />
<input type="hidden" name="workflow"  value="${param.workflow}" />

<p>${resources.html['Prompt']}</p>

<p class="submitFilter">
<input type="submit" name="apply" value="${resources.html['Apply']}"/>
<input type="submit" name="remove" value="${resources.html['Remove']}"/>
</p>

<table id="data">
<tr id="filterRow">

<td id="projFilter">
<a class="showFilter" href="#">${resources.html['Project.Header']}</a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper left"><div class="filterContent">
<input type="hidden" name="filterID" value="proj"/>
<input type="hidden" name="projEnabled" value="${filt.projEnabled}"/>
${resources.html['Project.Prompt']}
<div class="indent"><div class="indent">
<input type="radio" name="projLogic" value="include" ${
    filt.projLogic == 'include' ? 'checked="checked"' : ''
    }/>&nbsp;${resources.html['Included']}
<input type="radio" name="projLogic" value="exclude" ${
    filt.projLogic == 'include' ? '' : 'checked="checked"'
    }/>&nbsp;${resources.html['Excluded']}
</div></div>
<c:forEach var="proj" items="${projects}">
<div class="indent">
<input type="checkbox" name="projVal" value="${proj.key}" ${
    filt['projVal'.concat(proj.key)]}/>&nbsp;<c:out value="${proj.value}" />
</div>
</c:forEach>
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>

<td id="taskFilter">
<a class="showFilter" href="#">${resources.html['Task.Header']}</a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper left"><div class="filterContent">
<input type="hidden" name="filterID" value="task"/>
<input type="hidden" name="taskEnabled" value="${filt.taskEnabled}"/>
${resources.html['Task.Prompt']}
<div class="indent"><table>
<tr>
<td style="text-align:right">${resources.html['Task.Include']}</td>
<td><input type="text" name="taskInclude" size="40"
    value="<c:out value='${filt.taskInclude}'/>"
    title="${resources.html['Task.Tooltip']}"/></td>
<td><a href="#" class="clearButton">&times;</a></td>
</tr>
<tr>
<td style="text-align:right">${resources.html['Task.Exclude']}</td>
<td><input type="text" name="taskExclude" size="40" 
    value="<c:out value='${filt.taskExclude}'/>"
    title="${resources.html['Task.Tooltip']}"/></td>
<td><a href="#" class="clearButton">&times;</a></td>
</tr>
</table></div>
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>

<td id="dateFilter">
<a class="showFilter" href="#">${resources.html['Date.Header']}</a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper right"><div class="filterContent">
<input type="hidden" name="filterID" value="date"/>
<input type="hidden" name="dateEnabled" value="${filt.dateEnabled}"/>
${resources.html['Date.Prompt']}
<div class="indent"><table>
<tr>
<td style="text-align:right">${resources.html['Date.After']}</td>
<td><input type="text" name="dateAfter" value="${filt.dateAfter}" size="20"
     onfocus="JACS.show(this,event);"/></td>
<td><a href="#" class="clearButton">&times;</a></td>
</tr>
<tr>
<td style="text-align:right">${resources.html['Date.Before']}</td>
<td><input type="text" name="dateBefore" value="${filt.dateBefore}" size="20"
     onfocus="JACS.show(this,event);"/></td>
<td><a href="#" class="clearButton">&times;</a></td>
</tr>
</table></div>
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>

<td id="timeFilter">
<a class="showFilter" href="#">${resources.html['Time.Header']}</a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper right"><div class="filterContent">
<input type="hidden" name="filterID" value="time"/>
<input type="hidden" name="timeEnabled" value="${filt.timeEnabled}"/>
${resources.html['Time.Prompt']}
<div class="indent"><table>
<tr>
<td style="text-align:right">${resources.html['At_Least']}</td>
<td><div class="unitsField">
  <div class="unitsLabel">${resources.html['Hours']}</div>
  <input type="text" name="timeMin" value="${filt.timeMin}" size="20"/>
</div></td>
<td><a href="#" class="clearButton">&times;</a></td>
</tr>
<tr>
<td style="text-align:right">${resources.html['Less_Than']}</td>
<td><div class="unitsField">
  <div class="unitsLabel">${resources.html['Hours']}</div>
  <input type="text" name="timeMax" value="${filt.timeMax}" size="20"/>
</div></td>
<td><a href="#" class="clearButton">&times;</a></td>
</tr>
</table></div>
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>

<c:forEach var="units" items="${sizeUnits}">
<c:set var="sizeID" value="size${units.key}" />
<td id="${sizeID}Filter">
<a class="showFilter" href="#"><c:out value="${units.value}" /></a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper right"><div class="filterContent">
<input type="hidden" name="filterID" value="${sizeID}"/>
<input type="hidden" name="${sizeID}Enabled" value="${filt[sizeID.concat('Enabled')]}"/>
<input type="hidden" name="${sizeID}Units" value="<c:out value='${units.value}'/>"/>
${resources.html['Size.Prompt']}
<div class="indent"><table>
<tr>
<td style="text-align:right">${resources.html['At_Least']}</td>
<td><div class="unitsField">
  <div class="unitsLabel"><c:out value="${units.value}" /></div>
  <input type="text" name="${sizeID}Min" value="${filt[sizeID.concat('Min')]}" size="20"/>
</div></td>
<td><a href="#" class="clearButton">&times;</a></td>
</tr>
<tr>
<td style="text-align:right">${resources.html['Less_Than']}</td>
<td><div class="unitsField">
  <div class="unitsLabel"><c:out value="${units.value}" /></div>
  <input type="text" name="${sizeID}Max" value="${filt[sizeID.concat('Max')]}" size="20"/>
</div></td>
<td><a href="#" class="clearButton">&times;</a></td>
</tr>
</table></div>
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>
</c:forEach>

<td id="labelFilter">
<a class="showFilter" href="#">${resources.html['Labels.Header']}</a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper right"><div class="filterContent">
<input type="hidden" name="filterID" value="label"/>
<input type="hidden" name="labelEnabled" value="${filt.labelEnabled}"/>
${resources.html['Labels.Prompt']}
<div class="indent"><div class="indent">
<input type="radio" name="labelLogic" value="include" ${
    filt.labelLogic == 'include' ? 'checked="checked"' : ''
    }/>&nbsp;${resources.html['Included']}
<input type="radio" name="labelLogic" value="exclude" ${
    filt.labelLogic == 'include' ? '' : 'checked="checked"'
    }/>&nbsp;${resources.html['Excluded']}
</div></div>
<c:forEach var="label" items="${labels}">
<div class="indent">
<input type="checkbox" name="labelVal" value="<c:out value='${label}'/>" ${
    filt['labelVal'.concat(label)]}/>&nbsp;<c:out value="${label}" />
</div>
</c:forEach>
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>

<td id="outlierFilter">
<a class="showFilter" href="#">${resources.html['Outlier.Header']}</a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper right"><div class="filterContent">
<input type="hidden" name="filterID" value="outlier"/>
<input type="hidden" name="outlierEnabled"/>
${resources.html['Outlier.Prompt']}
<div class="filterButtons">
<input type="button" name="hidden" value="-" style="display:none"/>
<input type="button" name="hidden" value="-" style="display:none"/>
<input type="button" name="ok" value="${resources.html['OK']}"/>
</div></div></div></div></td>

</tr>

<c:forEach var="e" varStatus="s" items="${hist.enactments}">
<c:set var="eid" value="e${s.index}"/>
<tr id="${eid}">

<td id="${eid}.proj"><c:out value="${e.projectName}"/>
    <input type="hidden" name="val" value="${e.projectID}"/></td>

<td id="${eid}.task"><c:out value="${e.rootName}"/></td>

<td id="${eid}.date" class="right"><fmt:formatDate value="${e.completed}"
    type="DATE" dateStyle="SHORT" /><c:set var="dateKey"><fmt:formatDate
    value="${e.completed}" pattern="yyyy-MM-dd"/></c:set>
    <input type="hidden" name="val" value="${dateKey}"/></td>

<td id="${eid}.time" class="right"><fmt:formatNumber value="${e.hours}"
    maxFractionDigits="1"/></td>

<c:forEach var="units" items="${sizeUnits}">
  <td id="${eid}.size${units.key}" class="right"><c:if test="${e.sizes[units.value] > 0}">
    <fmt:formatNumber value="${e.sizes[units.value]}" maxFractionDigits="1"/></c:if>
  </td>
</c:forEach>

<td id="${eid}.label">
<c:forEach var="label" varStatus="stat" items="${e.labels}">
  <c:out value="${label}" /><c:if test="${!stat.last}">, </c:if>
  <input type="hidden" name="val" value="<c:out value='${label}'/>"/>
</c:forEach>
</td>

<td id="${eid}.outlier" class="center">
<input type="checkbox" name="outlierVal" value="${e.rootID}" ${
    empty filt['outlierVal'.concat(e.rootID)] ? '' : 'checked="checked"'}/>
</td>

</tr>
</c:forEach>
</table>

</form>

<c:set var="commaTest"><fmt:formatNumber value="${1.5}"/></c:set>
<c:if test="${commaTest == '1,5'}">
<script type="text/javascript">WFilt.commaNum = true;</script>
</c:if>
<script type="text/javascript">
  WFilt.init();
</script>
</body>
</html>

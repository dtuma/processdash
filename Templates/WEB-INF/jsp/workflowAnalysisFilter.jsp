<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<html>
<head>
<title>${resources.html['Workflow.Analysis.Title']} - <c:out value="${hist.workflowName}"/></title>
<link rel="stylesheet" type="text/css" href="/reports/workflowToDateFilter.css">
<script type="text/javascript" src="/lib/prototype.js"></script>
<script type="text/javascript" src="/reports/workflowToDateFilter.js"></script>
</head>
<body>

<h1>${resources.html['Workflow.Analysis.Title']} - <c:out value="${hist.workflowName}"/></h1>

<h2>${resources.html['Title']}</h2>

<form>

<table id="data">
<tr id="filterRow">

<td id="projFilter"><a href="#">${resources.html['Project.Header']}</a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper left"><div class="filterContent">
<input type="hidden" name="filterID" value="proj"/>
<input type="hidden" name="projEnabled"/>
${resources.html['Project.Prompt']}
<div class="indent"><div class="indent">
<input type="radio" name="projLogic" value="include"/>&nbsp;${resources.html['Included']}
<input type="radio" name="projLogic" value="exclude" checked="checked"/>&nbsp;${resources.html['Excluded']}
</div></div>
<c:forEach var="proj" items="${projects}">
<div class="indent">
<input type="checkbox" name="projVal" value="${proj.key}"/>&nbsp;<c:out
       value="${proj.value}" />
</div>
</c:forEach>
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>

<td><a href="#">${resources.html['Task']}</a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper left"><div class="filterContent">
<input type="hidden" name="filterID" value="task"/>
<input type="hidden" name="taskEnabled"/>
Fixme: add real content for editing this filter
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>

<td><a href="#">${resources.html['Completed']}</a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper right"><div class="filterContent">
<input type="hidden" name="filterID" value="date"/>
<input type="hidden" name="dateEnabled"/>
Fixme: add real content for editing this filter
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>

<td><a href="#">${resources.html['Time']}</a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper right"><div class="filterContent">
<input type="hidden" name="filterID" value="time"/>
<input type="hidden" name="timeEnabled"/>
Fixme: add real content for editing this filter
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>

<c:forEach var="units" items="${sizeUnits}">
<td><a href="#"><c:out value="${units}" /></a>
<div class="filter"><div class="filterSpacer"></div>
<div class="filterContentWrapper right"><div class="filterContent">
<input type="hidden" name="filterID" value="size"/>
<input type="hidden" name="sizeEnabled"/>
Fixme: add real content for editing this filter
<div class="filterButtons">
<input type="button" name="ok" value="${resources.html['OK']}"/>
<input type="button" name="off" class="off" value="${resources.html['Off']}"/>
<input type="button" name="cancel" class="cancel" value="${resources.html['Cancel']}"/>
</div></div></div></div></td>
</c:forEach>
</tr>

<c:forEach var="e" varStatus="s" items="${hist.enactments}">
<c:set var="eid" value="e${s.index}"/>
<tr id="${eid}">

<td id="${eid}.proj"><c:out value="${e.projectName}"/>
    <input type="hidden" name="val" value="${e.projectID}"/></td>

<td><c:out value="${e.rootName}"/></td>

<td><fmt:formatDate value="${e.completed}" type="DATE" dateStyle="SHORT" /></td>

<td><fmt:formatNumber value="${e.hours}" maxFractionDigits="1"/></td>

<c:forEach var="units" items="${sizeUnits}">
  <td><c:if test="${e.sizes[units] > 0}">
    <fmt:formatNumber value="${e.sizes[units]}" maxFractionDigits="1"/></c:if>
    </td>
</c:forEach>

</tr>
</c:forEach>
</table>

</form>

<script type="text/javascript">
  WFilt.init();
</script>
</body>
</html>

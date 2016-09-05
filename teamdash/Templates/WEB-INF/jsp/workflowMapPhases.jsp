<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
<title><c:out value="${resources[editing ? 'Edit.Title' : 'View.Title']}"/>
     - <c:out value="${sourceWorkflow.process}"/></title>
<link rel="stylesheet" type="text/css" href="workflowMapPhases.css">
<script type="text/javascript" src="workflowMapPhases.js"></script>
</head>
<c:set var="bodyClass" value="${param.focus == 'source' ? 'fwd' : 'hist'}"/>
<body class="${bodyClass}">

<h1><c:out value="${resources[editing ? 'Edit.Title' : 'View.Title']}"/></h1>

<form action="workflowMap" method="post">
<input type="hidden" name="source" value="${param.source}" />
<input type="hidden" name="target" value="${param.target}" />
<input type="hidden" name="focus"  value="${param.focus}" />
<input type="hidden" name="adding" value="${param.adding}" />

<table border="0" cellpadding="0" cellspacing="0">

<tr id="header" class="${emptyMapping ? 'empty' : ''}">
<td colspan="3" class="source workflow"><div class="workflow">
  <div class="projectName">&laquo;&nbsp;<c:out
          value="${sourceWorkflow.project}"/>&nbsp;&raquo;</div>
  <c:choose>
  <c:when test="${editing}"><c:out value="${sourceWorkflow.process}"/></c:when>
  <c:otherwise>
  <a title="${resources.html['View.Name_Link_Tooltip_FMT'][sourceWorkflow.process]}"
     href="workflowMap?list=${sourceWorkflow.id}"><c:out value="${sourceWorkflow.process}"/></a>
  </c:otherwise>
  </c:choose>
</div>
</td>

<td class="spacer">
  <div class="horizontalLine"><div class="arrow"></div></div>
</td>

<td colspan="2" class="target workflow"><div class="workflow">
  <div class="projectName">&laquo;&nbsp;<c:out
          value="${targetWorkflow.project}"/>&nbsp;&raquo;</div>
  <c:choose>
  <c:when test="${editing}"><c:out value="${targetWorkflow.process}"/></c:when>
  <c:otherwise>
  <a title="${resources.html['View.Name_Link_Tooltip_FMT'][targetWorkflow.process]}"
     href="workflowMap?list=${targetWorkflow.id}"><c:out value="${targetWorkflow.process}"/></a>
  </c:otherwise>
  </c:choose>
</div>
</td>
</tr>

<tr>
<td class="hierIndent"></td>
<td class="hierLine"></td>
<td class="description">
   <div><c:out value="${resources['View.Import_Description']}"/></div>
</td>
<td class="spacer"></td>
<td class="mapLine"></td>
<td class="description">
   <c:set var="expResKey">${editing ? 'Edit' : 'View'}.Export_Description</c:set>
   <div title="${resources.html['View.Export_Tooltip']}"><c:out
        value="${resources[expResKey]}"/></div>
</td>
</tr>

<c:forEach var="sourcePhase" items="${sourceWorkflow.phases}" varStatus="stat">
<c:if test="${!stat.first}">
  <tr class="spacerRow"><td class="hierIndent"></td></tr>
</c:if>

<c:set var="phaseClass" value="${(empty sourcePhase.mapsTo) ? 'excluded' : ''}" />
<tr class="${phaseClass}">
<td class="hierIndent"></td>
<td class="hierLine"><div class="hierLine"><c:if
    test="${stat.last}"><div class="hierLastLine"></div></c:if></div></td>
<td class="source phase">
  <c:out value="${sourcePhase.name}"/>
</td>

<td class="spacer">
  <div class="horizontalLine"></div>
</td>
<td class="mapLine">
  <div class="horizontalLine"><div class="arrow"></div></div>
</td>

<td class="target phase"><c:choose>

<c:when test="${editing}">
  <div class="modWrapper"><div class="modFlag"
       title="${resources.html['Edit.Modified_Tooltip']}"></div></div>
  <input type="hidden" name="phase" value="${stat.index}" />
  <input type="hidden" name="phase${stat.index}id" value="${sourcePhase.id}" />
  <select name="phase${stat.index}mapsTo" onchange="phaseChange(this)">
    <option value=""><c:out value="${resources['Edit.No_Phase']}"/></option>
    <c:forEach var="targetPhase" items="${targetWorkflow.phases}">
    <c:set var="sel"><c:if test="${targetPhase.id == sourcePhase.mapsTo}"
            >selected="selected"</c:if></c:set>
    <c:set var="def"><c:if test="${targetPhase.id == sourcePhase.nameMatch}"
            >class="nameMatch"</c:if></c:set>
    <option value="${targetPhase.id}" ${sel} ${def}><c:out
            value="${targetPhase.name}"/></option>
    </c:forEach>
  </select>
  <input type="hidden" name="phase${stat.index}origMapsTo" value="${sourcePhase.mapsTo}">
</c:when>

<c:when test="${!empty sourcePhase.mapsTo}">
  <c:forEach var="targetPhase" items="${targetWorkflow.phases}">
    <c:if test="${targetPhase.id == sourcePhase.mapsTo}">
      <c:out value="${targetPhase.name}"/>
    </c:if>
  </c:forEach>
</c:when>

<c:otherwise>
  <c:out value="${resources['View.No_Phase']}" />
</c:otherwise>

</c:choose></td>

</tr>
</c:forEach>

<tr class="buttons">
<td colspan="2"></td>
<c:choose>
  <c:when test="${editing}">
  <td colspan="${sourceWorkflow.hasNameMatch ? 2 : 1}">
  <c:if test="${sourceWorkflow.hasNameMatch}">
    <input type="button" value="${resources.html['Edit.Match']}" onclick="matchAllPhases()">
  </c:if>
  <input type="button" value="${resources.html['Edit.Clear']}" onclick="clearAllPhases()">
  <input type="button" value="${resources.html['Edit.Revert']}" onclick="revertAllPhases()">
  </td>
  <c:if test="${!sourceWorkflow.hasNameMatch}"><td></td></c:if>
  </c:when>

  <c:when test="${editingAllowed}">
  <td>
  <input type="submit" name="edit" value="${resources.html['View.Edit_Mappings']}">
  </td><td></td>
  </c:when>
</c:choose>
<td></td>
<td>
<c:choose>
  <c:when test="${editing}">
  <input type="submit" name="save" value="${resources.html['Save']}">
  <input type="submit" name="cancel" value="${resources.html['Cancel']}">
  </c:when>

  <c:otherwise>
  <input type="submit" name="reverse" value="${resources.html['View.Reverse']}">
  </c:otherwise>
</c:choose>
</td>
</tr>

</table>

</form>
</body>
</html>

<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
<title><c:out value="${resources['Add.Title']}"/>
     - <c:out value="${workflow.process}"/></title>
<link rel="stylesheet" type="text/css" href="workflowMapAdd.css">
</head>
<c:set var="bodyClass" value="${importing ? 'hist' : 'fwd' }"/>
<body class="${bodyClass}">

<h1><c:out value="${resources['Add.Title']}"/></h1>

<table border="0" cellpadding="0" cellspacing="0">

<tr>
<td class="source edge"></td>
<td class="source workflow top">
  <c:choose>
  <c:when test="${importing}">
  <div class="question">???</div>
  </c:when>
  <c:otherwise>
  <div class="projectName">&laquo;&nbsp;<c:out
          value="${workflow.project}"/>&nbsp;&raquo;</div>
  <a href="workflowMap?list=${workflow.id}"><c:out value="${workflow.process}"/></a>
  </c:otherwise>
  </c:choose>
</td>
<td class="source edge"></td>

<td class="spacer">
  <div class="horizontalLine"><div class="arrow"></div></div>
</td>

<td class="target edge"></td>
<td class="target workflow top">
  <c:choose>
  <c:when test="${!importing}">
  <div class="question">???</div>
  </c:when>
  <c:otherwise>
  <div class="projectName">&laquo;&nbsp;<c:out
          value="${workflow.project}"/>&nbsp;&raquo;</div>
  <a href="workflowMap?list=${workflow.id}"><c:out value="${workflow.process}"/></a>
  </c:otherwise>
  </c:choose>
</td>
<td class="target edge"></td>
</tr>

</table>

<c:choose>
<c:when test="${importing}">
  <c:set var="resKey" value="Add.Import_Prompt_FMT" />
  <c:set var="add" value="source" />
  <c:set var="wkflw" value="target" />
</c:when>
<c:otherwise>
  <c:set var="resKey" value="Add.Export_Prompt_FMT" />
  <c:set var="add" value="target" />
  <c:set var="wkflw" value="source" />
</c:otherwise>
</c:choose>

<div class="prompt"><c:out value="${resources[resKey][workflow.process]}"/></div>

<table border="0" cellpadding="0" cellspacing="6">

<c:forEach var="projectWorkflowSet" items="${allWorkflows}" >
  <tr>
  <td colspan="2" class="projectName">&laquo;&nbsp;<c:out
      value="${projectWorkflowSet.key}"/>&nbsp;&raquo;</td>
  </tr>

  <c:forEach var="addWorkflow" items="${projectWorkflowSet.value}">
    <tr>
    <td class="indent"></td>
    <td class="workflow add"><div class="workflow">
        <a href="workflowMap?${wkflw}=${workflow.id}&amp;${add}=${addWorkflow.id}&amp;focus=${wkflw}&amp;edit=t&amp;adding=t"><c:out
                value="${addWorkflow.process}"/></a>
    </div></td>
    </tr>
  </c:forEach>
</c:forEach>

<tr class="buttons">
<td class="indent"></td>
<td>
<form action="workflowMap" method="post">
<input type="hidden" name="source" value="${workflow.id}" />
<input type="hidden" name="focus" value="source" />
<input type="hidden" name="adding" value="t" />
<input type="submit" name="cancel" value="${resources.html.Cancel}"/>
</form>
</td>
</tr>

</table>

</body>
</html>

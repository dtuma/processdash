<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<html>
<head>
<title>${resources.html['Workflow.Analysis.Title']} - <c:out value="${hist.workflowName}"/></title>
<link rel="stylesheet" type="text/css" href="workflowToDateFilter.css">
</head>
<body>

<h1>${resources.html['Workflow.Analysis.Title']} - <c:out value="${hist.workflowName}"/></h1>

<h2>${resources.html['Title']}</h2>

<table border>
<tr>
<th>${resources.html['Project']}</th>
<th>${resources.html['Task']}</th>
<th>${resources.html['Completed']}</th>
<th>${resources.html['Time']}</th>
<c:forEach var="units" items="${sizeUnits}">
  <th><c:out value="${units}" /></th>
</c:forEach>
</tr>

<c:forEach var="e" items="${hist.enactments}">
<tr>
<td><c:out value="${e.projectName}"/></td>
<td><c:out value="${e.rootName}"/></td>
<td><fmt:formatDate value="${e.completed}" type="DATE" dateStyle="SHORT" /></td>
<td><fmt:formatNumber value="${e.hours}" maxFractionDigits="1"/></td>
<c:set var="size" value="${e.sizes}"/>
<c:forEach var="units" items="${sizeUnits}">
  <td><c:if test="${size[units] > 0}">
    <fmt:formatNumber value="${size[units]}" maxFractionDigits="1"/></c:if></td>
</c:forEach>
</tr>
</c:forEach>

</body>
</html>

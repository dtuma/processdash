<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<html>
<head>
<title><fmt:message key="Title" /></title>
</head>
<body>
<h3><fmt:message key="Title" /></h3>

<c:choose>

<c:when test="${empty reviews}">
<p><i><fmt:message key="None_Found" /></i></p>
</c:when>

<c:otherwise>
<table border>
<tr>
<th rowspan="2"><fmt:message key="Columns.Task" /></th>
<th rowspan="2"><fmt:message key="Columns.Who" /></th>
<th rowspan="2"><fmt:message key="Columns.Date" /></th>
<th colspan="3"><fmt:message key="Columns.Review_Time" /></th>
<th colspan="2"><fmt:message key="Columns.Defects_Found" /></th>
</tr>
<tr>
<th><fmt:message key="Plan" /></th>
<th><fmt:message key="Actual" /></th>
<th><fmt:message key="Columns.Ratio" /></th>
<th><fmt:message key="Columns.Count" /></th>
<th><fmt:message key="Columns.Per_Hour" /></th>
</tr>

<c:forEach var="review" items="${reviews}">
<tr>
<td><c:out value="${review.taskName}" /></td>
<td><c:out value="${review.personName}" /></td>
<td><fmt:formatDate value="${review.completionDate}"
     type="DATE" dateStyle="SHORT" /></td>
<td><c:out value="${review.planTime}" /></td>
<td><c:out value="${review.actualTime}" /></td>
<td><fmt:formatNumber value="${review.timeRatio}" type="PERCENT"/></td>
<td><c:out value="${review.numDefects}" /></td>
<td><c:out value="${review.defectsPerHour}" /></td>
</tr>
</c:forEach>

</table>
</c:otherwise>
</c:choose>
</body>
</html>

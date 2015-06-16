<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
<title>WBS Change History - <c:out value="${pdash.projectPath}"/></title>
</head>
<body>

<h1>WBS Change History - <c:out value="${pdash.projectPath}"/></h1>

<c:choose>

<c:when test="${!empty changes}">
<c:set var="dateHeader" value=""/>
<c:set var="timeHeader" value=""/>

<c:forEach var="change" items="${changes}">

<c:if test="${change.displayDate != dateHeader}">
    <c:if test="${!empty timeHeader}"></ul></c:if>
    <c:set var="timeHeader" value=""/>
    <c:set var="dateHeader" value="${change.displayDate}"/>
    <h2><c:out value="${dateHeader}"/></h2>
</c:if>

<c:if test="${change.displayTime != timeHeader || change.author != authorHeader}">
    <c:if test="${!empty timeHeader}"></ul></c:if>
    <c:set var="timeHeader" value="${change.displayTime}"/>
    <c:set var="authorHeader" value="${change.author}"/>
    <p><b><c:out value="${timeHeader}"/> - <c:out value="${authorHeader}"/></b></p>
    <ul>
</c:if>

<li><c:out value="${change.description}" /></li>
<c:set var="timestamp" value="${change.follupTimestamp}" />

</c:forEach>
</ul>

<c:if test="${!empty param.before}">
<a href="wbsChangeHistory">Newest changes...</a>
<c:if test="${!empty timestamp}"> | </c:if>
</c:if>
<c:if test="${!empty timestamp}">
<a href="wbsChangeHistory?before=${timestamp.time}">Older changes...</a>
</c:if>

</c:when>

<c:otherwise>
<p>No changes found.</p>
</c:otherwise>

</c:choose>

</body>
</html>

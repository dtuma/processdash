<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
<title>WBS Change History - <c:out value="${pdash.projectPath}"/></title>
<link rel="stylesheet" type="text/css" href="wbsChangeHistory.css">
<link rel="stylesheet" type="text/css" href="/lib/treetable.css">
<script type="text/javascript" src="/lib/treetable.js"></script>
</head>
<body>

<h1>WBS Change History - <c:out value="${pdash.projectPath}"/></h1>

<c:choose>

<c:when test="${!empty changes}">

<p>
<c:if test="${!empty param.before}">
    <a class="newestLink" href="wbsChangeHistory">Newest changes</a>
    <c:if test="${!empty followupTimestamp}"> | </c:if>
</c:if>
<c:if test="${!empty followupTimestamp}">
    <a href="wbsChangeHistory?before=${followupTimestamp.time}"
       class="olderLink">Older changes</a>
</c:if>
</p>

<c:set var="dateHeader" value=""/>
<c:set var="dateCtr" value="${0}"/>

<table class="changeHistory">

<c:forEach var="change" varStatus="changeStat" items="${changes}">

<c:if test="${change.displayDate != dateHeader}">
    <c:set var="dateHeader" value="${change.displayDate}"/>
    <c:set var="dateCtr" value="${dateCtr + 1}"/>
    <c:set var="timeHeader" value=""/>
    <c:set var="timeCtr" value="${0}"/>
    <tr id="d${dateCtr}" class="dateHeader"><td colspan="1">
        <div class="dateHeader"><c:out value="${dateHeader}"/></div>
    </td></tr>
</c:if>

<c:if test="${change.displayTime != timeHeader || change.author != authorHeader}">
    <c:set var="timeHeader" value="${change.displayTime}"/>
    <c:set var="timeCtr" value="${timeCtr + 1}"/>
    <c:set var="authorHeader" value="${change.author}"/>
    <tr id="d${dateCtr}-t${timeCtr}" class="timeHeader"><td colspan="1">
        <div class="timeHeader">
        <c:out value="${timeHeader}"/> - <c:out value="${authorHeader}"/>
    </div></td></tr>
</c:if>

<c:forEach var="row" items="${change.reportRows}">

    <tr id="d${dateCtr}-t${timeCtr}-c${changeStat.index}${row.expansionId}"
        class="tree-table-folder-${row.expanded ? 'open' : 'closed'}"
        ${row.visible ? '' : 'style="display:none"'}><td colspan="1">
    <div class="changeItem" style="margin-left: ${50 + 20 * row.indent}px;">

        <c:if test="${row.expandable && row.indent > 0}"><a
            class="treeTableFolder${empty row.icon ? '' : ' showOnHover shiftLeft'}"
            href="#" onclick="toggleRows(this); return false;">&nbsp;</a></c:if>

        <c:set var="tipStr">title="<c:out value="${row.iconTooltip}"/>"</c:set>
        <c:if test="${not empty row.icon}"><img src="${row.icon}.png"
            class="changeIcon" ${not empty row.iconTooltip ? tipStr : '' }></c:if>

        <c:out value="${row.html}" escapeXml="false"/>

    </div></td></tr>

</c:forEach>

</c:forEach>

</table>

<p>
<c:if test="${!empty param.before}">
    <a class="newestLink" href="wbsChangeHistory">Newest changes</a>
    <c:if test="${!empty followupTimestamp}"> | </c:if>
</c:if>
<c:if test="${!empty followupTimestamp}">
    <a href="wbsChangeHistory?before=${followupTimestamp.time}"
       class="olderLink">Older changes</a>
</c:if>
</p>

</c:when>

<c:otherwise>
<p>No changes found.</p>
</c:otherwise>

</c:choose>

</body>
</html>

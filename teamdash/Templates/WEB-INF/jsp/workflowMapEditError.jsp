<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
<title><c:out value="${resources['Edit.Error_Title']}"/> - <c:out
    value="${workflow.process}"/></title>
<style>
body {
    font-size: large;
}
div.error {
    background: #FFF2F2 url("/Images/warning-large.png") no-repeat 15px center;
    border: 1px solid #CC0000;
    color: #B20000;
    max-width: 800px;
    min-height: 48px;
    padding: 15px 15px 15px 100px;
}
pre {
    margin: 0.5em 0px 1em 2em;
}
p {
    max-width: 900px;
}
</style>
</head>
<body>

<h1><c:out value="${resources['Edit.Error_Title']}"/> - <c:out
    value="${workflow.process}"/></h1>

<div class="error">${errorMessageHtml}</div>

<p><c:out value="${resources['Edit.Error_Footer']}"/></p>

<p><input type="button"
    value="${resources.html['Edit.Error_Retry']}"
    onclick="location.reload(true)"></p>

</body>
</html>

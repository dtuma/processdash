
function dblesc(str) {
    str = escape(str);
    str = str.replace(/\//g, "%2F");
    str = str.replace(/\./g, "%2E");
    str = str.replace(/\+/g, "%2B");
    return str;
}


if (window.location.host.indexOf("localhost") == -1) {
    var url = dblesc(window.location.pathname + window.location.search);
    url = "../reports/form2html.class?uri=" + url;
    window.location.replace(url);
}

var compatSlashIndex = window.location.href.indexOf("/+/");

if (compatSlashIndex != -1) {
   var url = window.location.href.substring(0, compatSlashIndex+1) + window.location.href.substring(compatSlashIndex+2);
   window.location.replace(url);
}

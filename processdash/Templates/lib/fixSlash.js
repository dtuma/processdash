/*
 * For compatibility with certain Microsoft products (which DON'T follow
 * web standards), dashboard URLs must sometimes be encoded with "/+/"
 * instead of "//" separating the project path and the resource path. This
 * javascript is designed to detect the presence of the compatibility
 * slash, and retarget the web page to use the traditional double slash.
 */

var compatSlashIndex = window.location.href.indexOf("/+/");

if (compatSlashIndex != -1) {
    var url = window.location.href.substring(0, compatSlashIndex+1)
        + window.location.href.substring(compatSlashIndex+2);
    window.location.replace(url);
}

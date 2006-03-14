/*
 * Adapted from http://sstree.tigris.org/
 * Copyright jrobbins@tigris.org
 * Modified and distributed under the Apache License
 */

function toggleRows(elm) {
   var rows = document.getElementsByTagName("TR");
   setImage(elm, "folder-closed");
   var newDisplay = "none";
   var thisID = elm.parentNode.parentNode.parentNode.id + "-";
   // Are we expanding or contracting? If the first child is hidden, we expand
   for (var i = 0; i < rows.length; i++) {
      var r = rows[i];
      if (matchStart(r.id, thisID, true)) {
         if (r.style.display == "none") {
            if (document.all) newDisplay = "block"; //IE4+ specific code
            else newDisplay = "table-row"; //Netscape and Mozilla
            setImage(elm, "folder-open");
         }
         break;
      }
   }
 
   // When expanding, only expand one level.  Collapse all desendants.
   var matchDirectChildrenOnly = (newDisplay != "none");

   for (var j = 0; j < rows.length; j++) {
      var s = rows[j];
      if (matchStart(s.id, thisID, matchDirectChildrenOnly)) {
         s.style.display = newDisplay;
         var cell = lookForChild(s, "TD");
         var tier = lookForChild(cell, "DIV");
         var folder = lookForChild(tier, "A");
         setImage(folder, "folder-closed");
      }
   }
}

function lookForChild(elem, tagName) {
   if (elem == null) return null;
   var children = elem.getElementsByTagName(tagName);
   if (children == null || children.length == 0) return null;
   return children[0];
}

function setImage(elm, imgID) {
   var image = lookForChild(elm, "IMG");
   if (image != null)
	image.src = document.getElementById(imgID).src;
}

function matchStart(target, pattern, matchDirectChildrenOnly) {
   var pos = target.indexOf(pattern);
   if (pos != 0) return false;
   if (!matchDirectChildrenOnly) return true;
   if (target.slice(pos + pattern.length, target.length).indexOf("-") >= 0)
      return false;
   return true;
}

function collapseAllRows(belowDepth) {
   if (belowDepth < 1) belowDepth = 1;
   var rows = document.getElementsByTagName("TR");
   for (var j = 0; j < rows.length; j++) {
      var r = rows[j];
      if (r.id != null && r.id.split("-").length > belowDepth) {
        r.style.display = "none";    
      }
   }
}

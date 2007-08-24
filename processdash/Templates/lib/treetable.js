/*
 * Adapted from http://sstree.tigris.org/
 * Copyright jrobbins@tigris.org
 * Modified and distributed under the Apache License
 */

function toggleRows(elm) {
   var rows = document.getElementsByTagName("TR");
   var newDisplay = "none";
   var thisID = elm.parentNode.parentNode.parentNode.id + "-";
   var expandingOne = getExpandingOne(rows, thisID);  // Are we expanding one element ?
   var expandingAll = (elm.className == "treeTableExpandAll"); // Are we expanding all
                                                               //  elements ?	
   
   if (expandingOne || expandingAll) {
       if (document.all) newDisplay = "block"; //IE4+ specific code
	   else newDisplay = "table-row"; //Netscape and Mozilla
   }
 
   // When expanding, only expand one level.  Collapse all desendants, unless we
   //  are expanding all
   var matchDirectChildrenOnly = (newDisplay != "none") && !expandingAll;
   
   for (var j = 0; j < rows.length; j++) {
      var s = rows[j];
      if (matchStart(s.id, thisID, matchDirectChildrenOnly)) {
         s.style.display = newDisplay;
         var cell = lookForChild(s, "TD");
         var tier = lookForChild(cell, "DIV");
         var folder = lookForChild(tier, "A");
         
         // If we are expanding all, we want all folder's icon to be opened
         if (expandingAll)
         	setImage(folder, "tree-table-folder-open");
         else
            setImage(folder, "tree-table-folder-closed");
      }
   }
   
   // We finally set the correct image for the icon we have just clicked. If we are
   //  expanding all, we want to set the correct icon for the folder next to the
   //  "expand all" button.
   setFolderIconClicked(expandingOne, expandingAll, elm)
}

function getExpandingOne(rows, thisID) {
   // Are we expanding or contracting? If the first child is hidden, we expand.
   for (var i = 0; i < rows.length; i++) {
      var r = rows[i];
      if (matchStart(r.id, thisID, true)) {
         return (r.style.display == "none");
      }
   }
   
   // Should not normally get here
   return false;
}

function lookForChild(elem, tagName, childIndice) {
   // childIndice is an optional parameter
   if (typeof childIndice == "undefined") {
       childIndice = 0;
   }
   
   if (elem == null) return null;
   var children = elem.getElementsByTagName(tagName);
   if (children == null || children.length < childIndice) return null;
   return children[childIndice];
}

function setImage(elm, imgID) {
   var image = lookForChild(elm, "IMG");
   if (image == null) return;

   var allImages = document.getElementsByTagName("IMG");
   for (var i = 0;  i < allImages.length;  i++) {
      if (allImages[i].alt == imgID) {
         image.src = allImages[i].src;
         return;
      }     
   }
}

// Sets the correct image for the icon we have just clicked. If we are expanding
//  all, we want to set the correct icon for the folder next to the "expand all"
//  button.
function setFolderIconClicked(expandingOne, expandingAll, elm) {
   if (expandingAll) {
       var parentDiv = elm.parentNode;
       
       // The folder icon we want opened is the second children of the parent
       //  DIV (the child located at position 1)
       var folderToOpen = lookForChild(parentDiv, "A", 1);
       setImage(folderToOpen, "tree-table-folder-open");
   }
   else {
       if (expandingOne) setImage(elm, "tree-table-folder-open");
       else setImage(elm, "tree-table-folder-closed");
   }
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

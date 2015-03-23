var lastPopup = null;

function togglePopupInfo(elm) {
   var div = elm.parentNode.getElementsByTagName("DIV")[0];
   var table = div.childNodes[0];
   if (table.style.display == "block") {
      table.style.display = "none";
      lastPopup = null;
   } else { 
      if (lastPopup != null && lastPopup != table)
         lastPopup.style.display = "none";
      table.style.display = "block";
      lastPopup = table;
      if (table.offsetWidth > div.offsetLeft) {
         var cells = table.getElementsByTagName("TD");
         for (var i = 0; i < cells.length; i++) {
            cells[i].style.whiteSpace = "normal";
         }
         table.width = div.offsetLeft;
      }
   }
}

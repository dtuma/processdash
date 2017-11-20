
var PdashEV = {

   openCustomizeWindow: function(href) {
      var newWind = window.open (href, 'customize',
            'scrollbars=yes,dependent=yes,resizable=yes,width=420,height=720');
      newWind.focus();
      return false;
   },

   selectSingleChart: function (elem) {
      var newChartId = elem.value;
      if (!newChartId || newChartId == "" || newChartId == " ") return;

      var baseUrl = window.location.href.replace(/&showChart=[^&]+/, "");
      if (newChartId == "ALL") {
         window.location.href = baseUrl;
      } else {
         window.location.href = baseUrl + "&showChart=" + newChartId;
      }
   },

   toggleExpanded: function (elem) {
      elem = PdashEV.findExpandableElem($(elem));
      if (elem) {
         if (Element.hasClassName(elem, "expanded")) {
            Element.removeClassName(elem, "expanded");
            Element.addClassName(elem, "collapsed");
         } else {
            Element.removeClassName(elem, "collapsed");
            Element.addClassName(elem, "expanded");
         }
      }
   },

   findExpandableElem: function (elem) {
      if (!elem) return elem;
      if (Element.hasClassName(elem, "expanded")) { return elem; }
      if (Element.hasClassName(elem, "collapsed")) { return elem; }
      return PdashEV.findExpandableElem(elem.parentNode);
   },

   showGoToDateCalendar: function (link, event) {
      if (!$("jacs")) JACS.make("jacs", true);
      var startDateTs = parseInt($("JacsStart").value);
      $("jacs").dates[0] = [new Date(0), new Date(startDateTs)];
      JACS.show($("JacsOut"), event, "jacs");
      JACS.next("jacs", PdashEV.handleGoToDate, link.href);
      return false;
   },

   handleGoToDate: function (href) {
      var cal = $("jacs");
      if (cal && cal.dateReturned && cal.outputDate) {
         var ts = cal.outputDate.getTime();
         href = href.replace("00000000", ts.toString()) + "&adjustEff";
         location.href = href;
      }
   }

};

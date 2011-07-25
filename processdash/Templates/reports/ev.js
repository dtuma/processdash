
var PdashEV = {

   openCustomizeWindow: function() {
      var newWind = window.open ('', 'customize',
            'scrollbars=yes,dependent=yes,resizable=yes,width=420,height=720');
      newWind.focus();
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
   }

};

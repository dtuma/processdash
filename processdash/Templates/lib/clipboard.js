function copyToClipboard(id) {
    var url = "/dash/clipboard?id=" + encodeURIComponent(id);
    new Ajax.Request(url, {
        method: 'get',
	evalJS: false,
	evalJSON: false
    });
}

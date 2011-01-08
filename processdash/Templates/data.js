<!--#server-parsed#-->

<!--#if browserData.method eq 'iframe' #-->

/*
 * Load the iframe-based data connector for the Process Dashboard.
 */

document.write('<script type="text/javascript" src="/data-iframe.js?<!--#echo QUERY_STRING -->"></script>');

<!--#else#-->

/*
 * Load the prototype library, if it isn't already present.
 */

if ((typeof Prototype=='undefined') || 
    (typeof Element == 'undefined') || 
    (typeof Element.Methods=='undefined')) {

    document.write('<script type="text/javascript" src="/lib/prototype.js"></script>');
}


/*
 * Now load the ajax-aware data connector for the Process Dashboard.
 */

document.write('<script type="text/javascript" src="/data-ajax.js?<!--#echo QUERY_STRING -->"></script>');

<!--#endif-->

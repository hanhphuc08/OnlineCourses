// ------------------------------------------------------ //
// For demo purposes, can be deleted
// ------------------------------------------------------ //

var stylesheet = $('link#theme-stylesheet');
$("<link id='new-stylesheet' rel='stylesheet'>").insertAfter(stylesheet);
var alternateColour = $('link#new-stylesheet');

if ($.cookie("theme_csspath")) {
	alternateColour.attr("href", $.cookie("theme_csspath"));
}

$("#colour").change(function() {

	if ($(this).val() !== '') {

		var theme_csspath = $(this).val();

		alternateColour.attr("href", theme_csspath);

		$.cookie("theme_csspath", theme_csspath, {
			expires: 365,
			path: document.URL.substr(0, document.URL.lastIndexOf('/'))
		});

	}

	return false;
});
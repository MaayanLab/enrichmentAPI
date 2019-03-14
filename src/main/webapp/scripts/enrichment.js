
function openResult(id) {

	if ($('div.active').attr('id') == id) {
		toggleClose();
	} else {
		toggleClose();
		toggleOpen(id);
	}
}

function toggleClose() {
	$('div.active div.content').slideUp();
	$('div.active table.nav').fadeOut();
	$('div.active').removeClass('active');
}

function toggleOpen(id) {
	$('#' + id).addClass('active');
	$('div.active div.content').slideDown();
	$('div.active table.nav').fadeIn();
}

function showCategory(id, category){
	$('.shown').removeClass('shown');
	$('#' + category).addClass('shown');
	window.location.replace("enrichment?category="+category);
}



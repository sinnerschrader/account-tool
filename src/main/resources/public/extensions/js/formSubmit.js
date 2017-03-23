let forms = document.getElementsByTagName('form');
Array.from(forms).forEach(function (element) {
	element.addEventListener('submit', function () {

		if (this.getElementsByClassName('is-invalid').length > 0) {
			event.preventDefault();
		}

	});
});

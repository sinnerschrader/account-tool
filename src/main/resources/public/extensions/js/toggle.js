let links = document.getElementsByClassName('js_attributeButton');
Array.from(links).forEach(function (element) {
	element.addEventListener('click', function (event) {
		event.preventDefault();

		let id = this.getAttribute('data-toggleid');
		let component = document.getElementById(id);
		if (!component) {
			return;
		}
		if (component.classList.contains('active')) {
			component.classList.remove('active');
		} else {
			component.classList.add('active');
		}
		return false;
	});
});

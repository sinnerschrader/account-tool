let toggles = document.getElementsByClassName('js_switch');
Array.from(toggles).forEach(function (element) {
	element.addEventListener('click', function () {
		let checkedValue = this.getAttribute('data-checkedValue');
		let uncheckValue = this.getAttribute('data-uncheckValue');

		let hiddenInput = this.getElementsByTagName('input')[0];
		if (this.classList.contains('is-checked')) {
			hiddenInput.value = uncheckValue;
		} else {
			hiddenInput.value = checkedValue;
		}
	});
});

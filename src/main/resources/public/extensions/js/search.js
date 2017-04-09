import xhr from 'xhr';

let forms = document.getElementsByClassName('js_searchForm');
Array.from(forms).forEach(function (element) {
	element.addEventListener('submit', function (event) {
		event.preventDefault();
		let action = this.getAttribute('data-action');
		let searchTerm = this.querySelector('[name=searchTerm]').value;
		let resultContainer = document.getElementById(this.getAttribute('data-searchresultid'));
		let hasParams = action.indexOf('?') !== -1;
		xhr.get(action + (hasParams ? '&' : '?') + 'searchTerm=' + searchTerm, function (err, resp) {
			if (resp.statusCode !== 200 || resp.error) {
				// yep it isn't nice, but it works.
				resultContainer.innerHTML = '<div class="mdl-list__item s2act-groupList__item">' +
					'<span class="mdl-list__item-primary-content">' +
					'<i class="material-icons mdl-list__item-avatar">error</i><span>' +
					'There was an unexpected error' +
					'</span></span></div>';
				return false;
			}
			resultContainer.innerHTML = resp.body;
		});
		return false;
	});

	if (element.querySelector('[name=searchTerm]').value) {
		let event = new Event('submit');  // (*)
		element.dispatchEvent(event);
	}

});

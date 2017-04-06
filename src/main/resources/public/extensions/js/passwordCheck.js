let passwordInputs = document.getElementsByClassName('js_passwordStrength');
Array.from(passwordInputs).forEach(function (element) {
	element.addEventListener('keyup', function () {
		let password = this.value;
		let inputWrapper = this.parentNode;
		let messageElement = inputWrapper.querySelector('.js_passwordStrengthMessage');
		if (password.length < 10) {
			inputWrapper.classList.add('is-invalid');
			messageElement.innerHTML = 'The password required at least 10 characters';
			return true;
		}
		return true;
	});
});

let passwordRepeatInputs = document.getElementsByClassName('js_passwordMatch');
Array.from(passwordRepeatInputs).forEach(function (element) {
	element.addEventListener('keyup', function () {
		let wrapper = this.parentNode;
		let password = document.getElementById(this.getAttribute('data-matchid')).value;

		if (this.value === password) {
			wrapper.classList.remove('is-invalid');
		} else {
			wrapper.classList.add('is-invalid');
		}

		return true;
	});
});

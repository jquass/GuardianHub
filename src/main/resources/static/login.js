// Guardian Hub - Login Page Script

// Show reset form
document.getElementById('show-reset').addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('login-form').classList.add('hidden');
    document.getElementById('reset-form').classList.add('active');
    document.getElementById('factory-password').focus();
});

// Back to login form
document.getElementById('back-to-login').addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('reset-form').classList.remove('active');
    document.getElementById('login-form').classList.remove('hidden');
    document.getElementById('password').focus();

    // Clear reset form and messages
    document.getElementById('factory-password').value = '';
    document.getElementById('serial-number').value = '';
    document.getElementById('reset-message').style.display = 'none';
});

// Handle login form submission
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const password = document.getElementById('password').value;
    const errorDiv = document.getElementById('login-error');
    const submitButton = e.target.querySelector('button[type="submit"]');

    // Disable button during request
    submitButton.disabled = true;
    errorDiv.style.display = 'none';

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ password })
        });

        const data = await response.json();

        if (data.success) {
            sessionStorage.setItem('auth_token', data.token);
            window.location.href = '/';
        } else {
            errorDiv.textContent = data.message || 'Invalid password';
            errorDiv.style.display = 'block';
            submitButton.disabled = false;
        }
    } catch (error) {
        errorDiv.textContent = 'Error connecting to server';
        errorDiv.style.display = 'block';
        submitButton.disabled = false;
    }
});

// Handle factory reset form submission
document.getElementById('reset-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const factoryPassword = document.getElementById('factory-password').value;
    const serialNumber = document.getElementById('serial-number').value;
    const messageDiv = document.getElementById('reset-message');
    const submitButton = e.target.querySelector('button[type="submit"]');

    // Disable button during request
    submitButton.disabled = true;
    messageDiv.style.display = 'none';

    try {
        const response = await fetch('/api/auth/reset-to-factory', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                factoryPassword,
                serialNumber
            })
        });

        const data = await response.json();

        if (data.success) {
            messageDiv.className = 'success-message';
            messageDiv.textContent = data.message + ' - Redirecting to login...';
            messageDiv.style.display = 'block';

            setTimeout(() => {
                document.getElementById('reset-form').classList.remove('active');
                document.getElementById('login-form').classList.remove('hidden');

                // Clear forms
                document.getElementById('factory-password').value = '';
                document.getElementById('serial-number').value = '';
                messageDiv.style.display = 'none';

                document.getElementById('password').focus();
            }, 2000);
        } else {
            messageDiv.className = 'error-message';
            messageDiv.textContent = data.message || 'Invalid factory password or serial number';
            messageDiv.style.display = 'block';
            submitButton.disabled = false;
        }
    } catch (error) {
        messageDiv.className = 'error-message';
        messageDiv.textContent = 'Error connecting to server';
        messageDiv.style.display = 'block';
        submitButton.disabled = false;
    }
});
// Authentication module

export async function checkAuth() {
    const token = sessionStorage.getItem('auth_token');

    if (!token) {
        window.location.href = '/login.html';
        return false;
    }

    try {
        const response = await fetch('/api/auth/check', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const data = await response.json();

        if (!data.authenticated) {
            sessionStorage.removeItem('auth_token');
            window.location.href = '/login.html';
            return false;
        }

        return true;
    } catch (error) {
        console.error('Auth check failed:', error);
        sessionStorage.removeItem('auth_token');
        window.location.href = '/login.html';
        return false;
    }
}

export async function logout() {
    const token = sessionStorage.getItem('auth_token');

    try {
        await fetch('/api/auth/logout', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
    } catch (error) {
        console.error('Logout error:', error);
    }

    sessionStorage.removeItem('auth_token');
    window.location.href = '/login.html';
}

// Make logout available globally for onclick handlers
window.logout = logout;
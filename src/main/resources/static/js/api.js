// API wrapper with authentication

export async function authFetch(url, options = {}) {
    const token = sessionStorage.getItem('auth_token');

    const response = await fetch(url, {
        ...options,
        headers: {
            ...options.headers,
            'Authorization': `Bearer ${token}`
        }
    });

    if (response.status === 401) {
        sessionStorage.removeItem('auth_token');
        window.location.href = '/login.html';
        throw new Error('Unauthorized');
    }

    return response;
}
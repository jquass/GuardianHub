// Password update functions

import { authFetch } from './api.js';
import { showMessage, showStatus } from './utils.js';

export async function updatePassword(event, key, endpoint) {
    event.preventDefault();

    const input = document.getElementById(`input-${key}`);
    const statusDiv = document.getElementById(`status-${key}`);
    const password = input.value;
    const button = event.target.querySelector('button[type="submit"]');

    if (!password) {
        showMessage(statusDiv, 'error', 'Please enter a password');
        return;
    }

    input.disabled = true;
    button.disabled = true;

    showMessage(statusDiv, 'info', 'Updating password...');

    try {
        const response = await authFetch(`/api/password/${endpoint}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ password })
        });

        const data = await response.json();

        if (data.status === 'success') {
            showMessage(statusDiv, 'success', `✅ ${data.message}`);
            input.value = '';
        } else {
            throw new Error(data.message);
        }
    } catch (err) {
        showMessage(statusDiv, 'error', `❌ ${err.message}`);
    } finally {
        input.disabled = false;
        button.disabled = false;
    }
}

export async function updateLoginPassword(event) {
    event.preventDefault();

    const currentPassword = document.getElementById('current-login-password').value;
    const newPassword = document.getElementById('new-login-password').value;
    const serialNumber = document.getElementById('serial-number').value;
    const statusDiv = document.getElementById('status-LOGIN_PASSWORD');

    if (!currentPassword || !newPassword || !serialNumber) {
        showStatus(statusDiv, 'All fields are required', 'error');
        return;
    }

    if (newPassword.length < 8) {
        showStatus(statusDiv, 'New password must be at least 8 characters', 'error');
        return;
    }

    try {
        const response = await authFetch('/api/auth/change-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                currentPassword,
                newPassword,
                serialNumber
            })
        });

        const data = await response.json();

        if (data.success) {
            showStatus(statusDiv, 'Password changed successfully!', 'success');
            document.getElementById('current-login-password').value = '';
            document.getElementById('new-login-password').value = '';
            document.getElementById('serial-number').value = '';
        } else {
            showStatus(statusDiv, data.message || 'Failed to change password', 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showStatus(statusDiv, 'Error changing password', 'error');
    }
}

// IMPORTANT: Make functions available globally for inline event handlers
if (typeof window !== 'undefined') {
    window.updatePassword = updatePassword;
    window.updateLoginPassword = updateLoginPassword;
}
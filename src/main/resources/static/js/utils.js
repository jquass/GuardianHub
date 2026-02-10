// Helper functions

export function showMessage(element, type, message) {
    element.innerHTML = `<div class="message ${type}">${message}</div>`;
}

export function showStatus(statusDiv, message, type) {
    if (!statusDiv) return;

    statusDiv.className = '';

    if (type === 'success') {
        statusDiv.className = 'status-message success';
    } else if (type === 'error') {
        statusDiv.className = 'status-message error';
    } else {
        statusDiv.className = 'status-message';
    }

    statusDiv.textContent = message;
    statusDiv.style.display = 'block';

    setTimeout(() => {
        statusDiv.style.display = 'none';
    }, 5000);
}

export function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
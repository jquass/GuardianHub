// Timezone management

import { authFetch } from './api.js';
import { showMessage } from './utils.js';
import { loadConfig } from './config.js';

let validTimezones = new Set();

export async function loadTimezones() {
    try {
        const response = await authFetch('/api/timezone');
        const data = await response.json();

        if (data.status === 'success') {
            // Store valid timezones for validation
            validTimezones = new Set(data.timezones);

            const datalist = document.getElementById('timezone-list');
            if (datalist) {
                data.timezones.forEach(tz => {
                    const option = document.createElement('option');
                    option.value = tz;
                    datalist.appendChild(option);
                });
            }
        }
    } catch (err) {
        console.error('Failed to load timezones:', err);
    }
}

export async function updateTimezone(event) {
    event.preventDefault();

    const input = document.getElementById('timezone-select');
    const statusDiv = document.getElementById('status-TZ');
    const timezone = input.value.trim();
    const button = event.target.querySelector('button[type="submit"]');

    // Clear previous styling
    input.classList.remove('error', 'success');

    // Validation
    if (!timezone) {
        input.classList.add('error');
        showMessage(statusDiv, 'error', 'Please select a timezone');
        return;
    }

    if (!validTimezones.has(timezone)) {
        input.classList.add('error');
        showMessage(statusDiv, 'error', 'Invalid timezone. Please select from the list.');
        setTimeout(() => {
            input.value = '';
            input.classList.remove('error');
        }, 2000);
        return;
    }

    // Validation passed
    input.classList.add('success');

    input.disabled = true;
    button.disabled = true;

    showMessage(statusDiv, 'info', 'Updating timezone...');

    try {
        const response = await authFetch('/api/timezone', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ timezone })
        });

        const data = await response.json();

        if (data.status === 'success') {
            const taskId = data.taskId;
            showMessage(statusDiv, 'info', `✅ ${data.message}`);

            await pollTaskStatus(taskId, statusDiv);

            await loadConfig();
            await loadTimezones();

            input.disabled = false;
            button.disabled = false;
            input.classList.remove('success');

            showMessage(statusDiv, 'success', '✅ Timezone updated and services restarted!');
        } else {
            throw new Error(data.message);
        }
    } catch (err) {
        input.classList.remove('success');
        input.classList.add('error');
        showMessage(statusDiv, 'error', `❌ ${err.message}`);
        input.disabled = false;
        button.disabled = false;
    }
}

async function pollTaskStatus(taskId, statusDiv) {
    const maxAttempts = 30;
    let attempts = 0;

    while (attempts < maxAttempts) {
        attempts++;

        try {
            const response = await authFetch(`/api/status/task/${taskId}`);
            const data = await response.json();

            if (data.status === 'success') {
                const task = data.task;

                showMessage(statusDiv, 'info',
                    `⏳ ${task.message} (${task.progress}%)`);

                if (task.status === 'completed') {
                    return;
                } else if (task.status === 'failed') {
                    throw new Error(task.message);
                }

                await new Promise(resolve => setTimeout(resolve, 2000));
            }
        } catch (err) {
            if (attempts >= maxAttempts) {
                throw new Error('Service restart timed out. Please check manually.');
            }

            await new Promise(resolve => setTimeout(resolve, 2000));
        }
    }

    throw new Error('Service restart timed out');
}

if (typeof window !== 'undefined') {
    window.updateTimezone = updateTimezone;
}
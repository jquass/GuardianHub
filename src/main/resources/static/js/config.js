import { authFetch } from './api.js';
import { escapeHtml } from './utils.js';

export async function loadConfig() {
    const loading = document.getElementById('loading');
    const container = document.getElementById('config-container');

    loading.style.display = 'block';
    container.innerHTML = '';

    try {
        const response = await authFetch('/api/config');
        const data = await response.json();

        if (data.status === 'success') {
            const { categories, entries } = data.config;

            // Group entries by category
            const entriesByCategory = {};
            entries.forEach(entry => {
                if (!entriesByCategory[entry.categoryName]) {
                    entriesByCategory[entry.categoryName] = [];
                }
                entriesByCategory[entry.categoryName].push(entry);
            });

            // Render each category
            categories.forEach(category => {
                const categoryEntries = entriesByCategory[category.name] || [];

                const section = document.createElement('div');
                section.className = 'config-section';

                const gridClass = category.name === 'Network' ? 'config-grid two-column' : 'config-grid';

                // Category tooltip
                const tooltipHtml = category.tooltip
                    ? `<span class="tooltip-icon" data-tooltip="${escapeHtml(category.tooltip)}">?</span>`
                    : '';

                section.innerHTML = `
                    <div class="category-header">
                        <h2 class="category-title">${category.name}</h2>
                        ${tooltipHtml}
                    </div>
                    <div class="${gridClass}">
                        ${categoryEntries.map(entry => renderConfigItem(entry)).join('')}
                    </div>
                `;

                container.appendChild(section);
            });

            loading.style.display = 'none';
        } else {
            throw new Error(data.message || 'Failed to load configuration');
        }
    } catch (err) {
        loading.style.display = 'none';
        container.innerHTML = `
            <div class="config-section message error">
                <strong>❌ Error Loading Configuration</strong><br>
                ${err.message}
            </div>
        `;
    }
}

function renderConfigItem(entry) {
    const isPassword = entry.sensitive;
    const isTimezone = entry.key === 'TZ';
    const itemClass = isPassword ? 'config-item password-item' : 'config-item';

    // Field tooltip
    const tooltipHtml = entry.tooltip
        ? `<span class="tooltip-icon" data-tooltip="${escapeHtml(entry.tooltip)}">?</span>`
        : '';

    if (isPassword) {
        let endpoint, placeholder;

        if (entry.key === 'PIHOLE_PASSWORD') {
            endpoint = 'pihole';
            placeholder = 'Enter new Pi-hole password';
        } else if (entry.key === 'WIREGUARD_PASSWORD_HASH') {
            endpoint = 'wireguard';
            placeholder = 'Enter new WireGuard password';
        } else if (entry.key === 'NPM_ADMIN_PASSWORD') {
            endpoint = 'npm';
            placeholder = 'Enter new NPM password';
        } else if (entry.key === 'LOGIN_PASSWORD') {
            return `
                <div class="${itemClass}">
                    <div class="config-label">
                        <div class="config-description">${entry.description}</div>
                        ${tooltipHtml}
                    </div>
                    <form onsubmit="window.updateLoginPassword(event); return false;">
                        <div class="password-controls">
                            <input 
                                type="password" 
                                class="password-input" 
                                id="current-login-password" 
                                placeholder="Current password"
                                required
                            />
                            <input 
                                type="password" 
                                class="password-input" 
                                id="new-login-password" 
                                placeholder="New password (min 8 chars)"
                                required
                                minlength="8"
                            />
                            <input 
                                type="text" 
                                class="password-input" 
                                id="serial-number" 
                                placeholder="Serial number from label"
                                pattern="[0-9]{8}-[A-F0-9]{6}"
                                required
                                title="Format: YYYYMMDD-XXXXXX (from device label)"
                            />
                            <button type="submit" class="btn btn-small">
                                Update Password
                            </button>
                        </div>
                    </form>
                    <div id="status-${entry.key}"></div>
                </div>
            `;
        }

        return `
            <div class="${itemClass}">
                <div class="config-label">
                    <div class="config-description">${entry.description}</div>
                    ${tooltipHtml}
                </div>
                <form onsubmit="window.updatePassword(event, '${entry.key}', '${endpoint}'); return false;">
                    <div class="password-controls">
                        <input 
                            type="password" 
                            class="password-input" 
                            id="input-${entry.key}" 
                            placeholder="${placeholder}"
                            required
                        />
                        <button type="submit" class="btn btn-small">
                            Update Password
                        </button>
                    </div>
                </form>
                <div id="status-${entry.key}"></div>
            </div>
        `;
    } else if (isTimezone) {
        return `
            <div class="${itemClass}">
                <div class="config-label">
                    <div class="config-description">${entry.description}</div>
                    ${tooltipHtml}
                </div>
                <div class="config-value">${escapeHtml(entry.value)}</div>
                <form onsubmit="window.updateTimezone(event); return false;">
                    <div class="password-controls">
                        <input 
                            type="text"
                            list="timezone-list"
                            class="password-input" 
                            id="timezone-select"
                            placeholder="Type to search timezones..."
                            required
                        />
                        <datalist id="timezone-list">
                        </datalist>
                        <button type="submit" class="btn btn-small">
                            Update Timezone
                        </button>
                    </div>
                </form>
                <div id="status-TZ"></div>
            </div>
        `;
    } else {
        // Regular read-only config item
        return `
            <div class="${itemClass}">
                <div class="config-label">
                    <div class="config-description">${entry.description}</div>
                    ${tooltipHtml}
                </div>
                <div class="config-value">${escapeHtml(entry.value)}</div>
            </div>
        `;
    }
}
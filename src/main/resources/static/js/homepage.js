// Homepage link management

import { authFetch } from './api.js';

export async function initHomepageLink() {
    const link = document.getElementById('homepage-link');
    if (!link) return;

    try {
        const response = await authFetch('/api/homepage/link');
        const data = await response.json();

        if (data.status === 'success') {
            link.href = data.url;
            console.log(`Homepage link: ${data.url} (DNS: ${data.usedDns})`);
        }
    } catch (error) {
        console.error('Failed to get homepage link:', error);
        // Fallback to DNS link
        link.href = 'http://config.guardian.home';
    }
}
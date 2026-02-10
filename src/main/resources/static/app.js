// Main application entry point

import { checkAuth } from './js/auth.js';
import { loadConfig } from './js/config.js';
import { loadTimezones } from './js/timezone.js';
import { initHomepageLink } from './js/homepage.js';

// Run auth check and initialize app
(async function() {
    const isAuthenticated = await checkAuth();
    if (isAuthenticated) {
        await loadConfig();
        await loadTimezones();
        await initHomepageLink();
    }
})();
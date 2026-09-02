/**
 * Stream TV — Known Addons Configuration
 * 
 * External Stremio addons that provide streams.
 * The app proxies requests to these when fetching streams.
 */

const ADDONS = [
    {
        id: 'com.stremio.torrentio.addon',
        name: 'Torrentio',
        url: 'https://torrentio.strem.fun',
        description: 'Torrent streams from multiple providers. Supports RealDebrid, Premiumize, AllDebrid, and more.',
        version: '0.0.15',
        resources: ['stream'],
        types: ['movie', 'series', 'anime'],
        idPrefixes: ['tt', 'kitsu'],
        configurable: true,
        enabled: true
    }
];

/**
 * Get all enabled external addons.
 */
function getEnabledAddons() {
    return ADDONS.filter(a => a.enabled);
}

/**
 * Get a specific addon by ID.
 */
function getAddonById(id) {
    return ADDONS.find(a => a.id === id);
}

/**
 * Get the stream endpoint URL for an addon.
 * @param {string} addonId - The addon ID
 * @param {string} type - Content type (movie, series, anime)
 * @param {string} id - Content ID (e.g., tt1375666 for IMDB)
 * @returns {string|null} Full stream endpoint URL
 */
function getStreamUrl(addonId, type, id) {
    const addon = getAddonById(addonId);
    if (!addon || !addon.resources.includes('stream')) return null;
    return `${addon.url}/stream/${type}/${id}.json`;
}

module.exports = { ADDONS, getEnabledAddons, getAddonById, getStreamUrl };

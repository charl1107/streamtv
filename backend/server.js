require('dotenv').config();
const express = require('express');
const cors = require('cors');
const NodeCache = require('node-cache');
let extractStreams;
try {
    extractStreams = require('./scraper').extractStreams;
} catch {
    console.log('[Server] Puppeteer scraper not available — running without scraping support');
    extractStreams = async () => [];
}

const { getEnabledAddons, getStreamUrl } = require('./addons');
const https = require('https');
const http = require('http');

const app = express();
const cache = new NodeCache({ stdTTL: 3600 }); // 1 hour default TTL

// --- Middleware ---
app.use(cors({ origin: '*', methods: ['GET', 'OPTIONS'] }));
app.use(express.json());

const PORT = (process.env.PORT && process.env.PORT !== '0') ? parseInt(process.env.PORT) : 7000;

// --- Sample Data ---
// Replace this with real scraped data once Puppeteer is integrated.
const sampleAnime = [
    {
        id: 'kitsu:1',
        title: 'Attack on Titan',
        poster: 'https://cdn.myanimelist.net/images/anime/10/47347.jpg',
        background: 'https://cdn.myanimelist.net/images/anime/10/47347l.jpg',
        description: 'Centuries ago, mankind was slaughtered to near extinction by monstrous humanoid creatures called Titans, forcing humans to hide in fear behind enormous concentric walls.',
        genres: ['Action', 'Drama', 'Fantasy'],
        year: '2013',
        imdbId: 'tt1635178',  // IMDB ID for Torrentio
        episodes: [
            { id: 'kitsu:1:1', title: 'To You, in 2000 Years', season: 1, episode: 1, duration: 1440, scrapeUrl: 'https://www.w3schools.com/html/html5_video.asp' },
            { id: 'kitsu:1:2', title: 'That Day', season: 1, episode: 2, duration: 1440, scrapeUrl: 'https://www.w3schools.com/html/html5_video.asp' },
            { id: 'kitsu:1:3', title: 'A Dim Light Amid Despair', season: 1, episode: 3, duration: 1440, scrapeUrl: 'https://www.w3schools.com/html/html5_video.asp' },
            { id: 'kitsu:1:4', title: 'The Night of the Closing Ceremony', season: 1, episode: 4, duration: 1440, scrapeUrl: 'https://www.w3schools.com/html/html5_video.asp' },
            { id: 'kitsu:1:5', title: 'To You, in 2000 Years', season: 1, episode: 5, duration: 1440, scrapeUrl: 'https://www.w3schools.com/html/html5_video.asp' }
        ]
    },
    {
        id: 'kitsu:2',
        title: 'Demon Slayer',
        poster: 'https://cdn.myanimelist.net/images/anime/1286/99889.jpg',
        background: 'https://cdn.myanimelist.net/images/anime/1286/99889l.jpg',
        description: 'Ever since the death of his father, the burden of supporting the family has fallen upon Tanjirou Kamado\'s shoulders. Though living impoverished on a remote mountain, the Kamado family are able to enjoy a relatively peaceful and happy life.',
        genres: ['Action', 'Fantasy', 'Supernatural'],
        year: '2019',
        imdbId: 'tt1035178',  // IMDB ID for Torrentio
        episodes: [
            { id: 'kitsu:2:1', title: 'Cruelty', season: 1, episode: 1, duration: 1440 },
            { id: 'kitsu:2:2', title: 'Trainer Sakonji Urokodaki', season: 1, episode: 2, duration: 1440 },
            { id: 'kitsu:2:3', title: 'Sabito and Makomo', season: 1, episode: 3, duration: 1440 },
            { id: 'kitsu:2:4', title: 'Final Selection', season: 1, episode: 4, duration: 1440 }
        ]
    },
    {
        id: 'kitsu:3',
        title: 'One Piece',
        poster: 'https://cdn.myanimelist.net/images/anime/6/73245.jpg',
        background: 'https://cdn.myanimelist.net/images/anime/6/73245l.jpg',
        description: 'Gol D. Roger was known as the Pirate King, the strongest and most infamous being to have sailed the Grand Line. The capture and execution of Roger by the World Government brought a change throughout the world.',
        genres: ['Action', 'Adventure', 'Fantasy'],
        year: '1999',
        imdbId: 'tt0388629',  // IMDB ID for Torrentio
        episodes: [
            { id: 'kitsu:3:1', title: 'I\'m Luffy! The Man Who\'s Gonna Be King of the Pirates!', season: 1, episode: 1, duration: 1440 },
            { id: 'kitsu:3:2', title: 'The Man with the Devil Fruit', season: 1, episode: 2, duration: 1440 },
            { id: 'kitsu:3:3', title: 'Morgan versus Luffy! Who\'s This Beautiful Young Girl?', season: 1, episode: 3, duration: 1440 }
        ]
    },
    {
        id: 'kitsu:4',
        title: 'My Hero Academia',
        poster: 'https://cdn.myanimelist.net/images/anime/10/78745.jpg',
        background: 'https://cdn.myanimelist.net/images/anime/10/78745l.jpg',
        description: 'The appearance of "quirks," newly discovered super powers, has been steadily increasing over the years, with 80 percent of humanity possessing various abilities from manipulation of elements to shapeshifting.',
        genres: ['Action', 'Comedy', 'Superhero'],
        year: '2016',
        imdbId: 'tt5626028',  // IMDB ID for Torrentio
        episodes: [
            { id: 'kitsu:4:1', title: 'Izuku Midoriya: Origin', season: 1, episode: 1, duration: 1440 },
            { id: 'kitsu:4:2', title: 'What It Takes to Be a Hero', season: 1, episode: 2, duration: 1440 }
        ]
    }
];

// --- Stremio Addon Protocol Endpoints ---

/**
 * GET /manifest.json
 * Returns the addon manifest describing capabilities and catalog definitions.
 */
app.get('/manifest.json', (req, res) => {
    const manifest = {
        id: 'com.school.anime.addon',
        version: '1.0.0',
        name: 'School Anime Project',
        description: 'Educational anime streaming addon for Stream TV school project',
        logo: 'https://via.placeholder.com/256x256.png?text=StreamTV',
        resources: ['catalog', 'meta', 'stream'],
        types: ['series'],
        catalogs: [
            {
                type: 'series',
                id: 'popular',
                name: 'Popular Anime',
                extra: [{ name: 'genre', isRequired: false }]
            }
        ],
        idPrefixes: ['kitsu:'],
        behaviorHints: {
            configurable: false,
            configurationRequired: false
        }
    };

    res.set('Cache-Control', 'public, max-age=3600');
    res.json(manifest);
});

/**
 * GET /catalog/:type/:id.json
 * Returns a list of meta items for the given catalog.
 * Optional query params: genre, skip, limit
 */
app.get('/catalog/:type/:id.json', (req, res) => {
    const { type, id } = req.params;
    const genre = req.query.genre;
    const skip = parseInt(req.query.skip) || 0;
    const limit = parseInt(req.query.limit) || 20;

    const cacheKey = `catalog:${type}:${id}:${genre || 'all'}:${skip}:${limit}`;
    const cached = cache.get(cacheKey);
    if (cached) return res.json(cached);

    let filtered = [...sampleAnime];

    // Filter by genre if specified
    if (genre) {
        filtered = filtered.filter(a =>
            a.genres.some(g => g.toLowerCase() === genre.toLowerCase())
        );
    }

    const response = {
        metas: filtered.slice(skip, skip + limit).map(a => ({
            id: a.id,
            type: 'series',
            name: a.title,
            poster: a.poster,
            background: a.background,
            posterShape: 'poster'
        }))
    };

    cache.set(cacheKey, response);
    res.set('Cache-Control', 'public, max-age=3600');
    res.json(response);
});

/**
 * GET /meta/:type/:id.json
 * Returns full metadata for a single item (including episode list).
 */
app.get('/meta/:type/:id.json', (req, res) => {
    const cacheKey = `meta:${req.params.type}:${req.params.id}`;
    const cached = cache.get(cacheKey);
    if (cached) return res.json(cached);

    const anime = sampleAnime.find(a => a.id === req.params.id);
    if (!anime) {
        return res.status(404).json({
            error: 'Not found',
            message: `No meta found for ${req.params.type}/${req.params.id}`
        });
    }

    const response = {
        meta: {
            id: anime.id,
            type: 'series',
            name: anime.title,
            description: anime.description,
            poster: anime.poster,
            background: anime.background,
            logo: anime.poster,
            runtime: '24 min',
            released: anime.year ? new Date(anime.year) : undefined,
            year: anime.year,
            genres: anime.genres,
            imdbId: anime.imdbId || null,  // IMDB ID for Torrentio
            director: [],
            cast: [],
            videos: anime.episodes.map(ep => ({
                id: ep.id,
                title: `S${String(ep.season).padStart(2, '0')}E${String(ep.episode).padStart(2, '0')} - ${ep.title}`,
                season: ep.season,
                episode: ep.episode,
                duration: ep.duration,
                released: anime.year ? new Date(anime.year) : undefined
            }))
        }
    };

    cache.set(cacheKey, response);
    res.set('Cache-Control', 'public, max-age=3600');
    res.json(response);
});

/**
 * GET /stream/:type/:id.json
 * Returns available stream URLs for a given episode/content.
 * 
 * Uses Puppeteer to scrape the source site and extract real stream URLs.
 * Falls back to a test MP4 if scraping fails or is not configured.
 */
app.get('/stream/:type/:id.json', async (req, res) => {
    const { type, id } = req.params;
    const cacheKey = `stream:${type}:${id}`;
    const cached = cache.get(cacheKey);
    if (cached) return res.json(cached);

    const streams = [];

    // Look up episode metadata
    const parts = id.split(':');
    const animeId = `${parts[0]}:${parts[1]}`;
    const anime = sampleAnime.find(a => a.id === animeId);
    const episodeNum = parseInt(parts[2]) || 1;
    let episodeTitle = '';
    let scrapeUrl = null;

    if (anime) {
        const ep = anime.episodes.find(e => e.episode === episodeNum);
        if (ep) {
            episodeTitle = `${anime.title} - S${String(ep.season).padStart(2, '0')}E${String(ep.episode).padStart(2, '0')} - ${ep.title}`;
            scrapeUrl = ep.scrapeUrl || null; // If episode has a URL to scrape
        }
    }

    // Attempt Puppeteer scraping if we have a URL
    if (scrapeUrl) {
        try {
            console.log(`[Stream] Scraping ${scrapeUrl} for ${id}...`);
            const scraped = await extractStreams(scrapeUrl, { waitMs: 15000 });
            for (const s of scraped) {
                if (s.url) {
                    streams.push({
                        title: episodeTitle || s.type.toUpperCase(),
                        name: s.type === 'hls' ? 'HLS Stream' : 'Video Stream',
                        description: `Source: ${s.source || 'unknown'}`,
                        url: s.url,
                        behaviorHints: { notWebReady: s.type === 'hls' }
                    });
                }
            }
        } catch (err) {
            console.error(`[Stream] Scrape failed for ${scrapeUrl}:`, err.message);
        }
    }

    // If no scraped streams, add a meta entry with episode info
    if (streams.length === 0 && anime) {
        streams.push({
            title: episodeTitle,
            name: anime.title,
            description: episodeTitle,
            behaviorHints: { notWebReady: true }
        });
    }

    // Always include the test stream as a fallback
    streams.unshift({
        title: 'Test Stream (Big Buck Bunny)',
        description: 'Sample video — confirms playback is working',
        url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
        behaviorHints: { notWebReady: false }
    });

    const response = { streams };
    cache.set(cacheKey, response);
    res.set('Cache-Control', 'public, max-age=600');
    res.json(response);
});

// --- Scraping endpoint (for development/testing) ---

/**
 * POST /scrape
 * Triggers a scrape for a given URL and returns discovered streams.
 * Only for development — not part of Stremio protocol.
 */
app.post('/scrape', async (req, res) => {
    const { url } = req.body;
    if (!url) {
        return res.status(400).json({ error: 'Missing "url" in request body' });
    }

    try {
        const streams = await extractStreams(url, { waitMs: 10000 });
        res.json({ url, streams, count: streams.length });
    } catch (err) {
        console.error(`[Scraper] Failed for ${url}:`, err.message);
        res.status(500).json({ error: err.message });
    }
});

// --- External Addon Routes ---

/**
 * Helper: fetch JSON from a URL (supports http and https).
 */
function fetchJson(url, redirectCount = 0) {
    if (redirectCount > 5) {
        return Promise.reject(new Error('Too many redirects'));
    }
    return new Promise((resolve, reject) => {
        const client = url.startsWith('https') ? https : http;
        client.get(url, { headers: { 'User-Agent': 'StreamTV/1.0' } }, (res) => {
            // Follow redirects
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                const redirectUrl = res.headers.location.startsWith('http')
                    ? res.headers.location
                    : new URL(res.headers.location, url).href;
                return fetchJson(redirectUrl, redirectCount + 1).then(resolve, reject);
            }
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch (e) {
                    reject(new Error(`Invalid JSON from ${url}`));
                }
            });
        }).on('error', reject);
    });
}

/**
 * GET /addons
 * Lists all known external addons.
 */
app.get('/addons', (req, res) => {
    res.json({ addons: getEnabledAddons() });
});

/**
 * GET /addons/:addonId/streams/:type/:id.json
 * Proxies a stream request to an external addon (e.g., Torrentio).
 */
app.get('/addons/:addonId/streams/:type/:id.json', async (req, res) => {
    const { addonId, type, id } = req.params;
    const cacheKey = `ext:${addonId}:${type}:${id}`;
    const cached = cache.get(cacheKey);
    if (cached) return res.json(cached);

    const streamUrl = getStreamUrl(addonId, type, id);
    if (!streamUrl) {
        return res.status(404).json({
            error: 'Addon not found',
            message: `No addon with ID "${addonId}" that provides streams`
        });
    }

    try {
        console.log(`[Addon] Fetching streams from ${addonId}: ${streamUrl}`);
        const data = await fetchJson(streamUrl);
        cache.set(cacheKey, data);
        res.set('Cache-Control', 'public, max-age=300');
        res.json(data);
    } catch (err) {
        console.error(`[Addon] Failed to fetch from ${addonId}:`, err.message);
        res.status(502).json({
            error: 'Upstream error',
            message: `Failed to fetch from ${addonId}: ${err.message}`
        });
    }
});

// --- Utility endpoints ---

/**
 * GET /health
 * Simple health check for monitoring.
 */
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        uptime: process.uptime(),
        timestamp: new Date().toISOString(),
        cacheStats: cache.getStats()
    });
});

// --- 404 handler ---
app.use((req, res) => {
    res.status(404).json({
        error: 'Not found',
        message: `No route for ${req.method} ${req.path}`
    });
});

// --- Start server ---
app.listen(PORT, () => {
    console.log(`\n🚀 Stream TV Addon Server`);
    console.log(`   Listening on http://localhost:${PORT}`);
    console.log(`   Manifest: http://localhost:${PORT}/manifest.json`);
    console.log(`   Catalog:  http://localhost:${PORT}/catalog/series/popular.json`);
    console.log(`   Health:   http://localhost:${PORT}/health\n`);
});

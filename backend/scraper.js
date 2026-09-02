/**
 * Stream TV — Puppeteer Scraper Module
 * 
 * Extracts video stream URLs from anime streaming sites by:
 * 1. Navigating to the page
 * 2. Waiting for iframes/video players to load
 * 3. Intercepting network requests for .m3u8, .mp4, .ts URLs
 * 4. Extracting video src attributes
 */

const puppeteer = require('puppeteer');
const NodeCache = require('node-cache');

const scrapeCache = new NodeCache({ stdTTL: 1800 }); // 30 min TTL

// Patterns that indicate a video stream URL
const STREAM_PATTERNS = [
    /\.m3u8(\?|$)/i,           // HLS playlists
    /\.mp4(\?|$)/i,            // Direct MP4
    /\.ts(\?|$)/i,             // MPEG-TS segments
    /\.mkv(\?|$)/i,            // MKV files
    /manifest.*\.mpd(\?|$)/i,  // DASH manifests
    /videoplayback.*&/i,       // YouTube-style
    /googlevideo\.com.*videoplayback/i
];

// Common selectors for video elements
const VIDEO_SELECTORS = [
    'video source',
    'video',
    'iframe[src*="embed"]',
    'iframe[src*="player"]',
    '.plyr video',
    '#player video',
    '.video-player video',
    '[data-video-src]'
];

/**
 * Extracts video stream URLs from a given page URL.
 * 
 * @param {string} pageUrl - The URL to scrape
 * @param {object} options
 * @param {number} options.waitMs - Wait time after page load (default 12s)
 * @param {number} options.timeout - Navigation timeout (default 30s)
 * @returns {Promise<{url: string, type: string}[]>} Discovered streams
 */
async function extractStreams(pageUrl, options = {}) {
    const cacheKey = `scrape:${pageUrl}`;
    const cached = scrapeCache.get(cacheKey);
    if (cached) {
        console.log(`[Scraper] Cache hit for ${pageUrl}`);
        return cached;
    }

    const { waitMs = 12000, timeout = 30000 } = options;

    let browser;
    try {
        browser = await puppeteer.launch({
            headless: 'new',
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-dev-shm-usage',
                '--disable-gpu',
                '--disable-web-security',
                '--autoplay-policy=no-user-gesture-required'
            ]
        });

        const page = await browser.newPage();
        await page.setUserAgent(
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ' +
            '(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        );

        // Set a reasonable viewport
        await page.setViewport({ width: 1280, height: 720 });

        const discoveredStreams = [];

        // --- Network interception: catch stream URLs from requests ---
        await page.setRequestInterception(true);

        page.on('request', (request) => {
            const url = request.url();
            for (const pattern of STREAM_PATTERNS) {
                if (pattern.test(url)) {
                    const type = getStreamType(url);
                    discoveredStreams.push({ url, type, source: 'network' });
                    console.log(`[Scraper] Found stream via network: ${url.substring(0, 120)}...`);
                    break;
                }
            }
            request.continue();
        });

        // Also catch HLS manifest responses
        page.on('response', async (response) => {
            try {
                const contentType = response.headers()['content-type'] || '';
                const url = response.url();

                // Catch M3U8 responses
                if (contentType.includes('mpegurl') || contentType.includes('x-mpegurl') || url.includes('.m3u8')) {
                    const text = await response.text();
                    if (text && text.includes('#EXTM3U')) {
                        discoveredStreams.push({
                            url,
                            type: 'hls',
                            source: 'response',
                            body: text.substring(0, 500) // First 500 chars for debugging
                        });
                        console.log(`[Scraper] Found M3U8 response: ${url.substring(0, 120)}`);
                    }
                }
            } catch {
                // Response body might be unavailable (redirect, etc.)
            }
        });

        // --- Navigate to page ---
        console.log(`[Scraper] Navigating to ${pageUrl}...`);
        await page.goto(pageUrl, {
            waitUntil: 'domcontentloaded',
            timeout
        });

        // Wait for dynamic content to load
        console.log(`[Scraper] Waiting ${waitMs}ms for content to load...`);
        await sleep(waitMs);

        // --- Try to click play buttons ---
        const playSelectors = [
            '.play-btn', '.btn-play', '.play-button',
            '[class*="play"]', '[class*="Play"]',
            'button[title*="Play"]', 'button[title*="play"]',
            '.vjs-big-play-button',  // Video.js
            '.ytp-large-play-button', // YouTube
            '.plyr__control--overlaid', // Plyr
            'video'
        ];

        for (const selector of playSelectors) {
            try {
                await page.click(selector);
                console.log(`[Scraper] Clicked: ${selector}`);
                await sleep(3000); // Wait after clicking play
                break;
            } catch {
                // Selector not found, try next
            }
        }

        // --- Extract video src from DOM ---
        const domStreams = await page.evaluate((patterns) => {
            const results = [];

            // Check <video> and <source> elements
            document.querySelectorAll('video, video source').forEach(el => {
                const src = el.src || el.getAttribute('src') || el.getAttribute('data-src');
                if (src && !src.startsWith('blob:')) {
                    results.push({ url: src, type: src.includes('.m3u8') ? 'hls' : 'mp4', source: 'dom-video' });
                }
            });

            // Check <iframe> elements
            document.querySelectorAll('iframe').forEach(el => {
                const src = el.src || el.getAttribute('src');
                if (src && (src.includes('embed') || src.includes('player') || src.includes('video'))) {
                    results.push({ url: src, type: 'iframe', source: 'dom-iframe' });
                }
            });

            // Check data attributes
            document.querySelectorAll('[data-video-src], [data-stream-url], [data-src]').forEach(el => {
                const src = el.getAttribute('data-video-src') ||
                           el.getAttribute('data-stream-url') ||
                           el.getAttribute('data-src');
                if (src) {
                    results.push({ url: src, type: 'mp4', source: 'dom-data' });
                }
            });

            // Check for any script tags containing stream URLs
            document.querySelectorAll('script').forEach(el => {
                const text = el.textContent || '';
                patterns.forEach(pattern => {
                    // pattern is already a regex source string from .source,
                    // use it directly — do NOT re-escape
                    try {
                        const regex = new RegExp(pattern, 'gi');
                        let match;
                        while ((match = regex.exec(text)) !== null) {
                            // Extract the full URL from the match context
                            const urlMatch = text.substring(Math.max(0, match.index - 100), match.index + match[0].length + 100);
                            const urlRegex = /(https?:\/\/[^\s"'<>]+(?:\.m3u8|\.mp4|\.ts|\.mkv)[^\s"'<>]*)/i;
                            const urlResult = urlRegex.exec(urlMatch);
                            if (urlResult) {
                                results.push({ url: urlResult[1], type: 'mp4', source: 'dom-script' });
                            }
                        }
                    } catch {
                        // Invalid regex pattern, skip
                    }
                });
            });

            return results;
        }, STREAM_PATTERNS.map(p => p.source));

        discoveredStreams.push(...domStreams);

        // --- Deduplicate ---
        const uniqueStreams = [...new Map(
            discoveredStreams
                .filter(s => s.url && !s.url.startsWith('blob:'))
                .map(s => [s.url, s])
        ).values()];

        console.log(`[Scraper] Total streams found: ${uniqueStreams.length} for ${pageUrl}`);

        if (uniqueStreams.length > 0) {
            scrapeCache.set(cacheKey, uniqueStreams);
        }

        return uniqueStreams;

    } catch (err) {
        console.error(`[Scraper] Error scraping ${pageUrl}:`, err.message);
        return [];
    } finally {
        if (browser) {
            try {
                await browser.close();
            } catch {
                // Browser might already be closed
            }
        }
    }
}

/**
 * Classifies a stream URL by its type.
 */
function getStreamType(url) {
    if (url.includes('.m3u8')) return 'hls';
    if (url.includes('.mpd')) return 'dash';
    if (url.includes('.mp4')) return 'mp4';
    if (url.includes('.mkv')) return 'mkv';
    if (url.includes('.ts')) return 'ts';
    return 'unknown';
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

module.exports = { extractStreams, scrapeCache, getStreamType };

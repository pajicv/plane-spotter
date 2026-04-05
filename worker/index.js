const TOKEN_URL = 'https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token';
const API_BASE = 'https://opensky-network.org/api/states/all';

let cachedToken = null;
let tokenExpiry = 0;

async function getToken(env) {
  if (cachedToken && Date.now() < tokenExpiry - 30000) return cachedToken;

  try {
    const res = await fetch(TOKEN_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'client_credentials',
        client_id: env.OPENSKY_CLIENT_ID,
        client_secret: env.OPENSKY_CLIENT_SECRET,
      }),
    });

    if (!res.ok) {
      console.error('Token fetch failed:', res.status);
      return null;
    }

    const data = await res.json();
    cachedToken = data.access_token;
    tokenExpiry = Date.now() + data.expires_in * 1000;
    return cachedToken;
  } catch (e) {
    console.error('Token fetch error:', e);
    return null;
  }
}

function corsHeaders(env) {
  return {
    'Access-Control-Allow-Origin': env.ALLOWED_ORIGIN,
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
  };
}

export default {
  async fetch(request, env) {
    // Handle CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders(env) });
    }

    const url = new URL(request.url);

    // Only allow /states endpoint
    if (url.pathname !== '/states') {
      return new Response('Not found', { status: 404, headers: corsHeaders(env) });
    }

    // Forward query params to OpenSky
    const apiUrl = API_BASE + url.search;

    // Try authenticated first, fall back to unauthenticated
    const token = await getToken(env);
    const fetchOpts = token
      ? { headers: { 'Authorization': 'Bearer ' + token } }
      : {};

    let res = await fetch(apiUrl, fetchOpts);

    // On 401, retry unauthenticated
    if (res.status === 401) {
      cachedToken = null;
      res = await fetch(apiUrl);
    }

    return new Response(res.body, {
      status: res.status,
      headers: {
        'Content-Type': 'application/json',
        ...corsHeaders(env),
      },
    });
  },
};

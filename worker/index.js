const TOKEN_URL = 'https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token';
const API_BASE = 'https://opensky-network.org/api/states/all';

let cachedToken = null;
let tokenExpiry = 0;

async function getToken(env) {
  if (cachedToken && Date.now() < tokenExpiry - 30000) return cachedToken;

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

    const fetchOpts = {};
    const token = await getToken(env);
    if (token) {
      fetchOpts.headers = { 'Authorization': 'Bearer ' + token };
    }

    const res = await fetch(apiUrl, fetchOpts);

    // Retry once with fresh token on 401
    if (res.status === 401 && token) {
      cachedToken = null;
      const newToken = await getToken(env);
      if (newToken) {
        fetchOpts.headers = { 'Authorization': 'Bearer ' + newToken };
        const retry = await fetch(apiUrl, fetchOpts);
        return new Response(retry.body, {
          status: retry.status,
          headers: { ...Object.fromEntries(retry.headers), ...corsHeaders(env) },
        });
      }
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

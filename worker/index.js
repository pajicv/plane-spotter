const API_BASE = 'https://opensky-network.org/api/states/all';

function corsHeaders(env) {
  return {
    'Access-Control-Allow-Origin': env.ALLOWED_ORIGIN,
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
  };
}

export default {
  async fetch(request, env) {
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders(env) });
    }

    const url = new URL(request.url);
    if (url.pathname !== '/states') {
      return new Response('Not found', { status: 404, headers: corsHeaders(env) });
    }

    const apiUrl = API_BASE + url.search;
    const fetchOpts = {
      headers: {
        'Authorization': 'Basic ' + btoa(env.OPENSKY_USER + ':' + env.OPENSKY_PASS),
      },
    };

    const res = await fetch(apiUrl, fetchOpts);

    return new Response(res.body, {
      status: res.status,
      headers: {
        'Content-Type': 'application/json',
        ...corsHeaders(env),
      },
    });
  },
};

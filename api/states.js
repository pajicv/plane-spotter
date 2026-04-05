export default async function handler(req, res) {
  const allowedOrigin = process.env.ALLOWED_ORIGIN || 'https://pajicv.github.io';

  res.setHeader('Access-Control-Allow-Origin', allowedOrigin);
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(204).end();
  }

  const { lat, lon, dist } = req.query;
  if (!lat || !lon || !dist) {
    return res.status(400).json({ error: 'Missing lat, lon, or dist' });
  }

  const apiUrl = `https://api.adsb.lol/v2/lat/${lat}/lon/${lon}/dist/${dist}`;

  try {
    const response = await fetch(apiUrl);
    const data = await response.text();
    res.setHeader('Content-Type', 'application/json');
    return res.status(response.status).send(data);
  } catch (e) {
    return res.status(502).json({ error: 'Failed to reach ADSB.lol', detail: e.message });
  }
}

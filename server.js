// server.js
const express = require('express');
const fetch = require('node-fetch');
const path = require('path');
const app = express();

const FOOTBALL_KEY = '81e164bfa4364ff783bc397c30f39627';
const FOOTBALL_BASE = 'https://api.football-data.org/v4';
const NEWS_KEY = 'pub_7d7d1ac2f86b4bc6b4662fd5d6dad47c';
const NEWS_BASE = 'https://newsdata.io/api/1';

app.use(express.static(path.join(__dirname, 'public')));

// Proxy live matches
app.get('/api/matches/live', async (req, res) => {
  try {
    const r = await fetch(`${FOOTBALL_BASE}/matches?status=LIVE,IN_PLAY,PAUSED`, {
      headers: { 'X-Auth-Token': FOOTBALL_KEY }
    });
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Proxy today matches
app.get('/api/matches/today', async (req, res) => {
  try {
    const today = new Date().toISOString().split('T')[0];
    const r = await fetch(`${FOOTBALL_BASE}/matches?dateFrom=${today}&dateTo=${today}`, {
      headers: { 'X-Auth-Token': FOOTBALL_KEY }
    });
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Proxy matches by date
app.get('/api/matches/date/:date', async (req, res) => {
  try {
    const { date } = req.params;
    const r = await fetch(`${FOOTBALL_BASE}/matches?dateFrom=${date}&dateTo=${date}`, {
      headers: { 'X-Auth-Token': FOOTBALL_KEY }
    });
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Proxy teams
app.get('/api/teams', async (req, res) => {
  try {
    const r = await fetch(`${FOOTBALL_BASE}/teams?limit=4`, {
      headers: { 'X-Auth-Token': FOOTBALL_KEY }
    });
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Proxy news
app.get('/api/news', async (req, res) => {
  try {
    const r = await fetch(`${NEWS_BASE}/news?apikey=${NEWS_KEY}&q=football&language=en&category=sports&size=5`);
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Keep-alive ping para evitar adormecimento no Render free tier
app.get('/ping', (req, res) => res.send('pong'));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
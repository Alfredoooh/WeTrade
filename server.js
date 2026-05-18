// server.js
const express = require('express');
const fetch = require('node-fetch');
const cors = require('cors');
const app = express();

const FOOTBALL_KEY = '79816a0c124c78b025aea3b79a8bb5dd';
const FOOTBALL_BASE = 'https://v3.football.api-sports.io';
const NEWS_KEY = 'pub_7d7d1ac2f86b4bc6b4662fd5d6dad47c';
const NEWS_BASE = 'https://newsdata.io/api/1';

app.use(cors());
app.use(express.json());

// Cache em memória
const cache = {
  news: { data: [], lastFetch: 0 },
  liveMatches: { data: [], lastFetch: 0 },
  todayMatches: { data: [], lastFetch: 0 },
};

const NEWS_TTL = 30 * 60 * 1000; // 30 minutos
const MATCHES_TTL = 60 * 1000; // 1 minuto

app.get('/ping', (req, res) => res.send('pong'));

const footballHeaders = { 'x-apisports-key': FOOTBALL_KEY };

// Top league IDs
const TOP_LEAGUES = [2, 3, 39, 40, 45, 48, 61, 62, 71, 78, 79, 88, 94, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 135, 140, 143, 144, 197, 203, 207, 218, 235, 253, 262, 281, 283, 286, 307, 318, 332, 333, 848];

// Jogos ao vivo
app.get('/api/matches/live', async (req, res) => {
  try {
    const now = Date.now();
    if (now - cache.liveMatches.lastFetch < MATCHES_TTL && cache.liveMatches.data.length > 0) {
      return res.json({ response: cache.liveMatches.data });
    }
    const r = await fetch(`${FOOTBALL_BASE}/fixtures?live=all`, { headers: footballHeaders });
    const data = await r.json();
    cache.liveMatches.data = data.response || [];
    cache.liveMatches.lastFetch = now;
    res.json(data);
  } catch (e) {
    res.json({ response: cache.liveMatches.data });
  }
});

// Jogos de hoje
app.get('/api/matches/today', async (req, res) => {
  try {
    const now = Date.now();
    if (now - cache.todayMatches.lastFetch < MATCHES_TTL && cache.todayMatches.data.length > 0) {
      return res.json({ response: cache.todayMatches.data });
    }
    const today = new Date().toISOString().split('T')[0];
    const r = await fetch(`${FOOTBALL_BASE}/fixtures?date=${today}`, { headers: footballHeaders });
    const data = await r.json();
    cache.todayMatches.data = data.response || [];
    cache.todayMatches.lastFetch = now;
    res.json(data);
  } catch (e) {
    res.json({ response: cache.todayMatches.data });
  }
});

// Jogos por data
app.get('/api/matches/date/:date', async (req, res) => {
  try {
    const { date } = req.params;
    const r = await fetch(`${FOOTBALL_BASE}/fixtures?date=${date}`, { headers: footballHeaders });
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Detalhes de um jogo
app.get('/api/match/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const [fixtureRes, eventsRes, statsRes, lineupsRes] = await Promise.all([
      fetch(`${FOOTBALL_BASE}/fixtures?id=${id}`, { headers: footballHeaders }),
      fetch(`${FOOTBALL_BASE}/fixtures/events?fixture=${id}`, { headers: footballHeaders }),
      fetch(`${FOOTBALL_BASE}/fixtures/statistics?fixture=${id}`, { headers: footballHeaders }),
      fetch(`${FOOTBALL_BASE}/fixtures/lineups?fixture=${id}`, { headers: footballHeaders }),
    ]);
    const [fixture, events, stats, lineups] = await Promise.all([
      fixtureRes.json(), eventsRes.json(), statsRes.json(), lineupsRes.json()
    ]);
    res.json({
      fixture: fixture.response?.[0] || null,
      events: events.response || [],
      stats: stats.response || [],
      lineups: lineups.response || [],
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Ligas top
app.get('/api/leagues', async (req, res) => {
  try {
    const r = await fetch(`${FOOTBALL_BASE}/leagues?current=true`, { headers: footballHeaders });
    const data = await r.json();
    const topLeagues = (data.response || []).filter(l =>
      TOP_LEAGUES.includes(l.league?.id)
    ).slice(0, 20);
    res.json({ response: topLeagues });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Jogos por liga
app.get('/api/league/:id/matches', async (req, res) => {
  try {
    const { id } = req.params;
    const today = new Date().toISOString().split('T')[0];
    const r = await fetch(`${FOOTBALL_BASE}/fixtures?league=${id}&season=2025&date=${today}`, { headers: footballHeaders });
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Standings por liga
app.get('/api/standings/:leagueId', async (req, res) => {
  try {
    const { leagueId } = req.params;
    const r = await fetch(`${FOOTBALL_BASE}/standings?league=${leagueId}&season=2025`, { headers: footballHeaders });
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Notícias com cache persistente da semana
app.get('/api/news', async (req, res) => {
  try {
    const now = Date.now();
    if (now - cache.news.lastFetch < NEWS_TTL && cache.news.data.length > 0) {
      return res.json({ results: cache.news.data });
    }
    
    const today = new Date();
    const weekAgo = new Date(today);
    weekAgo.setDate(today.getDate() - 7);
    const fromDate = weekAgo.toISOString().split('T')[0];
    
    const r = await fetch(
      `${NEWS_BASE}/news?apikey=${NEWS_KEY}&q=football OR soccer&language=en&category=sports&from_date=${fromDate}&size=20`,
    );
    const data = await r.json();
    const articles = (data.results || []).filter(a => a.image_url && a.description);
    
    if (articles.length > 0) {
      // Merge com cache existente, sem duplicados
      const existingIds = new Set(cache.news.data.map(a => a.article_id));
      const newArticles = articles.filter(a => !existingIds.has(a.article_id));
      cache.news.data = [...newArticles, ...cache.news.data].slice(0, 50);
      cache.news.lastFetch = now;
    }
    
    res.json({ results: cache.news.data });
  } catch (e) {
    // Devolve cache mesmo com erro
    res.json({ results: cache.news.data });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
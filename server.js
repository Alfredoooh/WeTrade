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

const cache = {
  news: { data: [], lastFetch: 0 },
  liveMatches: { data: [], lastFetch: 0 },
  todayMatches: { data: [], lastFetch: 0 },
  leagues: { data: [], lastFetch: 0 },
  standings: {},
  teamInfo: {},
  teamFixtures: {},
  teamStanding: {},
};

const NEWS_TTL = 30 * 60 * 1000;
const MATCHES_TTL = 60 * 1000;
const LEAGUES_TTL = 60 * 60 * 1000;
const STANDINGS_TTL = 10 * 60 * 1000;
const TEAM_TTL = 30 * 60 * 1000;

const footballHeaders = { 'x-apisports-key': FOOTBALL_KEY };

const TOP_LEAGUES = [
  2, 3, 39, 40, 45, 48, 61, 62, 71, 78, 79, 88, 94, 135, 140, 143, 144,
  197, 203, 207, 218, 235, 253, 262, 281, 283, 307, 332, 333, 848
];

const CURRENT_SEASONS = [2025, 2024];

async function footballFetch(url) {
  const r = await fetch(url, { headers: footballHeaders });
  return r.json();
}

async function footballFetchWithSeasonFallback(buildUrl) {
  for (const season of CURRENT_SEASONS) {
    try {
      const data = await footballFetch(buildUrl(season));
      const resp = data.response || [];
      if (resp.length > 0) return { data, season };
    } catch (e) {}
  }
  return { data: { response: [] }, season: CURRENT_SEASONS[0] };
}

app.get('/ping', (req, res) => res.send('pong'));

// Live
app.get('/api/matches/live', async (req, res) => {
  try {
    const now = Date.now();
    if (now - cache.liveMatches.lastFetch < MATCHES_TTL && cache.liveMatches.data.length > 0)
      return res.json({ response: cache.liveMatches.data });
    const data = await footballFetch(`${FOOTBALL_BASE}/fixtures?live=all`);
    cache.liveMatches.data = data.response || [];
    cache.liveMatches.lastFetch = now;
    res.json(data);
  } catch (e) {
    res.json({ response: cache.liveMatches.data });
  }
});

// Today
app.get('/api/matches/today', async (req, res) => {
  try {
    const now = Date.now();
    if (now - cache.todayMatches.lastFetch < MATCHES_TTL && cache.todayMatches.data.length > 0)
      return res.json({ response: cache.todayMatches.data });
    const today = new Date().toISOString().split('T')[0];
    const data = await footballFetch(`${FOOTBALL_BASE}/fixtures?date=${today}`);
    cache.todayMatches.data = data.response || [];
    cache.todayMatches.lastFetch = now;
    res.json(data);
  } catch (e) {
    res.json({ response: cache.todayMatches.data });
  }
});

// By date
app.get('/api/matches/date/:date', async (req, res) => {
  try {
    const data = await footballFetch(`${FOOTBALL_BASE}/fixtures?date=${req.params.date}`);
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Match detail
app.get('/api/match/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const [fixture, events, stats, lineups] = await Promise.all([
      footballFetch(`${FOOTBALL_BASE}/fixtures?id=${id}`),
      footballFetch(`${FOOTBALL_BASE}/fixtures/events?fixture=${id}`),
      footballFetch(`${FOOTBALL_BASE}/fixtures/statistics?fixture=${id}`),
      footballFetch(`${FOOTBALL_BASE}/fixtures/lineups?fixture=${id}`),
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

// Leagues
app.get('/api/leagues', async (req, res) => {
  try {
    const now = Date.now();
    if (now - cache.leagues.lastFetch < LEAGUES_TTL && cache.leagues.data.length > 0)
      return res.json({ response: cache.leagues.data });

    // Try current=true first, then fallback to full list filtered
    let data = await footballFetch(`${FOOTBALL_BASE}/leagues?current=true`);
    let leagues = (data.response || []).filter(l => TOP_LEAGUES.includes(l.league?.id));

    if (leagues.length < 10) {
      // fallback: fetch all leagues without current filter
      data = await footballFetch(`${FOOTBALL_BASE}/leagues`);
      leagues = (data.response || []).filter(l => TOP_LEAGUES.includes(l.league?.id));
    }

    cache.leagues.data = leagues;
    cache.leagues.lastFetch = now;
    res.json({ response: leagues });
  } catch (e) {
    res.json({ response: cache.leagues.data });
  }
});

// Standings with season fallback
app.get('/api/standings/:leagueId', async (req, res) => {
  try {
    const { leagueId } = req.params;
    const cacheKey = leagueId;
    const now = Date.now();
    if (cache.standings[cacheKey] && now - cache.standings[cacheKey].lastFetch < STANDINGS_TTL)
      return res.json(cache.standings[cacheKey].data);

    const { data } = await footballFetchWithSeasonFallback(
      season => `${FOOTBALL_BASE}/standings?league=${leagueId}&season=${season}`
    );
    cache.standings[cacheKey] = { data, lastFetch: now };
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Team info
app.get('/api/team/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const now = Date.now();
    if (cache.teamInfo[id] && now - cache.teamInfo[id].lastFetch < TEAM_TTL)
      return res.json(cache.teamInfo[id].data);
    const data = await footballFetch(`${FOOTBALL_BASE}/teams?id=${id}`);
    cache.teamInfo[id] = { data, lastFetch: now };
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Team fixtures (last 5 + next 5) with season fallback
app.get('/api/team/:id/fixtures', async (req, res) => {
  try {
    const { id } = req.params;
    const now = Date.now();
    if (cache.teamFixtures[id] && now - cache.teamFixtures[id].lastFetch < TEAM_TTL)
      return res.json(cache.teamFixtures[id].data);

    const { data: last, season } = await footballFetchWithSeasonFallback(
      s => `${FOOTBALL_BASE}/fixtures?team=${id}&season=${s}&last=10`
    );
    const { data: next } = await footballFetchWithSeasonFallback(
      s => `${FOOTBALL_BASE}/fixtures?team=${id}&season=${s}&next=5`
    );

    const combined = [
      ...(next.response || []).reverse(),
      ...(last.response || []),
    ];
    // dedupe by fixture id
    const seen = new Set();
    const deduped = combined.filter(m => {
      const fid = m.fixture?.id;
      if (seen.has(fid)) return false;
      seen.add(fid);
      return true;
    });

    const result = { response: deduped };
    cache.teamFixtures[id] = { data: result, lastFetch: now };
    res.json(result);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Team standing (which position is this team in their league)
app.get('/api/team/:id/standing', async (req, res) => {
  try {
    const { id } = req.params;
    const now = Date.now();
    if (cache.teamStanding[id] && now - cache.teamStanding[id].lastFetch < STANDINGS_TTL)
      return res.json(cache.teamStanding[id].data);

    const { data } = await footballFetchWithSeasonFallback(
      s => `${FOOTBALL_BASE}/standings?team=${id}&season=${s}`
    );
    cache.teamStanding[id] = { data, lastFetch: now };
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Team statistics
app.get('/api/team/:id/statistics/:leagueId', async (req, res) => {
  try {
    const { id, leagueId } = req.params;
    const { data, season } = await footballFetchWithSeasonFallback(
      s => `${FOOTBALL_BASE}/teams/statistics?team=${id}&league=${leagueId}&season=${s}`
    );
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Team players
app.get('/api/team/:id/players', async (req, res) => {
  try {
    const { id } = req.params;
    const { data } = await footballFetchWithSeasonFallback(
      s => `${FOOTBALL_BASE}/players?team=${id}&season=${s}`
    );
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Search teams
app.get('/api/teams/search', async (req, res) => {
  try {
    const { q } = req.query;
    if (!q) return res.json({ response: [] });
    const data = await footballFetch(`${FOOTBALL_BASE}/teams?search=${encodeURIComponent(q)}`);
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// League matches (today + next days)
app.get('/api/matches/league/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { data } = await footballFetchWithSeasonFallback(
      s => `${FOOTBALL_BASE}/fixtures?league=${id}&season=${s}&next=20`
    );
    if ((data.response || []).length === 0) {
      // fallback: last 10
      const { data: last } = await footballFetchWithSeasonFallback(
        s => `${FOOTBALL_BASE}/fixtures?league=${id}&season=${s}&last=10`
      );
      return res.json(last);
    }
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// News — multiple queries for more results
app.get('/api/news', async (req, res) => {
  try {
    const now = Date.now();
    if (now - cache.news.lastFetch < NEWS_TTL && cache.news.data.length > 0)
      return res.json({ results: cache.news.data });

    const queries = ['football', 'soccer', 'Champions League', 'Premier League', 'La Liga'];
    const results = await Promise.allSettled(
      queries.map(q =>
        fetch(`${NEWS_BASE}/news?apikey=${NEWS_KEY}&q=${encodeURIComponent(q)}&language=en&category=sports&size=10`)
          .then(r => r.json())
          .catch(() => ({ results: [] }))
      )
    );

    const allArticles = [];
    const seen = new Set();
    for (const r of results) {
      if (r.status === 'fulfilled') {
        for (const a of (r.value.results || [])) {
          if (a.article_id && !seen.has(a.article_id) && a.image_url) {
            seen.add(a.article_id);
            allArticles.push(a);
          }
        }
      }
    }

    if (allArticles.length > 0) {
      cache.news.data = allArticles.slice(0, 60);
      cache.news.lastFetch = now;
    }

    res.json({ results: cache.news.data });
  } catch (e) {
    res.json({ results: cache.news.data });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
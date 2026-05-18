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

app.get('/ping', (req, res) => res.send('pong'));

// Headers para API-Football
const footballHeaders = {
  'x-apisports-key': FOOTBALL_KEY
};

// Jogos ao vivo
app.get('/api/matches/live', async (req, res) => {
  try {
    const r = await fetch(`${FOOTBALL_BASE}/fixtures?live=all`, {
      headers: footballHeaders
    });
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Jogos de hoje
app.get('/api/matches/today', async (req, res) => {
  try {
    const today = new Date().toISOString().split('T')[0];
    const r = await fetch(`${FOOTBALL_BASE}/fixtures?date=${today}`, {
      headers: footballHeaders
    });
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Jogos por data
app.get('/api/matches/date/:date', async (req, res) => {
  try {
    const { date } = req.params;
    const r = await fetch(`${FOOTBALL_BASE}/fixtures?date=${date}`, {
      headers: footballHeaders
    });
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Notícias
app.get('/api/news', async (req, res) => {
  try {
    const r = await fetch(`${NEWS_BASE}/news?apikey=${NEWS_KEY}&q=football&language=en&category=sports&size=5`);
    const data = await r.json();
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
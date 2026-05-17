// football-data.org free tier API
// Get your free key at https://www.football-data.org/client/register
const API_KEY = '81e164bfa4364ff783bc397c30f39627';
const BASE = 'https://api.football-data.org/v4';

const headers = {
  'X-Auth-Token': API_KEY
};

async function get(path) {
  try {
    const res = await fetch(`${BASE}${path}`, { headers });
    if (!res.ok) throw new Error(`API error ${res.status}`);
    return await res.json();
  } catch (e) {
    console.error('API error:', e);
    return null;
  }
}

// Competitions available on free tier
export const COMPETITIONS = {
  CL: { id: 'CL', name: 'Champions League', flag: '🏆' },
  PL: { id: 'PL', name: 'Premier League', flag: '🏴󠁧󠁢󠁥󠁮󠁧󠁿' },
  PD: { id: 'PD', name: 'La Liga', flag: '🇪🇸' },
  BL1: { id: 'BL1', name: 'Bundesliga', flag: '🇩🇪' },
  SA: { id: 'SA', name: 'Serie A', flag: '🇮🇹' },
  FL1: { id: 'FL1', name: 'Ligue 1', flag: '🇫🇷' },
  PPL: { id: 'PPL', name: 'Liga Portugal', flag: '🇵🇹' },
  DED: { id: 'DED', name: 'Eredivisie', flag: '🇳🇱' },
  BSA: { id: 'BSA', name: 'Brasileirão', flag: '🇧🇷' },
  ELC: { id: 'ELC', name: 'Championship', flag: '🏴󠁧󠁢󠁥󠁮󠁧󠁿' },
  EC: { id: 'EC', name: 'Europeu', flag: '🌍' },
  WC: { id: 'WC', name: 'Mundial FIFA', flag: '🌍' },
};

export async function getMatches(competitionId, dateFrom, dateTo) {
  const params = new URLSearchParams();
  if (dateFrom) params.append('dateFrom', dateFrom);
  if (dateTo) params.append('dateTo', dateTo);
  return get(`/competitions/${competitionId}/matches?${params}`);
}

export async function getStandings(competitionId) {
  return get(`/competitions/${competitionId}/standings`);
}

export async function getCompetition(competitionId) {
  return get(`/competitions/${competitionId}`);
}

export async function getTeam(teamId) {
  return get(`/teams/${teamId}`);
}

export async function getTeamMatches(teamId) {
  return get(`/teams/${teamId}/matches?status=SCHEDULED&limit=10`);
}

export async function getPerson(personId) {
  return get(`/persons/${personId}`);
}

export async function getLiveMatches() {
  return get('/matches?status=LIVE');
}

export async function getTodayMatches() {
  const today = new Date().toISOString().split('T')[0];
  return get(`/matches?dateFrom=${today}&dateTo=${today}`);
}

export async function getTopScorers(competitionId) {
  return get(`/competitions/${competitionId}/scorers?limit=20`);
}

export async function searchTeams(query) {
  // football-data.org doesn't have search — filter from known competitions
  return null;
}

// Format date helper
export function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('pt-PT', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function formatTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' });
}

export function formatMinute(match) {
  if (match.status === 'LIVE' && match.minute) return `${match.minute}'`;
  if (match.status === 'HALFTIME') return 'Int.';
  if (match.status === 'FINISHED') return 'FT';
  return formatTime(match.utcDate);
}

export function getScore(match) {
  if (!match.score || match.score.fullTime.home === null) return null;
  return { home: match.score.fullTime.home, away: match.score.fullTime.away };
}
const API_KEY = '81e164bfa4364ff783bc397c30f39627';
const BASE = 'https://api.football-data.org/v4';

async function get(path) {
  try {
    const res = await fetch(`${BASE}${path}`, {
      headers: { 'X-Auth-Token': API_KEY }
    });
    if (!res.ok) return null;
    return await res.json();
  } catch (e) {
    console.error('API error:', e);
    return null;
  }
}

export const COMPETITIONS = {
  CL: { id: 'CL', name: 'Champions League' },
  PL: { id: 'PL', name: 'Premier League' },
  PD: { id: 'PD', name: 'La Liga' },
  BL1: { id: 'BL1', name: 'Bundesliga' },
  SA: { id: 'SA', name: 'Serie A' },
  FL1: { id: 'FL1', name: 'Ligue 1' },
  PPL: { id: 'PPL', name: 'Liga Portugal' },
  DED: { id: 'DED', name: 'Eredivisie' },
  BSA: { id: 'BSA', name: 'Brasileirao' },
  ELC: { id: 'ELC', name: 'Championship' },
  EC: { id: 'EC', name: 'Europeu' },
  WC: { id: 'WC', name: 'Mundial FIFA' },
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

export function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('pt-PT', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function formatTime(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' });
}

export function getScore(match) {
  if (!match.score || match.score.fullTime.home === null) return null;
  return { home: match.score.fullTime.home, away: match.score.fullTime.away };
}
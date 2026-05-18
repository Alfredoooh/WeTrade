const API_KEY = '81e164bfa4364ff783bc397c30f39627';
const BASE = 'https://api.football-data.org/v4';

async function get(path) {
  try {
    const res = await fetch(`${BASE}${path}`, {
      headers: { 'X-Auth-Token': API_KEY },
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  } catch (e) {
    console.warn('[API]', e.message);
    return null;
  }
}

export const COMPETITIONS = {
  CL: { id: 'CL', name: 'Champions League', area: 'Europa' },
  PL: { id: 'PL', name: 'Premier League', area: 'Inglaterra' },
  PD: { id: 'PD', name: 'La Liga', area: 'Espanha' },
  BL1: { id: 'BL1', name: 'Bundesliga', area: 'Alemanha' },
  SA: { id: 'SA', name: 'Serie A', area: 'Itália' },
  FL1: { id: 'FL1', name: 'Ligue 1', area: 'França' },
  PPL: { id: 'PPL', name: 'Liga Portugal', area: 'Portugal' },
  DED: { id: 'DED', name: 'Eredivisie', area: 'Holanda' },
  BSA: { id: 'BSA', name: 'Brasileirão', area: 'Brasil' },
  ELC: { id: 'ELC', name: 'Championship', area: 'Inglaterra' },
};

const fmt = d => d.toISOString().split('T')[0];

export const getLiveMatches = () => get('/matches?status=LIVE');
export const getTodayMatches = () => {
  const t = fmt(new Date());
  return get(`/matches?dateFrom=${t}&dateTo=${t}`);
};
export const getMatches = (compId, from, to) =>
  get(`/competitions/${compId}/matches?dateFrom=${from}&dateTo=${to}`);

export function formatTime(utcDate) {
  if (!utcDate) return '';
  return new Date(utcDate).toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' });
}
export function formatDate(utcDate) {
  if (!utcDate) return '';
  return new Date(utcDate).toLocaleDateString('pt-PT', { day: '2-digit', month: '2-digit' });
}
export function isLive(m) {
  return m.status === 'IN_PLAY' || m.status === 'PAUSED' || m.status === 'HALFTIME';
}
export function getStatusLabel(m) {
  if (m.status === 'IN_PLAY' || m.status === 'PAUSED') return `${m.minute ?? ''}'`;
  if (m.status === 'HALFTIME') return 'Int.';
  if (m.status === 'FINISHED') return 'FT';
  return formatTime(m.utcDate);
}
export function getScore(m) {
  const s = m.score?.fullTime;
  if (!s || s.home === null) return null;
  return { home: s.home, away: s.away };
}
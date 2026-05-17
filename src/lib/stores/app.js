import { writable } from 'svelte/store';

export const drawerOpen = writable(false);
export const favorites = writable({ teams: [], competitions: [], players: [] });
export const notifications = writable([
  { id: 1, text: 'Benfica marcou! 1-0 vs Porto', time: '2m', read: false },
  { id: 2, text: 'Início em 15min: Real Madrid vs Barcelona', time: '13m', read: false },
  { id: 3, text: 'Resultado: Man City 2-1 Arsenal', time: '1h', read: true },
]);

export function toggleDrawer() {
  drawerOpen.update(v => !v);
}

export function closeDrawer() {
  drawerOpen.set(false);
}

export function toggleFavoriteTeam(team) {
  favorites.update(f => {
    const exists = f.teams.find(t => t.id === team.id);
    return {
      ...f,
      teams: exists ? f.teams.filter(t => t.id !== team.id) : [...f.teams, team]
    };
  });
}

export function toggleFavoriteComp(comp) {
  favorites.update(f => {
    const exists = f.competitions.find(c => c.id === comp.id);
    return {
      ...f,
      competitions: exists ? f.competitions.filter(c => c.id !== comp.id) : [...f.competitions, comp]
    };
  });
}
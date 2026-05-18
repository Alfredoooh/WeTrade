import React, { createContext, useContext, useReducer } from 'react';

const init = {
  favorites: { teams: [], competitions: [] },
  drawerOpen: false,
  notifications: [
    { id: 1, text: 'Benfica marcou! 1-0 vs Porto',              time: '2m',  read: false },
    { id: 2, text: 'Início em 15min: Real Madrid vs Barcelona', time: '13m', read: false },
    { id: 3, text: 'Resultado: Man City 2-1 Arsenal',           time: '1h',  read: true  },
  ],
};

function reducer(state, action) {
  switch (action.type) {
    case 'DRAWER_OPEN':  return { ...state, drawerOpen: true };
    case 'DRAWER_CLOSE': return { ...state, drawerOpen: false };
    case 'TOGGLE_FAV_COMP': {
      const comps = state.favorites.competitions;
      const exists = comps.find(c => c.id === action.comp.id);
      return { ...state, favorites: { ...state.favorites, competitions: exists ? comps.filter(c => c.id !== action.comp.id) : [...comps, action.comp] } };
    }
    case 'TOGGLE_FAV_TEAM': {
      const teams = state.favorites.teams;
      const exists = teams.find(t => t.id === action.team.id);
      return { ...state, favorites: { ...state.favorites, teams: exists ? teams.filter(t => t.id !== action.team.id) : [...teams, action.team] } };
    }
    default: return state;
  }
}

const Ctx = createContext(null);
export function StoreProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, init);
  return <Ctx.Provider value={{ state, dispatch }}>{children}</Ctx.Provider>;
}
export const useStore = () => useContext(Ctx);
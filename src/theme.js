export const light = {
  bg: '#ffffff',
  bg2: '#f5f5f7',
  bg3: '#ebebed',
  border: '#e4e4e8',
  text: '#0a0a0f',
  textSub: '#52525f',
  textDim: '#a0a0b0',
  primary: '#0D5BFF',
  primarySoft: 'rgba(13,91,255,0.10)',
  danger: '#ef4444',
  dangerSoft: 'rgba(239,68,68,0.10)',
  success: '#16a34a',
  successSoft: 'rgba(22,163,74,0.10)',
  warn: '#d97706',
  warnSoft: 'rgba(217,119,6,0.10)',
  card: '#ffffff',
};

export const dark = {
  bg: '#111113',
  bg2: '#1c1c1f',
  bg3: '#26262a',
  border: '#2e2e33',
  text: '#f2f2f5',
  textSub: '#8e8e9a',
  textDim: '#4a4a58',
  primary: '#3b82f6',
  primarySoft: 'rgba(59,130,246,0.13)',
  danger: '#f87171',
  dangerSoft: 'rgba(248,113,113,0.13)',
  success: '#4ade80',
  successSoft: 'rgba(74,222,128,0.13)',
  warn: '#fbbf24',
  warnSoft: 'rgba(251,191,36,0.13)',
  card: '#1c1c1f',
};

import React, { createContext, useContext, useState, useEffect } from 'react';

const ThemeContext = createContext(null);

export function ThemeProvider({ children }) {
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const [isDark, setIsDark] = useState(prefersDark);
  
  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = e => setIsDark(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);
  
  const colors = isDark ? dark : light;
  const toggle = () => setIsDark(v => !v);
  
  return (
    <ThemeContext.Provider value={{ colors, isDark, toggle }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() { return useContext(ThemeContext); }
import React, { useState } from 'react';
import { View, StyleSheet, useColorScheme } from 'react-native';
import { ThemeProvider } from './theme';
import { StoreProvider } from './store';
import Drawer from './components/Drawer';
import BottomNav from './components/BottomNav';
import HomeScreen from './screens/HomeScreen';
import MatchesScreen from './screens/MatchesScreen';
import FavoritesScreen from './screens/FavoritesScreen';
import TVScreen from './screens/TVScreen';
import SearchScreen from './screens/SearchScreen';

const SCREENS = {
  home: HomeScreen,
  matches: MatchesScreen,
  favorites: FavoritesScreen,
  tv: TVScreen,
  search: SearchScreen,
};

export default function App() {
  const [tab, setTab] = useState('home');
  const Screen = SCREENS[tab] || HomeScreen;
  
  return (
    <StoreProvider>
      <ThemeProvider>
        <View style={s.root}>
          <Drawer onNavigate={setTab} />
          <View style={s.content}>
            <Screen navigate={setTab} />
          </View>
          <BottomNav active={tab} onPress={setTab} />
        </View>
      </ThemeProvider>
    </StoreProvider>
  );
}

const s = StyleSheet.create({
  root: {
    flex: 1,
    maxWidth: 480,
    alignSelf: 'center',
    width: '100%',
    height: '100%',
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
    position: 'relative',
  },
  content: { flex: 1, overflow: 'hidden' },
});
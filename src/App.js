import React, { useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { colors, spacing } from './theme';
import TabBar from './components/TabBar';
import HomeScreen     from './screens/HomeScreen';
import ExploreScreen  from './screens/ExploreScreen';
import AnalyticsScreen from './screens/AnalyticsScreen';
import ProfileScreen  from './screens/ProfileScreen';

const SCREENS = {
  home:      HomeScreen,
  explore:   ExploreScreen,
  analytics: AnalyticsScreen,
  profile:   ProfileScreen,
};

const TITLES = {
  home:      'Dashboard',
  explore:   'Explorar',
  analytics: 'Análises',
  profile:   'Perfil',
};

export default function App() {
  const [tab, setTab] = useState('home');
  const Screen = SCREENS[tab];

  return (
    <View style={s.root}>
      {/* Top bar */}
      <View style={s.topbar}>
        <View style={s.logo}>
          <View style={s.logoDot} />
          <Text style={s.logoText}>PULSE</Text>
        </View>
        <Text style={s.topTitle}>{TITLES[tab]}</Text>
        <View style={s.topRight}>
          <View style={s.notif}><Text style={s.notifText}>3</Text></View>
        </View>
      </View>

      {/* Active screen */}
      <View style={s.screen}>
        <Screen />
      </View>

      {/* Tab bar */}
      <TabBar active={tab} onPress={setTab} />
    </View>
  );
}

const s = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: colors.bg,
    maxWidth: 480,
    alignSelf: 'center',
    width: '100%',
    height: '100%',
    display: 'flex',
    flexDirection: 'column',
  },
  topbar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    flexShrink: 0,
  },
  logo: { flexDirection: 'row', alignItems: 'center', gap: 7, width: 70 },
  logoDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.neon },
  logoText: { fontSize: 12, fontWeight: '800', color: colors.text, letterSpacing: 2 },
  topTitle: { fontSize: 13, fontWeight: '700', color: colors.text, letterSpacing: 0.5 },
  topRight: { width: 70, alignItems: 'flex-end' },
  notif: { width: 22, height: 22, borderRadius: 11, backgroundColor: colors.accent, alignItems: 'center', justifyContent: 'center' },
  notifText: { fontSize: 10, fontWeight: '800', color: '#fff' },
  screen: { flex: 1, overflow: 'hidden' },
});

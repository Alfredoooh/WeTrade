import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Animated } from 'react-native';
import { RiHomeLine, RiHomeFill, RiCalendar2Line, RiCalendar2Fill, RiHeartLine, RiHeartFill, RiTvLine, RiTvFill } from 'react-icons/ri';
import { useTheme } from '../theme';

const TABS = [
  { id: 'home',      label: 'Início',   IconOff: RiHomeLine,      IconOn: RiHomeFill },
  { id: 'matches',   label: 'Partidas', IconOff: RiCalendar2Line, IconOn: RiCalendar2Fill },
  { id: 'favorites', label: 'Favoritos',IconOff: RiHeartLine,     IconOn: RiHeartFill },
  { id: 'tv',        label: 'TV',       IconOff: RiTvLine,        IconOn: RiTvFill },
];

export default function BottomNav({ active, onPress }) {
  const { colors } = useTheme();
  const s = styles(colors);

  return (
    <View style={s.nav}>
      {TABS.map(tab => {
        const isActive = active === tab.id;
        const Icon = isActive ? tab.IconOn : tab.IconOff;
        return (
          <TouchableOpacity key={tab.id} style={s.tab} onPress={() => onPress(tab.id)} activeOpacity={0.7}>
            <View style={s.iconWrap}>
              <Icon size={24} color={isActive ? colors.primary : colors.textDim} />
              {isActive && <View style={[s.dot, { backgroundColor: colors.primary }]} />}
            </View>
            <Text style={[s.label, { color: isActive ? colors.primary : colors.textDim }]}>
              {tab.label}
            </Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

const styles = (c) => StyleSheet.create({
  nav: {
    flexDirection: 'row',
    backgroundColor: c.bg,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: c.border,
    height: 60,
    flexShrink: 0,
  },
  tab: {
    flex: 1, alignItems: 'center', justifyContent: 'center', gap: 3,
  },
  iconWrap: { alignItems: 'center', position: 'relative' },
  dot: { width: 4, height: 4, borderRadius: 2, marginTop: 2 },
  label: { fontSize: 10, fontWeight: '600', letterSpacing: 0.2 },
});
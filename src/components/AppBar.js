import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { RiMenuLine, RiSearchLine, RiBellLine, RiArrowLeftLine } from 'react-icons/ri';
import { useTheme } from '../theme';
import { useStore } from '../store';

export default function AppBar({ title, showBack, onBack, navigate }) {
  const { colors } = useTheme();
  const { state, dispatch } = useStore();
  const s = styles(colors);
  const unread = state.notifications.filter(n => !n.read).length;

  return (
    <View style={s.bar}>
      {/* Left */}
      <View style={s.side}>
        {showBack ? (
          <TouchableOpacity style={s.iconBtn} onPress={onBack} activeOpacity={0.7}>
            <RiArrowLeftLine size={22} color={colors.text} />
          </TouchableOpacity>
        ) : (
          <TouchableOpacity style={s.iconBtn} onPress={() => dispatch({ type: 'DRAWER_OPEN' })} activeOpacity={0.7}>
            <RiMenuLine size={22} color={colors.text} />
          </TouchableOpacity>
        )}
      </View>

      {/* Center */}
      {title ? (
        <Text style={[s.title, { color: colors.text }]}>{title}</Text>
      ) : (
        <View style={s.logo}>
          <View style={[s.logoDot, { backgroundColor: colors.primary }]} />
          <Text style={[s.logoText, { color: colors.primary }]}>GoalZone</Text>
        </View>
      )}

      {/* Right */}
      <View style={[s.side, s.sideRight]}>
        <TouchableOpacity style={s.iconBtn} onPress={() => navigate('search')} activeOpacity={0.7}>
          <RiSearchLine size={22} color={colors.text} />
        </TouchableOpacity>
        <TouchableOpacity style={s.iconBtn} activeOpacity={0.7}>
          <RiBellLine size={22} color={colors.text} />
          {unread > 0 && (
            <View style={[s.badge, { backgroundColor: colors.danger }]}>
              <Text style={s.badgeText}>{unread}</Text>
            </View>
          )}
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = (c) => StyleSheet.create({
  bar: {
    height: 56,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 4,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: c.border,
    backgroundColor: c.bg,
    flexShrink: 0,
  },
  side: { flexDirection: 'row', alignItems: 'center', minWidth: 80 },
  sideRight: { justifyContent: 'flex-end' },
  iconBtn: {
    width: 44, height: 44, alignItems: 'center', justifyContent: 'center',
    borderRadius: 22, position: 'relative',
  },
  logo: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  logoDot: { width: 8, height: 8, borderRadius: 4 },
  logoText: { fontSize: 17, fontWeight: '800', letterSpacing: -0.3 },
  title: { fontSize: 16, fontWeight: '700' },
  badge: {
    position: 'absolute', top: 8, right: 8,
    width: 14, height: 14, borderRadius: 7,
    alignItems: 'center', justifyContent: 'center',
  },
  badgeText: { fontSize: 8, fontWeight: '900', color: '#fff' },
});
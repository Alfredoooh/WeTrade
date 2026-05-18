import React, { useRef } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Animated } from 'react-native';
import { colors, spacing, radius } from '../theme';

const TABS = [
  { id: 'home',     icon: '⬡', label: 'Início' },
  { id: 'explore',  icon: '◈', label: 'Explorar' },
  { id: 'analytics',icon: '◉', label: 'Análises' },
  { id: 'profile',  icon: '◌', label: 'Perfil' },
];

export default function TabBar({ active, onPress }) {
  const scales = useRef(TABS.map(() => new Animated.Value(1))).current;

  const handlePress = (id, i) => {
    Animated.sequence([
      Animated.timing(scales[i], { toValue: 0.82, duration: 70, useNativeDriver: true }),
      Animated.spring(scales[i], { toValue: 1, useNativeDriver: true }),
    ]).start();
    onPress(id);
  };

  return (
    <View style={s.wrapper}>
      <View style={s.bar}>
        {TABS.map((tab, i) => {
          const isActive = active === tab.id;
          return (
            <Animated.View key={tab.id} style={{ flex: 1, transform: [{ scale: scales[i] }] }}>
              <TouchableOpacity style={s.tab} onPress={() => handlePress(tab.id, i)} activeOpacity={1}>
                {isActive && <View style={s.glow} />}
                <Text style={[s.icon, isActive && s.iconActive]}>{tab.icon}</Text>
                <Text style={[s.label, isActive && s.labelActive]}>{tab.label}</Text>
                {isActive && <View style={s.dot} />}
              </TouchableOpacity>
            </Animated.View>
          );
        })}
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  wrapper: {
    paddingBottom: 14,
    paddingHorizontal: spacing.md,
    paddingTop: spacing.sm,
    backgroundColor: colors.bg,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  bar: {
    flexDirection: 'row',
    backgroundColor: colors.bg2,
    borderRadius: radius.lg,
    borderWidth: 1,
    borderColor: colors.border,
    overflow: 'hidden',
  },
  tab: {
    alignItems: 'center',
    paddingVertical: spacing.sm + 2,
    position: 'relative',
    overflow: 'hidden',
  },
  glow: {
    position: 'absolute', inset: 0,
    backgroundColor: colors.accentDim,
  },
  icon: { fontSize: 18, color: colors.textDim, marginBottom: 2 },
  iconActive: { color: colors.accent },
  label: { fontSize: 10, fontWeight: '600', color: colors.textDim, letterSpacing: 0.3 },
  labelActive: { color: colors.accent },
  dot: {
    position: 'absolute', bottom: 4,
    width: 4, height: 4, borderRadius: 2,
    backgroundColor: colors.accent,
  },
});

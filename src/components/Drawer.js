import React, { useEffect, useRef } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, Animated, Pressable } from 'react-native';
import {
  RiUser3Line, RiBellLine, RiCalendarLine, RiRepeatLine,
  RiShieldLine, RiGlobalLine, RiTrophyLine, RiBarChartLine,
  RiGamepadLine, RiNewspaperLine, RiSettings3Line, RiCloseLine,
  RiArrowRightSLine, RiSunLine, RiMoonLine,
} from 'react-icons/ri';
import { useTheme } from '../theme';
import { useStore } from '../store';

const ICON_SIZE = 18;

const MENU_ITEMS = [
  { icon: RiUser3Line,    label: 'Perfil' },
  { icon: RiBellLine,     label: 'Notificações' },
  { icon: RiCalendarLine, label: 'Calendário' },
  { icon: RiRepeatLine,   label: 'Transferências' },
  { icon: RiShieldLine,   label: 'Árbitros' },
  { icon: RiGlobalLine,   label: 'Seleções' },
  { icon: RiTrophyLine,   label: 'Competições' },
  { icon: RiBarChartLine, label: 'Estatísticas' },
  { icon: RiGamepadLine,  label: 'Diversão' },
  { icon: RiNewspaperLine,label: 'Notícias' },
  { icon: RiSettings3Line,label: 'Configurações' },
];

export default function Drawer({ onNavigate }) {
  const { colors, isDark, toggle } = useTheme();
  const { state, dispatch } = useStore();
  const translateX = useRef(new Animated.Value(-400)).current;
  const opacity = useRef(new Animated.Value(0)).current;
  const s = styles(colors);

  useEffect(() => {
    if (state.drawerOpen) {
      Animated.parallel([
        Animated.timing(translateX, { toValue: 0, duration: 260, useNativeDriver: true }),
        Animated.timing(opacity, { toValue: 1, duration: 260, useNativeDriver: true }),
      ]).start();
    } else {
      Animated.parallel([
        Animated.timing(translateX, { toValue: -400, duration: 220, useNativeDriver: true }),
        Animated.timing(opacity, { toValue: 0, duration: 220, useNativeDriver: true }),
      ]).start();
    }
  }, [state.drawerOpen]);

  function close() { dispatch({ type: 'DRAWER_CLOSE' }); }

  function handleItem(label) {
    close();
  }

  if (!state.drawerOpen) return null;

  return (
    <View style={s.root}>
      {/* Overlay */}
      <Animated.View style={[s.overlay, { opacity }]}>
        <Pressable style={StyleSheet.absoluteFill} onPress={close} />
      </Animated.View>

      {/* Panel */}
      <Animated.View style={[s.panel, { transform: [{ translateX }] }]}>
        {/* Header */}
        <View style={s.header}>
          <View style={s.userRow}>
            <View style={[s.avatar, { backgroundColor: colors.primarySoft }]}>
              <Text style={[s.avatarLetter, { color: colors.primary }]}>G</Text>
            </View>
            <View>
              <Text style={[s.userName, { color: colors.text }]}>GoalZone</Text>
              <Text style={[s.userSub, { color: colors.textDim }]}>A tua app de futebol</Text>
            </View>
          </View>
          <View style={s.headerRight}>
            <TouchableOpacity style={[s.iconBtn, { backgroundColor: colors.bg2 }]} onPress={toggle}>
              {isDark
                ? <RiSunLine size={18} color={colors.textSub} />
                : <RiMoonLine size={18} color={colors.textSub} />}
            </TouchableOpacity>
            <TouchableOpacity style={[s.iconBtn, { backgroundColor: colors.bg2 }]} onPress={close}>
              <RiCloseLine size={20} color={colors.textSub} />
            </TouchableOpacity>
          </View>
        </View>

        <View style={[s.divider, { backgroundColor: colors.border }]} />

        {/* Menu */}
        <ScrollView style={s.menu} showsVerticalScrollIndicator={false}>
          {MENU_ITEMS.map(item => {
            const Icon = item.icon;
            return (
              <TouchableOpacity key={item.label} style={s.menuItem} onPress={() => handleItem(item.label)} activeOpacity={0.65}>
                <View style={[s.menuIconWrap, { backgroundColor: colors.bg2, borderColor: colors.border }]}>
                  <Icon size={ICON_SIZE} color={colors.primary} />
                </View>
                <Text style={[s.menuLabel, { color: colors.text }]}>{item.label}</Text>
                <RiArrowRightSLine size={18} color={colors.textDim} />
              </TouchableOpacity>
            );
          })}
        </ScrollView>

        {/* Footer */}
        <View style={[s.footer, { borderTopColor: colors.border }]}>
          <Text style={[s.version, { color: colors.textDim }]}>GoalZone v1.0.0</Text>
        </View>
      </Animated.View>
    </View>
  );
}

const styles = (c) => StyleSheet.create({
  root: {
    position: 'absolute', inset: 0, zIndex: 200,
  },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.48)',
  },
  panel: {
    position: 'absolute', top: 0, left: 0, bottom: 0,
    width: '82%', maxWidth: 320,
    backgroundColor: c.bg,
    display: 'flex', flexDirection: 'column',
    shadowColor: '#000', shadowOpacity: 0.18, shadowRadius: 24, shadowOffset: { width: 4, height: 0 },
    elevation: 16,
  },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 20, paddingTop: 52, paddingBottom: 20,
  },
  userRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  avatar: {
    width: 46, height: 46, borderRadius: 23,
    alignItems: 'center', justifyContent: 'center',
  },
  avatarLetter: { fontSize: 20, fontWeight: '800' },
  userName: { fontSize: 16, fontWeight: '700' },
  userSub: { fontSize: 12, marginTop: 2 },
  headerRight: { flexDirection: 'row', gap: 8 },
  iconBtn: {
    width: 36, height: 36, borderRadius: 18,
    alignItems: 'center', justifyContent: 'center',
  },
  divider: { height: StyleSheet.hairlineWidth, marginHorizontal: 20 },
  menu: { flex: 1, paddingVertical: 8 },
  menuItem: {
    flexDirection: 'row', alignItems: 'center',
    paddingHorizontal: 20, paddingVertical: 13, gap: 14,
  },
  menuIconWrap: {
    width: 38, height: 38, borderRadius: 10,
    alignItems: 'center', justifyContent: 'center',
    borderWidth: StyleSheet.hairlineWidth,
  },
  menuLabel: { flex: 1, fontSize: 15, fontWeight: '500' },
  footer: { borderTopWidth: StyleSheet.hairlineWidth, paddingVertical: 14 },
  version: { fontSize: 12, textAlign: 'center' },
});
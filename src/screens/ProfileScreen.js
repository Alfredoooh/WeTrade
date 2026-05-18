import React from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import { colors, spacing, radius } from '../theme';

const STATS = [
  { label: 'Commits',   value: '847' },
  { label: 'Projectos', value: '12' },
  { label: 'Reviews',   value: '203' },
];

const MENU = [
  { icon: '◈', label: 'Configurações',  desc: 'Preferências e tema' },
  { icon: '⬡', label: 'Integrações',    desc: 'APIs e serviços externos' },
  { icon: '◉', label: 'Notificações',   desc: 'Alertas e avisos' },
  { icon: '⟳', label: 'Sincronização',  desc: 'Dados e backups' },
  { icon: '◌', label: 'Privacidade',    desc: 'Controlo de dados' },
];

export default function ProfileScreen() {
  return (
    <ScrollView style={s.container} showsVerticalScrollIndicator={false} contentContainerStyle={s.content}>
      {/* Avatar */}
      <View style={s.hero}>
        <View style={s.ring}>
          <View style={s.avatar}><Text style={s.avatarText}>A</Text></View>
        </View>
        <Text style={s.name}>Alfred</Text>
        <Text style={s.handle}>@alfred · dev</Text>
        <View style={s.roleBadge}><Text style={s.roleText}>FULLSTACK · MOBILE · WEB</Text></View>
      </View>

      {/* Stats */}
      <View style={s.statsRow}>
        {STATS.map((st, i) => (
          <View key={i} style={[s.statBox, i > 0 && s.statBorder]}>
            <Text style={s.statValue}>{st.value}</Text>
            <Text style={s.statLabel}>{st.label}</Text>
          </View>
        ))}
      </View>

      {/* Menu */}
      <Text style={s.menuTitle}>DEFINIÇÕES</Text>
      {MENU.map((item, i) => (
        <TouchableOpacity key={i} style={s.menuItem} activeOpacity={0.7}>
          <View style={s.menuIconWrap}><Text style={s.menuIcon}>{item.icon}</Text></View>
          <View style={s.menuBody}>
            <Text style={s.menuLabel}>{item.label}</Text>
            <Text style={s.menuDesc}>{item.desc}</Text>
          </View>
          <Text style={s.menuArrow}>›</Text>
        </TouchableOpacity>
      ))}

      <TouchableOpacity style={s.logout} activeOpacity={0.8}>
        <Text style={s.logoutText}>TERMINAR SESSÃO</Text>
      </TouchableOpacity>
      <Text style={s.version}>Pulse App · v1.0.0 · React Native Web</Text>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingBottom: spacing.xxl },
  hero: { alignItems: 'center', paddingTop: spacing.xl, paddingBottom: spacing.lg },
  ring: { width: 80, height: 80, borderRadius: 40, borderWidth: 2, borderColor: colors.accent, padding: 3, marginBottom: spacing.md },
  avatar: { flex: 1, borderRadius: 36, backgroundColor: colors.accentDim, alignItems: 'center', justifyContent: 'center' },
  avatarText: { fontSize: 30, fontWeight: '800', color: colors.accent },
  name: { fontSize: 22, fontWeight: '800', color: colors.text, letterSpacing: -0.5 },
  handle: { fontSize: 12, color: colors.textMuted, marginTop: 4 },
  roleBadge: { marginTop: spacing.sm, borderWidth: 1, borderColor: colors.border, borderRadius: radius.full, paddingHorizontal: spacing.md, paddingVertical: 5 },
  roleText: { fontSize: 9, color: colors.textMuted, fontWeight: '700', letterSpacing: 1.5 },
  statsRow: { flexDirection: 'row', marginHorizontal: spacing.lg, marginBottom: spacing.lg, backgroundColor: colors.bg2, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, overflow: 'hidden' },
  statBox: { flex: 1, alignItems: 'center', paddingVertical: spacing.md },
  statBorder: { borderLeftWidth: 1, borderLeftColor: colors.border },
  statValue: { fontSize: 20, fontWeight: '800', color: colors.text, letterSpacing: -0.5 },
  statLabel: { fontSize: 10, color: colors.textMuted, marginTop: 2, letterSpacing: 1, textTransform: 'uppercase' },
  menuTitle: { fontSize: 10, color: colors.textDim, letterSpacing: 2, fontWeight: '700', marginHorizontal: spacing.lg, marginBottom: spacing.sm },
  menuItem: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: spacing.lg, paddingVertical: spacing.md, borderBottomWidth: 1, borderBottomColor: colors.border, gap: spacing.md },
  menuIconWrap: { width: 34, height: 34, borderRadius: radius.sm, backgroundColor: colors.bg2, borderWidth: 1, borderColor: colors.border, alignItems: 'center', justifyContent: 'center' },
  menuIcon: { fontSize: 16, color: colors.accent },
  menuBody: { flex: 1 },
  menuLabel: { fontSize: 14, fontWeight: '600', color: colors.text },
  menuDesc: { fontSize: 11, color: colors.textMuted, marginTop: 2 },
  menuArrow: { fontSize: 20, color: colors.textDim },
  logout: { marginHorizontal: spacing.lg, marginTop: spacing.xl, borderWidth: 1, borderColor: colors.danger + '55', borderRadius: radius.md, paddingVertical: spacing.md, alignItems: 'center', backgroundColor: colors.dangerDim },
  logoutText: { color: colors.danger, fontSize: 12, fontWeight: '700', letterSpacing: 2 },
  version: { textAlign: 'center', color: colors.textDim, fontSize: 11, marginTop: spacing.lg },
});

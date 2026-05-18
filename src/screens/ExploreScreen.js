import React, { useState } from 'react';
import { View, Text, TextInput, ScrollView, TouchableOpacity, StyleSheet, Dimensions } from 'react-native';
import { colors, spacing, radius } from '../theme';

const W = Dimensions.get('window').width;
const CATS = ['Todos', 'UI', 'API', 'Auth', 'DB', 'Utils'];
const ITEMS = [
  { id: 1, name: 'Button',      cat: 'UI',    desc: 'Botão reutilizável com variantes', tag: 'stable' },
  { id: 2, name: 'AuthGuard',   cat: 'Auth',  desc: 'HOC de protecção de rotas',        tag: 'stable' },
  { id: 3, name: 'useFetch',    cat: 'API',   desc: 'Hook genérico para REST',           tag: 'beta' },
  { id: 4, name: 'Modal',       cat: 'UI',    desc: 'Modal animado com backdrop',        tag: 'stable' },
  { id: 5, name: 'SQLHelper',   cat: 'DB',    desc: 'Abstracção sobre SQLite',           tag: 'wip' },
  { id: 6, name: 'Toast',       cat: 'UI',    desc: 'Sistema de notificações',           tag: 'stable' },
  { id: 7, name: 'useDebounce', cat: 'Utils', desc: 'Debounce para inputs',              tag: 'stable' },
  { id: 8, name: 'OAuthFlow',   cat: 'Auth',  desc: 'Fluxo OAuth2 completo',             tag: 'beta' },
];
const TAG_COLOR = { stable: colors.neon, beta: colors.accent, wip: colors.danger };

export default function ExploreScreen() {
  const [active, setActive] = useState('Todos');
  const [query, setQuery] = useState('');

  const filtered = ITEMS.filter(item => {
    const matchCat = active === 'Todos' || item.cat === active;
    const matchQ = item.name.toLowerCase().includes(query.toLowerCase());
    return matchCat && matchQ;
  });

  return (
    <View style={s.container}>
      <View style={s.searchRow}>
        <Text style={s.searchIcon}>⌕</Text>
        <TextInput
          style={s.input}
          placeholder="Pesquisar..."
          placeholderTextColor={colors.textDim}
          value={query}
          onChangeText={setQuery}
        />
      </View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false}
        contentContainerStyle={s.cats} style={{ flexShrink: 0 }}>
        {CATS.map(c => (
          <TouchableOpacity key={c} onPress={() => setActive(c)}
            style={[s.chip, active === c && s.chipActive]}>
            <Text style={[s.chipText, active === c && s.chipTextActive]}>{c}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>
      <Text style={s.count}>{filtered.length} RESULTADO{filtered.length !== 1 ? 'S' : ''}</Text>
      <ScrollView showsVerticalScrollIndicator={false} style={{ flex: 1 }}>
        <View style={s.grid}>
          {filtered.map(item => {
            const col = TAG_COLOR[item.tag];
            return (
              <TouchableOpacity key={item.id} style={s.card} activeOpacity={0.85}>
                <View style={s.cardTop}>
                  <View style={[s.catPill, { backgroundColor: col + '22' }]}>
                    <Text style={[s.catPillText, { color: col }]}>{item.cat}</Text>
                  </View>
                  <View style={[s.tagPill, { borderColor: col + '55' }]}>
                    <Text style={[s.tagPillText, { color: col }]}>{item.tag}</Text>
                  </View>
                </View>
                <Text style={s.name}>{item.name}</Text>
                <Text style={s.desc}>{item.desc}</Text>
                <Text style={[s.view, { color: col }]}>VER →</Text>
              </TouchableOpacity>
            );
          })}
        </View>
      </ScrollView>
    </View>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg, paddingTop: spacing.lg },
  searchRow: {
    flexDirection: 'row', alignItems: 'center',
    backgroundColor: colors.bg2, borderWidth: 1, borderColor: colors.border,
    borderRadius: radius.md, marginHorizontal: spacing.lg, paddingHorizontal: spacing.md, marginBottom: spacing.sm,
  },
  searchIcon: { fontSize: 18, color: colors.textMuted, marginRight: spacing.sm },
  input: { flex: 1, height: 44, color: colors.text, fontSize: 14, outlineStyle: 'none' },
  cats: { paddingHorizontal: spacing.lg, gap: spacing.xs, paddingBottom: spacing.sm },
  chip: { paddingHorizontal: spacing.md, paddingVertical: 6, borderRadius: radius.full, borderWidth: 1, borderColor: colors.border },
  chipActive: { backgroundColor: colors.accent, borderColor: colors.accent },
  chipText: { fontSize: 12, color: colors.textMuted, fontWeight: '600' },
  chipTextActive: { color: '#fff' },
  count: { fontSize: 10, color: colors.textDim, letterSpacing: 2, marginHorizontal: spacing.lg, marginBottom: spacing.sm, fontWeight: '700' },
  grid: { flexDirection: 'row', flexWrap: 'wrap', paddingHorizontal: spacing.lg, gap: spacing.sm, paddingBottom: spacing.xxl },
  card: {
    width: (W - spacing.lg * 2 - spacing.sm) / 2,
    backgroundColor: colors.bg2, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, padding: spacing.md,
  },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: spacing.sm },
  catPill: { borderRadius: radius.full, paddingHorizontal: 8, paddingVertical: 3 },
  catPillText: { fontSize: 9, fontWeight: '700', letterSpacing: 0.5 },
  tagPill: { borderWidth: 1, borderRadius: radius.full, paddingHorizontal: 7, paddingVertical: 2 },
  tagPillText: { fontSize: 8, fontWeight: '700', letterSpacing: 1, textTransform: 'uppercase' },
  name: { fontSize: 15, fontWeight: '700', color: colors.text, letterSpacing: -0.3, marginBottom: 4 },
  desc: { fontSize: 11, color: colors.textMuted, lineHeight: 16, marginBottom: spacing.md },
  view: { fontSize: 10, fontWeight: '700', letterSpacing: 1.5, borderTopWidth: 1, borderTopColor: colors.border, paddingTop: spacing.sm },
});

import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator } from 'react-native';
import { RiRefreshLine } from 'react-icons/ri';
import { useTheme } from '../theme';
import AppBar from '../components/AppBar';
import MatchCard from '../components/MatchCard';
import { getMatches, COMPETITIONS } from '../api';

const TABS = ['Hoje', 'Amanhã', 'Esta Semana'];
const COMP_IDS = Object.keys(COMPETITIONS);

function getRange(idx) {
  const now = new Date();
  const fmt = d => d.toISOString().split('T')[0];
  if (idx === 0) return { from: fmt(now), to: fmt(now) };
  if (idx === 1) { const t = new Date(now); t.setDate(t.getDate() + 1); return { from: fmt(t), to: fmt(t) }; }
  const end = new Date(now); end.setDate(end.getDate() + 7);
  return { from: fmt(now), to: fmt(end) };
}

export default function MatchesScreen({ navigate }) {
  const { colors } = useTheme();
  const [activeTab, setActiveTab] = useState(0);
  const [activeComp, setActiveComp] = useState('CL');
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const s = styles(colors);

  const load = useCallback(async () => {
    setLoading(true);
    const { from, to } = getRange(activeTab);
    const data = await getMatches(activeComp, from, to);
    setMatches(data?.matches || []);
    setLoading(false);
  }, [activeTab, activeComp]);

  useEffect(() => { load(); }, [load]);

  return (
    <View style={s.root}>
      <AppBar title="Partidas" navigate={navigate} />

      {/* Tabs */}
      <View style={[s.tabRow, { borderBottomColor: colors.border }]}>
        {TABS.map((tab, i) => (
          <TouchableOpacity key={tab} style={[s.tab, activeTab === i && { borderBottomColor: colors.primary }]} onPress={() => setActiveTab(i)} activeOpacity={0.7}>
            <Text style={[s.tabText, { color: activeTab === i ? colors.primary : colors.textDim }]}>{tab}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Competition chips */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={[s.chipRow, { borderBottomColor: colors.border }]} contentContainerStyle={{ gap: 8, paddingHorizontal: 16, paddingVertical: 10 }}>
        {COMP_IDS.map(id => {
          const comp = COMPETITIONS[id];
          const active = activeComp === id;
          return (
            <TouchableOpacity key={id} style={[s.chip, { backgroundColor: active ? colors.primarySoft : colors.bg2, borderColor: active ? colors.primary : colors.border }]} onPress={() => setActiveComp(id)} activeOpacity={0.7}>
              <Text style={[s.chipText, { color: active ? colors.primary : colors.textSub }]}>{comp.name}</Text>
            </TouchableOpacity>
          );
        })}
      </ScrollView>

      {loading ? (
        <View style={s.center}>
          <ActivityIndicator color={colors.primary} size="large" />
        </View>
      ) : matches.length === 0 ? (
        <View style={s.empty}>
          <RiRefreshLine size={36} color={colors.textDim} />
          <Text style={[s.emptyTitle, { color: colors.text }]}>Sem jogos</Text>
          <Text style={[s.emptySub, { color: colors.textDim }]}>Nenhum jogo para este período</Text>
        </View>
      ) : (
        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 24 }}>
          {matches.map(m => <MatchCard key={m.id} match={m} showDate={activeTab === 2} />)}
        </ScrollView>
      )}
    </View>
  );
}

const styles = (c) => StyleSheet.create({
  root: { flex: 1, backgroundColor: c.bg },
  tabRow: { flexDirection: 'row', borderBottomWidth: StyleSheet.hairlineWidth },
  tab: { flex: 1, alignItems: 'center', paddingVertical: 12, borderBottomWidth: 2, borderBottomColor: 'transparent' },
  tabText: { fontSize: 14, fontWeight: '600' },
  chipRow: { borderBottomWidth: StyleSheet.hairlineWidth },
  chip: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 14, paddingVertical: 7, borderRadius: 999, borderWidth: 1.5 },
  chipText: { fontSize: 12, fontWeight: '600' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 10 },
  emptyTitle: { fontSize: 17, fontWeight: '700' },
  emptySub: { fontSize: 13 },
});
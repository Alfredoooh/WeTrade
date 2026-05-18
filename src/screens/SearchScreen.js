import React, { useState, useRef, useEffect } from 'react';
import { View, Text, TextInput, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import {
  RiArrowLeftLine, RiSearchLine, RiCloseLine,
  RiTrophyLine, RiCalendarLine, RiBarChartLine,
  RiRepeatLine, RiLineChartLine, RiGlobalLine,
  RiShieldLine, RiNewspaperLine, RiCalendar2Line, RiMedalLine,
  RiArrowRightSLine,
} from 'react-icons/ri';
import { useTheme } from '../theme';
import { COMPETITIONS } from '../api';

const QUICK = [
  { label: 'Champions League', compId: 'CL', Icon: RiTrophyLine },
  { label: 'Premier League',   compId: 'PL', Icon: RiTrophyLine },
  { label: 'La Liga',          compId: 'PD', Icon: RiTrophyLine },
  { label: 'Liga Portugal',    compId: 'PPL', Icon: RiTrophyLine },
  { label: 'Bundesliga',       compId: 'BL1', Icon: RiTrophyLine },
  { label: 'Serie A',          compId: 'SA', Icon: RiTrophyLine },
];

const CATEGORIES = [
  { label: 'Ao Vivo',         Icon: RiCalendarLine },
  { label: 'Partidas',        Icon: RiCalendar2Line },
  { label: 'Tabelas',         Icon: RiBarChartLine },
  { label: 'Transferências',  Icon: RiRepeatLine },
  { label: 'Estatísticas',    Icon: RiLineChartLine },
  { label: 'Seleções',        Icon: RiGlobalLine },
  { label: 'Árbitros',        Icon: RiShieldLine },
  { label: 'Notícias',        Icon: RiNewspaperLine },
  { label: 'Calendário',      Icon: RiCalendar2Line },
  { label: 'Competições',     Icon: RiMedalLine },
];

export default function SearchScreen({ navigate }) {
  const { colors } = useTheme();
  const [query, setQuery] = useState('');
  const s = styles(colors);
  const ALL_COMPS = Object.values(COMPETITIONS);

  const filtered = query.length > 1
    ? ALL_COMPS.filter(c => c.name.toLowerCase().includes(query.toLowerCase()))
    : [];

  return (
    <View style={s.root}>
      {/* Search bar */}
      <View style={[s.bar, { borderBottomColor: colors.border, backgroundColor: colors.bg }]}>
        <TouchableOpacity style={s.backBtn} onPress={() => navigate('home')} activeOpacity={0.7}>
          <RiArrowLeftLine size={22} color={colors.text} />
        </TouchableOpacity>
        <View style={[s.inputWrap, { backgroundColor: colors.bg2 }]}>
          <RiSearchLine size={16} color={colors.textDim} />
          <TextInput
            style={[s.input, { color: colors.text }]}
            placeholder="Equipas, ligas, jogadores..."
            placeholderTextColor={colors.textDim}
            value={query}
            onChangeText={setQuery}
            autoFocus
          />
          {query.length > 0 && (
            <TouchableOpacity onPress={() => setQuery('')} activeOpacity={0.7}>
              <RiCloseLine size={18} color={colors.textDim} />
            </TouchableOpacity>
          )}
        </View>
      </View>

      {filtered.length > 0 ? (
        <ScrollView showsVerticalScrollIndicator={false}>
          {filtered.map(comp => (
            <TouchableOpacity key={comp.id} style={[s.resultRow, { borderBottomColor: colors.border }]} activeOpacity={0.7}>
              <View style={[s.resultIcon, { backgroundColor: colors.primarySoft }]}>
                <RiTrophyLine size={16} color={colors.primary} />
              </View>
              <View style={s.resultInfo}>
                <Text style={[s.resultName, { color: colors.text }]}>{comp.name}</Text>
                <Text style={[s.resultSub, { color: colors.textDim }]}>Competição · {comp.area}</Text>
              </View>
              <RiArrowRightSLine size={16} color={colors.textDim} />
            </TouchableOpacity>
          ))}
        </ScrollView>
      ) : query.length > 1 ? (
        <View style={s.noResults}>
          <RiSearchLine size={36} color={colors.textDim} />
          <Text style={[s.noResultsText, { color: colors.textDim }]}>Sem resultados para "{query}"</Text>
        </View>
      ) : (
        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 32 }}>
          {/* Quick access */}
          <Text style={[s.sectionLabel, { color: colors.textDim }]}>ACESSO RÁPIDO</Text>
          <View style={s.quickGrid}>
            {QUICK.map(item => {
              const Icon = item.Icon;
              return (
                <TouchableOpacity key={item.label} style={[s.quickCard, { backgroundColor: colors.bg2 }]} activeOpacity={0.75}>
                  <Icon size={20} color={colors.primary} />
                  <Text style={[s.quickLabel, { color: colors.textSub }]} numberOfLines={2}>{item.label}</Text>
                </TouchableOpacity>
              );
            })}
          </View>

          {/* Categories */}
          <Text style={[s.sectionLabel, { color: colors.textDim }]}>EXPLORAR</Text>
          {CATEGORIES.map(cat => {
            const Icon = cat.Icon;
            return (
              <TouchableOpacity key={cat.label} style={[s.catRow, { borderBottomColor: colors.border }]} activeOpacity={0.7}>
                <View style={[s.catIcon, { backgroundColor: colors.bg2, borderColor: colors.border }]}>
                  <Icon size={16} color={colors.primary} />
                </View>
                <Text style={[s.catLabel, { color: colors.text }]}>{cat.label}</Text>
                <RiArrowRightSLine size={16} color={colors.textDim} />
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      )}
    </View>
  );
}

const styles = (c) => StyleSheet.create({
  root: { flex: 1, backgroundColor: c.bg },
  bar: {
    flexDirection: 'row', alignItems: 'center', gap: 8,
    paddingHorizontal: 12, paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  backBtn: { width: 36, height: 36, alignItems: 'center', justifyContent: 'center' },
  inputWrap: {
    flex: 1, flexDirection: 'row', alignItems: 'center', gap: 8,
    borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10,
  },
  input: { flex: 1, fontSize: 15, outlineStyle: 'none' },
  resultRow: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 14, paddingHorizontal: 16, borderBottomWidth: StyleSheet.hairlineWidth },
  resultIcon: { width: 38, height: 38, borderRadius: 10, alignItems: 'center', justifyContent: 'center' },
  resultInfo: { flex: 1, gap: 2 },
  resultName: { fontSize: 15, fontWeight: '600' },
  resultSub: { fontSize: 12 },
  noResults: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 },
  noResultsText: { fontSize: 14 },
  sectionLabel: { fontSize: 11, fontWeight: '700', letterSpacing: 1.2, paddingHorizontal: 16, paddingTop: 20, paddingBottom: 10 },
  quickGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, paddingHorizontal: 16, marginBottom: 4 },
  quickCard: { width: '30%', borderRadius: 12, padding: 14, alignItems: 'center', gap: 8, flexGrow: 1 },
  quickLabel: { fontSize: 11, fontWeight: '600', textAlign: 'center' },
  catRow: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 13, paddingHorizontal: 16, borderBottomWidth: StyleSheet.hairlineWidth },
  catIcon: { width: 36, height: 36, borderRadius: 10, alignItems: 'center', justifyContent: 'center', borderWidth: StyleSheet.hairlineWidth },
  catLabel: { flex: 1, fontSize: 15, fontWeight: '500' },
});
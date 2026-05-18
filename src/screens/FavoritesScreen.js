import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import { RiStarLine, RiStarFill, RiArrowRightSLine, RiHeartLine } from 'react-icons/ri';
import { useTheme } from '../theme';
import AppBar from '../components/AppBar';
import { useStore } from '../store';
import { COMPETITIONS } from '../api';

const ALL_COMPS = Object.values(COMPETITIONS);
const TABS = ['Equipas', 'Competições'];

export default function FavoritesScreen({ navigate }) {
  const { colors } = useTheme();
  const { state, dispatch } = useStore();
  const [activeTab, setActiveTab] = useState(0);
  const { favorites } = state;
  const s = styles(colors);
  
  return (
    <View style={s.root}>
      <AppBar title="Favoritos" navigate={navigate} />

      {/* Tabs */}
      <View style={[s.tabRow, { borderBottomColor: colors.border }]}>
        {TABS.map((t, i) => (
          <TouchableOpacity key={t} style={[s.tab, activeTab === i && { borderBottomColor: colors.primary }]} onPress={() => setActiveTab(i)} activeOpacity={0.7}>
            <Text style={[s.tabText, { color: activeTab === i ? colors.primary : colors.textDim }]}>{t}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {activeTab === 0 ? (
        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 24 }}>
          {favorites.teams.length === 0 ? (
            <View style={s.empty}>
              <RiHeartLine size={44} color={colors.textDim} />
              <Text style={[s.emptyTitle, { color: colors.text }]}>Sem equipas favoritas</Text>
              <Text style={[s.emptySub, { color: colors.textDim }]}>Adiciona equipas para as seguir aqui</Text>
            </View>
          ) : (
            favorites.teams.map(team => (
              <TouchableOpacity key={team.id} style={[s.row, { borderBottomColor: colors.border }]} activeOpacity={0.7}>
                <View style={[s.crestCircle, { backgroundColor: colors.bg2 }]}>
                  <View style={[s.crestFallback, { backgroundColor: colors.bg3 }]} />
                </View>
                <Text style={[s.rowLabel, { color: colors.text }]}>{team.name}</Text>
                <RiArrowRightSLine size={18} color={colors.textDim} />
              </TouchableOpacity>
            ))
          )}
        </ScrollView>
      ) : (
        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 24 }}>
          {ALL_COMPS.map(comp => {
            const isFav = favorites.competitions.some(c => c.id === comp.id);
            return (
              <TouchableOpacity key={comp.id} style={[s.row, { borderBottomColor: colors.border }]} onPress={() => dispatch({ type: 'TOGGLE_FAV_COMP', comp })} activeOpacity={0.7}>
                <View style={[s.compArea, { backgroundColor: colors.primarySoft }]}>
                  <Text style={[s.compAreaText, { color: colors.primary }]}>{comp.area.slice(0, 2).toUpperCase()}</Text>
                </View>
                <View style={s.compInfo}>
                  <Text style={[s.rowLabel, { color: colors.text }]}>{comp.name}</Text>
                  <Text style={[s.rowSub, { color: colors.textDim }]}>{comp.area}</Text>
                </View>
                {isFav
                  ? <RiStarFill size={20} color="#f4b400" />
                  : <RiStarLine size={20} color={colors.textDim} />}
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
  tabRow: { flexDirection: 'row', borderBottomWidth: StyleSheet.hairlineWidth },
  tab: { flex: 1, alignItems: 'center', paddingVertical: 12, borderBottomWidth: 2, borderBottomColor: 'transparent' },
  tabText: { fontSize: 14, fontWeight: '600' },
  empty: { alignItems: 'center', paddingVertical: 64, gap: 10 },
  emptyTitle: { fontSize: 17, fontWeight: '700' },
  emptySub: { fontSize: 13, textAlign: 'center', paddingHorizontal: 32 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 14, paddingHorizontal: 16, borderBottomWidth: StyleSheet.hairlineWidth },
  crestCircle: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  crestFallback: { width: 26, height: 26, borderRadius: 13 },
  rowLabel: { flex: 1, fontSize: 15, fontWeight: '600' },
  rowSub: { fontSize: 12, marginTop: 1 },
  compArea: { width: 40, height: 40, borderRadius: 10, alignItems: 'center', justifyContent: 'center' },
  compAreaText: { fontSize: 11, fontWeight: '800', letterSpacing: 0.5 },
  compInfo: { flex: 1, gap: 2 },
});
import React from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import { RiTvLine, RiCircleFill, RiEyeLine, RiArrowRightSLine } from 'react-icons/ri';
import { useTheme } from '../theme';
import AppBar from '../components/AppBar';

const CHANNELS = [
  { name: 'Sport TV 1', live: true, match: 'Benfica vs Porto', time: '21:00' },
  { name: 'Sport TV 2', live: true, match: 'Man City vs Chelsea', time: '20:00' },
  { name: 'Sport TV 3', live: false, match: 'Real Madrid vs Barça', time: '21:00' },
  { name: 'Sport TV 4', live: false, match: 'Bayern vs Dortmund', time: '18:30' },
  { name: 'Sport TV+', live: false, match: 'Juventus vs Milan', time: '20:45' },
  { name: 'RTP 1', live: false, match: 'Portugal vs Espanha', time: '20:45' },
  { name: 'SIC', live: false, match: 'Europa League: highlights', time: '22:00' },
  { name: 'TVI', live: false, match: 'Liga Portugal: resumo', time: '23:00' },
  { name: 'Eleven 1', live: true, match: 'Ajax vs PSV', time: '20:00' },
  { name: 'Eleven 2', live: false, match: 'Premier League: top 10', time: '21:30' },
  { name: 'DAZN', live: false, match: 'Napoli vs Inter', time: '20:45' },
  { name: 'Eurosport', live: false, match: 'La Liga: magazine', time: '22:00' },
];

const STREAMS = [
  { name: 'GoalZone Live 1', match: 'Champions League', viewers: '12.4k', live: true },
  { name: 'GoalZone Live 2', match: 'Premier League', viewers: '8.1k', live: true },
  { name: 'GoalZone Live 3', match: 'La Liga', viewers: '5.7k', live: false },
];

function SectionHeader({ title, badge, colors }) {
  return (
    <View style={shS.wrap}>
      <Text style={[shS.title, { color: colors.text }]}>{title}</Text>
      {badge && (
        <View style={[shS.badge, { backgroundColor: colors.dangerSoft }]}>
          <RiCircleFill size={6} color={colors.danger} />
          <Text style={[shS.badgeText, { color: colors.danger }]}>{badge}</Text>
        </View>
      )}
    </View>
  );
}
const shS = StyleSheet.create({
  wrap: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingHorizontal: 16, paddingTop: 20, paddingBottom: 12 },
  title: { fontSize: 15, fontWeight: '700' },
  badge: { flexDirection: 'row', alignItems: 'center', gap: 5, paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999 },
  badgeText: { fontSize: 9, fontWeight: '800', letterSpacing: 0.8 },
});

function ChannelRow({ ch, colors }) {
  return (
    <TouchableOpacity style={[crS.row, { borderBottomColor: colors.border }]} activeOpacity={0.7}>
      <View style={[crS.iconWrap, { backgroundColor: colors.bg2, borderColor: colors.border }]}>
        <RiTvLine size={18} color={colors.primary} />
      </View>
      <View style={crS.info}>
        <Text style={[crS.name, { color: colors.text }]}>{ch.name}</Text>
        <Text style={[crS.match, { color: colors.textSub }]} numberOfLines={1}>{ch.match}</Text>
      </View>
      {ch.live ? (
        <View style={[crS.livePill, { backgroundColor: colors.dangerSoft }]}>
          <RiCircleFill size={6} color={colors.danger} />
          <Text style={[crS.liveText, { color: colors.danger }]}>AO VIVO</Text>
        </View>
      ) : (
        <Text style={[crS.time, { color: colors.textDim }]}>{ch.time}</Text>
      )}
    </TouchableOpacity>
  );
}
const crS = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12, paddingHorizontal: 16, borderBottomWidth: StyleSheet.hairlineWidth },
  iconWrap: { width: 44, height: 44, borderRadius: 12, alignItems: 'center', justifyContent: 'center', borderWidth: StyleSheet.hairlineWidth },
  info: { flex: 1, gap: 3 },
  name: { fontSize: 14, fontWeight: '700' },
  match: { fontSize: 12 },
  livePill: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 8, paddingVertical: 4, borderRadius: 999 },
  liveText: { fontSize: 9, fontWeight: '800', letterSpacing: 1 },
  time: { fontSize: 13, fontWeight: '600' },
});

function StreamCard({ stream, colors }) {
  return (
    <TouchableOpacity style={[stS.card, { backgroundColor: colors.card, borderColor: colors.border }]} activeOpacity={0.8}>
      <View style={[stS.thumb, { backgroundColor: colors.bg3 }]}>
        <RiTvLine size={28} color={colors.textDim} />
        {stream.live && (
          <View style={[stS.liveTag, { backgroundColor: colors.danger }]}>
            <Text style={stS.liveTagText}>AO VIVO</Text>
          </View>
        )}
      </View>
      <View style={stS.info}>
        <Text style={[stS.name, { color: colors.text }]} numberOfLines={1}>{stream.name}</Text>
        <View style={stS.subRow}>
          <Text style={[stS.sub, { color: colors.textDim }]}>{stream.match}</Text>
          <View style={stS.viewers}>
            <RiEyeLine size={11} color={colors.textDim} />
            <Text style={[stS.viewersText, { color: colors.textDim }]}>{stream.viewers}</Text>
          </View>
        </View>
      </View>
    </TouchableOpacity>
  );
}
const stS = StyleSheet.create({
  card: { width: 160, borderRadius: 12, overflow: 'hidden', borderWidth: StyleSheet.hairlineWidth },
  thumb: { height: 88, alignItems: 'center', justifyContent: 'center', position: 'relative' },
  liveTag: { position: 'absolute', top: 8, left: 8, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 4 },
  liveTagText: { fontSize: 8, fontWeight: '800', color: '#fff', letterSpacing: 1 },
  info: { padding: 10, gap: 4 },
  name: { fontSize: 13, fontWeight: '700' },
  subRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sub: { fontSize: 11 },
  viewers: { flexDirection: 'row', alignItems: 'center', gap: 3 },
  viewersText: { fontSize: 11 },
});

export default function TVScreen({ navigate }) {
  const { colors } = useTheme();
  
  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <AppBar title="TV" navigate={navigate} />
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 32 }}>
        <SectionHeader title="A transmitir agora" badge="AO VIVO" colors={colors} />
        {CHANNELS.filter(c => c.live).map(ch => <ChannelRow key={ch.name} ch={ch} colors={colors} />)}

        <SectionHeader title="Programação de hoje" colors={colors} />
        {CHANNELS.map(ch => <ChannelRow key={ch.name + '_all'} ch={ch} colors={colors} />)}

        <SectionHeader title="GoalZone Streams" colors={colors} />
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 12, paddingHorizontal: 16, paddingBottom: 8 }}>
          {STREAMS.map(st => <StreamCard key={st.name} stream={st} colors={colors} />)}
        </ScrollView>
      </ScrollView>
    </View>
  );
}
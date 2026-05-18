import React, { useState, useEffect, useRef } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, Animated, Dimensions } from 'react-native';
import {
  RiCalendarLine, RiArrowRightSLine, RiRefreshLine,
  RiCircleFill, RiCheckLine, RiAlertLine,
} from 'react-icons/ri';
import { useTheme } from '../theme';
import AppBar from '../components/AppBar';
import MatchCard from '../components/MatchCard';
import { getLiveMatches, getTodayMatches } from '../api';

const W = Dimensions.get('window').width > 480 ? 480 : Dimensions.get('window').width;

const HIGHLIGHTS = [
  { id: 'h1', team: 'Liverpool',  desc: "Salah hat-trick vs Manchester United", color: '#C8102E' },
  { id: 'h2', team: 'R. Madrid',  desc: 'Courtois: defesa histórica no Bernabéu', color: '#00529F' },
  { id: 'h3', team: 'Barcelona',  desc: 'Lamine Yamal hat-trick vs Atlético',    color: '#A50044' },
];

const FEED_STATIC = [
  { ok: true,  msg: 'Benfica: Paulo Bernardo lesionado',   time: '5m' },
  { ok: false, msg: 'Porto elimina o Braga na Taça',       time: '22m' },
  { ok: true,  msg: 'Sporting bate recorde de golos',      time: '1h' },
  { ok: false, msg: 'Manchester City com 3 baixas',        time: '2h' },
];

function buildWeek() {
  const today = new Date();
  const dow = today.getDay();
  const mondayOffset = dow === 0 ? -6 : 1 - dow;
  const base = new Date(today);
  base.setDate(today.getDate() + mondayOffset - 2);
  const DAYS = ['Seg','Ter','Qua','Qui','Sex','Sáb','Dom'];
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(base);
    d.setDate(base.getDate() + i);
    const isToday = d.toDateString() === today.toDateString();
    const dayIdx = d.getDay();
    return { label: isToday ? 'Hoje' : DAYS[dayIdx === 0 ? 6 : dayIdx - 1], date: d.getDate(), isToday, i };
  });
}

/* ─── Date Strip ─── */
function DateStrip({ colors }) {
  const week = buildWeek();
  const todayIdx = week.findIndex(d => d.isToday);
  const [selected, setSelected] = useState(todayIdx >= 0 ? todayIdx : 0);
  const s = dsStyles(colors);

  return (
    <View style={s.wrap}>
      <RiCalendarLine size={18} color={colors.textDim} style={{ flexShrink: 0 }} />
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.scroll}>
        {week.map(d => {
          const active = selected === d.i;
          return (
            <TouchableOpacity key={d.i} style={[s.dayBtn, active && { backgroundColor: colors.primary }]} onPress={() => setSelected(d.i)} activeOpacity={0.75}>
              <Text style={[s.dayLabel, { color: active ? '#fff' : colors.textDim }]}>{d.label}</Text>
              <Text style={[s.dayNum, { color: active ? '#fff' : colors.text }]}>{d.date}</Text>
              {d.isToday && !active && <View style={[s.dot, { backgroundColor: colors.primary }]} />}
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    </View>
  );
}
const dsStyles = (c) => StyleSheet.create({
  wrap: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingLeft: 16, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: c.border },
  scroll: { gap: 4, paddingRight: 16, paddingVertical: 8 },
  dayBtn: { alignItems: 'center', paddingHorizontal: 12, paddingVertical: 8, borderRadius: 12, minWidth: 46 },
  dayLabel: { fontSize: 10, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: 2 },
  dayNum: { fontSize: 15, fontWeight: '800' },
  dot: { width: 4, height: 4, borderRadius: 2, marginTop: 3 },
});

/* ─── Section Header ─── */
function SectionHeader({ title, action, onAction, colors }) {
  return (
    <View style={shS.wrap}>
      <Text style={[shS.title, { color: colors.text }]}>{title}</Text>
      {action && (
        <TouchableOpacity onPress={onAction} activeOpacity={0.7} style={shS.actionBtn}>
          <Text style={[shS.action, { color: colors.primary }]}>{action}</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}
const shS = StyleSheet.create({
  wrap: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingTop: 20, paddingBottom: 12 },
  title: { fontSize: 15, fontWeight: '700' },
  actionBtn: { paddingVertical: 4, paddingHorizontal: 2 },
  action: { fontSize: 13, fontWeight: '600' },
});

/* ─── Highlight Card ─── */
function HighlightCard({ item }) {
  const scaleAnim = useRef(new Animated.Value(1)).current;
  function onPressIn() { Animated.spring(scaleAnim, { toValue: 0.95, useNativeDriver: true, speed: 40 }).start(); }
  function onPressOut() { Animated.spring(scaleAnim, { toValue: 1, useNativeDriver: true, speed: 25 }).start(); }

  return (
    <TouchableOpacity onPressIn={onPressIn} onPressOut={onPressOut} activeOpacity={1}>
      <Animated.View style={[hlS.card, { backgroundColor: item.color, transform: [{ scale: scaleAnim }] }]}>
        <View style={hlS.overlay} />
        <View style={hlS.badge}>
          <View style={[hlS.teamDot, { borderColor: '#fff' }]} />
          <Text style={hlS.teamName}>{item.team}</Text>
        </View>
        <Text style={hlS.desc}>{item.desc}</Text>
      </Animated.View>
    </TouchableOpacity>
  );
}
const hlS = StyleSheet.create({
  card: { width: 180, height: 136, borderRadius: 16, padding: 14, justifyContent: 'flex-end', overflow: 'hidden' },
  overlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.42)' },
  badge: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 6, zIndex: 1 },
  teamDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: '#fff', borderWidth: 1.5 },
  teamName: { fontSize: 12, fontWeight: '700', color: '#fff' },
  desc: { fontSize: 11, fontWeight: '500', color: 'rgba(255,255,255,0.88)', lineHeight: 15, zIndex: 1 },
});

/* ─── Live Match Card ─── */
function LiveMatchCard({ match, colors }) {
  const live = match.status === 'IN_PLAY' || match.status === 'PAUSED';
  const score = match.score?.fullTime;
  const scaleAnim = useRef(new Animated.Value(1)).current;
  function onPressIn() { Animated.spring(scaleAnim, { toValue: 0.95, useNativeDriver: true, speed: 40 }).start(); }
  function onPressOut() { Animated.spring(scaleAnim, { toValue: 1, useNativeDriver: true, speed: 25 }).start(); }

  return (
    <TouchableOpacity onPressIn={onPressIn} onPressOut={onPressOut} activeOpacity={1}>
      <Animated.View style={[lcS.card, { backgroundColor: colors.primary, transform: [{ scale: scaleAnim }] }]}>
        {live && (
          <View style={lcS.topRow}>
            <View style={lcS.livePill}>
              <RiCircleFill size={6} color="#fff" />
              <Text style={lcS.liveText}>AO VIVO</Text>
            </View>
            <Text style={lcS.minute}>{match.minute ?? ''}'</Text>
          </View>
        )}
        <View style={lcS.scoreRow}>
          <Text style={lcS.teamName} numberOfLines={1}>
            {match.homeTeam?.shortName || match.homeTeam?.name || '—'}
          </Text>
          <Text style={lcS.score}>
            {score?.home ?? '-'} : {score?.away ?? '-'}
          </Text>
          <Text style={[lcS.teamName, { textAlign: 'right' }]} numberOfLines={1}>
            {match.awayTeam?.shortName || match.awayTeam?.name || '—'}
          </Text>
        </View>
        <Text style={lcS.comp} numberOfLines={1}>{match.competition?.name ?? ''}</Text>
      </Animated.View>
    </TouchableOpacity>
  );
}
const lcS = StyleSheet.create({
  card: { width: 170, borderRadius: 16, padding: 14, gap: 10 },
  topRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  livePill: { flexDirection: 'row', alignItems: 'center', gap: 4, backgroundColor: 'rgba(255,255,255,0.2)', paddingHorizontal: 7, paddingVertical: 3, borderRadius: 999 },
  liveText: { fontSize: 9, fontWeight: '800', color: '#fff', letterSpacing: 1 },
  minute: { fontSize: 11, fontWeight: '700', color: 'rgba(255,255,255,0.7)' },
  scoreRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 4 },
  teamName: { flex: 1, fontSize: 11, fontWeight: '700', color: 'rgba(255,255,255,0.85)' },
  score: { fontSize: 20, fontWeight: '900', color: '#fff', letterSpacing: -0.5 },
  comp: { fontSize: 10, fontWeight: '600', color: 'rgba(255,255,255,0.5)', letterSpacing: 0.3 },
});

/* ─── Feed Item ─── */
function FeedItem({ item, colors }) {
  return (
    <View style={[fiS.row, { backgroundColor: colors.card, borderColor: colors.border }]}>
      {item.ok
        ? <RiCheckLine size={14} color={colors.success} />
        : <RiAlertLine size={14} color={colors.danger} />}
      <Text style={[fiS.msg, { color: colors.text }]} numberOfLines={1}>{item.msg}</Text>
      <Text style={[fiS.time, { color: colors.textDim }]}>{item.time}</Text>
    </View>
  );
}
const fiS = StyleSheet.create({
  row: {
    flexDirection: 'row', alignItems: 'center', gap: 10,
    padding: 11, borderRadius: 10, borderWidth: StyleSheet.hairlineWidth, marginBottom: 6,
  },
  msg: { flex: 1, fontSize: 13, fontWeight: '500' },
  time: { fontSize: 11 },
});

/* ─── Home Screen ─── */
export default function HomeScreen({ navigate }) {
  const { colors } = useTheme();
  const [liveMatches, setLiveMatches] = useState([]);
  const [feed, setFeed] = useState(FEED_STATIC);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const s = scStyles(colors);

  async function load() {
    const [live, today] = await Promise.all([getLiveMatches(), getTodayMatches()]);
    const combined = [...(live?.matches || []), ...(today?.matches || [])];
    const seen = new Set();
    setLiveMatches(combined.filter(m => { if (seen.has(m.id)) return false; seen.add(m.id); return true; }));
    setLoading(false);
    setRefreshing(false);
  }

  useEffect(() => { load(); }, []);

  useEffect(() => {
    const NEWS = [
      { ok: true,  msg: 'Novo utilizador registado',   time: 'agora' },
      { ok: false, msg: 'Servidor B com carga elevada', time: 'agora' },
      { ok: true,  msg: 'Pagamento processado',         time: 'agora' },
    ];
    let i = 0;
    const t = setInterval(() => {
      setFeed(f => [{ ...NEWS[i % NEWS.length] }, ...f.slice(0, 5)]);
      i++;
    }, 6000);
    return () => clearInterval(t);
  }, []);

  return (
    <View style={s.root}>
      <AppBar navigate={navigate} />
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.content}>
        <DateStrip colors={colors} />

        {/* Highlights */}
        <SectionHeader title="Match Highlights" action="Ver todos" colors={colors} />
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 12, paddingHorizontal: 16, paddingBottom: 4 }}>
          {HIGHLIGHTS.map(h => <HighlightCard key={h.id} item={h} />)}
        </ScrollView>

        {/* Live */}
        <SectionHeader title="Jogos ao Vivo" action="Ver todos" onAction={() => navigate('matches')} colors={colors} />
        {loading ? (
          <View style={s.loadWrap}>
            <RiRefreshLine size={22} color={colors.textDim} />
            <Text style={[s.loadText, { color: colors.textDim }]}>A carregar...</Text>
          </View>
        ) : liveMatches.length === 0 ? (
          <View style={s.empty}>
            <Text style={[s.emptyTitle, { color: colors.text }]}>Sem jogos ao vivo</Text>
            <Text style={[s.emptySub, { color: colors.textDim }]}>Puxa para actualizar</Text>
          </View>
        ) : (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 12, paddingHorizontal: 16, paddingBottom: 4 }}>
            {liveMatches.slice(0, 6).map(m => <LiveMatchCard key={m.id} match={m} colors={colors} />)}
          </ScrollView>
        )}

        {/* Activity feed */}
        <SectionHeader title="Actividade" colors={colors} />
        <View style={[s.liveRow, { paddingHorizontal: 16, marginBottom: 10 }]}>
          <RiCircleFill size={8} color={colors.danger} />
          <Text style={[s.liveLabel, { color: colors.danger }]}>AO VIVO</Text>
        </View>
        <View style={{ paddingHorizontal: 16 }}>
          {feed.map((item, i) => <FeedItem key={i} item={item} colors={colors} />)}
        </View>

        {/* CTA */}
        <View style={{ paddingHorizontal: 16, marginTop: 12 }}>
          <TouchableOpacity style={[s.cta, { borderColor: colors.primary, backgroundColor: colors.primarySoft }]} activeOpacity={0.8}>
            <Text style={[s.ctaText, { color: colors.primary }]}>VER RELATÓRIO COMPLETO</Text>
            <RiArrowRightSLine size={16} color={colors.primary} />
          </TouchableOpacity>
        </View>
      </ScrollView>
    </View>
  );
}

const scStyles = (c) => StyleSheet.create({
  root: { flex: 1, backgroundColor: c.bg },
  content: { paddingBottom: 24 },
  loadWrap: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, padding: 24 },
  loadText: { fontSize: 14 },
  empty: { alignItems: 'center', padding: 28, gap: 4 },
  emptyTitle: { fontSize: 15, fontWeight: '700' },
  emptySub: { fontSize: 13 },
  liveRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  liveLabel: { fontSize: 10, fontWeight: '800', letterSpacing: 1.5 },
  cta: { borderWidth: 1, borderRadius: 12, paddingVertical: 14, alignItems: 'center', flexDirection: 'row', justifyContent: 'center', gap: 6 },
  ctaText: { fontSize: 12, fontWeight: '700', letterSpacing: 1.5 },
});
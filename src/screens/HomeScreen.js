import React, { useEffect, useRef, useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, Animated, Dimensions } from 'react-native';
import { colors, spacing, radius } from '../theme';

const W = Dimensions.get('window').width;

const CARDS = [
  { icon: '⬡', label: 'Visitas',   value: '24.8k', delta: '+12%',  color: colors.neon,   bg: colors.neonDim },
  { icon: '◉', label: 'Utilizadores', value: '1,247', delta: '+5%', color: colors.accent, bg: colors.accentDim },
  { icon: '◈', label: 'Uptime',    value: '98.4%', delta: '-0.2%', color: colors.warn,   bg: colors.warnDim },
  { icon: '◌', label: 'Receita',   value: '€8.3k', delta: '+31%',  color: colors.danger, bg: colors.dangerDim },
];

const FEED = [
  { ok: true,  msg: 'Deploy concluído com sucesso',    time: 'agora' },
  { ok: true,  msg: 'Build webpack finalizado',         time: '1 min' },
  { ok: false, msg: 'Timeout na rota /api/users',       time: '3 min' },
  { ok: true,  msg: 'Staging actualizado',              time: '7 min' },
  { ok: false, msg: 'Pico de tráfego detectado',        time: '12 min' },
];

function Card({ card, index }) {
  const anim = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    Animated.timing(anim, { toValue: 1, duration: 480, delay: index * 90, useNativeDriver: true }).start();
  }, []);
  return (
    <Animated.View style={[s.card, {
      opacity: anim,
      transform: [{ translateY: anim.interpolate({ inputRange: [0,1], outputRange: [16,0] }) }],
      borderColor: card.color + '33',
    }]}>
      <View style={[s.cardIconWrap, { backgroundColor: card.bg }]}>
        <Text style={[s.cardIcon, { color: card.color }]}>{card.icon}</Text>
      </View>
      <Text style={s.cardValue}>{card.value}</Text>
      <Text style={s.cardLabel}>{card.label}</Text>
      <Text style={[s.cardDelta, { color: card.delta.startsWith('-') ? colors.danger : colors.neon }]}>
        {card.delta}
      </Text>
    </Animated.View>
  );
}

export default function HomeScreen() {
  const [feed, setFeed] = useState(FEED);
  const pulseAnim = useRef(new Animated.Value(1)).current;

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, { toValue: 1.6, duration: 900, useNativeDriver: true }),
        Animated.timing(pulseAnim, { toValue: 1,   duration: 900, useNativeDriver: true }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, []);

  useEffect(() => {
    const NEW = [
      { ok: true,  msg: 'Novo utilizador registado' },
      { ok: false, msg: 'Memória em 82% no servidor B' },
      { ok: true,  msg: 'Pagamento processado' },
    ];
    let i = 0;
    const t = setInterval(() => {
      setFeed(f => [{ ...NEW[i % NEW.length], time: 'agora' }, ...f.slice(0, 6)]);
      i++;
    }, 4000);
    return () => clearInterval(t);
  }, []);

  return (
    <ScrollView style={s.container} showsVerticalScrollIndicator={false} contentContainerStyle={s.content}>
      {/* Header */}
      <View style={s.header}>
        <View>
          <Text style={s.title}>Bom dia, <Text style={s.titleAccent}>Alfred</Text></Text>
          <Text style={s.subtitle}>Sistema operacional · {new Date().toLocaleDateString('pt-PT')}</Text>
        </View>
        <View style={s.pulseWrap}>
          <Animated.View style={[s.pulseRing, { transform: [{ scale: pulseAnim }] }]} />
          <View style={s.pulseCore} />
        </View>
      </View>

      {/* Cards grid */}
      <View style={s.grid}>
        {CARDS.map((c, i) => <Card key={i} card={c} index={i} />)}
      </View>

      {/* Feed */}
      <View style={s.section}>
        <View style={s.sectionHead}>
          <Text style={s.sectionTitle}>ACTIVIDADE</Text>
          <View style={s.liveBadge}>
            <View style={s.liveDot} />
            <Text style={s.liveText}>LIVE</Text>
          </View>
        </View>
        {feed.map((item, i) => (
          <View key={i} style={s.feedItem}>
            <View style={[s.feedDot, { backgroundColor: item.ok ? colors.neon : colors.danger }]} />
            <Text style={s.feedMsg} numberOfLines={1}>{item.msg}</Text>
            <Text style={s.feedTime}>{item.time}</Text>
          </View>
        ))}
      </View>

      {/* CTA */}
      <TouchableOpacity style={s.cta} activeOpacity={0.85}>
        <Text style={s.ctaText}>⟶ VER RELATÓRIO COMPLETO</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.lg, paddingBottom: spacing.xxl },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: spacing.lg },
  title: { fontSize: 24, fontWeight: '800', color: colors.text, letterSpacing: -0.5 },
  titleAccent: { color: colors.neon },
  subtitle: { fontSize: 11, color: colors.textMuted, marginTop: 4, letterSpacing: 0.5 },
  pulseWrap: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
  pulseRing: {
    position: 'absolute', width: 40, height: 40, borderRadius: 20,
    borderWidth: 2, borderColor: colors.neon, opacity: 0.5,
  },
  pulseCore: { width: 14, height: 14, borderRadius: 7, backgroundColor: colors.neon, boxShadow: `0 0 12px ${colors.neonGlow}` },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm, marginBottom: spacing.lg },
  card: {
    width: (W - spacing.lg * 2 - spacing.sm) / 2,
    backgroundColor: colors.bg2,
    borderWidth: 1,
    borderRadius: radius.md,
    padding: spacing.md,
  },
  cardIconWrap: {
    width: 34, height: 34, borderRadius: radius.sm,
    alignItems: 'center', justifyContent: 'center', marginBottom: spacing.sm,
  },
  cardIcon: { fontSize: 16 },
  cardValue: { fontSize: 22, fontWeight: '800', color: colors.text, letterSpacing: -0.5 },
  cardLabel: { fontSize: 10, color: colors.textMuted, letterSpacing: 1, textTransform: 'uppercase', marginTop: 2 },
  cardDelta: { fontSize: 11, fontWeight: '700', marginTop: 6, letterSpacing: 0.5 },
  section: { marginBottom: spacing.lg },
  sectionHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: spacing.sm },
  sectionTitle: { fontSize: 10, fontWeight: '700', color: colors.textMuted, letterSpacing: 2 },
  liveBadge: { flexDirection: 'row', alignItems: 'center', gap: 5 },
  liveDot: { width: 5, height: 5, borderRadius: 3, backgroundColor: colors.danger },
  liveText: { fontSize: 9, color: colors.danger, fontWeight: '700', letterSpacing: 1.5 },
  feedItem: {
    flexDirection: 'row', alignItems: 'center', gap: spacing.sm,
    backgroundColor: colors.bg2, borderWidth: 1, borderColor: colors.border,
    borderRadius: radius.sm, padding: spacing.sm + 4, marginBottom: spacing.xs,
  },
  feedDot: { width: 7, height: 7, borderRadius: 4, flexShrink: 0 },
  feedMsg: { flex: 1, fontSize: 12, color: colors.text, fontWeight: '500' },
  feedTime: { fontSize: 10, color: colors.textDim, flexShrink: 0 },
  cta: {
    borderWidth: 1, borderColor: colors.accent, borderRadius: radius.md,
    paddingVertical: spacing.md, alignItems: 'center', backgroundColor: colors.accentDim,
  },
  ctaText: { color: colors.accent, fontSize: 11, fontWeight: '700', letterSpacing: 2 },
});

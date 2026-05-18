import React from 'react';
import { View, Text, ScrollView, StyleSheet, Dimensions } from 'react-native';
import { colors, spacing, radius } from '../theme';

const W = Dimensions.get('window').width;

const PAGES = [
  { page: '/home',     visits: 9842, bounce: '24%', status: 'ok' },
  { page: '/explore',  visits: 4217, bounce: '31%', status: 'ok' },
  { page: '/pricing',  visits: 2841, bounce: '47%', status: 'warn' },
  { page: '/docs',     visits: 1995, bounce: '18%', status: 'ok' },
  { page: '/api/v2',   visits: 994,  bounce: '62%', status: 'err' },
];

const BARS = [
  { day: 'Seg', v: 3200, s: 1800 },
  { day: 'Ter', v: 4800, s: 2700 },
  { day: 'Qua', v: 3900, s: 2100 },
  { day: 'Qui', v: 5600, s: 3200 },
  { day: 'Sex', v: 4200, s: 2500 },
  { day: 'Sáb', v: 6800, s: 4100 },
  { day: 'Dom', v: 7400, s: 4600 },
];

const maxV = Math.max(...BARS.map(b => b.v));
const STATUS_COLOR = { ok: colors.neon, warn: colors.warn, err: colors.danger };

export default function AnalyticsScreen() {
  return (
    <ScrollView style={s.container} showsVerticalScrollIndicator={false} contentContainerStyle={s.content}>
      {/* Chart */}
      <View style={s.card}>
        <Text style={s.cardTitle}>TRÁFEGO — 7 DIAS</Text>
        <View style={s.legend}>
          <View style={[s.legDot, { backgroundColor: colors.neon }]} /><Text style={s.legText}>Visitas</Text>
          <View style={[s.legDot, { backgroundColor: colors.accent }]} /><Text style={s.legText}>Sessões</Text>
        </View>
        <View style={s.barChart}>
          {BARS.map((b, i) => (
            <View key={i} style={s.barCol}>
              <View style={s.barStack}>
                <View style={[s.bar, { height: (b.v / maxV) * 120, backgroundColor: colors.neon + 'cc' }]} />
                <View style={[s.bar, s.barOverlay, { height: (b.s / maxV) * 120, backgroundColor: colors.accent + 'aa' }]} />
              </View>
              <Text style={s.barLabel}>{b.day}</Text>
            </View>
          ))}
        </View>
      </View>

      {/* Table */}
      <View style={s.card}>
        <Text style={s.cardTitle}>PÁGINAS MAIS VISITADAS</Text>
        <View style={s.tableHead}>
          <Text style={[s.th, { flex: 2 }]}>PÁGINA</Text>
          <Text style={[s.th, { flex: 1, textAlign: 'right' }]}>VISITAS</Text>
          <Text style={[s.th, { flex: 1, textAlign: 'right' }]}>SAÍDA</Text>
          <Text style={[s.th, { flex: 1, textAlign: 'right' }]}>ESTADO</Text>
        </View>
        {PAGES.map((p, i) => (
          <View key={i} style={[s.tableRow, i === PAGES.length - 1 && s.tableRowLast]}>
            <Text style={[s.td, { flex: 2 }]} numberOfLines={1}>{p.page}</Text>
            <Text style={[s.td, { flex: 1, textAlign: 'right' }]}>{p.visits.toLocaleString()}</Text>
            <Text style={[s.td, { flex: 1, textAlign: 'right' }]}>{p.bounce}</Text>
            <View style={{ flex: 1, alignItems: 'flex-end' }}>
              <View style={[s.pill, { backgroundColor: STATUS_COLOR[p.status] + '22', borderColor: STATUS_COLOR[p.status] + '55' }]}>
                <Text style={[s.pillText, { color: STATUS_COLOR[p.status] }]}>
                  {p.status === 'ok' ? 'OK' : p.status === 'warn' ? 'ALTO' : 'CRIT'}
                </Text>
              </View>
            </View>
          </View>
        ))}
      </View>

      {/* Summary stats */}
      <View style={s.summaryRow}>
        {[
          { label: 'Total Visitas',  value: '24.8k' },
          { label: 'Média / Dia',    value: '3.5k' },
          { label: 'Conversão',      value: '4.2%' },
        ].map((item, i) => (
          <View key={i} style={s.summaryCard}>
            <Text style={s.summaryValue}>{item.value}</Text>
            <Text style={s.summaryLabel}>{item.label}</Text>
          </View>
        ))}
      </View>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.lg, paddingBottom: spacing.xxl, gap: spacing.md },
  card: { backgroundColor: colors.bg2, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, padding: spacing.md },
  cardTitle: { fontSize: 10, fontWeight: '700', color: colors.textMuted, letterSpacing: 2, marginBottom: spacing.sm },
  legend: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: spacing.md },
  legDot: { width: 8, height: 8, borderRadius: 2 },
  legText: { fontSize: 11, color: colors.textMuted, marginRight: 8 },
  barChart: { flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between', height: 140 },
  barCol: { flex: 1, alignItems: 'center', justifyContent: 'flex-end' },
  barStack: { width: '70%', position: 'relative', alignItems: 'center', justifyContent: 'flex-end' },
  bar: { width: '100%', borderRadius: 3, minHeight: 4 },
  barOverlay: { position: 'absolute', bottom: 0, width: '60%' },
  barLabel: { fontSize: 9, color: colors.textDim, marginTop: 6, letterSpacing: 0.5 },
  tableHead: { flexDirection: 'row', paddingBottom: spacing.sm, borderBottomWidth: 1, borderBottomColor: colors.border, marginBottom: 2 },
  th: { fontSize: 9, color: colors.textDim, letterSpacing: 1.5, fontWeight: '700', textTransform: 'uppercase' },
  tableRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: colors.border },
  tableRowLast: { borderBottomWidth: 0 },
  td: { fontSize: 12, color: colors.text },
  pill: { borderWidth: 1, borderRadius: radius.full, paddingHorizontal: 7, paddingVertical: 2 },
  pillText: { fontSize: 9, fontWeight: '700', letterSpacing: 0.5 },
  summaryRow: { flexDirection: 'row', gap: spacing.sm },
  summaryCard: {
    flex: 1, backgroundColor: colors.bg2, borderWidth: 1, borderColor: colors.border,
    borderRadius: radius.md, padding: spacing.md, alignItems: 'center',
  },
  summaryValue: { fontSize: 20, fontWeight: '800', color: colors.text, letterSpacing: -0.5 },
  summaryLabel: { fontSize: 10, color: colors.textMuted, marginTop: 4, letterSpacing: 0.5, textAlign: 'center' },
});

import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { RiCircleFill } from 'react-icons/ri';
import { useTheme } from '../theme';
import { isLive, getScore, getStatusLabel, formatDate } from '../api';

export default function MatchCard({ match, showDate }) {
  const { colors } = useTheme();
  const live = isLive(match);
  const score = getScore(match);
  const status = getStatusLabel(match);
  const finished = match.status === 'FINISHED';
  const homeWin = finished && score && score.home > score.away;
  const awayWin = finished && score && score.away > score.home;
  const s = styles(colors);

  return (
    <TouchableOpacity style={s.card} activeOpacity={0.7}>
      {showDate && (
        <Text style={[s.dateLabel, { color: colors.textDim }]}>
          {formatDate(match.utcDate)}
        </Text>
      )}
      <View style={s.row}>
        {/* Home */}
        <View style={s.teamHome}>
          <View style={[s.crestBox, { backgroundColor: colors.bg2 }]}>
            {match.homeTeam?.crest
              ? <img src={match.homeTeam.crest} style={{ width: 28, height: 28, objectFit: 'contain' }} alt="" onError={e => e.target.style.display = 'none'} />
              : <View style={[s.crestFallback, { backgroundColor: colors.bg3 }]} />}
          </View>
          <Text style={[s.teamName, { color: homeWin ? colors.primary : colors.text }]} numberOfLines={1}>
            {match.homeTeam?.shortName || match.homeTeam?.name || '—'}
          </Text>
        </View>

        {/* Score block */}
        <View style={s.scoreBlock}>
          {live && (
            <View style={[s.livePill, { backgroundColor: colors.dangerSoft }]}>
              <RiCircleFill size={6} color={colors.danger} />
              <Text style={[s.liveText, { color: colors.danger }]}>AO VIVO</Text>
            </View>
          )}
          {score ? (
            <Text style={[s.score, { color: live ? colors.danger : colors.text }]}>
              {score.home} : {score.away}
            </Text>
          ) : (
            <Text style={[s.vs, { color: colors.primary }]}>VS</Text>
          )}
          <Text style={[s.statusLabel, { color: live ? colors.danger : colors.textDim }]}>{status}</Text>
        </View>

        {/* Away */}
        <View style={s.teamAway}>
          <View style={[s.crestBox, { backgroundColor: colors.bg2 }]}>
            {match.awayTeam?.crest
              ? <img src={match.awayTeam.crest} style={{ width: 28, height: 28, objectFit: 'contain' }} alt="" onError={e => e.target.style.display = 'none'} />
              : <View style={[s.crestFallback, { backgroundColor: colors.bg3 }]} />}
          </View>
          <Text style={[s.teamName, { color: awayWin ? colors.primary : colors.text }]} numberOfLines={1}>
            {match.awayTeam?.shortName || match.awayTeam?.name || '—'}
          </Text>
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = (c) => StyleSheet.create({
  card: {
    paddingVertical: 12, paddingHorizontal: 16,
    borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: c.border,
    backgroundColor: c.bg,
  },
  dateLabel: { fontSize: 10, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 0.6, marginBottom: 8 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  teamHome: { flex: 1, alignItems: 'flex-start', gap: 6 },
  teamAway: { flex: 1, alignItems: 'flex-end', gap: 6 },
  crestBox: { width: 36, height: 36, borderRadius: 8, alignItems: 'center', justifyContent: 'center', overflow: 'hidden' },
  crestFallback: { width: 24, height: 24, borderRadius: 12 },
  teamName: { fontSize: 12, fontWeight: '600', maxWidth: 90 },
  scoreBlock: { alignItems: 'center', gap: 2, minWidth: 80 },
  livePill: {
    flexDirection: 'row', alignItems: 'center', gap: 4,
    paddingHorizontal: 7, paddingVertical: 3, borderRadius: 999,
  },
  liveText: { fontSize: 9, fontWeight: '800', letterSpacing: 0.8 },
  score: { fontSize: 22, fontWeight: '900', letterSpacing: -0.5 },
  vs: { fontSize: 14, fontWeight: '700' },
  statusLabel: { fontSize: 10, fontWeight: '700', letterSpacing: 0.4 },
});
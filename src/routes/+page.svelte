<script>
  import { onMount } from 'svelte';
  import AppBar from '$lib/components/AppBar.svelte';
  import MatchCard from '$lib/components/MatchCard.svelte';
  import Loader from '$lib/components/Loader.svelte';
  import { getLiveMatches, getTodayMatches, COMPETITIONS } from '$lib/services/api.js';
  import { favorites } from '$lib/stores/app.js';
  import { goto } from '$app/navigation';

  let liveMatches = [];
  let todayMatches = [];
  let loading = true;

  const today = new Date();
  let selectedDay = today.getDay() === 0 ? 6 : today.getDay() - 1;

  function buildWeek() {
    const days = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
    const result = [];
    const base = new Date(today);
    const dow = today.getDay();
    const mondayOffset = dow === 0 ? -6 : 1 - dow;
    base.setDate(today.getDate() + mondayOffset - 2);
    for (let i = 0; i < 7; i++) {
      const d = new Date(base);
      d.setDate(base.getDate() + i);
      const isToday = d.toDateString() === today.toDateString();
      result.push({ label: isToday ? 'Hoje' : days[d.getDay() === 0 ? 6 : d.getDay() - 1], date: d.getDate(), isToday, idx: i });
    }
    return result;
  }

  const week = buildWeek();
  const todayIdx = week.findIndex(d => d.isToday);
  $: if (todayIdx >= 0) selectedDay = todayIdx;

  const highlights = [
    { id: 'h1', team: 'Liverpool', desc: "Salah's stunning goal vs. Manchester United", color: '#C8102E' },
    { id: 'h2', team: 'R. Madrid', desc: 'Courtois proves he is a world champion', color: '#00529F' },
    { id: 'h3', team: 'Barcelona', desc: 'Lamine Yamal hat-trick vs Atletico', color: '#A50044' },
  ];

  onMount(async () => {
    const [live, today2] = await Promise.all([getLiveMatches(), getTodayMatches()]);
    liveMatches = live?.matches || [];
    todayMatches = today2?.matches || [];
    loading = false;
  });

  $: favTeams = $favorites?.teams || [];
</script>

<AppBar />

<div class="page">

  <!-- Date strip -->
  <div class="date-strip">
    <button class="cal-btn pressable" aria-label="Calendário">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
      </svg>
    </button>
    <div class="days-scroll">
      {#each week as d, i}
        <button class="day-btn pressable" class:active={selectedDay === i} on:click={() => selectedDay = i}>
          <span class="day-label">{d.label}</span>
          <span class="day-num">{d.date}</span>
          {#if d.isToday && selectedDay !== i}<span class="today-dot"></span>{/if}
        </button>
      {/each}
    </div>
  </div>

  <!-- Match Highlights -->
  <section class="section">
    <div class="section-header">
      <h2>Match Highlights</h2>
      <button class="see-all pressable">View All</button>
    </div>
    <div class="highlights-scroll">
      {#each highlights as h}
        <button class="highlight-card pressable" style="--hc: {h.color}">
          <div class="hc-overlay"></div>
          <div class="hc-badge">
            <div class="hc-logo" style="background:{h.color}">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="white"><circle cx="12" cy="12" r="10"/></svg>
            </div>
            <span class="hc-team">{h.team}</span>
          </div>
          <p class="hc-desc">{h.desc}</p>
        </button>
      {/each}
    </div>
  </section>

  <!-- Favourites -->
  {#if favTeams.length > 0}
    <section class="section fav-section">
      <div class="section-header">
        <h2>Favourites</h2>
      </div>
      <div class="fav-row">
        {#each favTeams.slice(0, 4) as team}
          <button class="fav-pill pressable" on:click={() => goto(`/equipa/${team.id}`)}>
            {#if team.crest}
              <img src={team.crest} alt={team.name} class="fav-crest" on:error={e => e.target.style.display='none'} />
            {:else}
              <div class="fav-crest-fallback"></div>
            {/if}
          </button>
        {/each}
        <button class="fav-pill fav-add pressable" on:click={() => goto('/pesquisar')}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>
    </section>
  {/if}

  <!-- Live Matches -->
  <section class="section">
    <div class="section-header">
      <h2>Live Matches</h2>
      <button class="see-all pressable" on:click={() => goto('/partidas')}>View All</button>
    </div>
    {#if loading}
      <Loader />
    {:else if liveMatches.length === 0}
      <div class="live-scroll">
        {#each todayMatches.slice(0, 4) as match}
          <button class="live-card pressable" on:click={() => goto(`/jogos/${match.id}`)}>
            <div class="lc-top">
              {#if match.status === 'IN_PLAY' || match.status === 'PAUSED'}
                <span class="lc-live-tag">Live</span>
                <span class="lc-min">{match.minute ?? ''}'</span>
              {:else}
                <span class="lc-time">{match.utcDate ? new Date(match.utcDate).toLocaleTimeString('pt-PT', {hour:'2-digit',minute:'2-digit'}) : ''}</span>
              {/if}
            </div>
            <div class="lc-teams">
              {#if match.homeTeam?.crest}
                <img src={match.homeTeam.crest} alt="" class="lc-crest" on:error={e => e.target.style.display='none'} />
              {/if}
              <div class="lc-score">
                <span>{match.score?.fullTime?.home ?? '-'}</span>
                <span class="lc-sep">:</span>
                <span>{match.score?.fullTime?.away ?? '-'}</span>
              </div>
              {#if match.awayTeam?.crest}
                <img src={match.awayTeam.crest} alt="" class="lc-crest" on:error={e => e.target.style.display='none'} />
              {/if}
            </div>
            <p class="lc-label">{match.homeTeam?.shortName ?? ''} vs {match.awayTeam?.shortName ?? ''}</p>
          </button>
        {/each}
      </div>
    {:else}
      <div class="live-scroll">
        {#each liveMatches.slice(0, 4) as match}
          <button class="live-card pressable" on:click={() => goto(`/jogos/${match.id}`)}>
            <div class="lc-top">
              <span class="lc-live-tag">Live</span>
              <span class="lc-min">{match.minute ?? ''}'</span>
            </div>
            <div class="lc-teams">
              {#if match.homeTeam?.crest}
                <img src={match.homeTeam.crest} alt="" class="lc-crest" on:error={e => e.target.style.display='none'} />
              {/if}
              <div class="lc-score">
                <span>{match.score?.fullTime?.home ?? '-'}</span>
                <span class="lc-sep">:</span>
                <span>{match.score?.fullTime?.away ?? '-'}</span>
              </div>
              {#if match.awayTeam?.crest}
                <img src={match.awayTeam.crest} alt="" class="lc-crest" on:error={e => e.target.style.display='none'} />
              {/if}
            </div>
            <p class="lc-label">{match.homeTeam?.shortName ?? ''} vs {match.awayTeam?.shortName ?? ''}</p>
          </button>
        {/each}
      </div>
    {/if}
  </section>

</div>

<style>
  .page { padding-bottom: 8px; }

  /* Date strip */
  .date-strip {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px 8px;
    border-bottom: 1px solid var(--border);
  }

  .cal-btn {
    color: var(--fg-2);
    flex-shrink: 0;
    padding: 4px;
  }

  .days-scroll {
    display: flex;
    gap: 4px;
    overflow-x: auto;
    flex: 1;
  }

  .day-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 6px 10px;
    border-radius: 12px;
    flex-shrink: 0;
    position: relative;
    transition: background 0.15s;
  }

  .day-btn.active {
    background: var(--fg);
  }

  .day-label {
    font-size: 0.65rem;
    font-weight: 600;
    color: var(--fg-3);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .day-btn.active .day-label { color: var(--bg); }

  .day-num {
    font-size: 0.9rem;
    font-weight: 800;
    color: var(--fg);
  }

  .day-btn.active .day-num { color: var(--bg); }

  .today-dot {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--primary);
    position: absolute;
    bottom: 4px;
  }

  /* Sections */
  .section { margin-bottom: 4px; }

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 16px 10px;
  }

  h2 { font-size: 1rem; font-weight: 700; color: var(--fg); }

  .see-all { font-size: 0.8125rem; font-weight: 600; color: var(--primary); }

  /* Highlights */
  .highlights-scroll {
    display: flex;
    gap: 12px;
    overflow-x: auto;
    padding: 0 16px 16px;
  }

  .highlight-card {
    position: relative;
    width: 180px;
    height: 140px;
    border-radius: 16px;
    background: linear-gradient(135deg, var(--hc), #111);
    flex-shrink: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    padding: 12px;
    text-align: left;
  }

  .hc-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(to top, rgba(0,0,0,0.7) 0%, transparent 60%);
  }

  .hc-badge {
    display: flex;
    align-items: center;
    gap: 6px;
    position: relative;
    z-index: 1;
    margin-bottom: 4px;
  }

  .hc-logo {
    width: 22px;
    height: 22px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .hc-team {
    font-size: 0.75rem;
    font-weight: 700;
    color: white;
  }

  .hc-desc {
    font-size: 0.7rem;
    font-weight: 500;
    color: rgba(255,255,255,0.85);
    line-height: 1.3;
    position: relative;
    z-index: 1;
  }

  /* Favourites */
  .fav-section { border-bottom: 1px solid var(--border); }

  .fav-row {
    display: flex;
    gap: 10px;
    padding: 4px 16px 16px;
  }

  .fav-pill {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    background: var(--bg-2);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .fav-crest { width: 32px; height: 32px; object-fit: contain; }

  .fav-crest-fallback {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--bg-3);
  }

  .fav-add { color: var(--fg-3); }

  /* Live cards */
  .live-scroll {
    display: flex;
    gap: 12px;
    overflow-x: auto;
    padding: 0 16px 16px;
  }

  .live-card {
    flex-shrink: 0;
    width: 160px;
    background: var(--primary);
    border-radius: 16px;
    padding: 14px 12px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    text-align: left;
  }

  .live-card:nth-child(even) { background: #c0392b; }

  .lc-top {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .lc-live-tag {
    font-size: 0.6rem;
    font-weight: 800;
    color: white;
    background: rgba(255,255,255,0.25);
    padding: 2px 7px;
    border-radius: 4px;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .lc-live-tag::before {
    content: '';
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: white;
    display: inline-block;
  }

  .lc-min, .lc-time {
    font-size: 0.7rem;
    font-weight: 700;
    color: rgba(255,255,255,0.7);
  }

  .lc-teams {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 6px;
  }

  .lc-crest { width: 36px; height: 36px; object-fit: contain; }

  .lc-score {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 1.3rem;
    font-weight: 900;
    color: white;
  }

  .lc-sep { color: rgba(255,255,255,0.5); }

  .lc-label {
    font-size: 0.65rem;
    font-weight: 600;
    color: rgba(255,255,255,0.7);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
</style>
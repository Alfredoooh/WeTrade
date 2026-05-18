<script>
  import { onMount } from 'svelte';
  import AppBar from '$lib/components/AppBar.svelte';
  import Loader from '$lib/components/Loader.svelte';
  import { getLiveMatches, getTodayMatches } from '$lib/services/api.js';
  import { getFootballNews } from '$lib/services/news.js';
  import { favorites } from '$lib/stores/app.js';
  import { goto } from '$app/navigation';

  let liveMatches = [];
  let todayMatches = [];
  let news = [];
  let loading = true;

  const today = new Date();
  let selectedDay = 0;

  function buildWeek() {
    const days = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sab', 'Dom'];
    const result = [];
    const base = new Date(today);
    const dow = today.getDay();
    const mondayOffset = dow === 0 ? -6 : 1 - dow;
    base.setDate(today.getDate() + mondayOffset - 2);
    for (let i = 0; i < 7; i++) {
      const d = new Date(base);
      d.setDate(base.getDate() + i);
      const isToday = d.toDateString() === today.toDateString();
      result.push({
        label: isToday ? 'Hoje' : days[d.getDay() === 0 ? 6 : d.getDay() - 1],
        date: d.getDate(),
        isToday,
        idx: i
      });
    }
    return result;
  }

  const week = buildWeek();
  const todayIdx = week.findIndex(d => d.isToday);
  $: if (todayIdx >= 0) selectedDay = todayIdx;

  onMount(async () => {
    const [live, tod, n] = await Promise.all([
      getLiveMatches(),
      getTodayMatches(),
      getFootballNews()
    ]);
    liveMatches = live?.matches || [];
    todayMatches = tod?.matches || [];
    news = n;
    loading = false;
  });

  $: favTeams = $favorites?.teams || [];
  $: displayMatches = liveMatches.length > 0 ? liveMatches : todayMatches;
</script>

<AppBar />

<div class="page">

  <div class="date-strip">
    <button class="cal-btn pressable" on:click={() => goto('/calendario')}>
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="4" width="18" height="18" rx="2"/>
        <line x1="16" y1="2" x2="16" y2="6"/>
        <line x1="8" y1="2" x2="8" y2="6"/>
        <line x1="3" y1="10" x2="21" y2="10"/>
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

  {#if favTeams.length > 0}
    <section class="section">
      <div class="section-header">
        <h2>Favoritos</h2>
      </div>
      <div class="fav-row">
        {#each favTeams.slice(0, 5) as team}
          <button class="fav-pill pressable" on:click={() => goto(`/equipa/${team.id}`)}>
            {#if team.crest}
              <img src={team.crest} alt={team.name} class="fav-crest" on:error={e => e.target.style.display='none'} />
            {:else}
              <div class="fav-crest-fallback"></div>
            {/if}
          </button>
        {/each}
        <button class="fav-pill fav-add pressable" on:click={() => goto('/favoritos')}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
        </button>
      </div>
    </section>
  {/if}

  <section class="section">
    <div class="section-header">
      <h2>{liveMatches.length > 0 ? 'Ao Vivo' : 'Jogos de Hoje'}</h2>
      <button class="see-all pressable" on:click={() => goto('/partidas')}>Ver tudo</button>
    </div>
    {#if loading}
      <Loader />
    {:else if displayMatches.length === 0}
      <p class="empty-msg">Sem jogos de momento</p>
    {:else}
      <div class="matches-scroll">
        {#each displayMatches.slice(0, 8) as match}
          <button class="match-card pressable" on:click={() => goto(`/jogos/${match.id}`)}>
            <div class="mc-top">
              {#if match.status === 'IN_PLAY' || match.status === 'PAUSED'}
                <span class="mc-live">Live</span>
                <span class="mc-min">{match.minute ?? ''}'</span>
              {:else if match.status === 'FINISHED'}
                <span class="mc-ft">FT</span>
              {:else}
                <span class="mc-time">{match.utcDate ? new Date(match.utcDate).toLocaleTimeString('pt-PT', {hour:'2-digit',minute:'2-digit'}) : ''}</span>
              {/if}
            </div>
            <div class="mc-teams">
              {#if match.homeTeam?.crest}
                <img src={match.homeTeam.crest} alt="" class="mc-crest" on:error={e => e.target.style.display='none'} />
              {:else}
                <div class="mc-crest-fallback"></div>
              {/if}
              <div class="mc-score">
                <span>{match.score?.fullTime?.home ?? '-'}</span>
                <span class="mc-sep">:</span>
                <span>{match.score?.fullTime?.away ?? '-'}</span>
              </div>
              {#if match.awayTeam?.crest}
                <img src={match.awayTeam.crest} alt="" class="mc-crest" on:error={e => e.target.style.display='none'} />
              {:else}
                <div class="mc-crest-fallback"></div>
              {/if}
            </div>
            <p class="mc-label">{match.homeTeam?.shortName ?? ''} vs {match.awayTeam?.shortName ?? ''}</p>
          </button>
        {/each}
      </div>
    {/if}
  </section>

  <section class="section">
    <div class="section-header">
      <h2>Noticias</h2>
      <button class="see-all pressable" on:click={() => goto('/noticias')}>Ver tudo</button>
    </div>
    {#if news.length === 0}
      <p class="empty-msg">A carregar noticias...</p>
    {:else}
      {#each news.slice(0, 8) as item, i}
        <button class="news-item pressable" on:click={() => goto(`/noticias/${i}?title=${encodeURIComponent(item.title)}&desc=${encodeURIComponent(item.description)}&source=${encodeURIComponent(item.source)}&date=${encodeURIComponent(item.pubDate)}&link=${encodeURIComponent(item.link)}`)}>
          {#if item.image}
            <img src={item.image} alt="" class="news-thumb" on:error={e => e.target.style.display='none'} />
          {:else}
            <div class="news-thumb-fallback"></div>
          {/if}
          <div class="news-info">
            <p class="news-title">{item.title}</p>
            <p class="news-meta">{item.source} · {item.pubDate ? new Date(item.pubDate).toLocaleDateString('pt-PT') : ''}</p>
          </div>
        </button>
      {/each}
    {/if}
  </section>

</div>

<style>
  .page { padding-bottom: 8px; }

  .date-strip {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px 8px;
    border-bottom: 1px solid var(--border);
  }

  .cal-btn { color: var(--fg-2); flex-shrink: 0; padding: 4px; }

  .days-scroll { display: flex; gap: 4px; overflow-x: auto; flex: 1; }

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

  .day-btn.active { background: var(--fg); }

  .day-label {
    font-size: 0.65rem;
    font-weight: 600;
    color: var(--fg-3);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .day-btn.active .day-label { color: var(--bg); }

  .day-num { font-size: 0.9rem; font-weight: 800; color: var(--fg); }

  .day-btn.active .day-num { color: var(--bg); }

  .today-dot {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--primary);
    position: absolute;
    bottom: 4px;
  }

  .section { margin-bottom: 4px; }

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 16px 10px;
  }

  h2 { font-size: 1rem; font-weight: 700; color: var(--fg); }

  .see-all { font-size: 0.8125rem; font-weight: 600; color: var(--primary); }

  .fav-row { display: flex; gap: 10px; padding: 4px 16px 16px; overflow-x: auto; }

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

  .fav-crest-fallback { width: 32px; height: 32px; border-radius: 50%; background: var(--bg-3); }

  .fav-add { color: var(--fg-3); }

  .matches-scroll { display: flex; gap: 12px; overflow-x: auto; padding: 0 16px 16px; }

  .match-card {
    flex-shrink: 0;
    width: 155px;
    background: var(--primary);
    border-radius: 16px;
    padding: 14px 12px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    text-align: left;
  }

  .match-card:nth-child(even) { background: #0a3fa8; }

  .mc-top { display: flex; align-items: center; gap: 6px; }

  .mc-live {
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

  .mc-live::before {
    content: '';
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: white;
    display: inline-block;
  }

  .mc-min, .mc-time, .mc-ft {
    font-size: 0.7rem;
    font-weight: 700;
    color: rgba(255,255,255,0.7);
  }

  .mc-teams {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 6px;
  }

  .mc-crest { width: 34px; height: 34px; object-fit: contain; }

  .mc-crest-fallback { width: 34px; height: 34px; border-radius: 50%; background: rgba(255,255,255,0.15); }

  .mc-score {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 1.25rem;
    font-weight: 900;
    color: white;
  }

  .mc-sep { color: rgba(255,255,255,0.5); }

  .mc-label {
    font-size: 0.65rem;
    font-weight: 600;
    color: rgba(255,255,255,0.7);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .news-item {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
    text-align: left;
  }

  .news-thumb { width: 72px; height: 72px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }

  .news-thumb-fallback { width: 72px; height: 72px; border-radius: 10px; background: var(--bg-3); flex-shrink: 0; }

  .news-info { flex: 1; min-width: 0; }

  .news-title {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--fg);
    line-height: 1.35;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .news-meta { font-size: 0.72rem; color: var(--fg-3); margin-top: 4px; }

  .empty-msg { font-size: 0.875rem; color: var(--fg-3); padding: 16px; text-align: center; }
</style>
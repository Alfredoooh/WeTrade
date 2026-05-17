<script>
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import AppBar from '$lib/components/AppBar.svelte';
  import Loader from '$lib/components/Loader.svelte';
  import { formatDate, formatTime } from '$lib/services/api.js';
  
  $: id = $page.params.id;
  
  let match = null;
  let loading = true;
  
  const API_KEY = 'YOUR_API_KEY_HERE';
  
  onMount(async () => {
    try {
      const res = await fetch(`https://api.football-data.org/v4/matches/${id}`, {
        headers: { 'X-Auth-Token': API_KEY }
      });
      match = await res.json();
    } catch (e) {}
    loading = false;
  });
  
  $: isLive = match?.status === 'IN_PLAY' || match?.status === 'PAUSED';
  $: isFinished = match?.status === 'FINISHED';
  $: score = match?.score?.fullTime;
  $: htScore = match?.score?.halfTime;
</script>

<AppBar title="Jogo" showBack={true} showSearch={false} showNotif={false} />

{#if loading}
  <Loader />
{:else if !match || match.message}
  <div class="error">
    <p>⚽</p>
    <p>Jogo não disponível</p>
    <p class="sub">Configura a tua API key em src/lib/services/api.js</p>
  </div>
{:else}
  <div class="match-detail">

    <!-- Header score -->
    <div class="score-hero" class:live={isLive}>
      <p class="comp-label">{match.competition?.name || ''}</p>

      <div class="teams-score">
        <div class="team-col">
          {#if match.homeTeam?.crest}
            <img src={match.homeTeam.crest} alt="" class="big-crest" on:error={e => e.target.style.display='none'} />
          {:else}
            <div class="big-crest-fallback">⚽</div>
          {/if}
          <p class="team-nm">{match.homeTeam?.name || '—'}</p>
        </div>

        <div class="middle">
          {#if isLive}
            <div class="live-tag">AO VIVO</div>
            {#if match.minute}<p class="live-min">{match.minute}'</p>{/if}
          {:else if isFinished}
            <p class="status-s">FT</p>
          {:else}
            <p class="time-s">{formatTime(match.utcDate)}</p>
            <p class="date-s">{formatDate(match.utcDate)}</p>
          {/if}

          {#if score && (isLive || isFinished)}
            <p class="big-score">{score.home ?? '-'} : {score.away ?? '-'}</p>
            {#if htScore && htScore.home !== null}
              <p class="ht-score">Int. {htScore.home}-{htScore.away}</p>
            {/if}
          {:else}
            <p class="big-score vs">VS</p>
          {/if}
        </div>

        <div class="team-col">
          {#if match.awayTeam?.crest}
            <img src={match.awayTeam.crest} alt="" class="big-crest" on:error={e => e.target.style.display='none'} />
          {:else}
            <div class="big-crest-fallback">⚽</div>
          {/if}
          <p class="team-nm">{match.awayTeam?.name || '—'}</p>
        </div>
      </div>
    </div>

    <!-- Match info -->
    <div class="info-section">
      <div class="info-row">
        <span class="info-label">Competição</span>
        <span class="info-val">{match.competition?.name || '—'}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Data</span>
        <span class="info-val">{formatDate(match.utcDate)}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Hora</span>
        <span class="info-val">{formatTime(match.utcDate)}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Estádio</span>
        <span class="info-val">{match.venue || '—'}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Jornada</span>
        <span class="info-val">{match.matchday ? `J${match.matchday}` : '—'}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Estado</span>
        <span class="info-val status" class:live={isLive}>
          {isLive ? 'Ao vivo' : isFinished ? 'Terminado' : 'Agendado'}
        </span>
      </div>
    </div>

    <!-- Odds placeholder -->
    <div class="section-card">
      <h3>Prognósticos</h3>
      <div class="odds-row">
        <div class="odd-box">
          <p class="odd-label">1</p>
          <p class="odd-val">2.10</p>
        </div>
        <div class="odd-box">
          <p class="odd-label">X</p>
          <p class="odd-val">3.40</p>
        </div>
        <div class="odd-box">
          <p class="odd-label">2</p>
          <p class="odd-val">3.20</p>
        </div>
      </div>
    </div>

  </div>
{/if}

<style>
  .match-detail { padding-bottom: 24px; }

  .score-hero {
    background: var(--dark);
    padding: 20px 16px 24px;
    color: white;
  }

  .score-hero.live { background: linear-gradient(135deg, #1a0a0a, #2d0808); }

  .comp-label {
    text-align: center;
    font-size: 0.75rem;
    font-weight: 600;
    color: rgba(255,255,255,0.5);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin-bottom: 20px;
  }

  .teams-score {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .team-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
  }

  .big-crest { width: 56px; height: 56px; object-fit: contain; }
  .big-crest-fallback { font-size: 2rem; }

  .team-nm {
    font-size: 0.78rem;
    font-weight: 700;
    color: white;
    text-align: center;
  }

  .middle {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    min-width: 100px;
  }

  .live-tag {
    background: #ff3b30;
    color: white;
    font-size: 0.6rem;
    font-weight: 800;
    padding: 3px 8px;
    border-radius: 4px;
    letter-spacing: 0.08em;
  }

  .live-min { font-size: 0.75rem; color: #ff3b30; font-weight: 700; }

  .big-score {
    font-size: 2.2rem;
    font-weight: 900;
    color: white;
    letter-spacing: -0.02em;
  }

  .big-score.vs { font-size: 1.4rem; color: rgba(255,255,255,0.4); }

  .ht-score { font-size: 0.7rem; color: rgba(255,255,255,0.4); }

  .status-s { font-size: 0.75rem; font-weight: 700; color: rgba(255,255,255,0.5); }
  .time-s { font-size: 1.4rem; font-weight: 800; color: white; }
  .date-s { font-size: 0.75rem; color: rgba(255,255,255,0.5); }

  .info-section {
    margin: 16px;
    background: var(--card);
    border-radius: var(--radius);
    overflow: hidden;
    border: 1px solid var(--border);
  }

  .info-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 13px 16px;
    border-bottom: 1px solid var(--border);
  }

  .info-row:last-child { border-bottom: none; }

  .info-label { font-size: 0.875rem; color: var(--fg-2); }

  .info-val { font-size: 0.875rem; font-weight: 600; color: var(--fg); }

  .info-val.status { color: var(--fg-3); }
  .info-val.status.live { color: #ff3b30; }

  .section-card {
    margin: 0 16px 16px;
    background: var(--card);
    border-radius: var(--radius);
    padding: 16px;
    border: 1px solid var(--border);
  }

  h3 { font-size: 0.875rem; font-weight: 700; color: var(--fg); margin-bottom: 14px; }

  .odds-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .odd-box {
    background: var(--bg-2);
    border-radius: var(--radius-sm);
    padding: 12px;
    text-align: center;
  }

  .odd-label { font-size: 0.75rem; font-weight: 700; color: var(--fg-3); margin-bottom: 4px; }
  .odd-val { font-size: 1.1rem; font-weight: 800; color: var(--primary); }

  .error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 24px;
    gap: 12px;
    text-align: center;
    color: var(--fg);
  }

  .error p:first-child { font-size: 2rem; }
  .sub { font-size: 0.8rem; color: var(--fg-3); }
</style>
<script>
  import { formatTime, formatDate, getScore } from '$lib/services/api.js';
  import { goto } from '$app/navigation';
  
  export let match;
  export let showDate = false;
  
  $: score = getScore(match);
  $: isLive = match.status === 'IN_PLAY' || match.status === 'PAUSED';
  $: isFinished = match.status === 'FINISHED';
  $: homeWin = isFinished && score && score.home > score.away;
  $: awayWin = isFinished && score && score.away > score.home;
</script>

<button class="card pressable" on:click={()=> goto(`/jogo/${match.id}`)}>
  {#if showDate}
    <p class="date-label">{formatDate(match.utcDate)}</p>
  {/if}

  <div class="row">
    <div class="team home" class:winner={homeWin}>
      <div class="crest">
        {#if match.homeTeam?.crest}
          <img src={match.homeTeam.crest} alt={match.homeTeam.shortName} on:error={e => e.target.style.display='none'} />
        {:else}
          <span class="crest-fallback">⚽</span>
        {/if}
      </div>
      <span class="team-name">{match.homeTeam?.shortName || match.homeTeam?.name || '—'}</span>
    </div>

    <div class="score-block">
      {#if isLive}
        <div class="live-badge">AO VIVO</div>
        <div class="score">
          <span>{score ? score.home : '-'}</span>
          <span class="sep">:</span>
          <span>{score ? score.away : '-'}</span>
        </div>
        {#if match.minute}
          <span class="minute">{match.minute}'</span>
        {/if}
      {:else if isFinished && score}
        <div class="score finished">
          <span class:dim={awayWin}>{score.home}</span>
          <span class="sep">-</span>
          <span class:dim={homeWin}>{score.away}</span>
        </div>
        <span class="status-label">FT</span>
      {:else if match.status === 'HALFTIME'}
        <div class="score">
          <span>{score ? score.home : '-'}</span>
          <span class="sep">:</span>
          <span>{score ? score.away : '-'}</span>
        </div>
        <span class="status-label">Int.</span>
      {:else}
        <span class="time">{formatTime(match.utcDate)}</span>
        <span class="status-label vs">VS</span>
      {/if}
    </div>

    <div class="team away" class:winner={awayWin}>
      <div class="crest">
        {#if match.awayTeam?.crest}
          <img src={match.awayTeam.crest} alt={match.awayTeam.shortName} on:error={e => e.target.style.display='none'} />
        {:else}
          <span class="crest-fallback">⚽</span>
        {/if}
      </div>
      <span class="team-name">{match.awayTeam?.shortName || match.awayTeam?.name || '—'}</span>
    </div>
  </div>
</button>

<style>
  .card {
    width: 100%;
    background: var(--card);
    border-bottom: 1px solid var(--border);
    padding: 12px 16px;
    display: block;
    text-align: left;
  }

  .date-label {
    font-size: 0.7rem;
    font-weight: 600;
    color: var(--fg-3);
    text-transform: uppercase;
    letter-spacing: 0.06em;
    margin-bottom: 8px;
  }

  .row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .team {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
  }

  .home { align-items: flex-start; }
  .away { align-items: flex-end; }

  .crest {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .crest img {
    width: 32px;
    height: 32px;
    object-fit: contain;
  }

  .crest-fallback { font-size: 1.2rem; }

  .team-name {
    font-size: 0.78rem;
    font-weight: 600;
    color: var(--fg);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 100px;
  }

  .team.winner .team-name { color: var(--primary); }

  .score-block {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    min-width: 80px;
  }

  .live-badge {
    background: #ff3b30;
    color: white;
    font-size: 0.55rem;
    font-weight: 800;
    letter-spacing: 0.08em;
    padding: 2px 6px;
    border-radius: 4px;
  }

  .score {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 1.25rem;
    font-weight: 800;
    color: var(--fg);
  }

  .score.finished { font-size: 1.1rem; }

  .sep { color: var(--fg-3); }

  .dim { color: var(--fg-3); }

  .minute {
    font-size: 0.7rem;
    color: #ff3b30;
    font-weight: 700;
  }

  .status-label {
    font-size: 0.65rem;
    font-weight: 700;
    color: var(--fg-3);
    letter-spacing: 0.06em;
  }

  .status-label.vs { color: var(--primary); }

  .time {
    font-size: 1.1rem;
    font-weight: 700;
    color: var(--fg);
  }
</style>
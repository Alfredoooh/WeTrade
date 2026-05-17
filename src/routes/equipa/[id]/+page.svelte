<script>
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import AppBar from '$lib/components/AppBar.svelte';
  import MatchCard from '$lib/components/MatchCard.svelte';
  import Loader from '$lib/components/Loader.svelte';
  import { getTeam, getTeamMatches } from '$lib/services/api.js';
  import { favorites, toggleFavoriteTeam } from '$lib/stores/app.js';
  import { goto } from '$app/navigation';

  $: id = $page.params.id;
  let team = null, matches = [], loading = true;
  const tabs = ['Info', 'Jogos', 'Plantel'];
  let activeTab = 0;

  onMount(async () => {
    const [t, m] = await Promise.all([getTeam(id), getTeamMatches(id)]);
    team = t;
    matches = m?.matches || [];
    loading = false;
  });

  $: isFav = $favorites.teams.some(t => t.id === parseInt(id));
</script>

{#if loading}
  <AppBar title="Equipa" showBack={true} showSearch={false} showNotif={false} />
  <Loader />
{:else if !team || team.message}
  <AppBar title="Equipa" showBack={true} showSearch={false} showNotif={false} />
  <div class="error"><p>⚽</p><p>Equipa não disponível</p></div>
{:else}
  <AppBar title={team.shortName || team.name} showBack={true} showSearch={false} showNotif={false} />

  <div class="team-hero">
    {#if team.crest}
      <img src={team.crest} alt={team.name} class="hero-crest" on:error={e => e.target.style.display='none'} />
    {:else}
      <div class="hero-crest-fallback">⚽</div>
    {/if}
    <div class="hero-info">
      <h1>{team.name}</h1>
      <p class="hero-sub">{team.area?.name || ''} · {team.founded ? `Fundado em ${team.founded}` : ''}</p>
    </div>
    <button class="fav-btn pressable" on:click={() => toggleFavoriteTeam({ id: team.id, name: team.name, crest: team.crest })}>
      <span class="star" class:active={isFav}>★</span>
    </button>
  </div>

  <div class="tabs-row">
    {#each tabs as t, i}
      <button class="tab pressable" class:active={activeTab === i} on:click={() => activeTab = i}>{t}</button>
    {/each}
  </div>

  {#if activeTab === 0}
    <div class="info-section">
      {#each [
        ['Nome', team.name],
        ['Abreviatura', team.shortName],
        ['Fundação', team.founded],
        ['País', team.area?.name],
        ['Estádio', team.venue],
        ['Website', team.website],
        ['Cores', team.clubColors],
      ] as [label, val]}
        {#if val}
          <div class="info-row">
            <span class="info-label">{label}</span>
            <span class="info-val">{val}</span>
          </div>
        {/if}
      {/each}
    </div>

    {#if team.runningCompetitions?.length > 0}
      <div class="comps-section">
        <h3>Competições</h3>
        {#each team.runningCompetitions as c}
          <button class="comp-row pressable" on:click={() => goto(`/liga/${c.code}`)}>
            <span class="comp-emblem">{c.emblem ? '' : '🏆'}</span>
            <span>{c.name}</span>
          </button>
        {/each}
      </div>
    {/if}

  {:else if activeTab === 1}
    {#if matches.length === 0}
      <div class="empty-msg"><p>Sem jogos disponíveis</p></div>
    {:else}
      {#each matches as match}
        <MatchCard {match} showDate={true} />
      {/each}
    {/if}

  {:else}
    {#if team.squad?.length > 0}
      {#each team.squad as player}
        <button class="player-row pressable" on:click={() => goto(`/jogador/${player.id}`)}>
          <div class="player-num">{player.shirtNumber || '—'}</div>
          <div class="player-info">
            <p class="player-name">{player.name}</p>
            <p class="player-pos">{player.position || ''}</p>
          </div>
          <span class="player-nat">{player.nationality || ''}</span>
        </button>
      {/each}
    {:else}
      <div class="empty-msg"><p>Plantel não disponível</p></div>
    {/if}
  {/if}
{/if}

<style>
  .team-hero {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 20px 16px;
    border-bottom: 1px solid var(--border);
  }

  .hero-crest { width: 60px; height: 60px; object-fit: contain; }
  .hero-crest-fallback { font-size: 2.5rem; }

  .hero-info { flex: 1; }

  h1 { font-size: 1.1rem; font-weight: 800; color: var(--fg); }

  .hero-sub { font-size: 0.78rem; color: var(--fg-3); margin-top: 3px; }

  .fav-btn { padding: 8px; }

  .star { font-size: 1.6rem; color: var(--fg-3); transition: color 0.15s; }
  .star.active { color: #f4b400; }

  .tabs-row {
    display: flex;
    border-bottom: 1px solid var(--border);
    padding: 0 4px;
    background: var(--bg);
    position: sticky;
    top: var(--appbar-h);
    z-index: 10;
  }

  .tab {
    flex: 1;
    padding: 12px 8px;
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--fg-3);
    border-bottom: 2px solid transparent;
    transition: color 0.15s, border-color 0.15s;
  }

  .tab.active { color: var(--primary); border-bottom-color: var(--primary); }

  .info-section {
    margin: 16px;
    background: var(--card);
    border-radius: var(--radius);
    border: 1px solid var(--border);
    overflow: hidden;
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 13px 16px;
    border-bottom: 1px solid var(--border);
  }

  .info-row:last-child { border-bottom: none; }

  .info-label { font-size: 0.875rem; color: var(--fg-2); }
  .info-val { font-size: 0.875rem; font-weight: 600; color: var(--fg); text-align: right; max-width: 60%; }

  .comps-section { padding: 16px; }
  h3 { font-size: 0.875rem; font-weight: 700; color: var(--fg); margin-bottom: 10px; }

  .comp-row {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 0;
    border-bottom: 1px solid var(--border);
    font-size: 0.9rem;
    font-weight: 500;
    color: var(--fg);
  }

  .player-row {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
  }

  .player-num {
    width: 30px;
    height: 30px;
    border-radius: 8px;
    background: var(--primary-soft);
    color: var(--primary);
    font-size: 0.8rem;
    font-weight: 800;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .player-info { flex: 1; text-align: left; }

  .player-name { font-weight: 600; font-size: 0.9rem; color: var(--fg); }
  .player-pos { font-size: 0.75rem; color: var(--fg-3); margin-top: 2px; }
  .player-nat { font-size: 0.8rem; color: var(--fg-3); }

  .empty-msg {
    padding: 40px 24px;
    text-align: center;
    color: var(--fg-3);
    font-size: 0.875rem;
  }

  .error {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 60px 24px;
    gap: 12px;
    text-align: center;
    color: var(--fg);
  }
</style>
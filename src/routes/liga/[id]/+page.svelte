<script>
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import AppBar from '$lib/components/AppBar.svelte';
  import MatchCard from '$lib/components/MatchCard.svelte';
  import Loader from '$lib/components/Loader.svelte';
  import Empty from '$lib/components/Empty.svelte';
  import { getMatches, getStandings, getTopScorers, COMPETITIONS } from '$lib/services/api.js';
  import { goto } from '$app/navigation';

  $: id = $page.params.id;
  $: comp = COMPETITIONS[id] || { id, name: id, flag: '🏆' };

  const tabs = ['Jogos', 'Tabela', 'Marcadores'];
  let activeTab = 0;
  let matches = [], standing = [], scorers = [];
  let loading = true;

  async function load() {
    loading = true;
    if (activeTab === 0) {
      const today = new Date();
      const from = new Date(today); from.setDate(from.getDate() - 3);
      const to = new Date(today); to.setDate(to.getDate() + 7);
      const fmt = d => d.toISOString().split('T')[0];
      const data = await getMatches(id, fmt(from), fmt(to));
      matches = data?.matches || [];
    } else if (activeTab === 1) {
      const data = await getStandings(id);
      standing = data?.standings?.[0]?.table || [];
    } else {
      const data = await getTopScorers(id);
      scorers = data?.scorers || [];
    }
    loading = false;
  }

  onMount(load);
  $: activeTab, id, load();
</script>

<AppBar title={comp.name} showBack={true} showSearch={false} showNotif={false} />

<div class="comp-hero">
  <span class="hero-flag">{comp.flag}</span>
  <h1>{comp.name}</h1>
</div>

<div class="tabs-row">
  {#each tabs as t, i}
    <button class="tab pressable" class:active={activeTab === i} on:click={() => activeTab = i}>{t}</button>
  {/each}
</div>

{#if loading}
  <Loader />
{:else if activeTab === 0}
  {#if matches.length === 0}
    <Empty emoji="📅" title="Sem jogos disponíveis" />
  {:else}
    {#each matches as match}
      <MatchCard {match} showDate={true} />
    {/each}
  {/if}
{:else if activeTab === 1}
  {#if standing.length === 0}
    <Empty emoji="📊" title="Tabela não disponível" />
  {:else}
    <div class="table-wrap">
      <div class="table-header">
        <span class="pos">#</span>
        <span class="t-name">Equipa</span>
        <span class="col">J</span>
        <span class="col">V</span>
        <span class="col">E</span>
        <span class="col">D</span>
        <span class="col">Pts</span>
      </div>
      {#each standing as row}
        <button class="table-row pressable" on:click={() => goto(`/equipa/${row.team.id}`)}>
          <span class="pos" class:top3={row.position <= 3} class:rel={row.position >= standing.length - 2}>
            {row.position}
          </span>
          <div class="t-name-wrap">
            {#if row.team.crest}
              <img src={row.team.crest} alt={row.team.shortName} class="mini-crest" on:error={e => e.target.style.display='none'} />
            {/if}
            <span class="t-name">{row.team.shortName || row.team.name}</span>
          </div>
          <span class="col">{row.playedGames}</span>
          <span class="col">{row.won}</span>
          <span class="col">{row.draw}</span>
          <span class="col">{row.lost}</span>
          <span class="col pts">{row.points}</span>
        </button>
      {/each}
    </div>
  {/if}
{:else}
  {#if scorers.length === 0}
    <Empty emoji="⚽" title="Marcadores não disponíveis" />
  {:else}
    {#each scorers as s, i}
      <button class="scorer-row pressable" on:click={() => goto(`/jogador/${s.player.id}`)}>
        <span class="scorer-pos">{i + 1}</span>
        <div class="scorer-info">
          <p class="scorer-name">{s.player.name}</p>
          <p class="scorer-team">{s.team?.name || '—'}</p>
        </div>
        <div class="scorer-goals">
          <span class="goals-n">{s.goals}</span>
          <span class="goals-l">golos</span>
        </div>
      </button>
    {/each}
  {/if}
{/if}

<style>
  .comp-hero {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 16px 12px;
    border-bottom: 1px solid var(--border);
  }

  .hero-flag { font-size: 2rem; }

  h1 { font-size: 1.2rem; font-weight: 800; color: var(--fg); }

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

  .table-wrap { overflow-x: auto; }

  .table-header, .table-row {
    display: grid;
    grid-template-columns: 28px 1fr 28px 28px 28px 28px 32px;
    align-items: center;
    padding: 10px 16px;
    gap: 4px;
  }

  .table-header {
    background: var(--bg-2);
    font-size: 0.7rem;
    font-weight: 700;
    color: var(--fg-3);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .table-row {
    width: 100%;
    border-bottom: 1px solid var(--border);
  }

  .pos { font-size: 0.8rem; font-weight: 700; color: var(--fg-3); text-align: center; }
  .pos.top3 { color: var(--primary); }
  .pos.rel { color: #ff3b30; }

  .t-name-wrap {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .mini-crest { width: 18px; height: 18px; object-fit: contain; flex-shrink: 0; }

  .t-name { font-size: 0.8rem; font-weight: 600; color: var(--fg); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

  .col { font-size: 0.8rem; color: var(--fg-2); text-align: center; }

  .pts { font-weight: 800; color: var(--fg); }

  .scorer-row {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
  }

  .scorer-pos {
    font-size: 1rem;
    font-weight: 800;
    color: var(--fg-3);
    width: 24px;
    text-align: center;
  }

  .scorer-info { flex: 1; text-align: left; }

  .scorer-name { font-weight: 700; font-size: 0.9rem; color: var(--fg); }

  .scorer-team { font-size: 0.75rem; color: var(--fg-3); margin-top: 2px; }

  .scorer-goals { display: flex; flex-direction: column; align-items: center; }

  .goals-n { font-size: 1.2rem; font-weight: 800; color: var(--primary); }

  .goals-l { font-size: 0.65rem; color: var(--fg-3); font-weight: 600; }
</style>
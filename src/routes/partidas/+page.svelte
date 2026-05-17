<script>
  import { onMount } from 'svelte';
  import AppBar from '$lib/components/AppBar.svelte';
  import MatchCard from '$lib/components/MatchCard.svelte';
  import Loader from '$lib/components/Loader.svelte';
  import Empty from '$lib/components/Empty.svelte';
  import { getMatches, COMPETITIONS } from '$lib/services/api.js';

  const tabs = ['Hoje', 'Amanhã', 'Esta Semana'];
  let activeTab = 0;
  let activeComp = 'CL';
  let matches = [];
  let loading = true;

  const compIds = Object.keys(COMPETITIONS);

  function getDateRange(tab) {
    const now = new Date();
    const fmt = d => d.toISOString().split('T')[0];
    if (tab === 0) return { from: fmt(now), to: fmt(now) };
    if (tab === 1) {
      const t = new Date(now); t.setDate(t.getDate() + 1);
      return { from: fmt(t), to: fmt(t) };
    }
    const end = new Date(now); end.setDate(end.getDate() + 7);
    return { from: fmt(now), to: fmt(end) };
  }

  async function load() {
    loading = true;
    const { from, to } = getDateRange(activeTab);
    const data = await getMatches(activeComp, from, to);
    matches = data?.matches || [];
    loading = false;
  }

  onMount(load);

  $: activeTab, activeComp, load();
</script>

<AppBar title="Partidas" showSearch={false} />

<div class="tabs-scroll">
  {#each tabs as tab, i}
    <button class="tab pressable" class:active={activeTab === i} on:click={() => activeTab = i}>
      {tab}
    </button>
  {/each}
</div>

<div class="comp-scroll">
  {#each compIds as id}
    {@const c = COMPETITIONS[id]}
    <button class="comp-chip pressable" class:active={activeComp === id} on:click={() => activeComp = id}>
      {c.flag} {c.name}
    </button>
  {/each}
</div>

<div class="list">
  {#if loading}
    <Loader />
  {:else if matches.length === 0}
    <Empty emoji="📅" title="Sem jogos" sub="Não há jogos para este período nesta competição" />
  {:else}
    {#each matches as match}
      <MatchCard {match} showDate={activeTab === 2} />
    {/each}
  {/if}
</div>

<style>
  .tabs-scroll {
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

  .tab.active {
    color: var(--primary);
    border-bottom-color: var(--primary);
  }

  .comp-scroll {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    padding: 10px 16px;
    border-bottom: 1px solid var(--border);
  }

  .comp-chip {
    display: flex;
    align-items: center;
    gap: 5px;
    flex-shrink: 0;
    padding: 6px 12px;
    border-radius: 999px;
    font-size: 0.8rem;
    font-weight: 600;
    background: var(--bg-2);
    color: var(--fg-2);
    white-space: nowrap;
    border: 1.5px solid transparent;
    transition: all 0.15s;
  }

  .comp-chip.active {
    background: var(--primary-soft);
    color: var(--primary);
    border-color: var(--primary);
  }

  .list { padding-bottom: 16px; }
</style>
<script>
  import { onMount } from 'svelte';
  import AppBar from '$lib/components/AppBar.svelte';
  import MatchCard from '$lib/components/MatchCard.svelte';
  import Loader from '$lib/components/Loader.svelte';
  import Empty from '$lib/components/Empty.svelte';
  import { favorites, toggleFavoriteComp } from '$lib/stores/app.js';
  import { getMatches, COMPETITIONS } from '$lib/services/api.js';
  import { goto } from '$app/navigation';
  
  let matches = [];
  let loading = false;
  let activeTab = 0;
  const tabs = ['Equipas', 'Competições'];
  
  const allComps = Object.values(COMPETITIONS);
  
  async function loadFavMatches() {
    if ($favorites.competitions.length === 0) { matches = []; return; }
    loading = true;
    const today = new Date().toISOString().split('T')[0];
    const results = await Promise.all(
      $favorites.competitions.slice(0, 3).map(c => getMatches(c.id, today, today))
    );
    matches = results.flatMap(r => r?.matches || []);
    loading = false;
  }
  
  onMount(loadFavMatches);
</script>

<AppBar title="Favoritos" showSearch={false} />

<div class="tabs-row">
  {#each tabs as t, i}
    <button class="tab pressable" class:active={activeTab === i} on:click={() => activeTab = i}>{t}</button>
  {/each}
</div>

{#if activeTab === 0}
  {#if $favorites.teams.length === 0}
    <Empty emoji="⭐" title="Sem equipas favoritas" sub="Adiciona equipas como favoritas para as ver aqui" />
  {:else}
    {#each $favorites.teams as team}
      <button class="team-row pressable" on:click={() => goto(`/equipa/${team.id}`)}>
        <div class="crest">
          {#if team.crest}
            <img src={team.crest} alt={team.name} />
          {:else}
            <span>⚽</span>
          {/if}
        </div>
        <span class="team-name">{team.name}</span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="chev">
          <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
        </svg>
      </button>
    {/each}
  {/if}
{:else}
  <div class="comp-list">
    {#each allComps as comp}
      {@const isFav = $favorites.competitions.some(c => c.id === comp.id)}
      <button class="comp-row pressable" on:click={() => toggleFavoriteComp(comp)}>
        <span class="flag">{comp.flag}</span>
        <span class="comp-name">{comp.name}</span>
        <div class="star" class:active={isFav}>★</div>
      </button>
    {/each}
  </div>
{/if}

<style>
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

  .team-row {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
  }

  .crest {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .crest img { width: 36px; height: 36px; object-fit: contain; }

  .team-name { flex: 1; font-weight: 600; font-size: 0.9375rem; color: var(--fg); text-align: left; }

  .chev { color: var(--fg-3); }

  .comp-list { padding: 8px 0; }

  .comp-row {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
  }

  .flag { font-size: 1.4rem; }
  .comp-name { flex: 1; font-weight: 600; font-size: 0.9375rem; color: var(--fg); text-align: left; }

  .star {
    font-size: 1.4rem;
    color: var(--fg-3);
    transition: color 0.15s;
  }

  .star.active { color: #f4b400; }
</style>
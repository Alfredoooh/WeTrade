<script>
  import { goto } from '$app/navigation';
  import { COMPETITIONS } from '$lib/services/api.js';
  
  export let compId;
  export let matchCount = null;
  
  $: comp = COMPETITIONS[compId] || { id: compId, name: compId, flag: '🏆' };
</script>

<button class="card pressable" on:click={()=> goto(`/liga/${compId}`)}>
  <span class="flag">{comp.flag}</span>
  <div class="info">
    <p class="name">{comp.name}</p>
    {#if matchCount !== null}
      <p class="sub">{matchCount} jogos</p>
    {/if}
  </div>
  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="chevron">
    <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
  </svg>
</button>

<style>
  .card {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
    background: var(--card);
  }

  .flag { font-size: 1.5rem; }

  .info { flex: 1; text-align: left; }

  .name { font-weight: 600; font-size: 0.9375rem; color: var(--fg); }

  .sub { font-size: 0.75rem; color: var(--fg-3); margin-top: 2px; }

  .chevron { color: var(--fg-3); flex-shrink: 0; }
</style>
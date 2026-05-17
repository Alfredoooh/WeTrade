<script>
  import { goto } from '$app/navigation';
  import { COMPETITIONS } from '$lib/services/api.js';

  let query = '';
  let input;

  const quickAccess = [
    { label: 'Champions League', href: '/liga/CL', emoji: '🏆' },
    { label: 'Premier League',   href: '/liga/PL', emoji: '🏴󠁧󠁢󠁥󠁮󠁧󠁿' },
    { label: 'La Liga',          href: '/liga/PD', emoji: '🇪🇸' },
    { label: 'Liga Portugal',    href: '/liga/PPL', emoji: '🇵🇹' },
    { label: 'Bundesliga',       href: '/liga/BL1', emoji: '🇩🇪' },
    { label: 'Serie A',          href: '/liga/SA', emoji: '🇮🇹' },
  ];

  const categories = [
    { label: 'Ao Vivo',       href: '/ao-vivo',       emoji: '🔴' },
    { label: 'Partidas',      href: '/partidas',       emoji: '📅' },
    { label: 'Tabelas',       href: '/tabela/CL',      emoji: '📊' },
    { label: 'Transferências',href: '/transferencias',  emoji: '🔄' },
    { label: 'Estatísticas',  href: '/estatisticas',   emoji: '📈' },
    { label: 'Seleções',      href: '/selecoes',       emoji: '🌍' },
    { label: 'Árbitros',      href: '/arbitros',       emoji: '🟨' },
    { label: 'Notícias',      href: '/noticias',       emoji: '📰' },
    { label: 'Calendário',    href: '/calendario',     emoji: '🗓' },
    { label: 'Competições',   href: '/competicoes',    emoji: '🏅' },
  ];

  $: filtered = query.length > 1
    ? Object.values(COMPETITIONS).filter(c =>
        c.name.toLowerCase().includes(query.toLowerCase())
      )
    : [];

  import { onMount } from 'svelte';
  onMount(() => input?.focus());
</script>

<div class="page">
  <div class="search-bar">
    <button class="back-btn pressable" on:click={() => history.back()}>
      <img src="/icons/back.svg" alt="voltar" />
    </button>
    <div class="input-wrap">
      <img src="/icons/search.svg" alt="" class="s-icon" />
      <input
        bind:this={input}
        bind:value={query}
        placeholder="Equipas, ligas, jogadores..."
        type="search"
      />
      {#if query}
        <button class="clear pressable" on:click={() => query = ''}>✕</button>
      {/if}
    </div>
  </div>

  {#if filtered.length > 0}
    <div class="results">
      {#each filtered as comp}
        <button class="result-row pressable" on:click={() => goto(`/liga/${comp.id}`)}>
          <span class="r-emoji">{comp.flag}</span>
          <div class="r-info">
            <p class="r-name">{comp.name}</p>
            <p class="r-sub">Competição</p>
          </div>
        </button>
      {/each}
    </div>
  {:else if query.length > 1}
    <div class="no-results">
      <p>Sem resultados para "{query}"</p>
    </div>
  {:else}
    <div class="discovery">
      <section class="disc-section">
        <h2>Acesso rápido</h2>
        <div class="quick-grid">
          {#each quickAccess as item}
            <button class="quick-card pressable" on:click={() => goto(item.href)}>
              <span class="q-emoji">{item.emoji}</span>
              <span class="q-label">{item.label}</span>
            </button>
          {/each}
        </div>
      </section>

      <section class="disc-section">
        <h2>Explorar</h2>
        <div class="cat-list">
          {#each categories as cat}
            <button class="cat-row pressable" on:click={() => goto(cat.href)}>
              <span class="cat-emoji">{cat.emoji}</span>
              <span class="cat-label">{cat.label}</span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" class="chev">
                <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
              </svg>
            </button>
          {/each}
        </div>
      </section>
    </div>
  {/if}
</div>

<style>
  .page { min-height: 100dvh; background: var(--bg); }

  .search-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    border-bottom: 1px solid var(--border);
    position: sticky;
    top: 0;
    background: var(--bg);
    z-index: 10;
  }

  .back-btn {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .back-btn img { width: 22px; height: 22px; }

  @media (prefers-color-scheme: dark) {
    .back-btn img, .s-icon { filter: invert(1); }
  }

  .input-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    background: var(--bg-2);
    border-radius: 12px;
    padding: 10px 14px;
  }

  .s-icon { width: 16px; height: 16px; opacity: 0.5; }

  input {
    flex: 1;
    background: none;
    font-size: 0.9375rem;
    color: var(--fg);
  }

  input::placeholder { color: var(--fg-3); }

  .clear {
    color: var(--fg-3);
    font-size: 0.875rem;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .results { padding: 8px 0; }

  .result-row {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
  }

  .r-emoji { font-size: 1.4rem; }
  .r-info { text-align: left; }
  .r-name { font-weight: 600; font-size: 0.9rem; color: var(--fg); }
  .r-sub { font-size: 0.75rem; color: var(--fg-3); margin-top: 2px; }

  .no-results {
    padding: 40px 24px;
    text-align: center;
    color: var(--fg-3);
    font-size: 0.875rem;
  }

  .discovery { padding: 8px 0; }

  .disc-section { margin-bottom: 24px; }

  h2 {
    font-size: 0.75rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--fg-3);
    padding: 12px 16px 8px;
  }

  .quick-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    padding: 0 16px;
  }

  .quick-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    background: var(--bg-2);
    border-radius: var(--radius);
    padding: 14px 8px;
  }

  .q-emoji { font-size: 1.4rem; }
  .q-label { font-size: 0.72rem; font-weight: 600; color: var(--fg-2); text-align: center; }

  .cat-list { padding: 0 16px; }

  .cat-row {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 13px 0;
    border-bottom: 1px solid var(--border);
  }

  .cat-emoji { font-size: 1.2rem; }
  .cat-label { flex: 1; font-weight: 500; font-size: 0.9375rem; color: var(--fg); text-align: left; }
  .chev { color: var(--fg-3); }
</style>
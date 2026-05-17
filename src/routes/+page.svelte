<script>
  import { onMount } from 'svelte';
  import AppBar from '$lib/components/AppBar.svelte';
  import MatchCard from '$lib/components/MatchCard.svelte';
  import CompCard from '$lib/components/CompCard.svelte';
  import Loader from '$lib/components/Loader.svelte';
  import { getLiveMatches, getTodayMatches, COMPETITIONS } from '$lib/services/api.js';
  import { goto } from '$app/navigation';
  
  let liveMatches = [];
  let todayMatches = [];
  let loading = true;
  
  const featuredComps = ['CL', 'PL', 'PD', 'BL1', 'SA', 'FL1', 'PPL'];
  
  const news = [
    { id: 1, title: 'Haaland marca hat-trick e City vence', comp: 'Premier League', time: '1h', emoji: '🔥' },
    { id: 2, title: 'Vinicius Jr. eleito melhor jogador do mês', comp: 'La Liga', time: '2h', emoji: '⭐' },
    { id: 3, title: 'Champions League: sorteio dos quartos de final', comp: 'Champions', time: '3h', emoji: '🏆' },
    { id: 4, title: 'Sporting qualifica-se para Europa League', comp: 'Liga Portugal', time: '5h', emoji: '🇵🇹' },
  ];
  
  onMount(async () => {
    const [live, today] = await Promise.all([getLiveMatches(), getTodayMatches()]);
    liveMatches = live?.matches || [];
    todayMatches = today?.matches || [];
    loading = false;
  });
</script>

<AppBar />

<div class="page">
  
  <!-- Hero live section -->
  {#if liveMatches.length > 0}
    <section class="section">
      <div class="section-header">
        <div class="live-indicator">
          <span class="dot"></span>
          <span>AO VIVO</span>
        </div>
        <button class="see-all pressable" on:click={() => goto('/ao-vivo')}>Ver tudo</button>
      </div>
      {#each liveMatches.slice(0, 3) as match}
        <MatchCard {match} />
      {/each}
    </section>
  {/if}

  <!-- Today's matches -->
  <section class="section">
    <div class="section-header">
      <h2>Hoje</h2>
      <button class="see-all pressable" on:click={() => goto('/partidas')}>Ver tudo</button>
    </div>
    {#if loading}
      <Loader />
    {:else if todayMatches.length === 0}
      <p class="empty-inline">Sem jogos hoje</p>
    {:else}
      {#each todayMatches.slice(0, 5) as match}
        <MatchCard {match} />
      {/each}
    {/if}
  </section>

  <!-- Featured competitions -->
  <section class="section">
    <div class="section-header">
      <h2>Competições</h2>
      <button class="see-all pressable" on:click={() => goto('/competicoes')}>Ver todas</button>
    </div>
    <div class="comp-scroll">
      {#each featuredComps as id}
        {@const comp = COMPETITIONS[id]}
        <button class="comp-pill pressable" on:click={() => goto(`/liga/${id}`)}>
          <span>{comp.flag}</span>
          <span>{comp.name}</span>
        </button>
      {/each}
    </div>
  </section>

  <!-- News feed -->
  <section class="section">
    <div class="section-header">
      <h2>Notícias</h2>
      <button class="see-all pressable" on:click={() => goto('/noticias')}>Ver tudo</button>
    </div>
    {#each news as item}
      <button class="news-item pressable" on:click={() => goto('/noticias')}>
        <span class="news-emoji">{item.emoji}</span>
        <div class="news-info">
          <p class="news-title">{item.title}</p>
          <p class="news-meta">{item.comp} · {item.time}</p>
        </div>
      </button>
    {/each}
  </section>

</div>

<style>
  .page { padding-bottom: 8px; }

  .section { margin-bottom: 4px; }

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 16px 10px;
  }

  h2 {
    font-size: 1rem;
    font-weight: 700;
    color: var(--fg);
  }

  .see-all {
    font-size: 0.8125rem;
    font-weight: 600;
    color: var(--primary);
  }

  .live-indicator {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 0.8125rem;
    font-weight: 700;
    color: #ff3b30;
  }

  .dot {
    width: 7px;
    height: 7px;
    background: #ff3b30;
    border-radius: 50%;
    animation: pulse 1.2s infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.4; transform: scale(0.8); }
  }

  .comp-scroll {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    padding: 0 16px 12px;
  }

  .comp-pill {
    display: flex;
    align-items: center;
    gap: 6px;
    background: var(--bg-2);
    border-radius: 999px;
    padding: 8px 14px;
    font-size: 0.8125rem;
    font-weight: 600;
    color: var(--fg);
    white-space: nowrap;
    flex-shrink: 0;
  }

  .news-item {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
  }

  .news-emoji { font-size: 1.5rem; flex-shrink: 0; }

  .news-info { flex: 1; text-align: left; }

  .news-title {
    font-size: 0.9rem;
    font-weight: 600;
    color: var(--fg);
    line-height: 1.3;
  }

  .news-meta {
    font-size: 0.75rem;
    color: var(--fg-3);
    margin-top: 3px;
  }

  .empty-inline {
    font-size: 0.875rem;
    color: var(--fg-3);
    padding: 12px 16px;
    text-align: center;
  }
</style>
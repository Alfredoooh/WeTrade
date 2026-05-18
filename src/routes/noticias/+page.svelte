<script>
  import { onMount } from 'svelte';
  import AppBar from '$lib/components/AppBar.svelte';
  import Loader from '$lib/components/Loader.svelte';
  import { getFootballNews } from '$lib/services/news.js';
  import { goto } from '$app/navigation';
  
  let news = [];
  let loading = true;
  
  onMount(async () => {
    news = await getFootballNews();
    loading = false;
  });
</script>

<AppBar title="Noticias" showBack={true} showSearch={false} showNotif={false} />

{#if loading}
  <Loader />
{:else if news.length === 0}
  <div class="empty"><p>Sem noticias disponiveis</p></div>
{:else}
  {#each news as item, i}
    <button class="news-item pressable" on:click={() => goto(`/noticias/${i}?title=${encodeURIComponent(item.title)}&desc=${encodeURIComponent(item.description)}&source=${encodeURIComponent(item.source)}&date=${encodeURIComponent(item.pubDate)}&link=${encodeURIComponent(item.link)}`)}>
      {#if item.image}
        <img src={item.image} alt="" class="thumb" on:error={e => e.target.style.display='none'} />
      {:else}
        <div class="thumb-fallback"></div>
      {/if}
      <div class="info">
        <p class="title">{item.title}</p>
        <p class="meta">{item.source} · {item.pubDate ? new Date(item.pubDate).toLocaleDateString('pt-PT') : ''}</p>
      </div>
    </button>
  {/each}
{/if}

<style>
  .news-item {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
    text-align: left;
  }

  .thumb { width: 72px; height: 72px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }

  .thumb-fallback { width: 72px; height: 72px; border-radius: 10px; background: var(--bg-3); flex-shrink: 0; }

  .info { flex: 1; min-width: 0; }

  .title {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--fg);
    line-height: 1.35;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .meta { font-size: 0.72rem; color: var(--fg-3); margin-top: 4px; }

  .empty { padding: 60px 24px; text-align: center; color: var(--fg-3); font-size: 0.875rem; }
</style>
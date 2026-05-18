<script>
  import { page } from '$app/stores';
  import AppBar from '$lib/components/AppBar.svelte';
  
  $: params = $page.url.searchParams;
  $: title = params.get('title') || '';
  $: desc = params.get('desc') || '';
  $: source = params.get('source') || '';
  $: date = params.get('date') || '';
  $: link = params.get('link') || '';
</script>

<AppBar title="Noticia" showBack={true} showSearch={false} showNotif={false} />

<div class="detail">
  <div class="meta-row">
    <span class="source">{source}</span>
    <span class="date">{date ? new Date(date).toLocaleDateString('pt-PT', { day: '2-digit', month: 'long', year: 'numeric' }) : ''}</span>
  </div>
  
  <h1 class="title">{title}</h1>
  
  <p class="desc">{desc}</p>
  
  {#if link}
    <a href={link} target="_blank" rel="noreferrer" class="read-more">
      Ler artigo completo
    </a>
  {/if}
</div>

<style>
  .detail { padding: 20px 16px; }

  .meta-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
  }

  .source {
    font-size: 0.75rem;
    font-weight: 700;
    color: var(--primary);
    text-transform: uppercase;
    letter-spacing: 0.06em;
  }

  .date { font-size: 0.75rem; color: var(--fg-3); }

  .title {
    font-size: 1.2rem;
    font-weight: 800;
    color: var(--fg);
    line-height: 1.3;
    letter-spacing: -0.02em;
    margin-bottom: 16px;
  }

  .desc {
    font-size: 0.9375rem;
    color: var(--fg-2);
    line-height: 1.6;
    margin-bottom: 24px;
  }

  .read-more {
    display: block;
    text-align: center;
    padding: 14px;
    background: var(--primary);
    color: white;
    border-radius: var(--radius);
    font-weight: 700;
    font-size: 0.9rem;
  }
</style>
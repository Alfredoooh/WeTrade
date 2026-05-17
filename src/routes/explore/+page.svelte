<script>
  import TopBar from '$lib/components/TopBar.svelte';

  let query = '';

  const tags = ['#tech', '#portugal', '#design', '#svelte', '#fitness', '#fotografia', '#música', '#viagens'];
  const trending = [
    { tag: '#SvelteKit', posts: '2.4k posts' },
    { tag: '#WebDev', posts: '18k posts' },
    { tag: '#Portugal', posts: '45k posts' },
    { tag: '#OpenSource', posts: '9.1k posts' },
    { tag: '#UX', posts: '6.7k posts' },
  ];
  const suggested = [
    { name: 'Marta Oliveira', username: 'martaoliv', seed: 'marta', color: 'ec4899' },
    { name: 'Ricardo Santos', username: 'ricardos', seed: 'ricardo', color: '0891b2' },
    { name: 'Sofia Mendes', username: 'sofiamend', seed: 'sofia', color: 'f59e0b' },
  ];
</script>

<TopBar title="Explorar" showNotif={false} />

<div class="page">
  <div class="search-wrap">
    <div class="search-box">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
      </svg>
      <input bind:value={query} placeholder="Pesquisar pessoas, tags..." />
    </div>
  </div>

  <div class="tags-scroll">
    {#each tags as tag}
      <button class="tag-chip">{tag}</button>
    {/each}
  </div>

  <section class="section">
    <h2>Em alta</h2>
    {#each trending as t}
      <div class="trend-row">
        <div>
          <p class="trend-tag">{t.tag}</p>
          <p class="trend-sub">{t.posts}</p>
        </div>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </div>
    {/each}
  </section>

  <section class="section">
    <h2>Sugeridos</h2>
    {#each suggested as u}
      <div class="user-row">
        <img src="https://api.dicebear.com/8.x/notionists/svg?seed={u.seed}&backgroundColor={u.color}" alt={u.name} class="u-avatar" />
        <div class="u-meta">
          <p class="u-name">{u.name}</p>
          <p class="u-sub">@{u.username}</p>
        </div>
        <button class="follow-btn">Seguir</button>
      </div>
    {/each}
  </section>
</div>

<style>
  .page { padding: 16px; }

  .search-wrap { margin-bottom: 12px; }

  .search-box {
    display: flex;
    align-items: center;
    gap: 10px;
    background: var(--bg-2);
    border-radius: 14px;
    padding: 11px 14px;
    color: var(--fg-3);
  }

  input {
    flex: 1;
    border: none;
    background: none;
    font-family: var(--font);
    font-size: 0.9375rem;
    color: var(--fg);
    outline: none;
  }

  input::placeholder { color: var(--fg-3); }

  .tags-scroll {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    margin-bottom: 24px;
    padding-bottom: 4px;
  }

  .tag-chip {
    flex-shrink: 0;
    background: var(--accent-soft);
    color: var(--accent);
    border-radius: 999px;
    padding: 7px 14px;
    font-size: 0.8125rem;
    font-weight: 600;
    transition: background 0.15s;
  }

  .section { margin-bottom: 28px; }

  h2 {
    font-size: 0.75rem;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--fg-3);
    margin-bottom: 12px;
  }

  .trend-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid var(--border);
    color: var(--fg-3);
  }

  .trend-tag { font-weight: 600; font-size: 0.9375rem; color: var(--fg); }
  .trend-sub { font-size: 0.75rem; margin-top: 2px; }

  .user-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 0;
    border-bottom: 1px solid var(--border);
  }

  .u-avatar { width: 44px; height: 44px; border-radius: 50%; background: var(--bg-3); }
  .u-meta { flex: 1; }
  .u-name { font-weight: 600; font-size: 0.9rem; }
  .u-sub { font-size: 0.75rem; color: var(--fg-3); }

  .follow-btn {
    background: var(--accent);
    color: white;
    border-radius: 999px;
    padding: 7px 18px;
    font-size: 0.8125rem;
    font-weight: 600;
    transition: opacity 0.15s;
  }
</style>
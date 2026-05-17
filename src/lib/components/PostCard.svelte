<script>
  import { toggleLike, toggleSave } from '$lib/stores/posts.js';
  export let post;
</script>

<article class="card">
  <header>
    <img class="avatar" src={post.user.avatar} alt={post.user.name} />
    <div class="meta">
      <span class="name">{post.user.name}</span>
      <span class="sub">@{post.user.username} · {post.time}</span>
    </div>
    <button class="more">⋯</button>
  </header>
  
  <p class="content">{post.content}</p>
  
  {#if post.image}
    <img class="post-img" src={post.image} alt="post" />
  {/if}

  <footer>
    <button class="action {post.liked ? 'active' : ''}" on:click={() => toggleLike(post.id)}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill={post.liked ? 'currentColor' : 'none'} stroke="currentColor" stroke-width="2">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
      </svg>
      <span>{post.likes}</span>
    </button>

    <button class="action">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
      <span>{post.comments}</span>
    </button>

    <button class="action">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
        <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
      </svg>
    </button>

    <button class="action save {post.saved ? 'active' : ''}" on:click={() => toggleSave(post.id)}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill={post.saved ? 'currentColor' : 'none'} stroke="currentColor" stroke-width="2">
        <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
      </svg>
    </button>
  </footer>
</article>

<style>
  .card {
    background: var(--card);
    border-bottom: 1px solid var(--border);
    padding: 16px;
  }

  header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
  }

  .avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    flex-shrink: 0;
    background: var(--bg-3);
  }

  .meta { flex: 1; min-width: 0; }

  .name {
    display: block;
    font-weight: 600;
    font-size: 0.875rem;
    color: var(--fg);
    line-height: 1.2;
  }

  .sub {
    font-size: 0.75rem;
    color: var(--fg-3);
    font-family: var(--font-mono);
  }

  .more {
    color: var(--fg-3);
    font-size: 1.2rem;
    padding: 4px 8px;
    border-radius: 8px;
  }

  .content {
    font-size: 0.9375rem;
    line-height: 1.55;
    color: var(--fg);
    margin-bottom: 12px;
  }

  .post-img {
    width: 100%;
    border-radius: var(--radius-sm);
    margin-bottom: 12px;
    object-fit: cover;
    max-height: 280px;
  }

  footer {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .action {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 7px 10px;
    border-radius: 999px;
    color: var(--fg-3);
    font-size: 0.8125rem;
    font-weight: 500;
    transition: background 0.15s, color 0.15s;
  }

  .action:active { background: var(--bg-3); }
  .action.active { color: var(--accent); }
  .save { margin-left: auto; }
</style>
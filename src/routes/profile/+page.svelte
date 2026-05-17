<script>
  import { currentUser } from '$lib/stores/user.js';
  import { posts } from '$lib/stores/posts.js';

  $: myPosts = $posts.filter(p => p.user.username === $currentUser.username);
</script>

<div class="page">
  <header class="profile-header">
    <img class="avatar" src={$currentUser.avatar} alt={$currentUser.name} />
    <div class="info">
      <h1>{$currentUser.name}</h1>
      <p class="username">@{$currentUser.username}</p>
      <p class="bio">{$currentUser.bio}</p>
    </div>
  </header>

  <div class="stats-row">
    <div class="stat">
      <span class="stat-n">{myPosts.length}</span>
      <span class="stat-l">Posts</span>
    </div>
    <div class="divider-v"></div>
    <div class="stat">
      <span class="stat-n">{$currentUser.followers}</span>
      <span class="stat-l">Seguidores</span>
    </div>
    <div class="divider-v"></div>
    <div class="stat">
      <span class="stat-n">{$currentUser.following}</span>
      <span class="stat-l">A seguir</span>
    </div>
  </div>

  <button class="edit-btn">Editar perfil</button>

  <div class="posts-label">
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
      <rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
    </svg>
    Posts
  </div>

  {#if myPosts.length === 0}
    <div class="empty">
      <p>Ainda não publicaste nada</p>
      <span>As tuas publicações aparecem aqui</span>
    </div>
  {:else}
    {#each myPosts as post (post.id)}
      <div class="post-stub">
        <p>{post.content}</p>
        <span class="time">{post.time}</span>
      </div>
    {/each}
  {/if}
</div>

<style>
  .page { padding-bottom: 16px; }
  .profile-header {
    display: flex;
    gap: 16px;
    padding: 24px 16px 16px;
    align-items: flex-start;
  }
  .avatar {
    width: 72px;
    height: 72px;
    border-radius: 50%;
    background: var(--bg-3);
    flex-shrink: 0;
  }
  .info { flex: 1; }
  h1 { font-size: 1.125rem; font-weight: 700; letter-spacing: -0.01em; }
  .username { font-size: 0.8125rem; color: var(--fg-3); font-family: var(--font-mono); margin: 2px 0 8px; }
  .bio { font-size: 0.875rem; color: var(--fg-2); line-height: 1.45; }
  .stats-row {
    display: flex;
    align-items: center;
    justify-content: space-around;
    padding: 16px;
    border-top: 1px solid var(--border);
    border-bottom: 1px solid var(--border);
    margin: 0 16px;
    border-radius: var(--radius-sm);
    background: var(--bg-2);
  }
  .stat { display: flex; flex-direction: column; align-items: center; gap: 2px; }
  .stat-n { font-size: 1.25rem; font-weight: 700; }
  .stat-l { font-size: 0.7rem; color: var(--fg-3); font-weight: 500; text-transform: uppercase; letter-spacing: 0.05em; }
  .divider-v { width: 1px; height: 32px; background: var(--border); }
  .edit-btn {
    display: block;
    width: calc(100% - 32px);
    margin: 16px auto;
    padding: 11px;
    border: 1.5px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 0.9rem;
    font-weight: 600;
    color: var(--fg);
    background: var(--card);
    text-align: center;
  }
  .posts-label {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    font-size: 0.8125rem;
    font-weight: 600;
    color: var(--fg-2);
    border-bottom: 1px solid var(--border);
    text-transform: uppercase;
    letter-spacing: 0.06em;
  }
  .post-stub {
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
  }
  .post-stub p {
    font-size: 0.9rem;
    line-height: 1.5;
    color: var(--fg);
    flex: 1;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .time { font-size: 0.75rem; color: var(--fg-3); font-family: var(--font-mono); flex-shrink: 0; }
  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 60px 32px;
    color: var(--fg-3);
    text-align: center;
    gap: 8px;
  }
  .empty p { font-size: 1rem; font-weight: 600; color: var(--fg-2); }
  .empty span { font-size: 0.875rem; }
</style>

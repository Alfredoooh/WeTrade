<script>
  import { goto } from '$app/navigation';
  import { addPost } from '$lib/stores/posts.js';

  let content = '';
  $: canPost = content.trim().length > 0;

  function publish() {
    if (!canPost) return;
    addPost(content.trim());
    goto('/');
  }
</script>

<div class="composer">
  <header>
    <button class="cancel" on:click={() => goto('/')}>Cancelar</button>
    <span class="title">Nova publicação</span>
    <button class="post-btn" disabled={!canPost} on:click={publish}>Publicar</button>
  </header>

  <div class="body">
    <img class="avatar" src="https://api.dicebear.com/8.x/notionists/svg?seed=alfred&backgroundColor=6d28d9" alt="eu" />
    <textarea
      bind:value={content}
      placeholder="O que tens na cabeça?"
      rows="6"
      autofocus
    ></textarea>
  </div>

  <div class="char-row">
    <div class="progress-ring">
      <svg width="28" height="28" viewBox="0 0 28 28">
        <circle cx="14" cy="14" r="11" fill="none" stroke="var(--border)" stroke-width="2.5"/>
        <circle
          cx="14" cy="14" r="11"
          fill="none"
          stroke={content.length > 260 ? 'var(--danger)' : 'var(--accent)'}
          stroke-width="2.5"
          stroke-dasharray={69.12}
          stroke-dashoffset={69.12 - (69.12 * Math.min(content.length / 280, 1))}
          stroke-linecap="round"
          transform="rotate(-90 14 14)"
        />
      </svg>
      {#if content.length > 220}
        <span class="char-count" class:danger={content.length > 260}>{280 - content.length}</span>
      {/if}
    </div>
    <div class="media-actions">
      <button class="media-btn">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/>
          <polyline points="21 15 16 10 5 21"/>
        </svg>
      </button>
      <button class="media-btn">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>
        </svg>
      </button>
    </div>
  </div>
</div>

<style>
  .composer {
    min-height: 100dvh;
    background: var(--bg);
    display: flex;
    flex-direction: column;
  }
  header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
    background: var(--card);
  }
  .title { font-weight: 600; font-size: 0.9375rem; }
  .cancel { color: var(--fg-2); font-size: 0.9375rem; }
  .post-btn {
    background: var(--accent);
    color: white;
    border-radius: 999px;
    padding: 8px 20px;
    font-size: 0.875rem;
    font-weight: 700;
    transition: opacity 0.15s;
  }
  .post-btn:disabled { opacity: 0.4; }
  .body {
    display: flex;
    gap: 12px;
    padding: 16px;
    flex: 1;
  }
  .avatar {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    flex-shrink: 0;
    background: var(--bg-3);
  }
  textarea {
    flex: 1;
    border: none;
    background: none;
    font-family: var(--font);
    font-size: 1rem;
    color: var(--fg);
    line-height: 1.55;
    resize: none;
    outline: none;
  }
  textarea::placeholder { color: var(--fg-3); }
  .char-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-top: 1px solid var(--border);
    gap: 12px;
  }
  .progress-ring { position: relative; display: flex; align-items: center; justify-content: center; }
  .char-count {
    position: absolute;
    font-size: 0.625rem;
    font-weight: 700;
    font-family: var(--font-mono);
    color: var(--fg-2);
  }
  .char-count.danger { color: var(--danger); }
  .media-actions { display: flex; gap: 4px; margin-left: auto; }
  .media-btn {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    color: var(--accent);
    display: flex;
    align-items: center;
    justify-content: center;
  }
</style>

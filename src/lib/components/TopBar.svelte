<script>
  export let title = 'Comunidade';
  export let showNotif = true;
</script>

<header class="topbar">
  <span class="wordmark">{title}</span>
  {#if showNotif}
    <button class="notif-btn" aria-label="Notificações">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
      </svg>
      <span class="badge"></span>
    </button>
  {/if}
</header>

<style>
  .topbar {
    position: sticky;
    top: 0;
    z-index: 50;
    height: var(--nav-h);
    background: var(--card);
    border-bottom: 1px solid var(--border);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px;
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
  }
  .wordmark {
    font-size: 1.125rem;
    font-weight: 700;
    color: var(--fg);
    letter-spacing: -0.02em;
  }
  .notif-btn {
    position: relative;
    width: 38px;
    height: 38px;
    border-radius: 12px;
    background: var(--bg-2);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--fg-2);
  }
  .badge {
    position: absolute;
    top: 6px;
    right: 6px;
    width: 8px;
    height: 8px;
    background: var(--accent);
    border-radius: 50%;
    border: 2px solid var(--card);
  }
</style>

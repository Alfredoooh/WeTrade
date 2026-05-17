<script>
  import { page } from '$app/stores';
</script>

<nav class="bottom-nav">
  <a href="/" class="tab" class:active={$page.url.pathname === '/'}>
    <svg width="22" height="22" viewBox="0 0 24 24" fill={$page.url.pathname === '/' ? 'currentColor' : 'none'} stroke="currentColor" stroke-width="2">
      <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
      <polyline points="9 22 9 12 15 12 15 22"/>
    </svg>
    <span>Feed</span>
  </a>

  <a href="/explore" class="tab" class:active={$page.url.pathname === '/explore'}>
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
    </svg>
    <span>Explorar</span>
  </a>

  <a href="/new" class="tab new-tab" class:active={$page.url.pathname === '/new'}>
    <div class="new-btn">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
      </svg>
    </div>
  </a>

  <a href="/saved" class="tab" class:active={$page.url.pathname === '/saved'}>
    <svg width="22" height="22" viewBox="0 0 24 24" fill={$page.url.pathname === '/saved' ? 'currentColor' : 'none'} stroke="currentColor" stroke-width="2">
      <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
    </svg>
    <span>Guardados</span>
  </a>

  <a href="/profile" class="tab" class:active={$page.url.pathname === '/profile'}>
    <svg width="22" height="22" viewBox="0 0 24 24" fill={$page.url.pathname === '/profile' ? 'currentColor' : 'none'} stroke="currentColor" stroke-width="2">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
      <circle cx="12" cy="7" r="4"/>
    </svg>
    <span>Perfil</span>
  </a>
</nav>

<style>
  .bottom-nav {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: var(--tab-h);
    background: var(--card);
    border-top: 1px solid var(--border);
    display: flex;
    align-items: center;
    padding: 0 4px;
    padding-bottom: env(safe-area-inset-bottom);
    z-index: 100;
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
  }

  .tab {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    color: var(--fg-3);
    padding: 6px 0;
    transition: color 0.15s;
    font-size: 0.65rem;
    font-weight: 500;
    letter-spacing: 0.02em;
  }

  .tab.active { color: var(--accent); }

  .new-tab { color: transparent; }

  .new-btn {
    width: 44px;
    height: 44px;
    background: var(--accent);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    box-shadow: 0 4px 14px rgba(109,40,217,0.4);
    transition: transform 0.15s, box-shadow 0.15s;
  }

  .new-btn:active {
    transform: scale(0.93);
    box-shadow: 0 2px 8px rgba(109,40,217,0.3);
  }
</style>
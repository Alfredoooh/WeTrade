<script>
  import { drawerOpen, closeDrawer } from '$lib/stores/app.js';
  import { goto } from '$app/navigation';
  
  const menuItems = [
    { icon: '/icons/profile.svg', label: 'Perfil', href: '/perfil' },
    { icon: '/icons/notifications.svg', label: 'Notificações', href: '/notificacoes' },
    { icon: '/icons/calendar.svg', label: 'Calendário', href: '/calendario' },
    { icon: '/icons/transfer.svg', label: 'Transferências', href: '/transferencias' },
    { icon: '/icons/referee.svg', label: 'Árbitros', href: '/arbitros' },
    { icon: '/icons/national.svg', label: 'Seleções', href: '/selecoes' },
    { icon: '/icons/table.svg', label: 'Competições', href: '/competicoes' },
    { icon: '/icons/stats.svg', label: 'Estatísticas', href: '/estatisticas' },
    { icon: '/icons/fun.svg', label: 'Diversão', href: '/diversao' },
    { icon: '/icons/news.svg', label: 'Notícias', href: '/noticias' },
    { icon: '/icons/settings.svg', label: 'Configurações', href: '/configuracoes' },
  ];
  
  function navigate(href) {
    closeDrawer();
    setTimeout(() => goto(href), 200);
  }
</script>

{#if $drawerOpen}
  <div class="overlay" on:click={closeDrawer} role="presentation"></div>
{/if}

<aside class="drawer" class:open={$drawerOpen}>
  <div class="drawer-header">
    <div class="user-info">
      <div class="avatar">⚽</div>
      <div>
        <p class="user-name">GoalZone</p>
        <p class="user-sub">A tua app de futebol</p>
      </div>
    </div>
    <button class="close-btn pressable" on:click={closeDrawer}>
      <img src="/icons/close.svg" alt="fechar" />
    </button>
  </div>

  <div class="divider"></div>

  <nav class="menu-list">
    {#each menuItems as item}
      <button class="menu-item pressable" on:click={() => navigate(item.href)}>
        <div class="menu-icon">
          <img src={item.icon} alt={item.label} />
        </div>
        <span>{item.label}</span>
        <svg class="chevron" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
        </svg>
      </button>
    {/each}
  </nav>

  <div class="drawer-footer">
    <p class="version">GoalZone v1.0</p>
  </div>
</aside>

<style>
  .overlay {
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,0.5);
    z-index: 150;
    animation: fadeIn 0.25s ease;
  }

  @keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
  }

  .drawer {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 82%;
    max-width: 320px;
    background: var(--bg);
    z-index: 200;
    display: flex;
    flex-direction: column;
    transform: translateX(-100%);
    transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
    will-change: transform;
    padding-top: var(--safe-top);
    overflow: hidden;
  }

  .drawer.open {
    transform: translateX(0);
    box-shadow: 4px 0 24px rgba(0,0,0,0.18);
  }

  .drawer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 16px 16px;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .avatar {
    width: 46px;
    height: 46px;
    border-radius: 50%;
    background: var(--primary-soft);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.3rem;
  }

  .user-name {
    font-weight: 700;
    font-size: 1rem;
    color: var(--fg);
  }

  .user-sub {
    font-size: 0.75rem;
    color: var(--fg-3);
    margin-top: 1px;
  }

  .close-btn {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--bg-2);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .close-btn img {
    width: 18px;
    height: 18px;
  }

  @media (prefers-color-scheme: dark) {
    .close-btn img { filter: invert(1); }
  }

  .divider {
    height: 1px;
    background: var(--border);
    margin: 0 16px;
  }

  .menu-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px 0;
  }

  .menu-item {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    color: var(--fg);
    font-size: 0.9375rem;
    font-weight: 500;
    text-align: left;
  }

  .menu-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: var(--bg-2);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .menu-icon img {
    width: 18px;
    height: 18px;
  }

  @media (prefers-color-scheme: dark) {
    .menu-icon img { filter: invert(1); }
  }

  .chevron {
    margin-left: auto;
    color: var(--fg-3);
  }

  .drawer-footer {
    padding: 16px;
    border-top: 1px solid var(--border);
  }

  .version {
    font-size: 0.75rem;
    color: var(--fg-3);
    text-align: center;
  }
</style>
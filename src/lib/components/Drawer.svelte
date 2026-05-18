<script>
  import { drawerOpen, closeDrawer } from '$lib/stores/app.js';
  import { goto } from '$app/navigation';
  
  const menuItems = [
    { icon: '/icons/profile.svg', label: 'Perfil', href: '/perfil' },
    { icon: '/icons/transfer.svg', label: 'Transferencias', href: '/transferencias' },
    { icon: '/icons/national.svg', label: 'Selecoes', href: '/selecoes' },
    { icon: '/icons/stats.svg', label: 'Estatisticas', href: '/estatisticas' },
    { icon: '/icons/referee.svg', label: 'Arbitros', href: '/arbitros' },
    { icon: '/icons/settings.svg', label: 'Configuracoes', href: '/configuracoes' },
  ];
  
  function navigate(href) {
    closeDrawer();
    setTimeout(() => goto(href), 220);
  }
</script>

{#if $drawerOpen}
  <div class="overlay" on:click={closeDrawer} role="presentation"></div>
{/if}

<aside class="drawer" class:open={$drawerOpen}>
  <div class="drawer-top">
    <div class="brand">
      <span class="brand-name">WeSports</span>
    </div>
    <button class="close-btn pressable" on:click={closeDrawer}>
      <img src="/icons/close.svg" alt="fechar" />
    </button>
  </div>

  <nav class="menu-list">
    {#each menuItems as item}
      <button class="menu-item pressable" on:click={() => navigate(item.href)}>
        <div class="menu-icon">
          <img src={item.icon} alt={item.label} />
        </div>
        <span>{item.label}</span>
        <svg class="chevron" width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
          <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
        </svg>
      </button>
    {/each}
  </nav>
</aside>

<style>
  .overlay {
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,0.45);
    z-index: 150;
    animation: fadeIn 0.22s ease;
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
    width: 78%;
    max-width: 300px;
    background: var(--bg);
    z-index: 200;
    display: flex;
    flex-direction: column;
    transform: translateX(-100%);
    transition: transform 0.26s cubic-bezier(0.4, 0, 0.2, 1);
    will-change: transform;
    padding-top: var(--safe-top);
  }

  .drawer.open {
    transform: translateX(0);
    box-shadow: 2px 0 20px rgba(0,0,0,0.15);
  }

  .drawer-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 18px 16px 14px;
    border-bottom: 1px solid var(--border);
  }

  .brand-name {
    font-size: 1.2rem;
    font-weight: 800;
    color: var(--primary);
    letter-spacing: -0.03em;
  }

  .close-btn {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: var(--bg-2);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .close-btn img { width: 16px; height: 16px; }

  @media (prefers-color-scheme: dark) {
    .close-btn img { filter: invert(1); }
  }

  .menu-list { flex: 1; overflow-y: auto; padding: 6px 0; }

  .menu-item {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 15px 16px;
    color: var(--fg);
    font-size: 0.9375rem;
    font-weight: 500;
    text-align: left;
    border-bottom: 1px solid var(--border);
  }

  .menu-icon {
    width: 34px;
    height: 34px;
    border-radius: 9px;
    background: var(--bg-2);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .menu-icon img { width: 17px; height: 17px; }

  @media (prefers-color-scheme: dark) {
    .menu-icon img { filter: invert(1); }
  }

  .chevron { margin-left: auto; color: var(--fg-3); }
</style>
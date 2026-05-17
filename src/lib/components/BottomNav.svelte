<script>
  import { page } from '$app/stores';
  
  const tabs = [
    { href: '/', icon: '/icons/home.svg', label: 'Início' },
    { href: '/partidas', icon: '/icons/matches.svg', label: 'Partidas' },
    { href: '/favoritos', icon: '/icons/favorites.svg', label: 'Favoritos' },
    { href: '/tv', icon: '/icons/tv.svg', label: 'TV' },
  ];
  
  $: current = $page.url.pathname;
</script>

<nav class="bottom-nav">
  {#each tabs as tab}
    {@const active = current === tab.href || (tab.href !== '/' && current.startsWith(tab.href))}
    <a href={tab.href} class="tab pressable" class:active>
      <div class="icon-wrap">
        <img src={tab.icon} alt={tab.label} class="tab-icon" />
        {#if active}<div class="indicator"></div>{/if}
      </div>
      <span class="label">{tab.label}</span>
    </a>
  {/each}
</nav>

<style>
  .bottom-nav {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: calc(var(--tab-h) + var(--safe-bottom));
    padding-bottom: var(--safe-bottom);
    background: var(--bg);
    border-top: 1px solid var(--border);
    display: flex;
    align-items: stretch;
    z-index: 80;
    max-width: 480px;
    margin: 0 auto;
  }

  .tab {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    color: var(--fg-3);
    position: relative;
    padding-top: 8px;
  }

  .tab.active { color: var(--primary); }

  .icon-wrap {
    position: relative;
    width: 26px;
    height: 26px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .tab-icon {
    width: 22px;
    height: 22px;
    opacity: 0.5;
    transition: opacity 0.15s;
    filter: none;
  }

  @media (prefers-color-scheme: dark) {
    .tab-icon { filter: invert(1); }
  }

  .tab.active .tab-icon { opacity: 1; }

  .indicator {
    position: absolute;
    bottom: -4px;
    left: 50%;
    transform: translateX(-50%);
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--primary);
  }

  .label {
    font-size: 0.65rem;
    font-weight: 600;
    letter-spacing: 0.01em;
  }
</style>
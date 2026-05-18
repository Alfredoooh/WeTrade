<script>
  import { toggleDrawer } from '$lib/stores/app.js';
  import { goto } from '$app/navigation';
  
  export let title = '';
  export let showBack = false;
  export let showSearch = true;
  export let showNotif = true;
  
  function goBack() {
    history.back();
  }
</script>

<header class="appbar">
  <div class="left">
    {#if showBack}
      <button class="icon-btn pressable" on:click={goBack}>
        <img src="/icons/back.svg" alt="voltar" />
      </button>
    {:else}
      <button class="icon-btn pressable" on:click={toggleDrawer}>
        <img src="/icons/menu.svg" alt="menu" />
      </button>
    {/if}
  </div>

  {#if title}
    <h1 class="title">{title}</h1>
  {:else}
    <span class="logo">WeSports</span>
  {/if}

  <div class="right">
    {#if showSearch}
      <button class="icon-btn pressable" on:click={() => goto('/pesquisar')}>
        <img src="/icons/search.svg" alt="pesquisar" />
      </button>
    {/if}
    {#if showNotif}
      <button class="icon-btn pressable notif-wrap" on:click={() => goto('/notificacoes')}>
        <img src="/icons/notifications.svg" alt="notificacoes" />
        <span class="notif-dot"></span>
      </button>
    {/if}
  </div>
</header>

<style>
  .appbar {
    position: sticky;
    top: 0;
    z-index: 90;
    height: var(--appbar-h);
    background: var(--bg);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 8px;
    border-bottom: 1px solid var(--border);
  }

  .left, .right {
    display: flex;
    align-items: center;
    gap: 2px;
    min-width: 56px;
  }

  .right { justify-content: flex-end; }

  .icon-btn {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .icon-btn img { width: 22px; height: 22px; }

  @media (prefers-color-scheme: dark) {
    .icon-btn img { filter: invert(1); }
  }

  .logo {
    font-size: 1.15rem;
    font-weight: 800;
    color: var(--primary);
    letter-spacing: -0.03em;
  }

  .title {
    font-size: 1rem;
    font-weight: 700;
    color: var(--fg);
    letter-spacing: -0.01em;
  }

  .notif-wrap { position: relative; }

  .notif-dot {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 7px;
    height: 7px;
    background: #ff3b30;
    border-radius: 50%;
    border: 1.5px solid var(--bg);
  }
</style>
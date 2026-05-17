<script>
  import '../app.css';
  import BottomNav from '$lib/components/BottomNav.svelte';
  import Drawer from '$lib/components/Drawer.svelte';
  import { page } from '$app/stores';
  
  $: noNav = ['/pesquisar'].includes($page.url.pathname);
</script>

<div class="shell">
  <Drawer />
  <main class="main" class:no-nav={noNav}>
    <slot />
  </main>
  {#if !noNav}
    <BottomNav />
  {/if}
</div>

<style>
  .shell {
    max-width: 480px;
    margin: 0 auto;
    min-height: 100dvh;
    position: relative;
    background: var(--bg);
    overflow-x: hidden;
  }

  .main {
    padding-bottom: calc(var(--tab-h) + var(--safe-bottom));
    min-height: 100dvh;
  }

  .main.no-nav { padding-bottom: 0; }
</style>
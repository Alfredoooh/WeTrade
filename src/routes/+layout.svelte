<script>
  import '../app.css';
  import BottomNav from '$lib/components/BottomNav.svelte';
  import { page } from '$app/stores';
  
  $: isNew = $page.url.pathname === '/new';
</script>

<div class="app">
  <main class="main" class:no-pad={isNew}>
    <slot />
  </main>
  <BottomNav />
</div>

<style>
  .app {
    min-height: 100dvh;
    max-width: 480px;
    margin: 0 auto;
    position: relative;
    background: var(--bg);
  }
  
  .main {
    padding-bottom: calc(var(--tab-h) + env(safe-area-inset-bottom));
  }
</style>
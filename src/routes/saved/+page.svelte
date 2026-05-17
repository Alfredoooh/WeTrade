<script>
  import TopBar from '$lib/components/TopBar.svelte';
  import PostCard from '$lib/components/PostCard.svelte';
  import { posts } from '$lib/stores/posts.js';
  
  $: saved = $posts.filter(p => p.saved);
</script>

<TopBar title="Guardados" showNotif={false} />

{#if saved.length === 0}
  <div class="empty">
    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
      <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
    </svg>
    <p>Nenhum post guardado ainda</p>
    <span>Guarda posts que queiras ver mais tarde</span>
  </div>
{:else}
  {#each saved as post (post.id)}
    <PostCard {post} />
  {/each}
{/if}

<style>
  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 32px;
    color: var(--fg-3);
    text-align: center;
    gap: 10px;
  }

  .empty p {
    font-size: 1rem;
    font-weight: 600;
    color: var(--fg-2);
  }

  .empty span { font-size: 0.875rem; }
</style>
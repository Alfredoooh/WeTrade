<script>
  import TopBar from '$lib/components/TopBar.svelte';
  import PostCard from '$lib/components/PostCard.svelte';
  import { posts } from '$lib/stores/posts.js';

  const stories = [
    { name: 'Ana', seed: 'ana', color: '7c3aed' },
    { name: 'Bruno', seed: 'bruno', color: '2563eb' },
    { name: 'Carla', seed: 'carla', color: '059669' },
    { name: 'Diogo', seed: 'diogo', color: 'dc2626' },
    { name: 'Elisa', seed: 'elisa', color: 'd97706' },
    { name: 'Fábio', seed: 'fabio', color: '0891b2' },
  ];
</script>

<TopBar title="Comunidade" />

<section class="stories">
  <div class="stories-scroll">
    <div class="story-item">
      <div class="story-ring add-ring">
        <img src="https://api.dicebear.com/8.x/notionists/svg?seed=alfred&backgroundColor=6d28d9" alt="eu" />
        <span class="add-dot">+</span>
      </div>
      <span>Tu</span>
    </div>
    {#each stories as s}
      <div class="story-item">
        <div class="story-ring">
          <img src="https://api.dicebear.com/8.x/notionists/svg?seed={s.seed}&backgroundColor={s.color}" alt={s.name} />
        </div>
        <span>{s.name}</span>
      </div>
    {/each}
  </div>
</section>

<div class="divider"></div>

{#each $posts as post (post.id)}
  <PostCard {post} />
{/each}

<style>
  .stories {
    padding: 12px 0;
    background: var(--card);
    border-bottom: 1px solid var(--border);
  }
  .stories-scroll {
    display: flex;
    gap: 16px;
    padding: 0 16px;
    overflow-x: auto;
  }
  .story-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 5px;
    flex-shrink: 0;
    cursor: pointer;
  }
  .story-item span {
    font-size: 0.7rem;
    color: var(--fg-2);
    font-weight: 500;
    max-width: 56px;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .story-ring {
    width: 58px;
    height: 58px;
    border-radius: 50%;
    padding: 2px;
    background: linear-gradient(135deg, var(--accent), #ec4899);
    position: relative;
  }
  .story-ring img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    border: 2.5px solid var(--bg);
    object-fit: cover;
    background: var(--bg-3);
  }
  .add-ring { background: var(--bg-2); }
  .add-dot {
    position: absolute;
    bottom: 0;
    right: 0;
    width: 18px;
    height: 18px;
    background: var(--accent);
    border-radius: 50%;
    border: 2px solid var(--bg);
    color: white;
    font-size: 0.75rem;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
  }
  .divider { height: 8px; background: var(--bg-2); }
</style>

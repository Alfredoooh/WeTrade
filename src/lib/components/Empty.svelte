<script>
  export let emoji = '⚽';
  export let title = 'Sem resultados';
  export let sub = '';
</script>

<div class="empty">
  <span class="emoji">{emoji}</span>
  <p class="title">{title}</p>
  {#if sub}<p class="sub">{sub}</p>{/if}
</div>

<style>
  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 24px;
    gap: 10px;
    text-align: center;
  }

  .emoji { font-size: 2.5rem; }

  .title {
    font-size: 1rem;
    font-weight: 700;
    color: var(--fg);
  }

  .sub {
    font-size: 0.875rem;
    color: var(--fg-3);
    line-height: 1.45;
  }
</style>
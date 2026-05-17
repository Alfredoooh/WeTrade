<script>
  export let size = 28;
</script>

<div class="loader-wrap">
  <div class="loader" style="width:{size}px;height:{size}px"></div>
</div>

<style>
  .loader-wrap {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 0;
  }

  .loader {
    border-radius: 50%;
    border: 3px solid var(--border);
    border-top-color: var(--primary);
    animation: spin 0.7s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }
</style>
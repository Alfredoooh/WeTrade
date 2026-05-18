<script>
  import AppBar from '$lib/components/AppBar.svelte';
  
  let theme = 'system';
  
  function setTheme(val) {
    theme = val;
    if (val === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
      localStorage.setItem('theme', 'dark');
    } else if (val === 'light') {
      document.documentElement.setAttribute('data-theme', 'light');
      localStorage.setItem('theme', 'light');
    } else {
      document.documentElement.removeAttribute('data-theme');
      localStorage.removeItem('theme');
    }
  }
  
  import { onMount } from 'svelte';
  onMount(() => {
    const saved = localStorage.getItem('theme');
    if (saved) theme = saved;
    else theme = 'system';
  });
</script>

<AppBar title="Configuracoes" showBack={true} showSearch={false} showNotif={false} />

<div class="page">
  
  <div class="group-label">Aparencia</div>
  
  <div class="group">
    <div class="row">
      <span class="row-label">Tema</span>
    </div>
    <div class="theme-options">
      {#each [['system','Sistema'],['light','Claro'],['dark','Escuro']] as [val, label]}
        <button class="theme-btn pressable" class:active={theme === val} on:click={() => setTheme(val)}>
          {label}
        </button>
      {/each}
    </div>
  </div>

  <div class="group-label">Notificacoes</div>

  <div class="group">
    <div class="row">
      <span class="row-label">Jogos ao vivo</span>
      <div class="toggle active"></div>
    </div>
    <div class="row">
      <span class="row-label">Resultados</span>
      <div class="toggle active"></div>
    </div>
    <div class="row">
      <span class="row-label">Transferencias</span>
      <div class="toggle"></div>
    </div>
  </div>

  <div class="group-label">Sobre</div>

  <div class="group">
    <div class="row">
      <span class="row-label">Versao</span>
      <span class="row-val">1.0.0</span>
    </div>
    <div class="row">
      <span class="row-label">App</span>
      <span class="row-val">WeSports</span>
    </div>
  </div>

</div>

<style>
  .page { padding: 16px 0; }

  .group-label {
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--fg-3);
    padding: 0 16px 6px;
    margin-top: 16px;
  }

  .group {
    background: var(--card);
    border-top: 1px solid var(--border);
    border-bottom: 1px solid var(--border);
    margin-bottom: 8px;
  }

  .row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 15px 16px;
    border-bottom: 1px solid var(--border);
  }

  .row:last-child { border-bottom: none; }

  .row-label { font-size: 0.9375rem; color: var(--fg); }

  .row-val { font-size: 0.875rem; color: var(--fg-3); }

  .theme-options {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
  }

  .theme-btn {
    flex: 1;
    padding: 10px;
    border-radius: 10px;
    background: var(--bg-2);
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--fg-2);
    border: 1.5px solid transparent;
    transition: all 0.15s;
  }

  .theme-btn.active {
    background: var(--primary-soft);
    color: var(--primary);
    border-color: var(--primary);
  }

  .toggle {
    width: 44px;
    height: 26px;
    border-radius: 999px;
    background: var(--bg-3);
    position: relative;
    transition: background 0.2s;
  }

  .toggle::after {
    content: '';
    position: absolute;
    top: 3px;
    left: 3px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: white;
    transition: transform 0.2s;
    box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  }

  .toggle.active {
    background: var(--primary);
  }

  .toggle.active::after {
    transform: translateX(18px);
  }
</style>
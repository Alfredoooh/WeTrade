<script>
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import AppBar from '$lib/components/AppBar.svelte';
  import Loader from '$lib/components/Loader.svelte';
  import { getPerson } from '$lib/services/api.js';

  $: id = $page.params.id;
  let player = null, loading = true;

  onMount(async () => {
    player = await getPerson(id);
    loading = false;
  });
</script>

<AppBar title="Jogador" showBack={true} showSearch={false} showNotif={false} />

{#if loading}
  <Loader />
{:else if !player || player.message}
  <div class="error"><p>⚽</p><p>Jogador não disponível</p></div>
{:else}
  <div class="player-hero">
    <div class="player-avatar">{player.name?.[0] || '?'}</div>
    <div>
      <h1>{player.name}</h1>
      <p class="hero-sub">{player.nationality || ''} · {player.position || ''}</p>
    </div>
  </div>

  <div class="info-section">
    {#each [
      ['Posição', player.position],
      ['Nacionalidade', player.nationality],
      ['Data de nascimento', player.dateOfBirth],
      ['Secção', player.section],
      ['Contrato até', player.contract?.until],
    ] as [label, val]}
      {#if val}
        <div class="info-row">
          <span class="info-label">{label}</span>
          <span class="info-val">{val}</span>
        </div>
      {/if}
    {/each}
  </div>

  {#if player.currentTeam}
    <div class="team-section">
      <h3>Equipa atual</h3>
      <div class="team-row">
        {#if player.currentTeam.crest}
          <img src={player.currentTeam.crest} alt="" class="tc" on:error={e => e.target.style.display='none'} />
        {/if}
        <span>{player.currentTeam.name}</span>
      </div>
    </div>
  {/if}
{/if}

<style>
  .player-hero {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 24px 16px;
    border-bottom: 1px solid var(--border);
  }

  .player-avatar {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: var(--primary);
    color: white;
    font-size: 1.6rem;
    font-weight: 800;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  h1 { font-size: 1.1rem; font-weight: 800; color: var(--fg); }
  .hero-sub { font-size: 0.78rem; color: var(--fg-3); margin-top: 3px; }

  .info-section {
    margin: 16px;
    background: var(--card);
    border-radius: var(--radius);
    border: 1px solid var(--border);
    overflow: hidden;
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 13px 16px;
    border-bottom: 1px solid var(--border);
  }

  .info-row:last-child { border-bottom: none; }

  .info-label { font-size: 0.875rem; color: var(--fg-2); }
  .info-val { font-size: 0.875rem; font-weight: 600; color: var(--fg); }

  .team-section { padding: 0 16px 16px; }

  h3 { font-size: 0.875rem; font-weight: 700; color: var(--fg); margin-bottom: 10px; }

  .team-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px;
    background: var(--card);
    border-radius: var(--radius);
    border: 1px solid var(--border);
    font-weight: 600;
    font-size: 0.9rem;
    color: var(--fg);
  }

  .tc { width: 28px; height: 28px; object-fit: contain; }

  .error {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 60px 24px;
    gap: 12px;
    text-align: center;
    color: var(--fg);
  }
</style>
<script>
  import AppBar from '$lib/components/AppBar.svelte';
  
  const channels = [
    { name: 'Sport TV 1', live: true, match: 'Benfica vs Porto', time: '21:00' },
    { name: 'Sport TV 2', live: true, match: 'Man City vs Chelsea', time: '20:00' },
    { name: 'Sport TV 3', live: false, match: 'Real Madrid vs Barcelona', time: '21:00' },
    { name: 'Sport TV 4', live: false, match: 'Bayern vs Dortmund', time: '18:30' },
    { name: 'Sport TV+', live: false, match: 'Juventus vs Milan', time: '20:45' },
    { name: 'RTP 1', live: false, match: 'Portugal vs Espanha', time: '20:45' },
    { name: 'SIC', live: false, match: 'Europa League highlights', time: '22:00' },
    { name: 'TVI', live: false, match: 'Liga Portugal resumo', time: '23:00' },
    { name: 'Eleven 1', live: true, match: 'Ajax vs PSV', time: '20:00' },
    { name: 'Eleven 2', live: false, match: 'Premier League top 10', time: '21:30' },
    { name: 'DAZN', live: false, match: 'Napoli vs Inter', time: '20:45' },
    { name: 'Eurosport', live: false, match: 'La Liga magazine', time: '22:00' },
  ];
</script>

<AppBar title="TV" showSearch={false} />

<div class="page">
  
  <section class="section">
    <div class="section-header">
      <div class="live-row">
        <span class="dot"></span>
        <span class="live-label">A transmitir agora</span>
      </div>
    </div>
    {#each channels.filter(c => c.live) as ch}
      <div class="channel-card live-card">
        <div class="ch-icon-box live-box">
          <img src="/icons/tv.svg" alt="" class="ch-icon-img" />
        </div>
        <div class="ch-info">
          <p class="ch-name">{ch.name}</p>
          <p class="ch-match">{ch.match}</p>
        </div>
        <span class="live-tag">AO VIVO</span>
      </div>
    {/each}
  </section>

  <section class="section">
    <div class="section-header">
      <h2>Programacao de hoje</h2>
    </div>
    {#each channels as ch}
      <div class="channel-card pressable">
        <div class="ch-icon-box">
          <img src="/icons/tv.svg" alt="" class="ch-icon-img" />
        </div>
        <div class="ch-info">
          <p class="ch-name">{ch.name}</p>
          <p class="ch-match">{ch.match}</p>
        </div>
        <span class="ch-time">{ch.time}</span>
      </div>
    {/each}
  </section>

</div>

<style>
  .page { padding-bottom: 8px; }

  .section { margin-bottom: 4px; }

  .section-header {
    display: flex;
    align-items: center;
    padding: 16px 16px 10px;
  }

  h2 { font-size: 1rem; font-weight: 700; color: var(--fg); }

  .live-row { display: flex; align-items: center; gap: 8px; }

  .live-label { font-size: 0.875rem; font-weight: 700; color: #ff3b30; }

  .dot {
    width: 7px;
    height: 7px;
    background: #ff3b30;
    border-radius: 50%;
    animation: pulse 1.2s infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.3; }
  }

  .channel-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
  }

  .live-card { background: rgba(255,59,48,0.04); }

  .ch-icon-box {
    width: 38px;
    height: 38px;
    border-radius: 10px;
    background: var(--bg-2);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .live-box { background: rgba(255,59,48,0.12); }

  .ch-icon-img { width: 18px; height: 18px; }

  @media (prefers-color-scheme: dark) {
    .ch-icon-img { filter: invert(1); }
  }

  .ch-info { flex: 1; }

  .ch-name { font-weight: 700; font-size: 0.875rem; color: var(--fg); }

  .ch-match { font-size: 0.8rem; color: var(--fg-2); margin-top: 2px; }

  .live-tag {
    font-size: 0.6rem;
    font-weight: 800;
    background: #ff3b30;
    color: white;
    padding: 3px 7px;
    border-radius: 4px;
    letter-spacing: 0.06em;
    flex-shrink: 0;
  }

  .ch-time {
    font-size: 0.875rem;
    font-weight: 700;
    color: var(--primary);
    flex-shrink: 0;
  }
</style>
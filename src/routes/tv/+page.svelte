<script>
  import AppBar from '$lib/components/AppBar.svelte';
  
  const channels = [
    { name: 'Sport TV 1', icon: '📺', live: true, match: 'Benfica vs Porto', time: '21:00' },
    { name: 'Sport TV 2', icon: '📺', live: true, match: 'Man City vs Chelsea', time: '20:00' },
    { name: 'Sport TV 3', icon: '📺', live: false, match: 'Real Madrid vs Barça', time: '21:00' },
    { name: 'Sport TV 4', icon: '📺', live: false, match: 'Bayern vs Dortmund', time: '18:30' },
    { name: 'Sport TV+', icon: '📺', live: false, match: 'Juventus vs Milan', time: '20:45' },
    { name: 'RTP 1', icon: '🇵🇹', live: false, match: 'Portugal vs Espanha', time: '20:45' },
    { name: 'SIC', icon: '🔵', live: false, match: 'Europa League: highlights', time: '22:00' },
    { name: 'TVI', icon: '🔴', live: false, match: 'Liga Portugal: resumo', time: '23:00' },
    { name: 'Eleven 1', icon: '🎯', live: true, match: 'Ajax vs PSV', time: '20:00' },
    { name: 'Eleven 2', icon: '🎯', live: false, match: 'Premier League: top 10', time: '21:30' },
    { name: 'DAZN', icon: '⚡', live: false, match: 'Serie A: Napoli vs Inter', time: '20:45' },
    { name: 'Eurosport', icon: '🌍', live: false, match: 'La Liga: magazine', time: '22:00' },
  ];
  
  const streams = [
    { name: 'GoalZone Live 1', match: 'Champions League', viewers: '12.4k', live: true },
    { name: 'GoalZone Live 2', match: 'Premier League', viewers: '8.1k', live: true },
    { name: 'GoalZone Live 3', match: 'La Liga', viewers: '5.7k', live: false },
  ];
</script>

<AppBar title="TV" showSearch={false} />

<div class="page">
  
  <section class="section">
    <div class="section-header">
      <div class="live-badge-row">
        <span class="dot"></span>
        <span>A transmitir agora</span>
      </div>
    </div>
    {#each channels.filter(c => c.live) as ch}
      <div class="channel-card live-card">
        <span class="ch-icon">{ch.icon}</span>
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
      <h2>Programação de hoje</h2>
    </div>
    {#each channels as ch}
      <div class="channel-card pressable">
        <span class="ch-icon">{ch.icon}</span>
        <div class="ch-info">
          <p class="ch-name">{ch.name}</p>
          <p class="ch-match">{ch.match}</p>
        </div>
        <span class="ch-time">{ch.time}</span>
      </div>
    {/each}
  </section>

  <section class="section">
    <div class="section-header">
      <h2>GoalZone Streams</h2>
    </div>
    <div class="streams-grid">
      {#each streams as s}
        <div class="stream-card pressable">
          <div class="stream-thumb">
            <span>⚽</span>
            {#if s.live}<div class="stream-live">AO VIVO</div>{/if}
          </div>
          <p class="stream-name">{s.name}</p>
          <p class="stream-sub">{s.match} · 👁 {s.viewers}</p>
        </div>
      {/each}
    </div>
  </section>

</div>

<style>
  .page { padding-bottom: 8px; }

  .section { margin-bottom: 4px; }

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 16px 10px;
  }

  h2 { font-size: 1rem; font-weight: 700; color: var(--fg); }

  .live-badge-row {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 0.875rem;
    font-weight: 700;
    color: #ff3b30;
  }

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

  .ch-icon { font-size: 1.4rem; flex-shrink: 0; }

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

  .streams-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 0 16px 16px;
  }

  .stream-card {
    background: var(--bg-2);
    border-radius: var(--radius);
    overflow: hidden;
  }

  .stream-thumb {
    height: 90px;
    background: var(--bg-3);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 2rem;
    position: relative;
  }

  .stream-live {
    position: absolute;
    top: 6px;
    left: 6px;
    background: #ff3b30;
    color: white;
    font-size: 0.55rem;
    font-weight: 800;
    padding: 2px 6px;
    border-radius: 4px;
    letter-spacing: 0.08em;
  }

  .stream-name { font-size: 0.8rem; font-weight: 700; padding: 8px 10px 2px; color: var(--fg); }

  .stream-sub { font-size: 0.7rem; color: var(--fg-3); padding: 0 10px 10px; }
</style>
import { useState, useEffect } from 'react';
import { Shield, Target, ShoppingCart, Map, BarChart2, Gamepad2, LogOut, Menu, X, Clock, Swords, Skull, TrendingUp, History, Pickaxe, Package, Gem, Crown, Coins, XCircle, Gift } from 'lucide-react';
import { Route, Routes, Link, useLocation, Navigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ClientShop, ClientAh, ClientQuests, ClientMap } from './App';
import { ClientJobs } from './ClientJobs';
import { PlayerBalanceWidget } from './PlayerBalanceWidget';

const API_URL = '/api';

// Retourne l'URL de l'avatar du joueur.
// On passe par l'API du serveur qui gère les skins Bedrock via la BDD
function getPlayerAvatarUrl(name: string, size: number = 64): string {
  if (!name) return `${API_URL}/head/Steve/${size}`;
  return `${API_URL}/head/${encodeURIComponent(name)}/${size}`;
}

export function PlayerLogin({ onLogin }: { onLogin: (data: any) => void }) {
  const { t } = useTranslation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_URL}/player/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8' },
        body: JSON.stringify({ username, password })
      });
      const data = await res.json();
      if (res.ok) {
        onLogin(data);
      } else {
        setError(data.error || t('web.auth.invalid_credentials'));
      }
    } catch (err) {
      setError(t('web.auth.network_error'));
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-icon"><Gamepad2 size={48} /></div>
        <h2>{t('web.auth.player_login_title')}</h2>
        <p>{t('web.auth.player_login_desc')}</p>
        
        <form onSubmit={handleSubmit}>
          <input type="text" placeholder={t('web.auth.username_placeholder')} value={username} onChange={e=>setUsername(e.target.value)} className="login-input" />
          <input type="password" placeholder={t('web.auth.password_placeholder')} value={password} onChange={e=>setPassword(e.target.value)} className="login-input" />
          {error && <div className="login-error">{error}</div>}
          <button type="submit" className="login-button">{t('web.auth.login_btn')}</button>
        </form>
      </div>
    </div>
  );
}

function PlayerStats({ uuid, isEcoEnabled }: { uuid: string, isEcoEnabled: boolean }) {
  const { t } = useTranslation();
  const [stats, setStats] = useState<any>(null);
  const [bestTeam, setBestTeam] = useState<any>(null);
  useEffect(() => {
    fetch(`${API_URL}/player/stats?uuid=${uuid}`)
      .then(res => res.json())
      .then(data => setStats(data));

    fetch(`${API_URL}/stats/teams/best`)
      .then(res => {
        if (res.ok) return res.json();
        return null;
      })
      .then(data => setBestTeam(data))
      .catch(() => setBestTeam(null));
  }, [uuid]);

  if (!stats) return <div className="loading">{t('web.public.loading')}</div>;

  // Helper formatting
  const formatPlaytime = (minutes: number) => {
      if (!minutes) return "0h 0m";
      const h = Math.floor(minutes / 60);
      const m = minutes % 60;
      return `${h}h ${m}m`;
  };

  const kdRatio = stats.deaths > 0 ? (stats.playerKills / stats.deaths).toFixed(2) : stats.playerKills;

  return (
    <div className="dashboard-content" style={{padding: '2rem'}}>
      <h2 style={{marginBottom: '2rem', display: 'flex', alignItems: 'center', gap: '10px'}}><BarChart2/> {t('web.public.stats.global_title')}</h2>
      
      <div className="stats-grid" style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: '20px', marginBottom: '2rem'}}>
        
        <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
          <Clock size={40} style={{color: 'var(--accent)', marginBottom: '1rem', margin: '0 auto'}}/>
          <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{formatPlaytime(stats.playtimeMinutes)}</h3>
          <p style={{color: 'var(--text-muted)'}}>{t('web.public.stats.playtime')}</p>
        </div>

        <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
          <Pickaxe size={40} style={{color: '#a855f7', marginBottom: '1rem', margin: '0 auto'}}/>
          <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{stats.globalJobLevel || 0}</h3>
          <p style={{color: 'var(--text-muted)'}}>{t('web.public.stats.jobs_level')}</p>
        </div>

        <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
          <Swords size={40} style={{color: '#f97316', marginBottom: '1rem', margin: '0 auto'}}/>
          <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{kdRatio}</h3>
          <p style={{color: 'var(--text-muted)'}}>{t('web.public.stats.kd_ratio')} ({stats.playerKills} Kills)</p>
        </div>

        <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
          <Skull size={40} style={{color: '#ef4444', marginBottom: '1rem', margin: '0 auto'}}/>
          <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{stats.deaths}</h3>
          <p style={{color: 'var(--text-muted)'}}>{t('web.public.stats.total_deaths')}</p>
        </div>

        <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
          <Target size={40} style={{color: 'var(--accent)', marginBottom: '1rem', margin: '0 auto'}}/>
          <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{stats.questsCompleted}</h3>
          <p style={{color: 'var(--text-muted)'}}>{t('web.public.stats.quests_completed')}</p>
        </div>

        {isEcoEnabled && (
          <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
            <ShoppingCart size={40} style={{color: '#10b981', marginBottom: '1rem', margin: '0 auto'}}/>
            <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{stats.balance ? stats.balance.toFixed(2) : 0} $</h3>
            <p style={{color: 'var(--text-muted)'}}>{t('web.public.stats.balance')}</p>
          </div>
        )}
        
      </div>

      <div className="dashboard-split" style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px'}}>
        {/* GRAPHIQUE QUETES */}
        <div className="admin-card" style={{padding: '1.5rem'}}>
          <h3 style={{marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '10px'}}><TrendingUp size={20} color="var(--accent)"/> {t('web.public.stats.quests_7_days')}</h3>
          <div style={{display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', height: '150px', paddingTop: '20px', borderBottom: '1px solid var(--card-border)'}}>
            {(stats.questsActivity || [0,0,0,0,0,0,0]).map((count: number, index: number) => {
              const max = Math.max(...(stats.questsActivity || []), 5);
              const height = Math.max((count / max) * 100, 5);
              return (
                <div key={index} style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '5px', width: '10%'}}>
                  <div style={{
                    width: '100%', 
                    height: `${height}%`, 
                    background: 'var(--accent)', 
                    borderRadius: '4px 4px 0 0',
                    opacity: count > 0 ? 1 : 0.3
                  }}></div>
                  <span style={{fontSize: '0.8rem', color: 'var(--text-muted)'}}>{t('web.public.stats.day_prefix')}-{6-index}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* DERNIERES TRANSACTIONS */}
        {isEcoEnabled && (
          <div className="admin-card" style={{padding: '1.5rem'}}>
            <h3 style={{marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '10px'}}><History size={20} color="var(--accent)"/> {t('web.public.stats.recent_transactions')}</h3>
            {(!stats.recentTransactions || stats.recentTransactions.length === 0) ? (
              <p style={{color: 'var(--text-muted)'}}>{t('web.public.stats.no_transactions')}</p>
            ) : (
              <div style={{display: 'flex', flexDirection: 'column', gap: '10px'}}>
                {stats.recentTransactions.map((tr: any, idx: number) => (
                  <div key={idx} style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px', background: 'var(--bg-color)', borderRadius: '8px', border: '1px solid var(--card-border)'}}>
                    <div>
                      <strong style={{color: tr.type === 'BUY' || tr.type === 'ACHAT' ? '#ef4444' : '#10b981'}}>{tr.type === 'BUY' || tr.type === 'ACHAT' ? t('web.public.stats.buy') : t('web.public.stats.sell')}</strong> x{tr.amount} {tr.material}
                    </div>
                    <div style={{fontWeight: 'bold', color: tr.type === 'BUY' || tr.type === 'ACHAT' ? '#ef4444' : '#10b981'}}>
                      {tr.type === 'BUY' || tr.type === 'ACHAT' ? '-' : '+'}{tr.price.toFixed(2)} $
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* MEILLEURE GUILDE */}
      {bestTeam && (
        <div className="admin-card" style={{marginTop: '2rem', padding: '1.5rem', background: 'linear-gradient(135deg, rgba(168,85,247,0.1), rgba(249,115,22,0.1))', border: '1px solid rgba(168,85,247,0.3)'}}>
          <h3 style={{marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '10px', color: '#a855f7'}}>
            <Shield size={24}/> {t('web.public.stats.best_team')} : {bestTeam.name}
          </h3>
          <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
            <div style={{fontSize: '1.2rem'}}>
              <strong>{bestTeam.total_points}</strong> {t('web.public.stats.total_points')}
            </div>
            <div style={{display: 'flex', gap: '10px', flexWrap: 'wrap'}}>
              {bestTeam.members && bestTeam.members.map((member: any, idx: number) => (
                <div key={idx} style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '5px'}}>
                  <img src={getPlayerAvatarUrl(member.name, 32)} alt="Head" style={{borderRadius: '4px'}} />
                  <span style={{fontSize: '0.8rem', color: 'var(--text-muted)'}}>{member.name}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}


    </div>
  );
}

function PlayerGames({ uuid, token, isEnabled }: { uuid: string, token: string, isEnabled?: boolean }) {
  const { t } = useTranslation();
  const [config, setConfig] = useState<{ wheelEnabled?: boolean, casinoEnabled?: boolean, coinflipEnabled?: boolean, enabled?: boolean }>({ wheelEnabled: true, casinoEnabled: true, coinflipEnabled: true, enabled: true });
  
  // Wheel State
  const [spinResult, setSpinResult] = useState<string | null>(null);
  const [isSpinning, setIsSpinning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [rewards, setRewards] = useState<any[]>([]);
  const [rotation, setRotation] = useState(0);

  // Casino State
  const [casinoInventory, setCasinoInventory] = useState<any[]>([]);
  const [selectedBet, setSelectedBet] = useState<number | null>(null);
  const [casinoResult, setCasinoResult] = useState<string | null>(null);
  const [isRolling, setIsRolling] = useState(false);
  const [slotReels, setSlotReels] = useState<React.ReactNode[]>(['?', '?', '?']);

  // CoinFlip State
  const [selectedCoinBet, setSelectedCoinBet] = useState<number | null>(null);
  const [coinChoice, setCoinChoice] = useState<'HEADS' | 'TAILS'>('HEADS');
  const [coinResult, setCoinResult] = useState<string | null>(null);
  const [isFlipping, setIsFlipping] = useState(false);
  const [coinSide, setCoinSide] = useState<'HEADS' | 'TAILS'>('HEADS');

  useEffect(() => {
    fetch(`${API_URL}/games/config`)
      .then(res => res.json())
      .then(data => setConfig(data))
      .catch(console.error);

    fetch(`${API_URL}/games/wheel`)
      .then(res => res.json())
      .then(data => setRewards(data))
      .catch(err => console.error("Error fetching wheel", err));

    fetchCasinoInventory();
  }, [uuid]);

  if (isEnabled === false || config.enabled === false || (config.wheelEnabled === false && config.casinoEnabled === false && config.coinflipEnabled === false)) {
    return (
      <div className="dashboard-content" style={{textAlign: 'center', padding: '4rem 2rem', color: 'var(--text-muted)'}}>
        <Gamepad2 size={48} style={{opacity: 0.5, marginBottom: '1rem'}}/>
        <h2>{t('web.public.games.disabled_title') || 'Mini-Jeux Désactivés'}</h2>
        <p>{t('web.public.games.disabled_desc') || 'Ce module est actuellement désactivé par les administrateurs.'}</p>
      </div>
    );
  }

  const fetchCasinoInventory = () => {
    fetch(`${API_URL}/games/casino/inventory?uuid=${uuid}`)
      .then(res => res.json())
      .then(data => setCasinoInventory(data))
      .catch(console.error);
  };

  const playWheel = async () => {
    if (isSpinning || rewards.length === 0) return;
    setIsSpinning(true);
    setError(null);
    setSpinResult(null);

    try {
      const res = await fetch(`${API_URL}/games/play`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ gameId: 'wheel' })
      });
      const data = await res.json();
      
      if (res.ok && !data.error) {
        const sliceDeg = 360 / rewards.length;
        const prizeIndex = data.prizeIndex || 0;
        const targetDeg = -(prizeIndex * sliceDeg + sliceDeg / 2);
        const finalRotation = rotation - (rotation % 360) - (360 * 5) + targetDeg;
        
        setRotation(finalRotation);

        setTimeout(() => {
          setIsSpinning(false);
          setSpinResult(data.message);
        }, 5000);
      } else {
        setIsSpinning(false);
        if (res.status === 401 || data.error === 'Session expired' || data.error === 'Unauthorized') {
            setError("Session expirée ! Veuillez vous déconnecter en bas à gauche et vous reconnecter en jeu.");
        } else {
            setError(data.error || "Erreur inconnue");
        }
      }
    } catch (err) {
      setIsSpinning(false);
      setError("Erreur réseau ou session expirée. Veuillez vous reconnecter.");
    }
  };

  const playCasino = async () => {
    if (!selectedBet || isRolling) return;
    setIsRolling(true);
    setCasinoResult(null);

    const interval = setInterval(() => {
        const symbolIcons = [
          <Gem color="#8b5cf6" size={48} key="gem"/>,
          <Crown color="#f59e0b" size={48} key="crown"/>,
          <Coins color="#10b981" size={48} key="coins"/>,
          <XCircle color="#ef4444" size={48} key="xcircle"/>,
          <Gift color="#ec4899" size={48} key="gift"/>
        ];
        setSlotReels([
            symbolIcons[Math.floor(Math.random() * symbolIcons.length)],
            symbolIcons[Math.floor(Math.random() * symbolIcons.length)],
            symbolIcons[Math.floor(Math.random() * symbolIcons.length)]
        ]);
    }, 100);

    try {
      const res = await fetch(`${API_URL}/games/casino/play`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ betId: selectedBet })
      });
      const data = await res.json();
      
      setTimeout(() => {
          clearInterval(interval);
          setIsRolling(false);
          if (res.ok && !data.error) {
              if (data.result === 'LOSS') {
                  setSlotReels([<XCircle color="#ef4444" size={48} key="loss1"/>, <XCircle color="#ef4444" size={48} key="loss2"/>, <XCircle color="#ef4444" size={48} key="loss3"/>]);
                  setCasinoResult(t('web.public.games.casino_loss'));
              } else if (data.result === 'WIN_SMALL') {
                  setSlotReels([<Coins color="#10b981" size={48} key="win_s1"/>, <Coins color="#10b981" size={48} key="win_s2"/>, <Coins color="#10b981" size={48} key="win_s3"/>]);
                  setCasinoResult(t('web.public.games.casino_win_small'));
              } else if (data.result === 'WIN_MEDIUM') {
                  setSlotReels([<Crown color="#f59e0b" size={48} key="win_m1"/>, <Crown color="#f59e0b" size={48} key="win_m2"/>, <Crown color="#f59e0b" size={48} key="win_m3"/>]);
                  setCasinoResult(t('web.public.games.casino_win_medium'));
              } else if (data.result === 'JACKPOT') {
                  setSlotReels([<Gem color="#8b5cf6" size={48} key="jackpot1"/>, <Gem color="#8b5cf6" size={48} key="jackpot2"/>, <Gem color="#8b5cf6" size={48} key="jackpot3"/>]);
                  setCasinoResult(t('web.public.games.casino_jackpot'));
              }
              setSelectedBet(null);
              fetchCasinoInventory();
          } else {
              setSlotReels(['?', '?', '?']);
              if (res.status === 401 || data.error === 'Session expired' || data.error === 'Unauthorized') {
                  setCasinoResult(`[ERROR] Session expirée ! Veuillez vous déconnecter en bas à gauche et vous reconnecter.`);
              } else {
                  setCasinoResult(`[ERROR] ${data.error || t('web.public.games.casino_error')}`);
              }
          }
      }, 2000);

    } catch (err) {
      clearInterval(interval);
      setIsRolling(false);
      setCasinoResult("[ERROR] Erreur réseau ou session expirée. Veuillez vous reconnecter.");
    }
  };

  const playCoinFlip = async () => {
    if (!selectedCoinBet || isFlipping) return;
    setIsFlipping(true);
    setCoinResult(null);

    try {
      const res = await fetch(`${API_URL}/games/coinflip/play`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ betId: selectedCoinBet, choice: coinChoice })
      });
      const data = await res.json();

      setTimeout(() => {
        setIsFlipping(false);
        if (res.ok && !data.error) {
          const outcome = data.outcome === 'HEADS' ? 'HEADS' : 'TAILS';
          setCoinSide(outcome);
          const outcomeName = outcome === 'HEADS' ? (t('web.public.games.coinflip_heads') || 'PILE') : (t('web.public.games.coinflip_tails') || 'FACE');
          if (data.won) {
            setCoinResult(t('web.public.games.coinflip_win', { outcome: outcomeName }) || `Gagné ! La pièce est tombée sur ${outcomeName}. Vos gains sont doublés (x2) !`);
          } else {
            setCoinResult(t('web.public.games.coinflip_loss', { outcome: outcomeName }) || `Perdu ! La pièce est tombée sur ${outcomeName}. Votre mise a été perdue.`);
          }
          setSelectedCoinBet(null);
          fetchCasinoInventory();
        } else {
          if (res.status === 401 || data.error === 'Session expired' || data.error === 'Unauthorized') {
            setCoinResult(`[ERROR] Session expirée ! Veuillez vous déconnecter en bas à gauche et vous reconnecter.`);
          } else {
            setCoinResult(`[ERROR] ${data.error || t('web.public.games.casino_error') || 'Erreur lors du lancer'}`);
          }
        }
      }, 1500);
    } catch (err) {
      setIsFlipping(false);
      setCoinResult("[ERROR] Erreur réseau ou session expirée. Veuillez vous reconnecter.");
    }
  };

  const sliceDeg = rewards.length > 0 ? 360 / rewards.length : 0;
  const gradientStr = rewards.map((r, i) => `${r.color} ${i * sliceDeg}deg ${(i + 1) * sliceDeg}deg`).join(', ');

  return (
    <div className="dashboard-content" style={{padding: '2rem', textAlign: 'center'}}>
      <h2 style={{marginBottom: '2rem'}}><Gamepad2/> {t('web.public.games.title')}</h2>
      
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '2rem', justifyContent: 'center' }}>
        
        {config.wheelEnabled && (
          <div className="admin-card" style={{flex: '1 1 280px', maxWidth: '600px', position: 'relative'}}>
            <h3>{t('web.public.games.wheel_title')}</h3>
            <p style={{color: 'var(--text-muted)', marginBottom: '2rem'}}>{t('web.public.games.wheel_desc')}</p>
            
            {rewards.length > 0 ? (
              <div style={{position: 'relative', width: '300px', height: '300px', margin: '0 auto 3rem auto'}}>
                <div style={{
                  position: 'absolute', top: '-15px', left: '50%', transform: 'translateX(-50%)', zIndex: 10,
                  width: 0, height: 0, borderLeft: '15px solid transparent', borderRight: '15px solid transparent', borderTop: '25px solid white',
                  filter: 'drop-shadow(0 4px 4px rgba(0,0,0,0.5))'
                }}></div>

                <div style={{
                  width: '100%', height: '100%', borderRadius: '50%', background: `conic-gradient(${gradientStr})`, 
                  border: '8px solid var(--card-border)', display: 'flex', alignItems: 'center', justifyContent: 'center',
                  position: 'relative', overflow: 'hidden', boxShadow: '0 0 20px rgba(0,0,0,0.3)',
                  transition: 'transform 5s cubic-bezier(0.2, 0.8, 0.2, 1)',
                  transform: `rotate(${rotation}deg)`
                }}>
                  <div style={{width: '50px', height: '50px', background: 'var(--card-bg)', borderRadius: '50%', zIndex: 5, border: '4px solid var(--card-border)', boxShadow: 'inset 0 0 10px rgba(0,0,0,0.5)'}}></div>

                  {rewards.map((r, i) => {
                    const angle = i * sliceDeg + sliceDeg / 2;
                    return (
                      <div key={i} style={{
                        position: 'absolute', width: '100%', height: '100%',
                        transform: `rotate(${angle}deg)`, display: 'flex', justifyContent: 'center', paddingTop: '20px'
                      }}>
                        <span style={{ 
                          color: 'white', fontWeight: 'bold', fontSize: '0.85rem', textShadow: '1px 1px 3px black, -1px -1px 3px black',
                          maxWidth: '120px', textAlign: 'center', display: 'block', lineHeight: '1.2'
                        }}>
                          {r.name}<br/><span style={{fontSize: '0.75rem', opacity: 0.9}}>{r.chance}%</span>
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            ) : (
              <p>{t('web.public.loading')}</p>
            )}

            <button className="login-button" onClick={playWheel} disabled={isSpinning || rewards.length === 0} style={{padding: '12px 30px', fontSize: '1.1rem', background: 'linear-gradient(to right, #3b82f6, #8b5cf6)', border: 'none'}}>
              {isSpinning ? t('web.public.games.wheel_spinning') : t('web.public.games.wheel_spin_btn')}
            </button>

            {error && <div style={{color: '#ef4444', marginTop: '1.5rem', padding: '1rem', background: 'rgba(239, 68, 68, 0.1)', borderRadius: '8px', border: '1px solid rgba(239, 68, 68, 0.2)'}}>{error}</div>}
            {spinResult && <div style={{color: 'var(--success)', marginTop: '1.5rem', padding: '1rem', background: 'rgba(16, 185, 129, 0.1)', borderRadius: '8px', border: '1px solid rgba(16, 185, 129, 0.2)', fontSize: '1.2rem', fontWeight: 'bold'}}>{spinResult}</div>}
          </div>
        )}

        {config.casinoEnabled && (
          <div className="admin-card" style={{flex: '1 1 280px', maxWidth: '600px'}}>
            <h3>{t('web.public.games.casino_title')}</h3>
            <p style={{color: 'var(--text-muted)', marginBottom: '1rem'}}>
              {t('web.public.games.casino_desc_1')} <code style={{color: 'var(--accent)'}}>/web deposit</code>{t('web.public.games.casino_desc_2')} <code style={{color: 'var(--accent)'}}>/web withdraw</code>{t('web.public.games.casino_desc_3')}
            </p>

            <div style={{
              display: 'flex', justifyContent: 'center', gap: '1rem', margin: '2rem 0',
              fontSize: '3rem', background: 'var(--bg-color)', padding: '1.5rem', borderRadius: '12px',
              border: '2px solid var(--card-border)', boxShadow: 'inset 0 4px 10px rgba(0,0,0,0.5)'
            }}>
              <div style={{background: 'var(--card-bg)', width: '80px', height: '80px', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '8px', border: '1px solid var(--border-color)'}}>{slotReels[0]}</div>
              <div style={{background: 'var(--card-bg)', width: '80px', height: '80px', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '8px', border: '1px solid var(--border-color)'}}>{slotReels[1]}</div>
              <div style={{background: 'var(--card-bg)', width: '80px', height: '80px', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '8px', border: '1px solid var(--border-color)'}}>{slotReels[2]}</div>
            </div>

            {/* Paytable Strip */}
            <div style={{
              display: 'flex', justifyContent: 'space-around', alignItems: 'center',
              background: 'rgba(255,255,255,0.03)', padding: '0.6rem 0.8rem',
              borderRadius: '8px', border: '1px solid var(--card-border)', marginBottom: '1.5rem',
              fontSize: '0.8rem', color: 'var(--text-muted)', flexWrap: 'wrap', gap: '8px'
            }}>
              <span style={{color: '#8b5cf6', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '4px'}}><Gem size={14} /> x3 = x5 (4%)</span>
              <span style={{color: '#f59e0b', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '4px'}}><Crown size={14} /> x3 = x3 (8%)</span>
              <span style={{color: '#10b981', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '4px'}}><Coins size={14} /> x3 = x2 (20%)</span>
              <span style={{color: '#ef4444', display: 'inline-flex', alignItems: 'center', gap: '4px'}}><XCircle size={14} /> = x0 (68%)</span>
            </div>

            <div style={{textAlign: 'left', marginBottom: '1.5rem'}}>
              <h4 style={{marginBottom: '0.5rem'}}>{t('web.public.games.casino_inventory')} :</h4>
              {casinoInventory.length === 0 ? (
                <p style={{color: 'var(--text-muted)', fontStyle: 'italic', background: 'rgba(255,255,255,0.05)', padding: '1rem', borderRadius: '8px'}}>{t('web.public.games.casino_empty')}</p>
              ) : (
                <div style={{display: 'flex', flexWrap: 'wrap', gap: '10px'}}>
                  {casinoInventory.map(item => (
                    <div 
                      key={item.id} 
                      onClick={() => !isRolling && setSelectedBet(item.id)}
                      style={{
                        padding: '10px 15px', background: selectedBet === item.id ? 'rgba(59, 130, 246, 0.2)' : 'var(--bg-color)',
                        border: `2px solid ${selectedBet === item.id ? '#3b82f6' : 'var(--card-border)'}`,
                        borderRadius: '8px', cursor: isRolling ? 'not-allowed' : 'pointer', transition: 'all 0.2s',
                        display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: '80px'
                      }}
                    >
                      <div style={{marginBottom: '5px'}}><Package size={32} color="var(--accent)" /></div>
                      <span style={{fontSize: '0.8rem', color: 'var(--text-muted)'}}>{item.material.replace('_', ' ')} x{item.amount}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <button 
              className="login-button" 
              onClick={playCasino} 
              disabled={isRolling || !selectedBet} 
              style={{
                padding: '12px 30px', fontSize: '1.1rem', 
                background: (!selectedBet || isRolling) ? 'var(--card-bg)' : 'linear-gradient(to right, #f59e0b, #ef4444)', 
                border: 'none', color: (!selectedBet || isRolling) ? 'var(--text-muted)' : 'white', width: '100%'
              }}
            >
              {isRolling ? t('web.public.games.casino_rolling') : selectedBet ? t('web.public.games.casino_bet_btn') : t('web.public.games.casino_select')}
            </button>

            {casinoResult && (
              <div style={{
                marginTop: '1.5rem', padding: '1rem', 
                background: casinoResult.includes('Perdu') || casinoResult.startsWith('[ERROR]') ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)', 
                borderRadius: '8px', 
                border: `1px solid ${casinoResult.includes('Perdu') || casinoResult.startsWith('[ERROR]') ? 'rgba(239, 68, 68, 0.2)' : 'rgba(16, 185, 129, 0.2)'}`, 
                color: casinoResult.includes('Perdu') || casinoResult.startsWith('[ERROR]') ? '#ef4444' : '#10b981',
                fontSize: '1.1rem', fontWeight: 'bold'
              }}>
                {casinoResult.replace('[ERROR] ', '')}
              </div>
            )}
          </div>
        )}

        {config.coinflipEnabled && (
          <div className="admin-card" style={{flex: '1 1 280px', maxWidth: '600px'}}>
            <h3 style={{display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px'}}>
              <Coins size={22} color="#f59e0b"/> {t('web.public.games.coinflip_title') || 'Pile ou Face'}
            </h3>
            <p style={{color: 'var(--text-muted)', marginBottom: '1rem'}}>
              {t('web.public.games.coinflip_desc') || 'Pariez un objet déposé, choisissez votre côté et tentez de doubler (x2) votre mise !'}
            </p>

            {/* Coin 3D Visual */}
            <div style={{
              perspective: '1000px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '1.5rem 0'
            }}>
              <div 
                onClick={() => !isFlipping && setCoinChoice(coinChoice === 'HEADS' ? 'TAILS' : 'HEADS')}
                style={{
                  width: '90px',
                  height: '90px',
                  borderRadius: '50%',
                  background: coinSide === 'HEADS' 
                    ? 'radial-gradient(circle at 30% 30%, #fef08a, #eab308 60%, #ca8a04 100%)' 
                    : 'radial-gradient(circle at 30% 30%, #93c5fd, #3b82f6 60%, #1d4ed8 100%)',
                  border: `4px solid ${coinSide === 'HEADS' ? '#facc15' : '#60a5fa'}`,
                  boxShadow: isFlipping 
                    ? '0 0 30px rgba(234, 179, 8, 0.7)' 
                    : '0 8px 20px rgba(0,0,0,0.4), inset 0 2px 4px rgba(255,255,255,0.6)',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  transformStyle: 'preserve-3d',
                  transition: 'transform 1.4s cubic-bezier(0.4, 0, 0.2, 1)',
                  transform: isFlipping ? 'rotateY(1440deg) scale(1.1)' : 'rotateY(0deg) scale(1)',
                  cursor: isFlipping ? 'not-allowed' : 'pointer'
                }}
              >
                {coinSide === 'HEADS' ? (
                  <>
                    <Crown size={36} color="#713f12" />
                    <span style={{fontSize: '0.7rem', fontWeight: 900, color: '#713f12', letterSpacing: '1px'}}>PILE</span>
                  </>
                ) : (
                  <>
                    <Coins size={36} color="#1e3a8a" />
                    <span style={{fontSize: '0.7rem', fontWeight: 900, color: '#1e3a8a', letterSpacing: '1px'}}>FACE</span>
                  </>
                )}
              </div>
              <span style={{fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.8rem'}}>
                {isFlipping ? (t('web.public.games.coinflip_flipping') || 'La pièce vole dans les airs...') : (t('web.public.games.coinflip_multiplier') || 'Gain : x2 votre objet (50% de chance)')}
              </span>
            </div>

            {/* Side Selection Buttons */}
            <div style={{display: 'flex', gap: '10px', justifyContent: 'center', marginBottom: '1.5rem'}}>
              <button
                type="button"
                disabled={isFlipping}
                onClick={() => setCoinChoice('HEADS')}
                style={{
                  flex: 1,
                  padding: '10px',
                  borderRadius: '8px',
                  border: coinChoice === 'HEADS' ? '2px solid #eab308' : '2px solid var(--card-border)',
                  background: coinChoice === 'HEADS' ? 'rgba(234, 179, 8, 0.2)' : 'var(--bg-color)',
                  color: coinChoice === 'HEADS' ? '#facc15' : 'var(--text-muted)',
                  fontWeight: 'bold',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                  cursor: isFlipping ? 'not-allowed' : 'pointer'
                }}
              >
                <Crown size={18} /> {t('web.public.games.coinflip_heads') || 'PILE'}
              </button>

              <button
                type="button"
                disabled={isFlipping}
                onClick={() => setCoinChoice('TAILS')}
                style={{
                  flex: 1,
                  padding: '10px',
                  borderRadius: '8px',
                  border: coinChoice === 'TAILS' ? '2px solid #3b82f6' : '2px solid var(--card-border)',
                  background: coinChoice === 'TAILS' ? 'rgba(59, 130, 246, 0.2)' : 'var(--bg-color)',
                  color: coinChoice === 'TAILS' ? '#60a5fa' : 'var(--text-muted)',
                  fontWeight: 'bold',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                  cursor: isFlipping ? 'not-allowed' : 'pointer'
                }}
              >
                <Coins size={18} /> {t('web.public.games.coinflip_tails') || 'FACE'}
              </button>
            </div>

            {/* Inventory Selection */}
            <div style={{textAlign: 'left', marginBottom: '1.5rem'}}>
              <h4 style={{marginBottom: '0.5rem'}}>{t('web.public.games.casino_inventory')} :</h4>
              {casinoInventory.length === 0 ? (
                <p style={{color: 'var(--text-muted)', fontStyle: 'italic', background: 'rgba(255,255,255,0.05)', padding: '1rem', borderRadius: '8px'}}>{t('web.public.games.casino_empty')}</p>
              ) : (
                <div style={{display: 'flex', flexWrap: 'wrap', gap: '10px'}}>
                  {casinoInventory.map(item => (
                    <div 
                      key={item.id} 
                      onClick={() => !isFlipping && setSelectedCoinBet(item.id)}
                      style={{
                        padding: '10px 15px', background: selectedCoinBet === item.id ? 'rgba(234, 179, 8, 0.2)' : 'var(--bg-color)',
                        border: `2px solid ${selectedCoinBet === item.id ? '#eab308' : 'var(--card-border)'}`,
                        borderRadius: '8px', cursor: isFlipping ? 'not-allowed' : 'pointer', transition: 'all 0.2s',
                        display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: '80px'
                      }}
                    >
                      <div style={{marginBottom: '5px'}}><Package size={32} color="var(--accent)" /></div>
                      <span style={{fontSize: '0.8rem', color: 'var(--text-muted)'}}>{item.material.replace('_', ' ')} x{item.amount}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <button 
              className="login-button" 
              onClick={playCoinFlip} 
              disabled={isFlipping || !selectedCoinBet} 
              style={{
                padding: '12px 30px', fontSize: '1.1rem', 
                background: (!selectedCoinBet || isFlipping) ? 'var(--card-bg)' : 'linear-gradient(to right, #eab308, #f59e0b)', 
                border: 'none', color: (!selectedCoinBet || isFlipping) ? 'var(--text-muted)' : 'white', width: '100%'
              }}
            >
              {isFlipping ? (t('web.public.games.coinflip_flipping') || 'Lancer en cours...') : selectedCoinBet ? (t('web.public.games.coinflip_flip_btn') || 'Lancer la pièce !') : (t('web.public.games.casino_select'))}
            </button>

            {coinResult && (
              <div style={{
                marginTop: '1.5rem', padding: '1rem', 
                background: coinResult.includes('Perdu') || coinResult.startsWith('[ERROR]') ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)', 
                borderRadius: '8px', 
                border: `1px solid ${coinResult.includes('Perdu') || coinResult.startsWith('[ERROR]') ? 'rgba(239, 68, 68, 0.2)' : 'rgba(16, 185, 129, 0.2)'}`, 
                color: coinResult.includes('Perdu') || coinResult.startsWith('[ERROR]') ? '#ef4444' : '#10b981',
                fontSize: '1.1rem', fontWeight: 'bold'
              }}>
                {coinResult.replace('[ERROR] ', '')}
              </div>
            )}
          </div>
        )}

      </div>
    </div>
  );
}

export function PlayerDashboard({ playerData, onLogout }: { playerData: any, onLogout: () => void }) {
  const { t } = useTranslation();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [modules, setModules] = useState<any[]>([]);
  const [gamesConfig, setGamesConfig] = useState<{ wheelEnabled?: boolean, casinoEnabled?: boolean, coinflipEnabled?: boolean, enabled?: boolean } | null>(null);

  useEffect(() => {
    fetch(`${API_URL}/modules`)
      .then(res => res.json())
      .then(data => setModules(data))
      .catch(console.error);

    fetch(`${API_URL}/games/config`)
      .then(res => res.json())
      .then(data => setGamesConfig(data))
      .catch(console.error);
  }, []);

  const isModuleEnabled = (name: string) => {
    if (modules.length === 0) return true;
    const mod = modules.find(m => m.name.toLowerCase() === name.toLowerCase());
    return mod ? mod.enabled : true;
  };

  const areGamesAvailable = () => {
    if (modules.length > 0) {
      const minigamesMod = modules.find(m => m.name.toLowerCase() === 'minigames' || m.name.toLowerCase() === 'minigame');
      if (minigamesMod && !minigamesMod.enabled) return false;
    }
    if (gamesConfig) {
      if (gamesConfig.enabled === false) return false;
      if (gamesConfig.wheelEnabled === false && gamesConfig.casinoEnabled === false && gamesConfig.coinflipEnabled === false) return false;
    }
    return true;
  };

  return (
    <div className="admin-layout">
      {/* Sidebar Mobile Toggle */}
      <button className="mobile-toggle" onClick={() => setSidebarOpen(!sidebarOpen)} style={{position: 'fixed', top: '15px', left: '15px', zIndex: 101, background: 'var(--card-bg)', border: '1px solid var(--card-border)', color: 'var(--text-main)', padding: '10px', borderRadius: '8px', cursor: 'pointer', display: 'none'}}>
        {sidebarOpen ? <X size={24}/> : <Menu size={24}/>}
      </button>

      {/* Overlay mobile */}
      {sidebarOpen && (
        <div 
          style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', zIndex: 90 }}
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`admin-sidebar ${sidebarOpen ? 'open' : ''}`}>
        <div className="admin-sidebar-header" style={{flexDirection: 'column', gap: '10px'}}>
          <img src={getPlayerAvatarUrl(playerData.username, 100)} alt="Avatar" style={{width: '64px', height: '64px', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.3)'}} />
          <h2 style={{fontSize: '1.2rem', textAlign: 'center'}}>{playerData.username}</h2>
          {playerData.isOp && <span style={{background: '#ef4444', color: 'white', padding: '2px 8px', borderRadius: '12px', fontSize: '0.7rem', fontWeight: 'bold'}}>ADMIN</span>}
          <div style={{ width: '100%' }}>
            <PlayerBalanceWidget uuid={playerData.uuid} token={playerData.token} variant="card" />
          </div>
        </div>
        
        <nav className="admin-nav" style={{marginTop: '1.5rem'}}>
          <Link to="/dashboard" className={location.pathname === '/dashboard' ? 'active' : ''} onClick={() => setSidebarOpen(false)}><ShoppingCart size={18}/> {t('web.nav.shop')}</Link>
          <Link to="/dashboard/ah" className={location.pathname === '/dashboard/ah' ? 'active' : ''} onClick={() => setSidebarOpen(false)}><ShoppingCart size={18}/> {t('web.nav.ah')}</Link>
          {isModuleEnabled('bluemap') && <Link to="/dashboard/map" className={location.pathname === '/dashboard/map' ? 'active' : ''} onClick={() => setSidebarOpen(false)}><Map size={18}/> {t('web.nav.map')}</Link>}
          <Link to="/dashboard/stats" className={location.pathname === '/dashboard/stats' ? 'active' : ''} onClick={() => setSidebarOpen(false)}><BarChart2 size={18}/> {t('web.nav.stats')}</Link>
          {areGamesAvailable() && (
            <Link to="/dashboard/games" className={location.pathname === '/dashboard/games' ? 'active' : ''} onClick={() => setSidebarOpen(false)}>
              <Gamepad2 size={18}/> {t('web.nav.games')} <span style={{marginLeft: 'auto', background: 'var(--accent)', color: 'white', padding: '2px 6px', borderRadius: '4px', fontSize: '0.7rem', fontWeight: 'bold'}}>{t('web.nav.new')}</span>
            </Link>
          )}
          <Link to="/dashboard/jobs" className={location.pathname === '/dashboard/jobs' ? 'active' : ''} onClick={() => setSidebarOpen(false)}><Target size={18}/> {t('web.nav.jobs')}</Link>
          
          {playerData.isOp && (
            <div style={{marginTop: '2rem', borderTop: '1px solid var(--card-border)', paddingTop: '1rem'}}>
              <Link to="/admin" className={location.pathname.startsWith('/admin') ? 'active' : ''} style={{color: '#fbbf24'}}><Shield size={18}/> {t('web.nav.admin')}</Link>
            </div>
          )}
          
          <button onClick={onLogout} style={{marginTop: 'auto', background: 'transparent', border: 'none', color: '#ef4444', display: 'flex', alignItems: 'center', gap: '10px', padding: '15px', cursor: 'pointer', fontSize: '1rem', fontWeight: 'bold', width: '100%', borderRadius: '8px', transition: 'background 0.2s'}} 
            onMouseOver={e=>e.currentTarget.style.background='rgba(239, 68, 68, 0.1)'} 
            onMouseOut={e=>e.currentTarget.style.background='transparent'}>
            <LogOut size={18}/> {t('web.nav.logout')}
          </button>
        </nav>
      </aside>

      {/* Main Content */}
      <main className="admin-main">
        {/* Topbar Persistante avec le Widget de Solde */}
        <header className="player-topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600, color: 'white' }}>
              {location.pathname === '/dashboard' && (t('web.nav.shop') || 'Boutique')}
              {location.pathname === '/dashboard/ah' && (t('web.nav.ah') || 'Hôtel des Ventes')}
              {location.pathname === '/dashboard/games' && (t('web.nav.games') || 'Mini-Jeux')}
              {location.pathname === '/dashboard/jobs' && (t('web.nav.jobs') || 'Métiers')}
              {location.pathname === '/dashboard/map' && (t('web.nav.map') || 'Carte')}
              {location.pathname === '/dashboard/stats' && (t('web.nav.stats') || 'Statistiques')}
              {location.pathname === '/dashboard/quests' && (t('web.nav.quests') || 'Quêtes')}
            </h3>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
            <PlayerBalanceWidget uuid={playerData.uuid} token={playerData.token} variant="pill" />
          </div>
        </header>

        <Routes>
          <Route index element={<ClientShop isEnabled={isModuleEnabled('DynamicShop')} />} />
          <Route path="ah" element={<ClientAh isEnabled={isModuleEnabled('AuctionHouse')} />} />
          <Route path="quests" element={<ClientQuests isEnabled={isModuleEnabled('Quests')} />} />
          <Route path="jobs" element={<ClientJobs />} />
          <Route path="map" element={<ClientMap />} />
          <Route path="stats" element={<PlayerStats uuid={playerData.uuid} isEcoEnabled={isModuleEnabled('Economy')} />} />
          <Route path="games" element={areGamesAvailable() ? <PlayerGames uuid={playerData.uuid} token={playerData.token} isEnabled={true} /> : <Navigate to="/dashboard" replace />} />
        </Routes>
      </main>
    </div>
  );
}

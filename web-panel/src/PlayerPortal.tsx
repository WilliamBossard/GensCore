import { useState, useEffect } from 'react';
import { Shield, Target, ShoppingCart, Map, BarChart2, Gamepad2, LogOut, Menu, X, Clock, Swords, Skull, TrendingUp, History, Pickaxe, Package } from 'lucide-react';
import { Route, Routes, Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ClientShop, ClientAh, ClientQuests, ClientMap } from './App';
import { ClientJobs } from './ClientJobs';

const API_URL = '/api';

export function PlayerLogin({ onLogin }: { onLogin: (data: any) => void }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_URL}/player/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const data = await res.json();
      if (res.ok) {
        onLogin(data);
      } else {
        setError(data.error || 'Identifiants incorrects.');
      }
    } catch (err) {
      setError('Erreur réseau.');
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-icon"><Gamepad2 size={48} /></div>
        <h2>Connexion Joueur</h2>
        <p>Connectez-vous avec vos identifiants Minecraft (/register en jeu)</p>
        
        <form onSubmit={handleSubmit}>
          <input type="text" placeholder="Pseudo Minecraft" value={username} onChange={e=>setUsername(e.target.value)} className="login-input" />
          <input type="password" placeholder="Mot de passe" value={password} onChange={e=>setPassword(e.target.value)} className="login-input" />
          {error && <div className="login-error">{error}</div>}
          <button type="submit" className="login-button">Se Connecter</button>
        </form>
      </div>
    </div>
  );
}

function PlayerStats({ uuid, isEcoEnabled }: { uuid: string, isEcoEnabled: boolean }) {
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

  if (!stats) return <div className="loading">Chargement...</div>;

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
      <h2 style={{marginBottom: '2rem', display: 'flex', alignItems: 'center', gap: '10px'}}><BarChart2/> Vos Statistiques Globales</h2>
      
      <div className="stats-grid" style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px', marginBottom: '2rem'}}>
        
        <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
          <Clock size={40} style={{color: 'var(--accent)', marginBottom: '1rem', margin: '0 auto'}}/>
          <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{formatPlaytime(stats.playtimeMinutes)}</h3>
          <p style={{color: 'var(--text-muted)'}}>Temps de Jeu</p>
        </div>

        <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
          <Pickaxe size={40} style={{color: '#a855f7', marginBottom: '1rem', margin: '0 auto'}}/>
          <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{stats.globalJobLevel || 0}</h3>
          <p style={{color: 'var(--text-muted)'}}>Niveau Global Métiers</p>
        </div>

        <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
          <Swords size={40} style={{color: '#f97316', marginBottom: '1rem', margin: '0 auto'}}/>
          <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{kdRatio}</h3>
          <p style={{color: 'var(--text-muted)'}}>Ratio K/D ({stats.playerKills} Kills)</p>
        </div>

        <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
          <Skull size={40} style={{color: '#ef4444', marginBottom: '1rem', margin: '0 auto'}}/>
          <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{stats.deaths}</h3>
          <p style={{color: 'var(--text-muted)'}}>Morts Totales</p>
        </div>

        <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
          <Target size={40} style={{color: 'var(--accent)', marginBottom: '1rem', margin: '0 auto'}}/>
          <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{stats.questsCompleted}</h3>
          <p style={{color: 'var(--text-muted)'}}>Quêtes Accomplies</p>
        </div>

        {isEcoEnabled && (
          <div className="admin-card" style={{textAlign: 'center', padding: '2rem 1rem'}}>
            <ShoppingCart size={40} style={{color: '#10b981', marginBottom: '1rem', margin: '0 auto'}}/>
            <h3 style={{fontSize: '2rem', margin: '0.5rem 0'}}>{stats.balance ? stats.balance.toFixed(2) : 0} $</h3>
            <p style={{color: 'var(--text-muted)'}}>Solde Bancaire</p>
          </div>
        )}
        
      </div>

      <div className="dashboard-split" style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px'}}>
        {/* GRAPHIQUE QUETES */}
        <div className="admin-card" style={{padding: '1.5rem'}}>
          <h3 style={{marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '10px'}}><TrendingUp size={20} color="var(--accent)"/> Quêtes (7 derniers jours)</h3>
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
                  <span style={{fontSize: '0.8rem', color: 'var(--text-muted)'}}>J-{6-index}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* DERNIERES TRANSACTIONS */}
        {isEcoEnabled && (
          <div className="admin-card" style={{padding: '1.5rem'}}>
            <h3 style={{marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '10px'}}><History size={20} color="var(--accent)"/> Dernières Transactions</h3>
            {(!stats.recentTransactions || stats.recentTransactions.length === 0) ? (
              <p style={{color: 'var(--text-muted)'}}>Aucune transaction récente.</p>
            ) : (
              <div style={{display: 'flex', flexDirection: 'column', gap: '10px'}}>
                {stats.recentTransactions.map((tr: any, idx: number) => (
                  <div key={idx} style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px', background: 'var(--bg-color)', borderRadius: '8px', border: '1px solid var(--card-border)'}}>
                    <div>
                      <strong style={{color: tr.type === 'BUY' || tr.type === 'ACHAT' ? '#ef4444' : '#10b981'}}>{tr.type === 'BUY' || tr.type === 'ACHAT' ? 'Achat' : 'Vente'}</strong> de {tr.amount}x {tr.material}
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
            <Shield size={24}/> Meilleure Guilde : {bestTeam.name}
          </h3>
          <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
            <div style={{fontSize: '1.2rem'}}>
              <strong>{bestTeam.total_points}</strong> Points Totaux
            </div>
            <div style={{display: 'flex', gap: '10px', flexWrap: 'wrap'}}>
              {bestTeam.members && bestTeam.members.map((member: any, idx: number) => (
                <div key={idx} style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '5px'}}>
                  <img src={`https://mc-heads.net/avatar/${member.name}/32`} alt="Head" style={{borderRadius: '4px'}} />
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

function PlayerGames({ uuid }: { uuid: string }) {
  const [config, setConfig] = useState({ wheelEnabled: true, casinoEnabled: true });
  
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
  const [slotReels, setSlotReels] = useState(['?', '?', '?']);

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
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ uuid, gameId: 'wheel' })
      });
      const data = await res.json();
      
      if (res.ok) {
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
        setError(data.error || "Erreur inconnue");
      }
    } catch (err) {
      setIsSpinning(false);
      setError("Erreur réseau.");
    }
  };

  const playCasino = async () => {
    if (!selectedBet || isRolling) return;
    setIsRolling(true);
    setCasinoResult(null);

    const interval = setInterval(() => {
        const symbols = ['DIAMANT', 'POMME', 'PIECES', 'ECHEC', 'CERISE'];
        setSlotReels([
            symbols[Math.floor(Math.random() * symbols.length)],
            symbols[Math.floor(Math.random() * symbols.length)],
            symbols[Math.floor(Math.random() * symbols.length)]
        ]);
    }, 100);

    try {
      const res = await fetch(`${API_URL}/games/casino/play`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ uuid, betId: selectedBet })
      });
      const data = await res.json();
      
      setTimeout(() => {
          clearInterval(interval);
          setIsRolling(false);
          if (res.ok) {
              if (data.result === 'LOSS') {
                  setSlotReels(['ECHEC', 'ECHEC', 'ECHEC']);
                  setCasinoResult("Perdu ! Votre objet a été détruit.");
              } else if (data.result === 'WIN_SMALL') {
                  setSlotReels(['CERISE', 'CERISE', 'CERISE']);
                  setCasinoResult("Gagné ! Vous avez doublé (x2) votre objet !");
              } else if (data.result === 'WIN_MEDIUM') {
                  setSlotReels(['PIECES', 'PIECES', 'PIECES']);
                  setCasinoResult("Super ! Vous avez triplé (x3) votre objet !");
              } else if (data.result === 'JACKPOT') {
                  setSlotReels(['DIAMANT', 'DIAMANT', 'DIAMANT']);
                  setCasinoResult("JACKPOT ! Gains x5 !");
              }
              setSelectedBet(null);
              fetchCasinoInventory();
          } else {
              setSlotReels(['?', '?', '?']);
              setCasinoResult(data.error || "Erreur de mise.");
          }
      }, 2000);

    } catch (err) {
      clearInterval(interval);
      setIsRolling(false);
      setCasinoResult("Erreur réseau.");
    }
  };

  const sliceDeg = rewards.length > 0 ? 360 / rewards.length : 0;
  const gradientStr = rewards.map((r, i) => `${r.color} ${i * sliceDeg}deg ${(i + 1) * sliceDeg}deg`).join(', ');

  return (
    <div className="dashboard-content" style={{padding: '2rem', textAlign: 'center'}}>
      <h2 style={{marginBottom: '2rem'}}><Gamepad2/> Mini-Jeux</h2>
      
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '2rem', justifyContent: 'center' }}>
        
        {config.wheelEnabled && (
          <div className="admin-card" style={{flex: '1 1 280px', maxWidth: '600px', position: 'relative'}}>
            <h3>La Roue de la Fortune</h3>
            <p style={{color: 'var(--text-muted)', marginBottom: '2rem'}}>Tournez la roue une fois par jour pour gagner des récompenses !</p>
            
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
              <p>Chargement de la roue...</p>
            )}

            <button className="login-button" onClick={playWheel} disabled={isSpinning || rewards.length === 0} style={{padding: '12px 30px', fontSize: '1.1rem', background: 'linear-gradient(to right, #3b82f6, #8b5cf6)', border: 'none'}}>
              {isSpinning ? 'La roue tourne...' : 'Tourner la Roue !'}
            </button>

            {error && <div style={{color: '#ef4444', marginTop: '1.5rem', padding: '1rem', background: 'rgba(239, 68, 68, 0.1)', borderRadius: '8px', border: '1px solid rgba(239, 68, 68, 0.2)'}}>{error}</div>}
            {spinResult && <div style={{color: 'var(--success)', marginTop: '1.5rem', padding: '1rem', background: 'rgba(16, 185, 129, 0.1)', borderRadius: '8px', border: '1px solid rgba(16, 185, 129, 0.2)', fontSize: '1.2rem', fontWeight: 'bold'}}>{spinResult}</div>}
          </div>
        )}

        {config.casinoEnabled && (
          <div className="admin-card" style={{flex: '1 1 280px', maxWidth: '600px'}}>
            <h3>Machine à Sous</h3>
            <p style={{color: 'var(--text-muted)', marginBottom: '1rem'}}>
              Déposez un item en jeu avec <code style={{color: 'var(--accent)'}}>/web deposit</code>, pariez-le ici et récupérez vos gains avec <code style={{color: 'var(--accent)'}}>/web withdraw</code>.
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

            <div style={{textAlign: 'left', marginBottom: '1.5rem'}}>
              <h4 style={{marginBottom: '0.5rem'}}>Votre inventaire de Paris :</h4>
              {casinoInventory.length === 0 ? (
                <p style={{color: 'var(--text-muted)', fontStyle: 'italic', background: 'rgba(255,255,255,0.05)', padding: '1rem', borderRadius: '8px'}}>Vide. Connectez-vous et utilisez /web deposit !</p>
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
              {isRolling ? 'Lancement...' : selectedBet ? 'Parier cet objet !' : 'Sélectionnez un objet à parier'}
            </button>

            {casinoResult && (
              <div style={{
                marginTop: '1.5rem', padding: '1rem', 
                background: casinoResult.includes('Perdu') ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)', 
                borderRadius: '8px', 
                border: `1px solid ${casinoResult.includes('Perdu') ? 'rgba(239, 68, 68, 0.2)' : 'rgba(16, 185, 129, 0.2)'}`, 
                color: casinoResult.includes('Perdu') ? '#ef4444' : '#10b981',
                fontSize: '1.1rem', fontWeight: 'bold'
              }}>
                {casinoResult}
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

  useEffect(() => {
    fetch(`${API_URL}/modules`)
      .then(res => res.json())
      .then(data => setModules(data))
      .catch(console.error);
  }, []);

  const isModuleEnabled = (name: string) => {
    if (modules.length === 0) return true;
    const mod = modules.find(m => m.name.toLowerCase() === name.toLowerCase());
    return mod ? mod.enabled : true;
  };

  return (
    <div className="admin-layout">
      {/* Sidebar Mobile Toggle */}
      <button className="mobile-toggle" onClick={() => setSidebarOpen(!sidebarOpen)} style={{position: 'fixed', top: '15px', right: '15px', zIndex: 101, background: 'var(--card-bg)', border: '1px solid var(--card-border)', color: 'var(--text-main)', padding: '10px', borderRadius: '8px', cursor: 'pointer', display: 'none'}}>
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
        <div className="admin-sidebar-header" style={{flexDirection: 'column', gap: '15px'}}>
          <img src={`https://mc-heads.net/avatar/${playerData.username}/100`} alt="Avatar" style={{width: '64px', height: '64px', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.3)'}} />
          <h2 style={{fontSize: '1.2rem', textAlign: 'center'}}>{playerData.username}</h2>
          {playerData.isOp && <span style={{background: '#ef4444', color: 'white', padding: '2px 8px', borderRadius: '12px', fontSize: '0.7rem', fontWeight: 'bold'}}>ADMIN</span>}
        </div>
        
        <nav className="admin-nav" style={{marginTop: '2rem'}}>
          <Link to="/dashboard" className={location.pathname === '/dashboard' ? 'active' : ''} onClick={() => setSidebarOpen(false)}><ShoppingCart size={18}/> {t('web.nav.shop')}</Link>
          <Link to="/dashboard/ah" className={location.pathname === '/dashboard/ah' ? 'active' : ''} onClick={() => setSidebarOpen(false)}><ShoppingCart size={18}/> {t('web.nav.ah')}</Link>
          {isModuleEnabled('bluemap') && <Link to="/dashboard/map" className={location.pathname === '/dashboard/map' ? 'active' : ''} onClick={() => setSidebarOpen(false)}><Map size={18}/> {t('web.nav.map')}</Link>}
          <Link to="/dashboard/stats" className={location.pathname === '/dashboard/stats' ? 'active' : ''} onClick={() => setSidebarOpen(false)}><BarChart2 size={18}/> {t('web.nav.stats')}</Link>
          <Link to="/dashboard/games" className={location.pathname === '/dashboard/games' ? 'active' : ''} onClick={() => setSidebarOpen(false)}><Gamepad2 size={18}/> {t('web.nav.games')} <span style={{marginLeft: 'auto', background: 'var(--accent)', color: 'white', padding: '2px 6px', borderRadius: '4px', fontSize: '0.7rem', fontWeight: 'bold'}}>{t('web.nav.new')}</span></Link>
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
        <Routes>
          <Route path="/" element={<ClientShop isEnabled={isModuleEnabled('DynamicShop')} />} />
          <Route path="/ah" element={<ClientAh isEnabled={isModuleEnabled('AuctionHouse')} />} />
          <Route path="/quests" element={<ClientQuests isEnabled={isModuleEnabled('Quests')} />} />
          <Route path="/jobs" element={<ClientJobs />} />
          <Route path="/map" element={<ClientMap />} />
          <Route path="/stats" element={<PlayerStats uuid={playerData.uuid} isEcoEnabled={isModuleEnabled('Economy')} />} />
          <Route path="/games" element={<PlayerGames uuid={playerData.uuid} />} />
        </Routes>
      </main>
    </div>
  );
}

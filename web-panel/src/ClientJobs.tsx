import { useState, useEffect } from 'react';
import { Target, Pickaxe, Axe, Wheat, Crosshair, Trophy, Gift, Calendar, Clock, BarChart, ListOrdered, Shield } from 'lucide-react';

const jobConfig: any = {
  MINEUR: { icon: Pickaxe, color: '#0ea5e9', bg: 'rgba(14, 165, 233, 0.1)' },
  BUCHERON: { icon: Axe, color: '#f59e0b', bg: 'rgba(245, 158, 11, 0.1)' },
  FERMIER: { icon: Wheat, color: '#22c55e', bg: 'rgba(34, 197, 94, 0.1)' },
  CHASSEUR: { icon: Crosshair, color: '#ef4444', bg: 'rgba(239, 68, 68, 0.1)' },
  DEFAULT: { icon: Target, color: '#8b5cf6', bg: 'rgba(139, 92, 246, 0.1)' }
};

export function ClientJobs() {
  const [activeTab, setActiveTab] = useState<'JOBS' | 'QUESTS' | 'GUILDS'>('GUILDS');
  const [questSubTab, setQuestSubTab] = useState<'daily' | 'weekly' | 'monthly' | 'total'>('weekly');
  
  const [jobsData, setJobsData] = useState<any>({});
  const [questsData, setQuestsData] = useState<any>(null);
  const [teamsData, setTeamsData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = () => {
      Promise.all([
        fetch('/api/stats/jobs').then(res => res.json()),
        fetch('/api/stats/quests/leaderboard').then(res => res.json()),
        fetch('/api/stats/teams').then(res => res.ok ? res.json() : [])
      ]).then(([jobs, quests, teams]) => {
        setJobsData(jobs);
        setQuestsData(quests);
        setTeamsData(teams);
        setLoading(false);
      }).catch(() => setLoading(false));
    };
    
    fetchData(); // Premier chargement
    const interval = setInterval(fetchData, 5000); // Actualisation en temps reel
    
    return () => clearInterval(interval);
  }, []);

  if (loading) return (
    <div style={{display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh'}}>
      <div className="loader" style={{width: '50px', height: '50px', border: '3px solid var(--card-border)', borderTopColor: 'var(--accent)', borderRadius: '50%', animation: 'spin 1s linear infinite'}} />
    </div>
  );

  return (
    <div className="client-shop" style={{ padding: '2rem' }}>
      <div className="shop-header" style={{ textAlign: 'center', marginBottom: '3rem', animation: 'fadeInDown 0.8s ease-out' }}>
        <div style={{display: 'inline-flex', padding: '1rem', borderRadius: '50%', background: 'var(--card-bg)', border: '1px solid var(--card-border)', boxShadow: '0 0 30px rgba(99, 102, 241, 0.2)', marginBottom: '1rem'}}>
           <Trophy size={48} style={{color: 'var(--accent)'}} />
        </div>
        <h1 className="shop-title" style={{ fontSize: '3rem', fontWeight: 800, background: 'linear-gradient(to right, #6366f1, #a855f7)', WebkitBackgroundClip: 'text', color: 'transparent', margin: 0 }}>
          Légendes du Serveur
        </h1>
        
        {/* TAB SWITCHER */}
        <div style={{display: 'flex', justifyContent: 'center', gap: '1rem', marginTop: '2rem'}}>
          <button 
            onClick={() => setActiveTab('GUILDS')}
            style={{
              padding: '0.8rem 2rem', 
              background: activeTab === 'GUILDS' ? 'var(--accent)' : 'var(--card-bg)', 
              color: activeTab === 'GUILDS' ? '#000' : 'var(--text)',
              border: '1px solid var(--card-border)',
              borderRadius: '30px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              transition: 'all 0.3s'
            }}>
            <Shield size={20} /> Guildes
          </button>
          <button 
            onClick={() => setActiveTab('JOBS')}
            style={{
              padding: '0.8rem 2rem', 
              background: activeTab === 'JOBS' ? 'var(--accent)' : 'var(--card-bg)', 
              color: activeTab === 'JOBS' ? '#000' : 'var(--text)',
              border: '1px solid var(--card-border)',
              borderRadius: '30px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              transition: 'all 0.3s'
            }}>
            <Pickaxe size={20} /> Classement Métiers
          </button>
          <button 
            onClick={() => setActiveTab('QUESTS')}
            style={{
              padding: '0.8rem 2rem', 
              background: activeTab === 'QUESTS' ? 'var(--accent)' : 'var(--card-bg)', 
              color: activeTab === 'QUESTS' ? '#000' : 'var(--text)',
              border: '1px solid var(--card-border)',
              borderRadius: '30px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              transition: 'all 0.3s'
            }}>
            <Target size={20} /> Classement Quêtes
          </button>
        </div>
      </div>

      {activeTab === 'JOBS' ? (
        <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '2.5rem', animation: 'fadeIn 0.5s'}}>
          {Object.entries(jobsData).map(([jobName, players]: [string, any], groupIdx) => {
            const config = jobConfig[jobName] || jobConfig.DEFAULT;
            const Icon = config.icon;
            
            return (
              <div key={jobName} 
                   className="admin-card" 
                   style={{
                     padding: 0, overflow: 'hidden', background: 'var(--card-bg)', backdropFilter: 'blur(12px)',
                     border: `1px solid ${config.color}33`, boxShadow: `0 10px 40px ${config.bg}`,
                     animation: `fadeInUp 0.6s ease-out ${(groupIdx + 1) * 0.1}s both`,
                     transition: 'transform 0.3s ease, box-shadow 0.3s ease'
                   }}>
                <div style={{
                  background: `linear-gradient(135deg, ${config.bg}, transparent)`, padding: '2rem',
                  borderBottom: `1px solid ${config.color}22`, display: 'flex', alignItems: 'center', gap: '1rem'
                }}>
                  <div style={{padding: '1rem', background: `${config.color}22`, borderRadius: '16px', color: config.color}}>
                    <Icon size={32} />
                  </div>
                  <div>
                    <h3 style={{margin: 0, fontSize: '1.8rem', fontWeight: 800, textTransform: 'capitalize', color: 'var(--text)'}}>
                      {jobName.toLowerCase()}
                    </h3>
                    <span style={{color: config.color, fontSize: '0.9rem', fontWeight: 600}}>
                      {players.length} participant{players.length !== 1 && 's'}
                    </span>
                  </div>
                </div>
  
                <div style={{padding: '1rem'}}>
                  {players.length > 0 ? (
                    <div style={{display: 'flex', flexDirection: 'column', gap: '0.5rem'}}>
                      {players.map((p: any, idx: number) => {
                        const isTop = idx < 3;
                        const medals = ['#fbbf24', '#94a3b8', '#b45309'];
                        return (
                          <div key={idx} style={{
                            display: 'flex', alignItems: 'center', padding: '1rem',
                            background: isTop ? `linear-gradient(90deg, ${medals[idx]}11, transparent)` : 'transparent',
                            borderRadius: '12px', border: isTop ? `1px solid ${medals[idx]}33` : '1px solid transparent'
                          }}>
                            <div style={{
                              width: '40px', height: '40px', display: 'flex', alignItems: 'center', justifyContent: 'center',
                              borderRadius: '50%', background: isTop ? medals[idx] : 'var(--card-border)',
                              color: isTop ? '#000' : 'var(--text-muted)', fontWeight: 'bold', fontSize: '1.2rem', marginRight: '1rem'
                            }}>
                              {idx + 1}
                            </div>
                            <div style={{flex: 1}}>
                              <div style={{fontWeight: 700, fontSize: '1.1rem', color: isTop ? medals[idx] : 'var(--text)'}}>{p.playerName}</div>
                              <div style={{fontSize: '0.85rem', color: 'var(--text-muted)', display: 'flex', gap: '1rem', marginTop: '4px'}}>
                                <span style={{background: `${config.color}22`, color: config.color, padding: '2px 8px', borderRadius: '4px', fontWeight: 600}}>Lvl {p.level}</span>
                                <span>{p.xp.toFixed(1)} XP</span>
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <div style={{textAlign: 'center', padding: '3rem 1rem', color: 'var(--text-muted)'}}>Aucun joueur n'a rejoint ce métier</div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : activeTab === 'QUESTS' ? (
        <div style={{animation: 'fadeIn 0.5s'}}>
          
          {/* REWARD BANNER */}
          <div style={{
            background: 'linear-gradient(135deg, rgba(234, 179, 8, 0.1), rgba(234, 179, 8, 0.05))',
            border: '1px solid rgba(234, 179, 8, 0.3)',
            borderRadius: '16px', padding: '2rem', marginBottom: '2rem', display: 'flex', alignItems: 'center', gap: '2rem',
            boxShadow: '0 10px 40px rgba(234, 179, 8, 0.1)',
            position: 'relative', overflow: 'hidden'
          }}>
            <div style={{position: 'absolute', right: '-20px', top: '-20px', opacity: 0.1, color: '#eab308'}}>
              <Gift size={150} />
            </div>
            <div style={{padding: '1.5rem', background: 'rgba(234, 179, 8, 0.2)', borderRadius: '50%', color: '#eab308'}}>
              <Gift size={48} />
            </div>
            <div>
              <h2 style={{color: '#eab308', margin: '0 0 0.5rem 0', fontSize: '1.8rem', display: 'flex', alignItems: 'center', gap: '0.5rem'}}>
                Lot de la Semaine <span style={{fontSize: '0.9rem', background: '#eab308', color: '#000', padding: '2px 8px', borderRadius: '12px', fontWeight: 'bold', verticalAlign: 'middle'}}>COMPÉTITION</span>
              </h2>
              <p style={{color: 'var(--text)', margin: 0, fontSize: '1.1rem', maxWidth: '600px', lineHeight: '1.5'}}>
                Le joueur ayant complété le plus de quêtes <strong>à la fin de la semaine</strong> recevra automatiquement la récompense suivante : 
                <strong style={{color: '#eab308', fontSize: '1.3rem', display: 'block', marginTop: '0.5rem'}}>{questsData?.reward}</strong>
                <span style={{display: 'inline-block', marginTop: '0.5rem', fontSize: '0.9rem', color: '#fbbf24', background: 'rgba(234, 179, 8, 0.1)', padding: '4px 8px', borderRadius: '4px'}}>
                  <Clock size={14} style={{display: 'inline', verticalAlign: 'text-bottom', marginRight: '4px'}}/>
                  Temps restant : {questsData?.timeRemaining}
                </span>
              </p>
            </div>
          </div>

          {/* QUESTS TABS */}
          <div style={{display: 'flex', gap: '1rem', marginBottom: '2rem', borderBottom: '1px solid var(--card-border)', paddingBottom: '1rem'}}>
            {[
              { id: 'daily', label: "Aujourd'hui", icon: Clock },
              { id: 'weekly', label: "Cette Semaine", icon: Calendar },
              { id: 'monthly', label: "Ce Mois", icon: BarChart },
              { id: 'total', label: "Total Global", icon: ListOrdered }
            ].map(tab => (
              <button key={tab.id} onClick={() => setQuestSubTab(tab.id as any)} style={{
                background: 'transparent', border: 'none', color: questSubTab === tab.id ? 'var(--accent)' : 'var(--text-muted)',
                fontWeight: 600, fontSize: '1.1rem', cursor: 'pointer', padding: '0.5rem 1rem', display: 'flex', alignItems: 'center', gap: '0.5rem',
                borderBottom: questSubTab === tab.id ? '2px solid var(--accent)' : '2px solid transparent',
                transition: 'all 0.2s'
              }}>
                <tab.icon size={18} /> {tab.label}
              </button>
            ))}
          </div>

          {/* QUESTS LEADERBOARD LIST */}
          <div className="admin-card" style={{background: 'var(--card-bg)', backdropFilter: 'blur(12px)'}}>
            {questsData && questsData[questSubTab] && questsData[questSubTab].length > 0 ? (
              <div style={{display: 'flex', flexDirection: 'column', gap: '1rem'}}>
                {questsData[questSubTab].map((p: any, idx: number) => {
                  const isTop = idx < 3;
                  const medals = ['#fbbf24', '#94a3b8', '#b45309'];
                  return (
                    <div key={idx} style={{
                      display: 'flex', alignItems: 'center', padding: '1.2rem',
                      background: isTop ? `linear-gradient(90deg, ${medals[idx]}11, transparent)` : 'var(--card-bg)',
                      borderRadius: '12px', border: isTop ? `1px solid ${medals[idx]}33` : '1px solid var(--card-border)',
                      transition: 'transform 0.2s', cursor: 'default'
                    }}
                    onMouseEnter={e => e.currentTarget.style.transform = 'translateX(10px)'}
                    onMouseLeave={e => e.currentTarget.style.transform = 'translateX(0)'}>
                      <div style={{
                        width: '45px', height: '45px', display: 'flex', alignItems: 'center', justifyContent: 'center',
                        borderRadius: '50%', background: isTop ? medals[idx] : 'var(--card-border)',
                        color: isTop ? '#000' : 'var(--text-muted)', fontWeight: 'bold', fontSize: '1.3rem', marginRight: '1.5rem'
                      }}>
                        {idx + 1}
                      </div>
                      <div style={{flex: 1}}>
                        <div style={{fontWeight: 700, fontSize: '1.2rem', color: isTop ? medals[idx] : 'var(--text)'}}>{p.playerName}</div>
                      </div>
                      <div style={{textAlign: 'right'}}>
                        <div style={{fontSize: '1.5rem', fontWeight: 800, color: 'var(--text)'}}>{p.count}</div>
                        <div style={{fontSize: '0.85rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '1px'}}>Quêtes Fines</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div style={{textAlign: 'center', padding: '4rem', color: 'var(--text-muted)'}}>
                <Target size={48} style={{opacity: 0.3, marginBottom: '1rem'}} />
                <p style={{fontSize: '1.2rem'}}>Aucune donnée pour cette période.</p>
              </div>
            )}
          </div>
        </div>
      ) : activeTab === 'GUILDS' ? (
        <div style={{animation: 'fadeIn 0.5s'}}>
          {teamsData.length > 0 ? (
            <>
              {/* GLOBAL WEEKLY QUEST BANNER */}
              <div style={{
                background: 'linear-gradient(135deg, rgba(234, 179, 8, 0.15), rgba(234, 179, 8, 0.05))',
                border: '1px solid rgba(234, 179, 8, 0.4)',
                borderRadius: '16px', padding: '1.5rem 2rem', marginBottom: '2rem', display: 'flex', alignItems: 'center', gap: '1.5rem',
                boxShadow: '0 10px 40px rgba(234, 179, 8, 0.15)'
              }}>
                <div style={{padding: '1rem', background: 'rgba(234, 179, 8, 0.2)', borderRadius: '50%', color: '#eab308'}}>
                  <Target size={40} />
                </div>
                <div>
                  <h2 style={{color: '#eab308', margin: '0 0 0.5rem 0', fontSize: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem'}}>
                    Quête de Guilde Hebdomadaire
                  </h2>
                  <p style={{color: 'var(--text)', margin: 0, fontSize: '1.2rem', fontWeight: 'bold'}}>
                    Objectif : {teamsData[0]?.quest_desc || 'Quête en cours'}
                  </p>
                </div>
              </div>

              {/* LEADERBOARD */}
              <div style={{display: 'flex', flexDirection: 'column', gap: '15px'}}>
              {teamsData.map((team: any, idx: number) => {
                const isTop = idx < 3;
                const medals = ['#fbbf24', '#94a3b8', '#b45309'];
                
                return (
                  <div key={idx} className="admin-card" style={{
                    padding: '20px', 
                    background: isTop ? `linear-gradient(90deg, ${medals[idx]}11, transparent), var(--card-bg)` : 'var(--card-bg)', 
                    backdropFilter: 'blur(12px)',
                    border: isTop ? `1px solid ${medals[idx]}55` : '1px solid var(--card-border)',
                    boxShadow: isTop ? `0 5px 20px ${medals[idx]}22` : 'none',
                    display: 'flex', flexDirection: 'column', gap: '15px',
                    transition: 'transform 0.2s'
                  }}>
                    <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                      <div style={{display: 'flex', alignItems: 'center', gap: '15px'}}>
                        <div style={{
                          width: '40px', height: '40px', borderRadius: '50%', 
                          background: isTop ? medals[idx] : 'var(--card-border)', 
                          color: isTop ? '#000' : 'var(--text-muted)', 
                          display: 'flex', alignItems: 'center', justifyContent: 'center', 
                          fontWeight: 'bold', fontSize: '1.2rem'
                        }}>
                          {idx + 1}
                        </div>
                        <h2 style={{margin: 0, fontSize: '1.5rem', color: isTop ? medals[idx] : 'var(--text)'}}>{team.name}</h2>
                      </div>
                      <div style={{textAlign: 'right'}}>
                        <div style={{fontSize: '1.2rem', fontWeight: 800, color: 'var(--accent)'}}>{team.weekly_points} pts</div>
                        <div style={{fontSize: '0.8rem', color: 'var(--text-muted)'}}>Semaine en cours</div>
                      </div>
                    </div>
                    
                    <div style={{width: '100%', marginTop: '10px'}}>
                      <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '1rem'}}>
                        <span style={{display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)'}}>
                          <Target size={16}/> 
                          Progression ({team.quest_progress} / {team.quest_goal})
                        </span>
                        <span style={{fontWeight: 'bold', color: team.quest_progress_percent === 100 ? '#10b981' : '#f97316'}}>{team.quest_progress_percent}%</span>
                      </div>
                      <div style={{width: '100%', height: '12px', background: 'var(--bg-color)', borderRadius: '6px', overflow: 'hidden', border: '1px solid var(--card-border)'}}>
                        <div style={{
                          width: `${team.quest_progress_percent}%`, 
                          height: '100%', 
                          background: team.quest_progress_percent === 100 ? '#10b981' : 'linear-gradient(90deg, #f97316, #fbbf24)',
                          transition: 'width 0.5s ease-out',
                          boxShadow: '0 0 10px rgba(249, 115, 22, 0.5)'
                        }}></div>
                      </div>
                    </div>
                  </div>
                );
              })}
              </div>
            </>
          ) : (
            <div style={{textAlign: 'center', padding: '4rem', color: 'var(--text-muted)'}}>
              <Shield size={48} style={{opacity: 0.3, marginBottom: '1rem'}} />
              <p style={{fontSize: '1.2rem'}}>Aucune guilde n'a encore été créée.</p>
            </div>
          )}
        </div>
      ) : null}

      <style>{`
        @keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes fadeInDown { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
      `}</style>
    </div>
  );
}

import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import './i18n';
import { Lock, ShoppingCart, Settings, LogOut, Package, Plus, Trash2, TrendingUp, Shield, ToggleLeft, ToggleRight, FileText, Target, Gamepad2, Users, UserX, Gavel, Mic, MicOff, MessageSquare, Menu, X } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { PlayerLogin, PlayerDashboard } from './PlayerPortal';
import { Docs } from './Docs';

// === TYPES ===
interface ConfigState {
  inflationExponent: number;
  ahTaxPercentage: number;
  headDropChance: number;
  adminPassword?: string;
  maxQuestsRerolls: number;
  lootrPreventBreak?: boolean;
  lootrPreventHopper?: boolean;
  lootrParticles?: boolean;
  motdLine1?: string;
  motdLine2?: string;
  minigameWheelEnabled: boolean;
  minigameCasinoEnabled: boolean;
  publicFeaturesText: string;
  bluemapUrl: string;
  serverIp: string;
  tombBlockType: string;
  tombStoreXp: boolean;
  tombExpirationSeconds: number;
  tombExpirationAction: string;
  tombDefaultAccess: string;
}

interface ShopItem {
  material: string;
  baseBuyPrice: number;
  baseSellPrice: number;
  stock: number;
  targetStock: number;
  currentBuyPrice?: number;
  currentSellPrice?: number;
  isCommand?: boolean;
  commandToExecute?: string;
  isEnabled?: boolean;
}

interface ShopCategory {
  id: string;
  displayName: string;
  icon: string;
  items: ShopItem[];
}

// === CONSTANTES ===
const API_URL = '/api';

// === COMPOSANT : LOGIN ADMIN ===
function AdminLogin({ onLogin }: { onLogin: (pwd: string) => void }) {
  const { t } = useTranslation();
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_URL}/admin/config`, {
        headers: { 'Authorization': `Bearer ${password}` }
      });
      if (res.ok) {
        onLogin(password);
      } else {
        setError(t("web.auth.invalid_password") || "Invalid password");
      }
    } catch (err) {
      setError(t("web.auth.connection_error") || "Connection error");
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-icon"><Lock size={48} /></div>
        <h2>{t('web.auth.login_title')}</h2>
        <p>GensCore Admin</p>
        
        <form onSubmit={handleSubmit}>
          <input 
            type="password" 
            placeholder={t("web.auth.password")} 
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="login-input"
          />
          {error && <div className="login-error">{error}</div>}
          <button type="submit" className="login-button">{t("web.auth.login_btn")}</button>
        </form>
      </div>
    </div>
  );
}

// === COMPOSANT : VUE ADMIN (LAYOUT AVEC SIDEBAR) ===
function AdminLayout({ password, onLogout }: { password: string, onLogout: () => void }) {
  const { t } = useTranslation();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<'shop' | 'settings' | 'modules' | 'files' | 'players' | 'content'>((localStorage.getItem('gens_admin_tab') as any) || 'shop');
  const [config, setConfig] = useState<ConfigState>({
    inflationExponent: 0.5,
    ahTaxPercentage: 0.0,
    headDropChance: 10.0,
    maxQuestsRerolls: 3,
    lootrPreventBreak: false,
    lootrPreventHopper: true,
    lootrParticles: true,
    motdLine1: "&3&lLe Serveur Des Gens Bien",
    motdLine2: "&7&l>> &eSaison 4 &7&l- &bdiscord.gg/gensbien",
    minigameWheelEnabled: true,
    minigameCasinoEnabled: true,
    publicFeaturesText: "",
    bluemapUrl: "http://localhost:8100",
    serverIp: "gens-core.duckdns.org",
    tombBlockType: "CHEST",
    tombStoreXp: true,
    tombExpirationSeconds: 3600,
    tombExpirationAction: "UNLOCK",
    tombDefaultAccess: "OWNER_ONLY"
  });

  useEffect(() => {
    localStorage.setItem('gens_admin_tab', activeTab);
  }, [activeTab]);

  useEffect(() => {
    fetch(`${API_URL}/admin/config`, { headers: { 'Authorization': `Bearer ${password}` } })
      .then(res => res.json())
      .then(data => setConfig(data))
      .catch(console.error);
  }, [password]);

  const handleSaveConfig = (e: React.FormEvent) => {
    e.preventDefault();
    saveConfigToServer(config);
  };

  const saveConfigToServer = (newConfig: ConfigState) => {
    fetch(`${API_URL}/admin/config`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${password}` },
      body: JSON.stringify(newConfig)
    }).then(res => {
      if (!res.ok) alert('Erreur lors de la sauvegarde de la configuration');
    });
  };

  const toggleMinigame = (game: 'wheel' | 'casino', state: boolean) => {
    const newConfig = {
      ...config,
      ...(game === 'wheel' ? { minigameWheelEnabled: state } : { minigameCasinoEnabled: state })
    };
    setConfig(newConfig);
    saveConfigToServer(newConfig);
  };

  return (
    <div className="admin-layout">
      {/* Overlay mobile */}
      {isSidebarOpen && (
        <div 
          style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', zIndex: 90 }}
          onClick={() => setIsSidebarOpen(false)}
        />
      )}
      <aside className={`admin-sidebar ${isSidebarOpen ? 'open' : ''}`}>
        <div className="admin-sidebar-header">
          <Settings size={28} />
          <h2>GensCore</h2>
          <button 
            className="mobile-close-btn" 
            onClick={() => setIsSidebarOpen(false)}
            style={{ marginLeft: 'auto', background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', display: 'none' }}
          >
            <X size={24} />
          </button>
        </div>
        <nav className="admin-nav">
          <a style={{cursor: 'pointer'}} className={activeTab === 'shop' ? 'active' : ''} onClick={() => { setActiveTab('shop'); setIsSidebarOpen(false); }}><ShoppingCart size={18}/> {t("web.admin.tabs.shop") || "Shop"}</a>
          <a style={{cursor: 'pointer'}} className={activeTab === 'modules' ? 'active' : ''} onClick={() => { setActiveTab('modules'); setIsSidebarOpen(false); }}><Package size={18}/> {t("web.admin.tabs.modules") || "Modules"}</a>
          <a style={{cursor: 'pointer'}} className={activeTab === 'files' ? 'active' : ''} onClick={() => { setActiveTab('files'); setIsSidebarOpen(false); }}><FileText size={18}/> {t("web.admin.tabs.files") || "Files"}</a>
          <a style={{cursor: 'pointer'}} className={activeTab === 'settings' ? 'active' : ''} onClick={() => { setActiveTab('settings'); setIsSidebarOpen(false); }}><Settings size={18}/> {t("web.admin.tabs.settings") || "Settings"}</a>
          <a style={{cursor: 'pointer'}} className={activeTab === 'players' ? 'active' : ''} onClick={() => { setActiveTab('players'); setIsSidebarOpen(false); }}><Users size={18}/> {t("web.admin.tabs.players") || "Players"}</a>
          <a style={{cursor: 'pointer'}} className={activeTab === 'content' ? 'active' : ''} onClick={() => { setActiveTab('content'); setIsSidebarOpen(false); }}><FileText size={18}/> {t("web.admin.tabs.content") || "Content"}</a>
        </nav>
        <div className="admin-sidebar-footer">
          <button className="logout-button" onClick={onLogout}><LogOut size={18}/> {t("web.nav.logout")}</button>
        </div>
      </aside>
      
      <main className="admin-main">
        <header className="admin-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
            <button 
              className="mobile-menu-btn" 
              onClick={() => setIsSidebarOpen(true)}
              style={{ background: 'var(--card-bg)', border: '1px solid var(--card-border)', color: 'var(--text-main)', padding: '10px', borderRadius: '12px', cursor: 'pointer', display: 'none' }}
            >
              <Menu size={24} />
            </button>
            <h2>{activeTab === 'shop' ? 'Gestion de la Boutique' : activeTab === 'modules' ? 'Gestion des Modules' : activeTab === 'files' ? 'Éditeur de Fichiers' : activeTab === 'players' ? 'Modération' : activeTab === 'content' ? 'Édition de l\'Accueil' : 'Configuration du Serveur'}</h2>
          </div>
          <div className="admin-user"><Shield size={18}/> Administrateur</div>
        </header>

        {activeTab === 'shop' && <AdminShop password={password} />}

        {activeTab === 'settings' && config && (
          <form onSubmit={handleSaveConfig}>
            <div className="settings-grid">
              <div className="admin-card">
                <div className="settings-section-title"><Settings size={20} /> Économie & Quêtes</div>
                <div className="form-group">
                  <label>Exposant d'inflation (défaut: 0.5)</label>
                  <input type="number" step="0.01" value={config.inflationExponent} onChange={e => setConfig({...config, inflationExponent: parseFloat(e.target.value)})} required className="login-input" />
                </div>
                <div className="form-group">
                  <label>Taxe de l'Hôtel des Ventes (en %)</label>
                  <input type="number" step="0.1" value={config.ahTaxPercentage} onChange={e => setConfig({...config, ahTaxPercentage: parseFloat(e.target.value)})} required className="login-input" />
                </div>

                <div className="form-group">
                  <label>URL BlueMap (IP Serveur)</label>
                  <input type="text" value={config.bluemapUrl || ""} onChange={e => setConfig({...config, bluemapUrl: e.target.value})} required className="login-input" />
                </div>
                <div className="form-group">
                  <label>IP Serveur (Accueil)</label>
                  <input type="text" value={config.serverIp || ""} onChange={e => setConfig({...config, serverIp: e.target.value})} required className="login-input" />
                </div>
                <div className="form-group">
                  <label>Chance de drop d'une tête (en %)</label>
                  <input type="number" step="0.1" max="100" min="0" value={config.headDropChance} onChange={e => setConfig({...config, headDropChance: parseFloat(e.target.value)})} required className="login-input" />
                </div>
                <div className="form-group">
                  <label>Limite Quotidienne Reroll Quêtes</label>
                  <input type="number" min="0" className="login-input" value={config.maxQuestsRerolls} onChange={(e) => setConfig({...config, maxQuestsRerolls: parseInt(e.target.value) || 0})} />
                </div>
              </div>

              <div className="admin-card">
                <div className="settings-section-title"><Target size={20} /> Configuration MOTD</div>
                <div className="form-group" style={{marginBottom: '1rem'}}>
                  <p style={{fontSize: '0.9rem', color: 'var(--text-muted)'}}>
                    L'activation globale du MOTD se fait depuis l'onglet "Modules".
                  </p>
                </div>
                <div className="form-group">
                  <label>Ligne 1</label>
                  <input type="text" value={config.motdLine1 || ''} onChange={e => setConfig({...config, motdLine1: e.target.value})} className="login-input" />
                </div>
                <div className="form-group">
                  <label>Ligne 2</label>
                  <input type="text" value={config.motdLine2 || ''} onChange={e => setConfig({...config, motdLine2: e.target.value})} className="login-input" />
                </div>
              </div>

              <div className="admin-card">
                <div className="settings-section-title"><Package size={20} /> Configuration Lootr</div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.lootrPreventBreak} onChange={e => setConfig({...config, lootrPreventBreak: e.target.checked})} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>Empêcher de casser les coffres</span>
                </div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.lootrPreventHopper} onChange={e => setConfig({...config, lootrPreventHopper: e.target.checked})} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>Bloquer les entonnoirs (Hoppers)</span>
                </div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.lootrParticles} onChange={e => setConfig({...config, lootrParticles: e.target.checked})} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>Particules sur les coffres non fouillés</span>
                </div>
              </div>

              <div className="admin-card">
                <div className="settings-section-title"><Settings size={20} /> Configuration Tombes</div>
                <div className="form-group">
                  <label>Type de Bloc (ex: CHEST, BARREL, PLAYER_HEAD)</label>
                  <input type="text" value={config.tombBlockType || 'CHEST'} onChange={e => setConfig({...config, tombBlockType: e.target.value})} className="login-input" />
                </div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.tombStoreXp} onChange={e => setConfig({...config, tombStoreXp: e.target.checked})} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>Sauvegarder l'XP dans la tombe</span>
                </div>
                <div className="form-group">
                  <label>Temps d'expiration (Secondes)</label>
                  <input type="number" min="0" value={config.tombExpirationSeconds || 3600} onChange={e => setConfig({...config, tombExpirationSeconds: parseInt(e.target.value) || 0})} className="login-input" />
                </div>
                <div className="form-group">
                  <label>Action à l'expiration</label>
                  <select value={config.tombExpirationAction || 'UNLOCK'} onChange={e => setConfig({...config, tombExpirationAction: e.target.value})} className="login-input">
                    <option value="UNLOCK">UNLOCK (Ouvrir à tous)</option>
                    <option value="DROP">DROP (Lâcher les objets)</option>
                    <option value="DESTROY">DESTROY (Détruire les objets)</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Accès par défaut</label>
                  <select value={config.tombDefaultAccess || 'OWNER_ONLY'} onChange={e => setConfig({...config, tombDefaultAccess: e.target.value})} className="login-input">
                    <option value="OWNER_ONLY">OWNER_ONLY (Seulement le propriétaire)</option>
                    <option value="EVERYONE">EVERYONE (Tout le monde)</option>
                  </select>
                </div>
              </div>

              <div className="admin-card">
                <div className="settings-section-title"><Gamepad2 size={20} /> Mini-Jeux</div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.minigameWheelEnabled !== false} onChange={e => toggleMinigame('wheel', e.target.checked)} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>Activer la Roue de la Fortune</span>
                </div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.minigameCasinoEnabled !== false} onChange={e => toggleMinigame('casino', e.target.checked)} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>Activer la Machine à Sous</span>
                </div>
              </div>

              <div className="admin-card">
                <div className="settings-section-title"><Shield size={20} /> Sécurité</div>
                <div className="form-group">
                  <label>Nouveau Mot de passe Administrateur</label>
                  <input type="text" placeholder="Laisser vide pour ne pas changer" value={config.adminPassword || ''} onChange={e => setConfig({...config, adminPassword: e.target.value})} className="login-input" />
                </div>
                <div style={{marginTop: '2rem'}}>
                  <button type="submit" className="login-button" style={{display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '10px'}}>
                    <Settings size={18} /> Sauvegarder la configuration
                  </button>
                </div>
              </div>
              <div className="admin-card">
                <div className="settings-section-title"><Trash2 size={20} /> Gestion des Données</div>
                <div className="form-group" style={{ marginBottom: '0' }}>
                  <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                    Supprimer tous les homes des joueurs. Cette action est irréversible.
                  </p>
                  <button 
                    type="button" 
                    className="btn" 
                    style={{ background: '#ef4444', color: 'white', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '10px' }}
                    onClick={async () => {
                      if (confirm('Voulez-vous vraiment supprimer TOUS les homes de TOUS les joueurs ?')) {
                        const res = await fetch(`${API_URL}/admin/homes`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${password}` } });
                        if (res.ok) alert('Tous les homes ont été supprimés avec succès.');
                        else alert('Erreur lors de la suppression des homes.');
                      }
                    }}
                  >
                    <Trash2 size={18} /> Clear All Homes
                  </button>
                </div>
              </div>
            </div>
          </form>
        )}
        {activeTab === 'content' && (
          <form onSubmit={handleSaveConfig}>
            <div className="admin-card">
              <div className="settings-section-title"><FileText size={20} /> Annonces et Nouveautés</div>
              <p style={{color: 'var(--text-muted)', marginBottom: '1.5rem'}}>Ce texte apparaîtra sur la page d'accueil publique de tous les joueurs.</p>
              <div className="form-group">
                <textarea 
                  value={config.publicFeaturesText || ''} 
                  onChange={e => setConfig({...config, publicFeaturesText: e.target.value})} 
                  className="input-field" 
                  style={{minHeight: '300px', resize: 'vertical', fontFamily: 'monospace', lineHeight: '1.5'}} 
                  placeholder="Écrivez le texte de la page d'accueil ici..."
                />
              </div>
              <div style={{marginTop: '2rem'}}>
                <button type="submit" className="login-button" style={{display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '10px'}}>
                  <Settings size={18} /> Sauvegarder et Publier
                </button>
              </div>
            </div>
          </form>
        )}
        {activeTab === 'modules' && <AdminModules password={password} />}
        {activeTab === 'files' && <AdminFiles password={password} />}
        {activeTab === 'players' && <AdminPlayers password={password} />}
      </main>
    </div>
  );
}

// === COMPOSANT : GESTION DES FICHIERS ===
function AdminPlayers({ password }: { password: string }) {
  const [players, setPlayers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionModal, setActionModal] = useState<{player: string, action: string} | null>(null);
  const [reason, setReason] = useState('');
  const [duration, setDuration] = useState(0);
  const [durationType, setDurationType] = useState('hours');

  const fetchPlayers = () => {
    fetch(`${API_URL}/admin/players`, { headers: { 'Authorization': `Bearer ${password}` } })
      .then(res => res.json())
      .then(data => { setPlayers(data); setLoading(false); })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchPlayers();
    const interval = setInterval(fetchPlayers, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleAction = () => {
    if (!actionModal) return;
    
    let hours = 0;
    let days = 0;
    if (actionModal.action === 'ban' || actionModal.action === 'mute') {
        if (durationType === 'hours') hours = duration;
        else days = duration;
    }

    fetch(`${API_URL}/admin/players/action`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${password}` },
      body: JSON.stringify({ 
        action: actionModal.action, 
        playerName: actionModal.player, 
        reason: reason,
        durationHours: hours,
        durationDays: days
      })
    }).then(() => {
      setActionModal(null);
      setReason('');
      setDuration(0);
      fetchPlayers();
    });
  };

  if (loading) return <div className="loading">Chargement des joueurs...</div>;

  return (
    <div>
      <h2 style={{marginBottom: '2rem'}}>Tous les Joueurs ({players.length})</h2>
      
      <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '1.5rem'}}>
        {players.map(p => (
          <div key={p.uuid} className="admin-card" style={{padding: '1.5rem'}}>
            <div style={{display: 'flex', alignItems: 'center', gap: '15px', marginBottom: '1.5rem'}}>
              <div style={{position: 'relative'}}>
                <img src={`https://mc-heads.net/avatar/${p.name}`} alt={p.name} style={{width: '48px', height: '48px', borderRadius: '8px'}} />
                <div style={{
                  position: 'absolute', bottom: '-4px', right: '-4px', width: '14px', height: '14px', borderRadius: '50%',
                  background: p.online ? 'var(--success)' : 'var(--text-muted)', border: '2px solid var(--card-bg)'
                }}></div>
              </div>
              <div>
                <h3 style={{margin: 0, fontSize: '1.2rem'}}>{p.name}</h3>
                <div style={{fontSize: '0.9rem', color: 'var(--text-muted)'}}>
                  {p.online ? `Ping: ${p.ping}ms • ` : 'Hors-Ligne • '} 
                  Temps de jeu: {Math.floor(p.playtime/60)}h
                </div>
              </div>
            </div>
            
            <div style={{display: 'flex', gap: '10px', flexWrap: 'wrap'}}>
              {p.online && (
                <button className="btn-small" style={{flex: 1, background: 'var(--bg-color)', color: '#ef4444', border: '1px solid #ef4444'}} onClick={() => setActionModal({player: p.name, action: 'kick'})}>
                  <UserX size={16}/> Kick
                </button>
              )}
              {p.isBanned ? (
                <button className="btn-small" style={{flex: 1, background: 'var(--success)', color: 'white', border: 'none'}} onClick={() => setActionModal({player: p.name, action: 'unban'})}>
                  <Gavel size={16}/> Unban
                </button>
              ) : (
                <button className="btn-small" style={{flex: 1, background: '#ef4444', color: 'white', border: 'none'}} onClick={() => setActionModal({player: p.name, action: 'ban'})}>
                  <Gavel size={16}/> Ban
                </button>
              )}
              {p.isMuted ? (
                <button className="btn-small" style={{flex: 1, background: 'var(--success)', color: 'white', border: 'none'}} onClick={() => setActionModal({player: p.name, action: 'unmute'})}>
                  <Mic size={16}/> Unmute
                </button>
              ) : (
                <button className="btn-small" style={{flex: 1, background: 'var(--bg-color)', color: '#f59e0b', border: '1px solid #f59e0b'}} onClick={() => setActionModal({player: p.name, action: 'mute'})}>
                  <MicOff size={16}/> Mute
                </button>
              )}
              {p.online && (
                <button className="btn-small" style={{flex: 1, background: 'var(--bg-color)', color: '#3b82f6', border: '1px solid #3b82f6'}} onClick={() => setActionModal({player: p.name, action: 'message'})}>
                  <MessageSquare size={16}/> MP
                </button>
              )}
            </div>
          </div>
        ))}
        {players.length === 0 && <div style={{color: 'var(--text-muted)'}}>Aucun joueur inscrit.</div>}
      </div>

      {actionModal && (
        <div className="modal-overlay">
          <div className="modal-content admin-card">
            <h3 style={{marginBottom: '1rem', textTransform: 'capitalize'}}>Action: {actionModal.action} ({actionModal.player})</h3>
            
            {['ban', 'mute', 'kick', 'message'].includes(actionModal.action) && (
              <div className="form-group">
                <label>{actionModal.action === 'message' ? 'Message' : 'Raison'}</label>
                <input type="text" value={reason} onChange={e => setReason(e.target.value)} placeholder="Ex: Spam, triche..." className="input-field" />
              </div>
            )}

            {(actionModal.action === 'ban' || actionModal.action === 'mute') && (
              <div style={{display: 'flex', gap: '10px'}}>
                <div className="form-group" style={{flex: 2}}>
                  <label>Durée (0 = Permanent)</label>
                  <input type="number" value={duration} onChange={e => setDuration(parseFloat(e.target.value) || 0)} min="0" className="input-field" />
                </div>
                <div className="form-group" style={{flex: 1}}>
                  <label>Unité</label>
                  <select value={durationType} onChange={e => setDurationType(e.target.value)} className="input-field">
                    <option value="hours">Heures</option>
                    <option value="days">Jours</option>
                  </select>
                </div>
              </div>
            )}

            <div style={{display: 'flex', gap: '10px', marginTop: '20px'}}>
              <button className="btn" onClick={handleAction}>Confirmer</button>
              <button className="btn" style={{background: 'transparent', border: '1px solid var(--card-border)'}} onClick={() => setActionModal(null)}>Annuler</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function AdminFiles({ password }: { password: string }) {
  const [content, setContent] = useState('');
  const [currentFile, setCurrentFile] = useState('config.yml');
  const [loading, setLoading] = useState(true);
  const [saved, setSaved] = useState(false);

  const fetchFile = (fileName: string) => {
    setLoading(true);
    fetch(`${API_URL}/admin/file?path=${fileName}`, { headers: { 'Authorization': `Bearer ${password}` } })
      .then(res => res.text())
      .then(data => { setContent(data); setLoading(false); })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchFile(currentFile);
  }, [currentFile]);

  const saveFile = () => {
    fetch(`${API_URL}/admin/file?path=${currentFile}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${password}` },
      body: JSON.stringify({ content })
    }).then(() => {
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    });
  };

  // Calcul du nombre de lignes pour afficher la gouttière
  const lineCount = content.split('\n').length;
  const lines = Array.from({ length: Math.max(10, lineCount) }, (_, i) => i + 1);

  return (
    <div style={{display: 'flex', flexDirection: 'column', height: '100%', gap: '1rem'}}>
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
        <div style={{display: 'flex', gap: '15px', alignItems: 'center'}}>
          <FileText size={24} color="var(--accent)"/>
          <h2 style={{margin: 0}}>Éditeur de Fichiers</h2>
        </div>
        
        <div style={{display: 'flex', gap: '10px', alignItems: 'center'}}>
          <select value={currentFile} onChange={(e) => setCurrentFile(e.target.value)} style={{background: 'var(--bg-color)', color: 'var(--text-color)', padding: '10px 15px', borderRadius: '8px', border: '1px solid var(--card-border)', outline: 'none', fontSize: '1rem'}}>
            <option value="config.yml">config.yml</option>
          </select>
          <button className="btn" onClick={saveFile} style={{width: 'auto', background: saved ? '#10b981' : 'var(--accent)', display: 'flex', alignItems: 'center', gap: '8px'}}>
            {saved ? <Shield size={18}/> : <FileText size={18}/>}
            {saved ? 'Enregistré !' : 'Sauvegarder'}
          </button>
        </div>
      </div>
      
      {loading ? <div className="loading">Chargement du fichier...</div> : (
        <div style={{
            flex: 1, 
            display: 'flex', 
            background: '#1e1e1e', 
            borderRadius: '12px', 
            border: '1px solid #333', 
            overflow: 'hidden',
            boxShadow: '0 10px 30px rgba(0,0,0,0.5)',
            position: 'relative'
        }}>
          {/* Gouttière des numéros de ligne */}
          <div style={{
              width: '50px', 
              background: '#252526', 
              color: '#858585', 
              textAlign: 'right', 
              padding: '15px 10px 15px 0', 
              fontFamily: '"Fira Code", Consolas, monospace',
              fontSize: '14px',
              lineHeight: '21px',
              userSelect: 'none',
              borderRight: '1px solid #333'
          }}>
            {lines.map(l => <div key={l}>{l}</div>)}
          </div>
          
          {/* Zone de texte principale */}
          <textarea 
            value={content} 
            onChange={(e) => setContent(e.target.value)}
            spellCheck="false"
            style={{
              flex: 1, 
              padding: '15px',
              background: 'transparent', 
              color: '#d4d4d4',
              border: 'none', 
              outline: 'none',
              fontFamily: '"Fira Code", Consolas, monospace',
              fontSize: '14px',
              lineHeight: '21px',
              resize: 'none',
              whiteSpace: 'pre',
              overflowWrap: 'normal',
              overflowX: 'auto'
            }}
          />
        </div>
      )}
    </div>
  );
}

// === COMPOSANT : GESTION DES MODULES ===
function AdminModules({ password }: { password: string }) {
  const [modules, setModules] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchModules = () => {
    fetch(`${API_URL}/modules`)
      .then(res => res.json())
      .then(data => { setModules(data); setLoading(false); })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchModules();
  }, []);

  const toggleModule = (name: string, currentState: boolean) => {
    // Mise à jour optimiste (visuelle immédiate)
    setModules(prev => prev.map(m => m.name === name ? { ...m, enabled: !currentState } : m));
    
    fetch(`${API_URL}/admin/modules/${name}/toggle`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${password}` },
      body: JSON.stringify({ state: !currentState })
    });
  };

  const getCategory = (name: string) => {
    const n = name.toLowerCase();
    if (['economy', 'shop', 'dynamicshop', 'auctionhouse', 'jobs'].includes(n)) return 'Économie & Commerce';
    if (['quests', 'stats', 'spawners', 'loot', 'headdrop', 'minigame'].includes(n)) return 'Joueurs & Gameplay';
    if (['motd', 'tabboard', 'discord', 'gui', 'web'].includes(n)) return 'Interface & Communication';
    return 'Administration & Utilitaires';
  };

  const toggleCategory = (mods: any[], targetState: boolean) => {
    mods.forEach(mod => {
      if (mod.enabled !== targetState) {
        toggleModule(mod.name, mod.enabled);
      }
    });
  };

  if (loading) return <div className="loading">Chargement des modules...</div>;

  const groupedModules = modules.reduce((acc, mod) => {
    const cat = getCategory(mod.name);
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(mod);
    return acc;
  }, {} as Record<string, any[]>);

  return (
    <div style={{display: 'flex', flexDirection: 'column', gap: '3rem'}}>
      {Object.entries(groupedModules).map(([catName, _mods]) => {
        const mods = _mods as any[];
        return (
        <div key={catName}>
          <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', paddingBottom: '0.5rem', borderBottom: '1px solid var(--card-border)'}}>
            <h2 style={{fontSize: '1.5rem', margin: 0}}>{catName}</h2>
            <div style={{display: 'flex', gap: '10px'}}>
              <button className="btn-small" style={{background: '#10b981', color: 'white'}} onClick={() => toggleCategory(mods, true)}>Tout Activer</button>
              <button className="btn-small" style={{background: '#ef4444', color: 'white'}} onClick={() => toggleCategory(mods, false)}>Tout Désactiver</button>
            </div>
          </div>
          <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem'}}>
            {mods.map(mod => (
              <div key={mod.name} className="admin-card" style={{padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '15px'}}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                  <h3 style={{display: 'flex', alignItems: 'center', gap: '10px'}}>
                    <Package size={20} color="var(--accent)" />
                    {mod.name}
                  </h3>
                  <button className="btn-icon" onClick={() => toggleModule(mod.name, mod.enabled)} style={{color: mod.enabled ? '#10b981' : '#ef4444'}}>
                    {mod.enabled ? <ToggleRight size={32}/> : <ToggleLeft size={32}/>}
                  </button>
                </div>
                <p style={{color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: '1.4'}}>{mod.description}</p>
                <div style={{marginTop: 'auto', paddingTop: '10px', borderTop: '1px solid var(--card-border)', fontSize: '0.8rem'}}>
                  Statut : <strong style={{color: mod.enabled ? '#10b981' : '#ef4444'}}>{mod.enabled ? 'Actif' : 'Désactivé'}</strong>
                </div>
              </div>
            ))}
          </div>
        </div>
      )})}
    </div>
  );
}

// === COMPOSANT : GESTION DE LA BOUTIQUE ===
function AdminShop({ password }: { password: string }) {

  const [categories, setCategories] = useState<ShopCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [isEcoEnabled, setIsEcoEnabled] = useState(true);

  const [showCatModal, setShowCatModal] = useState(false);
  const [showItemModal, setShowItemModal] = useState(false);
  const [editingCategoryId, setEditingCategoryId] = useState('');

  const [catId, setCatId] = useState('');
  const [catName, setCatName] = useState('');
  const [catIcon, setCatIcon] = useState('BRICKS');

  const [itemMat, setItemMat] = useState('STONE');
  const [buyP, setBuyP] = useState(1.0);
  const [sellP, setSellP] = useState(0.5);
  const [targetS, setTargetS] = useState(1000);
  const [isCmd, setIsCmd] = useState(false);
  const [cmdExec, setCmdExec] = useState('');
  const [isEnabled, setIsEnabled] = useState(true);

  const fetchShop = () => {
    fetch(`${API_URL}/modules`)
      .then(res => res.json())
      .then(mods => {
        const eco = mods.find((m: any) => m.name === 'Economy' || m.name === 'DynamicShop');
        if (eco && !eco.enabled) {
            setIsEcoEnabled(false);
            setLoading(false);
            return;
        }
        setIsEcoEnabled(true);
        fetch(`${API_URL}/shop/categories`)
          .then(res => res.json())
          .then(data => { setCategories(data || []); setLoading(false); })
          .catch(() => { setCategories([]); setLoading(false); });
      }).catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchShop();
  }, []);

  const deleteItem = async (categoryId: string, material: string) => {
    if (!confirm('Supprimer cet objet ?')) return;
    try {
      await fetch(`${API_URL}/admin/shop/item/${categoryId}/${material}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${password}` }
      });
      fetchShop();
    } catch (err) {}
  };

  const deleteCategory = async (categoryId: string) => {
    if (!confirm('Supprimer cette catégorie et tous ses objets ?')) return;
    try {
      await fetch(`${API_URL}/admin/shop/category/${categoryId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${password}` }
      });
      fetchShop();
    } catch (err) {}
  };

  const toggleItemEnabled = (categoryId: string, item: any) => {
    fetch(`${API_URL}/admin/shop/item`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${password}` },
      body: JSON.stringify({ categoryId, ...item, isEnabled: !item.isEnabled })
    }).then(() => fetchShop());
  };

  const submitCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    await fetch(`${API_URL}/admin/shop/category`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${password}` },
      body: JSON.stringify({ id: catId, displayName: catName, icon: catIcon, items: [] })
    });
    setShowCatModal(false);
    fetchShop();
  };

  const submitItem = async (e: React.FormEvent) => {
    e.preventDefault();
    await fetch(`${API_URL}/admin/shop/item`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${password}` },
      body: JSON.stringify({
        categoryId: editingCategoryId,
        material: itemMat, baseBuyPrice: buyP, baseSellPrice: sellP, targetStock: targetS, isCommand: isCmd, commandToExecute: cmdExec, isEnabled: isEnabled
      })
    });
    setShowItemModal(false);
    fetchShop();
  };

  if (loading) return <div className="loading">Chargement...</div>;

  if (!isEcoEnabled) {
    return (
      <div className="admin-card" style={{padding: '3rem', textAlign: 'center'}}>
        <div style={{color: 'var(--text-muted)', marginBottom: '1rem'}}>
          <ShoppingCart size={64} style={{opacity: 0.5}} />
        </div>
        <h2>Boutique Désactivée</h2>
        <p style={{color: 'var(--text-muted)'}}>Le module Economy ou DynamicShop est actuellement désactivé. Veuillez le réactiver dans l'onglet Modules pour gérer la boutique.</p>
      </div>
    );
  }

  return (
    <div>
      <div style={{marginBottom: '20px'}}>
        <button className="btn-small btn-primary" onClick={() => { setCatId(''); setCatName(''); setCatIcon('BRICKS'); setShowCatModal(true); }}>
          <Plus size={16}/> Nouvelle Catégorie
        </button>
      </div>

      {/* MODAL CATEGORY */}
      {showCatModal && (
        <div className="modal-overlay">
          <div className="modal-card">
            <h2>Nouvelle Catégorie</h2>
            <form onSubmit={submitCategory}>
              <div className="form-group">
                <label>ID (minuscules, sans espace)</label>
                <input required value={catId} onChange={e=>setCatId(e.target.value)} className="login-input" />
              </div>
              <div className="form-group">
                <label>Nom d'affichage</label>
                <input required value={catName} onChange={e=>setCatName(e.target.value)} className="login-input" />
              </div>
              <div className="form-group">
                <label>Icône (Matériel Bukkit, ex: BRICKS)</label>
                <input required value={catIcon} onChange={e=>setCatIcon(e.target.value)} className="login-input" />
              </div>
              <div style={{display:'flex', gap:'10px', marginTop: '20px'}}>
                <button type="submit" className="login-button">enregistréer</button>
                <button type="button" className="login-button" style={{background:'var(--card-bg)'}} onClick={() => setShowCatModal(false)}>Annuler</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL ITEM */}
      {showItemModal && (
        <div className="modal-overlay">
          <div className="modal-card">
            <h2>Nouvel Objet</h2>
            <form onSubmit={submitItem}>
              <div className="form-group">
                <label>Matériel (ex: DIAMOND)</label>
                <input required value={itemMat} onChange={e=>setItemMat(e.target.value)} className="login-input" />
              </div>
              <div style={{display:'flex', gap:'10px'}}>
                <div className="form-group">
                  <label>Prix Achat Base</label>
                  <input type="number" step="0.1" required value={buyP} onChange={e=>setBuyP(parseFloat(e.target.value))} className="login-input" />
                </div>
                <div className="form-group">
                  <label>Prix Vente Base</label>
                  <input type="number" step="0.1" required value={sellP} onChange={e=>setSellP(parseFloat(e.target.value))} className="login-input" />
                </div>
              </div>
              <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px'}}>
                <input type="checkbox" id="isEnabled" checked={isEnabled} onChange={e => setIsEnabled(e.target.checked)} />
                <label htmlFor="isEnabled" style={{marginBottom: 0}}>Objet Actif (Vendu dans le shop)</label>
              </div>
              <div className="form-group">
                <label>Stock d'équilibre (Cible)</label>
                <input type="number" required value={targetS} onChange={e=>setTargetS(parseInt(e.target.value))} className="login-input" />
              </div>
              <div className="form-group" style={{display:'flex', alignItems:'center', gap:'10px', marginBottom:'15px'}}>
                <input type="checkbox" checked={isCmd} onChange={e=>setIsCmd(e.target.checked)} style={{width:'20px', height:'20px'}} />
                <label style={{marginBottom:0}}>Est-ce un Grade/Commande ?</label>
              </div>
              {isCmd && (
                <div className="form-group">
                  <label>Commande à exécuter (utilisez %player%)</label>
                  <input required value={cmdExec} onChange={e=>setCmdExec(e.target.value)} className="login-input" placeholder="lp user %player% parent set vip" />
                </div>
              )}
              <div style={{display:'flex', gap:'10px', marginTop: '20px'}}>
                <button type="submit" className="login-button">enregistréer</button>
                <button type="button" className="login-button" style={{background:'var(--card-bg)'}} onClick={() => setShowItemModal(false)}>Annuler</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {categories.map(cat => (
        <div key={cat.id} className="shop-category-card">
          <div className="shop-category-header">
            <h3>{cat.displayName} <span>({cat.items.length} objets)</span></h3>
            <div style={{display: 'flex', gap: '10px'}}>
              <button className="btn-small btn-primary" onClick={() => { 
                setEditingCategoryId(cat.id); setItemMat('STONE'); setBuyP(1.0); setSellP(0.5); setTargetS(1000); setIsCmd(false); setCmdExec(''); setIsEnabled(true); setShowItemModal(true); 
              }}><Plus size={16}/> Ajouter objet</button>
              <button className="btn-icon" onClick={() => deleteCategory(cat.id)} title="Supprimer la catégorie"><Trash2 size={16}/></button>
            </div>
          </div>
          <table className="shop-table">
            <thead>
              <tr>
                <th>Objet</th>
                <th>Prix Achat / Vente</th>
                <th>Stock / Target</th>
                <th>Type</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {(cat.items || []).map(item => (
                <tr key={item.material}>
                  <td><strong>{item.material}</strong></td>
                  <td>{item.baseBuyPrice} $ / {item.baseSellPrice} $</td>
                  <td>{item.stock} / {item.targetStock}</td>
                  <td>
                    {item.isCommand ? <span style={{color: 'var(--accent)'}}>Commande</span> : <span>Item Brut</span>}
                  </td>
                  <td>
                    <button className="btn-icon" onClick={() => toggleItemEnabled(cat.id, item)} title={item.isEnabled ? "Désactiver" : "Activer"} style={{marginRight: '10px', color: item.isEnabled ? '#10b981' : '#ef4444'}}>
                      {item.isEnabled ? <ToggleRight size={24}/> : <ToggleLeft size={24}/>}
                    </button>
                    <button className="btn-small btn-primary" onClick={() => {
                      setEditingCategoryId(cat.id);
                      setItemMat(item.material);
                      setBuyP(item.baseBuyPrice);
                      setSellP(item.baseSellPrice);
                      setTargetS(item.targetStock);
                      setIsCmd(item.isCommand || false);
                      setCmdExec(item.commandToExecute || '');
                      setIsEnabled(item.isEnabled ?? true);
                      setShowItemModal(true);
                    }} style={{marginRight: '10px', background: 'var(--accent)', color: 'black'}}>Éditer</button>
                    <button className="btn-icon" onClick={() => deleteItem(cat.id, item.material)} title="Supprimer"><Trash2 size={18}/></button>
                  </td>
                </tr>
              ))}
              {(!cat.items || cat.items.length === 0) && (
                <tr><td colSpan={5} style={{textAlign:'center', color:'var(--text-muted)'}}>Aucun objet dans cette catégorie</td></tr>
              )}
            </tbody>
          </table>
        </div>
      ))}
    </div>
  );
}

// === COMPOSANTS : VUE CLIENT ===

export function ClientShop({ isEnabled }: { isEnabled?: boolean }) {
  const [categories, setCategories] = useState<ShopCategory[]>([]);
  const [selectedItem, setSelectedItem] = useState<ShopItem | null>(null);
  const [history, setHistory] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (isEnabled === false) {
      setLoading(false);
      return;
    }
    fetch(`${API_URL}/shop/categories`)
      .then(res => res.json())
      .then(data => { setCategories(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, []);

  const loadHistory = (item: ShopItem) => {
    setSelectedItem(item);
    fetch(`${API_URL}/shop/history/${item.material}`)
      .then(res => res.json())
      .then(data => {
        const formatted = data.map((d: any) => ({
          time: new Date(d.timestamp).toLocaleTimeString(),
          Achat: d.buyPrice,
          Vente: d.sellPrice,
          Stock: d.stock
        }));
        setHistory(formatted);
      });
  };

  if (isEnabled === false) {
    return (
      <div style={{textAlign: 'center', padding: '4rem 0', color: 'var(--text-muted)'}}>
        <ShoppingCart size={48} style={{opacity: 0.5, marginBottom: '1rem'}} />
        <h2>Boutique Désactivée</h2>
        <p>Le module de boutique est actuellement désactivé par l'administrateur.</p>
      </div>
    );
  }

  if (loading) return <div className="loading">Chargement de la Boutique...</div>;

  return (
    <div>
      <div className="client-hero" style={{padding: '2rem 0'}}>
        <h2>Bourse Dynamique</h2>
        <p>Suivez les prix de l'économie en temps réel</p>
      </div>

      <div style={{display: 'flex', gap: '2rem', alignItems: 'flex-start'}}>
        <div style={{flex: 1}}>
          {categories.map(cat => (
            <div key={cat.id} className="shop-category-card">
              <div className="shop-category-header">
                <h3>{cat.displayName}</h3>
              </div>
              <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem'}}>
                {(cat.items || []).filter(i => i.isEnabled !== false).map(item => (
                  <div 
                    key={item.material} 
                    style={{
                      background: 'rgba(0,0,0,0.2)', padding: '1rem', borderRadius: '8px', cursor: 'pointer',
                      border: selectedItem?.material === item.material ? '1px solid var(--accent)' : '1px solid transparent'
                    }}
                    onClick={() => loadHistory(item)}
                  >
                    <div style={{fontWeight: 600, marginBottom: '5px'}}>{item.material}</div>
                    <div style={{fontSize: '0.9rem', color: 'green'}}>Achat: {item.currentBuyPrice?.toFixed(2)} $</div>
                    {item.baseSellPrice > 0 ? (
                      <div style={{fontSize: '0.9rem', color: 'red'}}>Vente: {item.currentSellPrice?.toFixed(2)} $</div>
                    ) : (
                      <div style={{fontSize: '0.9rem', color: 'var(--text-muted)'}}>Invendable</div>
                    )}
                    {item.isCommand && (
                      <div style={{fontSize: '0.8rem', color: 'var(--accent)', marginTop: '5px', fontWeight: 'bold'}}>🌟 Grade / Commande</div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div style={{flex: 1, background: 'var(--card-bg)', border: '1px solid var(--card-border)', borderRadius: '12px', padding: '1.5rem', position: 'sticky', top: '2rem'}}>
          <h3 style={{display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '1.5rem'}}>
            <TrendingUp size={20} color="var(--accent)" /> 
            {selectedItem ? `Historique : ${selectedItem.material}` : 'Sélectionnez un objet'}
          </h3>
          
          {selectedItem ? (
            <div style={{height: '300px', width: '100%'}}>
              {history.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={history}>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                    <XAxis dataKey="time" stroke="var(--text-muted)" />
                    <YAxis stroke="var(--text-muted)" />
                    <Tooltip contentStyle={{background: '#0b0f19', border: '1px solid var(--card-border)', borderRadius: '8px'}} />
                    <Line type="monotone" dataKey="Achat" stroke="#10b981" strokeWidth={2} dot={false} />
                    <Line type="monotone" dataKey="Vente" stroke="#ef4444" strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              ) : (
                <div style={{textAlign: 'center', color: 'var(--text-muted)', paddingTop: '4rem'}}>Aucune transaction récente.</div>
              )}
            </div>
          ) : (
            <div style={{textAlign: 'center', color: 'var(--text-muted)', paddingTop: '4rem'}}>
              Cliquez sur un objet pour voir l'évolution de son prix (Inflation/Déflation).
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export function ClientAh({ isEnabled }: { isEnabled?: boolean }) {
  const [items, setItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (isEnabled === false) {
      setLoading(false);
      return;
    }
    fetch(`${API_URL}/ah/items`)
      .then(res => res.json())
      .then(data => { setItems(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, [isEnabled]);

  if (isEnabled === false) {
    return (
      <div style={{textAlign: 'center', padding: '4rem 0', color: 'var(--text-muted)'}}>
        <Package size={48} style={{opacity: 0.5, marginBottom: '1rem'}} />
        <h2>Hôtel des Ventes Désactivé</h2>
        <p>Le module d'Hôtel des Ventes est actuellement désactivé par l'administrateur.</p>
      </div>
    );
  }

  if (loading) return <div className="loading">Chargement de l'Hôtel des Ventes...</div>;

  return (
    <div>
      <div className="client-hero" style={{padding: '2rem 0'}}>
        <h2>Hôtel des Ventes (AH)</h2>
        <p>Découvrez les objets mis en vente par les joueurs en direct</p>
      </div>

      <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '1rem'}}>
        {items.map(item => (
          <div key={item.id} style={{background: 'var(--card-bg)', border: '1px solid var(--card-border)', borderRadius: '12px', padding: '1.5rem'}}>
            <div style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem'}}>
              <Package size={24} color="var(--accent)" />
              <div style={{fontWeight: 'bold', fontSize: '1.1rem'}}>Offre n°{item.id}</div>
            </div>
            <div style={{color: 'var(--text-muted)', marginBottom: '5px'}}>Vendeur : <span style={{color: '#fff'}}>{item.sellerName}</span></div>
            <div style={{color: 'var(--text-muted)', marginBottom: '15px'}}>Prix : <span style={{color: '#10b981', fontWeight: 'bold'}}>{item.price.toFixed(2)} $</span></div>
            <div style={{fontSize: '0.8rem', color: 'var(--text-muted)', borderTop: '1px solid var(--card-border)', paddingTop: '10px'}}>
              Expire dans : {Math.max(0, Math.floor((item.expireTime - Date.now()) / (1000 * 60 * 60 * 24)))} jours
            </div>
          </div>
        ))}
        {items.length === 0 && (
          <div style={{gridColumn: '1 / -1', textAlign: 'center', color: 'var(--text-muted)', padding: '3rem 0'}}>
            L'Hôtel des Ventes est actuellement vide. Connectez-vous et utilisez /ah sell pour vendre !
          </div>
        )}
      </div>
    </div>
  );
}

export function ClientMap() {
  const [mapUrl, setMapUrl] = useState("http://localhost:8100");

  useEffect(() => {
    fetch('/api/public/bluemap')
      .then(res => res.text())
      .then(url => {
        if (url) {
          if (!url.startsWith('http://') && !url.startsWith('https://')) {
            setMapUrl('http://' + url);
          } else {
            setMapUrl(url);
          }
        }
      })
      .catch(console.error);
  }, []);

  return (
    <div style={{display: 'flex', flexDirection: 'column', gap: '1rem'}}>
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
        <h2 style={{margin: 0}}>Carte Interactive</h2>
        <a href={mapUrl} target="_blank" rel="noopener noreferrer" className="btn" style={{textDecoration: 'none', padding: '10px 20px', background: 'var(--accent)', color: '#fff', borderRadius: '8px', fontWeight: 'bold'}}>
          Ouvrir la carte en plein écran
        </a>
      </div>
      <div style={{width: '100%', height: '70vh', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--card-border)'}}>
        <iframe src={mapUrl} width="100%" height="100%" frameBorder="0" title="BlueMap"></iframe>
      </div>
    </div>
  );
}

function Leaderboard({ isEnabled }: { isEnabled?: boolean }) {
  const { t } = useTranslation();
  const [leaderboard, setLeaderboard] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (isEnabled === false) {
      setLoading(false);
      return;
    }
    fetch(`${API_URL}/stats/leaderboard`)
      .then(res => res.json())
      .then(data => { setLeaderboard(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, [isEnabled]);

  if (isEnabled === false) {
    return (
      <div style={{padding: '4rem 2rem', textAlign: 'center', color: 'var(--text-muted)'}}>
        <h2>{t('web.public.leaderboard_title')}</h2>
        <p>{t('web.public.stats_disabled')}</p>
      </div>
    );
  }

  if (loading) return <div className="loading">{t('web.public.loading')}</div>;

  return (
    <div>
      <div className="client-hero" style={{padding: '2rem 0'}}>
        <h2>{t('web.public.leaderboard_title')}</h2>
        <p>{t('web.public.leaderboard_subtitle')}</p>
      </div>

      <div style={{maxWidth: '1000px', margin: '0 auto', background: 'var(--card-bg)', border: '1px solid var(--card-border)', borderRadius: '12px', padding: '1.5rem', overflowX: 'auto'}}>
        <table className="shop-table" style={{width: '100%', textAlign: 'left', minWidth: '600px'}}>
          <thead>
            <tr>
              <th style={{width: '50px', textAlign: 'center'}}>#</th>
              <th>{t('web.public.table_player')}</th>
              <th style={{textAlign: 'right'}}>{t('web.public.table_quests')}</th>
              <th style={{textAlign: 'right'}}>{t('web.public.table_blocks')}</th>
              <th style={{textAlign: 'right'}}>{t('web.public.table_mobs')}</th>
              <th style={{textAlign: 'right'}}>{t('web.public.table_playtime')}</th>
            </tr>
          </thead>
          <tbody>
            {leaderboard.map((player, index) => (
              <tr key={index}>
                <td style={{textAlign: 'center', fontWeight: 'bold', color: index === 0 ? '#fbbf24' : index === 1 ? '#94a3b8' : index === 2 ? '#b45309' : 'var(--text-muted)'}}>
                  {index + 1}
                </td>
                <td style={{display: 'flex', alignItems: 'center', gap: '15px'}}>
                  <img src={`https://mc-heads.net/avatar/${player.playerName}`} alt="skin" style={{width: '32px', height: '32px', borderRadius: '4px'}} />
                  <strong style={{fontSize: '1.1rem'}}>{player.playerName}</strong>
                </td>
                <td style={{textAlign: 'right', fontWeight: 'bold', color: '#10b981', fontSize: '1.1rem'}}>
                  {player.questsCompleted}
                </td>
                <td style={{textAlign: 'right', color: '#3b82f6'}}>
                  {player.blocksBroken}
                </td>
                <td style={{textAlign: 'right', color: '#ef4444'}}>
                  {player.mobsKilled}
                </td>
                <td style={{textAlign: 'right', color: 'var(--text-muted)'}}>
                  {(player.playtime / 60).toFixed(1)}h
                </td>
              </tr>
            ))}
            {leaderboard.length === 0 && (
              <tr>
                <td colSpan={6} style={{textAlign: 'center', padding: '2rem', color: 'var(--text-muted)'}}>
                  {t('web.public.table_empty')}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function PublicHome({ playerData }: { playerData: any }) {
  const { t } = useTranslation();
  const [featuresText, setFeaturesText] = useState('');
  const [serverIp, setServerIp] = useState('gens-core.duckdns.org');

  useEffect(() => {
    fetch(`${API_URL}/public/features`)
      .then(res => res.text())
      .then(data => setFeaturesText(data))
      .catch(console.error);
      
    fetch(`${API_URL}/public/server_ip`)
      .then(res => res.text())
      .then(data => {
        if (data) setServerIp(data);
      })
      .catch(console.error);
  }, []);

  return (
    <div style={{minHeight: '100vh', background: 'var(--bg-color)', color: 'var(--text-color)', display: 'flex', flexDirection: 'column'}}>
      <header style={{padding: '20px 40px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(9, 9, 11, 0.8)', backdropFilter: 'blur(10px)', borderBottom: '1px solid var(--card-border)', position: 'sticky', top: 0, zIndex: 100}}>
        <div style={{display: 'flex', alignItems: 'center', gap: '15px'}}>
          <Package size={32} color="var(--accent)" />
          <h1 style={{margin: 0, fontSize: '1.5rem', fontFamily: 'Outfit'}}>GensCore</h1>
        </div>
        {playerData ? (
          <Link to="/dashboard" className="btn" style={{display: 'flex', alignItems: 'center', gap: '10px', padding: '8px 20px', background: 'var(--card-bg)', border: '1px solid var(--card-border)'}}>
            <img src={`https://mc-heads.net/avatar/${playerData.username}`} alt="avatar" style={{width: '24px', height: '24px', borderRadius: '4px'}} />
            {t('web.public.profile_btn')}
          </Link>
        ) : (
          <Link to="/login" className="btn">{t('web.auth.login_btn')}</Link>
        )}
      </header>

      <main style={{flex: 1, padding: '4rem 2rem', maxWidth: '1000px', margin: '0 auto', width: '100%'}}>
        <div className="client-hero" style={{textAlign: 'center', marginBottom: '4rem', padding: '5rem 2rem', background: 'linear-gradient(180deg, rgba(99, 102, 241, 0.05) 0%, rgba(0,0,0,0) 100%)', borderRadius: '24px', border: '1px solid rgba(255,255,255,0.03)'}}>
          <h2 style={{fontSize: '4.5rem', marginBottom: '1.5rem', background: 'linear-gradient(to right, #a855f7, #3b82f6)', WebkitBackgroundClip: 'text', color: 'transparent', filter: 'drop-shadow(0 0 20px rgba(139, 92, 246, 0.3))'}}>{t('web.public.hero_title')}</h2>
          <p style={{fontSize: '1.3rem', color: 'var(--text-muted)', maxWidth: '650px', margin: '0 auto'}}>{t('web.public.hero_subtitle')}<br/><strong style={{color: '#fff', fontSize: '1.5rem', display: 'block', marginTop: '1rem', background: 'rgba(255,255,255,0.05)', padding: '10px', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.1)'}}>{t('web.public.ip')}: {serverIp}</strong></p>
        </div>

        <div className="admin-card" style={{padding: '3rem', position: 'relative', overflow: 'hidden'}}>
          <div style={{position: 'absolute', top: 0, left: 0, width: '4px', height: '100%', background: 'var(--accent)'}}></div>
          <h2 style={{marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '10px'}}>
            <FileText color="var(--accent)"/> {t('web.public.features_title')}
          </h2>
          <div style={{lineHeight: '1.8', fontSize: '1.1rem', color: 'var(--text-muted)', whiteSpace: 'pre-wrap'}}>
            {featuresText || t('web.public.loading')}
          </div>
          <div style={{marginTop: '2rem', textAlign: 'center'}}>
            <Link to="/docs" className="btn" style={{padding: '12px 30px', fontSize: '1.1rem'}}>{t('web.public.learn_more')}</Link>
          </div>
        </div>
      </main>

      <footer style={{textAlign: 'center', padding: '2rem', color: 'var(--text-muted)', borderTop: '1px solid var(--card-border)'}}>
        <p style={{marginBottom: '1rem'}}>GensCore © 2026</p>
        <Link to="/admin" style={{color: 'var(--text-muted)', textDecoration: 'underline', fontSize: '0.9rem'}}>{t('web.public.admin_access')}</Link>
      </footer>
    </div>
  );
}

// === COMPOSANT PRINCIPAL ===
function App() {
  const [adminPassword, setAdminPassword] = useState<string | null>(localStorage.getItem('gens_admin_pwd'));
  
  // Player state
  const [playerData, setPlayerData] = useState<any>(() => {
    const saved = localStorage.getItem('gens_player_data');
    return saved ? JSON.parse(saved) : null;
  });

  useEffect(() => {
    const savedAdmin = localStorage.getItem('gens_admin_pwd');
    if (savedAdmin) setAdminPassword(savedAdmin);

    const savedPlayer = localStorage.getItem('gens_player_data');
    if (savedPlayer) {
      const parsed = JSON.parse(savedPlayer);
      setPlayerData(parsed);

      // Verify OP status to avoid phantom admin state
      fetch(`${API_URL}/player/info?uuid=${parsed.uuid}`)
        .then(res => res.json())
        .then(data => {
            if (data.isOp !== parsed.isOp) {
                const updated = { ...parsed, isOp: data.isOp };
                setPlayerData(updated);
                localStorage.setItem('gens_player_data', JSON.stringify(updated));
            }
        })
        .catch(console.error);
    }
  }, []);

  const handleAdminLogin = (pwd: string) => {
    localStorage.setItem('gens_admin_pwd', pwd);
    setAdminPassword(pwd);
  };

  const handleAdminLogout = () => {
    localStorage.removeItem('gens_admin_pwd');
    setAdminPassword(null);
    window.location.href = '/';
  };

  const handlePlayerLogin = (data: any) => {
    localStorage.setItem('gens_player_data', JSON.stringify(data));
    setPlayerData(data);
  };

  const handlePlayerLogout = () => {
    localStorage.removeItem('gens_player_data');
    setPlayerData(null);
    window.location.href = '/login';
  };

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<PublicHome playerData={playerData} />} />

        {/* PUBLIC ROUTES (WALL LOGIN) */}
        <Route path="/login" element={
          playerData ? <Navigate to="/dashboard" /> : <PlayerLogin onLogin={handlePlayerLogin} />
        } />

        {/* ADMIN ROUTES */}
        <Route path="/admin/*" element={
          adminPassword ? 
            <AdminLayout password={adminPassword} onLogout={handleAdminLogout} /> : 
            <AdminLogin onLogin={handleAdminLogin} />
        } />

        {/* PLAYER PORTAL ROUTES */}
        <Route path="/dashboard/*" element={
          playerData ? 
            <PlayerDashboard playerData={playerData} onLogout={handlePlayerLogout} /> :
            <Navigate to="/login" />
        } />

        {/* LEGACY REDIRECT */}
        <Route path="/docs" element={<Docs />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App
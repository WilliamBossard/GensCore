import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import './i18n';
import { Lock, ShoppingCart, Settings, LogOut, Package, Plus, Trash2, Shield, ToggleLeft, ToggleRight, FileText, Target, Gamepad2, Users, UserX, Gavel, Mic, MicOff, MessageSquare, Menu, X, Search, TrendingUp, AlertTriangle } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { PlayerLogin, PlayerDashboard } from './PlayerPortal';
import { Docs } from './Docs';
import { emitBalanceChange, usePlayerBalance, formatBalance } from './PlayerBalanceWidget';

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
  minigameCoinflipEnabled: boolean;
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

// Retourne l'URL de l'avatar (compatible Bedrock)
// On passe par l'API du serveur qui gère les skins Bedrock via la BDD
function getPlayerAvatarUrl(name: string, size: number = 64): string {
  if (!name) return `${API_URL}/head/Steve/${size}`;
  return `${API_URL}/head/${encodeURIComponent(name)}/${size}`;
}

import { getMinecraftItemUrl, handleMinecraftImageError } from './minecraftTextures';
export { getMinecraftItemUrl, handleMinecraftImageError };

const BUKKIT_MATERIALS = [
  "DIAMOND", "DIAMOND_BLOCK", "DIAMOND_SWORD", "DIAMOND_PICKAXE", "DIAMOND_AXE", "DIAMOND_SHOVEL", "DIAMOND_HOE", 
  "DIAMOND_HELMET", "DIAMOND_CHESTPLATE", "DIAMOND_LEGGINGS", "DIAMOND_BOOTS",
  "NETHERITE_INGOT", "NETHERITE_BLOCK", "NETHERITE_SWORD", "NETHERITE_PICKAXE", "NETHERITE_AXE", "NETHERITE_SHOVEL",
  "NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS",
  "IRON_INGOT", "IRON_BLOCK", "RAW_IRON", "IRON_SWORD", "IRON_PICKAXE", "IRON_AXE", "IRON_HELMET", "IRON_CHESTPLATE",
  "GOLD_INGOT", "GOLD_BLOCK", "RAW_GOLD", "GOLDEN_APPLE", "ENCHANTED_GOLDEN_APPLE", "GOLDEN_CARROT",
  "COPPER_INGOT", "COPPER_BLOCK", "RAW_COPPER", "COAL", "COAL_BLOCK", "CHARCOAL", "EMERALD", "EMERALD_BLOCK",
  "LAPIS_LAZULI", "LAPIS_BLOCK", "REDSTONE", "REDSTONE_BLOCK", "AMETHYST_SHARD", "QUARTZ",
  "OAK_LOG", "OAK_PLANKS", "SPRUCE_LOG", "SPRUCE_PLANKS", "BIRCH_LOG", "BIRCH_PLANKS", "DARK_OAK_LOG", "MANGROVE_LOG", "CHERRY_LOG",
  "STONE", "COBBLESTONE", "STONE_BRICKS", "SMOOTH_STONE", "MOSSY_STONE_BRICKS", "DEEPSLATE", "COBBLED_DEEPSLATE", "OBSIDIAN", "CRYING_OBSIDIAN", "ANCIENT_DEBRIS",
  "WHEAT", "WHEAT_SEEDS", "HAY_BLOCK", "CARROT", "POTATO", "BAKED_POTATO", "BEETROOT", "BEETROOT_SEEDS", "MELON", "MELON_SLICE", "PUMPKIN",
  "BREAD", "BEEF", "COOKED_BEEF", "PORKCHOP", "COOKED_PORKCHOP", "CHICKEN", "COOKED_CHICKEN", "APPLE", "SWEET_BERRIES",
  "ROTTEN_FLESH", "BONE", "STRING", "SPIDER_EYE", "GUNPOWDER", "ENDER_PEARL", "BLAZE_ROD", "BLAZE_POWDER", "MAGMA_CREAM", "GHAST_TEAR",
  "SLIME_BALL", "PHANTOM_MEMBRANE", "WITHER_SKELETON_SKULL", "NETHER_STAR", "SHULKER_SHELL",
  "POTION", "SPLASH_POTION", "LINGERING_POTION", "EXPERIENCE_BOTTLE", "ENCHANTED_BOOK", "TOTEM_OF_UNDYING", "ELYTRA", "TRIDENT", "BOW", "CROSSBOW", "SHIELD",
  "BEACON", "CONDUIT", "HEART_OF_THE_SEA", "SPONGE", "SHULKER_BOX", "ENDER_CHEST", "ENCHANTING_TABLE", "ANVIL", "BREWING_STAND"
];

// === COMPOSANT : LOGIN ADMIN ===
function AdminLogin({ onLogin }: { onLogin: (pwd: string) => void }) {
  const { t } = useTranslation();
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_URL}/admin/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8' },
        body: JSON.stringify({ password })
      });
      if (res.ok) {
        const data = await res.json();
        onLogin(data.token);
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
  const [isValidating, setIsValidating] = useState(true);
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
    minigameCoinflipEnabled: true,
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
    let isMounted = true;
    fetch(`${API_URL}/admin/config`, { headers: { 'Authorization': `Bearer ${password}` } })
      .then(res => {
        if (res.status === 401) {
          localStorage.removeItem('gens_admin_pwd');
          onLogout();
          return null;
        }
        return res.ok ? res.json() : null;
      })
      .then(data => {
        if (!isMounted) return;
        if (data) setConfig(data);
        setIsValidating(false);
      })
      .catch(err => {
        if (!isMounted) return;
        console.error(err);
        setIsValidating(false);
      });
    return () => { isMounted = false; };
  }, [password]);

  const handleSaveConfig = (e: React.FormEvent) => {
    e.preventDefault();
    saveConfigToServer(config);
  };

  const saveConfigToServer = (newConfig: ConfigState) => {
    fetch(`${API_URL}/admin/config`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json; charset=utf-8', 'Authorization': `Bearer ${password}` },
      body: JSON.stringify(newConfig)
    }).then(res => {
      if (!res.ok) alert('Erreur lors de la sauvegarde de la configuration');
    });
  };

  const toggleMinigame = (game: 'wheel' | 'casino' | 'coinflip', state: boolean) => {
    const newConfig = {
      ...config,
      ...(game === 'wheel' ? { minigameWheelEnabled: state } : game === 'casino' ? { minigameCasinoEnabled: state } : { minigameCoinflipEnabled: state })
    };
    setConfig(newConfig);
    saveConfigToServer(newConfig);
  };

  if (isValidating) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        background: 'var(--bg-color)',
        gap: '16px'
      }}>
        <div className="spinner" style={{
          width: '42px',
          height: '42px',
          border: '3px solid rgba(255,255,255,0.1)',
          borderTopColor: 'var(--accent)',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite'
        }} />
        <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', fontWeight: 500 }}>
          {t('web.admin.verifying') || 'Vérification des accès administrateur...'}
        </p>
      </div>
    );
  }

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
        <div className="admin-sidebar-footer" style={{display: 'flex', flexDirection: 'column', gap: '10px'}}>
          <Link to="/dashboard" className="logout-button" style={{textDecoration: 'none'}}><Gamepad2 size={18}/> {t("web.admin.tabs.back_to_game") || "Retour au Jeu"}</Link>
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
            <h2>{activeTab === 'shop' ? t('web.admin.settings.shop_title') : activeTab === 'modules' ? t('web.admin.settings.modules_title') : activeTab === 'files' ? t('web.admin.settings.files_title') : activeTab === 'players' ? t('web.admin.settings.players_title') : activeTab === 'content' ? t('web.admin.content.title') : t('web.admin.settings.title')}</h2>
          </div>
          <div className="admin-user"><Shield size={18}/> {t('web.admin.badge')}</div>
        </header>

        {activeTab === 'shop' && <AdminShop password={password} />}

        {activeTab === 'settings' && config && (
          <form onSubmit={handleSaveConfig}>
            <div className="settings-grid">
              <div className="admin-card">
                <div className="settings-section-title"><Settings size={20} /> {t('web.admin.settings.eco_quests')}</div>
                <div className="form-group">
                  <label>{t('web.admin.settings.inflation')}</label>
                  <input type="number" step="0.01" value={config.inflationExponent} onChange={e => setConfig({...config, inflationExponent: parseFloat(e.target.value)})} required className="login-input" />
                </div>
                <div className="form-group">
                  <label>{t('web.admin.settings.ah_tax')}</label>
                  <input type="number" step="0.1" value={config.ahTaxPercentage} onChange={e => setConfig({...config, ahTaxPercentage: parseFloat(e.target.value)})} required className="login-input" />
                </div>

                <div className="form-group">
                  <label>{t('web.admin.settings.bluemap_url')}</label>
                  <input type="text" value={config.bluemapUrl || ""} onChange={e => setConfig({...config, bluemapUrl: e.target.value})} required className="login-input" />
                </div>
                <div className="form-group">
                  <label>{t('web.admin.settings.server_ip')}</label>
                  <input type="text" value={config.serverIp || ""} onChange={e => setConfig({...config, serverIp: e.target.value})} required className="login-input" />
                </div>
                <div className="form-group">
                  <label>{t('web.admin.settings.head_drop')}</label>
                  <input type="number" step="0.1" max="100" min="0" value={config.headDropChance} onChange={e => setConfig({...config, headDropChance: parseFloat(e.target.value)})} required className="login-input" />
                </div>
                <div className="form-group">
                  <label>{t('web.admin.settings.quest_rerolls')}</label>
                  <input type="number" min="0" className="login-input" value={config.maxQuestsRerolls} onChange={(e) => setConfig({...config, maxQuestsRerolls: parseInt(e.target.value) || 0})} />
                </div>
              </div>

              <div className="admin-card">
                <div className="settings-section-title"><Target size={20} /> {t('web.admin.settings.motd_title')}</div>
                <div className="form-group" style={{marginBottom: '1rem'}}>
                  <p style={{fontSize: '0.9rem', color: 'var(--text-muted)'}}>
                    {t('web.admin.settings.motd_desc')}
                  </p>
                </div>
                <div className="form-group">
                  <label>{t('web.admin.settings.motd_line1')}</label>
                  <input type="text" value={config.motdLine1 || ''} onChange={e => setConfig({...config, motdLine1: e.target.value})} className="login-input" />
                </div>
                <div className="form-group">
                  <label>{t('web.admin.settings.motd_line2')}</label>
                  <input type="text" value={config.motdLine2 || ''} onChange={e => setConfig({...config, motdLine2: e.target.value})} className="login-input" />
                </div>
              </div>

              <div className="admin-card">
                <div className="settings-section-title"><Package size={20} /> {t('web.admin.settings.lootr_title')}</div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.lootrPreventBreak} onChange={e => setConfig({...config, lootrPreventBreak: e.target.checked})} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>{t('web.admin.settings.lootr_break')}</span>
                </div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.lootrPreventHopper} onChange={e => setConfig({...config, lootrPreventHopper: e.target.checked})} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>{t('web.admin.settings.lootr_hopper')}</span>
                </div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.lootrParticles} onChange={e => setConfig({...config, lootrParticles: e.target.checked})} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>{t('web.admin.settings.lootr_particles')}</span>
                </div>
              </div>

              <div className="admin-card">
                <div className="settings-section-title"><Settings size={20} /> {t('web.admin.settings.tomb_title')}</div>
                <div className="form-group">
                  <label>{t('web.admin.settings.tomb_block')}</label>
                  <input type="text" value={config.tombBlockType || 'CHEST'} onChange={e => setConfig({...config, tombBlockType: e.target.value})} className="login-input" />
                </div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.tombStoreXp} onChange={e => setConfig({...config, tombStoreXp: e.target.checked})} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>{t('web.admin.settings.tomb_xp')}</span>
                </div>
                <div className="form-group">
                  <label>{t('web.admin.settings.tomb_expire')}</label>
                  <input type="number" min="0" value={config.tombExpirationSeconds || 3600} onChange={e => setConfig({...config, tombExpirationSeconds: parseInt(e.target.value) || 0})} className="login-input" />
                </div>
                <div className="form-group">
                  <label>{t('web.admin.settings.tomb_action')}</label>
                  <select value={config.tombExpirationAction || 'UNLOCK'} onChange={e => setConfig({...config, tombExpirationAction: e.target.value})} className="login-input">
                    <option value="UNLOCK">{t('web.admin.settings.tomb_action_unlock')}</option>
                    <option value="DROP">{t('web.admin.settings.tomb_action_drop')}</option>
                    <option value="DESTROY">{t('web.admin.settings.tomb_action_destroy')}</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>{t('web.admin.settings.tomb_access')}</label>
                  <select value={config.tombDefaultAccess || 'OWNER_ONLY'} onChange={e => setConfig({...config, tombDefaultAccess: e.target.value})} className="login-input">
                    <option value="OWNER_ONLY">{t('web.admin.settings.tomb_access_owner')}</option>
                    <option value="EVERYONE">{t('web.admin.settings.tomb_access_everyone')}</option>
                  </select>
                </div>
              </div>

              <div className="admin-card">
                <div className="settings-section-title"><Gamepad2 size={20} /> {t('web.admin.settings.games_title')}</div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.minigameWheelEnabled !== false} onChange={e => toggleMinigame('wheel', e.target.checked)} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>{t('web.admin.settings.games_wheel')}</span>
                </div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.minigameCasinoEnabled !== false} onChange={e => toggleMinigame('casino', e.target.checked)} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>{t('web.admin.settings.games_casino')}</span>
                </div>
                <div className="form-group" style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1rem', flexDirection: 'row'}}>
                  <label className="switch">
                    <input type="checkbox" checked={config.minigameCoinflipEnabled !== false} onChange={e => toggleMinigame('coinflip', e.target.checked)} />
                    <span className="slider"></span>
                  </label>
                  <span style={{fontWeight: 500}}>{t('web.admin.settings.games_coinflip') || 'Pile ou Face (Web)'}</span>
                </div>
              </div>

              <div className="admin-card">
                <div className="settings-section-title"><Shield size={20} /> {t('web.admin.settings.security_title')}</div>
                <div className="form-group">
                  <label>{t('web.admin.settings.security_pass')}</label>
                  <input type="text" placeholder={t('web.admin.settings.security_pass_placeholder')} value={config.adminPassword || ''} onChange={e => setConfig({...config, adminPassword: e.target.value})} className="login-input" />
                </div>
                <div style={{marginTop: '2rem'}}>
                  <button type="submit" className="login-button" style={{display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '10px'}}>
                    <Settings size={18} /> {t('web.admin.settings.save')}
                  </button>
                </div>
              </div>
              <div className="admin-card">
                <div className="settings-section-title"><Trash2 size={20} /> {t('web.admin.settings.data_title')}</div>
                <div className="form-group" style={{ marginBottom: '0' }}>
                  <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                    <b>ATTENTION :</b> Ce bouton va formater la base de données (tous les stats, homes, inventaires, teams). Seuls le shop et les paramètres yml seront conservés. 
                  </p>
                  <button 
                    type="button" 
                    className="btn-danger-solid" 
                    style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '10px', width: '100%', maxWidth: '340px' }}
                    onClick={async () => {
                      const confirmPassword = prompt("Cette action est irréversible et supprimera toutes les données joueurs. Tapez le mot de passe admin pour confirmer :");
                      if (confirmPassword === password) {
                        const res = await fetch(`${API_URL}/admin/wipe-server`, { method: 'POST', headers: { 'Authorization': `Bearer ${password}` } });
                        if (res.ok) alert("Wipe terminé avec succès ! Le serveur est en train de s'arrêter. Supprimez le dossier de la map puis redémarrez le serveur.");
                        else alert("Erreur lors du wipe.");
                      } else if (confirmPassword !== null) {
                        alert("Mot de passe incorrect, wipe annulé.");
                      }
                    }}
                  >
                    <Trash2 size={18} /> Wipe Serveur (Nouvelle Saison)
                  </button>
                </div>
              </div>
            </div>
          </form>
        )}
        {activeTab === 'content' && (
          <form onSubmit={handleSaveConfig}>
            <div className="admin-card">
              <div className="settings-section-title"><FileText size={20} /> {t('web.admin.content.announcements')}</div>
              <p style={{color: 'var(--text-muted)', marginBottom: '1.5rem'}}>{t('web.admin.content.desc')}</p>
              <div className="form-group">
                <textarea 
                  value={config.publicFeaturesText || ''} 
                  onChange={e => setConfig({...config, publicFeaturesText: e.target.value})} 
                  className="input-field" 
                  style={{minHeight: '300px', resize: 'vertical', fontFamily: 'monospace', lineHeight: '1.5'}} 
                  placeholder={t('web.admin.content.placeholder')}
                />
              </div>
              <div style={{marginTop: '2rem'}}>
                <button type="submit" className="login-button" style={{display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '10px'}}>
                  <Settings size={18} /> {t('web.admin.content.save')}
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
      .then(res => {
        if (res.status === 401) {
          localStorage.removeItem('gens_admin_pwd');
          window.location.reload();
          return null;
        }
        return res.ok ? res.json() : null;
      })
      .then(data => { if (data) setPlayers(data); setLoading(false); })
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
      headers: { 'Content-Type': 'application/json; charset=utf-8', 'Authorization': `Bearer ${password}` },
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

  const { t } = useTranslation();
  if (loading) return <div className="loading">{t('web.admin.players.loading')}</div>;

  return (
    <div>
      <h2 style={{marginBottom: '2rem'}}>{t('web.admin.players.title')} ({players.length})</h2>
      
      <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 350px), 1fr))', gap: '1.5rem'}}>
        {players.map(p => (
          <div key={p.uuid} className="admin-card" style={{padding: '1.5rem'}}>
            <div style={{display: 'flex', alignItems: 'center', gap: '15px', marginBottom: '1.5rem'}}>
              <div style={{position: 'relative'}}>
                <img src={getPlayerAvatarUrl(p.name)} alt={p.name} style={{width: '48px', height: '48px', borderRadius: '8px'}} />
                <div style={{
                  position: 'absolute', bottom: '-4px', right: '-4px', width: '14px', height: '14px', borderRadius: '50%',
                  background: p.online ? 'var(--success)' : 'var(--text-muted)', border: '2px solid var(--card-bg)'
                }}></div>
              </div>
              <div>
                <h3 style={{margin: 0, fontSize: '1.2rem'}}>{p.name}</h3>
                <div style={{fontSize: '0.9rem', color: 'var(--text-muted)'}}>
                  {p.online ? `Ping: ${p.ping}ms • ` : `${t('web.admin.players.offline')} • `} 
                  {t('web.admin.players.playtime')}: {Math.floor(p.playtime/60)}h
                </div>
              </div>
            </div>
            
            <div style={{display: 'flex', gap: '10px', flexWrap: 'wrap'}}>
              {p.online && (
                <button className="btn-small" style={{flex: 1, background: 'var(--bg-color)', color: '#ef4444', border: '1px solid #ef4444'}} onClick={() => setActionModal({player: p.name, action: 'kick'})}>
                  <UserX size={16}/> {t('web.admin.players.kick')}
                </button>
              )}
              {p.isBanned ? (
                <button className="btn-small" style={{flex: 1, background: 'var(--success)', color: 'white', border: 'none'}} onClick={() => setActionModal({player: p.name, action: 'unban'})}>
                  <Gavel size={16}/> {t('web.admin.players.unban')}
                </button>
              ) : (
                <button className="btn-small" style={{flex: 1, background: '#ef4444', color: 'white', border: 'none'}} onClick={() => setActionModal({player: p.name, action: 'ban'})}>
                  <Gavel size={16}/> {t('web.admin.players.ban')}
                </button>
              )}
              {p.isMuted ? (
                <button className="btn-small" style={{flex: 1, background: 'var(--success)', color: 'white', border: 'none'}} onClick={() => setActionModal({player: p.name, action: 'unmute'})}>
                  <Mic size={16}/> {t('web.admin.players.unmute')}
                </button>
              ) : (
                <button className="btn-small" style={{flex: 1, background: 'var(--bg-color)', color: '#f59e0b', border: '1px solid #f59e0b'}} onClick={() => setActionModal({player: p.name, action: 'mute'})}>
                  <MicOff size={16}/> {t('web.admin.players.mute')}
                </button>
              )}
              {p.online && (
                <button className="btn-small" style={{flex: 1, background: 'var(--bg-color)', color: '#3b82f6', border: '1px solid #3b82f6'}} onClick={() => setActionModal({player: p.name, action: 'message'})}>
                  <MessageSquare size={16}/> {t('web.admin.players.msg')}
                </button>
              )}
            </div>
          </div>
        ))}
        {players.length === 0 && <div style={{color: 'var(--text-muted)'}}>{t('web.admin.players.empty')}</div>}
      </div>

      {actionModal && (
        <div className="modal-overlay">
          <div className="modal-content admin-card">
            <h3 style={{marginBottom: '1rem', textTransform: 'capitalize'}}>{t('web.admin.players.action')}: {actionModal.action} ({actionModal.player})</h3>
            
            {['ban', 'mute', 'kick', 'message'].includes(actionModal.action) && (
              <div className="form-group">
                <label>{actionModal.action === 'message' ? t('web.admin.players.message') : t('web.admin.players.reason')}</label>
                <input type="text" value={reason} onChange={e => setReason(e.target.value)} placeholder={t('web.admin.players.reason_placeholder')} className="input-field" />
              </div>
            )}

            {(actionModal.action === 'ban' || actionModal.action === 'mute') && (
              <div style={{display: 'flex', gap: '10px'}}>
                <div className="form-group" style={{flex: 2}}>
                  <label>{t('web.admin.players.duration')}</label>
                  <input type="number" value={duration} onChange={e => setDuration(parseFloat(e.target.value) || 0)} min="0" className="input-field" />
                </div>
                <div className="form-group" style={{flex: 1}}>
                  <label>{t('web.admin.players.unit')}</label>
                  <select value={durationType} onChange={e => setDurationType(e.target.value)} className="input-field">
                    <option value="hours">{t('web.admin.players.hours')}</option>
                    <option value="days">{t('web.admin.players.days')}</option>
                  </select>
                </div>
              </div>
            )}

            <div style={{display: 'flex', gap: '10px', marginTop: '20px'}}>
              <button className="btn" onClick={handleAction}>{t('web.admin.players.confirm')}</button>
              <button className="btn" style={{background: 'transparent', border: '1px solid var(--card-border)'}} onClick={() => setActionModal(null)}>{t('web.admin.players.cancel')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function AdminFiles({ password }: { password: string }) {
  const { t } = useTranslation();
  const [content, setContent] = useState('');
  const [currentFile, setCurrentFile] = useState('config.yml');
  const [loading, setLoading] = useState(true);
  const [saved, setSaved] = useState(false);

  const fileCategories = [
    { name: t('web.admin.files.cat_main'), files: ["config.yml", "modules.yml"] },
    { name: t('web.admin.files.cat_modules'), files: [
      "modules/web.yml", "modules/economy.yml", "modules/discord.yml", 
      "modules/motd.yml", "modules/lootr.yml", "modules/quests.yml", 
      "modules/spawners.yml", "modules/headdrop.yml", "modules/tabboard.yml",
      "modules/teleport.yml", "modules/tomb.yml", "modules/minigames.yml", "modules/bluemap.yml", "modules/teams.yml", "modules/chat.yml"
    ]},
    { name: t('web.admin.files.cat_langs'), files: ["lang/web_en_US.yml", "lang/web_fr_FR.yml"] }
  ];

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
      headers: { 'Content-Type': 'application/json; charset=utf-8', 'Authorization': `Bearer ${password}` },
      body: JSON.stringify({ content })
    }).then(() => {
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    });
  };

  const lineCount = content.split('\n').length;
  const lines = Array.from({ length: Math.max(10, lineCount) }, (_, i) => i + 1);

  return (
    <div style={{display: 'flex', gap: '20px', height: '100%'}} className="file-editor-container">
      {/* Sidebar Fichiers */}
      <div className="admin-card" style={{width: '280px', padding: '1.2rem', display: 'flex', flexDirection: 'column', gap: '1rem', overflowY: 'auto', maxHeight: 'calc(100vh - 120px)'}}>
        <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px'}}>
          <h3 style={{margin: 0, fontSize: '1.1rem'}}>{t('web.admin.files.sidebar_title')}</h3>
          <button className="btn-icon" onClick={() => fetchFile(currentFile)} title={t('web.admin.files.refresh')}>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{color: 'var(--text-main)'}}><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/></svg>
          </button>
        </div>
        
        {fileCategories.map(cat => (
          <div key={cat.name} style={{marginBottom: '0.5rem'}}>
            <div style={{fontSize: '0.8rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: 600, letterSpacing: '1px', marginBottom: '8px'}}>{cat.name}</div>
            <div style={{display: 'flex', flexDirection: 'column', gap: '4px'}}>
              {cat.files.map(f => (
                <button 
                  key={f} 
                  onClick={() => setCurrentFile(f)}
                  style={{
                    background: currentFile === f ? 'var(--accent-glow)' : 'transparent',
                    color: currentFile === f ? 'var(--text-main)' : 'var(--text-muted)',
                    border: 'none',
                    textAlign: 'left',
                    padding: '8px 12px',
                    borderRadius: '6px',
                    cursor: 'pointer',
                    fontSize: '0.9rem',
                    fontFamily: 'monospace',
                    transition: 'all 0.2s',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px'
                  }}
                  onMouseOver={(e) => { if (currentFile !== f) e.currentTarget.style.background = 'rgba(255,255,255,0.05)' }}
                  onMouseOut={(e) => { if (currentFile !== f) e.currentTarget.style.background = 'transparent' }}
                >
                  <FileText size={14} style={{color: currentFile === f ? 'var(--accent)' : 'var(--text-muted)'}} />
                  {f.replace('modules/', '')}
                </button>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Editeur Principal */}
      <div style={{flex: 1, display: 'flex', flexDirection: 'column', gap: '1rem'}}>
        <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--card-bg)', padding: '15px 20px', borderRadius: '12px', border: '1px solid var(--card-border)'}}>
          <div style={{display: 'flex', gap: '10px', alignItems: 'center'}}>
            <FileText size={20} color="var(--accent)"/>
            <h2 style={{margin: 0, fontSize: '1.2rem', fontFamily: 'monospace'}}>{currentFile}</h2>
          </div>
          
          <button className="btn" onClick={saveFile} style={{background: saved ? 'var(--success)' : 'var(--accent)', padding: '8px 16px', display: 'flex', alignItems: 'center', gap: '8px'}}>
            {saved ? <Shield size={16}/> : <FileText size={16}/>}
            {saved ? t('web.admin.files.saved') : t('web.admin.files.save')}
          </button>
        </div>
      
      {loading ? <div className="loading">{t('web.public.loading')}</div> : (
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
    </div>
  );
}

// === COMPOSANT : GESTION DES MODULES ===
function AdminModules({ password }: { password: string }) {
  const { t } = useTranslation();
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
      headers: { 'Content-Type': 'application/json; charset=utf-8', 'Authorization': `Bearer ${password}` },
      body: JSON.stringify({ state: !currentState })
    });
  };

  const getCategory = (name: string) => {
    const n = name.toLowerCase();
    if (['economy', 'shop', 'dynamicshop', 'auctionhouse', 'jobs'].includes(n)) return t('web.admin.modules_cat.economy') || 'Économie & Commerce';
    if (['quests', 'stats', 'spawners', 'loot', 'headdrop', 'minigame', 'minigames'].includes(n)) return t('web.admin.modules_cat.gameplay') || 'Joueurs & Gameplay';
    if (['motd', 'tabboard', 'discord', 'gui', 'web', 'bedrockskin'].includes(n)) return t('web.admin.modules_cat.interface') || 'Interface & Communication';
    return t('web.admin.modules_cat.admin') || 'Administration & Utilitaires';
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
              <button className="btn-small" style={{background: '#10b981', color: 'white'}} onClick={() => toggleCategory(mods, true)}>{t('web.admin.modules_cat.enable_all') || 'Tout Activer'}</button>
              <button className="btn-small" style={{background: '#ef4444', color: 'white'}} onClick={() => toggleCategory(mods, false)}>{t('web.admin.modules_cat.disable_all') || 'Tout Désactiver'}</button>
            </div>
          </div>
          <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 300px), 1fr))', gap: '1.5rem'}}>
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
                <p style={{color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: '1.4'}}>
                  {t(`web.admin.modules_desc.${mod.name.toLowerCase()}`, { defaultValue: mod.description })}
                </p>
                <div style={{marginTop: 'auto', paddingTop: '10px', borderTop: '1px solid var(--card-border)', fontSize: '0.8rem'}}>
                  Statut : <strong style={{color: mod.enabled ? '#10b981' : '#ef4444'}}>{mod.enabled ? t('web.admin.active') : t('web.admin.inactive')}</strong>
                </div>
              </div>
            ))}
          </div>
        </div>
      )})}
    </div>
  );
}

// === COMPOSANT : GESTION DE LA BOUTIQUE (ADMIN STUDIO) ===
function AdminShop({ password }: { password: string }) {
  const { t } = useTranslation();
  const [categories, setCategories] = useState<ShopCategory[]>([]);
  const [activeCatId, setActiveCatId] = useState<string>('');
  const [selectedMaterial, setSelectedMaterial] = useState<string>('');
  const [inspectorMode, setInspectorMode] = useState<'edit' | 'create'>('edit');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [isEcoEnabled, setIsEcoEnabled] = useState(true);
  const [statusMsg, setStatusMsg] = useState<{ text: string; type: 'success' | 'error' | 'info' } | null>(null);

  // Formulaire d'ajout / édition
  const [formMat, setFormMat] = useState('DIAMOND');
  const [formBuy, setFormBuy] = useState(10.0);
  const [formSell, setFormSell] = useState(3.0);
  const [formTargetStock, setFormTargetStock] = useState(1000);
  const [formCurrentStock, setFormCurrentStock] = useState(1000);
  const [formIsEnabled, setFormIsEnabled] = useState(true);
  const [formIsCommand, setFormIsCommand] = useState(false);
  const [formCommand, setFormCommand] = useState('');
  const [autocompleteResults, setAutocompleteResults] = useState<string[]>([]);
  const [showCatModal, setShowCatModal] = useState(false);
  const [newCatId, setNewCatId] = useState('');
  const [newCatName, setNewCatName] = useState('');
  const [newCatIcon, setNewCatIcon] = useState('DIAMOND');

  const showStatus = (text: string, type: 'success' | 'error' | 'info' = 'info') => {
    setStatusMsg({ text, type });
    setTimeout(() => setStatusMsg(null), 3500);
  };

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
          .then(data => {
            const list: ShopCategory[] = data || [];
            setCategories(list);
            if (list.length > 0) {
              const currentCat = list.find(c => c.id === activeCatId) || list[0];
              setActiveCatId(currentCat.id);
              if (currentCat.items && currentCat.items.length > 0) {
                const currentItem = currentCat.items.find(i => i.material === selectedMaterial) || currentCat.items[0];
                loadItemToForm(currentItem);
              }
            }
            setLoading(false);
          })
          .catch(() => { setCategories([]); setLoading(false); });
      }).catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchShop();
  }, []);

  const loadItemToForm = (item: ShopItem) => {
    setSelectedMaterial(item.material);
    setFormMat(item.material);
    setFormBuy(item.baseBuyPrice);
    setFormSell(item.baseSellPrice);
    setFormTargetStock(item.targetStock || 1000);
    setFormCurrentStock(item.stock || 0);
    setFormIsEnabled(item.isEnabled !== false);
    setFormIsCommand(item.isCommand || false);
    setFormCommand(item.commandToExecute || '');
    setInspectorMode('edit');
  };

  const handleAuthError = () => {
    alert('Votre session administrateur a expiré ou est invalide. Veuillez vous reconnecter.');
    localStorage.removeItem('gens_admin_pwd');
    window.location.reload();
  };

  const saveItemForm = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    try {
      const res = await fetch(`${API_URL}/admin/shop/item`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8', 'Authorization': `Bearer ${password}` },
        body: JSON.stringify({
          categoryId: activeCatId,
          material: formMat.trim().toUpperCase(),
          baseBuyPrice: formBuy,
          baseSellPrice: formSell,
          targetStock: formTargetStock,
          isCommand: formIsCommand,
          commandToExecute: formCommand,
          isEnabled: formIsEnabled
        })
      });
      if (!res.ok) {
        if (res.status === 401) return handleAuthError();
        const err = await res.json().catch(() => ({}));
        showStatus(err.error || "Erreur lors de l'enregistrement de l'objet", 'error');
        return;
      }
      showStatus(`Objet ${formMat} enregistre avec succes !`, 'success');
      fetchShop();
    } catch (err) {
      showStatus('Erreur de connexion avec le serveur', 'error');
    }
  };

  const deleteItem = async (categoryId: string, material: string) => {
    if (!confirm(`Supprimer l'objet ${material} ?`)) return;
    try {
      const res = await fetch(`${API_URL}/admin/shop/item/${categoryId}/${material}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${password}` }
      });
      if (!res.ok) {
        if (res.status === 401) return handleAuthError();
        const err = await res.json().catch(() => ({}));
        showStatus(err.error || 'Erreur lors de la suppression', 'error');
        return;
      }
      showStatus(`Objet ${material} supprime avec succes`, 'info');
      fetchShop();
    } catch (err) {
      showStatus('Erreur de communication avec le serveur', 'error');
    }
  };

  const deleteCategory = async (categoryId: string) => {
    if (!confirm('Supprimer cette categorie et tous ses objets ?')) return;
    try {
      const res = await fetch(`${API_URL}/admin/shop/category/${categoryId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${password}` }
      });
      if (!res.ok) {
        if (res.status === 401) return handleAuthError();
        const err = await res.json().catch(() => ({}));
        showStatus(err.error || 'Erreur lors de la suppression', 'error');
        return;
      }
      showStatus('Categorie supprimee', 'info');
      fetchShop();
    } catch (err) {
      showStatus('Erreur de communication avec le serveur', 'error');
    }
  };

  const submitCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_URL}/admin/shop/category`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8', 'Authorization': `Bearer ${password}` },
        body: JSON.stringify({ id: newCatId.trim().toLowerCase(), displayName: newCatName.trim(), icon: newCatIcon.trim().toUpperCase(), items: [] })
      });
      if (!res.ok) {
        if (res.status === 401) return handleAuthError();
        const err = await res.json().catch(() => ({}));
        showStatus(err.error || "Erreur lors de l'ajout de la categorie", 'error');
        return;
      }
      setShowCatModal(false);
      setNewCatId('');
      setNewCatName('');
      showStatus(`Categorie ${newCatName} creee avec succes !`, 'success');
      fetchShop();
    } catch (err) {
      showStatus('Erreur de connexion avec le serveur', 'error');
    }
  };


  const handleAutocomplete = (text: string) => {
    setFormMat(text);
    if (!text.trim()) {
      setAutocompleteResults([]);
      return;
    }
    const q = text.toUpperCase();
    const matches = BUKKIT_MATERIALS.filter(m => m.includes(q)).slice(0, 8);
    setAutocompleteResults(matches);
  };

  const selectBukkit = (mat: string) => {
    setFormMat(mat);
    setAutocompleteResults([]);
  };

  if (loading) return <div className="loading">Chargement...</div>;

  if (!isEcoEnabled) {
    return (
      <div className="admin-card" style={{padding: '3rem', textAlign: 'center'}}>
        <div style={{color: 'var(--text-muted)', marginBottom: '1rem'}}>
          <ShoppingCart size={64} style={{opacity: 0.5}} />
        </div>
        <h2>{t('web.public.shop.disabled_title')}</h2>
        <p style={{color: 'var(--text-muted)'}}>Le module Economy ou DynamicShop est actuellement desactive.</p>
      </div>
    );
  }

  const activeCat = categories.find(c => c.id === activeCatId) || categories[0];
  let filteredItems = activeCat ? activeCat.items || [] : [];
  if (searchQuery.trim()) {
    const q = searchQuery.toLowerCase();
    filteredItems = filteredItems.filter(i => i.material.toLowerCase().includes(q));
  }

  const calculatedMargin = formBuy > 0 ? (((formBuy - formSell) / formBuy) * 100).toFixed(1) : '0.0';
  const calculatedRatio = formSell > 0 ? (formBuy / formSell).toFixed(1) + 'x' : 'Inf';

  return (
    <div>
      {statusMsg && (
        <div className={`notification ${statusMsg.type}`} style={{position: 'fixed', top: '20px', right: '20px', zIndex: 1000}}>
          {statusMsg.text}
        </div>
      )}

      {/* MODAL CATEGORY */}
      {showCatModal && (
        <div className="modal-overlay">
          <div className="modal-card">
            <h2>Nouvelle Categorie</h2>
            <form onSubmit={submitCategory}>
              <div className="form-group">
                <label>Identifiant (minuscules, sans espace)</label>
                <input required value={newCatId} onChange={e => setNewCatId(e.target.value)} className="login-input" placeholder="ex: nether" />
              </div>
              <div className="form-group">
                <label>Nom d'affichage</label>
                <input required value={newCatName} onChange={e => setNewCatName(e.target.value)} className="login-input" placeholder="ex: Ressources du Nether" />
              </div>
              <div className="form-group">
                <label>Icone (Materiau Minecraft)</label>
                <input required value={newCatIcon} onChange={e => setNewCatIcon(e.target.value)} className="login-input" placeholder="ex: NETHERRACK" />
              </div>
              <div style={{display: 'flex', gap: '10px', marginTop: '20px'}}>
                <button type="submit" className="login-button">Enregistrer</button>
                <button type="button" className="login-button" style={{background: 'var(--card-bg)'}} onClick={() => setShowCatModal(false)}>Annuler</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="admin-studio-grid">
        {/* COLONNE 1 : LISTE DES CATEGORIES */}
        <div className="admin-panel-card">
          <div className="admin-panel-header">
            <span style={{fontWeight: 700, fontSize: '0.95rem'}}>Categories ({categories.length})</span>
            <button className="multiplier-btn" style={{padding: '4px 8px', fontSize: '0.75rem'}} onClick={() => { setNewCatId(''); setNewCatName(''); setNewCatIcon('DIAMOND'); setShowCatModal(true); }}>
              <Plus size={14}/> Creer
            </button>
          </div>
          <div style={{padding: '10px', display: 'flex', flexDirection: 'column', gap: '6px', maxHeight: '680px', overflowY: 'auto'}}>
            {categories.map(c => (
              <div 
                key={c.id} 
                className={`admin-cat-row ${c.id === activeCat?.id ? 'active' : ''}`}
                onClick={() => {
                  setActiveCatId(c.id);
                  if (c.items && c.items.length > 0) loadItemToForm(c.items[0]);
                }}
              >
                <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                  <div className="mc-item-icon" style={{width: '24px', height: '24px'}}>
                    <img src={getMinecraftItemUrl(c.icon || 'CHEST')} alt={c.id} loading="lazy" decoding="async" onError={handleMinecraftImageError} />
                  </div>
                  <span style={{fontWeight: 600, fontSize: '0.88rem'}}>{c.displayName}</span>
                </div>
                <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                  <span style={{fontSize: '0.75rem', color: 'var(--text-muted)'}}>{c.items?.length || 0}</span>
                  <button 
                    className="btn-trash" 
                    onClick={(e) => { e.stopPropagation(); deleteCategory(c.id); }}
                    title="Supprimer la catégorie"
                    aria-label="Supprimer la catégorie"
                  >
                    <Trash2 size={15}/>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* COLONNE 2 : GRILLE D'ITEMS */}
        <div className="admin-panel-card">
          <div className="admin-panel-header" style={{flexWrap: 'wrap', gap: '10px'}}>
            <div>
              <span style={{fontWeight: 700, fontSize: '1rem'}}>Objets : {activeCat?.displayName} ({filteredItems.length})</span>
              <div style={{fontSize: '0.72rem', color: 'var(--text-muted)'}}>Cliquez sur un objet pour afficher et modifier sa fiche complete</div>
            </div>
            <div style={{display: 'flex', gap: '6px'}}>
              <button 
                className="multiplier-btn" 
                style={{padding: '6px 12px', fontSize: '0.8rem', background: 'var(--accent)', color: 'white', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px'}}
                onClick={() => {
                  setInspectorMode('create');
                  setFormMat('DIAMOND');
                  setFormBuy(10.0);
                  setFormSell(3.0);
                  setFormTargetStock(1000);
                  setFormCurrentStock(1000);
                  setFormIsEnabled(true);
                  setFormIsCommand(false);
                  setFormCommand('');
                }}
              >
                <Plus size={15}/> Ajouter un objet
              </button>
            </div>
          </div>

          <div style={{padding: '12px 16px', borderBottom: '1px solid var(--card-border)', background: 'rgba(0,0,0,0.2)'}}>
            <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
              <Search size={16} color="var(--text-muted)"/>
              <input 
                type="text" 
                placeholder="Rechercher dans cette categorie..." 
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                style={{background: 'transparent', border: 'none', color: 'white', width: '100%', outline: 'none', fontSize: '0.88rem'}}
              />
            </div>
          </div>

          <div style={{padding: '16px', display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '620px', overflowY: 'auto'}}>
            {filteredItems.map(item => {
              const isSel = selectedMaterial === item.material && inspectorMode === 'edit';
              const isEnabled = item.isEnabled !== false;
              return (
                <div 
                  key={item.material}
                  className={`admin-item-row ${isSel ? 'selected' : ''}`}
                  onClick={() => loadItemToForm(item)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    background: isSel ? 'rgba(59, 130, 246, 0.15)' : 'rgba(255,255,255,0.02)',
                    border: isSel ? '1px solid var(--accent)' : '1px solid var(--card-border)',
                    transition: 'all 0.15s ease'
                  }}
                >
                  <div style={{display: 'flex', alignItems: 'center', gap: '12px'}}>
                    <div className="mc-slot-box" style={{width: '38px', height: '38px'}}>
                      <div className="mc-item-icon" style={{width: '28px', height: '28px'}}>
                        <img src={getMinecraftItemUrl(item.material)} alt={item.material} loading="lazy" decoding="async" onError={handleMinecraftImageError} />
                      </div>
                    </div>
                    <div>
                      <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                        <span style={{fontWeight: 700, fontSize: '0.9rem', color: 'white'}}>{item.material.replace(/_/g, ' ')}</span>
                        <span style={{fontSize: '0.65rem', fontWeight: 700, padding: '1px 6px', borderRadius: '3px', background: isEnabled ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)', color: isEnabled ? '#10b981' : '#ef4444'}}>
                          {isEnabled ? 'ACTIF' : 'MASQUE'}
                        </span>
                      </div>
                      <div style={{fontSize: '0.72rem', color: 'var(--text-muted)'}}>Stock : {item.stock} / {item.targetStock}</div>
                    </div>
                  </div>

                  <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                    <span style={{fontSize: '0.8rem', padding: '4px 8px', borderRadius: '4px', background: 'rgba(59, 130, 246, 0.1)', color: 'var(--accent)', fontWeight: 600}}>
                      Achat: {item.baseBuyPrice.toFixed(1)} $
                    </span>
                    <span style={{fontSize: '0.8rem', padding: '4px 8px', borderRadius: '4px', background: 'rgba(16, 185, 129, 0.1)', color: '#10b981', fontWeight: 600}}>
                      Vente: {item.baseSellPrice.toFixed(1)} $
                    </span>
                  </div>
                </div>
              );
            })}
            {filteredItems.length === 0 && (
              <div style={{textAlign: 'center', padding: '40px', color: 'var(--text-muted)'}}>
                Aucun objet trouve. Cliquez sur "+ Ajouter un objet" pour en creer un.
              </div>
            )}
          </div>
        </div>

        {/* COLONNE 3 : PANNEAU D'INSPECTION & ÉDITION */}
        <div className="admin-panel-card">
          <div className="admin-panel-header">
            <div>
              <span style={{fontWeight: 700, fontSize: '0.95rem'}}>
                {inspectorMode === 'edit' ? `Fiche : ${formMat}` : 'Ajouter un Nouvel Objet'}
              </span>
              <div style={{fontSize: '0.72rem', color: 'var(--accent)'}}>
                {inspectorMode === 'edit' ? 'Modification de la fiche complete' : 'Catalogue Minecraft Bukkit'}
              </div>
            </div>
            {inspectorMode === 'create' && (
              <button className="multiplier-btn" style={{padding: '4px 10px', fontSize: '0.75rem'}} onClick={() => setInspectorMode('edit')}>
                Annuler
              </button>
            )}
          </div>

          <form onSubmit={saveItemForm} style={{padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px'}}>
            {inspectorMode === 'create' ? (
              <div className="autocomplete-container">
                <label className="form-label">Materiau Minecraft Bukkit</label>
                <input 
                  type="text" 
                  value={formMat} 
                  onChange={e => handleAutocomplete(e.target.value)} 
                  className="input-field" 
                  placeholder="Tapez ex: DIAMOND, NETHER..." 
                  required 
                />
                {autocompleteResults.length > 0 && (
                  <div className="autocomplete-dropdown">
                    {autocompleteResults.map(mat => (
                      <div key={mat} className="autocomplete-item" onClick={() => selectBukkit(mat)}>
                        <div className="mc-item-icon" style={{width: '20px', height: '20px'}}>
                          <img src={getMinecraftItemUrl(mat)} alt={mat} onError={handleMinecraftImageError} />
                        </div>
                        <span>{mat}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <div style={{display: 'flex', alignItems: 'center', gap: '14px', background: 'rgba(0,0,0,0.3)', padding: '14px', borderRadius: '10px', border: '1px solid var(--card-border)'}}>
                <div className="mc-slot-box" style={{width: '54px', height: '54px'}}>
                  <div className="mc-item-icon" style={{width: '40px', height: '40px'}}>
                    <img src={getMinecraftItemUrl(formMat)} alt={formMat} loading="lazy" decoding="async" onError={handleMinecraftImageError} />
                  </div>
                </div>
                <div>
                  <div style={{fontWeight: 700, fontSize: '1.1rem', color: 'white'}}>{formMat.replace(/_/g, ' ')}</div>
                  <div style={{fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'monospace'}}>{formMat}</div>
                  <span style={{display: 'inline-block', marginTop: '4px', fontSize: '0.7rem', fontWeight: 700, padding: '2px 6px', borderRadius: '4px', background: formIsEnabled ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)', color: formIsEnabled ? '#10b981' : '#ef4444'}}>
                    {formIsEnabled ? 'EN VENTE (ACTIF)' : 'DESACTIVE (MASQUE)'}
                  </span>
                </div>
              </div>
            )}

            <div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: 'rgba(255,255,255,0.02)', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--card-border)'}}>
              <div>
                <div style={{fontWeight: 600, fontSize: '0.85rem', color: 'white'}}>Actif dans la Boutique</div>
                <div style={{fontSize: '0.72rem', color: 'var(--text-muted)'}}>Visible par les joueurs pour achat/vente</div>
              </div>
              <input 
                type="checkbox" 
                checked={formIsEnabled} 
                onChange={e => setFormIsEnabled(e.target.checked)} 
                style={{width: '18px', height: '18px', accentColor: 'var(--accent)'}} 
              />
            </div>

            <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px'}}>
              <div>
                <label className="form-label">Prix Achat Base ($)</label>
                <input 
                  type="number" 
                  step="0.1" 
                  value={formBuy} 
                  onChange={e => setFormBuy(parseFloat(e.target.value) || 0)} 
                  className="input-field" 
                  required 
                />
              </div>
              <div>
                <label className="form-label">Prix Vente Base ($)</label>
                <input 
                  type="number" 
                  step="0.1" 
                  value={formSell} 
                  onChange={e => setFormSell(parseFloat(e.target.value) || 0)} 
                  className="input-field" 
                  required 
                />
              </div>
            </div>

            <div style={{background: 'rgba(59, 130, 246, 0.08)', border: '1px solid rgba(59, 130, 246, 0.2)', padding: '10px 14px', borderRadius: '6px'}}>
              <div style={{fontSize: '0.72rem', color: 'var(--text-muted)'}}>Marge serveur en temps reel :</div>
              <div style={{fontSize: '1.05rem', fontWeight: 700, color: 'var(--accent)'}}>
                {calculatedMargin}% de marge (Ratio {calculatedRatio})
              </div>
            </div>

            <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px'}}>
              <div>
                <label className="form-label">Stock Cible (Equilibre)</label>
                <input 
                  type="number" 
                  value={formTargetStock} 
                  onChange={e => setFormTargetStock(parseInt(e.target.value) || 1000)} 
                  className="input-field" 
                  required 
                />
              </div>
              <div>
                <label className="form-label">Stock Actuel (Serveur)</label>
                <input 
                  type="number" 
                  value={formCurrentStock} 
                  onChange={e => setFormCurrentStock(parseInt(e.target.value) || 0)} 
                  className="input-field" 
                />
              </div>
            </div>

            <div style={{background: 'rgba(0,0,0,0.25)', padding: '12px', borderRadius: '8px', border: '1px solid var(--card-border)'}}>
              <div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px'}}>
                <div>
                  <div style={{fontWeight: 600, fontSize: '0.85rem', color: 'white'}}>Mode Commande / Grade</div>
                  <div style={{fontSize: '0.72rem', color: 'var(--text-muted)'}}>Execute une commande au lieu de donner l'item</div>
                </div>
                <input 
                  type="checkbox" 
                  checked={formIsCommand} 
                  onChange={e => setFormIsCommand(e.target.checked)} 
                  style={{width: '18px', height: '18px', accentColor: 'var(--accent)'}} 
                />
              </div>
              {formIsCommand && (
                <div style={{marginTop: '8px'}}>
                  <label className="form-label">Commande a executer (%player%)</label>
                  <input 
                    type="text" 
                    value={formCommand} 
                    onChange={e => setFormCommand(e.target.value)} 
                    className="input-field" 
                    placeholder="lp user %player% parent set vip" 
                  />
                </div>
              )}
            </div>

            <button type="submit" className="login-button" style={{padding: '12px', fontWeight: 700}}>
              {inspectorMode === 'edit' ? 'Enregistrer les Modifications' : `Ajouter a ${activeCat?.displayName}`}
            </button>

            {inspectorMode === 'edit' && activeCat && (
              <button 
                type="button" 
                className="btn-danger"
                onClick={() => {
                  if (confirm(`Voulez-vous vraiment retirer ${formMat} de la categorie ${activeCat.displayName} ?`)) {
                    deleteItem(activeCat.id, formMat);
                  }
                }}
                style={{ width: '100%', marginTop: '8px' }}
              >
                <Trash2 size={16} /> Supprimer cet objet de la boutique
              </button>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}

// Helper global pour recuperer les donnees du joueur connecte
export const getStoredPlayerData = () => {
  try {
    const saved = localStorage.getItem('gens_player_data');
    if (saved) return JSON.parse(saved);
  } catch (e) {}
  return null;
};

// === COMPOSANTS : VUE CLIENT (BOUTIQUE JOUEUR DYNAMIQUE) ===
export function ClientShop({ isEnabled }: { isEnabled?: boolean }) {
  const { t } = useTranslation();
  const { balance } = usePlayerBalance();
  const [categories, setCategories] = useState<ShopCategory[]>([]);
  const [activeCatId, setActiveCatId] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedItem, setSelectedItem] = useState<ShopItem | null>(null);
  const [history, setHistory] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<'buy' | 'sell'>('buy');
  const [drawerQuantity, setDrawerQuantity] = useState(1);

  // Objets deposes via /web deposit
  const [depositedItems, setDepositedItems] = useState<any[]>([]);
  const [loadingDeposited, setLoadingDeposited] = useState(false);
  const [actionNotice, setActionNotice] = useState<{ text: string; type: 'success' | 'error' | 'info' } | null>(null);
  const [isProcessingAction, setIsProcessingAction] = useState(false);
  const [qtyModal, setQtyModal] = useState<{
    open: boolean;
    item: any | null;
    action: 'sell' | 'withdraw';
    quantity: number;
  }>({
    open: false,
    item: null,
    action: 'sell',
    quantity: 1
  });

  const openQtyModal = (item: any, action: 'sell' | 'withdraw') => {
    setQtyModal({
      open: true,
      item,
      action,
      quantity: item.amount || 1
    });
  };

  const showActionNotice = (text: string, type: 'success' | 'error' | 'info' = 'info') => {
    setActionNotice({ text, type });
    setTimeout(() => setActionNotice(null), 6000);
  };

  const getPlayerData = getStoredPlayerData;

  const fetchDeposited = () => {
    const p = getPlayerData();
    const headers: Record<string, string> = { 'Content-Type': 'application/json; charset=utf-8' };
    let url = `${API_URL}/shop/deposited`;
    if (p) {
      if (p.token) headers['Authorization'] = `Bearer ${p.token}`;
      if (p.uuid) {
        headers['X-Player-UUID'] = p.uuid;
        url += `?uuid=${p.uuid}`;
      }
    }
    setLoadingDeposited(true);
    fetch(url, { headers })
      .then(res => res.json())
      .then(data => {
        setDepositedItems(Array.isArray(data) ? data : []);
        setLoadingDeposited(false);
      })
      .catch(() => {
        setDepositedItems([]);
        setLoadingDeposited(false);
      });
  };

  useEffect(() => {
    if (isEnabled === false) {
      setLoading(false);
      return;
    }
    fetch(`${API_URL}/shop/categories`)
      .then(res => res.json())
      .then(data => {
        setCategories(data || []);
        setLoading(false);
      })
      .catch(() => setLoading(false));

    fetchDeposited();
  }, [isEnabled]);

  const handleSellDeposited = async (item: any, quantity?: number) => {
    const qty = quantity && quantity > 0 ? quantity : item.amount;
    const p = getPlayerData();
    const headers: Record<string, string> = { 'Content-Type': 'application/json; charset=utf-8' };
    if (p) {
      if (p.token) headers['Authorization'] = `Bearer ${p.token}`;
      if (p.uuid) headers['X-Player-UUID'] = p.uuid;
    }

    setIsProcessingAction(true);
    try {
      const res = await fetch(`${API_URL}/shop/sell-deposited`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          betId: item.betId || item.id,
          material: item.material,
          quantity: qty
        })
      });
      const data = await res.json();
      if (res.ok && data.success) {
        const gain = data.earned !== undefined ? data.earned : data.gain !== undefined ? data.gain : 0;
        showActionNotice(`Vente réussie ! +${gain.toFixed(2)} $ ont été ajoutés à votre solde en jeu.`, 'success');
        if (typeof data.newBalance === 'number') {
          emitBalanceChange(data.newBalance, gain);
        } else {
          emitBalanceChange(0, gain);
        }
        fetchDeposited();
        fetch(`${API_URL}/shop/categories`).then(r => r.json()).then(d => setCategories(d || []));
      } else {
        showActionNotice(data.error || 'Erreur lors de la vente de l\'objet.', 'error');
      }
    } catch (err) {
      showActionNotice('Erreur réseau lors de la transaction.', 'error');
    } finally {
      setIsProcessingAction(false);
    }
  };

  const handleBuyItem = async () => {
    if (!selectedItem) return;
    const p = getPlayerData();
    if (!p || !p.uuid) {
      showActionNotice('Veuillez vous connecter à votre espace joueur pour acheter.', 'error');
      return;
    }
    const headers: Record<string, string> = { 'Content-Type': 'application/json; charset=utf-8' };
    if (p.token) headers['Authorization'] = `Bearer ${p.token}`;
    if (p.uuid) headers['X-Player-UUID'] = p.uuid;

    setIsProcessingAction(true);
    try {
      const res = await fetch(`${API_URL}/shop/buy`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          material: selectedItem.material,
          quantity: drawerQuantity
        })
      });
      const data = await res.json();
      if (res.ok && data.success) {
        const cost = data.totalCost ? data.totalCost : drawerQuantity * ((selectedItem.currentBuyPrice !== undefined ? selectedItem.currentBuyPrice : selectedItem.baseBuyPrice) || 0);
        showActionNotice(`Achat effectué (${cost.toFixed(2)} $) ! Objets livrés en jeu (ou conservés pour votre prochaine connexion).`, 'success');
        if (typeof data.newBalance === 'number') {
          emitBalanceChange(data.newBalance, -cost);
        } else {
          emitBalanceChange(0, -cost);
        }
        fetch(`${API_URL}/shop/categories`).then(r => r.json()).then(d => setCategories(d || []));
      } else {
        showActionNotice(data.error || 'Solde insuffisant ou transaction refusée.', 'error');
      }
    } catch (err) {
      showActionNotice('Erreur réseau lors de l\'achat.', 'error');
    } finally {
      setIsProcessingAction(false);
    }
  };

  const handleWithdrawDeposited = async (item: any, quantity?: number) => {
    const qty = quantity && quantity > 0 ? quantity : item.amount;
    const p = getPlayerData();
    const headers: Record<string, string> = { 'Content-Type': 'application/json; charset=utf-8' };
    if (p) {
      if (p.token) headers['Authorization'] = `Bearer ${p.token}`;
      if (p.uuid) headers['X-Player-UUID'] = p.uuid;
    }

    setIsProcessingAction(true);
    try {
      const res = await fetch(`${API_URL}/shop/withdraw-deposited`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          betId: item.betId || item.id,
          quantity: qty
        })
      });
      const data = await res.json();
      if (res.ok && data.success) {
        showActionNotice(`Objet restitué ! ${item.material} (x${qty}) a été transféré vers votre inventaire en jeu (ou sera livré à votre reconnexion).`, 'success');
        fetchDeposited();
      } else {
        showActionNotice(data.error || 'Erreur lors de la récupération de l\'objet.', 'error');
      }
    } catch (err) {
      showActionNotice('Erreur réseau lors de la récupération.', 'error');
    } finally {
      setIsProcessingAction(false);
    }
  };

  const openItemDrawer = (item: ShopItem) => {
    setSelectedItem(item);
    setDrawerQuantity(1);
    setDrawerMode('buy');
    setDrawerOpen(true);

    fetch(`${API_URL}/shop/history/${item.material}`)
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) {
          const formatted = data.map((d: any) => ({
            time: new Date(d.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            Achat: d.buyPrice,
            Vente: d.sellPrice,
            Stock: d.stock
          }));
          setHistory(formatted);
        } else {
          setHistory([]);
        }
      })
      .catch(() => setHistory([]));
  };

  if (isEnabled === false) {
    return (
      <div style={{textAlign: 'center', padding: '4rem 0', color: 'var(--text-muted)'}}>
        <ShoppingCart size={48} style={{opacity: 0.5, marginBottom: '1rem'}} />
        <h2>{t('web.public.shop.disabled_title')}</h2>
        <p>{t('web.public.shop.disabled_desc')}</p>
      </div>
    );
  }

  if (loading) return <div className="loading">{t('web.public.shop.loading')}</div>;

  // Filtrage des categories et items
  const allItems: ShopItem[] = categories.flatMap(c => c.items || []);
  let displayItems = activeCatId === 'all' 
    ? allItems 
    : (categories.find(c => String(c.id).toLowerCase() === String(activeCatId).toLowerCase())?.items || []);
  displayItems = displayItems.filter(i => i.isEnabled !== false);

  if (searchQuery.trim()) {
    const q = searchQuery.toLowerCase();
    displayItems = displayItems.filter(i => i.material.toLowerCase().includes(q));
  }

  const unitPrice = selectedItem ? (drawerMode === 'buy' ? (selectedItem.currentBuyPrice || selectedItem.baseBuyPrice) : (selectedItem.currentSellPrice || selectedItem.baseSellPrice)) : 0;
  const totalCost = unitPrice * drawerQuantity;

  return (
    <div>
      {/* NOTICE TOAST / NOTIFICATION D'ACTION */}
      {actionNotice && (
        <div style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          zIndex: 9999,
          padding: '14px 20px',
          borderRadius: '10px',
          fontWeight: 600,
          fontSize: '0.9rem',
          maxWidth: '420px',
          boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
          background: actionNotice.type === 'success' ? '#065f46' : actionNotice.type === 'error' ? '#991b1b' : '#1e3a8a',
          border: `1px solid ${actionNotice.type === 'success' ? '#10b981' : actionNotice.type === 'error' ? '#ef4444' : '#3b82f6'}`,
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
        }}>
          <span>{actionNotice.text}</span>
          <button onClick={() => setActionNotice(null)} style={{background: 'transparent', border: 'none', color: 'white', cursor: 'pointer', marginLeft: 'auto'}}>
            <X size={16} />
          </button>
        </div>
      )}

      <div className="client-hero" style={{padding: '2rem 0'}}>
        <h2>{t('web.public.shop.title')}</h2>
        <p>{t('web.public.shop.subtitle')}</p>
      </div>

      {/* BARRE DE FILTRES ET RECHERCHE */}
      <div className="shop-filter-bar">
        <div className="tab-row" style={{minWidth: 0, width: '100%'}}>
          <button 
            type="button"
            className={`tab-btn ${activeCatId === 'all' ? 'active' : ''}`}
            onClick={() => { setActiveCatId('all'); setSearchQuery(''); }}
          >
            Tous ({allItems.length})
          </button>

          <button 
            type="button"
            className={`tab-btn ${activeCatId === 'deposited' ? 'active' : ''}`}
            onClick={() => { setActiveCatId('deposited'); setSearchQuery(''); fetchDeposited(); }}
            style={{
              background: activeCatId === 'deposited' ? 'linear-gradient(135deg, rgba(16,185,129,0.3), rgba(5,150,105,0.2))' : undefined,
              borderColor: activeCatId === 'deposited' ? '#10b981' : undefined
            }}
          >
            <Package size={16} color="#10b981" style={{pointerEvents: 'none'}} />
            <span style={{pointerEvents: 'none'}}>Mes Objets Déposés (/web deposit)</span>
            {depositedItems.length > 0 && (
              <span style={{fontSize: '0.72rem', background: '#10b981', color: 'white', padding: '1px 6px', borderRadius: '10px', fontWeight: 'bold', pointerEvents: 'none'}}>
                {depositedItems.length}
              </span>
            )}
          </button>

          {categories.map(c => {
            const isSelected = String(activeCatId).toLowerCase() === String(c.id).toLowerCase();
            return (
              <button 
                key={c.id} 
                type="button"
                className={`tab-btn ${isSelected ? 'active' : ''}`}
                onClick={() => { setActiveCatId(c.id); setSearchQuery(''); }}
              >
                <div className="mc-item-icon" style={{width: '18px', height: '18px', pointerEvents: 'none'}}>
                  <img 
                    src={getMinecraftItemUrl(c.icon || 'CHEST')} 
                    alt={c.id} 
                    loading="lazy" 
                    decoding="async" 
                    onError={handleMinecraftImageError}
                    draggable={false}
                    style={{pointerEvents: 'none'}}
                  />
                </div>
                <span style={{pointerEvents: 'none'}}>{c.displayName}</span>
                <span style={{fontSize: '0.72rem', opacity: 0.7, pointerEvents: 'none'}}>({c.items?.length || 0})</span>
              </button>
            );
          })}
        </div>

        <div className="shop-search-box">
          <Search size={16} color="var(--text-muted)" style={{flexShrink: 0}} />
          <input 
            type="text" 
            placeholder="Rechercher un objet..." 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            style={{background: 'transparent', border: 'none', color: 'white', outline: 'none', width: '100%', fontSize: '0.88rem'}}
          />
        </div>
      </div>

      {/* VUE SPECIFIQUE : OBJETS DEPOSES (/web deposit) */}
      {activeCatId === 'deposited' ? (
        <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
          {/* BANDEAU ET COMPTEUR DE SLOTS (27 SLOTS MAX - COMME UN COFFRE) */}
          <div style={{background: 'rgba(16, 185, 129, 0.08)', border: '1px solid rgba(16, 185, 129, 0.25)', padding: '16px 20px', borderRadius: '12px', display: 'flex', flexDirection: 'column', gap: '12px'}}>
            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px'}}>
              <div style={{display: 'flex', alignItems: 'center', gap: '10px', fontWeight: 700, fontSize: '1.05rem', color: '#10b981'}}>
                <Package size={20} /> Vos Objets Déposés via /web deposit
              </div>
              <div style={{display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.82rem', fontWeight: 600}}>
                <span style={{color: 'var(--text-muted)'}}>Capacité de stockage :</span>
                <span style={{
                  padding: '3px 10px',
                  borderRadius: '12px',
                  background: depositedItems.length >= 27 ? 'rgba(239, 68, 68, 0.2)' : 'rgba(16, 185, 129, 0.15)',
                  color: depositedItems.length >= 27 ? '#ef4444' : '#10b981',
                  border: `1px solid ${depositedItems.length >= 27 ? '#ef4444' : 'rgba(16,185,129,0.3)'}`,
                  fontWeight: 700
                }}>
                  {depositedItems.length} / 27 slots {depositedItems.length >= 27 ? '(Plein !)' : '(Coffre)'}
                </span>
              </div>
            </div>

            {/* BARRE DE PROGRESSION DES SLOTS */}
            <div style={{background: 'rgba(0,0,0,0.35)', borderRadius: '6px', height: '8px', overflow: 'hidden'}}>
              <div style={{
                height: '100%',
                width: `${Math.min(100, Math.round((depositedItems.length / 27) * 100))}%`,
                background: depositedItems.length >= 27 ? '#ef4444' : depositedItems.length >= 20 ? '#f59e0b' : '#10b981',
                transition: 'width 0.3s ease'
              }}></div>
            </div>

            <div style={{fontSize: '0.85rem', color: 'var(--text-muted)', lineHeight: '1.5'}}>
              Ces objets sont stockés dans votre réserve web (limite : 27 slots d'un coffre Minecraft). Les objets identiques et empilables se fusionnent automatiquement pour économiser la place !
              <span style={{display: 'block', marginTop: '4px', fontStyle: 'italic'}}>
                Pour récupérer un objet directement dans votre inventaire en jeu, cliquez sur <strong>Récupérer</strong> ou tapez <strong style={{color: '#60a5fa'}}>/web withdraw</strong> en jeu.
              </span>
            </div>
          </div>

          {loadingDeposited ? (
            <div className="loading">Chargement de vos objets déposés...</div>
          ) : depositedItems.length === 0 ? (
            <div style={{textAlign: 'center', padding: '60px 20px', background: 'rgba(0,0,0,0.2)', borderRadius: '12px', border: '1px solid var(--card-border)'}}>
              <Package size={48} style={{opacity: 0.3, marginBottom: '12px', color: 'var(--accent)'}} />
              <h3 style={{color: 'white', marginBottom: '8px'}}>Aucun objet déposé en réserve</h3>
              <p style={{color: 'var(--text-muted)', fontSize: '0.9rem', maxWidth: '500px', margin: '0 auto 16px auto'}}>
                Prenez un bloc ou un objet en main sur le serveur Minecraft et tapez la commande <code style={{color: '#60a5fa', fontWeight: 700}}>/web deposit</code> pour le déposer et pouvoir le vendre depuis cette page !
              </p>
            </div>
          ) : (
            <div className="shop-grid">
              {depositedItems.map(item => {
                const sellPrice = item.unitSellPrice ?? item.currentSellPrice ?? item.baseSellPrice ?? 0;
                const totalGain = item.totalSellPrice ?? (sellPrice * item.amount);
                const canSell = item.canSell !== undefined ? item.canSell : sellPrice > 0;

                return (
                  <div key={item.betId || item.material} className="shop-card" style={{border: '1px solid rgba(16, 185, 129, 0.3)'}}>
                    <div style={{display: 'flex', alignItems: 'flex-start', gap: '14px'}}>
                      <div className="mc-slot-box">
                        <div className="mc-item-icon">
                          <img src={getMinecraftItemUrl(item.material)} alt={item.material} loading="lazy" decoding="async" onError={handleMinecraftImageError} />
                        </div>
                      </div>
                      <div style={{flex: 1, minWidth: 0}}>
                        <div style={{fontWeight: 700, fontSize: '1rem', color: 'white', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>
                          {item.material.replace(/_/g, ' ')}
                        </div>
                        <div style={{fontSize: '0.72rem', color: 'var(--text-muted)', fontFamily: 'monospace'}}>{item.material}</div>
                        <span style={{fontSize: '0.72rem', fontWeight: 700, padding: '2px 8px', borderRadius: '4px', background: 'rgba(16,185,129,0.15)', color: '#10b981', display: 'inline-block', marginTop: '6px'}}>
                          Quantité : x{item.amount}
                        </span>
                      </div>
                    </div>

                    <div className="price-row" style={{marginTop: '12px'}}>
                      <div>
                        <span style={{fontSize: '0.68rem', textTransform: 'uppercase', color: 'var(--text-muted)', display: 'block'}}>Cours actuel</span>
                        <span style={{fontWeight: 700, color: '#10b981', fontSize: '1rem'}}>{sellPrice.toFixed(2)} $ /u</span>
                      </div>
                      <div>
                        <span style={{fontSize: '0.68rem', textTransform: 'uppercase', color: 'var(--text-muted)', display: 'block'}}>Gain total</span>
                        <span style={{fontWeight: 700, color: '#10b981', fontSize: '1.05rem'}}>{totalGain.toFixed(2)} $</span>
                      </div>
                    </div>

                    <div style={{display: 'flex', gap: '8px', marginTop: '12px'}}>
                      <button 
                        className="login-button" 
                        style={{
                          flex: 1,
                          padding: '10px',
                          background: canSell ? '#10b981' : 'rgba(255,255,255,0.1)',
                          borderColor: canSell ? '#10b981' : 'transparent',
                          cursor: canSell ? 'pointer' : 'not-allowed',
                          opacity: canSell ? 1 : 0.5,
                          fontWeight: 700,
                          fontSize: '0.82rem'
                        }}
                        disabled={!canSell || isProcessingAction}
                        onClick={() => openQtyModal(item, 'sell')}
                      >
                        {canSell ? `Vendre (x${item.amount})` : 'Non racheté'}
                      </button>
                      <button
                        className="login-button"
                        style={{
                          flex: 1,
                          padding: '10px',
                          background: 'rgba(59, 130, 246, 0.15)',
                          borderColor: 'rgba(59, 130, 246, 0.5)',
                          color: '#93c5fd',
                          cursor: 'pointer',
                          fontWeight: 700,
                          fontSize: '0.82rem'
                        }}
                        disabled={isProcessingAction}
                        onClick={() => openQtyModal(item, 'withdraw')}
                      >
                        Récupérer en jeu
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ) : (
        /* GRILLE D'ITEMS DU SHOP REGULIER */
        <div className="shop-grid">
          {displayItems.map(item => {
            const buyPrice = item.currentBuyPrice || item.baseBuyPrice;
            const sellPrice = item.currentSellPrice || item.baseSellPrice;
            const targetStock = item.targetStock || 1000;
            const currentStock = item.stock || 0;
            const stockRatio = Math.min(100, Math.round((currentStock / targetStock) * 100));
            const stockClass = stockRatio > 80 ? 'stock-high' : stockRatio > 40 ? 'stock-medium' : 'stock-low';

            return (
              <div key={item.material} className="shop-card" onClick={() => openItemDrawer(item)}>
                <div style={{display: 'flex', alignItems: 'flex-start', gap: '14px'}}>
                  <div className="mc-slot-box">
                    <div className="mc-item-icon">
                      <img src={getMinecraftItemUrl(item.material)} alt={item.material} loading="lazy" decoding="async" onError={handleMinecraftImageError} />
                    </div>
                  </div>
                  <div style={{flex: 1, minWidth: 0}}>
                    <div style={{fontWeight: 700, fontSize: '1rem', color: 'white', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>
                      {item.material.replace(/_/g, ' ')}
                    </div>
                    <div style={{fontSize: '0.72rem', color: 'var(--text-muted)', fontFamily: 'monospace'}}>{item.material}</div>
                    {item.isCommand && (
                      <span style={{fontSize: '0.68rem', fontWeight: 700, padding: '2px 6px', borderRadius: '4px', background: 'rgba(245,158,11,0.15)', color: '#f59e0b', display: 'inline-block', marginTop: '4px'}}>
                        Commande
                      </span>
                    )}
                  </div>
                </div>

                <div style={{display: 'flex', flexDirection: 'column', gap: '4px'}}>
                  <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-muted)'}}>
                    <span>Stock : {currentStock}</span>
                    <span>Cible : {targetStock} ({stockRatio}%)</span>
                  </div>
                  <div className="stock-gauge-bar">
                    <div className={`stock-gauge-fill ${stockClass}`} style={{width: `${stockRatio}%`, height: '100%'}}></div>
                  </div>
                </div>

                <div className="price-row">
                  <div>
                    <span style={{fontSize: '0.68rem', textTransform: 'uppercase', color: 'var(--text-muted)', display: 'block'}}>Achat</span>
                    <span style={{fontWeight: 700, color: '#10b981', fontSize: '1.05rem'}}>{buyPrice.toFixed(2)} $</span>
                  </div>
                  <div>
                    <span style={{fontSize: '0.68rem', textTransform: 'uppercase', color: 'var(--text-muted)', display: 'block'}}>Vente</span>
                    {sellPrice > 0 ? (
                      <span style={{fontWeight: 700, color: '#ef4444', fontSize: '1.05rem'}}>{sellPrice.toFixed(2)} $</span>
                    ) : (
                      <span style={{fontSize: '0.8rem', color: 'var(--text-muted)'}}>Non revendable</span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
          {displayItems.length === 0 && (
            <div style={{gridColumn: '1 / -1', textAlign: 'center', padding: '60px', color: 'var(--text-muted)'}}>
              Aucun objet correspondant dans cette categorie.
            </div>
          )}
        </div>
      )}

      {/* TIROIR LATERAL D'ACHAT / VENTE RAPIDE */}
      <div className={`drawer-overlay ${drawerOpen ? 'open' : ''}`} onClick={() => setDrawerOpen(false)}></div>
      <aside className={`drawer-panel ${drawerOpen ? 'open' : ''}`}>
        {selectedItem && (
          <>
            <div className="drawer-header">
              <div style={{fontWeight: 700, fontSize: '1.1rem', color: 'white'}}>
                {drawerMode === 'buy' ? 'Acheter' : 'Vendre'} : {selectedItem.material}
              </div>
              <button className="btn-close" onClick={() => setDrawerOpen(false)} aria-label="Fermer">
                <X size={18}/>
              </button>
            </div>

            <div className="drawer-body">
              <div style={{display: 'flex', alignItems: 'center', gap: '16px', background: 'rgba(0,0,0,0.3)', padding: '16px', borderRadius: '12px', border: '1px solid var(--card-border)'}}>
                <div className="mc-slot-box" style={{width: '60px', height: '60px'}}>
                  <div className="mc-item-icon" style={{width: '44px', height: '44px'}}>
                    <img src={getMinecraftItemUrl(selectedItem.material)} alt={selectedItem.material} loading="lazy" decoding="async" onError={handleMinecraftImageError} />
                  </div>
                </div>
                <div>
                  <div style={{fontWeight: 700, fontSize: '1.15rem', color: 'white'}}>{selectedItem.material.replace(/_/g, ' ')}</div>
                  <div style={{fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'monospace'}}>{selectedItem.material}</div>
                  <div style={{fontSize: '0.8rem', color: 'var(--accent)', marginTop: '4px'}}>Stock disponible : {selectedItem.stock || 0}</div>
                </div>
              </div>

              {/* TABS ACHAT / VENTE */}
              <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', background: 'var(--bg-color)', padding: '4px', borderRadius: '8px'}}>
                <button 
                  className={`multiplier-btn ${drawerMode === 'buy' ? 'selected' : ''}`}
                  onClick={() => setDrawerMode('buy')}
                >
                  Mode Achat
                </button>
                <button 
                  className={`multiplier-btn ${drawerMode === 'sell' ? 'selected' : ''}`}
                  onClick={() => setDrawerMode('sell')}
                  disabled={(selectedItem.currentSellPrice || selectedItem.baseSellPrice) <= 0}
                  style={{opacity: (selectedItem.currentSellPrice || selectedItem.baseSellPrice) <= 0 ? 0.4 : 1}}
                >
                  Mode Vente
                </button>
              </div>

              {/* GRAPHIQUE HISTORIQUE */}
              <div style={{background: 'rgba(0,0,0,0.35)', border: '1px solid var(--card-border)', borderRadius: '10px', padding: '14px'}}>
                <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '10px'}}>
                  <span>Cours en temps reel</span>
                  <span style={{color: '#10b981', fontWeight: 700}}>Actuel : {unitPrice.toFixed(2)} $</span>
                </div>
                <div style={{height: '140px', width: '100%'}}>
                  {history.length > 0 ? (
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={history}>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                        <XAxis dataKey="time" stroke="var(--text-muted)" fontSize={10} />
                        <YAxis stroke="var(--text-muted)" fontSize={10} domain={['dataMin - 1', 'dataMax + 1']} />
                        <Tooltip contentStyle={{background: '#12141a', border: '1px solid var(--card-border)', borderRadius: '8px', fontSize: '0.8rem'}} />
                        <Line type="monotone" dataKey="Achat" stroke="#10b981" strokeWidth={2} dot={false} />
                        <Line type="monotone" dataKey="Vente" stroke="#ef4444" strokeWidth={2} dot={false} />
                      </LineChart>
                    </ResponsiveContainer>
                  ) : (
                    <div style={{textAlign: 'center', color: 'var(--text-muted)', paddingTop: '50px', fontSize: '0.85rem'}}>
                      Cours stable ou pas encore d'historique
                    </div>
                  )}
                </div>
              </div>

              {drawerMode === 'buy' ? (
                <>
                  {/* MULTIPLICATEURS DE QUANTITE */}
                  <div>
                    <span className="form-label">Quantite Rapide</span>
                    <div style={{display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '6px'}}>
                      <button className={`multiplier-btn ${drawerQuantity === 1 ? 'selected' : ''}`} onClick={() => setDrawerQuantity(1)}>x1</button>
                      <button className={`multiplier-btn ${drawerQuantity === 16 ? 'selected' : ''}`} onClick={() => setDrawerQuantity(16)}>x16</button>
                      <button className={`multiplier-btn ${drawerQuantity === 32 ? 'selected' : ''}`} onClick={() => setDrawerQuantity(32)}>x32</button>
                      <button className={`multiplier-btn ${drawerQuantity === 64 ? 'selected' : ''}`} onClick={() => setDrawerQuantity(64)}>x64</button>
                      <button className="multiplier-btn" onClick={() => setDrawerQuantity(Math.min(selectedItem.stock || 64, 576))}>Max</button>
                    </div>
                  </div>

                  {/* CURSEUR DE QUANTITE */}
                  <div>
                    <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '6px'}}>
                      <span style={{color: 'var(--text-muted)'}}>Ajuster la quantite :</span>
                      <span style={{fontWeight: 700, color: 'white'}}>{drawerQuantity} unites</span>
                    </div>
                    <input 
                      type="range" 
                      min="1" 
                      max="256" 
                      value={drawerQuantity} 
                      onChange={e => setDrawerQuantity(parseInt(e.target.value))} 
                      style={{width: '100%', accentColor: 'var(--accent)'}} 
                    />
                  </div>

                  {/* SYNTHESE */}
                  <div style={{background: 'rgba(0,0,0,0.3)', border: '1px solid var(--card-border)', borderRadius: '10px', padding: '16px', display: 'flex', flexDirection: 'column', gap: '10px'}}>
                    <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: 'var(--text-muted)'}}>
                      <span>Prix unitaire :</span>
                      <span style={{fontWeight: 600, color: 'white'}}>{unitPrice.toFixed(2)} $</span>
                    </div>
                    <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: 'var(--text-muted)'}}>
                      <span>Quantité :</span>
                      <span style={{fontWeight: 600, color: 'white'}}>x{drawerQuantity}</span>
                    </div>
                    {balance !== null && (
                      <>
                        <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: 'var(--text-muted)'}}>
                          <span>Votre solde actuel :</span>
                          <span style={{fontWeight: 600, color: '#34d399'}}>{formatBalance(balance)} $</span>
                        </div>
                        <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: 'var(--text-muted)'}}>
                          <span>Solde après achat :</span>
                          <span style={{fontWeight: 600, color: balance >= totalCost ? '#34d399' : '#ef4444'}}>
                            {formatBalance(balance - totalCost)} $
                          </span>
                        </div>
                      </>
                    )}
                    <div style={{borderTop: '1px solid var(--card-border)', paddingTop: '10px', display: 'flex', justifyContent: 'space-between', fontWeight: 700, fontSize: '1.15rem', color: 'white'}}>
                      <span>Total :</span>
                      <span style={{color: '#10b981'}}>{totalCost.toFixed(2)} $</span>
                    </div>
                  </div>

                  {balance !== null && balance < totalCost && (
                    <div style={{
                      background: 'rgba(239, 68, 68, 0.15)',
                      border: '1px solid rgba(239, 68, 68, 0.4)',
                      padding: '10px 14px',
                      borderRadius: '8px',
                      color: '#f87171',
                      fontSize: '0.85rem',
                      fontWeight: 600,
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px'
                    }}>
                      <AlertTriangle size={18} />
                      <span>Solde insuffisant ! Il vous manque {formatBalance(totalCost - balance)} $.</span>
                    </div>
                  )}

                  <button 
                    className="login-button" 
                    style={{
                      padding: '12px', 
                      fontWeight: 700, 
                      background: (balance !== null && balance < totalCost) ? 'rgba(255,255,255,0.08)' : '#10b981', 
                      borderColor: (balance !== null && balance < totalCost) ? 'transparent' : '#10b981',
                      color: (balance !== null && balance < totalCost) ? 'var(--text-muted)' : 'white',
                      cursor: (balance !== null && balance < totalCost) ? 'not-allowed' : 'pointer'
                    }}
                    disabled={isProcessingAction || (balance !== null && balance < totalCost)}
                    onClick={handleBuyItem}
                  >
                    {isProcessingAction ? 'Traitement en cours...' : (balance !== null && balance < totalCost) ? 'Solde insuffisant' : `Acheter maintenant (${totalCost.toFixed(2)} $)`}
                  </button>
                  <div style={{fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'center'}}>
                    Distribution directe si vous êtes en ligne, ou automatique à votre prochaine reconnexion.
                  </div>
                </>
              ) : (
                /* MODE VENTE */
                <div style={{display: 'flex', flexDirection: 'column', gap: '14px'}}>
                  {(() => {
                    const depositedForThis = depositedItems.filter(d => d.material === selectedItem.material);
                    const totalDepositedCount = depositedForThis.reduce((acc, d) => acc + (d.amount || 0), 0);
                    const sellUnitPrice = selectedItem.currentSellPrice || selectedItem.baseSellPrice || 0;
                    const canSellThis = sellUnitPrice > 0;
                    const clampedSellQty = Math.max(1, Math.min(drawerQuantity, totalDepositedCount || 1));
                    const totalGain = sellUnitPrice * clampedSellQty;

                    if (totalDepositedCount > 0) {
                      return (
                        <div style={{display: 'flex', flexDirection: 'column', gap: '14px'}}>
                          <div style={{background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', padding: '14px 16px', borderRadius: '10px'}}>
                            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px'}}>
                              <span style={{fontWeight: 700, color: '#10b981', fontSize: '0.95rem', display: 'flex', alignItems: 'center', gap: '6px'}}>
                                <Package size={16} /> Objets en réserve (/web deposit)
                              </span>
                              <span style={{background: '#10b981', color: 'white', fontWeight: 700, fontSize: '0.75rem', padding: '2px 8px', borderRadius: '10px'}}>
                                {totalDepositedCount} disponible{totalDepositedCount > 1 ? 's' : ''}
                              </span>
                            </div>
                            <div style={{fontSize: '0.82rem', color: 'var(--text-muted)'}}>
                              Vous possédez <strong style={{color: 'white'}}>{totalDepositedCount}</strong> unité(s) de cet objet en réserve.
                            </div>
                          </div>

                          {/* SÉLECTEUR DE QUANTITÉ POUR LA VENTE */}
                          <div>
                            <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '6px'}}>
                              <span style={{color: 'var(--text-muted)'}}>Quantité à traiter :</span>
                              <span style={{fontWeight: 700, color: 'white'}}>{clampedSellQty} / {totalDepositedCount} unités</span>
                            </div>
                            <div style={{display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '6px', marginBottom: '10px'}}>
                              <button className={`multiplier-btn ${clampedSellQty === 1 ? 'selected' : ''}`} onClick={() => setDrawerQuantity(1)}>x1</button>
                              {totalDepositedCount >= 16 ? (
                                <button className={`multiplier-btn ${clampedSellQty === 16 ? 'selected' : ''}`} onClick={() => setDrawerQuantity(16)}>x16</button>
                              ) : <button className="multiplier-btn" disabled style={{opacity: 0.3}}>x16</button>}
                              {totalDepositedCount >= 32 ? (
                                <button className={`multiplier-btn ${clampedSellQty === 32 ? 'selected' : ''}`} onClick={() => setDrawerQuantity(32)}>x32</button>
                              ) : <button className="multiplier-btn" disabled style={{opacity: 0.3}}>x32</button>}
                              {totalDepositedCount >= 64 ? (
                                <button className={`multiplier-btn ${clampedSellQty === 64 ? 'selected' : ''}`} onClick={() => setDrawerQuantity(64)}>x64</button>
                              ) : <button className="multiplier-btn" disabled style={{opacity: 0.3}}>x64</button>}
                              <button className={`multiplier-btn ${clampedSellQty === totalDepositedCount ? 'selected' : ''}`} onClick={() => setDrawerQuantity(totalDepositedCount)}>Max</button>
                            </div>
                            <input 
                              type="range" 
                              min="1" 
                              max={totalDepositedCount} 
                              value={clampedSellQty} 
                              onChange={e => setDrawerQuantity(parseInt(e.target.value) || 1)} 
                              style={{width: '100%', accentColor: '#10b981'}} 
                            />
                          </div>

                          {/* SYNTHÈSE DE TRANSACTION */}
                          <div style={{background: 'rgba(0,0,0,0.3)', border: '1px solid var(--card-border)', borderRadius: '10px', padding: '14px', display: 'flex', flexDirection: 'column', gap: '8px'}}>
                            <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: 'var(--text-muted)'}}>
                              <span>Cours de rachat :</span>
                              <span style={{fontWeight: 600, color: 'white'}}>{sellUnitPrice.toFixed(2)} $ /u</span>
                            </div>
                            <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: 'var(--text-muted)'}}>
                              <span>Quantité sélectionnée :</span>
                              <span style={{fontWeight: 600, color: 'white'}}>x{clampedSellQty}</span>
                            </div>
                            <div style={{borderTop: '1px solid var(--card-border)', paddingTop: '8px', display: 'flex', justifyContent: 'space-between', fontWeight: 700, fontSize: '1.05rem', color: 'white'}}>
                              <span>Gain estimé :</span>
                              <span style={{color: '#10b981'}}>+{totalGain.toFixed(2)} $</span>
                            </div>
                          </div>

                          {/* BOUTONS D'ACTION */}
                          <div style={{display: 'flex', flexDirection: 'column', gap: '8px'}}>
                            <button 
                              className="login-button"
                              style={{
                                width: '100%', 
                                background: canSellThis ? '#10b981' : 'rgba(255,255,255,0.1)', 
                                borderColor: canSellThis ? '#10b981' : 'transparent', 
                                padding: '12px', 
                                fontWeight: 700, 
                                fontSize: '0.88rem',
                                cursor: canSellThis ? 'pointer' : 'not-allowed',
                                opacity: canSellThis ? 1 : 0.5
                              }}
                              disabled={!canSellThis || isProcessingAction}
                              onClick={() => {
                                if (depositedForThis.length > 0) {
                                  handleSellDeposited(depositedForThis[0], clampedSellQty);
                                }
                              }}
                            >
                              {isProcessingAction ? 'Transaction en cours...' : canSellThis ? `Vendre ${clampedSellQty} unité(s) (+${totalGain.toFixed(2)} $)` : 'Non racheté par la boutique'}
                            </button>
                            <button
                              className="login-button"
                              style={{
                                width: '100%', 
                                background: 'rgba(59,130,246,0.15)', 
                                borderColor: 'rgba(59,130,246,0.5)', 
                                color: '#93c5fd', 
                                padding: '12px', 
                                fontWeight: 700, 
                                fontSize: '0.88rem',
                                cursor: 'pointer'
                              }}
                              disabled={isProcessingAction}
                              onClick={() => {
                                if (depositedForThis.length > 0) {
                                  handleWithdrawDeposited(depositedForThis[0], clampedSellQty);
                                }
                              }}
                            >
                              {isProcessingAction ? 'Traitement...' : `Récupérer ${clampedSellQty} unité(s) en jeu`}
                            </button>
                          </div>
                        </div>
                      );
                    }

                    // Si 0 déposé pour cet item
                    return (
                      <div style={{background: 'rgba(245, 158, 11, 0.08)', border: '1px solid rgba(245, 158, 11, 0.25)', padding: '14px', borderRadius: '10px'}}>
                        <div style={{display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 700, color: '#f59e0b', fontSize: '0.92rem', marginBottom: '6px'}}>
                          <AlertTriangle size={18} /> Aucun objet déposé en réserve
                        </div>
                        <div style={{fontSize: '0.82rem', color: 'var(--text-muted)', lineHeight: '1.5'}}>
                          Vous ne possédez aucun <strong style={{color: 'white'}}>{selectedItem.material}</strong> dans votre réserve web (0 disponible).
                        </div>
                      </div>
                    );
                  })()}

                  <div style={{background: 'rgba(59, 130, 246, 0.08)', border: '1px solid rgba(59, 130, 246, 0.25)', padding: '14px', borderRadius: '10px'}}>
                    <div style={{fontWeight: 700, color: 'var(--accent)', fontSize: '0.92rem', marginBottom: '6px'}}>
                      Comment vendre cet objet en ligne ?
                    </div>
                    <ol style={{margin: '0', paddingLeft: '18px', fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: '1.5'}}>
                      <li>Connectez-vous sur le serveur Minecraft.</li>
                      <li>Prenez l'objet en main et tapez la commande : <code style={{color: '#60a5fa', fontWeight: 700}}>/web deposit</code></li>
                      <li>Revenez ici ou ouvrez l'onglet <strong style={{color: 'white'}}>Mes Objets Déposés</strong> pour le vendre en ligne d'un simple clic !</li>
                    </ol>
                    <div style={{marginTop: '10px', fontSize: '0.72rem', color: 'var(--text-muted)', fontStyle: 'italic'}}>
                      Astuce : pour récupérer vos objets déposés non vendus en jeu, tapez <code style={{color: '#60a5fa'}}>/web withdraw</code>.
                    </div>
                  </div>
                </div>
              )}
            </div>
          </>
        )}
      </aside>

      {/* MODALE DE SÉLECTION DE QUANTITÉ POUR VENTE / RÉCUPÉRATION */}
      {qtyModal.open && qtyModal.item && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.75)',
          backdropFilter: 'blur(4px)',
          zIndex: 9998,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px'
        }} onClick={() => setQtyModal(prev => ({ ...prev, open: false }))}>
          <div style={{
            background: '#161922',
            border: '1px solid var(--card-border)',
            borderRadius: '14px',
            padding: '24px',
            maxWidth: '460px',
            width: '100%',
            boxShadow: '0 20px 50px rgba(0,0,0,0.6)',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px'
          }} onClick={e => e.stopPropagation()}>
            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
              <div style={{fontWeight: 700, fontSize: '1.1rem', color: 'white', display: 'flex', alignItems: 'center', gap: '8px'}}>
                {qtyModal.action === 'sell' ? (
                  <>
                    <TrendingUp size={20} color="#10b981" />
                    <span>Vendre des objets déposés</span>
                  </>
                ) : (
                  <>
                    <Package size={20} color="#3b82f6" />
                    <span>Récupérer des objets en jeu</span>
                  </>
                )}
              </div>
              <button onClick={() => setQtyModal(prev => ({ ...prev, open: false }))} style={{background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer'}}>
                <X size={20} />
              </button>
            </div>

            {/* ITEM BANNER */}
            <div style={{display: 'flex', alignItems: 'center', gap: '14px', background: 'rgba(0,0,0,0.3)', padding: '12px 16px', borderRadius: '10px', border: '1px solid var(--card-border)'}}>
              <div className="mc-slot-box" style={{width: '48px', height: '48px'}}>
                <div className="mc-item-icon" style={{width: '36px', height: '36px'}}>
                  <img src={getMinecraftItemUrl(qtyModal.item.material)} alt={qtyModal.item.material} loading="lazy" decoding="async" onError={handleMinecraftImageError} />
                </div>
              </div>
              <div style={{flex: 1}}>
                <div style={{fontWeight: 700, fontSize: '1rem', color: 'white'}}>{qtyModal.item.material.replace(/_/g, ' ')}</div>
                <div style={{fontSize: '0.75rem', color: 'var(--text-muted)'}}>
                  Total en réserve : <strong style={{color: '#10b981'}}>x{qtyModal.item.amount}</strong>
                </div>
              </div>
            </div>

            {/* SÉLECTEUR DE QUANTITÉ */}
            <div>
              <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '8px'}}>
                <span style={{color: 'var(--text-muted)'}}>Quantité à {qtyModal.action === 'sell' ? 'vendre' : 'récupérer'} :</span>
                <span style={{fontWeight: 700, color: 'white', fontSize: '1rem'}}>{qtyModal.quantity} / {qtyModal.item.amount}</span>
              </div>
              <div style={{display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '6px', marginBottom: '12px'}}>
                <button className={`multiplier-btn ${qtyModal.quantity === 1 ? 'selected' : ''}`} onClick={() => setQtyModal(prev => ({ ...prev, quantity: 1 }))}>x1</button>
                {qtyModal.item.amount >= 16 ? (
                  <button className={`multiplier-btn ${qtyModal.quantity === 16 ? 'selected' : ''}`} onClick={() => setQtyModal(prev => ({ ...prev, quantity: 16 }))}>x16</button>
                ) : <button className="multiplier-btn" disabled style={{opacity: 0.3}}>x16</button>}
                {qtyModal.item.amount >= 32 ? (
                  <button className={`multiplier-btn ${qtyModal.quantity === 32 ? 'selected' : ''}`} onClick={() => setQtyModal(prev => ({ ...prev, quantity: 32 }))}>x32</button>
                ) : <button className="multiplier-btn" disabled style={{opacity: 0.3}}>x32</button>}
                {qtyModal.item.amount >= 64 ? (
                  <button className={`multiplier-btn ${qtyModal.quantity === 64 ? 'selected' : ''}`} onClick={() => setQtyModal(prev => ({ ...prev, quantity: 64 }))}>x64</button>
                ) : <button className="multiplier-btn" disabled style={{opacity: 0.3}}>x64</button>}
                <button className={`multiplier-btn ${qtyModal.quantity === qtyModal.item.amount ? 'selected' : ''}`} onClick={() => setQtyModal(prev => ({ ...prev, quantity: prev.item.amount }))}>Max</button>
              </div>
              <input 
                type="range" 
                min="1" 
                max={qtyModal.item.amount} 
                value={qtyModal.quantity} 
                onChange={e => setQtyModal(prev => ({ ...prev, quantity: Math.max(1, Math.min(parseInt(e.target.value) || 1, prev.item.amount)) }))} 
                style={{width: '100%', accentColor: qtyModal.action === 'sell' ? '#10b981' : '#3b82f6'}} 
              />
            </div>

            {/* SYNTHÈSE SI VENTE */}
            {qtyModal.action === 'sell' && (
              <div style={{background: 'rgba(0,0,0,0.3)', border: '1px solid var(--card-border)', borderRadius: '10px', padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: '6px'}}>
                <div style={{display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', color: 'var(--text-muted)'}}>
                  <span>Cours actuel :</span>
                  <span style={{fontWeight: 600, color: 'white'}}>{(qtyModal.item.currentSellPrice || qtyModal.item.baseSellPrice || 0).toFixed(2)} $ /u</span>
                </div>
                <div style={{borderTop: '1px solid var(--card-border)', paddingTop: '6px', display: 'flex', justifyContent: 'space-between', fontWeight: 700, fontSize: '1rem', color: 'white'}}>
                  <span>Gain total :</span>
                  <span style={{color: '#10b981'}}>+{((qtyModal.item.currentSellPrice || qtyModal.item.baseSellPrice || 0) * qtyModal.quantity).toFixed(2)} $</span>
                </div>
              </div>
            )}

            {/* BOUTONS D'ACTION DE LA MODALE */}
            <div style={{display: 'flex', gap: '10px', marginTop: '4px'}}>
              <button 
                className="multiplier-btn" 
                style={{flex: 1, padding: '12px', background: 'rgba(255,255,255,0.05)', color: 'white'}}
                onClick={() => setQtyModal(prev => ({ ...prev, open: false }))}
              >
                Annuler
              </button>
              <button 
                className="login-button" 
                style={{
                  flex: 2, 
                  padding: '12px', 
                  fontWeight: 700, 
                  background: qtyModal.action === 'sell' ? '#10b981' : '#2563eb', 
                  borderColor: qtyModal.action === 'sell' ? '#10b981' : '#2563eb'
                }}
                disabled={isProcessingAction}
                onClick={() => {
                  const it = qtyModal.item;
                  const q = qtyModal.quantity;
                  setQtyModal(prev => ({ ...prev, open: false }));
                  if (qtyModal.action === 'sell') {
                    handleSellDeposited(it, q);
                  } else {
                    handleWithdrawDeposited(it, q);
                  }
                }}
              >
                {qtyModal.action === 'sell' ? `Confirmer la vente (x${qtyModal.quantity})` : `Confirmer le retrait (x${qtyModal.quantity})`}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// === COMPOSANTS : VUE CLIENT (HOTEL DES VENTES / AH) ===
export function ClientAh({ isEnabled }: { isEnabled?: boolean }) {
  const { t } = useTranslation();
  const [items, setItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [actionNotice, setActionNotice] = useState<{ text: string; type: 'success' | 'error' | 'info' } | null>(null);
  const [isProcessingAction, setIsProcessingAction] = useState(false);

  const showActionNotice = (text: string, type: 'success' | 'error' | 'info' = 'info') => {
    setActionNotice({ text, type });
    setTimeout(() => setActionNotice(null), 6000);
  };

  const fetchAhItems = () => {
    fetch(`${API_URL}/ah/items`)
      .then(res => res.json())
      .then(data => { setItems(data || []); setLoading(false); })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    if (isEnabled === false) {
      setLoading(false);
      return;
    }
    fetchAhItems();
  }, [isEnabled]);

  const handleBuyAh = async (item: any) => {
    const p = getStoredPlayerData();
    if (!p || !p.uuid) {
      showActionNotice('Veuillez vous connecter à votre espace joueur pour acheter un objet.', 'error');
      return;
    }
    const displayName = item.displayName || (item.material ? item.material.replace(/_/g, ' ') : `Offre #${item.id}`);
    if (!window.confirm(`Confirmer l'achat de ${displayName} (x${item.amount || 1}) pour ${item.price.toFixed(2)} $ ?`)) {
      return;
    }
    const headers: Record<string, string> = { 'Content-Type': 'application/json; charset=utf-8' };
    if (p.token) headers['Authorization'] = `Bearer ${p.token}`;
    if (p.uuid) headers['X-Player-UUID'] = p.uuid;

    setIsProcessingAction(true);
    try {
      const res = await fetch(`${API_URL}/ah/buy`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ id: item.id })
      });
      const data = await res.json();
      if (res.ok && data.success) {
        showActionNotice(data.message || `Achat réussi (${item.price.toFixed(2)} $) ! L'objet vous a été livré en jeu (ou stocké pour votre reconnexion).`, 'success');
        if (typeof data.newBalance === 'number') {
          emitBalanceChange(data.newBalance, -item.price);
        } else {
          emitBalanceChange(0, -item.price);
        }
        fetchAhItems();
      } else {
        showActionNotice(data.error || 'Erreur lors de la transaction.', 'error');
      }
    } catch (err) {
      showActionNotice('Erreur réseau lors de l\'achat.', 'error');
    } finally {
      setIsProcessingAction(false);
    }
  };

  const handleCancelAh = async (item: any) => {
    const p = getStoredPlayerData();
    if (!p || !p.uuid) {
      showActionNotice('Veuillez vous connecter à votre espace joueur pour récupérer votre objet.', 'error');
      return;
    }
    const displayName = item.displayName || (item.material ? item.material.replace(/_/g, ' ') : `Offre #${item.id}`);
    if (!window.confirm(`Voulez-vous vraiment retirer votre offre de ${displayName} (x${item.amount || 1}) et récupérer l'objet ?`)) {
      return;
    }
    const headers: Record<string, string> = { 'Content-Type': 'application/json; charset=utf-8' };
    if (p.token) headers['Authorization'] = `Bearer ${p.token}`;
    if (p.uuid) headers['X-Player-UUID'] = p.uuid;

    setIsProcessingAction(true);
    try {
      const res = await fetch(`${API_URL}/ah/cancel`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ id: item.id })
      });
      const data = await res.json();
      if (res.ok && data.success) {
        showActionNotice(data.message || 'Offre retirée ! L\'objet vous a été restitué en jeu (ou stocké pour votre reconnexion).', 'success');
        fetchAhItems();
      } else {
        showActionNotice(data.error || 'Erreur lors du retrait de l\'offre.', 'error');
      }
    } catch (err) {
      showActionNotice('Erreur réseau lors du retrait de l\'offre.', 'error');
    } finally {
      setIsProcessingAction(false);
    }
  };

  if (isEnabled === false) {
    return (
      <div style={{textAlign: 'center', padding: '4rem 0', color: 'var(--text-muted)'}}>
        <Package size={48} style={{opacity: 0.5, marginBottom: '1rem'}} />
        <h2>{t('web.public.ah.disabled_title')}</h2>
        <p>{t('web.public.ah.disabled_desc')}</p>
      </div>
    );
  }

  if (loading) return <div className="loading">{t('web.public.ah.loading')}</div>;

  let filtered = items;
  if (searchQuery.trim()) {
    const q = searchQuery.toLowerCase();
    filtered = filtered.filter(i => 
      (i.displayName && i.displayName.toLowerCase().includes(q)) || 
      (i.material && i.material.toLowerCase().includes(q)) ||
      (i.sellerName && i.sellerName.toLowerCase().includes(q))
    );
  }

  const currentUser = getStoredPlayerData();

  return (
    <div>
      {/* NOTICE TOAST / NOTIFICATION D'ACTION */}
      {actionNotice && (
        <div style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          zIndex: 9999,
          padding: '14px 20px',
          borderRadius: '10px',
          fontWeight: 600,
          fontSize: '0.9rem',
          maxWidth: '420px',
          boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
          background: actionNotice.type === 'success' ? '#065f46' : actionNotice.type === 'error' ? '#991b1b' : '#1e3a8a',
          border: `1px solid ${actionNotice.type === 'success' ? '#10b981' : actionNotice.type === 'error' ? '#ef4444' : '#3b82f6'}`,
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
        }}>
          <span>{actionNotice.text}</span>
          <button onClick={() => setActionNotice(null)} style={{background: 'transparent', border: 'none', color: 'white', cursor: 'pointer', marginLeft: 'auto'}}>
            <X size={16} />
          </button>
        </div>
      )}

      <div className="client-hero" style={{padding: '2rem 0'}}>
        <h2>{t('web.public.ah.title')}</h2>
        <p>{t('web.public.ah.subtitle')}</p>
      </div>

      {/* BARRE DE RECHERCHE ET FILTRES */}
      <div className="shop-filter-bar">
        <div className="tab-row" style={{flex: '1 1 auto', minWidth: 0}}>
          <button className={`tab-btn ${categoryFilter === 'all' ? 'active' : ''}`} onClick={() => setCategoryFilter('all')}>
            Toutes les offres ({items.length})
          </button>
        </div>

        <div className="shop-search-box">
          <Search size={18} color="var(--text-muted)"/>
          <input 
            type="text" 
            placeholder="Rechercher objet ou vendeur..." 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            style={{background: 'transparent', border: 'none', color: 'white', outline: 'none', width: '100%', fontSize: '0.88rem'}}
          />
        </div>
      </div>

      {/* GRILLE D'OFFRES AVEC CARTES ENCHANTÉES & TÊTES DE JOUEURS */}
      <div className="ah-grid">
        {filtered.map(item => {
          const isEnchanted = item.isEnchanted || (item.enchantments && item.enchantments.length > 0);
          const remainingDays = Math.max(0, Math.floor(((item.expireTime || Date.now()) - Date.now()) / (1000 * 60 * 60 * 24)));
          const unitPrice = item.price / (item.amount || 1);
          const isOwner = (currentUser?.uuid && item.sellerUuid && currentUser.uuid === item.sellerUuid) ||
                          (currentUser?.username && item.sellerName && currentUser.username.toLowerCase() === item.sellerName.toLowerCase());

          return (
            <div key={item.id} className={`ah-card ${isEnchanted ? 'enchanted-foil' : ''}`}>
              <div style={{display: 'flex', alignItems: 'center', gap: '10px'}}>
                <img 
                  className="mc-player-head" 
                  src={getPlayerAvatarUrl(item.sellerName, 32)} 
                  alt={item.sellerName} 
                  onError={(e: any) => e.currentTarget.src = `${API_URL}/head/Steve/32`}
                />
                <div>
                  <div style={{fontWeight: 700, fontSize: '0.88rem', color: 'white'}}>{item.sellerName}</div>
                  <span style={{fontSize: '0.68rem', fontWeight: 700, padding: '2px 6px', borderRadius: '4px', background: isOwner ? 'rgba(245,158,11,0.15)' : 'rgba(59,130,246,0.15)', color: isOwner ? '#f59e0b' : 'var(--accent)'}}>
                    {isOwner ? 'VOTRE OFFRE' : 'VENDEUR'}
                  </span>
                </div>
                <div style={{marginLeft: 'auto', fontSize: '0.75rem', color: 'var(--text-muted)'}}>#{item.id}</div>
              </div>

              <div style={{display: 'flex', alignItems: 'center', gap: '14px', background: 'rgba(0,0,0,0.3)', padding: '12px', borderRadius: '10px', border: '1px solid var(--card-border)'}}>
                <div className="mc-slot-box" style={{width: '50px', height: '50px'}}>
                  <div className="mc-item-icon" style={{width: '36px', height: '36px'}}>
                    <img 
                      src={getMinecraftItemUrl(item.material || 'STONE')} 
                      alt={item.material} 
                      loading="lazy" 
                      decoding="async" 
                      onError={handleMinecraftImageError}
                    />
                  </div>
                </div>
                <div style={{flex: 1, minWidth: 0}}>
                  <div style={{fontWeight: 700, fontSize: '1rem', color: 'white', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>
                    {item.displayName || (item.material ? item.material.replace(/_/g, ' ') : `Offre #${item.id}`)}
                  </div>
                  <div style={{fontSize: '0.78rem', color: 'var(--text-muted)'}}>Quantite : x{item.amount || 1}</div>
                </div>
              </div>

              {item.enchantments && item.enchantments.length > 0 && (
                <div style={{display: 'flex', flexWrap: 'wrap', gap: '6px'}}>
                  {item.enchantments.map((ench: string, idx: number) => (
                    <span key={idx} className="enchant-tag">{ench}</span>
                  ))}
                </div>
              )}

              {item.lore && item.lore.length > 0 && (
                <div className="mc-lore-box">
                  {item.lore.map((l: string, idx: number) => (
                    <div key={idx}>{l}</div>
                  ))}
                </div>
              )}

              <div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderTop: '1px solid var(--card-border)', paddingTop: '12px', marginTop: 'auto', gap: '10px', flexWrap: 'wrap'}}>
                <div>
                  <div style={{fontSize: '1.2rem', fontWeight: 800, color: '#10b981'}}>{item.price.toFixed(2)} $</div>
                  <div style={{fontSize: '0.72rem', color: 'var(--text-muted)'}}>{unitPrice.toFixed(2)} $ / unite</div>
                  <div style={{fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '2px'}}>Expire dans {remainingDays}j</div>
                </div>
                <div>
                  {isOwner ? (
                    <button
                      className="login-button"
                      style={{
                        padding: '8px 14px',
                        background: 'rgba(245, 158, 11, 0.15)',
                        borderColor: 'rgba(245, 158, 11, 0.5)',
                        color: '#fcd34d',
                        cursor: isProcessingAction ? 'not-allowed' : 'pointer',
                        fontWeight: 700,
                        fontSize: '0.82rem',
                        borderRadius: '8px',
                        opacity: isProcessingAction ? 0.6 : 1
                      }}
                      disabled={isProcessingAction}
                      onClick={() => handleCancelAh(item)}
                    >
                      Récupérer mon objet
                    </button>
                  ) : (
                    <button
                      className="login-button"
                      style={{
                        padding: '8px 14px',
                        background: '#10b981',
                        borderColor: '#10b981',
                        color: 'white',
                        cursor: isProcessingAction ? 'not-allowed' : 'pointer',
                        fontWeight: 700,
                        fontSize: '0.82rem',
                        borderRadius: '8px',
                        opacity: isProcessingAction ? 0.6 : 1
                      }}
                      disabled={isProcessingAction}
                      onClick={() => handleBuyAh(item)}
                    >
                      Acheter ({item.price.toFixed(2)} $)
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
        {filtered.length === 0 && (
          <div style={{gridColumn: '1 / -1', textAlign: 'center', color: 'var(--text-muted)', padding: '3rem 0'}}>
            {t('web.public.ah.empty')}
          </div>
        )}
      </div>
    </div>
  );
}

export function ClientMap() {
  const { t } = useTranslation();
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
        <h2 style={{margin: 0}}>{t('web.public.map.title')}</h2>
        <a href={mapUrl} target="_blank" rel="noopener noreferrer" className="btn" style={{textDecoration: 'none', padding: '10px 20px', background: 'var(--accent)', color: '#fff', borderRadius: '8px', fontWeight: 'bold'}}>
          {t('web.public.map.open_fullscreen')}
        </a>
      </div>
      <div style={{width: '100%', height: '70vh', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--card-border)'}}>
        <iframe src={mapUrl} width="100%" height="100%" frameBorder="0" title="BlueMap"></iframe>
      </div>
    </div>
  );
}

export function ClientQuests({ isEnabled }: { isEnabled?: boolean }) {
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

      <div style={{maxWidth: '1000px', width: '100%', boxSizing: 'border-box', margin: '0 auto', background: 'var(--card-bg)', border: '1px solid var(--card-border)', borderRadius: '12px', padding: 'clamp(1rem, 3vw, 1.5rem)', overflowX: 'auto'}}>
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
                  <img src={getPlayerAvatarUrl(player.playerName, 32)} alt="skin" style={{width: '32px', height: '32px', borderRadius: '4px'}} />
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
      <header className="public-header" style={{padding: '20px 40px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(9, 9, 11, 0.8)', backdropFilter: 'blur(10px)', borderBottom: '1px solid var(--card-border)', position: 'sticky', top: 0, zIndex: 100}}>
        <div style={{display: 'flex', alignItems: 'center', gap: '15px'}}>
          <Package size={32} color="var(--accent)" />
          <h1 style={{margin: 0, fontSize: '1.5rem', fontFamily: 'Outfit'}}>GensCore</h1>
        </div>
        {playerData ? (
          <Link to="/dashboard" className="btn" style={{display: 'flex', alignItems: 'center', gap: '10px', padding: '8px 20px', background: 'var(--card-bg)', border: '1px solid var(--card-border)'}}>
            <img src={getPlayerAvatarUrl(playerData.username)} alt="avatar" style={{width: '24px', height: '24px', borderRadius: '4px'}} />
            {t('web.public.profile_btn')}
          </Link>
        ) : (
          <Link to="/login" className="btn">{t('web.auth.login_btn')}</Link>
        )}
      </header>

      <main style={{flex: 1, padding: '4rem 2rem', maxWidth: '1000px', margin: '0 auto', width: '100%'}}>
        <div className="client-hero hero-container" style={{textAlign: 'center', marginBottom: '4rem', padding: '5rem 2rem', background: 'linear-gradient(180deg, rgba(99, 102, 241, 0.05) 0%, rgba(0,0,0,0) 100%)', borderRadius: '24px', border: '1px solid rgba(255,255,255,0.03)'}}>
          <h2 className="hero-title" style={{fontSize: '4.5rem', marginBottom: '1.5rem', background: 'linear-gradient(to right, #a855f7, #3b82f6)', WebkitBackgroundClip: 'text', color: 'transparent', filter: 'drop-shadow(0 0 20px rgba(139, 92, 246, 0.3))'}}>{t('web.public.hero_title')}</h2>
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
      try {
        const parsed = JSON.parse(savedPlayer);
        setPlayerData(parsed);

        // Verify session and OP status to avoid phantom states or expired tokens
        const headers: Record<string, string> = {};
        if (parsed.token) headers['Authorization'] = `Bearer ${parsed.token}`;

        fetch(`${API_URL}/player/info?uuid=${encodeURIComponent(parsed.uuid || '')}`, { headers })
          .then(async res => {
            if (res.status === 401) {
              console.warn('[App] Player session expired on server, clearing stale credentials');
              localStorage.removeItem('gens_player_data');
              setPlayerData(null);
              return;
            }
            if (res.ok) {
              const data = await res.json();
              if (data && data.isOp !== parsed.isOp) {
                const updated = { ...parsed, isOp: data.isOp };
                setPlayerData(updated);
                localStorage.setItem('gens_player_data', JSON.stringify(updated));
              }
            }
          })
          .catch(console.error);
      } catch (e) {
        localStorage.removeItem('gens_player_data');
        setPlayerData(null);
      }
    }
  }, []);

  const handleAdminLogin = (pwd: string) => {
    localStorage.setItem('gens_admin_pwd', pwd);
    setAdminPassword(pwd);
  };

  const handleAdminLogout = () => {
    localStorage.removeItem('gens_admin_pwd');
    setAdminPassword(null);
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

import { useState, useEffect, useCallback } from 'react';
import { Sparkles, CheckCircle2, AlertCircle, RefreshCw, Lock, Zap, Clock, Home, TrendingUp, Utensils, Flame, Magnet, Layers, Wrench, Award, Power } from 'lucide-react';
import { useTranslation } from 'react-i18next';

const API_URL = '/api';

export interface PerkItem {
  id: string;
  nameFr: string;
  nameEn: string;
  descriptionFr: string;
  descriptionEn: string;
  icon: string;
  requiredQuests: number;
  costMoney: number;
  costXp: number;
  isMajor: boolean;
  isToggleable: boolean;
  isUnlocked: boolean;
  isEnabled: boolean;
  canClaim: boolean;
}

export interface PerksData {
  enabled: boolean;
  questsCompleted: number;
  isEconomyEnabled: boolean;
  balance: number;
  xpLevels: number;
  perks: Record<string, PerkItem>;
}

export function PlayerPerksSection({ token, isEnabled = true }: { token: string, uuid?: string, isEnabled?: boolean }) {
  const { t, i18n } = useTranslation();
  const isFr = (i18n.language || 'fr').startsWith('fr');

  const [data, setData] = useState<PerksData | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ text: string, type: 'success' | 'error' } | null>(null);

  const fetchPerks = useCallback(async () => {
    try {
      const res = await fetch(`${API_URL}/player/perks`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (res.ok) {
        const json = await res.json();
        setData(json);
      }
    } catch (err) {
      console.error('Erreur lors du chargement des bonus personnels:', err);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchPerks();
  }, [fetchPerks]);

  const handleClaim = async (perkId: string) => {
    setActionLoading(perkId);
    setFeedback(null);
    try {
      const res = await fetch(`${API_URL}/player/perks/claim`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ perkId, token })
      });
      const resData = await res.json();
      if (res.ok) {
        const perkObj = data?.perks[perkId];
        const perkName = perkObj ? (isFr ? perkObj.nameFr : perkObj.nameEn) : perkId;
        setFeedback({
          text: (t('web.perks.unlock_success', { name: perkName }) || `Bonus ${perkName} debloque avec succes !`).replace('{{name}}', perkName),
          type: 'success'
        });
        await fetchPerks();
      } else {
        setFeedback({
          text: resData.error || t('web.common.error') || 'Erreur lors du deblocage.',
          type: 'error'
        });
      }
    } catch (err) {
      setFeedback({
        text: t('web.auth.network_error') || 'Erreur reseau.',
        type: 'error'
      });
    } finally {
      setActionLoading(null);
    }
  };

  const handleToggle = async (perkId: string) => {
    setActionLoading(perkId);
    setFeedback(null);
    try {
      const res = await fetch(`${API_URL}/player/perks/toggle`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ perkId, token })
      });
      const resData = await res.json();
      if (res.ok) {
        const perkObj = data?.perks[perkId];
        const perkName = perkObj ? (isFr ? perkObj.nameFr : perkObj.nameEn) : perkId;
        setFeedback({
          text: (t('web.perks.toggle_success', { name: perkName }) || `Statut de ${perkName} mis a jour !`).replace('{{name}}', perkName),
          type: 'success'
        });
        await fetchPerks();
      } else {
        setFeedback({
          text: resData.error || t('web.common.error') || 'Erreur lors de la modification.',
          type: 'error'
        });
      }
    } catch (err) {
      setFeedback({
        text: t('web.auth.network_error') || 'Erreur reseau.',
        type: 'error'
      });
    } finally {
      setActionLoading(null);
    }
  };

  const renderIcon = (perkId: string) => {
    switch (perkId) {
      case 'FREE_REROLL':
        return <RefreshCw size={24} color="#38bdf8" />;
      case 'EXTRA_HOME':
        return <Home size={24} color="#4ade80" />;
      case 'SPEED_BOOST':
        return <Zap size={24} color="#facc15" />;
      case 'JOBS_XP':
        return <TrendingUp size={24} color="#c084fc" />;
      case 'WARMUP_REDUCTION':
        return <Clock size={24} color="#22d3ee" />;
      case 'FEED_ACCESS':
        return <Utensils size={24} color="#fb923c" />;
      case 'MAGNET':
        return <Magnet size={24} color="#f472b6" />;
      case 'DOUBLE_DROP':
        return <Layers size={24} color="#eab308" />;
      case 'PORTABLE_WORKBENCH':
        return <Wrench size={24} color="#fb923c" />;
      case 'AUTO_SMELT':
        return <Flame size={24} color="#ef4444" />;
      case 'KEEP_EXP':
        return <Sparkles size={24} color="#10b981" />;
      default:
        return <Award size={24} color="var(--accent)" />;
    }
  };

  if (!isEnabled) {
    return (
      <div className="dashboard-content" style={{ padding: '2rem' }}>
        <div className="admin-card" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
          <AlertCircle size={48} color="#f59e0b" style={{ margin: '0 auto 1rem auto' }} />
          <h2>{t('web.perks.title') || 'Bonus Personnels de Quêtes'}</h2>
          <p style={{ color: 'var(--text-muted)' }}>
            Ce module est actuellement désactivé par l'administration.
          </p>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="dashboard-content" style={{ padding: '2rem' }}>
        <div className="loading" style={{ textAlign: 'center', padding: '3rem' }}>
          <RefreshCw size={32} className="spinning" style={{ margin: '0 auto 1rem auto', color: 'var(--accent)' }} />
          <p>{t('web.public.loading') || 'Chargement des bonus...'}</p>
        </div>
      </div>
    );
  }

  const questsDone = data?.questsCompleted || 0;
  const isEco = data?.isEconomyEnabled ?? true;
  const balance = data?.balance || 0;
  const xpLevels = data?.xpLevels || 0;
  const perksMap = data?.perks || {};

  const freePerkOrder = ['FREE_REROLL', 'EXTRA_HOME', 'SPEED_BOOST', 'JOBS_XP', 'WARMUP_REDUCTION', 'FEED_ACCESS'];
  const majorPerkOrder = ['MAGNET', 'DOUBLE_DROP', 'PORTABLE_WORKBENCH', 'AUTO_SMELT', 'KEEP_EXP'];

  const freePerks = freePerkOrder.map(id => perksMap[id]).filter(Boolean);
  const majorPerks = majorPerkOrder.map(id => perksMap[id]).filter(Boolean);

  const totalUnlocked = Object.values(perksMap).filter(p => p.isUnlocked).length;
  const totalPerks = Object.values(perksMap).length || 11;
  const globalProgress = Math.min(100, Math.round((questsDone / 100) * 100));

  return (
    <div className="dashboard-content" style={{ padding: '2rem' }}>
      {/* Header */}
      <div className="admin-card" style={{ marginBottom: '2rem', display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.2rem' }}>
          <div style={{
            width: '56px', height: '56px', borderRadius: '12px',
            background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 0 20px rgba(245, 158, 11, 0.4)'
          }}>
            <Sparkles size={32} color="#ffffff" />
          </div>
          <div>
            <h1 style={{ margin: 0, fontSize: '1.8rem' }}>
              {t('web.perks.title') || 'Bonus Personnels de Quêtes'}
            </h1>
            <p style={{ color: 'var(--text-muted)', margin: '4px 0 0 0', fontSize: '0.9rem' }}>
              {t('web.perks.subtitle') || 'Progressez dans vos quetes pour debloquer des avantages permanents et des maitrises exclusives'}
            </p>
          </div>
        </div>

        <button
          onClick={fetchPerks}
          className="btn-action"
          style={{
            display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 16px',
            background: 'rgba(255,255,255,0.05)', border: '1px solid var(--card-border)',
            borderRadius: '8px', color: 'var(--text-main)', cursor: 'pointer'
          }}
        >
          <RefreshCw size={16} /> {t('web.wallet.refresh') || 'Actualiser'}
        </button>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div style={{
          padding: '1rem', borderRadius: '8px', marginBottom: '1.5rem',
          background: feedback.type === 'success' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
          border: `1px solid ${feedback.type === 'success' ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
          color: feedback.type === 'success' ? '#10b981' : '#ef4444',
          display: 'flex', alignItems: 'center', gap: '10px'
        }}>
          {feedback.type === 'success' ? <CheckCircle2 size={20} /> : <AlertCircle size={20} />}
          <span>{feedback.text}</span>
        </div>
      )}

      {/* Progress & Overview Summary Bar */}
      <div style={{
        display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 260px), 1fr))',
        gap: '1.2rem', marginBottom: '2.5rem'
      }}>
        {/* Quests Completed Box */}
        <div className="admin-card" style={{ padding: '1.4rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              {t('web.perks.quests_completed') || 'Quêtes complétées :'}
            </span>
            <Award size={18} color="#f59e0b" />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#f59e0b' }}>
            {questsDone} <span style={{ fontSize: '1rem', color: 'var(--text-muted)', fontWeight: 'normal' }}>quêtes</span>
          </div>
          <div style={{ width: '100%', background: 'rgba(255,255,255,0.08)', borderRadius: '6px', height: '8px', marginTop: '12px', overflow: 'hidden' }}>
            <div style={{
              width: `${globalProgress}%`, height: '100%',
              background: 'linear-gradient(to right, #f59e0b, #10b981)',
              borderRadius: '6px', transition: 'width 0.5s ease'
            }} />
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginTop: '6px', textAlign: 'right' }}>
            {globalProgress}% palier 100 quêtes
          </span>
        </div>

        {/* Unlocked Perks Count */}
        <div className="admin-card" style={{ padding: '1.4rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Avantages Débloqués :</span>
            <CheckCircle2 size={18} color="#10b981" />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#10b981' }}>
            {totalUnlocked} <span style={{ fontSize: '1rem', color: 'var(--text-muted)', fontWeight: 'normal' }}>/ {totalPerks}</span>
          </div>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginTop: '12px' }}>
            Bonus actifs sur votre compte
          </span>
        </div>

        {/* Balance or XP Box */}
        <div className="admin-card" style={{ padding: '1.4rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              {isEco ? 'Porte-monnaie joueur :' : 'Niveaux d\'expérience :'}
            </span>
            <Sparkles size={18} color={isEco ? '#f59e0b' : '#38bdf8'} />
          </div>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: isEco ? '#f59e0b' : '#38bdf8' }}>
            {isEco ? `${Number(balance).toFixed(2)} $` : `${xpLevels} Niveaux`}
          </div>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginTop: '12px' }}>
            {isEco ? 'Utilisable pour les maîtrises majeures' : 'Utilisable pour les achats de maîtrises'}
          </span>
        </div>
      </div>

      {/* Section 1: Paliers Gratuits */}
      <div style={{ marginBottom: '3rem' }}>
        <div style={{ marginBottom: '1.2rem' }}>
          <h2 style={{ margin: '0 0 6px 0', fontSize: '1.4rem', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Award size={22} color="#38bdf8" />
            {t('web.perks.free_section_title') || 'Paliers Gratuits de Quêtes'}
          </h2>
          <p style={{ color: 'var(--text-muted)', margin: 0, fontSize: '0.9rem' }}>
            {t('web.perks.free_section_desc') || 'Avantages automatiquement debloquables des que vous atteignez le nombre de quetes requis'}
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 320px), 1fr))', gap: '1.2rem' }}>
          {freePerks.map(perk => {
            const hasRequiredQuests = questsDone >= perk.requiredQuests;
            const isUnlocked = perk.isUnlocked;
            const title = isFr ? perk.nameFr : perk.nameEn;
            const desc = isFr ? perk.descriptionFr : perk.descriptionEn;

            return (
              <div
                key={perk.id}
                style={{
                  background: 'rgba(255,255,255,0.02)',
                  border: `1px solid ${isUnlocked ? 'rgba(16, 185, 129, 0.4)' : hasRequiredQuests ? 'rgba(56, 189, 248, 0.4)' : 'var(--card-border)'}`,
                  borderRadius: '12px',
                  padding: '1.3rem',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  position: 'relative',
                  overflow: 'hidden'
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div style={{
                        width: '42px', height: '42px', borderRadius: '10px',
                        background: 'rgba(255,255,255,0.05)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        border: '1px solid var(--card-border)'
                      }}>
                        {renderIcon(perk.id)}
                      </div>
                      <div>
                        <h3 style={{ margin: 0, fontSize: '1.1rem' }}>{title}</h3>
                        <span style={{ fontSize: '0.75rem', color: '#38bdf8', fontWeight: 'bold' }}>
                          Palier {perk.requiredQuests} Quêtes
                        </span>
                      </div>
                    </div>

                    <span style={{
                      padding: '3px 10px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 'bold',
                      background: isUnlocked ? 'rgba(16, 185, 129, 0.15)' : hasRequiredQuests ? 'rgba(56, 189, 248, 0.15)' : 'rgba(148, 163, 184, 0.15)',
                      color: isUnlocked ? '#10b981' : hasRequiredQuests ? '#38bdf8' : '#94a3b8',
                      border: `1px solid ${isUnlocked ? 'rgba(16, 185, 129, 0.3)' : hasRequiredQuests ? 'rgba(56, 189, 248, 0.3)' : 'rgba(148, 163, 184, 0.3)'}`,
                      display: 'flex', alignItems: 'center', gap: '4px'
                    }}>
                      {isUnlocked ? (
                        <><CheckCircle2 size={12} /> {t('web.perks.claimed_badge') || 'Acquis'}</>
                      ) : hasRequiredQuests ? (
                        <><Sparkles size={12} /> Disponible</>
                      ) : (
                        <><Lock size={12} /> {questsDone}/{perk.requiredQuests}</>
                      )}
                    </span>
                  </div>

                  <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', lineHeight: '1.4', marginBottom: '1.2rem', minHeight: '40px' }}>
                    {desc}
                  </p>
                </div>

                <div>
                  {isUnlocked ? (
                    <div style={{
                      background: 'rgba(16, 185, 129, 0.08)',
                      border: '1px solid rgba(16, 185, 129, 0.2)',
                      padding: '8px 12px',
                      borderRadius: '8px',
                      textAlign: 'center',
                      color: '#10b981',
                      fontSize: '0.85rem',
                      fontWeight: 'bold',
                      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px'
                    }}>
                      <CheckCircle2 size={16} /> Bonus Permanent Actif
                    </div>
                  ) : hasRequiredQuests ? (
                    <button
                      onClick={() => handleClaim(perk.id)}
                      disabled={actionLoading === perk.id}
                      className="login-button"
                      style={{
                        margin: 0, width: '100%', padding: '10px', fontSize: '0.9rem',
                        background: 'linear-gradient(to right, #0284c7, #0369a1)',
                        color: 'white', cursor: 'pointer', fontWeight: 'bold'
                      }}
                    >
                      {actionLoading === perk.id ? (
                        <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
                          <RefreshCw size={16} className="spinning" /> Déblocage...
                        </span>
                      ) : (
                        t('web.perks.claim_btn') || 'Réclamer ce bonus'
                      )}
                    </button>
                  ) : (
                    <div style={{
                      background: 'rgba(0,0,0,0.2)',
                      padding: '8px 12px',
                      borderRadius: '8px',
                      textAlign: 'center',
                      color: 'var(--text-muted)',
                      fontSize: '0.8rem',
                      fontStyle: 'italic'
                    }}>
                      Encore {perk.requiredQuests - questsDone} quête(s) requise(s)
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Section 2: Maîtrises Majeures */}
      <div>
        <div style={{ marginBottom: '1.2rem' }}>
          <h2 style={{ margin: '0 0 6px 0', fontSize: '1.4rem', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Sparkles size={22} color="#f59e0b" />
            {t('web.perks.major_section_title') || 'Maîtrises Majeures Personnelles'}
          </h2>
          <p style={{ color: 'var(--text-muted)', margin: 0, fontSize: '0.9rem' }}>
            {t('web.perks.major_section_desc') || 'Debloquez des maitrises puissantes avec vos quetes et payez en dollars ou en niveaux d\'experience'}
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 340px), 1fr))', gap: '1.2rem' }}>
          {majorPerks.map(perk => {
            const hasRequiredQuests = questsDone >= perk.requiredQuests;
            const isUnlocked = perk.isUnlocked;
            const title = isFr ? perk.nameFr : perk.nameEn;
            const desc = isFr ? perk.descriptionFr : perk.descriptionEn;
            const cost = isEco ? perk.costMoney : perk.costXp;
            const hasFunds = isEco ? (balance >= cost) : (xpLevels >= cost);

            return (
              <div
                key={perk.id}
                style={{
                  background: 'rgba(255,255,255,0.02)',
                  border: `1px solid ${isUnlocked ? 'rgba(245, 158, 11, 0.4)' : hasRequiredQuests ? 'rgba(59, 130, 246, 0.4)' : 'var(--card-border)'}`,
                  borderRadius: '12px',
                  padding: '1.3rem',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  position: 'relative'
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div style={{
                        width: '44px', height: '44px', borderRadius: '10px',
                        background: 'rgba(255,255,255,0.05)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        border: '1px solid var(--card-border)'
                      }}>
                        {renderIcon(perk.id)}
                      </div>
                      <div>
                        <h3 style={{ margin: 0, fontSize: '1.1rem' }}>{title}</h3>
                        <span style={{ fontSize: '0.75rem', color: '#f59e0b', fontWeight: 'bold' }}>
                          Niveau {perk.requiredQuests} Quêtes
                        </span>
                      </div>
                    </div>

                    <span style={{
                      padding: '3px 10px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 'bold',
                      background: isUnlocked ? (perk.isEnabled ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)') : 'rgba(148, 163, 184, 0.15)',
                      color: isUnlocked ? (perk.isEnabled ? '#10b981' : '#ef4444') : '#94a3b8',
                      border: `1px solid ${isUnlocked ? (perk.isEnabled ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)') : 'rgba(148, 163, 184, 0.3)'}`,
                      display: 'flex', alignItems: 'center', gap: '4px'
                    }}>
                      {isUnlocked ? (
                        perk.isToggleable ? (
                          perk.isEnabled ? <><Power size={12} /> {t('web.perks.active_badge') || 'Actif'}</> : <><Power size={12} /> {t('web.perks.inactive_badge') || 'Désactivé'}</>
                        ) : (
                          <><CheckCircle2 size={12} /> {t('web.perks.claimed_badge') || 'Acquis'}</>
                        )
                      ) : (
                        <><Lock size={12} /> {questsDone}/{perk.requiredQuests}</>
                      )}
                    </span>
                  </div>

                  <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', lineHeight: '1.4', marginBottom: '1.2rem', minHeight: '40px' }}>
                    {desc}
                  </p>

                  <div style={{
                    background: 'rgba(0,0,0,0.2)', padding: '10px 12px', borderRadius: '8px',
                    marginBottom: '1.2rem', fontSize: '0.85rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                  }}>
                    <span style={{ color: 'var(--text-muted)' }}>Coût d'acquisition :</span>
                    <strong style={{ color: isEco ? '#f59e0b' : '#38bdf8' }}>
                      {isEco ? `${Number(perk.costMoney).toFixed(0)} $` : `${perk.costXp} Niveaux XP`}
                    </strong>
                  </div>
                </div>

                <div>
                  {isUnlocked ? (
                    perk.isToggleable ? (
                      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                        <button
                          onClick={() => handleToggle(perk.id)}
                          disabled={actionLoading === perk.id}
                          className="login-button"
                          style={{
                            margin: 0, flex: 1, padding: '10px', fontSize: '0.85rem', fontWeight: 'bold',
                            background: perk.isEnabled ? 'rgba(239, 68, 68, 0.15)' : 'rgba(16, 185, 129, 0.15)',
                            color: perk.isEnabled ? '#ef4444' : '#10b981',
                            border: `1px solid ${perk.isEnabled ? 'rgba(239, 68, 68, 0.3)' : 'rgba(16, 185, 129, 0.3)'}`,
                            cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px'
                          }}
                        >
                          <Power size={14} />
                          {actionLoading === perk.id
                            ? 'Mise à jour...'
                            : (perk.isEnabled ? (t('web.perks.toggle_off') || 'Désactiver') : (t('web.perks.toggle_on') || 'Activer'))}
                        </button>
                      </div>
                    ) : (
                      <div style={{
                        background: 'rgba(16, 185, 129, 0.08)',
                        border: '1px solid rgba(16, 185, 129, 0.2)',
                        padding: '8px 12px',
                        borderRadius: '8px',
                        textAlign: 'center',
                        color: '#10b981',
                        fontSize: '0.85rem',
                        fontWeight: 'bold',
                        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px'
                      }}>
                        <CheckCircle2 size={16} /> Maîtrise Active
                      </div>
                    )
                  ) : hasRequiredQuests ? (
                    <div>
                      <button
                        onClick={() => handleClaim(perk.id)}
                        disabled={actionLoading === perk.id || !hasFunds}
                        className="login-button"
                        style={{
                          margin: 0, width: '100%', padding: '10px', fontSize: '0.9rem',
                          background: !hasFunds ? 'var(--card-bg)' : 'linear-gradient(to right, #d97706, #b45309)',
                          color: !hasFunds ? 'var(--text-muted)' : 'white',
                          cursor: hasFunds ? 'pointer' : 'not-allowed',
                          fontWeight: 'bold',
                          border: !hasFunds ? '1px solid var(--card-border)' : 'none'
                        }}
                      >
                        {actionLoading === perk.id ? (
                          <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
                            <RefreshCw size={16} className="spinning" /> Achat...
                          </span>
                        ) : !hasFunds ? (
                          isEco ? (t('web.perks.not_enough_funds') || 'Fonds insuffisants') : (t('web.perks.not_enough_xp') || 'Niveaux XP insuffisants')
                        ) : (
                          t('web.perks.buy_perk_btn') || 'Débloquer la maîtrise'
                        )}
                      </button>
                    </div>
                  ) : (
                    <div style={{
                      background: 'rgba(0,0,0,0.2)',
                      padding: '8px 12px',
                      borderRadius: '8px',
                      textAlign: 'center',
                      color: 'var(--text-muted)',
                      fontSize: '0.8rem',
                      fontStyle: 'italic'
                    }}>
                      Verrouillé - Requières {perk.requiredQuests} quêtes
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

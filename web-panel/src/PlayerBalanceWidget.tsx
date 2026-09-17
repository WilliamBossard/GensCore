import { useState, useEffect, useCallback } from 'react';
import { Coins, RefreshCw, TrendingUp, TrendingDown, Wallet } from 'lucide-react';
import { useTranslation } from 'react-i18next';

const API_URL = '/api';

export interface BalanceEventDetail {
  newBalance?: number;
  delta?: number;
  source?: string;
}

// Déclenche une mise à jour globale du solde reçue suite à un achat/vente ou event
export function emitBalanceChange(newBalance: number, delta?: number) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(
    new CustomEvent<BalanceEventDetail>('gens_balance_changed', {
      detail: { newBalance, delta }
    })
  );
}

// Hook réutilisable pour suivre le solde en temps réel
export function usePlayerBalance(initialUuid?: string, token?: string) {
  const [balance, setBalance] = useState<number | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [lastDelta, setLastDelta] = useState<{ amount: number; id: number } | null>(null);
  const [pulseType, setPulseType] = useState<'up' | 'down' | null>(null);

  const fetchBalance = useCallback(async (isManual: boolean = false) => {
    let uuid = initialUuid;
    if (!uuid) {
      try {
        const stored = localStorage.getItem('gens_player_data');
        if (stored) {
          const parsed = JSON.parse(stored);
          uuid = parsed.uuid;
        }
      } catch (e) {}
    }
    if (!uuid) return;

    if (isManual) setLoading(true);
    try {
      const headers: Record<string, string> = { 'Content-Type': 'application/json; charset=utf-8' };
      if (token) headers['Authorization'] = `Bearer ${token}`;
      headers['X-Player-UUID'] = uuid;

      const res = await fetch(`${API_URL}/player/balance?uuid=${encodeURIComponent(uuid)}`, { headers });
      if (res.ok) {
        const data = await res.json();
        if (typeof data.balance === 'number') {
          setBalance(prev => {
            if (prev !== null && prev !== data.balance) {
              const diff = data.balance - prev;
              triggerDelta(diff);
            }
            return data.balance;
          });
        }
      }
    } catch (e) {
      // Ignorer silencieusement en polling
    } finally {
      if (isManual) setLoading(false);
    }
  }, [initialUuid, token]);

  const triggerDelta = (amount: number) => {
    if (Math.abs(amount) < 0.001) return;
    setLastDelta({ amount, id: Date.now() });
    setPulseType(amount > 0 ? 'up' : 'down');

    // Réinitialiser le badge de variation après 4 secondes
    setTimeout(() => {
      setLastDelta(curr => (curr && Date.now() - curr.id >= 3800 ? null : curr));
      setPulseType(null);
    }, 4000);
  };

  useEffect(() => {
    fetchBalance(true);

    // Écouter les changements manuels émis par la boutique (achats, ventes)
    const handleCustomChange = (e: Event) => {
      const custom = e as CustomEvent<BalanceEventDetail>;
      if (!custom.detail) return;
      const { newBalance, delta } = custom.detail;

      if (typeof newBalance === 'number') {
        setBalance(prev => {
          const calculatedDelta = delta !== undefined ? delta : (prev !== null ? newBalance - prev : 0);
          if (calculatedDelta !== 0) {
            triggerDelta(calculatedDelta);
          }
          return newBalance;
        });
      } else if (typeof delta === 'number') {
        setBalance(prev => {
          if (prev === null) return null;
          const next = Math.max(0, prev + delta);
          triggerDelta(delta);
          return next;
        });
      }
    };

    window.addEventListener('gens_balance_changed', handleCustomChange);

    // Polling doux toutes les 14 secondes pour capter les gains/dépenses en jeu
    const interval = setInterval(() => {
      fetchBalance(false);
    }, 14000);

    return () => {
      window.removeEventListener('gens_balance_changed', handleCustomChange);
      clearInterval(interval);
    };
  }, [fetchBalance]);

  return {
    balance,
    loading,
    lastDelta,
    pulseType,
    refreshBalance: () => fetchBalance(true)
  };
}

export function formatBalance(amount: number | null | undefined): string {
  if (amount === null || amount === undefined) return '...';
  return Number(amount).toLocaleString('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

interface PlayerBalanceWidgetProps {
  uuid?: string;
  token?: string;
  variant?: 'pill' | 'card' | 'compact';
  showRefresh?: boolean;
}

export function PlayerBalanceWidget({
  uuid,
  token,
  variant = 'pill',
  showRefresh = true
}: PlayerBalanceWidgetProps) {
  const { t } = useTranslation();
  const { balance, loading, lastDelta, pulseType, refreshBalance } = usePlayerBalance(uuid, token);

  // Variante Carte (pour la sidebar sous l'avatar)
  if (variant === 'card') {
    return (
      <div 
        className={`wallet-sidebar-card ${pulseType ? `pulse-${pulseType}` : ''}`}
        style={{
          position: 'relative',
          background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.12), rgba(6, 78, 59, 0.25))',
          border: `1px solid ${pulseType === 'up' ? 'rgba(16, 185, 129, 0.8)' : pulseType === 'down' ? 'rgba(239, 68, 68, 0.8)' : 'rgba(16, 185, 129, 0.3)'}`,
          borderRadius: '12px',
          padding: '12px 14px',
          marginTop: '12px',
          marginBottom: '8px',
          boxShadow: pulseType === 'up' 
            ? '0 0 20px rgba(16, 185, 129, 0.35)' 
            : pulseType === 'down' 
            ? '0 0 20px rgba(239, 68, 68, 0.35)' 
            : '0 4px 12px rgba(0,0,0,0.2)',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '4px' }}>
          <span style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.5px', color: 'rgba(255,255,255,0.7)', display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 600 }}>
            <Wallet size={14} color="#10b981" />
            {t('web.wallet.title') || 'Porte-monnaie'}
          </span>
          {showRefresh && (
            <button
              onClick={() => refreshBalance()}
              title={t('web.wallet.refresh') || 'Actualiser le solde'}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'rgba(255,255,255,0.6)',
                cursor: 'pointer',
                padding: '2px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderRadius: '4px',
                transition: 'color 0.2s, transform 0.3s'
              }}
              onMouseEnter={e => (e.currentTarget.style.color = '#10b981')}
              onMouseLeave={e => (e.currentTarget.style.color = 'rgba(255,255,255,0.6)')}
            >
              <RefreshCw size={13} className={loading ? 'spin-anim' : ''} />
            </button>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'baseline', gap: '6px', position: 'relative' }}>
          <span style={{ fontSize: '1.35rem', fontWeight: 700, color: '#ecfdf5', letterSpacing: '-0.5px' }}>
            {formatBalance(balance)}
          </span>
          <span style={{ fontSize: '1rem', fontWeight: 700, color: '#10b981' }}>$</span>

          {/* Badge flottant lors d'un débit ou crédit */}
          {lastDelta && (
            <div
              key={lastDelta.id}
              className={`delta-bubble ${lastDelta.amount > 0 ? 'delta-positive' : 'delta-negative'}`}
              style={{
                position: 'absolute',
                right: '0',
                top: '-24px',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
                padding: '3px 8px',
                borderRadius: '20px',
                fontSize: '0.78rem',
                fontWeight: 700,
                background: lastDelta.amount > 0 ? 'rgba(16, 185, 129, 0.9)' : 'rgba(239, 68, 68, 0.9)',
                color: 'white',
                boxShadow: '0 4px 10px rgba(0,0,0,0.3)',
                animation: 'floatUpDelta 4s forwards'
              }}
            >
              {lastDelta.amount > 0 ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
              {lastDelta.amount > 0 ? `+${formatBalance(lastDelta.amount)}` : formatBalance(lastDelta.amount)} $
            </div>
          )}
        </div>
      </div>
    );
  }

  // Variante Pilule (Top bar persistante, header de la boutique, etc.)
  return (
    <div
      className={`wallet-pill-widget ${pulseType ? `pulse-${pulseType}` : ''}`}
      style={{
        position: 'relative',
        display: 'inline-flex',
        alignItems: 'center',
        gap: '8px',
        padding: '6px 14px',
        background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(6, 78, 59, 0.35))',
        backdropFilter: 'blur(10px)',
        WebkitBackdropFilter: 'blur(10px)',
        border: `1.5px solid ${pulseType === 'up' ? '#10b981' : pulseType === 'down' ? '#ef4444' : 'rgba(16, 185, 129, 0.4)'}`,
        borderRadius: '30px',
        boxShadow: pulseType === 'up'
          ? '0 0 18px rgba(16, 185, 129, 0.45)'
          : pulseType === 'down'
          ? '0 0 18px rgba(239, 68, 68, 0.45)'
          : '0 2px 8px rgba(0,0,0,0.25)',
        cursor: 'default',
        userSelect: 'none',
        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
      }}
    >
      <div 
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '24px',
          height: '24px',
          borderRadius: '50%',
          background: 'rgba(16, 185, 129, 0.25)',
          color: '#34d399'
        }}
      >
        <Coins size={15} />
      </div>

      <div style={{ display: 'flex', alignItems: 'baseline', gap: '4px' }}>
        <span style={{ fontSize: '0.95rem', fontWeight: 700, color: '#f0fdf4', letterSpacing: '0.2px' }}>
          {formatBalance(balance)}
        </span>
        <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#34d399' }}>$</span>
      </div>

      {showRefresh && (
        <button
          onClick={() => refreshBalance()}
          title={t('web.wallet.refresh') || 'Actualiser le solde'}
          style={{
            background: 'transparent',
            border: 'none',
            color: 'rgba(255,255,255,0.6)',
            cursor: 'pointer',
            padding: '2px',
            marginLeft: '2px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: '50%',
            transition: 'color 0.2s, transform 0.2s'
          }}
          onMouseEnter={e => (e.currentTarget.style.color = '#34d399')}
          onMouseLeave={e => (e.currentTarget.style.color = 'rgba(255,255,255,0.6)')}
        >
          <RefreshCw size={13} className={loading ? 'spin-anim' : ''} />
        </button>
      )}

      {/* Variation animée montante / descendante */}
      {lastDelta && (
        <div
          key={lastDelta.id}
          className={`delta-bubble ${lastDelta.amount > 0 ? 'delta-positive' : 'delta-negative'}`}
          style={{
            position: 'absolute',
            top: '-28px',
            right: '10px',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px',
            padding: '3px 9px',
            borderRadius: '16px',
            fontSize: '0.8rem',
            fontWeight: 800,
            background: lastDelta.amount > 0 ? '#059669' : '#dc2626',
            color: 'white',
            boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
            pointerEvents: 'none',
            animation: 'floatUpDelta 4s forwards'
          }}
        >
          {lastDelta.amount > 0 ? <TrendingUp size={13} /> : <TrendingDown size={13} />}
          {lastDelta.amount > 0 ? `+${formatBalance(lastDelta.amount)}` : formatBalance(lastDelta.amount)} $
        </div>
      )}
    </div>
  );
}

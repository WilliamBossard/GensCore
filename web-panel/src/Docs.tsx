import { Link } from 'react-router-dom';
import { Package, Shield, Target, Pickaxe, Map, ShoppingCart, Sword } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export function Docs() {
  const { t } = useTranslation();
  return (
    <div style={{minHeight: '100vh', background: 'var(--bg-color)', color: 'var(--text-color)', display: 'flex', flexDirection: 'column'}}>
      <header style={{padding: '20px 40px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(9, 9, 11, 0.8)', backdropFilter: 'blur(10px)', borderBottom: '1px solid var(--card-border)', position: 'sticky', top: 0, zIndex: 100}}>
        <div style={{display: 'flex', alignItems: 'center', gap: '15px'}}>
          <Package size={32} color="var(--accent)" />
          <h1 style={{margin: 0, fontSize: '1.5rem', fontFamily: 'Outfit'}}>GensCore Docs</h1>
        </div>
        <Link to="/" className="btn" style={{background: 'var(--card-bg)', border: '1px solid var(--card-border)'}}>{t('web.public.docs.back_btn')}</Link>
      </header>

      <main style={{flex: 1, padding: '4rem 2rem', maxWidth: '800px', margin: '0 auto', width: '100%'}}>
        <div style={{marginBottom: '3rem'}}>
          <h2 style={{fontSize: '3rem', marginBottom: '1rem', background: 'linear-gradient(to right, #a855f7, #3b82f6)', WebkitBackgroundClip: 'text', color: 'transparent'}}>{t('web.public.docs.title')}</h2>
          <p style={{fontSize: '1.2rem', color: 'var(--text-muted)'}}>{t('web.public.docs.subtitle')}</p>
        </div>

        <div style={{display: 'flex', flexDirection: 'column', gap: '2rem'}}>
          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#60a5fa'}}>
              <Shield size={24} /> {t('web.public.docs.sections.management.title')}
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}} dangerouslySetInnerHTML={{ __html: t('web.public.docs.sections.management.desc') }} />
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#34d399'}}>
              <ShoppingCart size={24} /> {t('web.public.docs.sections.economy.title')}
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}} dangerouslySetInnerHTML={{ __html: t('web.public.docs.sections.economy.desc') }} />
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#fbbf24'}}>
              <Target size={24} /> {t('web.public.docs.sections.quests.title')}
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}} dangerouslySetInnerHTML={{ __html: t('web.public.docs.sections.quests.desc') }} />
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#f87171'}}>
              <Pickaxe size={24} /> {t('web.public.docs.sections.jobs.title')}
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}} dangerouslySetInnerHTML={{ __html: t('web.public.docs.sections.jobs.desc') }} />
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#c084fc'}}>
              <Map size={24} /> {t('web.public.docs.sections.bluemap.title')}
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}} dangerouslySetInnerHTML={{ __html: t('web.public.docs.sections.bluemap.desc') }} />
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#fb7185'}}>
              <Sword size={24} /> {t('web.public.docs.sections.loot.title')}
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}} dangerouslySetInnerHTML={{ __html: t('web.public.docs.sections.loot.desc') }} />
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#3b82f6'}}>
              <Shield size={24} /> {t('web.public.docs.sections.guilds.title')}
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}} dangerouslySetInnerHTML={{ __html: t('web.public.docs.sections.guilds.desc') }} />
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#8b5cf6'}}>
              <Package size={24} /> {t('web.public.docs.sections.locks.title')}
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}} dangerouslySetInnerHTML={{ __html: t('web.public.docs.sections.locks.desc') }} />
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#6366f1'}}>
              <Map size={24} /> {t('web.public.docs.sections.discord.title')}
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}} dangerouslySetInnerHTML={{ __html: t('web.public.docs.sections.discord.desc') }} />
          </div>
        </div>
      </main>
    </div>
  );
}

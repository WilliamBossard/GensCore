import { Link } from 'react-router-dom';
import { Package, Shield, Target, Pickaxe, Map, ShoppingCart, Sword } from 'lucide-react';

export function Docs() {
  return (
    <div style={{minHeight: '100vh', background: 'var(--bg-color)', color: 'var(--text-color)', display: 'flex', flexDirection: 'column'}}>
      <header style={{padding: '20px 40px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(9, 9, 11, 0.8)', backdropFilter: 'blur(10px)', borderBottom: '1px solid var(--card-border)', position: 'sticky', top: 0, zIndex: 100}}>
        <div style={{display: 'flex', alignItems: 'center', gap: '15px'}}>
          <Package size={32} color="var(--accent)" />
          <h1 style={{margin: 0, fontSize: '1.5rem', fontFamily: 'Outfit'}}>GensCore Docs</h1>
        </div>
        <Link to="/" className="btn" style={{background: 'var(--card-bg)', border: '1px solid var(--card-border)'}}>Retour à l'accueil</Link>
      </header>

      <main style={{flex: 1, padding: '4rem 2rem', maxWidth: '800px', margin: '0 auto', width: '100%'}}>
        <div style={{marginBottom: '3rem'}}>
          <h2 style={{fontSize: '3rem', marginBottom: '1rem', background: 'linear-gradient(to right, #a855f7, #3b82f6)', WebkitBackgroundClip: 'text', color: 'transparent'}}>Documentation Complète</h2>
          <p style={{fontSize: '1.2rem', color: 'var(--text-muted)'}}>Découvrez toutes les fonctionnalités exclusives du serveur Gens.</p>
        </div>

        <div style={{display: 'flex', flexDirection: 'column', gap: '2rem'}}>
          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#60a5fa'}}>
              <Shield size={24} /> Système de Gestion Autonome
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}}>
              Le serveur dispose d'un système de panel web unique permettant au propriétaire de gérer les joueurs, la boutique, l'économie, et les paramètres du serveur <strong>en temps réel</strong> sans avoir besoin de redémarrer.
            </p>
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#34d399'}}>
              <ShoppingCart size={24} /> Économie & Boutique Dynamique
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}}>
              L'économie réagit aux actions des joueurs. Les prix de la boutique (<code>/shop</code>) s'ajustent automatiquement selon l'offre et la demande. Vous pouvez aussi vendre vos objets entre joueurs via l'Hôtel des Ventes (<code>/ah</code>).
            </p>
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#fbbf24'}}>
              <Target size={24} /> Quêtes Quotidiennes
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}}>
              Des défis quotidiens (<code>/quests</code>) sont générés pour chaque joueur. Tuez des monstres, minez des blocs ou craftez des objets pour gagner de l'argent et de l'expérience de métier.
            </p>
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#f87171'}}>
              <Pickaxe size={24} /> Métiers & Spawners
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}}>
              Progressez dans vos métiers (<code>/jobs</code>) pour devenir le meilleur artisan du serveur. De plus, vous avez la capacité de récupérer les <strong>Spawners</strong> avec une pioche Silk Touch et de les replacer n'importe où pour vos fermes personnelles !
            </p>
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#c084fc'}}>
              <Map size={24} /> Intégration BlueMap
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}}>
              Visualisez le monde en 3D depuis votre navigateur grâce à BlueMap. L'intégration au panel permet même aux administrateurs de traquer les joueurs en temps réel si besoin.
            </p>
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#fb7185'}}>
              <Sword size={24} /> Têtes de Monstres & Loot Personnalisé
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}}>
              Collectionnez les têtes des monstres que vous tuez, et profitez d'un système de coffres personnalisés (Loot) instanciés pour chaque joueur. Les premiers arrivés ne volent plus tout le loot !
            </p>
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#3b82f6'}}>
              <Shield size={24} /> Guildes & Quêtes Hebdomadaires
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}}>
              Créez votre propre Guilde (<code>/team</code>) et invitez vos amis. Ensemble, participez à la quête hebdomadaire pour gagner des récompenses exclusives et monter en haut du classement global !
            </p>
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#8b5cf6'}}>
              <Package size={24} /> Verrous de Coffres (Locks)
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}}>
              Sécurisez vos coffres avec la commande <code>/lock</code>. Vous pouvez créer des verrous privés, ou utiliser <code>/lock guild</code> pour partager l'accès à un coffre avec tous les membres de votre guilde.
            </p>
          </div>

          <div className="admin-card" style={{padding: '2rem'}}>
            <h3 style={{display: 'flex', alignItems: 'center', gap: '10px', fontSize: '1.5rem', marginBottom: '1rem', color: '#6366f1'}}>
              <Map size={24} /> Pont Discord-Minecraft
            </h3>
            <p style={{color: 'var(--text-muted)', lineHeight: '1.6'}}>
              Le serveur Discord et le jeu sont entièrement reliés. Vous pouvez lier votre compte via <code>/discord link</code>. Le chat est synchronisé dans les deux sens et affiche même vos grades et votre guilde !
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}

import { defineConfig } from 'vitepress'

export default defineConfig({
  base: '/GensCore/',
  cleanUrls: true,

  head: [
    ['link', { rel: 'icon', href: '/GensCore/icon.png' }],
    [
      'script',
      {},
      `
      (function() {
        var base = window.location.pathname.indexOf('/GensCore/') === 0 ? '/GensCore/' : '/';
        var path = window.location.pathname;
        var isFr = path.indexOf(base + 'fr/') === 0 || path === base + 'fr' || path === base + 'fr.html';
        var savedLang = null;
        try {
          savedLang = localStorage.getItem('genscore_lang');
        } catch (e) {}

        if (!savedLang) {
          var navLang = (navigator.languages && navigator.languages[0]) || navigator.language || '';
          if (navLang.toLowerCase().indexOf('fr') === 0) {
            try { localStorage.setItem('genscore_lang', 'fr'); } catch(e) {}
            if (!isFr) {
              var target = (path === base || path === base + 'index.html') ? base + 'fr/' : path.replace(base, base + 'fr/');
              window.location.replace(target);
            }
          } else {
            try { localStorage.setItem('genscore_lang', 'en'); } catch(e) {}
          }
        } else if (savedLang === 'fr' && !isFr && (path === base || path === base + 'index.html')) {
          window.location.replace(base + 'fr/');
        }
      })();
      `
    ]
  ],

  locales: {
    root: {
      label: 'English',
      lang: 'en-US',
      title: 'GensCore',
      description: 'The Ultimate Paper & Folia Core Plugin for Minecraft 26.2+ and Java 25',
      themeConfig: {
        nav: [
          { text: 'Getting Started', link: '/guide/getting-started' },
          { text: 'Commands', link: '/guide/commands' },
          { text: 'Permissions', link: '/guide/permissions' },
          { text: 'Modules', link: '/guide/modules' },
          { text: 'Web Panel', link: '/guide/web-panel' },
          { text: 'Config', link: '/guide/configuration' }
        ],
        sidebar: [
          {
            text: 'Overview',
            items: [
              { text: 'Introduction & Setup', link: '/guide/getting-started' },
              { text: 'Configuration & Network', link: '/guide/configuration' }
            ]
          },
          {
            text: 'Reference',
            items: [
              { text: 'All Commands', link: '/guide/commands' },
              { text: 'Permissions & LuckPerms', link: '/guide/permissions' }
            ]
          },
          {
            text: 'Deep-Dive Features',
            items: [
              { text: 'In-Game Modules (28)', link: '/guide/modules' },
              { text: 'Web Panel & Minigames', link: '/guide/web-panel' }
            ]
          }
        ],
        docFooter: {
          prev: 'Previous page',
          next: 'Next page'
        }
      }
    },
    fr: {
      label: 'Français',
      lang: 'fr-FR',
      link: '/fr/',
      title: 'GensCore',
      description: 'Le plugin Core ultime Paper & Folia pour Minecraft 26.2+ et Java 25',
      themeConfig: {
        nav: [
          { text: 'Démarrage', link: '/fr/guide/getting-started' },
          { text: 'Commandes', link: '/fr/guide/commands' },
          { text: 'Permissions', link: '/fr/guide/permissions' },
          { text: 'Modules', link: '/fr/guide/modules' },
          { text: 'Panel Web', link: '/fr/guide/web-panel' },
          { text: 'Configuration', link: '/fr/guide/configuration' }
        ],
        sidebar: [
          {
            text: 'Vue d\'ensemble',
            items: [
              { text: 'Introduction & Installation', link: '/fr/guide/getting-started' },
              { text: 'Configuration & Réseau', link: '/fr/guide/configuration' }
            ]
          },
          {
            text: 'Référence',
            items: [
              { text: 'Toutes les Commandes', link: '/fr/guide/commands' },
              { text: 'Permissions & LuckPerms', link: '/fr/guide/permissions' }
            ]
          },
          {
            text: 'Fonctionnalités avancées',
            items: [
              { text: 'Modules en jeu (28)', link: '/fr/guide/modules' },
              { text: 'Panel Web & Mini-jeux', link: '/fr/guide/web-panel' }
            ]
          }
        ],
        docFooter: {
          prev: 'Page précédente',
          next: 'Page suivante'
        }
      }
    }
  },

  themeConfig: {
    logo: '/icon.png',
    siteTitle: 'GensCore',

    search: {
      provider: 'local',
      options: {
        locales: {
          fr: {
            translations: {
              button: {
                buttonText: 'Rechercher',
                buttonAriaLabel: 'Rechercher'
              },
              modal: {
                displayDetails: 'Afficher les détails',
                resetButtonTitle: 'Effacer la recherche',
                backButtonTitle: 'Retour',
                noResultsText: 'Aucun résultat pour',
                footer: {
                  selectText: 'choisir',
                  navigateText: 'naviguer',
                  closeText: 'fermer'
                }
              }
            }
          }
        }
      }
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/WilliamBossard/GensCore' }
    ],

    footer: {
      message: 'Released under the MIT License. Developed with passion for GensBien.',
      copyright: 'Copyright © 2026 William Bossard'
    }
  }
})

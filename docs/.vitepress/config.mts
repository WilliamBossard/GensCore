import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'GensCore',
  description: 'The Ultimate Paper & Folia Core Plugin for Minecraft 26.2+ and Java 25',
  base: '/GensCore/',
  cleanUrls: true,

  head: [
    ['link', { rel: 'icon', href: 'https://img.shields.io/badge/GensCore-Paper%20%26%20Folia-green.svg' }]
  ],

  themeConfig: {
    logo: 'https://raw.githubusercontent.com/WilliamBossard/GensCore/dev/src/main/resources/icon.png',
    siteTitle: 'GensCore',

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

    search: {
      provider: 'local',
      options: {
        placeholder: 'Search documentation...'
      }
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/WilliamBossard/GensCore' }
    ],

    footer: {
      message: 'Released under the MIT License. Developed with passion for GensBien.',
      copyright: 'Copyright © 2026 William Bossard'
    },

    docFooter: {
      prev: 'Previous page',
      next: 'Next page'
    }
  }
})

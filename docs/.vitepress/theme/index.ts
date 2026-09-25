import DefaultTheme from 'vitepress/theme'
import './custom.css'

export default {
  extends: DefaultTheme,
  enhanceApp({ router }) {
    if (typeof window !== 'undefined') {
      const originalOnAfterRouteChanged = router.onAfterRouteChanged
      router.onAfterRouteChanged = (to: string) => {
        if (originalOnAfterRouteChanged) {
          originalOnAfterRouteChanged(to)
        }
        const isFr = to.includes('/fr/') || to.endsWith('/fr') || to.endsWith('/fr.html')
        try {
          localStorage.setItem('genscore_lang', isFr ? 'fr' : 'en')
        } catch (e) {}
      }
    }
  }
}

import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import Backend from 'i18next-http-backend';
import LanguageDetector from 'i18next-browser-languagedetector';

i18n
  .use(Backend)
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'fr_FR',
    backend: {
      loadPath: '/api/public/lang?lang={{lng}}',
    },
    interpolation: {
      escapeValue: false, // React gère déjà l'XSS
    },
  });

export default i18n;

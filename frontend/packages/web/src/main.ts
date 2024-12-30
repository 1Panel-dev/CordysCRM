import { createApp } from 'vue';
import { createPinia } from 'pinia';

// TODO 国际化接口对接
// import localforage from 'localforage';
import './assets/main.css';

import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
import App from './App.vue';

import { setupI18n } from './locale';
import useLocale from './locale/useLocale';
import router from './router';

async function setupApp() {
  const app = createApp(App);

  app.use(createPinia());
  // 注册国际化，需要异步阻塞，确保语言包加载完毕
  await setupI18n(app);

  app.component('CrmIcon', CrmIcon);

  // 获取默认语言
  const localLocale = localStorage.getItem('CRM-locale');
  if (!localLocale) {
    // TODO 国际化接口对接
    // const defaultLocale = await getDefaultLocale();
    const { changeLocale } = useLocale();
    changeLocale('zh-CN');
  }

  app.use(router);

  app.mount('#app');
}

setupApp();

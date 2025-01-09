import { createApp } from 'vue';

// TODO 国际化接口对接
// import localforage from 'localforage';
import '@/assets/style/index.less';

import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
import App from './App.vue';

// eslint-disable-next-line import/no-unresolved
import 'virtual:svg-icons-register';
import { setupI18n } from './locale';
import useLocale from './locale/useLocale';
import router from './router';
import store from './store';

async function setupApp() {
  const app = createApp(App);

  app.use(store);
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

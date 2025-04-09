import { createApp } from 'vue';
import { showLoadingToast } from 'vant';

import '@/assets/icon-font/iconfont';
import '@/assets/icon-font/iconfont.css';
import '@/assets/style/index.less';

import { setupI18n } from '@lib/shared/locale';
import useLocale from '@lib/shared/locale/useLocale';

import App from './App.vue';

import 'vant/es/toast/style'; // 解决toast样式被popup覆盖的问题
// eslint-disable-next-line import/no-unresolved
import 'virtual:svg-icons-register';
import router from './router';
import store from './store';

async function setupApp() {
  const app = createApp(App);

  app.use(store);
  // 注册国际化，需要异步阻塞，确保语言包加载完毕
  await setupI18n(app);

  // 获取默认语言
  const localLocale = localStorage.getItem('CRM-locale');
  if (!localLocale) {
    // TODO 国际化接口对接
    // const defaultLocale = await getDefaultLocale();
    const { changeLocale } = useLocale(showLoadingToast);
    changeLocale('zh-CN');
  }

  app.use(router);
  app.mount('#app');
}

setupApp();

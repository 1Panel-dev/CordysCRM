import { App } from 'vue';

import permission from './permission';
import validateExpiration from './validateExpiration';

export default {
  install(Vue: App) {
    Vue.directive('permission', permission);
    Vue.directive('expire', validateExpiration);
  },
};

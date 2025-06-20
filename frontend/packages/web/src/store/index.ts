import { createPinia } from 'pinia';

import useAppStore from './modules/app';
import useVisitStore from './modules/app/visit';
import useUserStore from './modules/user';
import { debouncePlugin } from './plugins';
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';

const pinia = createPinia().use(debouncePlugin).use(piniaPluginPersistedstate);
export { useAppStore, useUserStore, useVisitStore };

export default pinia;

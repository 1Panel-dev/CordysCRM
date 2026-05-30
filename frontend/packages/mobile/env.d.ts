/// <reference types="vite/client" />

declare module '*.vue' {
  import { DefineComponent } from 'vue';

  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  const component: DefineComponent<{}, {}, any>;
  export default component;
}
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_DEV_DOMAIN: string; // 开发环境域名
}

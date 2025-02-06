export interface LoginParams {
  username: string;
  password: string;
  authenticate: string;
  loginAddress?: string;
  platform: 'WEB' | 'MOBILE'; // 平台,WEB\MOBILE
}

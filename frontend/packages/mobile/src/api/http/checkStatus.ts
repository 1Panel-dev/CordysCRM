import { showFailToast } from 'vant';

import { useI18n } from '@lib/shared/hooks/useI18n';

import router from '@/router';
import { NO_RESOURCE_ROUTE_NAME } from '@/router/constants';

export default function checkStatus(status: number, msg: string, code?: number): void {
  const { t } = useI18n();
  let errMessage = '';
  switch (status) {
    case 400:
      errMessage = `${msg}`;
      break;
    case 401: {
      errMessage = msg || t('api.errMsg401');
      // TODO: 401处理
      break;
    }
    case 403:
      if (router.currentRoute.value.name !== NO_RESOURCE_ROUTE_NAME) {
        router.push({ name: NO_RESOURCE_ROUTE_NAME });
      }
      break;
    // 404请求不存在
    case 404:
      errMessage = msg || t('api.errMsg404');
      break;
    case 405:
      errMessage = msg || t('api.errMsg405');
      break;
    case 408:
      errMessage = msg || t('api.errMsg408');
      break;
    case 500:
      // 101003 用于查重功能，表示模块未开启，不需要弹出消息
      if (code === 101003) {
        errMessage = '';
      } else {
        errMessage = msg || t('api.errMsg500');
      }
      break;
    case 501:
      errMessage = msg || t('api.errMsg501');
      break;
    case 502:
      errMessage = msg || t('api.errMsg502');
      break;
    case 503:
      errMessage = msg || t('api.errMsg503');
      break;
    case 504:
      errMessage = msg || t('api.errMsg504');
      break;
    case 505:
      errMessage = msg || t('api.errMsg505');
      break;
    default:
  }
  if (errMessage) {
    showFailToast(errMessage);
  }
}

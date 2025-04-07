import { showFailToast } from 'vant';

import createAxios from '@lib/shared/api/http';

import useAppStore from '@/store/modules/app';

import checkStatus from './checkStatus';

const CDR = createAxios({
  useAppStore,
  showErrorMsg: showFailToast,
  checkStatus,
});

export default CDR;

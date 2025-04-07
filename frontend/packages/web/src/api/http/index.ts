import createAxios from '@lib/shared/api/http';

import useDiscreteApi from '@/hooks/useDiscreteApi';
import useAppStore from '@/store/modules/app';

import checkStatus from './checkStatus';

const { message } = useDiscreteApi();

const CDR = createAxios({
  useAppStore,
  showErrorMsg: message.error,
  checkStatus,
});

export default CDR;

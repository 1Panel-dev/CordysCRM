import { type NavigationGuardNext, onBeforeRouteLeave } from 'vue-router';

import { useI18n } from '@lib/shared/hooks/useI18n';

import useModal, { type DialogType } from '@/hooks/useModal';
import useAppStore from '@/store/modules/app';

export interface LeaveProps {
  leaveTitle: string;
  leaveContent: string;
  tipType: DialogType;
}

const leaveProps: LeaveProps = {
  leaveTitle: 'common.unSaveLeaveTitle',
  leaveContent: 'common.unSaveLeaveContent',
  tipType: 'warning',
};

// 离开页面确认提示
export default function useLeaveUnSaveTip(leaveProp = leaveProps) {
  const appStore = useAppStore();
  const { openModal } = useModal();
  const { t } = useI18n();
  const { leaveTitle, leaveContent, tipType } = leaveProp;

  const isSave = ref(true);

  const setIsSave = (flag: boolean) => {
    isSave.value = flag;
  };

  function openUnsavedTip(next: NavigationGuardNext | (() => void)) {
    openModal({
      type: tipType,
      title: t(leaveTitle),
      content: t(leaveContent),
      positiveText: t('common.leave'),
      negativeText: t('common.stay'),
      okButtonProps: {
        status: 'normal',
      },
      onPositiveClick: async () => {
        isSave.value = true;
        next();
      },
      hideCancel: false,
    });
  }

  onBeforeRouteLeave((to, from, next) => {
    if (to.path === from.path) {
      next();
      return;
    }

    if (!isSave.value) {
      openUnsavedTip(next);
      appStore.setRestoreMenuTimeStamp(Date.now());
    } else {
      next();
    }
  });

  // 页面有变更时，关闭或刷新页面弹出浏览器的保存提示
  window.onbeforeunload = () => {
    if (!isSave.value) {
      return '';
    }
  };
  return {
    setIsSave,
    openUnsavedTip,
    isSave,
  };
}

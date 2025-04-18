<template>
  <div id="wecom-qr" class="wecom-qr"> </div>
</template>

<script lang="ts" setup>
  import { useRouter } from 'vue-router';
  import { showFailToast, showLoadingToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { setLoginExpires, setLoginType } from '@lib/shared/method/auth';

  import { getThirdConfigByType, getWeComCallback } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  import { AppRouteEnum } from '@/enums/routeEnum';

  import * as ww from '@wecom/jssdk';
  import { WWLoginErrorResp, WWLoginPanelSizeType, WWLoginRedirectType, WWLoginType } from '@wecom/jssdk';

  const { t } = useI18n();

  const userStore = useUserStore();
  const router = useRouter();

  const wwLogin = ref({});

  const obj = ref<any>({
    isWeComLogin: false,
  });

  const init = async () => {
    showLoadingToast(t('common.login'));
    const data = await getThirdConfigByType('WECOM');
    wwLogin.value = ww.createWWLoginPanel({
      el: '#wecom-qr',
      params: {
        login_type: WWLoginType.corpApp,
        appid: data.corpId ? data.corpId : '',
        agentid: data.agentId,
        redirect_uri: window.location.origin,
        state: 'fit2cloud-wecom-qr',
        redirect_type: WWLoginRedirectType.callback,
        panel_size: WWLoginPanelSizeType.small,
      },
      onCheckWeComLogin: obj.value,
      async onLoginSuccess({ code }: any) {
        const weComCallback = getWeComCallback(code);
        const boolean = userStore.qrCodeLogin(await weComCallback);
        if (boolean) {
          setLoginExpires();
          setLoginType('QR_CODE');
          const { redirect, ...othersQuery } = router.currentRoute.value.query;
          router.push({
            name: (redirect as string) || AppRouteEnum.WORKBENCH,
            query: {
              ...othersQuery,
            },
          });
        }
      },
      onLoginFail(err: WWLoginErrorResp) {
        showFailToast(`errorMsg of errorCbk: ${err.errMsg}`);
      },
    });
  };
  if (import.meta.env.DEV) {
    router.push({
      name: AppRouteEnum.WORKBENCH,
    });
  } else {
    init();
  }
</script>

<style lang="less" scoped></style>

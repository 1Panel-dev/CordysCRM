<template>
  <div id="wecom-qr" class="wecom-qr" />
</template>

<script lang="ts" setup>
  import { useRouter } from 'vue-router';
  import { useMessage } from 'naive-ui';

  import { setLoginExpires, setLoginType } from '@lib/shared/method/auth';

  import { getThirdConfigByType } from '@/api/modules/system/business';
  import { getWeComCallback } from '@/api/modules/system/login';
  import { useI18n } from '@/hooks/useI18n';
  import { NO_RESOURCE_ROUTE_NAME } from '@/router/constants';
  import useUserStore from '@/store/modules/user';

  import * as ww from '@wecom/jssdk';
  import { WWLoginErrorResp, WWLoginPanelSizeType, WWLoginRedirectType, WWLoginType } from '@wecom/jssdk';

  const { t } = useI18n();

  const userStore = useUserStore();
  const router = useRouter();
  const Message = useMessage();

  const wwLogin = ref({});

  const obj = ref<any>({
    isWeComLogin: false,
  });

  const init = async () => {
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
        userStore.qrCodeLogin(await weComCallback);
        setLoginType('WE_COM');
        Message.success(t('login.form.login.success'));
        const { redirect, ...othersQuery } = router.currentRoute.value.query;
        setLoginExpires();
        router.push({
          name: (redirect as string) || NO_RESOURCE_ROUTE_NAME,
          query: {
            ...othersQuery,
          },
        });
      },
      onLoginFail(err: WWLoginErrorResp) {
        Message.error(`errorMsg of errorCbk: ${err.errMsg}`);
      },
    });
  };

  init();
</script>

<style lang="less" scoped>
  .wecom-qr {
    margin-top: -20px;
  }
</style>

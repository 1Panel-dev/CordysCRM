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
  import useLoading from '@/hooks/useLoading';
  import useUserStore from '@/store/modules/user';

  import { AppRouteEnum } from '@/enums/routeEnum';

  import * as ww from '@wecom/jssdk';
  import { WWLoginErrorResp, WWLoginPanelSizeType, WWLoginRedirectType, WWLoginType } from '@wecom/jssdk';

  const { t } = useI18n();
  const { setLoading } = useLoading();

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
        const boolean = userStore.qrCodeLogin(await weComCallback);
        if (boolean) {
          setLoginExpires();
          setLoginType('QR_CODE');
          Message.success(t('login.form.login.success'));
          const { redirect, ...othersQuery } = router.currentRoute.value.query;
          router.push({
            name: (redirect as string) || AppRouteEnum.SYSTEM,
            query: {
              ...othersQuery,
            },
          });
        }
        setLoading(false);
        userStore.getAuthentication();
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

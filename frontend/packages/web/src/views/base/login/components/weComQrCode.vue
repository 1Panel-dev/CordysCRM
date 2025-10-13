<template>
  <div id="wecom-qr" class="wecom-qr" />
</template>

<script lang="ts" setup>
  import { useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { setLoginExpires, setLoginType } from '@lib/shared/method/auth';

  import { getThirdConfigByType, getThirdCallback } from '@/api/modules';
  import useLoading from '@/hooks/useLoading';
  import useUser from '@/hooks/useUser';
  import useUserStore from '@/store/modules/user';

  import * as ww from '@wecom/jssdk';
  import { WWLoginErrorResp, WWLoginPanelSizeType, WWLoginRedirectType, WWLoginType } from '@wecom/jssdk';

  const { goUserHasPermissionPage } = useUser();

  const { t } = useI18n();
  const { setLoading } = useLoading();

  const userStore = useUserStore();
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
        const weComCallback = getThirdCallback(code, 'wecom');
        const boolean = userStore.qrCodeLogin(await weComCallback);
        if (boolean) {
          setLoginExpires();
          setLoginType('WECOM');
          Message.success(t('login.form.login.success'));
          goUserHasPermissionPage();
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

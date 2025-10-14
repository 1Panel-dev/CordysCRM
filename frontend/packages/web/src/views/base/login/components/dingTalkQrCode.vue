<template>
  <div id="ding-talk-qr" class="ding-talk-qrName" />
</template>

<script lang="ts" setup>
  import { useScriptTag } from '@vueuse/core';
  import { useMessage } from 'naive-ui';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { setLoginExpires, setLoginType } from '@lib/shared/method/auth';

  import { getThirdCallback, getThirdConfigByType } from '@/api/modules';
  import useLoading from '@/hooks/useLoading';
  import useUser from '@/hooks/useUser';
  import useUserStore from '@/store/modules/user';

  const { t } = useI18n();

  const userStore = useUserStore();
  const { goUserHasPermissionPage } = useUser();
  const { setLoading } = useLoading();

  const Message = useMessage();

  const { load } = useScriptTag('https://g.alicdn.com/dingding/h5-dingtalk-login/0.21.0/ddlogin.js');

  const initActive = async () => {
    const data = await getThirdConfigByType(CompanyTypeEnum.DINGTALK);

    await load(true);
    const url = encodeURIComponent(window.location.origin);
    window.DTFrameLogin(
      {
        id: 'ding-talk-qr',
        width: 300,
        height: 300,
      },
      {
        redirect_uri: url,
        client_id: data.agentId ? data.agentId : '',
        scope: 'openid',
        response_type: 'code',
        state: 'fit2cloud-ding-qr',
        prompt: 'consent',
        corpId: data.corpId ? data.corpId : '',
      },
      async (loginResult) => {
        const { authCode } = loginResult;
        const dingCallback = getThirdCallback(authCode, 'ding-talk');
        const boolean = await userStore.qrCodeLogin(await dingCallback);
        if (boolean) {
          setLoginExpires();
          setLoginType('DINGTALK');
          Message.success(t('login.form.login.success'));
          goUserHasPermissionPage();
        }
        setLoading(false);
        userStore.getAuthentication();
        // 也可以在不跳转页面的情况下，使用code进行授权
      },
      (errorMsg) => {
        // 这里一般需要展示登录失败的具体原因,可以使用toast等轻提示
        Message.error(`errorMsg of errorCbk: ${errorMsg}`);
      }
    );
  };
  onMounted(() => {
    initActive();
  });
</script>

<style lang="less" scoped>
  .ding-talk-qrName {
    width: 300px;
    height: 300px;
  }
</style>

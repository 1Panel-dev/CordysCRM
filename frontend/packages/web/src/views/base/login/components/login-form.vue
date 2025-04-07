<template>
  <div
    v-if="preheat && !props.isPreview"
    class="login-form"
    :style="props.isPreview ? 'height: inherit' : 'height: 100vh'"
  >
    <n-spin :loading="preheat" :description="t('login.form.loading')"> </n-spin>
  </div>
  <div v-else class="login-form" :style="props.isPreview ? 'height: inherit' : 'height: 100vh'">
    <div class="title">
      <div class="flex justify-center">
        <img :src="innerLogo" class="h-[60px] w-[290px]" />
      </div>
      <div class="title-0 mt-[16px] flex justify-center">
        <span class="title-welcome one-line-text max-w-[300px]">
          {{ t(innerSlogan || '') || t('login.form.title') }}
        </span>
      </div>
    </div>

    <div class="form mt-[32px] min-w-[480px]">
      <div v-if="userInfo.authenticate === 'LOCAL'" class="mb-7 text-[18px] font-medium text-[var(--primary-8)]">
        {{ t('login.form.accountLogin') }}
      </div>
      <div
        v-if="isShowLDAP && userInfo.authenticate === 'LDAP'"
        class="mb-7 text-[18px] font-medium text-[var(--primary-8)]"
        >{{ t('login.form.LDAPLogin') }}</div
      >
      <n-form
        v-if="!showQrCodeTab"
        ref="formRef"
        :model="userInfo"
        :rules="{
          username: [{ required: true, message: t('login.form.userName.errMsg'), trigger: ['input', 'blur'] }],
          password: [{ required: true, message: t('login.form.password.errMsg'), trigger: ['input', 'blur'] }],
        }"
      >
        <!-- TOTO 第一版本暂时只考虑普通登录&LDAP -->
        <n-form-item class="login-form-item" path="username" :show-label="false">
          <n-input
            v-model:value="userInfo.username"
            class="login-input"
            :max-length="64"
            size="large"
            :placeholder="
              userInfo.authenticate !== 'LOCAL'
                ? t('login.form.userName.placeholderOther')
                : t('login.form.userName.placeholder')
            "
          />
        </n-form-item>
        <n-form-item class="login-form-item" path="password" :show-label="false">
          <n-input
            v-model:value="userInfo.password"
            type="password"
            class="login-password-input"
            :placeholder="t('login.form.password.placeholder')"
            clearable
            :max-length="64"
            size="large"
            show-password-on="click"
            @keydown.enter="handleSubmit"
          />
        </n-form-item>
        <div class="mt-[12px]" :class="hasMoreLoginWay ? 'mb-[60px]' : 'mb-7'">
          <n-button type="primary" size="large" block :loading="loading" @click="handleSubmit">
            {{ t('login.form.login') }}
          </n-button>
          <div v-if="showDemo" class="mb-[-16px] mt-[16px] flex items-center gap-[16px]">
            <div class="flex items-center">
              <div>{{ t('login.form.username') }}：</div>
              <div>demo</div>
            </div>
            <div class="flex items-center">
              <div>{{ t('login.form.password') }}：</div>
              <div>demo</div>
            </div>
          </div>
        </div>
      </n-form>
      <div v-if="showQrCodeTab">
        <tab-qr-code :tab-name="activeName ? activeName : orgOptions[0].value"></tab-qr-code>
      </div>
      <template v-if="hasMoreLoginWay">
        <n-divider orientation="center" type="dashed" class="!m-0 !mb-2">
          <span class="text-[12px] font-normal text-[var(--text-n4)]">{{ t('login.form.modeLoginMethods') }}</span>
        </n-divider>
        <div class="mt-4 flex items-center justify-center">
          <div
            v-if="isShowQRCode && !showQrCodeTab && orgOptions.length > 0"
            class="loginType"
            @click="switchLoginType('QR_CODE')"
          >
            <CrmIcon type="iconicon_scan" class="text-[var(--primary-8)]" />
          </div>
          <div v-if="userInfo.authenticate !== 'LDAP' && isShowLDAP" class="loginType" @click="switchLoginType('LDAP')">
            <span class="type-text text-[10px]">LDAP</span>
          </div>
          <div v-if="userInfo.authenticate !== 'LOCAL'" class="loginType" @click="switchLoginType('LOCAL')">
            <CrmSvg width="18px" height="18px" name="userLogin"></CrmSvg>
          </div>
          <div v-if="isShowOIDC && userInfo.authenticate !== 'OIDC'" class="loginType" @click="redirectAuth('OIDC')">
            <span class="type-text text-[10px]">OIDC</span>
          </div>
          <div
            v-if="isShowOAUTH && userInfo.authenticate !== 'OAUTH2'"
            class="loginType"
            @click="redirectAuth('OAUTH2')"
          >
            <span class="type-text text-[10px]">OAuth</span>
          </div>
          <div v-if="isShowCAS && userInfo.authenticate !== 'CAS'" class="loginType" @click="redirectAuth('CAS')">
            <span class="type-text text-[10px]">CAS</span>
          </div>
        </div>
      </template>
      <div v-if="props.isPreview" class="mask"></div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { useStorage } from '@vueuse/core';
  import { FormInst, NButton, NDivider, NForm, NFormItem, NInput, NSpin, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getLoginType, setLoginExpires, setLoginType } from '@lib/shared/method/auth';
  import { encrypted } from '@lib/shared/method/index';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSvg from '@/components/pure/crm-svg/index.vue';
  import TabQrCode from './tabQrCode.vue';

  // import { getAuthDetailByType } from '@/api/modules/setting/config';
  // import { getPlatformParamUrl } from '@/api/modules/user';
  // import { GetLoginLogoUrl } from '@/api/requrls/setting/config';
  import { getThirdTypeList } from '@/api/modules';
  import useLoading from '@/hooks/useLoading';
  import useAppStore from '@/store/modules/app';
  // import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  import { AppRouteEnum } from '@/enums/routeEnum';

  import { Option } from 'naive-ui/es/legacy-transfer/src/interface';

  const router = useRouter();
  const { t } = useI18n();
  const appStore = useAppStore();
  const userStore = useUserStore();
  const Message = useMessage();
  // const { openModal } = useModal();

  const formRef = ref<FormInst | null>(null);
  const orgOptions = ref<Option[]>([]);

  const preheat = ref(true);

  const props = defineProps<{
    isPreview?: boolean;
    slogan?: string;
    logo?: string;
  }>();

  const innerLogo = computed(() => {
    return props.isPreview && props.logo ? props.logo : '/images/login-logo.svg'; // TODO: GetLoginLogoUrl();
  });

  const innerSlogan = computed(() => {
    return props.isPreview ? props.slogan : ''; // TODO: GetLoginSlogan();
  });

  const showDemo = window.location.hostname === 'demo.metersphere.com';

  const { loading, setLoading } = useLoading();

  const loginConfig = useStorage('login-config', {
    rememberPassword: true,
    username: '',
    password: '',
  });

  const userInfo = ref<{
    authenticate: string;
    username: string;
    password: string;
  }>({
    authenticate: getLoginType() || 'LOCAL',
    username: '',
    password: '',
  });

  const showQrCodeTab = ref(false);
  const activeName = ref('');

  function switchLoginType(type: string) {
    userInfo.value.authenticate = type;
    if (type === 'QR_CODE') {
      showQrCodeTab.value = showQrCodeTab.value === false;
    } else {
      showQrCodeTab.value = false;
    }
  }

  const handleSubmit = () => {
    if (loading.value) return;
    formRef.value?.validate(async (errors) => {
      if (!errors) {
        setLoading(true);
        try {
          try {
            await userStore.logout(true); // 登录之前先注销，防止未登出就继续登录导致报错
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log('logout error', error);
          }
          await userStore.login({
            username: encrypted(userInfo.value.username) || '',
            password: encrypted(userInfo.value.password) || '',
            authenticate: userInfo.value.authenticate,
            platform: 'WEB',
          });
          setLoginExpires();
          setLoginType(userInfo.value.authenticate);
          Message.success(t('login.form.login.success'));
          const { rememberPassword } = loginConfig.value;
          const { username, password } = userInfo.value;
          loginConfig.value.username = rememberPassword ? username : '';
          loginConfig.value.password = rememberPassword ? password : '';
          const { redirect, ...othersQuery } = router.currentRoute.value.query;
          router.push({
            name: (redirect as string) || AppRouteEnum.SYSTEM,
            query: {
              ...othersQuery,
            },
          });
        } catch (err) {
          // eslint-disable-next-line no-console
          console.log(err);
        } finally {
          setLoading(false);
          userStore.getAuthentication();
        }
      }
    });
  };

  const isShowLDAP = computed(() => {
    return userStore.loginType.includes('LDAP');
  });
  const isShowOIDC = computed(() => {
    return userStore.loginType.includes('OIDC');
  });
  const isShowOAUTH = computed(() => {
    return userStore.loginType.includes('OAUTH2');
  });
  const isShowCAS = computed(() => {
    return userStore.loginType.includes('CAS');
  });

  const isShowQRCode = ref(true);
  const hasMoreLoginWay = computed(
    () =>
      isShowLDAP.value ||
      isShowOIDC.value ||
      isShowOAUTH.value ||
      isShowCAS.value ||
      (isShowQRCode.value && orgOptions.value.length > 0)
  );

  async function initPlatformInfo() {
    try {
      const res = await getThirdTypeList();
      if (getLoginType() && getLoginType() !== 'LOCAL' && getLoginType() !== 'LDAP') {
        showQrCodeTab.value = true;
        activeName.value = getLoginType() || 'WE_COM';
      }
      orgOptions.value = res.map((e) => ({
        label: e.name,
        value: e.id,
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function redirectAuth(authType: string) {
    if (authType === 'LDAP' || authType === 'LOCAL') {
      return;
    }
    // const res = await getAuthDetailByType(authType);
    const res: any = {}; // TODO: getAuthDetailByType(authType);
    if (!res) {
      return;
    }
    if (!res.enable) {
      Message.error(t('login.auth_not_enable'));
      return;
    }
    const authId = res.id;
    // openModal({
    //   type: 'info',
    //   title: t('common.auth_redirect_tip'),
    //   content: '',
    //   okText: t('common.jump'),
    //   cancelText: t('common.cancel'),
    //   okButtonProps: {
    //     status: 'normal',
    //   },
    //   mask: true,
    //   maskStyle: { color: '#323233' },
    //   onBeforeOk: async () => {
    //     const config = JSON.parse(res.configuration);
    //     // eslint-disable-next-line no-eval
    //     const redirectUrl = eval(`\`${config.redirectUrl}\``);
    //     setLoginType('LOCAL');
    //     let url;
    //     if (authType === 'CAS') {
    //       url = `${config.loginUrl}?service=${encodeURIComponent(redirectUrl)}`;
    //     }
    //     if (authType === 'OIDC') {
    //       url = `${config.authUrl}?client_id=${config.clientId}&redirect_uri=${redirectUrl}&response_type=code&scope=openid+profile+email&state=${authId}`;
    //       // 保存一个登录地址，禁用本地登录
    //       if (config.loginUrl) {
    //         localStorage.setItem('oidcLoginUrl', config.loginUrl);
    //       }
    //     }
    //     if (authType === 'OAUTH2') {
    //       url =
    //         `${config.authUrl}?client_id=${config.clientId}&response_type=code` +
    //         `&redirect_uri=${redirectUrl}&state=${authId}`;
    //       if (config.scope) {
    //         url += `&scope=${config.scope}`;
    //       }
    //     }
    //     if (url) {
    //       window.location.href = url;
    //     }
    //   },
    //   hideCancel: false,
    // });
  }

  onMounted(async () => {
    if (!props.isPreview) {
      // userStore.getAuthentication();
      initPlatformInfo();
      appStore.initPublicKey();
      try {
        preheat.value = await userStore.isLogin();
      } catch (e) {
        // eslint-disable-next-line no-console
        console.log(e);
        preheat.value = false;
      }
    }
  });
</script>

<style lang="less" scoped>
  /* stylelint-disable color-function-notation */
  .login-form {
    @apply flex flex-1 flex-col items-center justify-center;
    .title-welcome {
      color: var(--primary-8);
    }
    .form {
      @apply relative;

      padding: 40px;
      border-radius: var(--border-radius-large);
      background-color: var(--text-n10);
      box-shadow: 0 8px 10px 0 #3232330d, 0 16px 24px 0 #3232330d, 0 6px 30px 0 #3232330d;
      .login-form-item {
        @apply block;
      }
      .mask {
        @apply absolute left-0 top-0 h-full w-full;
      }
      .loginType {
        margin: 0 8px;
        width: 32px;
        height: 32px;
        border: 1px solid var(--text-n8);
        border-radius: 50%;
        @apply flex cursor-pointer items-center justify-center;
        .type-text {
          color: var(--primary-8);
          @apply font-medium;
        }
      }
    }
  }
  :deep(.arco-divider-text) {
    padding: 0 8px !important;
  }
  .login-input {
    padding-right: 0;
    padding-left: 0;
    width: 400px;
  }
  .login-input :deep(.arco-input) {
    padding-right: 10px;
    padding-left: 10px;
  }
  .login-password-input {
    position: relative;
    padding-right: 0;
    padding-left: 0;
    width: 400px;
    height: 36px;
  }
  .login-password-input :deep(.arco-input) {
    padding-right: 50px;
    padding-left: 10px;
  }
  .login-password-input :deep(.arco-input-clear-btn) {
    position: absolute;
    top: 10px;
    float: right;
    margin-left: 350px;
  }
  .login-password-input :deep(.arco-input-suffix) {
    position: absolute;
    top: 10px;
    float: right;
    margin-left: 360px;
  }
</style>

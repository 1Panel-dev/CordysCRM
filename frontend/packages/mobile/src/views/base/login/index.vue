<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="login-wrapper w-full">
      <CrmPageHeader :title="''" />
      <div class="login-title text-[28px] font-semibold text-[var(--text-n1)]">{{ t('login.form.title') }}</div>
    </div>
    <van-form ref="formRef" required class="crm-form">
      <van-cell-group inset>
        <van-field
          v-model="userInfo.username"
          name="username"
          :label="t('login.form.username')"
          :placeholder="t('login.form.userName.placeholderOther')"
          :rules="[{ required: true, message: t('login.form.userName.errMsg') }]"
          class="!p-[16px] !text-[16px]"
        />
        <van-field
          v-model="userInfo.password"
          name="password"
          :type="visible ? undefined : 'password'"
          :label="t('login.form.password')"
          :placeholder="t('login.form.password.placeholder')"
          :rules="[{ required: true, message: t('login.form.password.errMsg') }]"
          class="!p-[16px] !text-[16px]"
        >
          <template #button>
            <CrmIcon
              :name="visible ? 'iconicon_browse' : 'iconicon_browse_off'"
              width="24px"
              height="24px"
              color="var(--text-n4)"
              @click="handleToggleVisible"
            />
          </template>
        </van-field>
      </van-cell-group>
    </van-form>
    <div class="p-[16px]">
      <van-button block type="primary" :loading="loading" native-type="submit" @click="login">
        {{ t('login.form.login') }}
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInstance } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { clearToken, getLoginType, isLoginExpires, setLoginExpires, setLoginType } from '@lib/shared/method/auth';
  import { encrypted } from '@lib/shared/method/index';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmPageHeader from '@/components/pure/crm-page-header/index.vue';

  import useUser from '@/hooks/useUser';
  import useAppStore from '@/store/modules/app';
  import useUserStore from '@/store/modules/user';

  const { goUserHasPermissionPage } = useUser();

  const appStore = useAppStore();

  const userStore = useUserStore();

  const { t } = useI18n();

  const userInfo = ref<{
    authenticate: string;
    username: string;
    password: string;
  }>({
    authenticate: getLoginType() || 'LOCAL',
    username: '',
    password: '',
  });

  const visible = ref(false);
  function handleToggleVisible() {
    visible.value = !visible.value;
  }
  const formRef = ref<FormInstance>();
  const loading = ref(false);

  async function logout() {
    try {
      await userStore.logout(true);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log('logout error', error);
    }
  }

  async function login() {
    try {
      await formRef.value?.validate();
      loading.value = true;
      logout();

      await userStore.login({
        username: encrypted(userInfo.value.username) || '',
        password: encrypted(userInfo.value.password) || '',
        authenticate: userInfo.value.authenticate,
        platform: 'MOBILE',
      });
      setLoginExpires();
      setLoginType(userInfo.value.authenticate);
      goUserHasPermissionPage();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    appStore.initPublicKey();
    try {
      if (isLoginExpires()) {
        clearToken();
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  });
</script>

<style scoped lang="less">
  .login-wrapper {
    padding: 16px 16px 0 20px;
    height: 150px;
    background-image: url('/images/login_bg.png');
    @apply relative w-full bg-cover bg-no-repeat;
    .login-title {
      bottom: 16px;
      @apply absolute;
    }
  }
  :deep(.crm-page-header) {
    background: transparent;
  }
</style>

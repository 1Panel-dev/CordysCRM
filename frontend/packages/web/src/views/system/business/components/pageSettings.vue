<template>
  <!-- 风格、主题色配置 -->
  <CrmCard class="mb-[16px]" :loading="pageLoading" hide-footer auto-height>
    <div class="flex items-center gap-[16px]">
      <div class="w-[60px]"> {{ t('system.business.page.platformStyle') }}</div>
      <CrmTab v-model:active-tab="pageConfig.theme" no-content :tab-list="themeList" type="segment" />
    </div>
    <div v-if="pageConfig.theme === 'custom'" class="ml-[4px] mt-[4px]">
      <CrmColorSelect key="customTheme" v-model:pure-color="pageConfig.customTheme" />
    </div>
    <div class="mt-[16px] flex items-center gap-[16px]">
      <div class="w-[60px]">{{ t('system.business.page.background') }}</div>
      <CrmTab v-model:active-tab="pageConfig.style" no-content :tab-list="styleList" type="segment" />
    </div>
    <div v-if="pageConfig.style === 'custom'" class="my-[4px] ml-[4px]">
      <CrmColorSelect key="customStyle" v-model:pure-color="pageConfig.customStyle" />
    </div>
  </CrmCard>

  <!-- 登录页配置 -->
  <CrmCard class="mb-[16px]" :loading="pageLoading" hide-footer auto-height>
    {{ t('system.config.page.loginPageConfig') }}
    <div class="config-content">
      <div class="config-content-head">
        {{ t('system.config.page.pagePreview') }}
        <n-button type="primary" text @click="resetLoginPageConfig">
          {{ t('system.config.page.reset') }}
        </n-button>
      </div>
      <div class="config-main">
        <div ref="loginPageFullRef" class="config-preview">
          <div :class="['config-preview-head', isLoginPageFullscreen ? 'config-preview-head-full' : '']">
            <div class="flex items-center justify-between">
              <img :src="pageConfig.icon[0]?.url ? pageConfig.icon[0].url : '/logo.png'" class="h-[18px] w-[18px]" />
              <div class="one-line-text ml-[4px] text-[10px]">{{ pageConfig.title }}</div>
            </div>
            <CrmIcon
              class="cursor-pointer text-[var(--text-n2)]"
              :type="!isLoginPageFullscreen ? 'iconicon_full_screen_one' : 'iconicon_off_screen'"
              @click="loginFullscreenToggle"
            />
          </div>
          <!-- 登录页预览实际渲染 DOM，按三种屏幕尺寸缩放 -->
          <div :class="['page-preview', isLoginPageFullscreen ? 'page-preview-full' : 'page-preview-normal']">
            <Banner :banner="pageConfig.loginImage[0]?.url ?? undefined" is-preview />
            <LoginForm :slogan="pageConfig.slogan" :logo="pageConfig.loginLogo[0]?.url ?? undefined" is-preview />
          </div>
        </div>
        <div class="config-form">
          <UploadCard
            v-for="item in cardList"
            :key="item.valueKey"
            v-model:file-list="pageConfig[item.valueKey as keyof typeof pageConfig] as CrmFileItem[]"
            :show-tag="item.showTag"
            :title="item.title"
            :max-size="item.maxSize"
            :size-unit="item.sizeUnit"
            :tip="item.tip"
          />
          <n-form ref="loginConfigFormRef" :model="pageConfig">
            <n-form-item
              :label="t('system.config.page.slogan')"
              path="slogan"
              :rule="[
                {
                  required: true,
                  trigger: ['change', 'blur'],
                  message: t('common.notNull', { value: `${t('system.config.page.slogan')} ` }),
                },
              ]"
            >
              <n-input
                v-model:value="pageConfig.slogan"
                allow-clear
                :max-length="255"
                :placeholder="t('common.pleaseInput')"
              />
              <div class="text-[12px] text-[var(--text-n4)]">{{ t('system.config.page.sloganTip') }}</div>
            </n-form-item>
            <n-form-item :label="t('system.config.page.title')" path="title">
              <n-input
                v-model:value="pageConfig.title"
                allow-clear
                :max-length="255"
                :placeholder="t('common.pleaseInput')"
              />
              <div class="text-[12px] text-[var(--text-n4)]">{{ t('system.config.page.titleTip') }}</div>
            </n-form-item>
          </n-form>
        </div>
      </div>
      <div class="config-content-tip">{{ t('system.config.page.loginPreviewTip') }}</div>
    </div>
  </CrmCard>

  <!-- 平台主页面配置 -->
  <CrmCard class="mb-[88px]" :loading="pageLoading" hide-footer auto-height>
    {{ t('system.config.page.platformConfig') }}
    <div class="config-content border border-solid border-[var(--text-n8)] !bg-[var(--text-n10)]">
      <div class="config-content-head">
        {{ t('system.config.page.pagePreview') }}
        <n-button type="primary" text @click="resetPlatformConfig">
          {{ t('system.config.page.reset') }}
        </n-button>
      </div>
      <div :class="['config-main', '!h-[290px]']">
        <div ref="platformPageFullRef" class="config-preview relative">
          <CrmIcon
            class="absolute right-[12px] top-[8px] z-[998] cursor-pointer text-[var(--text-n2)]"
            :type="!isPlatformPageFullscreen ? 'iconicon_full_screen_one' : 'iconicon_off_screen'"
            @click="platformFullscreenToggle"
          />
          <div
            :class="[
              'page-preview',
              'platform-preview',
              '!h-[550px]',
              isPlatformPageFullscreen ? 'page-preview-full' : 'page-preview-normal',
            ]"
          >
            <n-layout>
              <LayoutHeader :logo="pageConfig.logoPlatform[0]?.url ?? '/logo.png'" :name="pageConfig.platformName" />
              <n-layout class="flex-1" has-sider>
                <div class="w-[180px] bg-[var(--text-n10)]"></div>
                <div class="w-full bg-[var(--text-n9)]"></div>
              </n-layout>
            </n-layout>
          </div>
        </div>
        <div class="config-form">
          <UploadCard
            v-for="item in platformCardList"
            :key="item.valueKey"
            v-model:file-list="pageConfig[item.valueKey as keyof typeof pageConfig] as CrmFileItem[]"
            :show-tag="item.showTag"
            :title="item.title"
            :max-size="item.maxSize"
            :size-unit="item.sizeUnit"
            :tip="item.tip"
          />
          <n-form ref="platformConfigFormRef" :model="pageConfig">
            <n-form-item
              :label="t('system.config.page.platformName')"
              path="platformName"
              :rule="[
                {
                  required: true,
                  trigger: ['change', 'blur'],
                  message: t('common.notNull', { value: t('system.config.page.platformName') }),
                },
              ]"
            >
              <n-input
                v-model:value="pageConfig.platformName"
                allow-clear
                :max-length="255"
                :placeholder="t('common.pleaseInput')"
              />
              <div class="text-[12px] text-[var(--text-n4)]">{{ t('system.config.page.platformNameTip') }}</div>
            </n-form-item>
            <n-form-item :label="t('settings.help.doc')" path="helpDoc">
              <n-input
                v-model:value="pageConfig.helpDoc"
                allow-clear
                :max-length="255"
                :placeholder="t('common.pleaseInput')"
              />
              <div class="text-[12px] text-[var(--text-n4)]">{{ t('system.config.page.helpDocTip') }}</div>
            </n-form-item>
          </n-form>
        </div>
      </div>
      <div class="config-content-tip">{{ t('system.config.page.loginPreviewTip') }}</div>
    </div>
  </CrmCard>

  <div
    class="base-box-shadow fixed bottom-0 right-0 z-[999] flex justify-end gap-[16px] bg-[var(--text-n10)] p-[24px]"
    :style="{ width: `calc(100% - ${menuWidth}px)` }"
  >
    <n-button secondary @click="resetAll">
      {{ t('system.config.page.cancel') }}
    </n-button>
    <n-button type="primary" @click="beforeSave">
      {{ t('system.config.page.save') }}
    </n-button>
  </div>
</template>

<script setup lang="ts">
  import { FormInst, NButton, NForm, NFormItem, NInput, NLayout, useMessage } from 'naive-ui';

  import { scrollIntoView } from '@lib/shared/method/dom';
  import { sleep } from '@lib/shared/method/index';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmColorSelect from '@/components/pure/crm-color-select/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import UploadCard, { UploadCardItem } from './uploadCard.vue';
  import LayoutHeader from '@/layout/components/layout-header.vue';
  import Banner from '@/views/base/login/components/banner.vue';
  import LoginForm from '@/views/base/login/components/login-form.vue';

  import useFullScreen from '@/hooks/useFullScreen';
  import { useI18n } from '@/hooks/useI18n';
  import useAppStore from '@/store/modules/app';
  import { setCustomTheme, setPlatformColor, watchStyle, watchTheme } from '@/utils/theme';

  const { t } = useI18n();
  const appStore = useAppStore();
  const Message = useMessage();

  const pageLoading = ref(false);
  const pageConfig = ref({ ...appStore.pageConfig, slogan: t(appStore.pageConfig.slogan) });

  const styleList = [
    {
      tab: t('common.default'),
      name: 'default',
    },
    {
      tab: t('system.business.page.follow'),
      name: 'follow',
    },
    {
      tab: t('common.custom'),
      name: 'custom',
    },
  ];

  const themeList = [
    {
      tab: t('common.default'),
      name: 'default',
    },
    {
      tab: t('common.custom'),
      name: 'custom',
    },
  ];

  watch(
    () => pageConfig.value.theme,
    (val) => {
      watchTheme(val, pageConfig.value);
    }
  );

  watch(
    () => pageConfig.value.customTheme,
    (val) => {
      if (val && pageConfig.value.theme === 'custom') {
        setCustomTheme(val);
        if (pageConfig.value.style === 'follow') {
          // 若平台风格跟随主题色
          setPlatformColor(pageConfig.value.customTheme, true);
        }
      }
    }
  );

  watch(
    () => pageConfig.value.style,
    (val) => {
      watchStyle(val, pageConfig.value);
    }
  );

  watch(
    () => pageConfig.value.customStyle,
    (val) => {
      if (val && pageConfig.value.style === 'custom') {
        setPlatformColor(val);
      }
    }
  );

  const loginPageFullRef = ref<HTMLElement | null>(null);
  const { isFullScreen: isLoginPageFullscreen, toggleFullScreen: loginFullscreenToggle } =
    useFullScreen(loginPageFullRef);
  const platformPageFullRef = ref<HTMLElement | null>(null);
  const { isFullScreen: isPlatformPageFullscreen, toggleFullScreen: platformFullscreenToggle } =
    useFullScreen(platformPageFullRef);

  const loginConfigFormRef = ref<FormInst | null>(null);
  const platformConfigFormRef = ref<FormInst | null>(null);

  const cardList = ref<UploadCardItem[]>([
    {
      title: t('system.config.page.icon'),
      valueKey: 'icon',
      tip: t('system.config.page.iconTip'),
      maxSize: 200,
      sizeUnit: 'KB',
    },
    {
      title: t('system.config.page.loginLogo'),
      valueKey: 'loginLogo',
      tip: t('system.config.page.loginLogoTip'),
      maxSize: 200,
      sizeUnit: 'KB',
    },
    {
      title: t('system.config.page.loginBg'),
      valueKey: 'loginImage',
      tip: t('system.config.page.loginBgTip'),
      maxSize: 1,
      sizeUnit: 'MB',
    },
  ]);

  const platformCardList = ref<UploadCardItem[]>([
    {
      title: t('system.config.page.platformLogo'),
      valueKey: 'logoPlatform',
      tip: t('system.config.page.platformLogoTip'),
      maxSize: 200,
      sizeUnit: 'KB',
    },
  ]);

  function resetLoginPageConfig() {
    pageConfig.value = {
      ...pageConfig.value,
      ...appStore.defaultLoginConfig,
      slogan: t(appStore.defaultLoginConfig.slogan),
    };
  }

  function resetPlatformConfig() {
    pageConfig.value = {
      ...pageConfig.value,
      ...appStore.defaultPlatformConfig,
    };
  }

  const menuWidth = computed(() => {
    return appStore.menuCollapsed ? appStore.collapsedWidth : 180;
  });

  function resetAll() {
    pageConfig.value = { ...appStore.getDefaultPageConfig, slogan: t(appStore.defaultLoginConfig.slogan) };
  }

  // 保存并应用
  async function save() {
    try {
      pageLoading.value = true;
      // TODO lmy 联调 (第一版不上)
      Message.success(t('common.saveSuccess'));
      await sleep(300);
      window.location.reload();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      pageLoading.value = false;
    }
  }

  // 保存前校验
  async function beforeSave() {
    try {
      await loginConfigFormRef.value?.validate();
      await platformConfigFormRef.value?.validate();
      save();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      const errDom = document.querySelector('.n-form-item-blank--error');
      scrollIntoView(errDom, { block: 'center' });
    }
  }
</script>

<style lang="less" scoped>
  .config-content {
    margin-top: 8px;
    padding: 16px;
    min-width: 1150px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n9);
    .config-content-head {
      margin-bottom: 8px;
      @apply flex items-center justify-between;
    }
    .config-main {
      display: flex;
      overflow: hidden;
      height: 495px;
      gap: 16px;
      @media screen and (min-width: 1600px) {
        height: 550px;
        &--en {
          height: 570px;
        }
      }
      @media screen and (min-width: 1800px) {
        height: auto;
      }
      .config-preview {
        width: 740px;
        background-color: var(--text-n10);
        @media screen and (min-width: 1600px) {
          width: 888px;
        }
        @media screen and (min-width: 1800px) {
          width: 100%;
        }
        .config-preview-head {
          @apply flex items-center justify-between;

          padding: 8px;
          &.config-preview-head-full {
            width: 100vw;
          }
        }
        .page-preview {
          @apply relative flex flex-1;

          width: 1480px;
          height: 916px;
          transform-origin: center;
          @media screen and (min-width: 1800px) {
            width: 100%;
            height: 632px;
          }
          &.page-preview-normal {
            transform: translate(-25%, -25%) scale(0.5);
            @media screen and (min-width: 1600px) {
              transform: translate(-20%, -20%) scale(0.6);
            }
            @media screen and (min-width: 1800px) {
              transform: none;
            }
          }
          &.page-preview-full {
            width: 100vw;
            height: 100vh !important;
            transform: none;
          }
        }
      }
      .config-form {
        margin-left: 16px;
        width: 40%;
      }
    }
    .config-content-tip {
      margin-top: 4px;
      color: var(--text-n4);
    }
  }
  :deep(.n-form-item .n-form-item-blank) {
    flex-direction: column;
    align-items: flex-start;
  }
</style>

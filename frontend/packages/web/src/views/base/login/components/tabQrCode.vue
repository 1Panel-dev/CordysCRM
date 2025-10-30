<template>
  <div v-if="activeQrCodeName === 'WECOM'" class="login-qrcode">
    <div class="qrcode">
      <wecom-qr v-if="activeQrCodeName === 'WECOM'" />
    </div>
  </div>
  <div v-if="activeQrCodeName === 'DINGTALK'" class="login-qrcode">
    <div class="qrcode">
      <div class="title">
        <CrmIcon class="mr-[8px]" type="iconlogo_dingtalk" :size="24" />
        {{ t('login.form.DINGTALKScanLogin') }}
      </div>
      <ding-talk-qr v-if="activeQrCodeName === 'DINGTALK'" />
    </div>
  </div>
  <div v-if="activeQrCodeName === 'LARK'" class="login-qrcode">
    <div class="qrcode">
      <div class="title">
        <CrmIcon class="mr-[8px]" type="iconlogo_lark" :size="24"></CrmIcon>
        {{ t('login.form.LARKScanLogin') }}
      </div>
      <lark-qr-code v-if="activeQrCodeName === 'LARK'" />
    </div>
  </div>
  <!-- TODO 先不上 -->
  <!-- <div v-if="activeQrCodeName === 'LARK_SUITE'" class="login-qrcode">
    <div class="qrcode">
      <div class="title">
        <CrmIcon type="icon-logo_lark" :size="24"></CrmIcon>
        国际飞书登录
      </div>
      <lark-suite-qr-code v-if="activeQrCodeName === 'LARK_SUITE'" />
    </div>
  </div> -->
</template>

<script lang="ts" setup>
  import { useI18n } from 'vue-i18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import dingTalkQr from './dingTalkQrCode.vue';
  import LarkQrCode from './larkQrCode.vue';
  // import LarkSuiteQrCode from './larkSuiteQrCode.vue';
  import WecomQr from './weComQrCode.vue';

  import { useAppStore } from '@/store';

  const { t } = useI18n();

  const appStore = useAppStore();

  const activeQrCodeName = computed(() => appStore.activePlatformResource.syncResource);
</script>

<style lang="less" scoped>
  .login-qrcode {
    display: flex;
    align-items: center;
    margin-top: 24px;
    flex-direction: column;
    .qrcode {
      display: flex;
      justify-content: center;
      align-items: center;
      overflow: hidden;
      border-radius: 8px;
      background: #ffffff;
      flex-direction: column;
    }
    .title {
      z-index: 100000;
      margin-bottom: -24px;
      font-size: 18px;
      color: var(--text-n1);
      @apply flex w-full items-center justify-center overflow-hidden font-medium;
      .ed-icon {
        margin-right: 8px;
        font-size: 24px;
      }
    }
  }
</style>

<template>
  <CrmDrawer
    v-model:show="visible"
    width="100%"
    :footer="false"
    :closable="false"
    :close-on-esc="false"
    :loading="loading"
    header-class="crm-form-drawer-header"
    body-content-class="!p-0"
  >
    <template #header>
      <div class="flex items-center justify-between">
        {{ formCreateTitle }}
        <n-button class="p-[8px]" quaternary @click="handleBack">
          <CrmIcon type="iconicon_close" :size="18" />
        </n-button>
      </div>
    </template>
    <CrmFormCreate
      v-if="visible"
      ref="formCreateRef"
      v-model:loading="loading"
      v-model:unsaved="unsaved"
      :source-id="props.sourceId"
      :form-key="props.formKey"
      :need-init-detail="props.needInitDetail"
      :initial-source-name="props.initialSourceName"
      :other-save-params="props.otherSaveParams"
      :is-edit="needInitDetail"
      :link-form-info="props.linkFormInfo"
      :link-form-key="props.linkFormKey"
      class="!pt-[16px]"
      @cancel="handleBack"
      @saved="handleSaved"
      @init="($event) => (formCreateTitle = $event)"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import useModal from '@/hooks/useModal';

  const CrmFormCreate = defineAsyncComponent(() => import('@/components/business/crm-form-create/index.vue'));

  const props = defineProps<{
    sourceId?: string;
    formKey: FormDesignKeyEnum;
    needInitDetail?: boolean; // 是否需要初始化详情
    initialSourceName?: string; // 初始化详情时的名称
    otherSaveParams?: Record<string, any>;
    linkFormInfo?: Record<string, any>; // 关联表单信息
    linkFormKey?: FormDesignKeyEnum;
  }>();
  const emit = defineEmits<{
    (e: 'saved', res: any): void;
  }>();

  const { t } = useI18n();
  const { openModal } = useModal();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });
  const formCreateTitle = ref('');
  const loading = ref(false);
  const unsaved = ref(false);

  function showUnsavedLeaveTip() {
    openModal({
      type: 'warning',
      title: t('common.unSaveLeaveTitle'),
      content: t('common.editUnsavedLeave'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        visible.value = false;
      },
    });
  }

  function handleBack() {
    if (!loading.value) {
      if (unsaved.value) {
        showUnsavedLeaveTip();
      } else {
        visible.value = false;
      }
    }
  }

  function handleSaved(isContinue: boolean, res: any) {
    visible.value = isContinue;
    emit('saved', res);
  }
</script>

<style lang="less">
  .crm-form-drawer-header {
    padding: 12px 16px !important;
    .n-drawer-header__main {
      max-width: 100%;
      .crm-form-drawer-title {
        --n-border: none !important;
        --n-border-hover: none !important;
        --n-border-focus: none !important;
        --n-box-shadow-focus: none !important;

        min-width: 80px;
        border-bottom: 2px solid var(--text-n8);
      }
    }
  }
</style>

<template>
  <CrmDrawer
    v-model:show="visible"
    width="100%"
    :footer="false"
    :closable="false"
    :close-on-esc="false"
    :loading="loading"
    :title="props.title"
    header-class="crm-form-drawer-header"
    body-content-class="!p-0"
  >
    <template #header>
      <div class="flex items-center justify-end">
        <n-button type="primary" @click="handleBack">
          <Close />
        </n-button>
      </div>
    </template>
    <CrmFormCreate
      v-model:list="fieldList"
      :form-detail="formDetail"
      :form-config="formConfig"
      @cancel="handleBack"
      @save="saveForm"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';
  import { Close } from '@vicons/ionicons5';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';

  const CrmFormCreate = defineAsyncComponent(() => import('@/components/business/crm-form-create/index.vue'));

  const props = defineProps<{
    sourceId?: string;
    title: string;
    formKey: FormDesignKeyEnum;
  }>();

  const { t } = useI18n();
  const { openModal } = useModal();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { fieldList, formConfig, formDetail, unsaved, loading, initFormConfig, initFormDetail, saveForm } =
    useFormCreateApi({
      sourceId: props.sourceId,
      formKey: props.formKey,
    });

  watch(
    () => [fieldList.value, formConfig.value],
    () => {
      unsaved.value = true;
    },
    {
      deep: true,
    }
  );

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

  watch(
    () => visible.value,
    async (val) => {
      if (val) {
        await initFormConfig();
        if (props.sourceId) {
          initFormDetail();
        }
      }
    },
    {
      immediate: true,
    }
  );
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

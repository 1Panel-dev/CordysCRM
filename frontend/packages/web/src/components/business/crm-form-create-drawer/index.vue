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
      v-model:list="fieldList"
      :form-detail="formDetail"
      :form-config="formConfig"
      class="pt-[16px]"
      @cancel="handleBack"
      @save="handleSave"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useModal from '@/hooks/useModal';

  const CrmFormCreate = defineAsyncComponent(() => import('@/components/business/crm-form-create/index.vue'));

  const props = defineProps<{
    sourceId?: string;
    formKey: FormDesignKeyEnum;
    needInitDetail?: boolean; // 是否需要初始化详情
    initialSourceName?: string; // 初始化详情时的名称
    otherSaveParams?: Record<string, any>;
  }>();
  const emit = defineEmits<{
    (e: 'saved'): void;
  }>();

  const { t } = useI18n();
  const { openModal } = useModal();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });
  const formCreateRef = ref<InstanceType<typeof CrmFormCreate>>();

  const {
    fieldList,
    formConfig,
    formDetail,
    unsaved,
    loading,
    formCreateTitle,
    initFormConfig,
    initFormDetail,
    saveForm,
  } = useFormCreateApi({
    formKey: toRefs(props).formKey,
    sourceId: toRefs(props).sourceId,
    needInitDetail: toRefs(props).needInitDetail,
    initialSourceName: toRefs(props).initialSourceName,
    otherSaveParams: toRefs(props).otherSaveParams,
    formCreateRef,
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
        if (props.sourceId && props.needInitDetail) {
          initFormDetail();
        }
      }
    },
    {
      immediate: true,
    }
  );

  function handleSave(form: Record<string, any>, isContinue: boolean) {
    saveForm(form, isContinue, () => {
      visible.value = isContinue;
      emit('saved');
    });
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

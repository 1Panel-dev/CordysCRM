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
        <div class="flex items-center">
          <n-button text class="mr-[4px] w-[32px]" @click="handleBack">
            <n-icon size="16">
              <ChevronBackOutline />
            </n-icon>
          </n-button>
          <div class="text-[14px] font-normal"> {{ props.title }}</div>
          <!-- <n-input
            v-model:value="name"
            type="text"
            :placeholder="t('common.pleaseInput')"
            size="medium"
            clearable
            class="crm-form-drawer-title"
            autosize
            :status="name.trim() === '' ? 'error' : undefined"
            :maxlength="255"
          ></n-input> -->
        </div>
        <n-button type="primary" :loading="loading" @click="handleSave">{{ t('common.save') }}</n-button>
      </div>
    </template>
    <CrmFormDesign
      v-if="visible"
      ref="formDesignRef"
      v-model:form-config="formConfig"
      v-model:field-list="fieldList"
      :form-key="props.formKey"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton, NIcon, useMessage } from 'naive-ui';
  import { ChevronBackOutline } from '@vicons/ionicons5';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import { saveFormDesignConfig } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import { useFormDesignConfig } from './useFormDesignConfig';

  const CrmFormDesign = defineAsyncComponent(() => import('@/components/business/crm-form-design/index.vue'));

  const props = defineProps<{
    title: string;
    formKey: FormDesignKeyEnum;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const formKey = computed(() => props.formKey);
  const { loading, fieldList, formConfig, formDesignRef, unsaved, checkRepeat, buildSavePayload, initFormConfig } =
    useFormDesignConfig({ formKey });

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

  async function handleSave() {
    if (!checkRepeat()) {
      return;
    }
    try {
      loading.value = true;
      await saveFormDesignConfig(buildSavePayload());
      Message.success(t('common.saveSuccess'));
      visible.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        initFormConfig();
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

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
    <CrmFormCreate v-model:list="fieldList" :form-config="formConfig" @cancel="handleBack" @save="handleSave" />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton, useMessage } from 'naive-ui';
  import { Close } from '@vicons/ionicons5';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { FormConfig, FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import {
    addCustomer,
    addCustomerContact,
    addCustomerFollowPlan,
    addCustomerFollowRecord,
    getCustomerContactFormConfig,
    getCustomerFollowPlanFormConfig,
    getCustomerFollowRecordFormConfig,
    getCustomerFormConfig,
  } from '@/api/modules/customer/index';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { safeFractionConvert } from '@/utils';

  import { FormCreateField } from '../crm-form-create/types';

  const CrmFormCreate = defineAsyncComponent(() => import('@/components/business/crm-form-create/index.vue'));

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

  const loading = ref(false);
  const fieldList = ref<FormCreateField[]>([]);
  const formConfig = ref<FormConfig>({
    layout: 1,
    labelPos: 'top',
    inputWidth: 'custom',
    optBtnContent: [
      {
        text: t('common.save'),
        enable: true,
      },
      {
        text: t('common.saveAndContinue'),
        enable: false,
      },
      {
        text: t('common.cancel'),
        enable: true,
      },
    ],
    optBtnPos: 'flex-row',
  });
  const unsaved = ref(false);

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

  const getFormConfigApiMap: Record<FormDesignKeyEnum, () => Promise<FormDesignConfigDetailParams>> = {
    [FormDesignKeyEnum.CUSTOMER]: getCustomerFormConfig,
    [FormDesignKeyEnum.BUSINESS]: getCustomerFormConfig, // TODO:
    [FormDesignKeyEnum.CONTACT]: getCustomerContactFormConfig,
    [FormDesignKeyEnum.FOLLOW_PLAN]: getCustomerFollowPlanFormConfig,
    [FormDesignKeyEnum.FOLLOW_RECORD]: getCustomerFollowRecordFormConfig,
    [FormDesignKeyEnum.LEAD]: getCustomerFormConfig, // TODO:
    [FormDesignKeyEnum.PRODUCT]: getCustomerFormConfig, // TODO:
  };

  const saveFormConfigApiMap: Record<FormDesignKeyEnum, (data: any) => Promise<any>> = {
    [FormDesignKeyEnum.CUSTOMER]: addCustomer,
    [FormDesignKeyEnum.BUSINESS]: addCustomer, // TODO:
    [FormDesignKeyEnum.CONTACT]: addCustomerContact,
    [FormDesignKeyEnum.FOLLOW_PLAN]: addCustomerFollowPlan,
    [FormDesignKeyEnum.FOLLOW_RECORD]: addCustomerFollowRecord,
    [FormDesignKeyEnum.LEAD]: addCustomer, // TODO:
    [FormDesignKeyEnum.PRODUCT]: addCustomer, // TODO:
  };

  async function handleSave(form: Record<string, any>, isContinue: boolean) {
    try {
      loading.value = true;
      const params: Record<string, any> = {
        moduleFields: [],
      };
      fieldList.value.forEach((item) => {
        if (item.businessKey) {
          // 存在业务字段，则按照业务字段的key存储
          params[item.businessKey] = form[item.id];
        } else {
          params.moduleFields.push({
            fieldId: item.id,
            fieldValue: form[item.id],
          });
        }
      });
      await saveFormConfigApiMap[props.formKey](params);
      Message.success(t('common.saveSuccess'));
      if (!isContinue) {
        visible.value = false;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function initFormConfig() {
    try {
      loading.value = true;
      const res = await getFormConfigApiMap[props.formKey]();
      fieldList.value = res.fields.map((item) => ({
        ...item,
        id: item.id,
        internalKey: item.internalKey,
        type: item.type,
        name: t(item.name),
        placeholder: t(item.placeholder || ''),
        fieldWidth: safeFractionConvert(item.fieldWidth),
      }));
      formConfig.value = res.formProp;
      nextTick(() => {
        unsaved.value = false;
      });
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

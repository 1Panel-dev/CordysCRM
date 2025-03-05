<template>
  <n-spin :show="loading">
    <CrmDescription :descriptions="descriptions"> </CrmDescription>
  </n-spin>
</template>

<script setup lang="ts">
  import { NSpin } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleField } from '@lib/shared/models/customer';

  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import { getFormConfigApiMap } from '@/components/business/crm-form-create/config';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import { getCustomer, getCustomerContact, getCustomerFollowRecord } from '@/api/modules/customer';
  import { useI18n } from '@/hooks/useI18n';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum;
  }>();

  const { t } = useI18n();

  const descriptions = ref<Description[]>([]);
  const fieldList = ref<FormCreateField[]>([]);
  const loading = ref(false);

  interface FormDetail {
    moduleFields: ModuleField[];
    [key: string]: any;
  }

  const getFormDetailApiMap: Partial<Record<FormDesignKeyEnum, (id: string) => Promise<FormDetail>>> = {
    [FormDesignKeyEnum.CUSTOMER]: getCustomer,
    [FormDesignKeyEnum.BUSINESS]: getCustomer, // TODO:
    [FormDesignKeyEnum.CONTACT]: getCustomerContact,
    [FormDesignKeyEnum.FOLLOW_RECORD]: getCustomerFollowRecord,
    [FormDesignKeyEnum.LEAD]: getCustomer, // TODO:
    [FormDesignKeyEnum.PRODUCT]: getCustomer, // TODO:
  };

  async function initFormDetail() {
    try {
      const asyncApi = getFormDetailApiMap[props.formKey];
      if (!asyncApi) return;
      const form = await asyncApi(props.sourceId);
      fieldList.value.forEach((item) => {
        if (item.businessKey) {
          // 业务标准字段读取最外层
          descriptions.value.push({
            label: item.name,
            value: form[item.businessKey],
          });
        } else {
          // 其他的字段读取moduleFields
          const field = form.moduleFields.find((moduleField: ModuleField) => moduleField.fieldId === item.id);
          descriptions.value.push({
            label: item.name,
            value: field?.fieldValue || [],
          });
        }
      });
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
        name: t(item.name),
      }));
      initFormDetail();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    initFormConfig();
  });
</script>

<style lang="less" scoped></style>

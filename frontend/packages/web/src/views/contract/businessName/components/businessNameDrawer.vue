<template>
  <CrmDrawer
    v-model:show="visible"
    width="1000"
    show-continue
    :title="form.id ? t('contract.businessName.update') : t('contract.businessName.add')"
    @confirm="handleConfirm(false)"
    @continue="handleConfirm(true)"
    @cancel="cancelHandler"
  >
    <n-scrollbar>
      <n-form ref="formRef" :model="form">
        <n-form-item path="method" :label="t('contract.businessName.addMethod')">
          <CrmTab
            v-model:active-tab="form.method"
            no-content
            :tab-list="tabList"
            type="segment"
            @change="handleChangeTab"
          />
        </n-form-item>
        <n-form-item path="name" :label="t('contract.businessName.companyName')">
          <n-input
            v-if="form.method === 'custom'"
            v-model:value="form.name"
            allow-clear
            :maxlength="255"
            :placeholder="t('common.pleaseInput')"
          />
          <CrmAutoSearchSelect
            v-else
            v-model:value="form.name"
            :fetch="getOpportunityList"
            :placeholder="t('contract.businessName.selectCompanyPlaceholder')"
            @select="handleAutoFillInfo"
          />
        </n-form-item>
        <div class="grid grid-cols-2 gap-x-[16px]">
          <n-form-item v-for="item of formConfigList" :key="item.field" :path="item.field" :label="item.label">
            <n-input
              v-model:value="form[item.field]"
              allow-clear
              :disabled="form.method === 'thirdParty'"
              :maxlength="255"
              :placeholder="t('common.pleaseInput')"
            />
          </n-form-item>
        </div>
      </n-form>
    </n-scrollbar>
  </CrmDrawer>
</template>

<script setup lang="ts">
  // todo 表单校验配置没有加
  import { ref } from 'vue';
  import { FormInst, NForm, NFormItem, NInput, NScrollbar, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmAutoSearchSelect from '@/components/business/crm-auto-search-select/index.vue';

  import { addBusinessName, getOpportunityList, updateBusinessName } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    sourceId: string;
  }>();

  const emit = defineEmits<{
    (e: 'load'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const initForm = {
    id: '',
    method: 'thirdParty',
    name: null,
    taxpayerNumber: '',
    address: '',
    bank: '',
    bankAccount: '',
    phone: '',
    capital: '',
    customerScale: '',
    registrationAccount: '',
  };

  const form = ref<Record<string, any>>({
    id: '',
    method: 'thirdParty',
  });

  const tabList = [
    {
      name: 'thirdParty',
      tab: t('contract.businessName.addMethodThird'),
    },
    {
      name: 'custom',
      tab: t('contract.businessName.addMethodCustom'),
    },
  ];

  const formRef = ref<FormInst | null>(null);

  const formConfigList = [
    {
      label: t('contract.businessName.taxpayerNumber'),
      field: 'taxpayerNumber',
    },
    {
      label: t('contract.businessName.address'),
      field: 'bank',
    },
    {
      label: t('contract.businessName.bank'),
      field: 'bank',
    },
    {
      label: t('contract.businessName.bankAccount'),
      field: 'bankAccount',
    },
    {
      label: t('contract.businessName.phone'),
      field: 'phone',
    },
    {
      label: t('contract.businessName.capital'),
      field: 'capital',
    },
    {
      label: t('contract.businessName.customerScale'),
      field: 'customerScale',
    },
    {
      label: t('contract.businessName.registrationAccount'),
      field: 'registrationAccount',
    },
  ];

  function handleAutoFillInfo(raw: any) {
    //  todo 带入信息
  }

  function handleChangeTab() {
    form.value = cloneDeep({
      ...initForm,
      method: form.value.method,
    });
  }

  function cancelHandler() {
    form.value = cloneDeep(initForm);
    visible.value = false;
  }

  const loading = ref<boolean>(false);
  async function handleSave(isContinue: boolean) {
    try {
      loading.value = true;
      // todo 保存接口
      if (form.value.id) {
        // await updateBusinessName(form.value);
        Message.success(t('common.updateSuccess'));
      } else {
        // await addBusinessName(form.value);
        Message.success(t('common.addSuccess'));
      }
      if (isContinue) {
        form.value = cloneDeep(initForm);
      } else {
        cancelHandler();
      }
      emit('load');
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      loading.value = false;
    }
  }

  function handleConfirm(isContinue: boolean) {
    formRef.value?.validate(async (error) => {
      if (!error) {
        handleSave(isContinue);
      }
    });
  }
</script>

<style scoped></style>

<template>
  <CrmDrawer
    v-model:show="visible"
    width="1000"
    :show-continue="!form.id"
    :title="form.id ? t('contract.businessTitle.update') : t('contract.businessTitle.add')"
    :ok-text="form.id ? t('common.update') : t('common.add')"
    :loading="loading"
    @confirm="handleConfirm(false)"
    @continue="handleConfirm(true)"
    @cancel="cancelHandler"
  >
    <n-scrollbar>
      <!-- todo xinixnwu 表单校验没有加 -->
      <n-form ref="formRef" :model="form">
        <n-form-item path="type" :label="t('contract.businessTitle.addMethod')">
          <CrmTab
            v-model:active-tab="form.type"
            no-content
            :tab-list="tabList"
            type="segment"
            @change="handleChangeTab"
          />
        </n-form-item>
        <n-form-item path="businessName" :label="t('contract.businessTitle.companyName')">
          <n-input
            v-if="form.type === 'custom'"
            v-model:value="form.businessName"
            allow-clear
            :maxlength="255"
            :placeholder="t('common.pleaseInput')"
          />
          <CrmAutoSearchSelect
            v-else
            v-model:value="form.businessName"
            :fetch="getOpportunityList"
            :placeholder="t('contract.businessTitle.selectCompanyPlaceholder')"
            @select="handleAutoFillInfo"
          />
        </n-form-item>
        <div class="grid grid-cols-2 gap-x-[16px]">
          <n-form-item
            v-for="item of businessTitleFormConfigList"
            :key="item.value"
            :path="item.value"
            :label="item.label"
          >
            <n-input
              v-model:value="form[item.value] as string | null"
              allow-clear
              :disabled="form.type === 'thirdParty'"
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
  import { ref } from 'vue';
  import { FormInst, NForm, NFormItem, NInput, NScrollbar, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { SaveBusinessTitleParams } from '@lib/shared/models/contract';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmAutoSearchSelect from '@/components/business/crm-auto-search-select/index.vue';

  import { addBusinessTitle, getBusinessTitleDetail, getOpportunityList, updateBusinessTitle } from '@/api/modules';
  import { businessTitleFormConfigList } from '@/config/contract';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    sourceId: string;
  }>();

  const emit = defineEmits<{
    (e: 'load'): void;
    (e: 'cancel'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const initForm: SaveBusinessTitleParams = {
    id: '',
    type: 'thirdParty',
    businessName: null,
    identificationNumber: '',
    openingBank: '',
    bankAccount: '',
    phoneNumber: '',
    registeredCapital: '',
    companySize: '',
    registrationNumber: '',
    registrationAddress: '',
  };

  const form = ref<SaveBusinessTitleParams>({
    ...initForm,
  });

  const tabList = [
    {
      name: 'thirdParty',
      tab: t('contract.businessTitle.addMethodThird'),
    },
    {
      name: 'custom',
      tab: t('contract.businessTitle.addMethodCustom'),
    },
  ];

  const formRef = ref<FormInst | null>(null);

  function handleAutoFillInfo(raw: any) {
    //  todo xinixnwu 带入信息
  }

  function handleChangeTab() {
    form.value = { ...initForm, type: form.value.type };
  }

  function cancelHandler() {
    form.value = { ...initForm };
    emit('cancel');
    visible.value = false;
  }

  const loading = ref<boolean>(false);
  async function handleSave(isContinue: boolean) {
    try {
      loading.value = true;
      if (form.value.id) {
        await updateBusinessTitle(form.value);
        Message.success(t('common.updateSuccess'));
      } else {
        await addBusinessTitle(form.value);
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

  async function initDetail() {
    if (!props.sourceId) return;
    try {
      const result = await getBusinessTitleDetail(props.sourceId);
      form.value = { ...result };
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => visible.value,
    (newVal) => {
      if (newVal) {
        initDetail();
      }
    }
  );
</script>

<style scoped></style>

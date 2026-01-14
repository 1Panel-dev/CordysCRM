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
      <n-form ref="formRef" :model="form">
        <n-form-item v-if="showType" path="type" :label="t('contract.businessTitle.addMethod')">
          <CrmTab
            v-model:active-tab="form.type"
            no-content
            :tab-list="tabList"
            type="segment"
            @change="handleChangeTab"
          />
        </n-form-item>
        <n-form-item
          path="businessName"
          :label="t('contract.businessTitle.companyName')"
          :rule="[
            {
              required: true,
              message: t('common.notNull', { value: t('contract.businessTitle.companyName') }),
              trigger: ['input', 'blur'],
            },
          ]"
        >
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
            :fetch="getBusinessTitleThirdQueryOption"
            :placeholder="t('contract.businessTitle.selectCompanyPlaceholder')"
            label-key="name"
            value-key="id"
            @select="handleAutoFillInfo"
          />
        </n-form-item>
        <div class="grid grid-cols-2 gap-x-[16px]">
          <n-form-item
            v-for="item of formConfigList"
            :key="item.id"
            :path="item.field"
            :label="item.title"
            :rule="item.rule"
          >
            <n-input
              v-model:value="form[item.field]"
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

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { BusinessTitleValidateConfig, SaveBusinessTitleParams } from '@lib/shared/models/contract';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmAutoSearchSelect from '@/components/business/crm-auto-search-select/index.vue';

  import {
    addBusinessTitle,
    getBusinessTitleConfig,
    getBusinessTitleDetail,
    getBusinessTitleThirdQuery,
    getBusinessTitleThirdQueryOption,
    getThirdPartyConfig,
    updateBusinessTitle,
  } from '@/api/modules';
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
    businessName: '',
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

  async function handleQueryByBusinessName(businessName: string) {
    try {
      const result = await getBusinessTitleThirdQuery(businessName);
      form.value = {
        ...result,
        type: 'thirdParty',
      };
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log('error', error);
    }
  }

  function handleAutoFillInfo(businessName: string) {
    handleQueryByBusinessName(businessName);
  }

  function handleChangeTab() {
    form.value = {
      ...initForm,
      id: form.value.id,
      type: form.value.type,
    };
  }

  function cancelHandler() {
    form.value = { ...initForm };
    emit('cancel');
    visible.value = false;
  }

  const loading = ref<boolean>(false);
  const originType = ref<string | null>(null);
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
      originType.value = result.type;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const isEnableQccConfig = ref<boolean>(false);

  const showType = computed(() => {
    if (isEnableQccConfig.value) return true;
    if (form.value.id) {
      return originType.value === 'thirdParty';
    }
  });

  async function initQccConfig() {
    try {
      const result = await getThirdPartyConfig(CompanyTypeEnum.QCC);
      isEnableQccConfig.value = !!result && !!result.config && !!result.config?.qccEnable;
      if (!isEnableQccConfig.value && !form.value.id) {
        form.value.type = 'custom';
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const formConfigList = ref<BusinessTitleValidateConfig[]>([]);
  async function initValidateConfig() {
    try {
      const result = await getBusinessTitleConfig();
      formConfigList.value = businessTitleFormConfigList.map((item) => {
        const e = result.find((c) => c.field === item.value);
        return {
          ...e,
          title: item.label,
          rule: e?.required
            ? [
                {
                  required: true,
                  message: t('common.notNull', { value: `${item.label}` }),
                  trigger: ['input', 'blur'],
                },
              ]
            : undefined,
        } as BusinessTitleValidateConfig;
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => visible.value,
    (newVal) => {
      if (newVal) {
        initQccConfig();
        initDetail();
      }
    }
  );

  onBeforeMount(() => {
    initValidateConfig();
  });
</script>

<style scoped></style>

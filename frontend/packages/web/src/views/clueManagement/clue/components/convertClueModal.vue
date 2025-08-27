<template>
  <CrmModal
    v-model:show="visible"
    :title="t('clue.convertClueToOtherTitle')"
    :positive-text="t('clue.convert')"
    :negative-text="t('common.cancel')"
    :width="600"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <n-form ref="formRef" :model="form" require-mark-placement="left" :label-width="120" label-placement="left">
      <n-form-item path="name" :label="t('clue.convertClueToOther')">
        <n-checkbox-group v-model:value="selectedType">
          <n-space item-style="display: flex;" align="center">
            <n-checkbox
              v-for="item of convertTypeOptions"
              :key="item.value"
              :disabled="item.disabled"
              :value="item.value"
            >
              <n-tooltip :delay="300" :show-arrow="false" placement="bottom" :disabled="!item.disabledToolTip">
                <template #trigger>
                  <div>{{ item.label }}</div>
                </template>
                {{ item.disabledToolTip }}
              </n-tooltip>
            </n-checkbox>
          </n-space>
        </n-checkbox-group>
      </n-form-item>
      <n-form-item
        v-if="selectedType.includes(FormDesignKeyEnum.BUSINESS)"
        path="oppName"
        :label="oppLabel"
        :rule="[{ required: true, message: t('common.notNull', { value: oppLabel }), trigger: ['input'] }]"
      >
        <n-input v-model:value="form.oppName" allow-clear :maxlength="255" :placeholder="t('common.pleaseInput')" />
      </n-form-item>
      <div class="rounded-md bg-[var(--primary-7)] px-[24px] py-[16px]">
        <div class="mb-[8px] font-medium text-[var(--text-n1)]">{{ t('clue.remarks') }}</div>
        <div>{{ t('clue.sameNameConvertTip') }}</div>
        <div>{{ t('clue.notSameNameConvertTip') }}</div>
        <div>{{ t('clue.linkFormConfigTip') }}</div>
        <n-button v-permission="['MODULE_SETTING:UPDATE']" text type="primary" @click="handleOpenFormDesignDrawer">
          {{ t('module.customerFormSetting') }}
        </n-button>
      </div>
    </n-form>
    <template #footer>
      <div class="flex items-center justify-end gap-[12px]">
        <n-button :disabled="loading" secondary @click="handleCancel">
          {{ t('common.cancel') }}
        </n-button>
        <n-tooltip :delay="300" :show-arrow="false" placement="bottom" :disabled="!isNoPermissionDisabled">
          <template #trigger>
            <n-button :disabled="isNoPermissionDisabled" :loading="loading" type="primary" @click="handleConfirm">
              {{ t('clue.convert') }}
            </n-button>
          </template>
          {{ t('clue.convertOpportunityNoPermission') }}
        </n-tooltip>
      </div>
    </template>
  </CrmModal>

  <CrmModal
    v-model:show="showConvertedSuccessModal"
    :title="t('common.conversionSuccessful')"
    :width="600"
    size="small"
    :footer="false"
  >
    <div class="text-center">
      <div class="flex justify-center"><CrmSvg width="32px" height="32px" name="success" /></div>
      <div class="mt-[8px] text-[20px] font-medium text-[var(--text-n1)]">
        {{ t('common.conversionSuccessful') }}
      </div>
      <div class="mt-[16px]">
        <n-countdown
          ref="countdownRef"
          :precision="0"
          :render="renderTime"
          :duration="5000"
          :active="activeCount"
          @finish="handleFinish"
        />
        <span> {{ t('clue.countDownTip') }} </span>
      </div>
      <div class="mt-[16px] flex items-center justify-center gap-[12px]">
        <n-button secondary @click="back">
          {{ t('clue.backClueList') }}
        </n-button>
        <n-button type="primary" @click="goDetail">
          {{
            t('clue.afterConvertGoDetailText', {
              name:
                type === FormDesignKeyEnum.BUSINESS ? t('module.businessManagement') : t('module.customerManagement'),
            })
          }}
        </n-button>
      </div>
    </div>
  </CrmModal>
  <customManagementFormDrawer v-model:visible="customerManagementFormVisible" />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import {
    FormInst,
    NButton,
    NCheckbox,
    NCheckboxGroup,
    NCountdown,
    NForm,
    NFormItem,
    NInput,
    NSpace,
    NTooltip,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ConvertClueParams } from '@lib/shared/models/clue';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmSvg from '@/components/pure/crm-svg/index.vue';
  import customManagementFormDrawer from '@/views/system/module/components/customManagement/formDrawer.vue';

  import { getOptFormConfig, transformClue } from '@/api/modules';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { hasAllPermission } from '@/utils/permission';

  import { CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();

  const props = defineProps<{
    clueId: string;
  }>();

  const emit = defineEmits<{
    (e: 'success'): void;
  }>();

  const visible = defineModel<boolean>('show', { required: true });

  const initForm: ConvertClueParams = {
    clueId: '',
    oppName: '',
    oppCreated: false,
  };

  const initSelectedType = [FormDesignKeyEnum.CONTACT, FormDesignKeyEnum.CUSTOMER];
  const selectedType = ref(cloneDeep(initSelectedType));

  const form = ref<ConvertClueParams>({
    ...initForm,
  });

  const convertTypeOptions = computed(() => [
    {
      value: FormDesignKeyEnum.CONTACT,
      label: t('common.contact'),
      disabled: true,
    },
    {
      value: FormDesignKeyEnum.CUSTOMER,
      label: t('module.customerManagement'),
      disabled: true,
    },
    {
      value: FormDesignKeyEnum.BUSINESS,
      label: t('module.businessManagement'),
      disabled: !hasAllPermission(['OPPORTUNITY_MANAGEMENT:ADD', 'CUSTOMER_MANAGEMENT:ADD']),
      disabledToolTip: !hasAllPermission(['OPPORTUNITY_MANAGEMENT:ADD', 'CUSTOMER_MANAGEMENT:ADD'])
        ? t('clue.convertOpportunityNoPermission')
        : '',
    },
  ]);

  const customerManagementFormVisible = ref(false);
  function handleOpenFormDesignDrawer() {
    customerManagementFormVisible.value = true;
  }

  function handleCancel() {
    form.value = { ...initForm };
    selectedType.value = cloneDeep(initSelectedType);
    visible.value = false;
  }

  const formRef = ref<FormInst | null>(null);
  const loading = ref(false);
  const type = ref(FormDesignKeyEnum.CUSTOMER);
  const successDataId = ref('');
  const showConvertedSuccessModal = ref(false);
  const activeCount = ref(false);
  const lastConvertSuccessName = ref('');
  function handleConfirm() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          const res = await transformClue({
            ...form.value,
            oppCreated: selectedType.value.includes(FormDesignKeyEnum.BUSINESS),
            clueId: props.clueId,
          });

          type.value = selectedType.value.includes(FormDesignKeyEnum.BUSINESS)
            ? FormDesignKeyEnum.BUSINESS
            : FormDesignKeyEnum.CUSTOMER;
          showConvertedSuccessModal.value = true;
          activeCount.value = true;
          successDataId.value = res;
          lastConvertSuccessName.value = form.value.oppName;
          emit('success');
          handleCancel();
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          loading.value = false;
        }
      }
    });
  }

  const isNoPermissionDisabled = computed(() =>
    selectedType.value.includes(FormDesignKeyEnum.BUSINESS)
      ? !hasAllPermission(['CUSTOMER_MANAGEMENT:ADD', 'OPPORTUNITY_MANAGEMENT:ADD'])
      : !hasAllPermission(['CUSTOMER_MANAGEMENT:ADD'])
  );

  const renderTime = (timeProps: { hours: number; minutes: number; seconds: number; milliseconds: number }) =>
    h(
      'span',
      {
        class: 'mr-[4px] text-[var(--primary-8)]',
      },
      timeProps.seconds
    );

  function handleFinish() {
    showConvertedSuccessModal.value = false;
    activeCount.value = false;
  }

  const countdownRef = ref();
  function back() {
    handleFinish();
    countdownRef.value?.reset();
  }

  const { openNewPage } = useOpenNewPage();
  function goDetail() {
    if (type.value === FormDesignKeyEnum.BUSINESS) {
      const opportunityParams = {
        id: successDataId.value,
        opportunityName: lastConvertSuccessName.value,
      };
      openNewPage(OpportunityRouteEnum.OPPORTUNITY, opportunityParams);
    } else {
      openNewPage(CustomerRouteEnum.CUSTOMER_INDEX, {
        id: successDataId.value,
      });
    }
  }

  const oppLabel = ref('');
  async function initOppName() {
    try {
      const result = await getOptFormConfig();
      oppLabel.value = result.fields.find((e) => e.businessKey === 'name')?.name ?? t('opportunity.name');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        initOppName();
      }
    }
  );
</script>

<style scoped></style>

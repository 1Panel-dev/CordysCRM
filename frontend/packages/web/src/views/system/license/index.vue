<template>
  <CrmCard hide-footer :special-height="licenseStore.expiredDuring ? 64 : 0">
    <n-scrollbar x-scrollable content-class="flex w-full items-center justify-center">
      <div class="license-wrapper">
        <CrmSvg width="888px" class="mb-[40px] flex-shrink-0" height="180px" name="cordysBackground" />
        <div class="license-content">
          <div class="license-column">
            <div v-for="item in licenseInfo" :key="item.label" class="mb-[24px]">
              {{ item.label }}
            </div>
            <n-button
              v-permission="['LICENSE:EDIT']"
              strong
              text
              size="large"
              class="!text-[18px]"
              type="primary"
              @click="handleUpdate"
            >
              {{ t('system.license.authorityUpdate') }}
            </n-button>
          </div>
          <div class="license-column font-medium">
            <div v-for="item in licenseInfo" :key="`${item.label}-${item.value}`" class="mb-[24px]">
              {{ item.value ?? '-' }}
            </div>
          </div>
        </div>
      </div>
    </n-scrollbar>
  </CrmCard>
  <CrmDrawer
    v-model:show="visible"
    :width="600"
    :show-continue="false"
    no-padding
    :ok-text="t('common.update')"
    :title="t('system.license.authorityUpdate')"
    :loading="loading"
    @cancel="handleCancel"
    @confirm="handleConfirm"
  >
    <n-scrollbar class="p-[24px]">
      <n-form ref="formRef" :model="form" require-mark-placement="left" label-placement="left">
        <n-form-item
          path="licenseCode"
          :label="t('system.license')"
          :rule="[{ required: true, message: t('common.notNull', { value: t('system.license') }) }]"
        >
          <CrmUpload
            v-model:file-list="fileList"
            accept="none"
            directory-dnd
            :is-limit="false"
            :show-sub-text="false"
            :file-type-tip="t('crmImportButton.onlyAllowFileTypeTip')"
          />
          <n-input v-model:value="form.licenseCode" class="mt-[16px]" type="textarea" placeholder="License Code" />
        </n-form-item>
      </n-form>
    </n-scrollbar>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { FormInst, NButton, NForm, NFormItem, NInput, NScrollbar, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { addCommasToNumber } from '@lib/shared/method';
  import { LicenseInfo } from '@lib/shared/models/system/authorizedManagement';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { Description } from '@/components/pure/crm-description/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSvg from '@/components/pure/crm-svg/index.vue';
  import CrmUpload from '@/components/pure/crm-upload/index.vue';

  import { addLicense } from '@/api/modules';
  import useLicenseStore from '@/store/modules/setting/license';

  const licenseStore = useLicenseStore();

  const { t } = useI18n();
  const Message = useMessage();

  const form = ref<{
    licenseCode: string | null;
  }>({
    licenseCode: '',
  });
  const visible = ref(false);

  const fileList = ref([]);

  function handleCancel() {
    visible.value = false;
    form.value.licenseCode = null;
    fileList.value = [];
  }

  function handleUpdate() {
    visible.value = true;
  }

  const formRef = ref<FormInst | null>(null);

  const loading = ref(false);

  function handleConfirm() {
    formRef.value?.validate(async (error: any) => {
      if (!error) {
        try {
          loading.value = true;
          await addLicense(form.value.licenseCode as string);
          licenseStore.getValidateLicense();
          Message.success(t('common.updateSuccess'));
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

  function handlePreviewText() {
    const reader = new FileReader();
    if (typeof FileReader === 'undefined') {
      Message.warning(t('system.license.uploadFileTip'));
    }
    reader.readAsText((fileList.value[0] as any)?.file, 'UTF-8');
    reader.onload = (e) => {
      form.value.licenseCode = e.target?.result as string;
    };
  }

  watch(
    () => fileList.value,
    (val) => {
      if (val.length) {
        handlePreviewText();
      }
    }
  );

  const getStatusText = (status: string) => {
    switch (status) {
      case 'valid':
        return t('system.license.valid');
      case 'expired':
        return t('system.license.invalid');
      default:
        return t('system.license.failure');
    }
  };

  const licenseInfo = computed(() => {
    const { corporation, expired, product, edition, count, licenseVersion } = licenseStore?.licenseInfo || {};
    return [
      {
        label: t('system.license.customerName'),
        value: corporation,
      },
      { label: t('system.license.authorizationTime'), value: expired },
      { label: t('system.license.productName'), value: product },
      { label: t('system.license.productionVersion'), value: edition },
      { label: t('system.license.licenseVersion'), value: licenseVersion },
      { label: t('system.license.authorizationsCount'), value: addCommasToNumber(count || 0) },
      {
        label: t('system.license.authorizationStatus'),
        value: getStatusText(licenseStore.licenseInfo?.status as string),
      },
    ];
  });

  onBeforeMount(() => {
    licenseStore.getValidateLicense();
  });
</script>

<style scoped lang="less">
  .license-content-class {
    @apply flex !w-full items-center justify-center;
  }
  .license-wrapper {
    display: flex;
    align-items: center;
    padding: 24px;
    width: 936px;
    min-height: 628px;
    font-size: 18px;
    color: var(--text-n1);
    flex-direction: column;
    .license-content {
      display: flex;
      gap: 220px;
    }
    .license-column {
      display: flex;
      align-items: flex-start;
      text-align: left;
      flex-direction: column;
    }
  }
</style>

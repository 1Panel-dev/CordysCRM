<template>
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
    <n-scrollbar class="p-[24px] pb-0">
      <n-form ref="formRef" :model="form" require-mark-placement="left" label-placement="left">
        <n-form-item
          path="licenseCode"
          :label="t('system.license')"
          :rule="[{ required: true, message: t('system.license.LicenseIsRequired') }]"
        >
          <CrmUpload
            v-model:file-list="fileList"
            accept="none"
            directory-dnd
            :is-limit="false"
            :show-sub-text="false"
            :file-type-tip="t('crmImportButton.onlyAllowFileTypeTip')"
          />
          <n-input
            v-model:value="form.licenseCode"
            :autosize="{
              minRows: 5,
            }"
            class="mt-[16px] max-h-[calc(100vh-334px)]"
            type="textarea"
            placeholder="License Code"
          />
        </n-form-item>
      </n-form>
    </n-scrollbar>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, NForm, NFormItem, NInput, NScrollbar, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmUpload from '@/components/pure/crm-upload/index.vue';

  import { addLicense } from '@/api/modules';
  import useLicenseStore from '@/store/modules/setting/license';

  const licenseStore = useLicenseStore();

  const { t } = useI18n();
  const Message = useMessage();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const form = ref<{
    licenseCode: string | null;
  }>({
    licenseCode: '',
  });

  const fileList = ref([]);

  function handleCancel() {
    visible.value = false;
    form.value.licenseCode = null;
    fileList.value = [];
  }

  const formRef = ref<FormInst | null>(null);

  const loading = ref(false);

  function handleConfirm() {
    formRef.value?.validate(async (error: any) => {
      if (!error) {
        try {
          loading.value = true;
          let code = form.value.licenseCode as string;
          if (/^\d+$/.test(code)) {
            code = `"${code}"`;
          }
          await addLicense(code);
          licenseStore.getValidateLicense();
          Message.success(t('common.updateSuccess'));
          handleCancel();
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          loading.value = false;
          window.location.reload();
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
</script>

<style scoped></style>

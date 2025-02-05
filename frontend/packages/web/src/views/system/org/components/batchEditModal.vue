<template>
  <CrmModal
    v-model:show="showEditDrawer"
    size="small"
    :title="t('common.batchEdit')"
    :ok-loading="loading"
    @cancel="cancelHandler"
  >
    <n-form ref="formRef" :model="form" label-placement="left" label-width="auto">
      <n-form-item
        :rule="[{ required: true, message: t('common.notNull', { value: `${t('org.attributes')}` }) }]"
        require-mark-placement="left"
        label-placement="left"
        path="attributes"
        :label="t('org.attributes')"
      >
        <n-select
          v-model:value="form.attributes"
          :placeholder="t('common.pleaseSelect')"
          :options="attributesOptions"
        />
      </n-form-item>
      <n-form-item
        :rule="[{ required: true, message: t('common.value.notNull') }]"
        require-mark-placement="left"
        label-placement="left"
        path="value"
        :label="t('common.batchUpdate')"
      >
        <n-select v-model:value="form.value" :placeholder="t('common.pleaseSelect')" :options="valueOptions" />
      </n-form-item>
    </n-form>
    <template #footer>
      <div class="flex w-full items-center justify-end gap-[12px]">
        <n-button :disabled="loading" secondary @click="cancelHandler">
          {{ t('common.cancel') }}
        </n-button>
        <n-button :loading="loading" type="primary" @click="confirmHandler">
          {{ t('common.update') }}
        </n-button>
      </div>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, NButton, NForm, NFormItem, NSelect, useMessage } from 'naive-ui';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const Message = useMessage();
  const { t } = useI18n();

  const showEditDrawer = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const form = ref({
    attributes: '',
    value: '',
  });

  const attributesOptions = ref([]);

  const valueOptions = ref([]);

  function cancelHandler() {
    showEditDrawer.value = false;
  }

  const loading = ref<boolean>(false);
  const formRef = ref<FormInst | null>(null);
  function confirmHandler() {
    formRef.value?.validate((error) => {
      if (!error) {
        try {
          loading.value = true;
          Message.success(t('common.updateSuccess'));
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        }
      }
    });
  }
</script>

<style scoped></style>

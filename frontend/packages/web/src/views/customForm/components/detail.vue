<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    no-padding
    :width="800"
    :footer="false"
    :title="title"
    :view-size="formViewSize"
  >
    <template #titleRight>
      <n-button v-if="isAdmin" type="primary" ghost class="n-btn-outline-primary" @click="emit('edit', props.sourceId)">
        {{ t('common.edit') }}
      </n-button>
      <n-button v-if="isAdmin" type="error" ghost class="ml-[12px]" @click="handleDelete">
        {{ t('common.delete') }}
      </n-button>
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.CUSTOM_FORM"
            :source-id="props.sourceId"
            :column="3"
            :refresh-key="props.refreshId"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            :readonly="!isAdmin"
            :customFormId="props.customFormId"
            @init="handleInit"
          />
        </div>
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';

  import { deleteCustomFormData } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
    refreshId?: number;
    customFormId?: string;
  }>();
  const emit = defineEmits<{
    (e: 'edit', sourceId: string): void;
    (e: 'refresh'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();
  const title = ref('');
  const isAdmin = ref(false);
  const formViewSize = ref<FormViewSize>('large');

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>, config?: FormConfig) {
    title.value = name || '';
    isAdmin.value = !!detail?.isAdmin;
    formViewSize.value = config?.viewSize || 'large';
  }

  // 删除
  function handleDelete() {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(title.value) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomFormData(props.sourceId);
          Message.success(t('common.deleteSuccess'));
          emit('refresh');
          visible.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }
</script>

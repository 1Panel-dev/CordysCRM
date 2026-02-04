<template>
  <CrmDrawer
    v-model:show="showDrawer"
    :title="`${props.sourceName} ${
      props.formKey === FormDesignKeyEnum.FOLLOW_RECORD ? t('module.customer.followRecord') : t('common.plan')
    }`"
    width="800"
    no-padding
    :footer="false"
  >
    <template #titleRight>
      <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleEdit">
        {{ t('common.edit') }}
      </n-button>
      <n-button type="error" ghost class="n-btn-outline-error ml-[12px]" @click="handleDelete">
        {{ t('common.delete') }}
      </n-button>
    </template>
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="props.formKey"
            :source-id="props.sourceId"
            :refresh-key="props.refreshKey"
            :column="3"
            label-width="auto"
            value-align="start"
          />
        </div>
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum;
    sourceName: string;
    refreshKey: number;
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'delete'): void;
    (e: 'edit'): void;
  }>();

  const { t } = useI18n();

  function handleDelete() {
    emit('delete');
  }

  function handleEdit() {
    emit('edit');
  }
</script>

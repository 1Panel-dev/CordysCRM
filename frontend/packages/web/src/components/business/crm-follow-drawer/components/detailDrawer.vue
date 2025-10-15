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
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmCard hide-footer>
        <div class="mb-[16px] flex justify-between">
          <div class="text-[16px] font-semibold">{{ t('common.detail') }}</div>
          <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleDelete">
            {{ t('common.delete') }}
          </n-button>
        </div>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="props.formKey"
            :source-id="props.sourceId"
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
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'delete'): void;
  }>();

  const { t } = useI18n();

  function handleDelete() {
    showDrawer.value = false;
    emit('delete');
  }
</script>

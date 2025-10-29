<template>
  <CrmDrawer v-model:show="visible" resizable no-padding width="800" :footer="false" :title="title">
    <template #titleRight>
      <n-button
        v-permission="['PRODUCT_MANAGEMENT:UPDATE']"
        type="primary"
        ghost
        class="n-btn-outline-primary"
        @click="emit('edit', props.sourceId)"
      >
        {{ t('common.edit') }}
      </n-button>
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.PRODUCT"
            :source-id="props.sourceId"
            :column="3"
            :refresh-key="props.refreshId"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            @init="handleInit"
          />
        </div>
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';

  const props = defineProps<{
    sourceId: string;
    refreshId?: number;
  }>();
  const emit = defineEmits<{
    (e: 'edit', sourceId: string): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const title = ref('');

  function handleInit(type?: CollaborationType, name?: string) {
    title.value = name || '';
  }
</script>

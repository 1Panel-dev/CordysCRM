<template>
  <CrmDrawer v-model:show="show" :title="sourceName" :width="800" :footer="false">
    <template #titleRight>
      <n-button
        v-permission="['PRICE:UPDATE']"
        type="primary"
        ghost
        class="n-btn-outline-primary ml-[12px]"
        @click="handleEdit"
      >
        {{ t('common.edit') }}
      </n-button>
    </template>
    <CrmFormDescription
      ref="descriptionRef"
      :form-key="FormDesignKeyEnum.PRICE"
      :source-id="props.id"
      :column="3"
      label-width="auto"
      value-align="start"
      tooltip-position="top-start"
      class="p-[8px]"
      @init="handleDescriptionInit"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';

  const props = defineProps<{
    id: string;
  }>();
  const emit = defineEmits<{
    (e: 'edit', id: string): void;
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    default: false,
  });

  const sourceName = ref<string>('');

  function handleDescriptionInit(
    _collaborationType?: CollaborationType,
    _sourceName?: string,
    detail?: Record<string, any>
  ) {
    sourceName.value = _sourceName || '';
  }

  function handleEdit() {
    emit('edit', props.id);
    show.value = false;
  }
</script>

<style lang="less" scoped></style>

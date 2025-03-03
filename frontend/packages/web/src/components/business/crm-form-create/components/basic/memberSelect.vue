<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
  >
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <CrmUserTagSelector v-model:selected-list="value" :multiple="fieldConfig.multiple" />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem } from 'naive-ui';

  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[]): void;
  }>();

  const value = defineModel<SelectedUsersItem[]>('value', {
    default: [],
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val || [];
    },
    {
      immediate: true,
    }
  );

  watch(
    () => value.value,
    (val) => {
      emit(
        'change',
        val.map((item) => item.id)
      );
    },
    {
      deep: true,
    }
  );
</script>

<style lang="less" scoped></style>

<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
  >
    <div v-if="props.fieldConfig.description" class="n-form-item-desc" v-html="props.fieldConfig.description"></div>
    <CrmUserTagSelector v-model:selected-list="value" :multiple="fieldConfig.type === FieldTypeEnum.MEMBER_MULTIPLE" />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem } from 'naive-ui';

  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { FieldTypeEnum } from '../../enum';
  import { FormCreateField } from '../../types';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();

  const value = defineModel<SelectedUsersItem[]>('value', {
    default: [],
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
    }
  );
</script>

<style lang="less" scoped></style>

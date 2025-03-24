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
    <CrmUserTagSelector
      v-model:selected-list="selectedUsers"
      :multiple="fieldConfig.multiple"
      :drawer-title="t('crmFormDesign.selectDataSource', { type: props.fieldConfig.name })"
      :api-type-key="MemberApiTypeEnum.FORM_FIELD"
      :disabled-node-types="
        props.fieldConfig.type === FieldTypeEnum.MEMBER
          ? [DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]
          : [DeptNodeTypeEnum.USER, DeptNodeTypeEnum.ROLE]
      "
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem } from 'naive-ui';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberApiTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string | number | (string | number)[]): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string | number | (string | number)[]>('value', {
    default: [],
  });
  const selectedUsers = ref<SelectedUsersItem[]>(props.fieldConfig.initialOptions || []);

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
    () => props.fieldConfig.initialOptions,
    (val) => {
      selectedUsers.value = val || [];
    },
    {
      immediate: true,
    }
  );

  watch(
    () => selectedUsers.value,
    (val) => {
      const ids = val.map((item) => item.id);
      value.value = props.fieldConfig.multiple ? ids : ids[0];
      emit('change', ids);
    },
    {
      deep: true,
    }
  );
</script>

<style lang="less" scoped></style>

<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
  >
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <CrmUserTagSelector
      v-model:selected-list="selectedUsers"
      :multiple="[FieldTypeEnum.MEMBER_MULTIPLE, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(fieldConfig.type)"
      :drawer-title="t('crmFormDesign.selectDataSource', { type: props.fieldConfig.name })"
      :api-type-key="MemberApiTypeEnum.FORM_FIELD"
      :disabled="props.fieldConfig.editable === false"
      :member-types="
        [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(props.fieldConfig.type)
          ? [
              {
                label: t('menu.settings.org'),
                value: MemberSelectTypeEnum.ORG,
              },
            ]
          : [
              {
                label: t('menu.settings.org'),
                value: MemberSelectTypeEnum.ONLY_ORG,
              },
            ]
      "
      :disabled-node-types="
        [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(props.fieldConfig.type)
          ? [DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]
          : [DeptNodeTypeEnum.USER, DeptNodeTypeEnum.ROLE]
      "
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem } from 'naive-ui';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
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
      if (!props.needInitDetail) {
        value.value = val || value.value || [];
        emit('change', value.value);
      }
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
      value.value = [FieldTypeEnum.MEMBER_MULTIPLE, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(props.fieldConfig.type)
        ? ids
        : ids[0];
      emit(
        'change',
        [FieldTypeEnum.MEMBER_MULTIPLE, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(props.fieldConfig.type)
          ? ids
          : ids[0]
      );
    },
    {
      deep: true,
    }
  );
</script>

<style lang="less" scoped></style>

<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
  >
    <template #label>
      <div class="flex items-center gap-[4px]">
        {{ props.fieldConfig.name }}
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
    </template>
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <CrmUserTagSelector
      v-model:selected-list="selectedUsers"
      :multiple="[FieldTypeEnum.MEMBER_MULTIPLE, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(fieldConfig.type)"
      :drawer-title="t('crmFormDesign.selectDataSource', { type: props.fieldConfig.name })"
      :api-type-key="MemberApiTypeEnum.FORM_FIELD"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      :member-types="memberTypes"
      :disabled-node-types="
        [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(props.fieldConfig.type)
          ? [DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]
          : [DeptNodeTypeEnum.USER, DeptNodeTypeEnum.ROLE]
      "
      :class="props.isSubTableField ? '!w-[150px]' : ''"
      @confirm="handleConfirm"
      @delete-tag="handleConfirm"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem } from 'naive-ui';

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
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string | number | (string | number)[]): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string | number | (string | number)[]>('value', {
    default: [],
  });
  const selectedUsers = ref<SelectedUsersItem[]>(props.fieldConfig.initialOptions || []);
  const memberTypes = computed(() => {
    if ([FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(props.fieldConfig.type)) {
      return [
        {
          label: t('menu.settings.org'),
          value: MemberSelectTypeEnum.ORG,
        },
      ];
    }
    return [
      {
        label: t('menu.settings.org'),
        value: MemberSelectTypeEnum.ONLY_ORG,
      },
    ];
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value =
          [FieldTypeEnum.MEMBER_MULTIPLE, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(props.fieldConfig.type) &&
          !Array.isArray(val)
            ? [val]
            : val || value.value || [];
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
      if ([FieldTypeEnum.MEMBER_MULTIPLE, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(props.fieldConfig.type)) {
        selectedUsers.value = val as SelectedUsersItem[];
      } else if (Array.isArray(val) && val.length) {
        selectedUsers.value = [val[0]];
      } else {
        selectedUsers.value = val || [];
      }
    },
    {
      immediate: true,
    }
  );

  function handleConfirm() {
    if (selectedUsers.value) {
      const ids = selectedUsers.value.map((item) => item.id);
      value.value = [FieldTypeEnum.MEMBER_MULTIPLE, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(props.fieldConfig.type)
        ? ids
        : ids[0];
      emit(
        'change',
        [FieldTypeEnum.MEMBER_MULTIPLE, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(props.fieldConfig.type)
          ? ids
          : ids[0]
      );
    } else {
      value.value = [];
      emit('change', []);
    }
  }
</script>

<style lang="less" scoped></style>

<template>
  <CrmMemberSelectorBox
    v-model:value="value"
    v-model:selected-list="selectedList"
    :label="label"
    :add-text="addText"
    :required="required"
    :max-count="maxCount"
    :tip-text="memberLimitTip"
    :member-types="memberTypes"
    :api-type-key="apiTypeKey"
    :disabled-node-types="disabledNodeTypes"
    :disabled="props.disabled"
  />
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import { MemberApiTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmMemberSelectorBox from '@/components/business/crm-member-selector-box/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';

  const props = withDefaults(
    defineProps<{
      label?: string;
      addText?: string;
      required?: boolean;
      maxCount?: number;
      memberTypes?: Option[];
      apiTypeKey?: MemberApiTypeEnum;
      disabledNodeTypes?: DeptNodeTypeEnum[];
      limitLabel?: string;
      disabled?: boolean;
    }>(),
    {
      label: '',
      addText: '',
      required: false,
      maxCount: 1,
    }
  );

  const { t } = useI18n();

  const value = defineModel<string[]>('value', {
    default: () => [],
  });
  const selectedList = defineModel<SelectedUsersItem[]>('selectedList', {
    default: () => [],
  });

  const isMemberInvalid = computed(() => {
    const selectedCount = selectedList.value.length;
    return (props.required && selectedCount === 0) || Boolean(props.maxCount && selectedCount > props.maxCount);
  });

  const memberLimitTip = computed(() =>
    isMemberInvalid.value
      ? t('process.process.flow.addMemberLimitTip', { count: props.maxCount, target: props.limitLabel })
      : t('process.process.flow.maxAddMemberTip', { count: props.maxCount, target: props.limitLabel })
  );
</script>

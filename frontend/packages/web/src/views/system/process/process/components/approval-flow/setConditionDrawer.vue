<template>
  <CrmDrawer
    v-model:show="show"
    :width="1000"
    :min-width="600"
    :okText="t('common.confirm')"
    footer
    :title="t('process.process.flow.setTriggerCondition')"
    @confirm="handleConfirm"
  >
    <div class="flex flex-col gap-[16px]">
      <n-form label-placement="top" require-mark-placement="right">
        <n-form-item :label="t('process.process.flow.conditionName')">
          <n-input v-model:value="form.name" :maxlength="255" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </n-form>

      <FilterContent
        ref="filterContentRef"
        v-model:form-model="form.conditionConfig"
        no-filter-option
        :config-list="[]"
        :custom-list="[]"
      />
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { reactive, ref, watch } from 'vue';
  import { NForm, NFormItem, NInput } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalConditionBranch } from '@lib/shared/models/system/process';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import type { FilterForm } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  defineOptions({
    name: 'SetConditionDrawer',
  });

  const props = defineProps<{
    branch: ApprovalConditionBranch | null;
  }>();
  const emit = defineEmits<{
    (
      e: 'confirm',
      payload: {
        name: string;
        conditionConfig: FilterForm;
      }
    ): void;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const { t } = useI18n();

  const filterContentRef = ref<InstanceType<typeof FilterContent>>();

  function createDefaultFormModel(): FilterForm {
    return {
      searchMode: 'AND',
      list: [{ dataIndex: null, operator: undefined, value: null, type: FieldTypeEnum.INPUT }],
    };
  }

  const form = reactive<{
    name: string;
    conditionConfig: FilterForm;
  }>({
    name: '',
    conditionConfig: createDefaultFormModel(),
  });

  function initDraft(branch: ApprovalConditionBranch | null) {
    form.name = branch?.name ?? '';
    form.conditionConfig = cloneDeep(branch?.conditionConfig ?? createDefaultFormModel());
  }

  watch(
    () => show.value,
    (visible) => {
      if (visible) {
        initDraft(props.branch);
      }
    }
  );

  watch(
    () => props.branch?.id,
    () => {
      if (show.value) {
        initDraft(props.branch);
      }
    }
  );

  function handleConfirm() {
    emit('confirm', {
      name: form.name.trim(),
      conditionConfig: cloneDeep(form.conditionConfig),
    });
    show.value = false;
  }
</script>

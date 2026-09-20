<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : props.formConfig?.labelPos"
    :show-label="!props.isSubTableRender && !props.isDescriptionRender"
    class="crm-form-create-statistic"
  >
    <template #label>
      <div class="flex w-full items-center justify-between">
        <div
          v-if="props.fieldConfig.showLabel"
          class="flex h-[22px] w-full items-center gap-[4px] overflow-hidden whitespace-nowrap"
        >
          <div class="one-line-text">{{ props.fieldConfig.name }}</div>
          <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
        </div>
        <div v-else class="h-[22px]"></div>
        <CrmPopConfirm
          v-if="!props.isDesignRender && props.sourceId"
          v-model:show="popShow"
          :title="t('crmFormCreate.reCalculation')"
          icon-type="warning"
          :content="t('crmFormCreate.reCalculationTip')"
          :positive-text="t('common.confirm')"
          trigger="click"
          :negative-text="t('common.cancel')"
          placement="bottom-end"
          @confirm="handleReCalculation()"
        >
          <n-button type="warning" quaternary>
            <template #icon><CrmIcon type="iconicon_error_circle_filled" /></template>
            {{ t('crmFormCreate.reCalculation') }}
          </n-button>
        </CrmPopConfirm>
      </div>
    </template>
    <div
      v-if="props.fieldConfig.description && !props.isSubTableRender"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <CrmInputNumber
      v-model:value="value"
      :path="props.path"
      :field-config="props.fieldConfig"
      :disabled="true"
      :need-init-detail="props.needInitDetail"
      pureInput
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NButton, NDivider, NFormItem, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';
  import CrmInputNumber from '../basic/inputNumber.vue';

  import { refreshStatistic } from '@/api/modules';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    sourceId?: string; // 资源ID, 新建时没有(记录还没落库, 统计值无处可算), 刷新按钮也不展示
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
    isDescriptionRender?: boolean; // 是否是描述渲染
    isDesignRender?: boolean; // 是否是设计渲染
    formDetail?: Record<string, any>;
    disabled?: boolean;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const value = defineModel<number>('value', {
    default: null,
  });

  const popShow = ref(false);
  async function handleReCalculation() {
    if (!props.sourceId) {
      return;
    }
    try {
      value.value = await refreshStatistic(props.sourceId, props.fieldConfig.id);
      Message.success(t('common.refreshSuccess'));
      popShow.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
</script>

<style lang="less" scoped>
  .crm-form-create-statistic {
    :deep(.n-form-item-label__text) {
      width: 100%;
    }
  }
</style>

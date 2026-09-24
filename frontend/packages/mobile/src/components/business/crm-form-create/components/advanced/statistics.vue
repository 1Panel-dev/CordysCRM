<template>
  <van-field
    v-model="value"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    type="text"
    placeholder=""
    :disabled="true"
  >
    <template #button>
      <van-button size="mini" type="warning" @click="handleReCalculation">
        {{ t('formCreate.advanced.reCalculation') }}
      </van-button>
    </template>
    <template v-if="props.fieldConfig.numberFormat === 'percent'" #right-icon> % </template>
  </van-field>
</template>

<script setup lang="ts">
  import { showConfirmDialog, showToast } from 'vant';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import { refreshStatistic } from '@/api/modules';
  import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types.js';
  import { formatNumberValueToString } from '@lib/shared/method/formCreate';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    sourceId?: string; // 资源ID, 新建时没有(记录还没落库, 统计值无处可算), 刷新按钮也不展示
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
    isDesignRender?: boolean; // 是否是设计渲染
    formDetail?: Record<string, any>;
  }>();

  const { t } = useI18n();

  const value = defineModel<number | string>('value', {
    default: null,
  });

  function handleReCalculation() {
    showConfirmDialog({
      title: t('formCreate.advanced.reCalculation'),
      message: t('formCreate.advanced.reCalculationTip'),
      confirmButtonText: t('common.confirm'),
      confirmButtonColor: 'var(--warning-yellow)',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          if (!props.sourceId) {
            return Promise.resolve(false);
          }
          try {
            const res = await refreshStatistic(props.sourceId, props.fieldConfig.id);
            value.value = formatNumberValueToString(res, props.fieldConfig);
            showToast({
              message: t('formCreate.advanced.reCalculationSuccess'),
              type: 'success',
            });
            return Promise.resolve(true);
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
            return Promise.resolve(false);
          }
        } else {
          return Promise.resolve(true);
        }
      },
    });
  }
</script>

<style lang="less" scoped>
  .crm-form-create-statistic {
    :deep(.n-form-item-label__text) {
      width: 100%;
    }
  }
</style>

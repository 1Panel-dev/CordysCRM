<template>
  <div :class="`flex items-center ${props.showTotal ? 'justify-between' : 'justify-end'}`">
    <div v-show="props.showTotal" class="text-[var(--text-n2)]">
      {{ t('crmPagination.total', { count: props.itemCount }) }}
    </div>
    <n-pagination
      v-model:page="page"
      v-model:page-size="pageSize"
      :size="props.size"
      :item-count="props.itemCount"
      :display-order="props.displayOrder"
      :show-size-picker="props.showSizePicker"
      :page-sizes="pageSizes"
      :show-quick-jumper="props.showQuickJumper"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #prev>
        <CrmIcon type="iconicon_chevron_left" :size="16" />
      </template>
      <template #next>
        <CrmIcon type="iconicon_chevron_right" :size="16" />
      </template>
      <template #goto> {{ t('crmPagination.goto') }} </template>
    </n-pagination>
  </div>
</template>

<script lang="ts" setup>
  import { NPagination, PaginationSizeOption } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

  const props = withDefaults(
    defineProps<{
      itemCount?: number; // 总条数
      size?: 'small' | 'medium' | 'large';
      displayOrder?: Array<'pages' | 'size-picker' | 'quick-jumper'>; // 不同部分的展示顺序
      showSizePicker?: boolean; // 是否显示每页条数的选择器
      showQuickJumper?: boolean; // 是否显示快速跳转
      showTotal?: boolean; // 显示总量
    }>(),
    {
      size: 'medium',
      displayOrder: () => ['size-picker', 'pages', 'quick-jumper'],
      showSizePicker: true,
      showQuickJumper: true,
      showTotal: true,
    }
  );
  const emit = defineEmits<{
    (e: 'handlePageChange', value: number): void;
    (e: 'handlePageSizeChange', value: number): void;
  }>();

  const { t } = useI18n();

  const page = defineModel<number>('page');
  const pageSize = defineModel<number>('pageSize');

  const pageSizes = ref<PaginationSizeOption[]>([
    {
      label: t('crmPagination.pageSizes', { count: 10 }),
      value: 10,
    },
    {
      label: t('crmPagination.pageSizes', { count: 20 }),
      value: 20,
    },
    {
      label: t('crmPagination.pageSizes', { count: 30 }),
      value: 30,
    },
    {
      label: t('crmPagination.pageSizes', { count: 40 }),
      value: 40,
    },
    {
      label: t('crmPagination.pageSizes', { count: 50 }),
      value: 50,
    },
  ]);

  function handlePageChange(value: number) {
    emit('handlePageChange', value);
  }
  function handlePageSizeChange(value: number) {
    emit('handlePageSizeChange', value);
  }
</script>

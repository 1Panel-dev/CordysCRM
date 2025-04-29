<template>
  <div
    :class="`crm-pagination--${props.size} flex items-center ${props.showTotal ? 'justify-between' : 'justify-end'}`"
  >
    <div v-show="props.showTotal" class="total text-[var(--text-n2)]">
      {{ t('crmPagination.total', { count: props.itemCount }) }}
    </div>
    <n-pagination
      v-model:page="page"
      v-model:page-size="pageSize"
      :size="props.size"
      :item-count="props.itemCount"
      :display-order="displayOrder"
      :show-size-picker="props.showSizePicker"
      :page-sizes="pageSizes"
      :page-slot="props.pageSlot"
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
      <template #goto>
        {{ t('crmPagination.goto') }}
      </template>
      <template v-if="props.showQuickJumper || isSimple" #suffix>
        / {{ totalPages }} {{ t('crmPagination.page') }}
      </template>
    </n-pagination>
  </div>
</template>

<script lang="ts" setup>
  import { NPagination, PaginationSizeOption } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  export interface CrmPaginationProps {
    itemCount?: number;
    size?: 'small' | 'medium' | 'large';
    showSizePicker?: boolean;
    showQuickJumper?: boolean;
    showTotal?: boolean;
    isSimple?: boolean;
    pageSlot?: number;
  }

  const props = withDefaults(defineProps<CrmPaginationProps>(), {
    size: 'medium',
    showSizePicker: true,
    showQuickJumper: true,
    showTotal: true,
    isSimple: false,
    pageSlot: 6,
  });
  const emit = defineEmits<{
    (e: 'handlePageChange', value: number): void;
    (e: 'handlePageSizeChange', value: number): void;
  }>();

  const { t } = useI18n();

  const page = defineModel<number>('page');
  const pageSize = defineModel<number>('pageSize');

  // 计算总页数
  const totalPages = computed(() => {
    return Math.ceil((props.itemCount || 0) / (pageSize.value || 10));
  });

  const displayOrder = computed<Array<'pages' | 'size-picker' | 'quick-jumper'>>(() => {
    const order: Array<'pages' | 'size-picker' | 'quick-jumper'> = [];
    if (props.showSizePicker) {
      order.push('size-picker');
    }
    if (!props.isSimple) {
      order.push('pages');
    }
    if (props.showQuickJumper || props.isSimple) {
      order.push('quick-jumper');
    }
    return order;
  });

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

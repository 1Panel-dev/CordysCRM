<template>
  <div class="mb-[16px] flex w-full items-center overflow-hidden">
    <div class="mr-[24px] flex flex-1 items-center overflow-hidden">
      <n-button
        v-if="showArrows"
        :disabled="!canScrollLeft"
        size="small"
        secondary
        class="mr-[8px] px-[3px]"
        @click="scrollLeft"
      >
        <template #icon>
          <CrmIcon type="iconicon_chevron_left" />
        </template>
      </n-button>

      <div ref="scrollWrapperRef" class="scroll-container flex flex-1 gap-[8px] overflow-x-auto">
        <CrmTag
          v-for="tag in tags"
          :key="tag.id"
          :type="activeTab === tag.id ? 'primary' : 'default'"
          theme="light"
          custom-class="cursor-pointer"
          @click="handleChangeActive(tag.id)"
        >
          {{ tag.name }}
        </CrmTag>
        <n-button class="outline--secondary" size="small" @click="handleAdd">
          ＋ {{ t('crmViewSelect.newView') }}
        </n-button>
      </div>

      <n-button
        v-if="showArrows"
        :disabled="!canScrollRight"
        size="small"
        secondary
        class="ml-[8px] px-[3px]"
        @click="scrollRight"
      >
        <template #icon>
          <CrmIcon type="iconicon_chevron_right" />
        </template>
      </n-button>
    </div>
    <div class="flex items-center gap-[12px]">
      <n-select
        v-model:value="activeTab"
        :options="options"
        filterable
        label-field="name"
        value-field="id"
        :reset-menu-on-options-change="false"
        :virtual-scroll="false"
        :show-checkmark="false"
        :render-option="renderOption"
        class="view-select w-[200px]"
        :node-props="getNodeProps"
        :menu-props="{ class: 'crm-view-select-menu' }"
        @update:show="setDraggerSort"
      >
        <template #header>
          <n-button type="primary" text @click="handleAdd">
            <template #icon>
              <n-icon><Add /></n-icon>
            </template>
            {{ t('crmViewSelect.add') }}
          </n-button>
        </template>
        <template #action>
          <n-button type="primary" text @click="handleManage">
            {{ t('crmViewSelect.manageViews') }}
          </n-button>
        </template>
      </n-select>
      <n-button
        v-if="
          ![
            FormDesignKeyEnum.FOLLOW_PLAN,
            FormDesignKeyEnum.FOLLOW_RECORD,
            FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
            FormDesignKeyEnum.CONTRACT,
            FormDesignKeyEnum.CONTRACT_PAYMENT,
            FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD,
            FormDesignKeyEnum.INVOICE,
          ].includes(props.type)
        "
        type="default"
        class="outline--secondary px-[8px]"
        @click="openViewDrawer"
      >
        <CrmIcon class="text-[var(--text-n1)]" type="iconicon_data" :size="16" />
      </n-button>
    </div>
  </div>
  <ManageViewsDrawer
    v-model:visible="manageViewsDrawerVisible"
    :type="props.type"
    :config-list="props.filterConfigList"
    :custom-list="props.customFieldsConfigList"
    @change-active="handleChangeActive"
    @handle-delete-or-disable="handleDeleteOrDisable"
  />
  <AddOrEditViewsDrawer
    v-model:visible="addOrEditViewsDrawerVisible"
    :type="props.type"
    :row="detail"
    :config-list="props.filterConfigList"
    :custom-list="props.customFieldsConfigList"
    @refresh="handleChangeActive"
  />
  <chartsDrawer
    v-if="isInitChartDrawer"
    v-model:show="chartDrawerVisible"
    :config-list="props.filterConfigList"
    :custom-list="props.customFieldsConfigList"
    :type="props.type"
    :default-view-id="activeTab"
    :advanced-original-form="props.advancedOriginalForm"
    :route-name="props.routeName"
    :pool-id="props.poolId"
    @generated-chart="handleGeneratedChart"
  />
</template>

<script setup lang="ts">
  import {
    NButton,
    NIcon,
    NSelect,
    NTooltip,
    SelectGroupOption,
    SelectOption,
    TabPaneProps,
    useMessage,
  } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getCopiedName } from '@lib/shared/method';
  import type { TableDraggedParams } from '@lib/shared/models/common';
  import { ViewItem } from '@lib/shared/models/view';

  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import AddOrEditViewsDrawer from './components/addOrEditViewsDrawer.vue';
  import ManageViewsDrawer from './components/manageViewsDrawer.vue';

  import useHiddenTab, { TabType } from '@/hooks/useHiddenTab';
  import useHorizontalScrollArrows from '@/hooks/useHorizontalScrollArrows';
  import useAppStore from '@/store/modules/app';
  import useViewStore from '@/store/modules/view';

  import Sortable from 'sortablejs';

  const chartsDrawer = defineAsyncComponent(() => import('./components/chartsDrawer.vue'));

  const { t } = useI18n();
  const viewStore = useViewStore();
  const Message = useMessage();
  const appStore = useAppStore();

  const props = defineProps<{
    type: TabType;
    filterConfigList: FilterFormItem[]; // 系统字段
    customFieldsConfigList?: FilterFormItem[]; // 自定义字段
    advancedOriginalForm?: FilterForm;
    poolId?: string | number;
    routeName?: string;
  }>();

  const emit = defineEmits<{
    (e: 'refreshTableData'): void;
    (e: 'generatedChart', filterResult: FilterResult, filterForm: FilterForm): void;
  }>();

  const activeTab = defineModel<string>('activeTab', { default: '' });
  async function handleChangeActive(id: string, refreshTable?: boolean) {
    await viewStore.loadCustomViews(props.type);
    if (id) {
      activeTab.value = id;
    }
    if (refreshTable) {
      emit('refreshTableData');
    }
  }
  async function handleDeleteOrDisable(id: string) {
    await viewStore.loadCustomViews(props.type);
    if (activeTab.value === id) {
      // 取第一个系统视图
      const list = [...viewStore.internalViews].filter((item) => item.enable);
      activeTab.value = list[0].id;
    }
  }

  const tags = computed(() =>
    [...viewStore.internalViews, ...viewStore.customViews].filter((item) => item.enable && item.fixed)
  );

  const sortData = computed(() => [...viewStore.internalViews, ...viewStore.customViews].filter((item) => item.enable));

  const { tabList, initTab } = useHiddenTab(props.type);

  onMounted(async () => {
    await initTab();
    await viewStore.loadInternalViews(props.type, tabList.value as TabPaneProps[]);
    await viewStore.loadCustomViews(props.type);
    nextTick(async () => {
      // 如果上一次的值存在则取上一次，不存在就取固定视图的第一个
      const lastTab = await viewStore.getActiveView(props.type);
      if (lastTab && sortData.value.find((item) => item.id === lastTab)) {
        activeTab.value = lastTab;
      } else {
        activeTab.value = tags.value[0].id;
      }
    });
  });

  watch(
    () => activeTab.value,
    async (val) => {
      viewStore.setActiveView(props.type, val);
    }
  );

  const scrollWrapperRef = ref<HTMLDivElement | null>(null);
  const { showArrows, scrollLeft, scrollRight, updateScrollStatus, canScrollLeft, canScrollRight } =
    useHorizontalScrollArrows(scrollWrapperRef);

  watch(tags, () => {
    nextTick(updateScrollStatus);
  });

  const options = computed(() => [
    {
      type: 'group',
      name: t('crmViewSelect.systemView'),
      key: 'internal',
      children: [...viewStore.internalViews].filter((item) => item.enable),
    },
    {
      type: 'group',
      name: t('crmViewSelect.myView'),
      key: 'custom',
      children: [...viewStore.customViews].filter((item) => item.enable),
    },
  ]);

  const addOrEditViewsDrawerVisible = ref(false);
  const detail = ref();
  function handleAdd() {
    detail.value = undefined;
    addOrEditViewsDrawerVisible.value = true;
  }

  const manageViewsDrawerVisible = ref(false);
  function handleManage() {
    manageViewsDrawerVisible.value = true;
  }

  async function handleCopy(option: ViewItem) {
    const res = await viewStore.getViewDetail(props.type, option);
    detail.value = { ...res, name: getCopiedName(res.name as string) };
    addOrEditViewsDrawerVisible.value = true;
  }

  function renderOption({ node, option }: { node: VNode; option: ViewItem }) {
    if (option.type === 'group') return node;
    node.children = [
      h('div', { class: 'flex items-center justify-between w-full' }, [
        h('div', { class: 'flex items-center gap-[8px] overflow-hidden flex-1' }, [
          h(CrmIcon, {
            type: 'iconicon_move',
            class: 'text-[var(--text-n4)] cursor-move drag-icon',
            size: 12,
            onMousedown: (e: MouseEvent) => {
              e.stopPropagation();
            },
            onClick: (e: MouseEvent) => {
              e.stopPropagation();
            },
          }),
          h(
            NTooltip,
            {
              delay: 300,
              disabled: tags.value.length > 1 || !option.fixed,
            },
            {
              trigger: () =>
                h(CrmIcon, {
                  type: option.fixed ? 'iconicon_pin_filled' : 'iconicon_pin',
                  class: `${tags.value.length > 1 || !option.fixed ? 'cursor-pointer' : 'cursor-not-allowed'} ${
                    option.fixed ? 'text-[var(--primary-8)]' : 'text-[var(--text-n1)]'
                  }`,
                  size: 12,
                  onClick: (e: MouseEvent) => {
                    e.stopPropagation(); // 阻止事件冒泡，防止影响 select 行为
                    if (tags.value.length <= 1 && option.fixed) {
                      return;
                    }
                    viewStore.toggleFixed(props.type, option);
                  },
                }),
              default: () => t('crmViewSelect.keepAFixedViewTip'),
            }
          ),
          h('span', {
            class: 'one-line-text',
            innerText: option.name,
          }),
        ]),
        // 右侧内容：复制图标
        ...(option.id === CustomerSearchTypeEnum.CUSTOMER_COLLABORATION
          ? []
          : [
              h(CrmIcon, {
                type: 'iconicon_file_copy',
                class: 'text-[var(--text-n4)]',
                size: 12,
                onClick: (e: MouseEvent) => {
                  e.stopPropagation(); // 阻止事件冒泡，防止影响 select 行为
                  handleCopy(option);
                },
              }),
            ]),
      ]),
    ];

    return node;
  }

  // 拖拽限制
  let warningInstance: ReturnType<typeof Message.warning> | null = null;

  function clearWarningInstance() {
    if (warningInstance) {
      warningInstance.destroy();
      warningInstance = null;
    }
  }
  function dragMoveValidator(fromRow: ViewItem, toRow: ViewItem) {
    if (fromRow?.type !== toRow?.type) {
      if (!warningInstance) {
        warningInstance = Message.warning(t('crmViewSelect.dragTip'), {
          duration: 0,
        });
      }
      return false;
    }
    clearWarningInstance();

    return true;
  }

  // 拖拽结束
  async function dragHandler(params: TableDraggedParams) {
    clearWarningInstance();
    viewStore.toggleDrag(props.type, params);
  }

  const getNodeProps = (option: SelectOption | SelectGroupOption) => ({
    'data-id': option.id,
  });

  const sortable = ref();

  function initSelectSortable(el: HTMLElement) {
    sortable.value = Sortable.create(el, {
      ghostClass: 'sortable-ghost',
      handle: '.drag-icon',
      draggable: '.n-base-select-option',
      filter: '.n-base-select-group-header',
      setData(dataTransfer) {
        dataTransfer.setData('Text', '');
      },
      onMove(evt) {
        if (!dragMoveValidator) return true;
        const draggedEl = evt.dragged as HTMLElement;
        const relatedEl = evt.related as HTMLElement;
        const allDraggable = Array.from(el.querySelectorAll('.n-base-select-option'));
        const fromRow = sortData.value[allDraggable.indexOf(draggedEl)];
        const toRow = sortData.value[allDraggable.indexOf(relatedEl)];
        return dragMoveValidator(fromRow, toRow);
      },
      onEnd(evt) {
        const { oldDraggableIndex, newDraggableIndex } = evt;
        if (oldDraggableIndex == null || newDraggableIndex == null || oldDraggableIndex === newDraggableIndex) {
          clearWarningInstance();
          return;
        }

        const data = sortData.value as any[];
        const rowKey = 'id';

        const moveId = data[oldDraggableIndex][rowKey];
        let targetId;
        let moveMode: 'AFTER' | 'BEFORE';
        if (newDraggableIndex >= data.length) {
          targetId = data[data.length - 1][rowKey];
          moveMode = 'AFTER';
        } else if (newDraggableIndex === 0) {
          targetId = data[0][rowKey];
          moveMode = 'BEFORE';
        } else if (oldDraggableIndex < newDraggableIndex) {
          targetId = data[newDraggableIndex][rowKey];
          moveMode = 'AFTER';
        } else {
          targetId = data[newDraggableIndex][rowKey];
          moveMode = 'BEFORE';
        }

        dragHandler?.({
          moveId,
          targetId,
          moveMode,
          oldIndex: oldDraggableIndex,
          newIndex: newDraggableIndex,
          orgId: appStore.orgId,
        });
      },
    });
  }

  async function setDraggerSort(show: boolean) {
    if (!show) return;
    await nextTick();
    setTimeout(() => {
      const el = document?.querySelector('.crm-view-select-menu .n-base-select-menu-option-wrapper');
      if (el) {
        // DOM已经存在，直接初始化
        initSelectSortable(el as HTMLElement);
      }
    }, 50);
  }

  const chartDrawerVisible = ref(false);
  const isInitChartDrawer = ref(false);
  function openViewDrawer() {
    isInitChartDrawer.value = true;
    nextTick(() => {
      chartDrawerVisible.value = true;
    });
  }

  function handleGeneratedChart(filterResult: FilterResult, filterForm: FilterForm, viewId: string) {
    activeTab.value = viewId;
    emit('generatedChart', filterResult, filterForm);
  }
</script>

<style lang="less" scoped>
  .view-select {
    :deep(.n-base-selection-label) {
      &::before {
        margin-left: 8px;
        white-space: nowrap;
        color: var(--text-n4);
        content: v-bind("`'${t('crmViewSelect.view')}'`");
      }
      .n-base-selection-label__render-label {
        padding-left: 40px;
      }
      .n-base-selection-placeholder {
        padding-left: 40px !important;
      }
      .n-base-selection-input {
        padding-left: 4px;
      }
    }
  }
  .scroll-container::-webkit-scrollbar {
    display: none;
  }
</style>

<style lang="less">
  .crm-view-select-menu {
    padding-bottom: 0;
    .n-base-select-menu__header {
      padding: 0 0 0 12px;
      border: none;
    }
    .n-base-select-option.n-base-select-option--grouped {
      margin: 0 6px;
      padding-right: 8px;
      padding-left: 8px;
      border-radius: @border-radius-mini;
    }
    .n-base-select-menu__action {
      padding: 0;
      text-align: center;
      box-shadow: 0 -1px 4px 0 #1f23291a;
    }
  }
</style>

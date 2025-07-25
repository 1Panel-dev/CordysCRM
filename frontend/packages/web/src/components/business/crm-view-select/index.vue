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
          :key="tag.value"
          :type="modelValue === tag.value ? 'primary' : 'default'"
          theme="light"
          custom-class="cursor-pointer"
        >
          {{ tag.label }}
        </CrmTag>
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
    <n-select
      v-model:value="modelValue"
      :options="options"
      filterable
      :show-checkmark="false"
      :render-option="renderOption"
      class="view-select w-[200px]"
      :menu-props="{
        class: 'crm-view-select-menu',
      }"
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
  </div>
  <ManageViewsDrawer
    v-model:visible="manageViewsDrawerVisible"
    :config-list="props.filterConfigList"
    :custom-list="props.customFieldsConfigList"
  />
  <AddOrEditViewsDrawer
    v-model:visible="addOrEditViewsDrawerVisible"
    :row="detail"
    :config-list="props.filterConfigList"
    :custom-list="props.customFieldsConfigList"
  />
</template>

<script setup lang="ts">
  import { NButton, NIcon, NSelect } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import AddOrEditViewsDrawer from './components/addOrEditViewsDrawer.vue';
  import ManageViewsDrawer from './components/manageViewsDrawer.vue';

  import useHorizontalScrollArrows from '@/hooks/useHorizontalScrollArrows';

  import type { SelectOption } from 'naive-ui';

  const { t } = useI18n();

  const props = defineProps<{
    internalViews: any[]; // TODO lmy
    customViews: any[]; // TODO lmy
    filterConfigList: FilterFormItem[]; // 系统字段
    customFieldsConfigList?: FilterFormItem[]; // 自定义字段
  }>();

  const modelValue = defineModel<string>('value', { default: '' });

  const tags = computed(() => [...props.internalViews, ...props.customViews].filter((item) => item.fixed));

  const scrollWrapperRef = ref<HTMLDivElement | null>(null);
  const { showArrows, scrollLeft, scrollRight, updateScrollStatus, canScrollLeft, canScrollRight } =
    useHorizontalScrollArrows(scrollWrapperRef);

  watch(tags, () => {
    nextTick(updateScrollStatus);
  });

  const options = computed(() => [
    {
      type: 'group',
      label: t('crmViewSelect.systemView'),
      key: 'internalViews',
      children: [...props.internalViews],
    },
    {
      type: 'group',
      label: t('crmViewSelect.myView'),
      key: 'my',
      children: [...props.customViews],
    },
  ]);

  const addOrEditViewsDrawerVisible = ref(false);
  function handleAdd() {
    addOrEditViewsDrawerVisible.value = true;
  }

  const manageViewsDrawerVisible = ref(false);
  function handleManage() {
    manageViewsDrawerVisible.value = true;
  }

  function toggleFixed(option: SelectOption) {
    // TODO lmy
    console.log('🤔️ =>', option);
  }

  const detail = ref();
  function handleCopy(option: SelectOption) {
    // TODO lmy
    detail.value = {
      name: `${option.name}copy`,
      condition: option.condition,
    };
    addOrEditViewsDrawerVisible.value = true;
  }

  // TODO 第一个icon是拖拽，同一个SelectGroup里可以拖拽，不能拖拽到另一个SelectGroup
  function renderOption({ node, option }: { node: VNode; option: SelectOption }) {
    if (option.type === 'group') return node;
    node.children = [
      h('div', { class: 'flex items-center justify-between w-full' }, [
        h('div', { class: 'flex items-center gap-[8px] overflow-hidden flex-1' }, [
          h(CrmIcon, {
            type: 'iconicon_move',
            class: 'text-[var(--text-n4)]',
            size: 12,
          }),
          h(CrmIcon, {
            type: option.fixed ? 'iconicon_pin_filled' : 'iconicon_pin',
            class: `cursor-pointer ${option.fixed ? 'text-[var(--primary-8)]' : 'text-[var(--text-n1)]'}`,
            size: 12,
            onClick: (e: MouseEvent) => {
              e.stopPropagation(); // 阻止事件冒泡，防止影响 select 行为
              toggleFixed(option);
            },
          }),
          h('span', {
            class: 'one-line-text',
            innerText: option.label,
          }),
        ]),
        // 右侧内容：复制图标
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
    ];

    return node;
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

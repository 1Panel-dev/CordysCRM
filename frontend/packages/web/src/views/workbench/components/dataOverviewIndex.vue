<template>
  <CrmCard hide-footer auto-height class="mb-[16px]">
    <div class="analytics-title">
      <div class="analytics-name flex items-center">
        {{ t('workbench.dataOverView') }}
      </div>
      <div class="flex items-center gap-[12px]">
        <n-tree-select
          v-model:value="activeDeptId"
          :options="departmentOptions"
          label-field="name"
          key-field="id"
          filterable
          :render-switcher-icon="renderSwitcherIconDom"
          class="w-[240px]"
          children-field="children"
          @update:value="changeHandler"
        />
        <n-button type="default" class="outline--secondary px-[8px]" @click="refresh">
          <CrmIcon class="text-[var(--text-n1)]" type="iconicon_refresh" :size="16" />
        </n-button>
      </div>
    </div>
    <overview ref="overviewRef" />
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NTreeSelect, TreeOption } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { mapTree } from '@lib/shared/method';
  import { GetHomeStatisticParams } from '@lib/shared/models/home';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import overview from './overview.vue';

  import { getHomeDepartmentTree } from '@/api/modules';
  import { useUserStore } from '@/store';

  const { t } = useI18n();

  const params = ref<GetHomeStatisticParams>({
    deptIds: [],
    searchType: '', // ALL/SELF/DEPARTMENT
  });

  const activeDeptId = ref('');

  const useStore = useUserStore();

  const originDepartmentOptions = ref<CrmTreeNodeData[]>([]);
  const departmentOptions = ref<CrmTreeNodeData[]>([]);

  function getSpringIds(children: CrmTreeNodeData[] | undefined): string[] {
    const offspringIds: string[] = [];
    mapTree(children || [], (e) => {
      offspringIds.push(e.id);
      return e;
    });
    return offspringIds;
  }

  function setSearchType(isInit: boolean, activeId?: string) {
    if (isInit) {
      params.value.searchType = useStore.userInfo.roles.some((e: any) => e?.dataScope === 'SELF') ? 'SELF' : 'ALL';
      activeDeptId.value = params.value.searchType === 'SELF' ? 'SELF' : originDepartmentOptions.value[0]?.id;
      return;
    }

    if (activeId) {
      activeDeptId.value = activeId;
    }

    if (activeDeptId.value === 'SELF') {
      params.value.searchType = 'SELF';
    } else if (activeDeptId.value !== originDepartmentOptions.value[0]?.id) {
      params.value.searchType = 'DEPARTMENT';
    } else if (useStore.isAdmin || activeDeptId.value === originDepartmentOptions.value[0]?.id) {
      params.value.searchType = 'ALL';
    } else {
      params.value.searchType = useStore.userInfo.roles.some((e: any) => e?.dataScope === 'SELF') ? 'SELF' : 'ALL';
    }
  }

  function renderSwitcherIconDom(nodeSwitchProps: { option: CrmTreeNodeData; expanded: boolean; selected: boolean }) {
    const { option } = nodeSwitchProps;
    return h(CrmIcon, {
      size: 16,
      type: option.children?.length ? 'iconicon_caret_right_small' : '',
      class: `text-[var(--text-n2)]`,
    });
  }

  async function initDepartList() {
    try {
      const result = await getHomeDepartmentTree();
      originDepartmentOptions.value = result;
      departmentOptions.value = [
        {
          id: 'SELF',
          name: t('opportunity.mine'),
          organizationId: '100001',
        },
        ...result,
      ];
      setSearchType(true);
      if (params.value.searchType === 'ALL') {
        params.value.deptIds = getSpringIds(result);
      } else {
        params.value.deptIds = [];
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  const overviewRef = ref<InstanceType<typeof overview>>();

  function refresh() {
    nextTick(() => {
      overviewRef.value?.initHomeStatistic(params.value);
    });
  }

  async function changeHandler(
    value: string | number | Array<string | number> | null,
    option: Array<CrmTreeNodeData | null> | CrmTreeNodeData,
    _: { node: TreeOption | null; action: 'select' | 'unselect' | 'delete' | 'clear' }
  ) {
    activeDeptId.value = value as string;
    if (value !== 'SELF') {
      params.value.deptIds = [value as string, ...getSpringIds((option as CrmTreeNodeData).children)];
    } else {
      params.value.deptIds = [];
    }
    setSearchType(false, value as string);
    refresh();
  }

  watch(
    () => params.value.searchType,
    () => {
      refresh();
    }
  );

  onMounted(() => {
    initDepartList();
  });
</script>

<style lang="less" scoped>
  .analytics-title {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
    .analytics-name {
      font-weight: 600;
    }
  }
</style>

<style lang="less"></style>

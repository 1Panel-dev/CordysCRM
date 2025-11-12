<template>
  <div :class="`crm-follow-detail p-[24px] ${props.wrapperClass}`">
    <div class="mb-[16px] flex items-center justify-between">
      <div>
        <n-button v-if="showAdd" type="primary" @click="handleAdd">
          {{ t(props.activeType === 'followPlan' ? 'crmFollowRecord.writePlan' : 'crmFollowRecord.writeRecord') }}
        </n-button>
      </div>
      <div class="flex gap-[12px]">
        <CrmTab
          v-if="props.activeType === 'followPlan'"
          v-model:active-tab="activeStatus"
          no-content
          :tab-list="statusTabList"
          type="segment"
          @change="() => loadFollowList()"
        >
        </CrmTab>
        <CrmSearchInput
          v-model:value="followKeyword"
          :placeholder="t('common.byKeywordSearch')"
          class="mr-[1px] !w-[240px]"
          @search="(val) => searchData(val)"
        />
      </div>
    </div>
    <n-spin :show="loading" class="h-full">
      <FollowRecord
        v-model:data="data"
        :virtual-scroll-height="`${props.virtualScrollHeight || '1000px'}`"
        :get-description-fun="getDescriptionFun"
        key-field="id"
        :disabled-open-detail="props.followApiKey !== 'myPlan'"
        :type="props.activeType"
        :empty-text="emptyText"
        :get-disabled-fun="getShowAction"
        @reach-bottom="handleReachBottom"
        @change="changePlanStatus"
      >
        <template #headerAction="{ item }">
          <div v-if="getShowAction(item)" class="flex items-center gap-[12px]">
            <n-button
              v-if="
                props.activeType === 'followPlan' &&
                [CustomerFollowPlanStatusEnum.COMPLETED].includes(item.status) &&
                !item.converted
              "
              type="primary"
              class="text-btn-primary"
              quaternary
              @click="handleConvert(item)"
            >
              {{ t('common.convertPlanToRecord') }}
            </n-button>
            <n-button
              v-if="
                props.activeType === 'followRecord' ||
                (props.activeType === 'followPlan' &&
                  ![CustomerFollowPlanStatusEnum.CANCELLED, CustomerFollowPlanStatusEnum.CANCELLED].includes(
                    item.status
                  ))
              "
              type="primary"
              class="text-btn-primary"
              quaternary
              @click="handleEdit(item)"
            >
              {{ t('common.edit') }}
            </n-button>
            <n-button type="error" class="text-btn-error" quaternary @click="handleDelete(item)">
              {{ t('common.delete') }}
            </n-button>
          </div>
        </template>
        <template #createTime="{ descItem }">
          <div class="flex items-center gap-[8px]">
            {{ dayjs(descItem.value).format('YYYY-MM-DD HH:mm:ss') }}
          </div>
        </template>
        <template #updateTime="{ descItem }">
          <div class="flex items-center gap-[8px]">
            {{ dayjs(descItem.value).format('YYYY-MM-DD HH:mm:ss') }}
          </div>
        </template>
      </FollowRecord>
    </n-spin>
    <CrmFormCreateDrawer
      v-model:visible="formDrawerVisible"
      :form-key="realFormKey"
      :source-id="realFollowSourceId"
      :initial-source-name="props.initialSourceName"
      :need-init-detail="needInitDetail"
      :link-form-info="linkFormInfo"
      :link-form-key="FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER"
      :link-scenario="FormLinkScenarioEnum.PLAN_TO_RECORD"
      :other-save-params="props.activeType === 'followPlan' ? otherFollowRecordSaveParams : undefined"
      @saved="handleAfterSave"
    />
  </div>
</template>

<script lang="ts" setup>
  import { NButton, NSpin } from 'naive-ui';
  import dayjs from 'dayjs';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { FieldTypeEnum, FormDesignKeyEnum, FormLinkScenarioEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CustomerFollowPlanListItem, FollowDetailItem } from '@lib/shared/models/customer';

  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import FollowRecord from './followRecord.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  import { descriptionList, statusTabList } from './config';
  import useFollowApi, { type followEnumType } from './useFollowApi';

  const { t } = useI18n();

  export type ActiveType = 'followPlan' | 'followRecord';

  interface FollowDetailProps {
    activeType: 'followRecord' | 'followPlan'; // 跟进记录|跟进计划
    followApiKey: followEnumType; // 跟进计划apiKey
    virtualScrollHeight?: string; // 虚拟高度
    wrapperClass?: string;
    sourceId: string; // 资源id
    refreshKey?: number;
    showAction?: boolean; // 显示操作
    initialSourceName?: string; // 初始化详情时的名称
    showAdd?: boolean; // 显示增加按钮
    anyPermission?: string[]; // 无任一权限展示无权限
  }

  const props = withDefaults(defineProps<FollowDetailProps>(), {
    showAction: true,
  });

  const formDrawerVisible = ref(false);

  const {
    data,
    loading,
    handleReachBottom,
    searchData,
    activeStatus,
    loadFollowList,
    changePlanStatus,
    followKeyword,
    followFormKeyMap,
    handleDelete,
    getApiKey,
  } = useFollowApi({
    type: toRef(props, 'activeType'),
    followApiKey: props.followApiKey,
    sourceId: toRef(props, 'sourceId'),
  });

  const needInitDetail = ref(false);
  const activePlan = ref();
  const otherFollowRecordSaveParams = ref({
    converted: false,
  });

  const realFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS);
  const planFormKey = ref(FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS);
  const { fieldList, formDetail, initFormDetail, initFormConfig, linkFormFieldMap, saveForm } = useFormCreateApi({
    formKey: computed(() => planFormKey.value),
    sourceId: computed(() => activePlan.value.id),
    needInitDetail: computed(() => needInitDetail.value),
    otherSaveParams: computed(() => otherFollowRecordSaveParams.value),
  });

  function getDescriptionFun(item: FollowDetailItem) {
    const isClue = item.type === 'CLUE' && item.clueId?.length;
    const customerNameKey = isClue ? 'clueName' : 'customerName';
    let lastDescriptionList = [
      ...(props.followApiKey === 'myPlan' || props.activeType === 'followPlan'
        ? [
            {
              key: customerNameKey,
              label: isClue ? t('crmFollowRecord.companyName') : t('opportunity.customerName'),
              value: customerNameKey,
            },
          ]
        : []),
      ...descriptionList,
    ];

    if (isClue) {
      lastDescriptionList = lastDescriptionList.filter((e) => !['contactName', 'phone'].includes(e.key));
    }

    return (lastDescriptionList.map((desc: Description) => ({
      ...desc,
      value: item[desc.key as keyof FollowDetailItem],
    })) || []) as Description[];
  }

  // 编辑记录或计划
  const realFollowSourceId = ref<string | undefined>('');
  const isConverted = ref(false);
  function handleAdd() {
    realFormKey.value =
      (followFormKeyMap[props.followApiKey as keyof typeof followFormKeyMap]?.[
        props.activeType
      ] as FormDesignKeyEnum) ?? realFormKey.value;
    realFollowSourceId.value = props.sourceId;
    needInitDetail.value = false;
    formDrawerVisible.value = true;
    if (props.activeType === 'followPlan') {
      isConverted.value = false;
      otherFollowRecordSaveParams.value.converted = isConverted.value;
    }
  }

  const linkFormInfo = ref<Record<string, any> | undefined>({});
  async function handleConvert(item: FollowDetailItem) {
    activePlan.value = item;
    isConverted.value = true;
    otherFollowRecordSaveParams.value.converted = isConverted.value;
    realFollowSourceId.value = '';
    realFormKey.value =
      followFormKeyMap[getApiKey(item) as keyof typeof followFormKeyMap]?.followRecord ?? realFormKey.value;
    planFormKey.value =
      followFormKeyMap[getApiKey(item) as keyof typeof followFormKeyMap]?.followPlan ?? realFormKey.value;
    needInitDetail.value = false;
    await initFormConfig();
    await initFormDetail(false, true);
    linkFormInfo.value = linkFormFieldMap.value;
    formDrawerVisible.value = true;
  }

  function handleEdit(item: FollowDetailItem) {
    realFormKey.value = followFormKeyMap[getApiKey(item) as keyof typeof followFormKeyMap]?.[
      props.activeType
    ] as FormDesignKeyEnum;
    realFollowSourceId.value = item.id;
    needInitDetail.value = true;
    formDrawerVisible.value = true;
    if (props.activeType === 'followPlan') {
      isConverted.value = false;
      otherFollowRecordSaveParams.value.converted = (item as CustomerFollowPlanListItem).converted;
    }
  }

  const emptyText = computed(() => {
    if (!hasAnyPermission(props.anyPermission)) {
      return t('common.noPermission');
    }
    return props.activeType === 'followPlan' ? t('crmFollowRecord.noFollowPlan') : t('crmFollowRecord.noFollowRecord');
  });

  const planPermission: Partial<Record<followEnumType, string[]>> = {
    [FormDesignKeyEnum.CLUE]: ['CLUE_MANAGEMENT:UPDATE'],
    [FormDesignKeyEnum.CUSTOMER]: ['CUSTOMER_MANAGEMENT:UPDATE'],
    [FormDesignKeyEnum.BUSINESS]: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
  };

  function getShowAction(item: FollowDetailItem) {
    if (props.followApiKey === 'myPlan') {
      const permission = planPermission[getApiKey(item) as keyof typeof followFormKeyMap];
      return hasAnyPermission(permission);
    }
    return props.showAction;
  }

  // 更新计划为已转记录
  async function updatePlan() {
    planFormKey.value = followFormKeyMap[getApiKey(activePlan.value) as keyof typeof followFormKeyMap]?.[
      props.activeType
    ] as FormDesignKeyEnum;
    realFollowSourceId.value = activePlan.value.id;
    needInitDetail.value = true;
    initFormConfig();
    await initFormDetail();
    fieldList.value.forEach((item) => {
      if (
        [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT].includes(item.type) &&
        Array.isArray(formDetail.value[item.id])
      ) {
        formDetail.value[item.id] = formDetail.value[item.id]?.[0];
      }
    });
    await saveForm(formDetail.value, false, () => ({}), true);
    isConverted.value = false;
    otherFollowRecordSaveParams.value.converted = isConverted.value;
    loadFollowList();
  }

  function handleAfterSave() {
    if (isConverted.value) {
      updatePlan();
    } else {
      loadFollowList();
    }
  }

  onBeforeMount(() => {
    loadFollowList();
  });

  watch(
    () => props.refreshKey,
    (val) => {
      if (val) {
        loadFollowList();
      }
    }
  );
</script>

<style lang="less" scoped>
  .crm-follow-detail {
    @apply overflow-hidden;

    border-radius: @border-radius-medium;
    background: var(--text-n10);
  }
  :deep(.n-tabs) {
    width: auto;
  }
</style>

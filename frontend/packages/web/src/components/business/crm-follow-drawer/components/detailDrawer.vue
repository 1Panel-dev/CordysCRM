<template>
  <CrmDrawer
    v-model:show="showDrawer"
    :title="`${props.sourceName} ${
      props.formKey === FormDesignKeyEnum.FOLLOW_RECORD ? t('module.customer.followRecord') : t('common.plan')
    }`"
    :width="800"
    no-padding
    :footer="false"
  >
    <template #titleLeft>
      <StatusTagSelect
        v-if="props.formKey === FormDesignKeyEnum.FOLLOW_PLAN && props.detail?.status"
        v-model:status="planStatus"
        :disabled="!canUpdate || !!props.detail?.converted"
        class="font-normal"
        @change="handleStatusChange"
      />
      <CrmTag
        v-if="
          props.formKey === FormDesignKeyEnum.FOLLOW_PLAN &&
          [CustomerFollowPlanStatusEnum.COMPLETED].includes(props.detail?.status) &&
          props.detail?.converted
        "
        class="font-normal"
      >
        {{ t('common.hasConvertToRecord') }}
      </CrmTag>
    </template>
    <template #titleRight>
      <div class="flex items-center gap-[12px]">
        <n-button v-if="canUpdate" type="primary" ghost class="n-btn-outline-primary" @click="handleEdit">
          {{ t('common.edit') }}
        </n-button>
        <n-button
          v-if="
            canConvert &&
            props.detail?.status &&
            [CustomerFollowPlanStatusEnum.COMPLETED].includes(props.detail?.status) &&
            !props.detail?.converted
          "
          type="primary"
          ghost
          class="n-btn-outline-primary"
          @click="handleConvert"
        >
          {{ t('common.convertPlanToRecord') }}
        </n-button>
        <n-button v-if="canDelete" type="error" ghost class="n-btn-outline-error" @click="handleDelete">
          {{ t('common.delete') }}
        </n-button>
      </div>
    </template>
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="props.formKey"
            :source-id="props.sourceId"
            :refresh-key="detailRefreshKey"
            :column="3"
            label-width="auto"
            value-align="start"
            readonly
            @init="handleDescriptionInit"
          />
        </div>
        <n-divider class="!mb-[12px] !mt-[16px] bg-[var(--text-n8)]" />
        <CrmComment
          v-model:expanded="commentExpanded"
          v-model:count="commentInitialCount"
          :type="commentResourceType"
          :source-id="props.sourceId"
          :can-create="canComment"
          :can-reply="canComment"
        />
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton, NDivider } from 'naive-ui';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { FOLLOW_COMMENT_OPERATE_PERMISSIONS } from '@lib/shared/method/comment';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmComment from '@/components/business/crm-comment/index.vue';
  import StatusTagSelect from '@/components/business/crm-follow-detail/statusTagSelect.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';

  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    sourceId: string;
    detail?: any;
    formKey: FormDesignKeyEnum;
    sourceName: string;
    refreshKey: number;
    readonly?: boolean;
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'delete'): void;
    (e: 'edit'): void;
    (e: 'convert', detail?: any): void;
    (e: 'detailInit', detail?: Record<string, any>): void;
    (e: 'countChange', count: number): void;
    (e: 'statusChange'): void;
  }>();

  const { t } = useI18n();
  const canComment = computed(() => hasAnyPermission(FOLLOW_COMMENT_OPERATE_PERMISSIONS));
  const permissionPrefix = computed(() =>
    props.formKey === FormDesignKeyEnum.FOLLOW_RECORD ? 'FOLLOW_RECORD' : 'FOLLOW_PLAN'
  );
  const canUpdate = computed(() => !props.readonly && hasAnyPermission([`${permissionPrefix.value}:UPDATE`]));
  const canDelete = computed(() => !props.readonly && hasAnyPermission([`${permissionPrefix.value}:DELETE`]));
  const canConvert = computed(
    () => props.formKey === FormDesignKeyEnum.FOLLOW_PLAN && canUpdate.value && hasAnyPermission(['FOLLOW_RECORD:ADD'])
  );
  const planStatus = computed({
    get: () => props.detail?.status,
    set: (status) => {
      if (props.detail) {
        props.detail.status = status;
      }
    },
  });

  const commentExpanded = ref(false);
  const commentResourceType = computed(() =>
    props.formKey === FormDesignKeyEnum.FOLLOW_RECORD ? 'followRecord' : 'followPlan'
  );
  const commentInitialCount = ref<number>();
  const detailRefreshKey = ref(0);

  watch(
    () => [showDrawer.value, props.sourceId, props.formKey, props.refreshKey],
    ([visible]) => {
      if (visible) {
        commentExpanded.value = false;
        commentInitialCount.value = props.detail?.commentCount;
        detailRefreshKey.value += 1;
      }
    },
    {
      immediate: true,
    }
  );

  function handleDescriptionInit(_collaborationType?: unknown, _sourceName?: string, detail?: Record<string, any>) {
    if (typeof detail?.commentCount === 'number') {
      commentInitialCount.value = detail.commentCount;
    }
    emit('detailInit', detail);
  }

  watch(commentInitialCount, (count) => {
    emit('countChange', count || 0);
  });

  function handleDelete() {
    emit('delete');
  }

  function handleEdit() {
    emit('edit');
  }

  function handleConvert() {
    emit('convert', props.detail);
  }

  function handleStatusChange() {
    emit('statusChange');
  }
</script>

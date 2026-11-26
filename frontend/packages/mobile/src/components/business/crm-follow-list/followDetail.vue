<template>
  <CrmPageWrapper :title="t('common.detail')">
    <div class="bg-[var(--text-n9)] py-[16px]">
      <CrmDescription
        :description="descriptions"
        :source-id="sourceId"
        :readonly="!canEditDetail"
        @delete-file="removeAttachment"
      />
    </div>
    <div class="bg-[var(--text-n9)] px-[16px] pt-0">
      <div class="bg-[var(--text-n10)] p-[16px]">
        <div class="font-[600]">{{ t('common.communicationContent') }}</div>
        <div class="mt-[16px] rounded-[var(--border-radius-large)] bg-[var(--text-n9)] p-[16px]">
          {{ detail.content }}
        </div>
      </div>
    </div>
    <div class="crm-follow-detail-comment bg-[var(--text-n9)] p-[16px]">
      <CrmComment
        v-model:count="commentCount"
        class="crm-comment--detail"
        :type="commentType"
        :source-id="sourceId"
        :can-create="canComment"
        :can-reply="canComment"
        disable-pull-refresh
        @change-editor="commentEditing = Boolean($event)"
      />
    </div>
    <template v-if="(canEditDetail || canDeleteDetail) && !commentEditing" #footer>
      <CrmActionButtons
        :show-edit-button="canEditDetail"
        :actions="canDeleteDetail ? [{ key: 'delete', text: t('common.delete'), color: 'var(--error-red)' }] : []"
        @select="handleFooterAction"
      />
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { FOLLOW_COMMENT_OPERATE_PERMISSIONS } from '@lib/shared/method/comment';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmActionButtons from '@/components/business/crm-action-buttons/index.vue';
  import CrmComment from '@/components/business/crm-comment/index.vue';
  import type { MobileCommentResourceType } from '@/components/business/crm-comment/useCommentResource';

  import { followPlanApiMap, followRecordApiMap, PlanEnumType, RecordEnumType } from '@/config/follow';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useUserStore from '@/store/modules/user';

  import { CommonRouteEnum } from '@/enums/routeEnum';
  import { hasAnyPermission } from '@/utils/permission';

  const route = useRoute();
  const router = useRouter();
  const userStore = useUserStore();
  const { t } = useI18n();

  const formKey = computed(() => (route.query.formKey?.toString() as RecordEnumType | PlanEnumType) || '');
  const isPlan = computed(() => formKey.value.toLowerCase().includes('plan'));
  const sourceId = computed(() => route.query.id?.toString() || '');
  const commentType = computed<MobileCommentResourceType>(() => (isPlan.value ? 'plan' : 'record'));
  const canComment = computed(() => hasAnyPermission(FOLLOW_COMMENT_OPERATE_PERMISSIONS));
  const commentEditing = ref(false);

  const { descriptions, initFormConfig, initFormDescription, detail, removeAttachment } = useFormCreateApi({
    formKey: formKey.value,
    sourceId,
    needInitDetail: route.query.needInitDetail === 'Y',
  });

  const permissionPrefix = computed(() => (isPlan.value ? 'FOLLOW_PLAN' : 'FOLLOW_RECORD'));
  const canManageDetail = computed(
    () => route.query.readonly?.toString() !== 'true' && detail.value.owner === userStore.userInfo.id
  );
  const canEditDetail = computed(() => canManageDetail.value && hasAnyPermission([`${permissionPrefix.value}:UPDATE`]));
  const canDeleteDetail = computed(
    () => canManageDetail.value && hasAnyPermission([`${permissionPrefix.value}:DELETE`])
  );

  const commentCount = computed({
    get: () => detail.value.commentCount || 0,
    set: (value: number) => {
      detail.value.commentCount = value;
    },
  });

  onBeforeMount(async () => {
    await initFormConfig();
    initFormDescription();
  });

  async function handleDelete() {
    try {
      if (isPlan.value) {
        await followPlanApiMap.delete?.[formKey.value as PlanEnumType]?.(sourceId.value);
      } else {
        await followRecordApiMap.delete?.[formKey.value as RecordEnumType]?.(sourceId.value);
      }
      showSuccessToast(t('common.deleteSuccess'));
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleEdit() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: formKey.value,
        id: sourceId.value,
        needInitDetail: 'Y',
      },
      ...(detail.value.converted === undefined
        ? {}
        : {
            state: {
              params: JSON.stringify({ converted: detail.value.converted }),
            },
          }),
    });
  }

  function handleFooterAction(action: string) {
    if (action === 'edit') {
      handleEdit();
      return;
    }
    if (action === 'delete') {
      handleDelete();
    }
  }
</script>

<style scoped lang="less">
  .crm-follow-detail-comment {
    @apply box-border flex h-full flex-none basis-full flex-col overflow-hidden;
  }
  :deep(.crm-page-content) {
    @apply !overflow-auto;
  }
</style>

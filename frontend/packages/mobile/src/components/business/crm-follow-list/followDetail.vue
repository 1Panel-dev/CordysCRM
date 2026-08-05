<template>
  <CrmPageWrapper :title="t('common.detail')">
    <div class="bg-[var(--text-n9)] py-[16px]">
      <CrmDescription :description="descriptions" />
    </div>
    <div class="bg-[var(--text-n9)] px-[16px] pt-0">
      <div class="bg-[var(--text-n10)] p-[16px]">
        <div class="font-[600]">{{ t('common.communicationContent') }}</div>
        <div class="mt-[16px] rounded-[var(--border-radius-large)] bg-[var(--text-n9)] p-[16px]">
          {{ detail.content }}
        </div>
      </div>
    </div>
    <div class="bg-[var(--text-n9)] p-[16px]">
      <CrmComment
        class="mt-0"
        :type="commentType"
        :source-id="sourceId"
        :count="commentCount"
        @change-editor="commentEditing = Boolean($event)"
      />
    </div>
    <template v-if="canEditDetail && !commentEditing" #footer>
      <div class="flex items-center justify-center gap-[16px]">
        <div class="flex w-[100px] items-center">
          <CrmTextButton
            color="var(--text-n1)"
            icon="iconicon_delete"
            :text="t('common.delete')"
            icon-size="18px"
            direction="column"
            class="flex-1"
            @click="handleDelete"
          />
        </div>
        <van-button
          type="primary"
          class="flex-1 !rounded-[var(--border-radius-small)] !text-[16px]"
          plain
          @click="handleEdit"
        >
          {{ t('common.edit') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';
  import CrmComment from '@/components/business/crm-comment/index.vue';
  import type { MobileCommentResourceType } from '@/components/business/crm-comment/useCommentResource';

  import { followPlanApiMap, followRecordApiMap, PlanEnumType, RecordEnumType } from '@/config/follow';
  import useFormCreateApi from '@/hooks/useFormCreateApi';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const formKey = computed(() => (route.query.formKey?.toString() as RecordEnumType | PlanEnumType) || '');
  const isPlan = computed(() => formKey.value.toLowerCase().includes('plan'));
  const sourceId = computed(() => route.query.id?.toString() || '');
  const commentType = computed<MobileCommentResourceType>(() => (isPlan.value ? 'plan' : 'record'));
  const canEditDetail = computed(() => route.query.readonly?.toString() !== 'true');
  const commentEditing = ref(false);

  const { descriptions, initFormConfig, initFormDescription, detail } = useFormCreateApi({
    formKey: formKey.value,
    sourceId,
    needInitDetail: route.query.needInitDetail === 'Y',
  });

  const commentCount = computed(() => detail.value.commentCount || 0);

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
</script>

<style lang="less" scoped>
  :deep(.crm-page-content) {
    @apply !overflow-auto;
  }
</style>

<template>
  <CrmPageWrapper :title="t('common.detail')">
    <div class="h-full bg-[var(--text-n9)] py-[16px]">
      <CrmDescription :description="descriptions" />
      <div class="mt-[16px]">
        <van-cell-group inset class="p-[16px]">
          <div class="font-[600]">{{ t('common.communicationContent') }}</div>
          <div class="mt-[16px] rounded-[var(--border-radius-large)] bg-[var(--text-n9)] p-[16px]">
            {{ detail.content }}
          </div>
        </van-cell-group>
      </div>
    </div>
    <template v-if="route.query.readonly?.toString() !== 'true'" #footer>
      <div class="flex items-center justify-center gap-[16px]">
        <div class="flex w-[100px] items-center">
          <CrmTextButton
            v-if="isPlan && detail.status !== CustomerFollowPlanStatusEnum.CANCELLED"
            color="var(--text-n1)"
            icon="iconicon_minus_circle1"
            :text="t('common.cancelPlan')"
            icon-size="18px"
            direction="column"
            class="flex-1"
            @click="handleCancel"
          />
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
          v-if="!isPlan || (isPlan && detail.status !== CustomerFollowPlanStatusEnum.CANCELLED)"
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

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { followPlanApiMap, followRecordApiMap, PlanEnumType, RecordEnumType } from '@/config/follow';
  import useFormCreateApi from '@/hooks/useFormCreateApi';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const isPlan = computed(() => route.query.formKey?.includes('plan'));
  const formKey = computed(() => (route.query.formKey?.toString() as RecordEnumType | PlanEnumType) || '');
  const sourceId = computed(() => route.query.id?.toString() || '');

  const { descriptions, initFormConfig, initFormDescription, detail } = useFormCreateApi({
    formKey: formKey.value,
    sourceId: sourceId.value,
    needInitDetail: route.query.needInitDetail === 'Y',
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

  async function handleCancel() {
    try {
      await followPlanApiMap.cancel?.[formKey.value as PlanEnumType]?.(sourceId.value);
      showSuccessToast(t('common.cancelSuccess'));
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
    });
  }
</script>

<style lang="less" scoped>
  :deep(.crm-page-content) {
    @apply !overflow-hidden;
  }
</style>

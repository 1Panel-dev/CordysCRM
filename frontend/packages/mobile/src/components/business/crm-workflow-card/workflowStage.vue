<template>
  <CrmPageWrapper :title="route.query.lastName?.toString() || ''">
    <van-form ref="formRef" required>
      <van-cell-group inset>
        <van-field name="radio" :label="t('common.result')">
          <template #input>
            <van-radio-group v-model="form.stage" direction="horizontal">
              <van-radio :name="StageResultEnum.SUCCESS"> {{ t('common.success') }}</van-radio>
              <van-radio :name="StageResultEnum.FAIL"> {{ t('common.fail') }}</van-radio>
            </van-radio-group>
          </template>
        </van-field>
      </van-cell-group>
    </van-form>
    <template #footer>
      <div class="flex items-center gap-[16px]">
        <van-button
          type="default"
          class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
          block
          :disabled="loading"
          @click="router.back"
        >
          {{ t('common.cancel') }}
        </van-button>
        <van-button
          type="primary"
          class="!rounded-[var(--border-radius-small)] !text-[16px]"
          :loading="loading"
          block
          @click="handleSave"
        >
          {{ t('common.confirm') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { FormInstance } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';

  import { updateClueStatus, updateOptStage } from '@/api/modules';

  import type { WorkStageTypeKey } from './index.vue';

  const { t } = useI18n();

  const route = useRoute();
  const router = useRouter();

  const updateStageApi: Record<WorkStageTypeKey, (params: { id: string; stage: string }) => Promise<any>> = {
    [FormDesignKeyEnum.BUSINESS]: updateOptStage,
    [FormDesignKeyEnum.CLUE]: updateClueStatus,
  };

  const form = ref({
    stage: StageResultEnum.SUCCESS,
  });

  const formRef = ref<FormInstance>();
  const loading = ref(false);

  async function handleSave() {
    try {
      await formRef.value?.validate();
      loading.value = true;
      const stageType = route.query.type as WorkStageTypeKey;
      await updateStageApi[stageType]({
        id: route.query.id as string,
        stage: form.value.stage,
      });
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }
</script>

<style scoped></style>

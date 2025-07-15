<template>
  <CrmPageWrapper :title="route.query.lastName?.toString() || ''">
    <van-form ref="formRef" required>
      <van-cell-group inset>
        <van-field name="radio" :label="t('common.result')">
          <template #input>
            <van-radio-group v-model="form.stage" direction="horizontal">
              <van-radio :name="StageResultEnum.SUCCESS" :disabled="isHasBackPermission">
                {{ t('common.success') }}
              </van-radio>
              <van-radio :name="StageResultEnum.FAIL"> {{ t('common.fail') }}</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <!-- <van-field
          v-model="form.expectedEndTime"
          is-link
          name="datePicker"
          :label="t('opportunity.endTime')"
          :placeholder="t('opportunity.selectActualEndTime')"
          :rules="[{ required: true, message: t('common.notNull', { value: `${t('opportunity.endTime')}` }) }]"
          @click="selectExpectedEndTime"
        />
        <van-popup v-model:show="showEndTimePicker" destroy-on-close position="bottom">
          <van-date-picker v-model="currentDate" @confirm="onSelectDateConfirm" @cancel="showEndTimePicker = false" />
        </van-popup> -->
        <van-field
          v-if="form.stage === StageResultEnum.FAIL"
          v-model="form.failureReason"
          is-link
          name="picker"
          :label="t('opportunity.failureReason')"
          :placeholder="t('opportunity.selectFailureReason')"
          :rules="[{ required: true, message: t('common.notNull', { value: `${t('opportunity.failureReason')}` }) }]"
          @click="showReasonPicker = true"
        />
        <van-popup v-model:show="showReasonPicker" destroy-on-close position="bottom">
          <van-picker
            v-model="currentReason"
            :columns="failureReasonOptions"
            @cancel="() => (showReasonPicker = false)"
            @confirm="onSelectReasonConfirm"
          />
        </van-popup>
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
  import { FormInstance, showSuccessToast } from 'vant';
  import dayjs from 'dayjs';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { UpdateStageParams } from '@lib/shared/models/opportunity';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';

  import { updateClueStatus, updateOptStage } from '@/api/modules';
  import { failureReasonOptions } from '@/config/opportunity';

  import type { WorkStageTypeKey } from './index.vue';

  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();

  const updateStageApi: Record<WorkStageTypeKey, (params: UpdateStageParams) => Promise<any>> = {
    [FormDesignKeyEnum.BUSINESS]: updateOptStage,
    [FormDesignKeyEnum.CLUE]: updateClueStatus,
  };

  const isHasBackPermission = computed(() => route.query.isHasBack?.toString() === 'Y');
  const form = ref<{
    stage: string;
    failureReason?: string;
    // expectedEndTime?: string;
  }>({
    stage: isHasBackPermission.value ? StageResultEnum.FAIL : StageResultEnum.SUCCESS,
    failureReason: '',
    // expectedEndTime: '',
  });

  // const showEndTimePicker = ref(false);
  // const currentDate = ref<string[]>([]);
  // function selectExpectedEndTime() {
  //   const date = dayjs();
  //   currentDate.value = date.format('YYYY-MM-DD').split('-');
  //   showEndTimePicker.value = true;
  // }

  // const onSelectDateConfirm = ({ selectedValues }: { selectedValues: string[] }) => {
  //   form.value.expectedEndTime = selectedValues.join('-');
  //   showEndTimePicker.value = false;
  // };

  const showReasonPicker = ref(false);
  const currentReason = ref<string[]>([]);
  function onSelectReasonConfirm({
    selectedValues,
    selectedOptions,
  }: {
    selectedValues: string[];
    selectedOptions: { value: string; text: string }[];
  }) {
    form.value.failureReason = selectedOptions[0]?.text;
    currentReason.value = selectedValues;
    showReasonPicker.value = false;
  }

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
        // TODO 先不要了
        // expectedEndTime: form.value.expectedEndTime
        //   ? (dayjs(form.value.expectedEndTime).valueOf() as number)
        //   : undefined,
        failureReason: form.value.stage === StageResultEnum.FAIL ? currentReason.value[0] : undefined,
      });
      showSuccessToast(t('common.operationSuccess'));
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

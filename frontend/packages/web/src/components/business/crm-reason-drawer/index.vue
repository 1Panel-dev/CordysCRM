<template>
  <CrmDrawer
    v-model:show="showDrawer"
    :width="600"
    :footer="false"
    :auto-focus="false"
    no-padding
    :show-back="false"
    closable
  >
    <template #title>
      <div class="flex items-center gap-[8px]">
        <div class="one-line-text">{{ titleValue }}</div>
        <n-tooltip trigger="hover" :delay="300" :disabled="hasConfig">
          <template #trigger>
            <n-switch
              :value="enableReason"
              :disabled="!hasConfig"
              size="small"
              :rubber-band="false"
              @click="changeHandler"
            />
          </template>
          {{ t('module.configReasonTooltip') }}
        </n-tooltip>
      </div>
    </template>
    <n-scrollbar content-class="p-[24px]">
      <div class="bg-[var(--text-n9)] p-[16px]">
        <div class="text-[var(--text-n1)]">{{ props.title }}</div>
        <CrmBatchForm
          ref="batchFormRef"
          class="p-0"
          :models="formItemModel"
          :default-list="form.list"
          :add-text="t('crmReasonDrawer.addReason')"
          validate-when-add
          draggable
          :disabled-add="form.list.length >= 50"
          :pop-confirm-props="getConfirmPropsFun"
          @delete-row="handleDelete"
          @save-row="handleSave"
        />
      </div>
    </n-scrollbar>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NScrollbar, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';

  import { addReason, deleteReasonItem, getReasonList, updateReason, updateReasonEnable } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    title: string;
    reasonKey: ReasonTypeEnum.OPPORTUNITY_FAIL_RS | ReasonTypeEnum.CUSTOMER_POOL_RS | ReasonTypeEnum.CLUE_POOL_RS;
  }>();

  const emit = defineEmits<{
    (e: 'loadConfig'): void;
  }>();

  const showDrawer = defineModel<boolean>('visible', {
    required: true,
  });

  const enableReason = defineModel<boolean>('enable', {
    required: true,
  });

  const hasConfig = defineModel<boolean>('config', {
    required: true,
  });

  const titleValue = computed(() => `${props.title}${t('crmReasonDrawer.setting')}`);

  const form = ref<any>({ list: [] });

  const formItemModel = ref([
    {
      path: 'name',
      type: FieldTypeEnum.INPUT,
      formItemClass: 'w-full flex-initial',
      inputProps: {
        maxlength: 255,
      },
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: props.title }),
        },
        { notRepeat: true, message: t('module.capacitySet.repeatMsg') },
      ],
    },
  ]);

  const popConfirmLoading = ref(false);
  function getConfirmPropsFun(_: Record<string, any>) {
    return {
      title: t('crmReasonDrawer.deleteReasonTitleTip', { title: props.title }),
      content: t('crmReasonDrawer.deleteReasonContentTip'),
      positiveText: t('common.remove'),
      loading: popConfirmLoading.value,
    };
  }

  const loading = ref(false);
  async function initReason() {
    try {
      loading.value = true;
      const res = await getReasonList(props.reasonKey);
      form.value.list = res ?? [];
      hasConfig.value = res.length > 0;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function changeHandler() {
    if (form.value.list.length > 0) {
      try {
        await updateReasonEnable({
          module: props.reasonKey,
          enable: !enableReason.value,
        });
        Message.success(enableReason.value ? t('common.closeSuccess') : t('common.enableSuccess'));
        initReason();
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    }
  }

  async function handleDelete(_i: number, id: string, done: () => void) {
    if (enableReason.value && form.value.list.length === 1) return;
    try {
      popConfirmLoading.value = true;
      await deleteReasonItem(id);
      done();
      initReason();
      emit('loadConfig');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      popConfirmLoading.value = false;
    }
  }

  async function handleSave(element: Record<string, any>, done: () => void) {
    try {
      const params = {
        id: element?.id,
        name: element.name,
        module: props.reasonKey,
      };
      if (element.id) {
        await updateReason(params);
        done();
      } else {
        await addReason(params);
        done();
        await initReason();
      }
      emit('loadConfig');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => showDrawer.value,
    (val) => {
      if (val) {
        initReason();
      }
    }
  );
</script>

<style scoped></style>

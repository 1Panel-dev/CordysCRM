<template>
  <div class="flex items-center gap-[8px]" @click.stop>
    {{ props.title }}
    <n-switch
      :value="value"
      :disabled="props.disabled"
      size="small"
      :rubber-band="false"
      @update:value="(val:boolean)=>handleChange(val)"
    />
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NSwitch, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { getReasonConfig, updateReasonEnable } from '@/api/modules';

  export type approvalConfigType =
    | FormDesignKeyEnum.OPPORTUNITY_QUOTATION
    | FormDesignKeyEnum.CONTRACT
    | FormDesignKeyEnum.INVOICE;

  const props = defineProps<{
    type: approvalConfigType;
    title: string;
    disabled?: boolean;
    toolTipContent?: string;
  }>();

  const emit = defineEmits<{
    (e: 'change'): void;
  }>();

  const value = ref(true);

  const apiParamsKey: Record<approvalConfigType, string> = {
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: ReasonTypeEnum.QUOTATION_APPROVAL,
    [FormDesignKeyEnum.CONTRACT]: ReasonTypeEnum.CONTRACT_APPROVAL,
    [FormDesignKeyEnum.INVOICE]: ReasonTypeEnum.INVOICE_APPROVAL,
  };

  async function initStatus() {
    try {
      const result = await getReasonConfig(apiParamsKey[props.type] as ReasonTypeEnum);
      value.value = result.enable;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleChange(val: boolean) {
    try {
      await updateReasonEnable({
        module: apiParamsKey[props.type] as ReasonTypeEnum,
        enable: val,
      });
      value.value = val;
      initStatus();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    initStatus();
  });
</script>

<style scoped></style>

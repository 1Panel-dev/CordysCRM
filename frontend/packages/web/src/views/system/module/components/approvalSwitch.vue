<template>
  <div class="flex items-center gap-[8px]" @click.stop>
    {{ props.title }}
    <n-switch
      :value="props.value"
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
    apiParamsKey: Record<approvalConfigType, string>;
    value: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'change', type: approvalConfigType): void;
  }>();

  async function handleChange(val: boolean) {
    try {
      await updateReasonEnable({
        module: props.apiParamsKey[props.type] as ReasonTypeEnum,
        enable: val,
      });
      emit('change', props.type);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
</script>

<style scoped></style>

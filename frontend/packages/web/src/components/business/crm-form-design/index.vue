<template>
  <n-scrollbar x-scrollable content-class="flex h-full !w-full bg-[var(--text-n9)]" content-style="min-width: 800px">
    <div class="crm-form-design--left"><fieldComponents :field-list="list" @select="handleFieldSelect" /></div>
    <div class="crm-form-design--center">
      <div class="crm-form-design--center-content">
        <formComposition ref="formCompositionRef" v-model:list="list" v-model:field="field" :form-config="formConfig" />
      </div>
    </div>
    <div class="crm-form-design--right">
      <formAttrConfig
        v-model:field="field"
        v-model:field-list="list"
        :form-config="formConfig"
        :form-key="props.formKey"
      />
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { FormConfig } from '@lib/shared/models/system/module';

  import fieldComponents from './components/fieldComponents.vue';
  import formAttrConfig from './components/formAttrConfig/index.vue';
  import formComposition from './components/formComposition.vue';

  import { rules } from '../crm-form-create/config';
  import { FormCreateField, FormCreateFieldRule } from '../crm-form-create/types';

  const props = defineProps<{
    formKey: FormDesignKeyEnum;
  }>();

  const list = defineModel<FormCreateField[]>('fieldList', {
    required: true,
  });
  const formConfig = defineModel<FormConfig>('formConfig', {
    required: true,
  });

  const field = ref<FormCreateField>();
  const formCompositionRef = ref<InstanceType<typeof formComposition>>();

  function handleFieldSelect(item: FormCreateField) {
    formCompositionRef.value?.addItem(item);
  }

  watch(
    () => list.value,
    () => {
      list.value.forEach((item) => {
        const fullRules: FormCreateFieldRule[] = [];
        (item.rules || []).forEach((rule) => {
          // 遍历规则集合，将全量的规则配置载入
          const staticRule = cloneDeep(rules.find((e) => e.key === rule.key));
          if (staticRule) {
            staticRule.regex = rule.regex; // 正则表达式(目前没有)是配置到后台存储的，需要读取
            fullRules.push(staticRule);
          }
        });
        item.rules = fullRules;
      });
    },
    { immediate: true }
  );

  function setActiveField(item: FormCreateField) {
    field.value = item;
  }

  defineExpose({
    setActiveField,
  });
</script>

<style lang="less" scoped>
  .crm-form-design--left,
  .crm-form-design--right {
    @apply h-full overflow-hidden;

    width: 280px;
    background-color: var(--text-n10);
  }
  .crm-form-design--left {
    padding: 16px;
  }
  .crm-form-design--center {
    @apply h-full flex-1 bg-transparent;

    padding: 16px;
    .crm-form-design--center-content {
      @apply h-full;

      border-radius: var(--border-radius-small);
      background-color: var(--text-n10);
    }
  }
</style>

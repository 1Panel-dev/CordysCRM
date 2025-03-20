<template>
  <CrmCard hide-footer>
    <div class="flex flex-col gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
      <div class="flex w-[calc(100%-40px)] items-center gap-[8px] text-[var(--text-n2)]">
        <div class="flex-1">{{ t('customer.relation') }}</div>
        <div class="flex-1">{{ t('customer.selectCustomer') }}</div>
      </div>
      <div v-for="(relation, index) in relations" :key="relation.customerId" class="flex items-center gap-[8px]">
        <n-select v-model:value="relation.relationType" :options="getRelationOptions(relation)" />
        <n-select v-model:value="relation.customerId" :options="customerOptions" />
        <n-button class="bg-[var(--text-n10)] p-[8px]" @click="deleteRelation(index)">
          <template #icon>
            <CrmIcon type="iconicon_minus_circle1" class="text-[var(--text-n4)]" />
          </template>
        </n-button>
      </div>
      <div class="flex justify-center">
        <n-button type="primary" text :disabled="relations.length >= 11" @click="addRelation">
          <template #icon>
            <CrmIcon type="iconicon_add" />
          </template>
          {{ t('common.add') }}
        </n-button>
      </div>
    </div>
    <div class="mt-[16px] flex items-center justify-end gap-[16px]">
      <n-button :disabled="loading" @click="reset">{{ t('common.reset') }}</n-button>
      <n-button type="primary" :loading="loading" @click="handleSave">{{ t('common.save') }}</n-button>
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NSelect, useMessage } from 'naive-ui';

  import { RelationListItem } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { saveCustomerRelation } from '@/api/modules/customer';
  import { useI18n } from '@/hooks/useI18n';

  import { SelectMixedOption } from 'naive-ui/es/select/src/interface';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const originRelations = ref<RelationListItem[]>([
    { relationType: 'GROUP', customerId: '', id: '', customerName: '' },
  ]);
  const relations = ref<RelationListItem[]>([{ relationType: 'GROUP', customerId: '', id: '', customerName: '' }]);
  const relationOptions = [
    {
      label: t('customer.group'),
      value: 'group',
    },
    {
      label: t('customer.subsidiary'),
      value: 'subsidiary',
    },
  ];
  function getRelationOptions(relation: any) {
    if (relations.value.every((item) => item.relationType !== 'GROUP') || relation.relationType === 'GROUP') {
      return relationOptions;
    }
    return relationOptions.filter((item) => item.value !== 'group');
  }
  const customerOptions = ref<SelectMixedOption[]>([]);

  function addRelation() {
    relations.value.push({
      id: relations.value.length,
      relationType: 'SUBSIDIARY',
      customerId: '',
      customerName: '',
    });
  }

  function deleteRelation(index: number) {
    relations.value.splice(index, 1);
  }

  function reset() {
    relations.value = originRelations.value.map((item) => ({ ...item }));
  }

  const loading = ref(false);
  async function handleSave() {
    try {
      loading.value = true;
      await saveCustomerRelation(props.sourceId, relations.value);
      Message.success(t('common.saveSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }
</script>

<style lang="less" scoped></style>

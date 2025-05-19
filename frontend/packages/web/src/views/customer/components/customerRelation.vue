<template>
  <CrmCard :loading="loading" hide-footer>
    <div class="flex flex-col gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
      <div class="flex w-[calc(100%-40px)] items-center gap-[8px] text-[var(--text-n2)]">
        <div class="flex-1">{{ t('customer.relation') }}</div>
        <div class="flex-1">{{ t('customer.selectCustomer') }}</div>
      </div>
      <div v-for="(relation, index) in relations" :key="relation.customerId" class="flex items-center gap-[8px]">
        <n-select
          v-model:value="relation.relationType"
          :options="getRelationOptions(relation)"
          :disabled="props.readonly"
          :placeholder="t('common.pleaseSelect')"
        />
        <CrmDataSource
          v-model:value="relation.customerId"
          v-model:rows="relation.customerName"
          :data-source-type="FieldDataSourceTypeEnum.CUSTOMER_OPTIONS"
          :multiple="false"
          :disabled-selection="disabledSelection"
          :disabled="props.readonly"
        />
        <n-button
          v-if="!props.readonly"
          v-permission="['CUSTOMER_MANAGEMENT:UPDATE']"
          class="bg-[var(--text-n10)] p-[8px]"
          @click="deleteRelation(index)"
        >
          <template #icon>
            <CrmIcon type="iconicon_minus_circle1" class="text-[var(--text-n4)]" />
          </template>
        </n-button>
      </div>
      <div class="flex">
        <n-button
          v-if="!props.readonly"
          v-permission="['CUSTOMER_MANAGEMENT:UPDATE']"
          type="primary"
          text
          :disabled="relations.length >= 11"
          @click="addRelation"
        >
          <template #icon>
            <CrmIcon type="iconicon_add" />
          </template>
          {{ t('common.add') }}
        </n-button>
      </div>
    </div>
    <div v-if="!props.readonly" class="mt-[16px] flex items-center justify-end gap-[16px]">
      <n-button v-permission="['CUSTOMER_MANAGEMENT:UPDATE']" :disabled="loading" @click="reset">
        {{ t('common.reset') }}
      </n-button>
      <n-button v-permission="['CUSTOMER_MANAGEMENT:UPDATE']" type="primary" :loading="loading" @click="handleSave">
        {{ t('common.save') }}
      </n-button>
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NSelect, useMessage } from 'naive-ui';

  import { FieldDataSourceTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { RelationListItem } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';

  import { getCustomerRelationList, saveCustomerRelation } from '@/api/modules';

  import { RowData } from 'naive-ui/es/data-table/src/interface';

  const props = defineProps<{
    sourceId: string;
    readonly?: boolean;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const originRelations = ref<Record<string, any>[]>([
    { relationType: undefined, customerId: [], id: '', customerName: [] },
  ]);
  const relations = ref<Record<string, any>[]>([{ relationType: undefined, customerId: [], id: '', customerName: [] }]);
  const relationOptions = [
    {
      label: t('customer.group'),
      value: 'GROUP',
    },
    {
      label: t('customer.subsidiary'),
      value: 'SUBSIDIARY',
    },
  ];
  function getRelationOptions(relation: any) {
    if (relations.value.every((item) => item.relationType !== 'GROUP') || relation.relationType === 'GROUP') {
      return relationOptions;
    }
    return relationOptions.filter((item) => item.value !== 'GROUP');
  }

  function addRelation() {
    relations.value.push({
      id: relations.value.length,
      relationType: 'SUBSIDIARY',
      customerId: [],
      customerName: [],
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
      await saveCustomerRelation(
        props.sourceId,
        relations.value.map((item) => ({
          ...item,
          customerId: item.customerId[0],
          customerName: item.customerName[0]?.name,
        })) as RelationListItem[]
      );
      Message.success(t('common.saveSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  async function initList() {
    try {
      loading.value = true;
      const res = await getCustomerRelationList(props.sourceId);
      if (res.length === 0) {
        relations.value = [
          {
            relationType: undefined,
            customerId: [],
            id: '',
            customerName: [],
          },
        ];
      } else {
        relations.value = res.map((item) => ({
          ...item,
          customerId: item.customerId ? [item.customerId] : [],
          customerName: item.customerName
            ? [
                {
                  id: item.customerId,
                  name: item.customerName,
                },
              ]
            : [],
        }));
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function disabledSelection(row: RowData) {
    return row.id === props.sourceId || relations.value.some((e) => e.customerId.includes(row.id));
  }

  onBeforeMount(() => {
    initList();
  });
</script>

<style lang="less" scoped></style>

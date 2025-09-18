<template>
  <van-field
    v-model="fieldValue"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    is-link
    readonly
    :placeholder="props.fieldConfig.placeholder || t('common.pleaseSelect')"
    :disabled="props.fieldConfig.editable === false"
    clearable
    type="textarea"
    rows="1"
    autosize
    @click="handleClick"
    @update:model-value="($event) => emit('change', $event)"
  >
  </van-field>
  <van-popup
    v-model:show="showPicker"
    destroy-on-close
    round
    position="bottom"
    safe-area-inset-top
    safe-area-inset-bottom
    class="h-[100vh]"
  >
    <CrmPageWrapper
      :title="
        t('datasource.pickResource', {
          name: props.fieldConfig.type ? t(typeLocaleMap[props.fieldConfig.type]) : '',
        })
      "
      hide-back
    >
      <div class="flex h-full flex-col overflow-hidden">
        <van-search
          v-model="keyword"
          class="crm-datasource-search"
          shape="round"
          :placeholder="
            t('datasource.searchPlaceholder', {
              name: props.fieldConfig.type ? t(typeLocaleMap[props.fieldConfig.type]) : '',
            })
          "
          @search="search"
        />
        <div class="flex-1 overflow-hidden px-[16px]">
          <CrmSelectList
            ref="crmSelectListRef"
            v-model:value="pickerValue"
            v-model:selected-rows="pickerSelectedRows"
            v-model:loading="loading"
            :data="dataList"
            :multiple="isMultiple"
            :keyword="keyword"
            :transform="selectListTransform"
            no-page-nation
          >
            <template #label="{ item }">
              <div class="flex flex-col gap-[4px]">
                <div class="text-[16px]">{{ item.name }}</div>
                <div v-if="item.pathName" class="text-[14px] text-[var(--text-n2)]">{{ item.pathName }}</div>
              </div>
            </template>
          </CrmSelectList>
        </div>
      </div>
      <template #footer>
        <div class="flex items-center gap-[16px]">
          <van-button
            type="default"
            class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
            block
            @click="onCancel"
          >
            {{ t('common.cancel') }}
          </van-button>
          <van-button
            type="primary"
            class="!rounded-[var(--border-radius-small)] !text-[16px]"
            block
            :disabled="!pickerSelectedRows.length"
            @click="onConfirm"
          >
            {{ t('common.confirm') }}
          </van-button>
        </div>
      </template>
    </CrmPageWrapper>
  </van-popup>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { mapTree } from '@lib/shared/method';
  import { DeptUserTreeNode } from '@lib/shared/models/system/role';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmSelectList from '@/components/business/crm-select-list/index.vue';

  import { getFieldDeptTree, getFieldDeptUerTree } from '@/api/modules';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    needInitDetail?: boolean; // 是否需要初始化详情
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string | string[]): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string | string[]>('value', {
    default: '',
  });

  const fieldValue = ref('');
  const pickerValue = ref<string[]>([]);
  const pickerSelectedRows = ref<Record<string, any>[]>(props.fieldConfig.initialOptions || []);
  const crmSelectListRef = ref<InstanceType<typeof CrmSelectList>>();
  const showPicker = ref(false);
  const keyword = ref('');

  const typeLocaleMap: Record<string, any> = {
    [FieldTypeEnum.MEMBER]: t('formCreate.member'),
    [FieldTypeEnum.MEMBER_MULTIPLE]: t('formCreate.member'),
    [FieldTypeEnum.DEPARTMENT]: t('formCreate.department'),
    [FieldTypeEnum.DEPARTMENT_MULTIPLE]: t('formCreate.department'),
  };
  const sourceApi: Partial<Record<FieldTypeEnum, (params?: any) => Promise<DeptUserTreeNode[]>>> = {
    [FieldTypeEnum.MEMBER]: getFieldDeptUerTree,
    [FieldTypeEnum.MEMBER_MULTIPLE]: getFieldDeptUerTree,
    [FieldTypeEnum.DEPARTMENT]: getFieldDeptTree,
    [FieldTypeEnum.DEPARTMENT_MULTIPLE]: getFieldDeptTree,
  };
  const isMultiple = computed(() =>
    [FieldTypeEnum.MEMBER_MULTIPLE, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(props.fieldConfig.type)
  );
  const dataList = ref<Record<string, any>[]>([]);
  const loading = ref(false);

  async function loadData() {
    try {
      if (props.fieldConfig.type && sourceApi[props.fieldConfig.type]) {
        loading.value = true;
        const res = await sourceApi[props.fieldConfig.type]!();
        const tempArr: Record<string, any>[] = [];
        mapTree(res, (node) => {
          node.pathName = node.parent?.pathName
            ? `${node.parent.pathName}${node.children?.length ? `/${node.name}` : ''}`
            : node.name;
          if (
            ([FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(props.fieldConfig.type) &&
              node.nodeType === DeptNodeTypeEnum.USER) ||
            [FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(props.fieldConfig.type)
          ) {
            tempArr.push({
              name: node.name,
              id: node.id,
              pathName: node.parentId === 'NONE' ? '' : node.pathName,
            });
          }
          return node;
        });
        dataList.value = tempArr;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function onConfirm() {
    showPicker.value = false;
    fieldValue.value = pickerSelectedRows.value.map((item) => item.name).join('；');
    value.value = isMultiple.value ? pickerSelectedRows.value.map((item) => item.id) : pickerSelectedRows.value[0].id;
    emit('change', value.value);
  }

  function onCancel() {
    pickerValue.value = Array.isArray(value.value) ? value.value : [value.value];
    showPicker.value = false;
  }

  function selectListTransform(item: Record<string, any>) {
    return {
      ...item,
      checked: pickerSelectedRows.value.some((row) => row.id === item.id),
    };
  }

  function search() {
    crmSelectListRef.value?.filterListByKeyword('name');
  }

  function handleClick() {
    if (props.fieldConfig.editable) {
      keyword.value = '';
      showPicker.value = true;
      nextTick(() => {
        loadData();
      });
    }
  }

  watch(
    () => value.value,
    (val) => {
      pickerValue.value = Array.isArray(val) ? val : [val];
    },
    {
      immediate: true,
    }
  );

  watch(
    () => props.fieldConfig.defaultValue,
    (newVal) => {
      if (!props.needInitDetail) {
        value.value = newVal;
        fieldValue.value = (props.fieldConfig.initialOptions || [])
          .filter((e) => newVal.includes(e.id))
          .map((item) => item.name)
          .join('；');
      }
    },
    {
      immediate: true,
    }
  );

  watch(
    () => props.fieldConfig.initialOptions,
    (newVal) => {
      pickerSelectedRows.value = newVal || [];
      fieldValue.value = (newVal || [])
        .filter((e) => (props.needInitDetail ? value.value : props.fieldConfig.defaultValue).includes(e.id))
        .map((item) => item.name)
        .join('；');
    },
    { immediate: true }
  );
</script>

<style lang="less" scoped>
  .crm-datasource-search {
    :deep(.van-cell) {
      &:last-child::before {
        @apply !hidden;
      }
    }
  }
</style>

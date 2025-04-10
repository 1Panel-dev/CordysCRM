<template>
  <CrmPageWrapper :title="formCreateTitle">
    <van-form ref="formRef" required>
      <van-cell-group class="crm-form-create-inset" inset>
        <template v-for="item in fieldList" :key="item.id">
          <component
            :is="getItemComponent(item.type, item.multiple)"
            v-if="item.show !== false"
            v-model:value="formDetail[item.id]"
            :field-config="item"
            @change="($event: any) => handleFieldChange($event, item)"
          />
        </template>
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
          {{ route.query.id ? t('common.update') : t('common.create') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { FormInstance } from 'vant';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';

  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useUserStore from '@/store/modules/user';

  import { rules } from '@cordys/web/src/components/business/crm-form-create/config';
  import { FormCreateField, FormCreateFieldRule } from '@cordys/web/src/components/business/crm-form-create/types';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();
  const userStore = useUserStore();

  const formRef = ref<FormInstance>();

  const lastPageParams = window.history.state.params ? JSON.parse(window.history.state.params) : null; // 获取上个页面带过来的表格查询参数

  const { fieldList, formDetail, loading, formCreateTitle, initFormConfig, initFormDetail, saveForm } =
    useFormCreateApi({
      formKey: route.query.formKey as FormDesignKeyEnum,
      sourceId: route.query.id as string,
      needInitDetail: route.query.needInitDetail === 'Y',
      initialSourceName: route.query.initialSourceName as string,
      otherSaveParams: lastPageParams,
    });

  function getItemComponent(type: FieldTypeEnum, multiple?: boolean) {
    if (type === FieldTypeEnum.INPUT) {
      return CrmFormCreateComponents.basicComponents.singleText;
    }
    if (type === FieldTypeEnum.TEXTAREA) {
      return CrmFormCreateComponents.basicComponents.textarea;
    }
    if (type === FieldTypeEnum.DATE_TIME) {
      return CrmFormCreateComponents.basicComponents.datePicker;
    }
    if (type === FieldTypeEnum.SELECT) {
      return multiple
        ? CrmFormCreateComponents.basicComponents.multiplePick
        : CrmFormCreateComponents.basicComponents.pick;
    }
    if (type === FieldTypeEnum.PHONE) {
      return CrmFormCreateComponents.advancedComponents.phone;
    }
    if (type === FieldTypeEnum.DATA_SOURCE) {
      return CrmFormCreateComponents.advancedComponents.dataSource;
    }
  }

  function handleFieldChange(value: any, item: FormCreateField) {
    // 控制显示规则
    if (item.showControlRules?.length) {
      item.showControlRules.forEach((rule) => {
        fieldList.value.forEach((e) => {
          // 若配置了该值的显示规则，且该字段在显示规则中，则显示
          if (rule.value === value && rule.fieldIds.includes(e.id)) {
            e.show = true;
          } else if (rule.fieldIds.includes(e.id)) {
            // 若该字段在显示规则中，但值不符合，则隐藏该字段
            e.show = false;
          }
        });
      });
    }
  }

  async function handleSave() {
    try {
      await formRef.value?.validate();
      const result = cloneDeep(formDetail.value);
      fieldList.value.forEach((item) => {
        if (item.type === FieldTypeEnum.DATA_SOURCE) {
          // 处理数据源字段，单选传单个值，多选传数组
          result[item.id] = item.multiple ? result[item.id] : result[item.id]?.[0];
        }
      });
      saveForm(result, () => router.back());
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(async () => {
    await initFormConfig();
    if (route.query.id && route.query.needInitDetail === 'Y') {
      initFormDetail();
    }
  });

  function getRuleType(item: FormCreateField) {
    if (
      (item.type === FieldTypeEnum.SELECT && item.multiple) ||
      item.type === FieldTypeEnum.CHECKBOX ||
      item.type === FieldTypeEnum.MULTIPLE_INPUT ||
      (item.type === FieldTypeEnum.MEMBER && item.multiple) ||
      (item.type === FieldTypeEnum.DEPARTMENT && item.multiple) ||
      item.type === FieldTypeEnum.DATA_SOURCE ||
      item.type === FieldTypeEnum.PICTURE
    ) {
      return 'array';
    }
    if (item.type === FieldTypeEnum.DATE_TIME) {
      return 'date';
    }
    if (item.type === FieldTypeEnum.INPUT_NUMBER) {
      return 'number';
    }
    return 'string';
  }

  watch(
    () => fieldList.value,
    () => {
      fieldList.value.forEach((item) => {
        if (!formDetail.value[item.id]) {
          let defaultValue = item.defaultValue || '';
          if ([FieldTypeEnum.DATE_TIME, FieldTypeEnum.INPUT_NUMBER].includes(item.type)) {
            defaultValue = Number(defaultValue) || null;
          }
          if (getRuleType(item) === 'array') {
            defaultValue = defaultValue || [];
          }
          formDetail.value[item.id] = defaultValue;
        }
        handleFieldChange(formDetail.value[item.id], item); // 初始化时，根据字段值控制显示
        const fullRules: FormCreateFieldRule[] = [];
        (item.rules || []).forEach((rule) => {
          // 遍历规则集合，将全量的规则配置载入
          const staticRule = cloneDeep(rules.find((e) => e.key === rule.key));
          if (staticRule) {
            staticRule.regex = rule.regex; // 正则表达式(目前没有)是配置到后台存储的，需要读取
            staticRule.message = t(staticRule.message as string, { value: t(item.name) });
            staticRule.type = getRuleType(item);
            if (item.type === FieldTypeEnum.DATA_SOURCE) {
              staticRule.trigger = 'none';
            }
            fullRules.push(staticRule);
          }
        });
        item.rules = fullRules;
        if (item.type === FieldTypeEnum.MEMBER && item.hasCurrentUser) {
          item.defaultValue = userStore.userInfo.id;
          item.initialOptions = [
            {
              id: userStore.userInfo.id,
              name: userStore.userInfo.name,
            },
          ];
        }
        if (item.type === FieldTypeEnum.DEPARTMENT && item.hasCurrentUserDept) {
          item.defaultValue = userStore.userInfo.departmentId;
          item.initialOptions = [
            {
              id: userStore.userInfo.departmentId,
              name: userStore.userInfo.departmentName,
            },
          ];
        }
      });
    }
  );
</script>

<style lang="less" scoped>
  .crm-form-create-inset {
    margin: 0;
  }
</style>

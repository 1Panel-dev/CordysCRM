<template>
  <CrmPageWrapper :title="formCreateTitle">
    <van-form ref="formRef" class="crm-form" required="auto">
      <van-cell-group inset>
        <template v-for="item in mobileFieldList" :key="item.id">
          <component
            :is="getItemComponent(item.type)"
            v-if="item.show !== false"
            :id="item.id"
            v-model:value="formDetail[item.id]"
            :field-config="item"
            :origin-form-detail="originFormDetail"
            :need-init-detail="route.query.needInitDetail === 'Y'"
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
          {{ route.query.needInitDetail === 'Y' ? t('common.update') : t('common.create') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { FormInstance } from 'vant';
  import { cloneDeep } from 'lodash-es';

  import { FieldRuleEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';

  import { checkRepeat } from '@/api/modules';
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

  const {
    fieldList,
    formDetail,
    originFormDetail,
    loading,
    formCreateTitle,
    initFormConfig,
    initFormDetail,
    saveForm,
    initFormShowControl,
  } = useFormCreateApi({
    formKey: route.query.formKey as FormDesignKeyEnum,
    sourceId: route.query.id as string,
    needInitDetail: route.query.needInitDetail === 'Y',
    initialSourceName: route.query.initialSourceName as string,
    otherSaveParams: lastPageParams,
  });

  const mobileFieldList = computed(() => {
    return fieldList.value.filter((item) => item.mobile !== false || item.type === FieldTypeEnum.PICTURE);
  });

  function getItemComponent(type: FieldTypeEnum) {
    if (type === FieldTypeEnum.INPUT) {
      return CrmFormCreateComponents.basicComponents.singleText;
    }
    if (type === FieldTypeEnum.TEXTAREA) {
      return CrmFormCreateComponents.basicComponents.textarea;
    }
    if (type === FieldTypeEnum.INPUT_NUMBER) {
      return CrmFormCreateComponents.basicComponents.inputNumber;
    }
    if (type === FieldTypeEnum.DATE_TIME) {
      return CrmFormCreateComponents.basicComponents.datePicker;
    }
    if (type === FieldTypeEnum.RADIO) {
      return CrmFormCreateComponents.basicComponents.radio;
    }
    if (type === FieldTypeEnum.CHECKBOX) {
      return CrmFormCreateComponents.basicComponents.checkbox;
    }
    if (type === FieldTypeEnum.SELECT) {
      return CrmFormCreateComponents.basicComponents.pick;
    }
    if (type === FieldTypeEnum.SELECT_MULTIPLE) {
      return CrmFormCreateComponents.basicComponents.multiplePick;
    }
    if (type === FieldTypeEnum.DIVIDER) {
      return CrmFormCreateComponents.basicComponents.divider;
    }
    if (type === FieldTypeEnum.LOCATION) {
      return CrmFormCreateComponents.advancedComponents.location;
    }
    if (type === FieldTypeEnum.PHONE) {
      return CrmFormCreateComponents.advancedComponents.phone;
    }
    if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(type)) {
      return CrmFormCreateComponents.advancedComponents.dataSource;
    }
    if (type === FieldTypeEnum.SERIAL_NUMBER) {
      return CrmFormCreateComponents.advancedComponents.serialNumber;
    }
    if (
      [
        FieldTypeEnum.MEMBER,
        FieldTypeEnum.MEMBER_MULTIPLE,
        FieldTypeEnum.DEPARTMENT,
        FieldTypeEnum.DEPARTMENT_MULTIPLE,
      ].includes(type)
    ) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if (type === FieldTypeEnum.PICTURE) {
      return CrmFormCreateComponents.advancedComponents.upload;
    }
  }

  function handleFieldChange(value: any, item: FormCreateField) {
    // 控制显示规则
    if (item.showControlRules?.length) {
      initFormShowControl();
    }
  }

  async function handleSave() {
    try {
      await formRef.value?.validate();
      const result = cloneDeep(formDetail.value);
      mobileFieldList.value.forEach((item) => {
        if (item.type === FieldTypeEnum.DATA_SOURCE && Array.isArray(result[item.id])) {
          // 处理数据源字段，单选传单个值
          result[item.id] = result[item.id]?.[0];
        }
        if (item.type === FieldTypeEnum.PHONE) {
          // 去空格
          result[item.id] = result[item.id].replace(/[\s\uFEFF\xA0]+/g, '');
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
      item.type === FieldTypeEnum.SELECT_MULTIPLE ||
      item.type === FieldTypeEnum.CHECKBOX ||
      item.type === FieldTypeEnum.INPUT_MULTIPLE ||
      item.type === FieldTypeEnum.MEMBER_MULTIPLE ||
      item.type === FieldTypeEnum.DEPARTMENT_MULTIPLE ||
      [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.type) ||
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

  function createValidatorRule(item: FormCreateField, rule: FormCreateFieldRule): FormCreateFieldRule | null {
    const staticRule: any = cloneDeep(rules.find((e) => e.key === rule.key));
    if (!staticRule) return null;

    if (staticRule.key === FieldRuleEnum.UNIQUE) {
      // 唯一性校验
      staticRule.trigger = 'onBlur';
      staticRule.validator = async (value: string) => {
        if (
          !value.length ||
          formDetail.value[item.id].replace(/[\s\uFEFF\xA0]+/g, '') === originFormDetail.value[item.id]
        ) {
          return true;
        }

        const info = await checkRepeat({
          id: item.id,
          value,
          formKey: route.query.formKey as FormDesignKeyEnum,
        });
        if (info.repeat) {
          return info.name.length
            ? t('formCreate.repeatTip', { name: info.name })
            : t('formCreate.repeatTipWithoutName');
        }
        return true;
      };
    } else {
      staticRule.regex = rule.regex; // 正则表达式(目前没有)是配置到后台存储的，需要读取
      staticRule.message = t(staticRule.message as string, { value: t(item.name) });
      staticRule.type = getRuleType(item);
      staticRule.trigger = 'onBlur';
      if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.type)) {
        staticRule.trigger = 'none';
      }
    }

    return staticRule;
  }

  watch(
    () => mobileFieldList.value,
    () => {
      mobileFieldList.value.forEach((item) => {
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
        const fullRules: FormCreateFieldRule[] = [];
        (item.rules || []).forEach((rule) => {
          const staticRule = createValidatorRule(item, rule);
          if (staticRule) {
            fullRules.push(staticRule);
          }
        });
        item.rules = fullRules;
        if ([FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.type) && item.hasCurrentUser) {
          item.defaultValue = userStore.userInfo.id;
          item.initialOptions = [
            ...(item.initialOptions || []),
            {
              id: userStore.userInfo.id,
              name: userStore.userInfo.name,
            },
          ].filter((option, index, self) => self.findIndex((o) => o.id === option.id) === index);
        } else if (
          [FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(item.type) &&
          item.hasCurrentUserDept
        ) {
          item.defaultValue = userStore.userInfo.departmentId;
          item.initialOptions = [
            ...(item.initialOptions || []),
            {
              id: userStore.userInfo.departmentId,
              name: userStore.userInfo.departmentName,
            },
          ].filter((option, index, self) => self.findIndex((o) => o.id === option.id) === index);
        }
      });
      nextTick(() => {
        initFormShowControl();
      });
    }
  );
</script>

<style lang="less" scoped></style>

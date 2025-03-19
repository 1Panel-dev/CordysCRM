import { useMessage } from 'naive-ui';

import { FieldTypeEnum, type FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { ModuleField } from '@lib/shared/models/customer';
import type { FormConfig } from '@lib/shared/models/system/module';

import type { Description } from '@/components/pure/crm-description/index.vue';
import {
  createFormApi,
  getFormConfigApiMap,
  getFormDetailApiMap,
  updateFormApi,
} from '@/components/business/crm-form-create/config';
import type { FormCreateField } from '@/components/business/crm-form-create/types';

import { getCityPath, safeFractionConvert } from '@/utils';

import { useI18n } from './useI18n';

export interface FormCreateApiProps {
  sourceId?: Ref<string | undefined>;
  formKey: Ref<FormDesignKeyEnum>;
  otherSaveParams?: Ref<Record<string, any> | undefined>;
}

export default function useFormCreateApi(props: FormCreateApiProps) {
  const { t } = useI18n();
  const Message = useMessage();

  const descriptions = ref<Description[]>([]); // 表单详情描述列表
  const fieldList = ref<FormCreateField[]>([]); // 表单字段列表
  const loading = ref(false);
  const unsaved = ref(false);
  const formConfig = ref<FormConfig>({
    layout: 1,
    labelPos: 'top',
    inputWidth: 'custom',
    optBtnContent: [
      {
        text: t('common.save'),
        enable: true,
      },
      {
        text: t('common.saveAndContinue'),
        enable: false,
      },
      {
        text: t('common.cancel'),
        enable: true,
      },
    ],
    optBtnPos: 'flex-row',
  }); // 表单属性配置
  const formDetail = ref<Record<string, any>>({});

  async function initFormDescription() {
    try {
      const asyncApi = getFormDetailApiMap[props.formKey.value];
      if (!asyncApi || !props.sourceId?.value) return;
      const form = await asyncApi(props.sourceId?.value);
      fieldList.value.forEach((item) => {
        if (item.businessKey) {
          // 业务标准字段读取最外层
          descriptions.value.push({
            label: item.name,
            value: form[item.businessKey],
          });
        } else {
          // 其他的字段读取moduleFields TODO: 等接口字段
          const field = form.moduleFields?.find((moduleField: ModuleField) => moduleField.fieldId === item.id);
          if (item.type === FieldTypeEnum.DIVIDER) {
            descriptions.value.push({
              label: item.name,
              value: field?.fieldValue || [],
              slotName: 'divider',
              fieldInfo: item,
            });
          } else {
            let value = field?.fieldValue || '';
            if (item.type === FieldTypeEnum.LOCATION) {
              const address = (field?.fieldValue as string)?.split('-');
              value = address ? `${getCityPath(address[0])}-${address[1]}` : '-';
            }
            descriptions.value.push({
              label: item.name,
              value,
            });
          }
        }
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function initFormDetail() {
    try {
      const asyncApi = getFormDetailApiMap[props.formKey.value];
      if (!asyncApi || !props.sourceId?.value) return;
      const res = await asyncApi(props.sourceId?.value);
      fieldList.value.forEach((item) => {
        // TODO: options字段
        if (item.businessKey) {
          // 业务标准字段读取最外层
          formDetail.value[item.id] = res[item.businessKey];
        } else {
          formDetail.value[item.id] = res.moduleFields?.find(
            (moduleField) => moduleField.fieldId === item.id
          )?.fieldValue;
        }
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function initFormConfig() {
    try {
      loading.value = true;
      const res = await getFormConfigApiMap[props.formKey.value]();
      fieldList.value = res.fields.map((item) => ({
        ...item,
        fieldWidth: safeFractionConvert(item.fieldWidth),
      }));
      formConfig.value = res.formProp;
      nextTick(() => {
        unsaved.value = false;
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function saveForm(form: Record<string, any>, isContinue: boolean, callback?: (_isContinue: boolean) => void) {
    try {
      loading.value = true;
      const params: Record<string, any> = {
        ...props.otherSaveParams?.value,
        moduleFields: [],
      };
      fieldList.value.forEach((item) => {
        if (item.businessKey) {
          // 存在业务字段，则按照业务字段的key存储
          params[item.businessKey] = form[item.id];
        } else {
          params.moduleFields.push({
            fieldId: item.id,
            fieldValue: form[item.id],
          });
        }
      });
      if (props.sourceId?.value) {
        await updateFormApi[props.formKey.value](params);
        Message.success(t('common.updateSuccess'));
      } else {
        await createFormApi[props.formKey.value](params);
        Message.success(t('common.createSuccess'));
      }
      if (callback) {
        callback(isContinue);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  const formCreateTitle = computed(() => {
    const prefix = props.sourceId?.value ? t('common.edit') : t('common.add');
    return `${prefix}${t(`crmFormCreate.drawer.${props.formKey.value}`)}`;
  });

  return {
    descriptions,
    fieldList,
    loading,
    unsaved,
    formConfig,
    formDetail,
    formCreateTitle,
    initFormDescription,
    initFormConfig,
    initFormDetail,
    saveForm,
  };
}

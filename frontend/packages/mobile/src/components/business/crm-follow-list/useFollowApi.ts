import { useRouter } from 'vue-router';

import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import type { FollowDetailItem } from '@lib/shared/models/customer';

import { getCustomerFollowPlanFormConfig, getCustomerFollowRecordFormConfig } from '@/api/modules';
import { PlanEnumType, RecordEnumType } from '@/config/follow';

import { CommonRouteEnum } from '@/enums/routeEnum';

import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

export default function useFollowApi(followProps: {
  type: 'followRecord' | 'followPlan';
  formKey: PlanEnumType | RecordEnumType;
  sourceId: string;
  initialSourceName?: string;
  readonly?: boolean;
}) {
  const router = useRouter();

  const fieldList = ref<FormCreateField[]>([]);

  function transformField(e: FollowDetailItem) {
    const tmpObject: Record<string, any> = {};
    (e.moduleFields || []).forEach((moduleField) => {
      const fieldVal = fieldList.value.find((field) => moduleField.fieldId === field.id);
      if (fieldVal) {
        const isSelectableField = [FieldTypeEnum.SELECT, FieldTypeEnum.CHECKBOX, FieldTypeEnum.RADIO].includes(
          fieldVal.type
        );
        if (isSelectableField && fieldVal.options?.length) {
          const option = fieldVal.options.find((item) => item.value === moduleField.fieldValue);
          tmpObject[fieldVal.internalKey as string] = option ? option.label : '-';
        } else {
          tmpObject[fieldVal.internalKey as string] = moduleField.fieldValue;
        }
      }
    });
    return {
      ...e,
      ...tmpObject,
    };
  }

  async function initFollowFormConfig() {
    try {
      let res;
      if (followProps.type === 'followRecord') {
        res = await getCustomerFollowRecordFormConfig();
      } else {
        res = await getCustomerFollowPlanFormConfig();
      }
      fieldList.value = res.fields;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function goCreate() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: followProps.formKey,
        id: followProps.sourceId,
        initialSourceName: followProps.initialSourceName,
      },
    });
  }

  function handleEdit(item: FollowDetailItem) {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: followProps.formKey,
        id: item.id,
        needInitDetail: 'Y',
      },
    });
  }

  function goDetail(item: FollowDetailItem) {
    router.push({
      name: CommonRouteEnum.FOLLOW_DETAIL,
      query: {
        formKey: followProps.formKey,
        id: item.id,
        needInitDetail: 'Y',
        readonly: String(followProps.readonly),
      },
    });
  }

  return {
    transformField,
    initFollowFormConfig,
    goCreate,
    handleEdit,
    goDetail,
  };
}

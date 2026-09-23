import { type Component, defineAsyncComponent } from 'vue';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { FormDetailTabConfig } from '@lib/shared/models/system/module';

type SystemDetailTabTableConfig = {
  component: Component;
  permission: string[];
  props?: Record<string, unknown>;
};

export const systemDetailTabTableConfigMap: Partial<Record<FormDesignKeyEnum, SystemDetailTabTableConfig>> = {
  [FormDesignKeyEnum.CUSTOMER]: {
    component: defineAsyncComponent(() => import('@/views/customer/components/customerTable.vue')),
    permission: ['CUSTOMER_MANAGEMENT:READ'],
    props: { formKey: FormDesignKeyEnum.CUSTOMER },
  },
  [FormDesignKeyEnum.CONTACT]: {
    component: defineAsyncComponent(() => import('@/components/business/crm-form-create-table/contactTable.vue')),
    permission: ['CUSTOMER_MANAGEMENT_CONTACT:READ'],
    props: { formKey: FormDesignKeyEnum.CONTACT },
  },
  [FormDesignKeyEnum.BUSINESS]: {
    component: defineAsyncComponent(() => import('@/views/opportunity/components/opportunityTable.vue')),
    permission: ['OPPORTUNITY_MANAGEMENT:READ'],
    props: { formKey: FormDesignKeyEnum.BUSINESS },
  },
  [FormDesignKeyEnum.CLUE]: {
    component: defineAsyncComponent(() => import('@/views/clueManagement/clue/components/clueTable.vue')),
    permission: ['CLUE_MANAGEMENT:READ'],
    props: { tableFormKey: FormDesignKeyEnum.CLUE },
  },
  [FormDesignKeyEnum.FOLLOW_RECORD]: {
    component: defineAsyncComponent(() => import('@/components/business/crm-follow-drawer/components/recordTable.vue')),
    permission: [],
  },
  [FormDesignKeyEnum.FOLLOW_PLAN]: {
    component: defineAsyncComponent(() => import('@/components/business/crm-follow-drawer/components/planTable.vue')),
    permission: [],
  },
  // 详情标签候选接口中，客户跟进模块使用 record、plan 作为关联表单 ID。
  [FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER]: {
    component: defineAsyncComponent(() => import('@/components/business/crm-follow-drawer/components/recordTable.vue')),
    permission: [],
  },
  [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER]: {
    component: defineAsyncComponent(() => import('@/components/business/crm-follow-drawer/components/planTable.vue')),
    permission: [],
  },
  [FormDesignKeyEnum.CONTRACT]: {
    component: defineAsyncComponent(() => import('@/views/contract/contract/components/contractTable.vue')),
    permission: ['CONTRACT:READ'],
  },
  [FormDesignKeyEnum.CONTRACT_PAYMENT]: {
    component: defineAsyncComponent(() => import('@/views/contract/contractPaymentPlan/components/paymentTable.vue')),
    permission: ['CONTRACT_PAYMENT_PLAN:READ'],
    props: { formKey: FormDesignKeyEnum.CONTRACT_PAYMENT },
  },
  [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: {
    component: defineAsyncComponent(() => import('@/views/contract/contractPaymentRecord/components/paymentTable.vue')),
    permission: ['CONTRACT_PAYMENT_RECORD:READ'],
    props: { formKey: FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD },
  },
  [FormDesignKeyEnum.INVOICE]: {
    component: defineAsyncComponent(() => import('@/views/contract/invoice/components/invoiceTable.vue')),
    permission: ['CONTRACT_INVOICE:READ'],
  },
  [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: {
    component: defineAsyncComponent(() => import('@/views/opportunity/components/quotation/quotationTable.vue')),
    permission: ['OPPORTUNITY_QUOTATION:READ'],
    props: { formKey: FormDesignKeyEnum.OPPORTUNITY_QUOTATION },
  },
  [FormDesignKeyEnum.ORDER]: {
    component: defineAsyncComponent(() => import('@/views/order/order/components/orderTable.vue')),
    permission: ['ORDER:READ'],
    props: { formKey: FormDesignKeyEnum.ORDER },
  },
  [FormDesignKeyEnum.PRODUCT]: {
    component: defineAsyncComponent(() => import('@/views/product/components/productTable.vue')),
    permission: ['PRODUCT_MANAGEMENT:READ'],
  },
  [FormDesignKeyEnum.PRICE]: {
    component: defineAsyncComponent(() => import('@/views/product/components/priceTable.vue')),
    permission: ['PRICE:READ'],
  },
};

const CustomFormTable = defineAsyncComponent(() => import('@/views/customForm/components/formTable.vue'));

export interface FormDetailTabTable {
  component: Component;
  props: Record<string, unknown>;
}

export default function useFormDetailTabTable() {
  function getDetailTabTable(
    detailTab?: FormDetailTabConfig,
    resourceId?: string,
    pageFormId?: string
  ): FormDetailTabTable | undefined {
    const relatedFormId = detailTab?.relatedForm?.id;
    const relatedFieldId = detailTab?.relatedField?.id;
    if (!relatedFormId || !relatedFieldId || !resourceId || !pageFormId) {
      return undefined;
    }

    const commonProps = {
      readonly: false,
      hideOperationColumn: false,
      detailTabResourceId: resourceId,
      detailTabPageFormId: pageFormId,
      detailTabQuery: {
        relatedFormId,
        relatedFieldId,
      },
      // 关联表单与关联字段的组合在当前表单内唯一，用于隔离各关联列表的列配置缓存。
      tableKey: `form-detail-tab:${relatedFormId}:${relatedFieldId}`,
    };

    const config = systemDetailTabTableConfigMap[relatedFormId as FormDesignKeyEnum];
    if (config) {
      return {
        component: config.component,
        props: {
          ...commonProps,
          ...config.props,
        },
      };
    }

    return {
      component: CustomFormTable,
      props: {
        ...commonProps,
        formKey: relatedFormId,
        formKeyName: detailTab.relatedForm!.name,
      },
    };
  }

  return {
    getDetailTabTable,
  };
}

import { type Component, defineAsyncComponent } from 'vue';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { FormDetailTabConfig } from '@lib/shared/models/system/module';

import type { DetailTabFilter } from './useDetailTabTableFilter';

const systemTableComponentMap: Partial<Record<FormDesignKeyEnum, Component>> = {
  [FormDesignKeyEnum.CUSTOMER]: defineAsyncComponent(() => import('@/views/customer/components/customerTable.vue')),
  [FormDesignKeyEnum.CONTACT]: defineAsyncComponent(
    () => import('@/components/business/crm-form-create-table/contactTable.vue')
  ),
  [FormDesignKeyEnum.BUSINESS]: defineAsyncComponent(
    () => import('@/views/opportunity/components/opportunityTable.vue')
  ),
  [FormDesignKeyEnum.CLUE]: defineAsyncComponent(() => import('@/views/clueManagement/clue/components/clueTable.vue')),
  [FormDesignKeyEnum.CONTRACT]: defineAsyncComponent(
    () => import('@/views/contract/contract/components/contractTable.vue')
  ),
  [FormDesignKeyEnum.CONTRACT_PAYMENT]: defineAsyncComponent(
    () => import('@/views/contract/contractPaymentPlan/components/paymentTable.vue')
  ),
  [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: defineAsyncComponent(
    () => import('@/views/contract/contractPaymentRecord/components/paymentTable.vue')
  ),
  [FormDesignKeyEnum.INVOICE]: defineAsyncComponent(
    () => import('@/views/contract/invoice/components/invoiceTable.vue')
  ),
  [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: defineAsyncComponent(
    () => import('@/views/opportunity/components/quotation/quotationTable.vue')
  ),
  [FormDesignKeyEnum.ORDER]: defineAsyncComponent(() => import('@/views/order/order/components/orderTable.vue')),
  [FormDesignKeyEnum.PRODUCT]: defineAsyncComponent(() => import('@/views/product/components/productTable.vue')),
  [FormDesignKeyEnum.PRICE]: defineAsyncComponent(() => import('@/views/product/components/priceTable.vue')),
};

const systemTablePropsMap: Partial<Record<FormDesignKeyEnum, Record<string, unknown>>> = {
  [FormDesignKeyEnum.CUSTOMER]: { formKey: FormDesignKeyEnum.CUSTOMER },
  [FormDesignKeyEnum.CONTACT]: { formKey: FormDesignKeyEnum.CONTACT },
  [FormDesignKeyEnum.BUSINESS]: { formKey: FormDesignKeyEnum.BUSINESS },
  [FormDesignKeyEnum.CLUE]: { tableFormKey: FormDesignKeyEnum.CLUE },
  [FormDesignKeyEnum.CONTRACT_PAYMENT]: { formKey: FormDesignKeyEnum.CONTRACT_PAYMENT },
  [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: { formKey: FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD },
  [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: { formKey: FormDesignKeyEnum.OPPORTUNITY_QUOTATION },
  [FormDesignKeyEnum.ORDER]: { formKey: FormDesignKeyEnum.ORDER },
};

const CustomFormTable = defineAsyncComponent(() => import('@/views/customForm/components/formTable.vue'));

export interface FormDetailTabTable {
  component: Component;
  filter: DetailTabFilter;
  props: Record<string, unknown>;
}

export default function useFormDetailTabTable() {
  function getDetailTabTable(detailTab?: FormDetailTabConfig, sourceId?: string): FormDetailTabTable | undefined {
    if (!detailTab?.relatedFormId || !detailTab.relatedFieldId || !sourceId) {
      return undefined;
    }

    const filter = {
      fieldId: detailTab.relatedFieldId,
      sourceId,
    };
    const commonProps = {
      readonly: true,
      hideOperationColumn: true,
      hiddenAdvanceFilter: true,
      detailTabFilter: filter,
      tableKey: `form-detail-tab:${detailTab.id}`,
    };

    if (detailTab.relatedFormType === 'CUSTOM') {
      return {
        component: CustomFormTable,
        filter,
        props: {
          ...commonProps,
          formKey: detailTab.relatedFormId,
          formKeyName: detailTab.name,
        },
      };
    }

    const formKey = detailTab.relatedFormId as FormDesignKeyEnum;
    const component = systemTableComponentMap[formKey];
    if (!component) {
      return undefined;
    }

    return {
      component,
      filter,
      props: {
        ...commonProps,
        ...systemTablePropsMap[formKey],
      },
    };
  }

  return {
    getDetailTabTable,
  };
}

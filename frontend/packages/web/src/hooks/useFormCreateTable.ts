import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
import type { ModuleField } from '@lib/shared/models/customer';

import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
import useTable from '@/components/pure/crm-table/useTable';
import { getFormConfigApiMap, getFormListApiMap } from '@/components/business/crm-form-create/config';

export interface FormCreateTableProps {
  formKey: FormDesignKeyEnum;
  operationColumn?: CrmDataTableColumn;
  specialRender?: Record<string, (row: any) => void>;
}

export default async function useFormCreateTable(props: FormCreateTableProps) {
  const loading = ref(false);
  let columns: CrmDataTableColumn[] = [];
  const tableKeyMap = {
    [FormDesignKeyEnum.CUSTOMER]: TableKeyEnum.CUSTOMER,
    [FormDesignKeyEnum.CONTACT]: TableKeyEnum.CUSTOMER_CONTRACT,
    [FormDesignKeyEnum.BUSINESS]: TableKeyEnum.BUSINESS,
    [FormDesignKeyEnum.FOLLOW_PLAN]: TableKeyEnum.CUSTOMER_FOLLOW_PLAN,
    [FormDesignKeyEnum.FOLLOW_RECORD]: TableKeyEnum.CUSTOMER_FOLLOW_RECORD,
    [FormDesignKeyEnum.LEAD]: TableKeyEnum.LEAD,
    [FormDesignKeyEnum.PRODUCT]: TableKeyEnum.PRODUCT,
  };

  async function initFormConfig() {
    try {
      loading.value = true;
      const res = await getFormConfigApiMap[props.formKey]();
      columns = res.fields
        .filter((e) => e.type !== FieldTypeEnum.DIVIDER)
        .map((field) => {
          if (
            [
              FieldTypeEnum.RADIO,
              FieldTypeEnum.CHECKBOX,
              FieldTypeEnum.SELECT,
              FieldTypeEnum.MEMBER,
              FieldTypeEnum.DEPARTMENT,
            ].includes(field.type)
          ) {
            // 带筛选的列
            return {
              title: field.name,
              width: 150,
              key: field.businessKey || field.id,
              ellipsis: {
                tooltip: true,
              },
              filterOptions: field.options,
              filter: true,
            };
          }
          if (field.businessKey === 'name') {
            return {
              title: field.name,
              width: 150,
              key: field.businessKey,
              ellipsis: {
                tooltip: true,
              },
              sortOrder: false,
              sorter: true,
              render: props.specialRender?.[field.businessKey],
            };
          }
          return {
            title: field.name,
            width: 120,
            key: field.businessKey || field.id,
            ellipsis: {
              tooltip: true,
            },
            sortOrder: false,
            sorter: true,
          };
        });
      if (props.operationColumn) {
        columns.push(props.operationColumn);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  await initFormConfig();

  const useTableRes = useTable(
    getFormListApiMap[props.formKey],
    {
      tableKey: tableKeyMap[props.formKey],
      showSetting: true,
      columns,
      scrollX: 1000,
    },
    (item) => {
      const customFieldAttr: Record<string, any> = {};
      item.moduleFields.forEach((field: ModuleField) => {
        customFieldAttr[field.fieldId] = field.fieldValue; // TODO:多值字段处理
      });
      return {
        ...item,
        ...customFieldAttr,
      };
    }
  );

  return {
    loading,
    useTableRes,
  };
}

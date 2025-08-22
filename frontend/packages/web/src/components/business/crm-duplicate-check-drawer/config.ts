import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { useAppStore } from '@/store';
import { hasAnyPermission } from '@/utils/permission';

const { t } = useI18n();
const appStore = useAppStore();

export interface ScopedOptions {
  label: string;
  value: FormDesignKeyEnum;
  moduleKey: ModuleConfigEnum;
  permission: string[];
}

const scopedOptions = [
  {
    label: t('crmFormDesign.clue'),
    value: FormDesignKeyEnum.SEARCH_ADVANCED_CLUE,
    moduleKey: ModuleConfigEnum.CLUE_MANAGEMENT,
    permission: ['CLUE_MANAGEMENT:READ'],
  },
  {
    label: t('module.cluePool'),
    value: FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL,
    moduleKey: ModuleConfigEnum.CLUE_MANAGEMENT,
    permission: ['CLUE_MANAGEMENT_POOL:READ'],
  },
  {
    label: t('crmFormDesign.customer'),
    value: FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER,
    moduleKey: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
    permission: ['CUSTOMER_MANAGEMENT:READ'],
  },
  {
    label: t('module.openSea'),
    value: FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC,
    moduleKey: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
    permission: ['CUSTOMER_MANAGEMENT_POOL:READ'],
  },
  {
    label: t('module.businessManagement'),
    value: FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY,
    moduleKey: ModuleConfigEnum.BUSINESS_MANAGEMENT,
    permission: ['OPPORTUNITY_MANAGEMENT:READ'],
  },

  {
    label: t('crmFormDesign.contract'),
    value: FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT,
    moduleKey: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
    permission: ['CUSTOMER_MANAGEMENT_CONTACT:READ'],
  },
];

export const lastScopedOptions = computed<ScopedOptions[]>(() =>
  scopedOptions.filter((e) =>
    appStore.moduleConfigList.find((m) => m.moduleKey === e.moduleKey && m.enable && hasAnyPermission(e.permission))
  )
);

export default {};

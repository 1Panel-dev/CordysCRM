import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { useAppStore } from '@/store';

const { t } = useI18n();
const appStore = useAppStore();

const scopedOptions = [
  {
    label: t('crmFormDesign.customer'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER,
    moduleKey: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
  },
  {
    label: t('crmFormDesign.contract'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT,
    moduleKey: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
  },
  {
    label: t('module.openSea'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_PUBLIC,
    moduleKey: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
  },
  {
    label: t('crmFormDesign.clue'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_CLUE,
    moduleKey: ModuleConfigEnum.CLUE_MANAGEMENT,
  },
  {
    label: t('module.cluePool'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_CLUE_POOL,
    moduleKey: ModuleConfigEnum.CLUE_MANAGEMENT,
  },
  {
    label: t('module.businessManagement'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY,
    moduleKey: ModuleConfigEnum.BUSINESS_MANAGEMENT,
  },
];

export const lastScopedOptions = computed(() =>
  scopedOptions.filter((e) => appStore.moduleConfigList.find((m) => m.moduleKey === e.moduleKey && m.enable))
);

export default {};

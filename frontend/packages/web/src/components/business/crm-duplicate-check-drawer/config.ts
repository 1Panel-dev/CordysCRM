import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

const { t } = useI18n();

export const scopedOptions = ref([
  {
    label: t('crmFormDesign.customer'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER,
  },
  {
    label: t('crmFormDesign.contract'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT,
  },
  {
    label: t('module.openSea'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_PUBLIC,
  },
  {
    label: t('crmFormDesign.clue'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_CLUE,
  },
  {
    label: t('module.cluePool'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_CLUE_POOL,
  },
  {
    label: t('module.businessManagement'),
    value: FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY,
  },
]);

export default {};

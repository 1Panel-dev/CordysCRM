import { useI18n } from '@lib/shared/hooks/useI18n';

const { t } = useI18n();

export const timeOptions = [
  { label: t('workbench.today'), value: 'TODAY' },
  { label: t('common.yesterday'), value: 'YESTERDAY' },
  { label: t('common.tomorrow'), value: 'TOMORROW' },
  { label: t('workbench.thisWeek'), value: 'WEEK' },
  { label: t('common.lastWeek'), value: 'LAST_WEEK' },
  { label: t('common.nextWeek'), value: 'NEXT_WEEK' },
  { label: t('workbench.thisMonth'), value: 'MONTH' },
  { label: t('common.lastMonth'), value: 'LAST_MONTH' },
  { label: t('common.nextMonth'), value: 'NEXT_MONTH' },
  { label: t('common.lastSevenDays'), value: 'LAST_SEVEN' },
  { label: t('common.nextSevenDays'), value: 'SEVEN' },
  { label: t('common.lastThirtyDays'), value: 'LAST_THIRTY' },
  { label: t('common.nextThirtyDays'), value: 'THIRTY' },
  { label: t('common.lastSixtyDays'), value: 'LAST_SIXTY' },
  { label: t('common.nextSixtyDays'), value: 'SIXTY' },
  { label: t('common.thisQuarter'), value: 'QUARTER' },
  { label: t('common.lastQuarter'), value: 'LAST_QUARTER' },
  { label: t('common.nextQuarter'), value: 'NEXT_QUARTER' },
  { label: t('common.thisYear'), value: 'YEAR' },
  { label: t('common.lastYear'), value: 'LAST_YEAR' },
  { label: t('common.nextYear'), value: 'NEXT_YEAR' },
  { label: t('common.custom'), value: 'CUSTOM' },
];

export const unitOptions = [
  { label: t('common.day'), value: 'BEFORE_DAY' },
  { label: t('common.afterDay'), value: 'AFTER_DAY' },
  { label: t('common.week'), value: 'BEFORE_WEEK' },
  { label: t('common.afterWeek'), value: 'AFTER_WEEK' },
  { label: t('common.month'), value: 'BEFORE_MONTH' },
  { label: t('common.afterMonth'), value: 'AFTER_MONTH' },
];

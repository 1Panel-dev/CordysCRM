import type { SelectOption } from 'naive-ui';

export const DEFAULT_MODEL_PROVIDER = 'OpenAI';

export function getModelProviderOptions(t: (key: string) => string): SelectOption[] {
  return [
    { label: 'OpenAI', value: DEFAULT_MODEL_PROVIDER },
    { label: 'DeepSeek', value: 'DeepSeek' },
    { label: t('system.business.modelSettings.providerAliyun'), value: '阿里云' },
    { label: 'Anthropic', value: 'Anthropic' },
    { label: t('system.business.modelSettings.providerTencent'), value: '腾讯云' },
    { label: t('system.business.modelSettings.providerCustom'), value: '自定义' },
  ];
}

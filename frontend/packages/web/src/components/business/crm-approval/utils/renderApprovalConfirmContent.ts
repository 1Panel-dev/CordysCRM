import { h } from 'vue';
import { NAlert } from 'naive-ui';

import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

export default function renderApprovalConfirmContent(content: string, approvalTip?: string) {
  if (!approvalTip) {
    return content;
  }

  return () =>
    h('div', [
      h(
        NAlert,
        { type: 'warning' },
        {
          icon: () => h(CrmIcon, { type: 'iconicon_info_circle_filled', size: 20 }),
          default: () => approvalTip,
        }
      ),
      h('div', { class: 'mt-[8px]' }, content),
    ]);
}

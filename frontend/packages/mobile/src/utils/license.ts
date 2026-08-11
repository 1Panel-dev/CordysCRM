import { showDialog } from 'vant';

export default function showNoLicenseDialog(t: (key: string) => string): void {
  showDialog({
    title: t('common.tip'),
    message: `${t(
      'common.businessFeatureTip'
    )}<br /><a class="text-[var(--primary-8)]" href="https://cordys.cn/pricing.html" target="_blank">https://cordys.cn/pricing.html</a>`,
    allowHtml: true,
    confirmButtonText: t('common.confirm'),
  });
}

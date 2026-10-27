<template>
  <n-modal
    v-model:show="show"
    preset="card"
    :title="props.file?.name || t('common.preview')"
    :mask-closable="false"
    :style="{ padding: '0', height: '100vh', transform: 'none', boxSizing: 'border-box', overflow: 'hidden' }"
    class="crm-document-preview-modal crm-full-modal"
    @after-leave="clearViewer"
  >
    <div class="relative h-full w-full overflow-hidden bg-[var(--text-n9)]">
      <div v-if="previewKind === 'table'" class="h-full w-full overflow-auto">
        <div v-if="sheetNames.length > 1" class="sticky top-0 z-10 flex gap-[4px] border-b bg-white px-[12px] py-[8px]">
          <button
            v-for="(sheetName, index) in sheetNames"
            :key="sheetName"
            type="button"
            class="rounded px-[12px] py-[6px] text-[13px]"
            :class="
              activeSheetIndex === index
                ? 'bg-[var(--primary-8)] text-white'
                : 'bg-[var(--text-n8)] text-[var(--text-n2)]'
            "
            @click="activeSheetIndex = index"
          >
            {{ sheetName }}
          </button>
        </div>
        <table class="min-w-full border-collapse bg-white text-[13px]">
          <tbody>
            <tr v-for="(row, rowIndex) in tableRows" :key="rowIndex">
              <td
                v-for="(cell, cellIndex) in row"
                :key="cellIndex"
                class="max-w-[360px] whitespace-pre-wrap break-words border border-[var(--text-n7)] px-[8px] py-[6px] align-top"
                :class="rowIndex === 0 ? 'bg-[var(--text-n8)] font-medium' : ''"
              >
                {{ cell }}
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="truncated" class="px-[12px] py-[8px] text-[12px] text-[var(--text-n3)]">
          {{ t('crm.fileListModal.previewTruncated') }}
        </p>
      </div>
      <div
        v-else-if="previewKind === 'docx'"
        ref="docxElement"
        class="h-full w-full overflow-auto px-[16px] py-[20px]"
      ></div>
      <div
        v-else-if="previewKind === 'pptx'"
        ref="pptxElement"
        class="h-full w-full overflow-y-auto overflow-x-hidden"
      ></div>
      <div v-else ref="viewerElement" class="absolute inset-0 h-full w-full"></div>
      <div
        v-if="loading"
        class="absolute inset-0 flex flex-col items-center justify-center gap-[12px] bg-[var(--text-n9)]"
      >
        <n-spin size="large" />
        <span class="text-[14px] text-[var(--text-n3)]">{{ t('crm.fileListModal.previewLoading') }}</span>
      </div>
      <div
        v-if="errorMessage"
        class="absolute inset-0 flex items-center justify-center bg-[var(--text-n9)] px-[24px] text-center text-[14px] text-[var(--error-red)]"
      >
        {{ errorMessage }}
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
  import { computed, nextTick, ref, watch } from 'vue';
  import { NModal, NSpin } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import type { AttachmentInfo } from '@/components/business/crm-form-create/types';

  import { downloadAttachment } from '@/api/modules';

  import type { Viewer } from 'xq-doc-viewer';

  type PptxPreviewerInstance = ReturnType<typeof import('pptx-preview').init>;

  const props = defineProps<{
    file: AttachmentInfo | null;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const { t } = useI18n();
  const viewerElement = ref<HTMLDivElement | null>(null);
  const docxElement = ref<HTMLDivElement | null>(null);
  const pptxElement = ref<HTMLDivElement | null>(null);
  const loading = ref(false);
  const errorMessage = ref('');
  const tableRowsBySheet = ref<Array<{ name: string; rows: string[][] }>>([]);
  const activeSheetIndex = ref(0);
  const truncated = ref(false);
  let viewer: Viewer | null = null;
  let pptxPreviewer: PptxPreviewerInstance | null = null;
  let requestId = 0;

  const extension = computed(() => props.file?.name.split('.').pop()?.toLowerCase() || '');
  const previewKind = computed(() => {
    if (extension.value === 'docx') return 'docx';
    if (extension.value === 'pptx') return 'pptx';
    if (['xls', 'xlsx', 'csv'].includes(extension.value)) return 'table';
    return 'viewer';
  });
  const sheetNames = computed(() => tableRowsBySheet.value.map((sheet) => sheet.name));
  const tableRows = computed(() => tableRowsBySheet.value[activeSheetIndex.value]?.rows || []);

  async function getSourceFile() {
    const { file } = props;
    if (!file) throw new Error('Missing preview file');
    return file.file || new File([await downloadAttachment(file.id)], file.name);
  }

  async function loadDocument() {
    const { file } = props;
    if (!file) return;

    const currentRequestId = ++requestId;
    loading.value = true;
    errorMessage.value = '';
    try {
      await nextTick();
      const sourceFile = await getSourceFile();
      if (currentRequestId !== requestId) return;

      if (previewKind.value === 'docx') {
        if (!docxElement.value) return;
        const { renderAsync } = await import('docx-preview');
        if (currentRequestId !== requestId || !docxElement.value) return;
        await renderAsync(sourceFile, docxElement.value, undefined, {
          inWrapper: true,
          breakPages: true,
          renderHeaders: true,
          renderFooters: true,
        });
      } else if (previewKind.value === 'pptx') {
        if (!pptxElement.value) return;
        const { init } = await import('pptx-preview');
        if (currentRequestId !== requestId || !pptxElement.value) return;
        pptxPreviewer = init(pptxElement.value, {
          width: pptxElement.value.clientWidth || 960,
          height: 540,
        });
        await pptxPreviewer.preview(await sourceFile.arrayBuffer());
      } else if (previewKind.value === 'table') {
        const buffer = await sourceFile.arrayBuffer();
        if (currentRequestId !== requestId) return;
        truncated.value = false;

        if (extension.value === 'csv') {
          const Papa = (await import('papaparse')).default;
          const csvText = await sourceFile.text();
          const result = Papa.parse<string[]>(csvText, {
            skipEmptyLines: true,
            preview: 501,
          });
          const rows = result.data.slice(0, 500).map((row) => row.slice(0, 40).map((cell) => String(cell ?? '')));
          tableRowsBySheet.value = [
            {
              name: file.name,
              rows,
            },
          ];
          truncated.value =
            result.data.length > 500 ||
            result.data.some((row) => row.length > 40) ||
            result.meta.cursor < csvText.length;
        } else if (extension.value === 'xls') {
          const XLSX = await import('@e965/xlsx');
          const workbook = XLSX.read(buffer, { type: 'array' });
          if (currentRequestId !== requestId) return;
          tableRowsBySheet.value = workbook.SheetNames.map((name) => {
            const worksheet = workbook.Sheets[name];
            const usedRange = worksheet['!ref'] ? XLSX.utils.decode_range(worksheet['!ref']) : null;
            if (!usedRange) return { name, rows: [] };
            if (usedRange.e.r - usedRange.s.r >= 500 || usedRange.e.c - usedRange.s.c >= 40) truncated.value = true;
            const sourceRows = XLSX.utils.sheet_to_json<unknown[]>(worksheet, {
              header: 1,
              raw: false,
              blankrows: false,
              range: {
                s: usedRange.s,
                e: { r: Math.min(usedRange.e.r, usedRange.s.r + 499), c: Math.min(usedRange.e.c, usedRange.s.c + 39) },
              },
            });
            const rows = sourceRows.map((row) => row.map((cell) => String(cell ?? '')));
            return { name, rows };
          });
          activeSheetIndex.value = 0;
        } else {
          const ExcelJS = (await import('exceljs')).default;
          const workbook = new ExcelJS.Workbook();
          await workbook.xlsx.load(buffer);
          if (currentRequestId !== requestId) return;
          tableRowsBySheet.value = workbook.worksheets.map((worksheet) => {
            const rows: string[][] = [];
            worksheet.eachRow({ includeEmpty: false }, (row) => {
              if (rows.length < 500) {
                rows.push(
                  Array.from({ length: Math.min(row.cellCount, 40) }, (_, index) => {
                    const value = row.getCell(index + 1).text;
                    return String(value ?? '');
                  })
                );
              }
              if (rows.length >= 500 || row.cellCount > 40) truncated.value = true;
            });
            return { name: worksheet.name, rows };
          });
          activeSheetIndex.value = 0;
        }
      } else {
        if (!viewerElement.value) return;
        const { createViewer } = await import('xq-doc-viewer');
        if (currentRequestId !== requestId || !viewerElement.value) return;
        viewer = createViewer({
          target: viewerElement.value,
          theme: 'light',
          locale: 'zh-CN',
          toolbar: {
            zoom: true,
            rotate: true,
            pagination: true,
            fullscreen: true,
            print: false,
            download: false,
          },
          onError: (error: Error) => {
            errorMessage.value = error.message || t('crm.fileListModal.previewFailed');
            loading.value = false;
          },
        });
        await viewer.mount();
        if (currentRequestId !== requestId || !viewer) return;
        await viewer.setFile(sourceFile);
      }
    } catch {
      if (currentRequestId === requestId) {
        errorMessage.value = t('crm.fileListModal.previewFailed');
      }
    } finally {
      if (currentRequestId === requestId) {
        loading.value = false;
      }
    }
  }

  function clearViewer() {
    requestId += 1;
    viewer?.destroy();
    viewer = null;
    pptxPreviewer?.destroy();
    pptxPreviewer = null;
    docxElement.value?.replaceChildren();
    pptxElement.value?.replaceChildren();
    tableRowsBySheet.value = [];
    activeSheetIndex.value = 0;
    truncated.value = false;
    loading.value = false;
  }

  watch(show, (visible) => {
    if (visible) {
      loadDocument();
    } else {
      clearViewer();
    }
  });
</script>

<style lang="less">
  .crm-document-preview-modal {
    margin: 0;
    width: 100vw;
    max-width: 100vw;
    height: 100vh;
    max-height: 100vh;
    border-radius: 0;
  }
  .crm-document-preview-modal > .n-card-content {
    overflow: hidden;
    padding: 0;
    min-height: 0;
  }
  .xq-doc-viewer__branding {
    display: none;
  }
  .docx-wrapper {
    margin: 0 auto;
  }
  .docx-wrapper {
    .docx {
      overflow-x: auto;
    }
  }
  .pptx-preview-wrapper {
    height: 100% !important;
  }
</style>

<template>
  <div
    class="ai-chat-composer relative flex items-start gap-[12px] bg-[var(--text-n10)] p-[16px]"
    @dragenter="handleComposerDragEnter"
    @dragover="handleComposerDragOver"
    @dragleave="handleComposerDragLeave"
    @drop="handleComposerDrop"
  >
    <div class="flex min-w-0 flex-1 flex-col gap-[6px]" :class="{ 'mb-[28px]': props.showFooter }">
      <AiAttachmentList
        v-if="props.showAttachments && attachments.length"
        :attachments="attachments"
        removable
        retryable
        @remove="removeAttachment"
        @retry="retryAttachment"
      />

      <div
        ref="editorRef"
        class="ai-chat-composer__input min-w-0 flex-1"
        contenteditable="true"
        :data-placeholder="props.placeholder || t('aiChat.inputPlaceholder')"
        @compositionend="handleCompositionEnd"
        @compositionstart="handleCompositionStart"
        @input="syncEditorValue"
        @keydown="handleKeydown"
        @paste="handleEditorPaste"
      ></div>
    </div>

    <div
      v-if="props.showFooter"
      class="absolute bottom-[16px] left-[16px] right-[16px] flex min-h-[22px] items-center justify-between"
    >
      <div class="flex items-center">
        <input ref="fileInputRef" type="file" class="hidden" multiple @change="handleFileInputChange" />
        <input
          ref="mcpImportInputRef"
          type="file"
          accept=".json,application/json"
          class="hidden"
          @change="handleMcpImportChange"
        />
        <n-button text class="ai-chat-tool-button" @click="openFileSelector">
          <CrmIcon type="iconicon_link1" :size="16" />
        </n-button>
        <n-divider vertical class="!mx-[12px]" />
        <n-dropdown
          :key="mcpDropdownKey"
          v-model:show="mcpDropdownShow"
          trigger="click"
          placement="top-start"
          class="ai-chat-mcp-dropdown"
          :options="mcpDropdownOptions"
          :render-label="renderMcpDropdownLabel"
          :render-option="renderMcpDropdownOption"
          @select="handleMcpSelect"
        >
          <n-button class="ai-chat-mcp-button" :class="{ 'ai-chat-mcp-button--active': mcpDropdownShow }" text>
            <CrmIcon type="iconicon_mcp" :size="16" />
            <span>MCP</span>
            <CrmIcon
              :type="mcpDropdownShow ? 'iconicon_chevron_up' : 'iconicon_chevron_down'"
              :size="16"
              color="var(--text-n4)"
            />
          </n-button>
        </n-dropdown>
      </div>

      <n-button v-if="canStop" circle size="small" type="primary" @click="runtime.stop()">
        <template #icon>
          <span class="block h-[8px] w-[8px] rounded-[2px] bg-[var(--text-n10)]" />
        </template>
      </n-button>
      <n-button
        v-else
        circle
        size="small"
        type="primary"
        :loading="isLoading"
        :disabled="!canSubmit"
        @click="handleSubmit"
      >
        <template #icon>
          <CrmIcon type="iconicon_send" :size="14" color="var(--text-n10)" />
        </template>
      </n-button>
    </div>

    <div v-if="showDragMask" class="ai-chat-composer__drag-mask">
      <CrmIcon type="iconicon_cloud_upload" :size="32" color="var(--primary-8)" />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, nextTick, onMounted, ref, watch } from 'vue';
  import { type DropdownOption, NButton, NDivider, NDropdown, NTooltip, useMessage } from 'naive-ui';

  import type { AiChatAttachment, AiChatMcp, AiComposerSubmitPayload, AiFileKind } from '@lib/shared/ai-chat';
  import { getMatchedMcp, useAiChatRuntime } from '@lib/shared/ai-chat';
  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AiAttachmentList from './AiAttachmentList.vue';

  import { deleteAgentMcpConfig, importAgentMcpConfig, uploadAgentChatFile } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const props = withDefaults(
    defineProps<{
      placeholder?: string;
      mcpOptions?: AiChatMcp[];
      submitMode?: 'runtime' | 'emit';
      initialContent?: string;
      initialMcps?: AiChatMcp[];
      showAttachments?: boolean;
      showFooter?: boolean;
      syncRuntime?: boolean;
    }>(),
    {
      placeholder: '',
      mcpOptions: () => [],
      submitMode: 'runtime',
      initialContent: '',
      initialMcps: () => [],
      showAttachments: true,
      showFooter: true,
      syncRuntime: true,
    }
  );

  const emit = defineEmits<{
    (e: 'submit', payload: AiComposerSubmitPayload): void;
    (e: 'change', payload: AiComposerSubmitPayload): void;
    (e: 'mcpUpdated'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const runtime = useAiChatRuntime();

  const editorRef = ref<HTMLElement | null>(null);
  const inputValue = ref(props.initialContent || runtime.state.input.value);

  const attachments = computed(() => runtime.state.attachments.value);
  const submitAttachments = computed(() => (props.showAttachments ? attachments.value : []));
  const hasUnavailableAttachment = computed(() =>
    submitAttachments.value.some((attachment) => attachment.status === 'uploading' || attachment.status === 'error')
  );
  const isLoading = computed(() => runtime.state.loading.value);
  const canStop = computed(() => runtime.state.canStop.value);
  const canSubmit = computed(
    () =>
      !isLoading.value &&
      !hasUnavailableAttachment.value &&
      (inputValue.value.trim().length > 0 || submitAttachments.value.length > 0)
  );

  const mcpDropdownShow = ref(false);
  const mcpDropdownKey = ref(0);
  const shouldReopenMcpDropdown = ref(false);
  const isComposing = ref(false);

  function focusInput(): void {
    nextTick(() => {
      editorRef.value?.focus();
    });
  }

  // 找当前要插入的位置
  function getEditorRange(): Range {
    const editor = editorRef.value;
    const selection = window.getSelection();

    // contenteditable 没有 selectionStart/selectionEnd，只能通过 Selection/Range 找当前插入点。
    if (selection?.rangeCount && editor?.contains(selection.anchorNode)) {
      return selection.getRangeAt(0);
    }

    // 下拉选择 MCP 时焦点可能已经离开输入框，兜底插入到当前内容末尾。
    const range = document.createRange();
    if (editor) {
      range.selectNodeContents(editor);
      range.collapse(false);
    }
    return range;
  }

  // 插入后把光标放到正确位置
  function setCaretAfter(node: Node): void {
    // DOM 插入后浏览器不会自动把光标放到期望位置，需要手动重建选区
    const range = document.createRange();
    const selection = window.getSelection();

    range.setStartAfter(node);
    range.collapse(true);
    selection?.removeAllRanges();
    selection?.addRange(range);
  }

  function createMcpNode(mcp: AiChatMcp): HTMLElement {
    const node = document.createElement('span');
    const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
    const text = document.createElement('span');

    // MCP 在输入框里是一个不可编辑的整体节点，避免用户只删掉名称或图标的一部分。
    node.className = 'ai-chat-mcp-token';
    node.contentEditable = 'false';
    node.dataset.mcpId = mcp.id;
    node.dataset.mcpName = mcp.name;

    icon.setAttribute('width', '16');
    icon.setAttribute('height', '16');
    icon.setAttribute('aria-hidden', 'true');
    use.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', '#iconicon_mcp');
    icon.appendChild(use);

    text.textContent = mcp.name;
    node.append(icon, text);

    return node;
  }

  function isMcpNode(node: Node | null): node is HTMLElement {
    return node instanceof HTMLElement && node.classList.contains('ai-chat-mcp-token');
  }

  function getMcpSubmitText(node: HTMLElement): string {
    const name = node.dataset.mcpName ?? '';
    const id = node.dataset.mcpId ?? '';

    return name && id ? `[[${name}:${id}]]` : name;
  }

  function getEditorText(): string {
    const editor = editorRef.value;
    if (!editor) {
      return '';
    }

    // 提交给后端的 content 仍是纯文本；MCP token 转成后端可识别的引用格式。
    return Array.from(editor.childNodes)
      .map((node) => {
        if (isMcpNode(node)) {
          return getMcpSubmitText(node);
        }
        return node.textContent ?? '';
      })
      .join('');
  }

  function getEditorMcps(): AiChatMcp[] {
    // MCP 列表只以 token 节点为准，避免普通文本里刚好包含同名 MCP 时被误判为已选。
    const ids = new Set(
      Array.from(editorRef.value?.querySelectorAll<HTMLElement>('.ai-chat-mcp-token') ?? [])
        .map((node) => node.dataset.mcpId)
        .filter(Boolean)
    );

    return props.mcpOptions.filter((mcp) => ids.has(mcp.id));
  }

  function getSubmitPayload(): AiComposerSubmitPayload {
    return {
      content: inputValue.value,
      attachments: [...submitAttachments.value],
      options: {
        mcps: getEditorMcps(),
      },
    };
  }

  function syncEditorValue(): void {
    // contenteditable 负责展示富文本，runtime 仍只接收文本内容和选中的 MCP 列表。
    inputValue.value = getEditorText();
    if (props.syncRuntime) {
      runtime.setSelectedMcps(getEditorMcps());
    }
    emit('change', getSubmitPayload());
  }

  function isEditorEmpty(): boolean {
    const editor = editorRef.value;
    if (!editor) {
      return true;
    }

    // 只看 innerText 不够，空输入聚焦后可能只有 br；有 MCP token 时也不能判空。
    return !getEditorText().trim() && !editor.querySelector('.ai-chat-mcp-token');
  }

  function getRangeSideText(range: Range, direction: 'before' | 'after'): string {
    const editor = editorRef.value;
    if (!editor) {
      return '';
    }

    const sideRange = range.cloneRange();
    // 插入 MCP 前后各取一段文本，用来判断是否需要自动补空格，避免 token 和文字粘连。
    if (direction === 'before') {
      sideRange.selectNodeContents(editor);
      sideRange.setEnd(range.startContainer, range.startOffset);
    } else {
      sideRange.selectNodeContents(editor);
      sideRange.setStart(range.endContainer, range.endOffset);
    }

    return sideRange.toString();
  }

  function insertMcp(mcp: AiChatMcp): void {
    if (isEditorEmpty()) {
      // 空 contenteditable 聚焦后浏览器可能插入 br，直接替换子节点可避免 MCP 跑到第二行。
      const tokenNode = createMcpNode(mcp);
      const suffixNode = document.createTextNode(' ');

      editorRef.value?.replaceChildren(tokenNode, suffixNode);
      setCaretAfter(suffixNode);
      syncEditorValue();
      focusInput();
      return;
    }

    const range = getEditorRange();
    const fragment = document.createDocumentFragment();
    const tokenNode = createMcpNode(mcp);
    const beforeText = getRangeSideText(range, 'before');
    const afterText = getRangeSideText(range, 'after');
    const prefixNode = beforeText && !/\s$/.test(beforeText) ? document.createTextNode(' ') : null;
    const suffixNode = afterText && /^\s/.test(afterText) ? null : document.createTextNode(' ');

    // 使用 fragment 一次性插入 “前置空格 + MCP token + 后置空格”，减少 DOM 光标错位。
    range.deleteContents();
    if (prefixNode) {
      fragment.appendChild(prefixNode);
    }
    fragment.appendChild(tokenNode);
    if (suffixNode) {
      fragment.appendChild(suffixNode);
    }
    range.insertNode(fragment);
    setCaretAfter(suffixNode ?? tokenNode);
    syncEditorValue();
    focusInput();
  }

  function handleMcpSelect(key: string | number): void {
    const mcp = props.mcpOptions.find((item) => item.id === String(key));

    if (mcp) {
      insertMcp(mcp);
    }
  }

  const mcpImportMaxFileSize = 100 * 1024 * 1024;
  function validateMcpImportFile(file: File): boolean {
    if (!file.name.toLowerCase().endsWith('.json')) {
      Message.warning(t('aiChat.mcpImportOnlyJson'));
      return false;
    }

    if (file.size > mcpImportMaxFileSize) {
      Message.warning(t('aiChat.mcpImportOverSize'));
      return false;
    }

    return true;
  }

  async function handleMcpImportChange(event: Event) {
    const target = event.target as HTMLInputElement;
    const file = target.files?.[0];

    target.value = '';
    if (!file || !validateMcpImportFile(file)) {
      return;
    }

    try {
      await importAgentMcpConfig(file);
      Message.success(t('aiChat.mcpImportSuccess'));
      shouldReopenMcpDropdown.value = mcpDropdownShow.value;
      emit('mcpUpdated');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleMcpDelete(event: MouseEvent, option: DropdownOption): void {
    event.preventDefault();
    event.stopPropagation();

    const mcp = props.mcpOptions.find((item) => item.id === String(option.key));

    if (!mcp) {
      return;
    }

    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(mcp.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.delete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        await deleteAgentMcpConfig(mcp.id);
        Message.success(t('common.deleteSuccess'));
        emit('mcpUpdated');
      },
    });
  }

  const mcpImportInputRef = ref<HTMLInputElement | null>(null);
  function handleImportMcp(event?: MouseEvent): void {
    event?.preventDefault();
    event?.stopPropagation();

    if (mcpImportInputRef.value) {
      mcpImportInputRef.value.value = '';
      mcpImportInputRef.value.click();
    }
  }

  const mcpDropdownOptions = computed<DropdownOption[]>(() => [
    {
      key: 'mcp-import',
      type: 'render',
      render: () =>
        h('div', { class: 'px-[12px]' }, [
          h(
            NButton,
            {
              text: true,
              type: 'primary',
              onClick: handleImportMcp,
            },
            {
              icon: () => h(CrmIcon, { type: 'iconicon_add', size: 16 }),
              default: () => t('aiChat.mcpImport'),
            }
          ),
        ]),
    },
    ...props.mcpOptions.map((mcp) => ({
      label: mcp.name,
      key: mcp.id,
      description: mcp.description,
    })),
  ]);

  function renderMcpDropdownLabel(option: DropdownOption) {
    const description = option.description as string | undefined;

    return h(
      NTooltip,
      {
        delay: 300,
        disabled: !description,
        flip: true,
        placement: 'top',
        to: 'body',
        trigger: 'hover',
      },
      {
        trigger: () =>
          h('div', { class: 'min-w-0' }, [
            h('div', { class: 'truncate  leading-[22px]' }, option.label as string),
            description ? h('div', { class: 'truncate leading-[20px] text-[var(--text-n4)]' }, description) : null,
          ]),
        default: () => description,
      }
    );
  }

  function renderMcpDropdownOption({ node, option }: { node: ReturnType<typeof h>; option: DropdownOption }) {
    if (option.type === 'render') {
      return node;
    }

    return h('div', { class: 'flex w-full min-w-0 items-center gap-[8px]' }, [
      h(
        NTooltip,
        {
          delay: 300,
          flip: true,
          placement: 'top',
          to: 'body',
        },
        {
          trigger: () => h('div', { class: 'min-w-0 flex-1' }, node),
          default: () => option.label as string,
        }
      ),
      h(
        NButton,
        {
          text: true,
          class: 'shrink-0 !p-[2px] text-[var(--text-n4)]',
          onClick: (event: MouseEvent) => handleMcpDelete(event, option),
        },
        {
          default: () => h(CrmIcon, { type: 'iconicon_delete', size: 14 }),
        }
      ),
    ]);
  }

  function renderEditorValue(value: string, mcps: AiChatMcp[] = []): void {
    const editorElement = editorRef.value;
    if (!editorElement) {
      return;
    }

    const sortedMcps = [...mcps].sort((a, b) => b.name.length - a.name.length);
    editorElement.innerHTML = '';
    let currentText = '';
    let index = 0;

    function appendText(target: HTMLElement): void {
      if (!currentText) {
        return;
      }

      target.appendChild(document.createTextNode(currentText));
      currentText = '';
    }

    while (index < value.length) {
      const matchedMcp = getMatchedMcp(value, index, sortedMcps);

      if (matchedMcp) {
        appendText(editorElement);
        editorElement.appendChild(createMcpNode(matchedMcp.mcp));
        index += matchedMcp.length;
      } else {
        currentText += value[index];
        index += 1;
      }
    }

    appendText(editorElement);
  }

  watch(inputValue, (value) => {
    if (props.syncRuntime) {
      runtime.setInput(value);
    }
  });
  watch(runtime.state.input, (value) => {
    if (!props.syncRuntime) {
      return;
    }

    if (value !== inputValue.value) {
      inputValue.value = value;
      renderEditorValue(value, runtime.state.selectedMcps.value);
      runtime.setSelectedMcps(getEditorMcps());
    }
  });

  watch(
    () => props.mcpOptions,
    async () => {
      if (props.syncRuntime) {
        runtime.setSelectedMcps(getEditorMcps());
      }

      if (shouldReopenMcpDropdown.value) {
        shouldReopenMcpDropdown.value = false;
        mcpDropdownShow.value = false;
        mcpDropdownKey.value += 1;
        await nextTick();
        mcpDropdownShow.value = true;
      }
    }
  );

  function removeAttachment(attachmentId: string): void {
    const targetAttachment = attachments.value.find((attachment) => attachment.id === attachmentId);
    const previewUrl = targetAttachment?.metadata?.previewUrl;

    if (typeof previewUrl === 'string') {
      URL.revokeObjectURL(previewUrl);
    }
    runtime.removeAttachment(attachmentId);
  }

  function getFileKind(file: File): AiFileKind {
    if (file.type.startsWith('image/')) {
      return 'image';
    }

    return 'file';
  }

  function createLocalAttachment(file: File, status: AiChatAttachment['status'] = 'uploading'): AiChatAttachment {
    const kind = getFileKind(file);
    const previewUrl = kind === 'image' ? URL.createObjectURL(file) : undefined;

    return {
      id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      name: file.name,
      mimeType: file.type,
      size: file.size,
      kind,
      status,
      url: previewUrl,
      metadata: {
        file,
        previewUrl,
      },
    };
  }

  function updateAttachment(attachmentId: string, attachment: AiChatAttachment): void {
    runtime.setAttachments(
      attachments.value.map((item) => {
        if (item.id !== attachmentId) {
          return item;
        }

        return attachment;
      })
    );
  }

  const defaultMaxFileSize = 100 * 1024 * 1024;

  function validateFile(file: File): boolean {
    if (attachments.value.some((attachment) => attachment.name === file.name)) {
      Message.warning(t('crm.upload.repeatFileTip'));
      return false;
    }

    if (file.size > defaultMaxFileSize) {
      Message.warning(t('crm.upload.overSize', { size: 100, unit: 'MB' }));
      return false;
    }

    return true;
  }

  function toUploadedAttachment(file: File, id: string, previewUrl?: string): AiChatAttachment {
    const kind = getFileKind(file);

    return {
      id,
      name: file.name,
      mimeType: file.type,
      size: file.size,
      kind,
      status: 'done',
      url: kind === 'image' ? previewUrl || `${PreviewPictureUrl}/${id}` : undefined,
      metadata: {
        fileId: id,
        previewUrl: kind === 'image' ? previewUrl : undefined,
      },
    };
  }

  async function addSystemFiles(files: File[]): Promise<void> {
    const validFiles = files.filter((file) => validateFile(file));

    if (!validFiles.length) {
      return;
    }

    const localAttachments = validFiles.map((file) => createLocalAttachment(file, 'uploading'));

    runtime.setAttachments([...attachments.value, ...localAttachments]);

    try {
      const res = await uploadAgentChatFile(validFiles);
      const uploadedAttachments = validFiles.map((file, index) =>
        toUploadedAttachment(file, res.data[index], localAttachments[index].metadata?.previewUrl as string | undefined)
      );

      if (uploadedAttachments.some((attachment) => !attachment.id)) {
        throw new Error('Upload response id is empty');
      }

      localAttachments.forEach((localAttachment, index) => {
        updateAttachment(localAttachment.id, uploadedAttachments[index]);
      });
    } catch {
      localAttachments.forEach((localAttachment) => {
        updateAttachment(localAttachment.id, {
          ...localAttachment,
          status: 'error',
        });
      });
    }
  }

  async function addFiles(files: FileList | File[] | null | undefined) {
    await addSystemFiles(Array.from(files ?? []));
  }

  function insertPlainText(text: string): void {
    if (!text) {
      return;
    }

    const range = getEditorRange();
    const textNode = document.createTextNode(text);

    range.deleteContents();
    range.insertNode(textNode);
    setCaretAfter(textNode);
    syncEditorValue();
  }

  async function handleEditorPaste(event: ClipboardEvent) {
    const { clipboardData } = event;

    if (!clipboardData) {
      return;
    }

    const files = Array.from(clipboardData.items ?? [])
      .filter((item) => item.kind === 'file')
      .map((item) => item.getAsFile())
      .filter((file): file is File => Boolean(file));

    if (props.showAttachments && files.length) {
      event.preventDefault();
      await addFiles(files);
      return;
    }

    const plainText = clipboardData.getData('text/plain');

    if (plainText) {
      event.preventDefault();
      insertPlainText(plainText);
    }
  }

  const dragDepth = ref(0);
  const showDragMask = ref(false);
  function isFileDrag(event: DragEvent): boolean {
    return Boolean(props.showAttachments && event.dataTransfer?.types.includes('Files'));
  }

  function handleComposerDragEnter(event: DragEvent): void {
    if (!isFileDrag(event)) {
      return;
    }

    event.preventDefault();
    dragDepth.value += 1;
    showDragMask.value = true;
  }

  function handleComposerDragOver(event: DragEvent): void {
    if (!isFileDrag(event)) {
      return;
    }

    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'copy';
    }
  }

  function handleComposerDragLeave(event: DragEvent): void {
    if (!isFileDrag(event)) {
      return;
    }

    event.preventDefault();
    dragDepth.value = Math.max(0, dragDepth.value - 1);
    showDragMask.value = dragDepth.value > 0;
  }

  async function handleComposerDrop(event: DragEvent): Promise<void> {
    if (!isFileDrag(event)) {
      return;
    }

    event.preventDefault();
    dragDepth.value = 0;
    showDragMask.value = false;
    await addFiles(event.dataTransfer?.files ?? null);
  }

  function resetFileInput(input: HTMLInputElement | null): void {
    if (input) {
      input.value = '';
    }
  }

  async function handleFileInputChange(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;

    await addFiles(input.files);
    resetFileInput(input);
  }

  const fileInputRef = ref<HTMLInputElement | null>(null);
  function openFileSelector(): void {
    fileInputRef.value?.click();
  }

  async function retryAttachment(attachment: AiChatAttachment): Promise<void> {
    const file = attachment.metadata?.file;

    if (!(file instanceof File)) {
      return;
    }

    removeAttachment(attachment.id);
    await addFiles([file]);
  }

  /**
   * Composer 只负责输入交互。
   * 真正的消息追加、请求发送、流式更新都交给 Runtime。
   */
  async function handleSubmit(): Promise<void> {
    if (!canSubmit.value) {
      return;
    }

    const payload = getSubmitPayload();

    if (props.submitMode === 'emit') {
      emit('submit', payload);
    } else {
      await runtime.submit(payload);
    }
  }

  function handleCompositionStart(): void {
    isComposing.value = true;
  }

  function handleCompositionEnd(): void {
    isComposing.value = false;
    syncEditorValue();
  }

  function removeAdjacentMcp(event: KeyboardEvent): boolean {
    // MCP token 是 contenteditable=false，手动接管前后删除，保证按一次删除整个 token。
    if (event.key !== 'Backspace' && event.key !== 'Delete') {
      return false;
    }

    const selection = window.getSelection();
    if (!selection?.rangeCount || !selection.isCollapsed || !editorRef.value?.contains(selection.anchorNode)) {
      return false;
    }

    const range = selection.getRangeAt(0);
    const container = range.startContainer;
    const offset = range.startOffset;
    let targetNode: Node | null;

    if (event.key === 'Backspace') {
      targetNode = offset === 0 ? container.previousSibling : container.childNodes[offset - 1];
    } else {
      targetNode = container.childNodes[offset] ?? container.nextSibling;
    }

    if (!isMcpNode(targetNode)) {
      return false;
    }

    event.preventDefault();
    const nextCaretNode = document.createTextNode('');
    targetNode.replaceWith(nextCaretNode);
    setCaretAfter(nextCaretNode);
    syncEditorValue();
    return true;
  }

  async function handleKeydown(event: KeyboardEvent): Promise<void> {
    if (removeAdjacentMcp(event)) {
      return;
    }

    if (isComposing.value || event.isComposing) {
      return;
    }

    if (event.key !== 'Enter' || event.shiftKey || event.metaKey || event.ctrlKey) {
      return;
    }

    event.preventDefault();
    await handleSubmit();
  }

  onMounted(() => {
    renderEditorValue(
      inputValue.value,
      runtime.state.selectedMcps.value.length ? runtime.state.selectedMcps.value : props.initialMcps
    );
    emit('change', getSubmitPayload());
  });

  defineExpose({
    getSubmitPayload,
  });
</script>

<style scoped lang="scss">
  .ai-chat-composer {
    box-shadow: 0 4px 15px 2px #6467671a;
  }
  .ai-chat-composer__input {
    overflow-y: auto;
    min-width: 0;
    max-height: 132px;
    white-space: pre-wrap;
    color: var(--text-n1);
    outline: none;
    line-height: 26px;
    word-break: break-word;
    &:empty::before {
      color: var(--text-n4);
      content: attr(data-placeholder);
      pointer-events: none;
    }
  }
  :global(.ai-chat-mcp-token) {
    display: inline-flex;
    align-items: center;
    padding: 0 6px;
    height: 26px;
    border-radius: 4px;
    color: var(--primary-8);
    background: var(--primary-7);
    gap: 4px;
    line-height: 26px;
    vertical-align: top;
    user-select: all;
  }
  :global(.ai-chat-mcp-token svg) {
    flex: none;
    fill: currentcolor;
  }
  .ai-chat-tool-button,
  .ai-chat-mcp-button {
    padding: 2px 4px;
    height: 26px !important;
    border-radius: 4px;
    cursor: pointer;
    &:hover,
    &--active {
      background: var(--text-n9);
    }
    :deep(.n-button__content) {
      gap: 4px;
    }
  }
  .ai-chat-composer__drag-mask {
    position: absolute;
    z-index: 5;
    inset: 8px;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 1px dashed var(--primary-8);
    border-radius: 4px;
    background: color-mix(in srgb, var(--primary-7) 70%, transparent);
    pointer-events: none;
  }
</style>

<style lang="scss">
  .ai-chat-mcp-dropdown {
    overflow-y: auto;
    width: 280px;
    max-height: 320px;
    .n-dropdown-option-body {
      padding: 4px 0 !important;
      height: auto !important;
    }
    .n-dropdown-option-body__label {
      width: 100%;
      min-width: 0;
    }
  }
</style>

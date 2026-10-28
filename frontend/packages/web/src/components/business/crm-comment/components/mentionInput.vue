<template>
  <div class="crm-comment-mention-input">
    <div
      v-if="props.enableAttachment"
      class="flex h-[24px] items-center rounded-[3px_3px_0_0] border border-b-0 border-[var(--text-n7)] px-[8px]"
    >
      <n-tooltip trigger="hover" placement="top-start">
        <template #trigger>
          <n-upload
            v-model:file-list="fileList"
            class="crm-file-input-upload"
            :custom-request="uploadAttachment"
            multiple
            :show-file-list="false"
            :max="10"
            directory-dnd
            @before-upload="beforeUpload"
          >
            <CrmIcon type="iconicon_link1" :size="16" class="text-[var(--text-n4)]" />
          </n-upload>
        </template>
        {{ fileList.length === 10 ? t('crm.approval.fileLimitTip') : t('crmFormDesign.file') }}
      </n-tooltip>
    </div>
    <div class="relative">
      <div
        ref="editorRef"
        class="crm-comment-mention-input__editor"
        :class="{ 'crm-comment-mention-input__editor--with-attachment': props.enableAttachment }"
        :contenteditable="!props.disabled"
        :data-placeholder="placeholder"
        role="textbox"
        aria-multiline="true"
        @blur="handleEditorBlur"
        @input="handleEditorInput"
        @keydown="handleEditorKeydown"
        @paste="handleEditorPaste"
        @scroll="updateMentionMenuPosition"
      ></div>
      <div
        v-if="showMentionMenu"
        class="crm-comment-mention-input__menu absolute z-[2] w-[200px] overflow-hidden rounded-[var(--border-radius-small)] bg-[var(--text-n10)] py-[4px] shadow-[0_4px_12px_rgb(0_0_0_/_12%)]"
        :style="mentionMenuStyle"
        @mousedown.prevent
      >
        <div class="border-b border-[var(--text-n8)] px-[8px] py-[8px]">
          <n-checkbox v-model:checked="includeDisabled" @update:checked="handleIncludeDisabledChange">
            <span class="whitespace-nowrap text-[var(--text-n2)]">{{ t('common.showDisabledUsers') }}</span>
          </n-checkbox>
        </div>
        <n-scrollbar class="max-h-[216px]">
          <div
            v-for="(user, index) in userOptions"
            :key="user.id"
            :ref="(element) => setMentionOptionRef(element, index)"
            class="flex h-[32px] w-full items-center justify-between gap-[8px] border-0 bg-transparent px-[12px] text-left hover:bg-[var(--primary-7)]"
            :class="{ '!bg-[var(--primary-7)]': activeMentionIndex === index }"
            @click="handleMentionSelect(user)"
          >
            <span class="min-w-0 flex-1 truncate whitespace-nowrap">{{ user.name }}</span>
            <CrmTag v-if="user.enable === false" theme="light" size="small" tooltip-disabled class="shrink-0">
              {{ t('common.disabled') }}
            </CrmTag>
          </div>
          <div v-if="!loading && userOptions.length === 0" class="px-[12px] py-[8px] text-[var(--text-n4)]">
            {{ t('common.noData') }}
          </div>
        </n-scrollbar>
      </div>
    </div>
    <CrmFileList
      v-if="props.enableAttachment && fileList.length"
      :files="fileList as unknown as AttachmentInfo[]"
      class="mt-[8px]"
      @delete-file="deleteAttachment"
    />
    <div class="mt-[8px] flex justify-end gap-[8px]">
      <n-button :disabled="props.loading" secondary @click="handleCancel">
        {{ t('common.cancel') }}
      </n-button>
      <n-button
        type="primary"
        :disabled="!content.trim() || hasPendingAttachment"
        :loading="props.loading"
        @click="handleSubmit"
      >
        {{ props.submitText || t('crmComment.reply') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import {
    NButton,
    NCheckbox,
    NScrollbar,
    NTooltip,
    NUpload,
    type UploadCustomRequestOptions,
    type UploadFileInfo,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { filterMentionUsers, FOLLOW_COMMENT_MAX_LENGTH } from '@lib/shared/method/comment';
  import type { FollowCommentUser } from '@lib/shared/models/follow';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmFileList from '@/components/business/crm-file-list/index.vue';
  import type { AttachmentInfo } from '@/components/business/crm-form-create/types';

  import { getUserOptions, uploadTempAttachment } from '@/api/modules';

  const { t } = useI18n();
  const message = useMessage();

  const props = withDefaults(
    defineProps<{
      replyUserName?: string;
      submitText?: string;
      disabled?: boolean;
      loading?: boolean;
      includeDisabled?: boolean;
      initialMentionUsers?: FollowCommentUser[];
      initialAttachments?: AttachmentInfo[];
      enableAttachment?: boolean;
    }>(),
    {
      includeDisabled: false,
    }
  );

  const emit = defineEmits<{
    (event: 'submit', value: { content: string; mentionUserIds: string[]; attachmentIds: string[] }): void;
    (event: 'cancel'): void;
  }>();

  const content = defineModel<string>('value', {
    default: '',
  });

  const loading = ref(false);
  const includeDisabled = ref(props.includeDisabled);
  const rawUserOptions = ref<FollowCommentUser[]>([]);
  const selectedMentionUsers = ref<FollowCommentUser[]>([]);
  const editorRef = ref<HTMLElement>();
  const showMentionMenu = ref(false);
  const mentionMenuStyle = ref<Record<string, string>>({ left: '36px', top: '56px' });
  const activeMentionIndex = ref(0);
  const mentionOptionRefs = ref<HTMLElement[]>([]);
  const fileList = ref<UploadFileInfo[]>([]);
  const temporaryAttachmentIds = ref<Record<string, string>>({});
  const uploadingAttachmentIds = ref(new Set<string>());
  const hasPendingAttachment = computed(
    () => uploadingAttachmentIds.value.size > 0 || fileList.value.some((file) => file.status === 'error')
  );
  const lastKeyword = ref('');
  const userRequestId = ref(0);
  const hasLoadedUsers = ref(false);
  const loadedIncludeDisabled = ref(false);

  const userOptions = computed(() => {
    const keyword = lastKeyword.value.trim();
    return rawUserOptions.value.filter((user) => {
      if (!includeDisabled.value && user.enable === false) {
        return false;
      }
      return !keyword || user.name?.includes(keyword);
    });
  });

  const placeholder = computed(() => {
    if (props.replyUserName) {
      return t('crmComment.replyPlaceholder', { name: props.replyUserName });
    }
    return `${t('crmComment.commentPlaceholder')}@${t('crmComment.commentPlaceholderOthers')}`;
  });

  function isMentionToken(node: Node | null): node is HTMLElement {
    return node instanceof HTMLElement && node.classList.contains('crm-comment-mention-token');
  }

  function getEditorText() {
    return editorRef.value?.innerText.replace(/\n$/, '') || '';
  }

  function setCaretAfter(node: Node) {
    const range = document.createRange();
    const selection = window.getSelection();
    range.setStartAfter(node);
    range.collapse(true);
    selection?.removeAllRanges();
    selection?.addRange(range);
  }

  function getCaretTextOffset() {
    const editor = editorRef.value;
    const selection = window.getSelection();
    const anchorNode = selection?.anchorNode;
    if (!editor || !selection?.rangeCount || !selection.isCollapsed || !anchorNode || !editor.contains(anchorNode)) {
      return undefined;
    }

    const range = document.createRange();
    range.selectNodeContents(editor);
    range.setEnd(anchorNode, selection.anchorOffset);
    return range.toString().length;
  }

  function setCaretAtTextOffset(offset: number) {
    const editor = editorRef.value;
    if (!editor) return;

    const selection = window.getSelection();
    const range = document.createRange();
    let remainingOffset = offset;
    let targetNode: Node | undefined;

    Array.from(editor.childNodes).some((node) => {
      const textLength = node.textContent?.length || 0;
      if (remainingOffset > textLength) {
        remainingOffset -= textLength;
        return false;
      }
      targetNode = node;
      return true;
    });

    if (targetNode) {
      if (targetNode.nodeType === Node.TEXT_NODE) {
        range.setStart(targetNode, remainingOffset);
      } else {
        range.setStartAfter(targetNode);
      }
    } else {
      range.selectNodeContents(editor);
      range.collapse(false);
    }

    range.collapse(true);
    selection?.removeAllRanges();
    selection?.addRange(range);
  }

  function createMentionToken(user: FollowCommentUser) {
    const token = document.createElement('span');
    token.className = 'crm-comment-mention-token';
    token.contentEditable = 'false';
    token.dataset.userId = user.id;
    token.dataset.userName = user.name;
    token.textContent = `@${user.name}`;
    return token;
  }

  function getMatchedMention(value: string, index: number, users: FollowCommentUser[]) {
    return users.find((user) => {
      const mention = `@${user.name}`;
      const nextChar = value[index + mention.length];
      return value.startsWith(mention, index) && (!nextChar || /[@\s,，。.!！?？;；:：、]/.test(nextChar));
    });
  }

  function renderEditorValue(value = content.value) {
    const editor = editorRef.value;
    if (!editor) return;

    const users = [...selectedMentionUsers.value].sort((a, b) => b.name.length - a.name.length);
    const fragment = document.createDocumentFragment();
    let text = '';
    let index = 0;
    const appendText = () => {
      if (text) {
        fragment.appendChild(document.createTextNode(text));
        text = '';
      }
    };

    while (index < value.length) {
      const user = getMatchedMention(value, index, users);
      if (user) {
        appendText();
        fragment.appendChild(createMentionToken(user));
        index += user.name.length + 1;
      } else {
        text += value[index];
        index += 1;
      }
    }
    appendText();
    editor.replaceChildren(fragment);
  }

  function syncMentionUserIds() {
    selectedMentionUsers.value = filterMentionUsers(getEditorText(), selectedMentionUsers.value);
  }

  async function loadUsers() {
    if (hasLoadedUsers.value && (!includeDisabled.value || loadedIncludeDisabled.value)) {
      return;
    }

    const currentRequestId = userRequestId.value + 1;
    userRequestId.value = currentRequestId;
    const needIncludeDisabled = includeDisabled.value;

    try {
      loading.value = true;
      const users = (await getUserOptions({
        ...(needIncludeDisabled ? { includeDisabled: true } : {}),
      })) as FollowCommentUser[];

      if (currentRequestId !== userRequestId.value) {
        return;
      }

      rawUserOptions.value = users;
      hasLoadedUsers.value = true;
      loadedIncludeDisabled.value = needIncludeDisabled;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      if (currentRequestId === userRequestId.value) {
        loading.value = false;
      }
    }
  }

  function getCurrentMentionKeyword() {
    const editor = editorRef.value;
    const selection = window.getSelection();
    if (!editor || !selection?.rangeCount || !selection.isCollapsed || !editor.contains(selection.anchorNode)) {
      return undefined;
    }

    const childNodes: Node[] = Array.from(editor.childNodes);
    let currentNode: Node | null = selection.anchorNode === editor ? null : selection.anchorNode;
    while (currentNode?.parentNode && currentNode.parentNode !== editor) {
      currentNode = currentNode.parentNode;
    }

    const currentNodeIndex = currentNode ? childNodes.indexOf(currentNode) : -1;
    const nodesBeforeCaret =
      selection.anchorNode === editor
        ? childNodes.slice(0, selection.anchorOffset)
        : childNodes.slice(0, currentNodeIndex);
    const currentText =
      currentNode?.nodeType === Node.TEXT_NODE ? currentNode.textContent?.slice(0, selection.anchorOffset) || '' : '';

    let textAfterLastMention = '';
    [...nodesBeforeCaret, ...(currentNode ? [currentNode] : [])].forEach((node) => {
      if (isMentionToken(node)) {
        textAfterLastMention = '';
        return;
      }
      textAfterLastMention += node === currentNode ? currentText : node.textContent || '';
    });

    const matched = textAfterLastMention.match(/@([^\s@\n\r]*)$/);
    return matched ? matched[1] : undefined;
  }

  function handleMentionSelect(user: FollowCommentUser) {
    if (!selectedMentionUsers.value.some((item) => item.id === user.id)) {
      selectedMentionUsers.value.push({
        id: user.id,
        name: user.name,
        enable: user.enable,
      });
    }

    const value = getEditorText();
    const keyword = getCurrentMentionKeyword();
    const caretOffset = getCaretTextOffset();
    if (keyword === undefined || caretOffset === undefined) return;

    const mentionStart = caretOffset - keyword.length - 1;
    content.value = `${value.slice(0, mentionStart)}@${user.name} ${value.slice(caretOffset)}`.slice(
      0,
      FOLLOW_COMMENT_MAX_LENGTH
    );
    renderEditorValue();
    showMentionMenu.value = false;
    nextTick(() => {
      editorRef.value?.focus();
      setCaretAtTextOffset(mentionStart + user.name.length + 2);
    });
  }

  function setMentionOptionRef(element: unknown, index: number) {
    if (element instanceof HTMLElement) {
      mentionOptionRefs.value[index] = element;
    }
  }

  function scrollActiveMentionIntoView() {
    mentionOptionRefs.value[activeMentionIndex.value]?.scrollIntoView({ block: 'nearest' });
  }

  function changeActiveMention(step: number) {
    const count = userOptions.value.length;
    if (!count) return;
    activeMentionIndex.value = (activeMentionIndex.value + step + count) % count;
    nextTick(scrollActiveMentionIntoView);
  }

  function getMentionUserIds() {
    syncMentionUserIds();
    return selectedMentionUsers.value.map((user) => user.id);
  }

  function handleCancel() {
    content.value = '';
    editorRef.value?.replaceChildren();
    selectedMentionUsers.value = [];
    fileList.value = [];
    temporaryAttachmentIds.value = {};
    uploadingAttachmentIds.value.clear();
    emit('cancel');
  }

  function resetAttachments() {
    fileList.value = (props.initialAttachments || []).map((attachment) => ({
      ...attachment,
      status: 'finished' as const,
    }));
    temporaryAttachmentIds.value = {};
    uploadingAttachmentIds.value.clear();
  }

  function handleSubmit() {
    const trimmedContent = content.value.trim();
    if (!trimmedContent) {
      return;
    }

    emit('submit', {
      content: trimmedContent,
      mentionUserIds: getMentionUserIds(),
      attachmentIds: fileList.value.map((file) => temporaryAttachmentIds.value[file.id] || file.id),
    });
  }

  function beforeUpload({ file }: { file: UploadFileInfo }) {
    if (fileList.value.some((item) => item.name === file.name)) {
      message.warning(t('crm.upload.repeatFileTip'));
      return false;
    }
    if (file.file?.size && file.file.size > 50 * 1024 * 1024) {
      message.warning(t('crm.upload.overSize', { size: 50, unit: 'MB' }));
      return false;
    }
    return true;
  }

  function deleteAttachment(fileId: string) {
    fileList.value = fileList.value.filter((file) => file.id !== fileId);
    Object.entries(temporaryAttachmentIds.value).forEach(([localId, temporaryId]) => {
      if (localId === fileId || temporaryId === fileId) {
        delete temporaryAttachmentIds.value[localId];
        uploadingAttachmentIds.value.delete(localId);
      }
    });
  }

  async function uploadAttachment({ file, onFinish, onError }: UploadCustomRequestOptions) {
    const localFileId = file.id;
    uploadingAttachmentIds.value.add(localFileId);
    try {
      const result = await uploadTempAttachment(file.file);
      const [temporaryAttachmentId] = result.data;
      temporaryAttachmentIds.value[localFileId] = temporaryAttachmentId;
      onFinish();
      fileList.value = fileList.value.map((item) =>
        item.id === localFileId ? { ...item, id: temporaryAttachmentId, local: false } : item
      );
    } catch {
      onError();
      deleteAttachment(localFileId);
    } finally {
      uploadingAttachmentIds.value.delete(localFileId);
    }
  }

  function handleIncludeDisabledChange(value: boolean) {
    includeDisabled.value = value;
    loadUsers();
  }

  function getCaretPosition() {
    const selection = window.getSelection();
    if (!selection?.rangeCount || !selection.isCollapsed) return undefined;

    const range = selection.getRangeAt(0).cloneRange();
    const marker = document.createElement('span');
    marker.setAttribute('aria-hidden', 'true');
    marker.textContent = '\u200b';
    marker.style.cssText = 'display:inline-block;width:1px;height:1em;margin:0;padding:0;overflow:hidden;';
    range.insertNode(marker);
    const position = {
      left: marker.offsetLeft,
      top: marker.offsetTop,
      height: marker.offsetHeight,
    };
    marker.parentNode?.removeChild(marker);

    return position;
  }

  function updateMentionMenuPosition() {
    const editor = editorRef.value;
    if (!editor) return;

    const caretPosition = getCaretPosition();
    const menuWidth = 200;
    const left = Math.max(0, Math.min((caretPosition?.left ?? 36) - editor.scrollLeft, editor.clientWidth - menuWidth));
    const top = (caretPosition?.top ?? 36) - editor.scrollTop + (caretPosition?.height ?? 16) + 4;

    mentionMenuStyle.value = {
      left: `${left}px`,
      top: `${top}px`,
    };
  }

  function handleEditorInput() {
    const value = getEditorText();
    if (value.length > FOLLOW_COMMENT_MAX_LENGTH) {
      content.value = value.slice(0, FOLLOW_COMMENT_MAX_LENGTH);
      renderEditorValue();
      const lastNode = editorRef.value?.lastChild;
      if (lastNode) setCaretAfter(lastNode);
      return;
    }
    content.value = value;
    syncMentionUserIds();
    const keyword = getCurrentMentionKeyword();
    const openingMentionMenu = keyword !== undefined && !showMentionMenu.value;
    showMentionMenu.value = keyword !== undefined;
    if (keyword !== undefined) {
      if (openingMentionMenu) {
        activeMentionIndex.value = 0;
        nextTick(scrollActiveMentionIntoView);
      }
      nextTick(updateMentionMenuPosition);
    }
    if (keyword !== undefined && (keyword !== lastKeyword.value || !hasLoadedUsers.value)) {
      lastKeyword.value = keyword;
      loadUsers();
    }
  }

  function removeAdjacentMention(event: KeyboardEvent) {
    if (event.key !== 'Backspace' && event.key !== 'Delete') return false;
    const selection = window.getSelection();
    if (!selection?.rangeCount || !selection.isCollapsed || !editorRef.value?.contains(selection.anchorNode))
      return false;

    const range = selection.getRangeAt(0);
    const { startContainer: container, startOffset: offset } = range;
    const isTextNode = container.nodeType === Node.TEXT_NODE;
    let target: Node | null = null;

    if (isTextNode) {
      if (event.key === 'Backspace') {
        const textBeforeCaret = container.textContent?.slice(0, offset) || '';
        if (offset === 0 || /^\s*$/.test(textBeforeCaret)) {
          target = container.previousSibling;
        }
      } else if (offset === (container.textContent?.length || 0)) {
        target = container.nextSibling;
      }
    } else if (event.key === 'Backspace') {
      target = offset === 0 ? container.previousSibling : container.childNodes[offset - 1];
    } else {
      target = container.childNodes[offset] || container.nextSibling;
    }
    if (!isMentionToken(target)) return false;

    event.preventDefault();
    const previousNode = target.previousSibling;
    const nextNode = target.nextSibling;
    if (event.key === 'Backspace' && isTextNode && /^\s*$/.test(container.textContent || '')) {
      container.parentNode?.removeChild(container);
    } else if (event.key === 'Delete' && nextNode?.nodeType === Node.TEXT_NODE) {
      const text = nextNode.textContent || '';
      if (text.startsWith(' ')) {
        nextNode.textContent = text.slice(1);
      }
    }
    const caretNode = document.createTextNode('');
    target.replaceWith(caretNode);
    if (event.key === 'Backspace' && previousNode) {
      setCaretAfter(previousNode);
    } else {
      setCaretAfter(caretNode);
    }
    handleEditorInput();
    showMentionMenu.value = false;
    return true;
  }

  function handleEditorKeydown(event: KeyboardEvent) {
    if (showMentionMenu.value) {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        changeActiveMention(1);
        return;
      }
      if (event.key === 'ArrowUp') {
        event.preventDefault();
        changeActiveMention(-1);
        return;
      }
      if (event.key === 'Enter') {
        const user = userOptions.value[activeMentionIndex.value];
        if (user) {
          event.preventDefault();
          handleMentionSelect(user);
        }
        return;
      }
      if (event.key === 'Escape') {
        event.preventDefault();
        showMentionMenu.value = false;
        return;
      }
    }
    removeAdjacentMention(event);
  }

  function handleEditorPaste(event: ClipboardEvent) {
    const text = event.clipboardData?.getData('text/plain');
    if (!text) return;
    event.preventDefault();
    const selection = window.getSelection();
    if (!selection?.rangeCount) return;
    const range = selection.getRangeAt(0);
    const textNode = document.createTextNode(text);
    range.deleteContents();
    range.insertNode(textNode);
    setCaretAfter(textNode);
    handleEditorInput();
  }

  function handleEditorBlur() {
    syncMentionUserIds();
    window.setTimeout(() => {
      showMentionMenu.value = false;
    }, 100);
  }

  watch(
    () => props.initialMentionUsers,
    (users) => {
      selectedMentionUsers.value = users ? [...users] : [];
      nextTick(renderEditorValue);
    },
    { immediate: true }
  );

  watch(content, (value) => {
    if (value !== getEditorText()) {
      renderEditorValue(value);
    }
  });

  watch(userOptions, () => {
    activeMentionIndex.value = 0;
    mentionOptionRefs.value = [];
    nextTick(scrollActiveMentionIntoView);
  });

  watch(
    () => props.initialAttachments,
    () => {
      resetAttachments();
    },
    { immediate: true }
  );
</script>

<style scoped lang="less">
  .crm-file-input-upload {
    :deep(.n-upload-trigger) {
      @apply flex cursor-pointer items-center;
    }
  }
  .crm-comment-mention-input__editor {
    position: relative;
    overflow-x: hidden;
    overflow-y: auto;
    padding: 8px 12px;
    min-height: 90px;
    max-height: 180px;
    border: 1px solid var(--text-n7);
    border-radius: var(--border-radius-small);
    white-space: pre-wrap;
    color: var(--text-n1);
    outline: none;
    box-sizing: border-box;
    line-height: 22px;
    word-break: break-word;
    overflow-wrap: anywhere;
    cursor: text;
    &:empty::before {
      color: var(--text-n4);
      content: attr(data-placeholder);
      pointer-events: none;
    }
    &:focus {
      border-color: var(--primary-8);
    }
    &[contenteditable='false'] {
      cursor: not-allowed;
      background: var(--text-n9);
    }
  }
  .crm-comment-mention-input__editor--with-attachment {
    border-top-left-radius: 0;
    border-top-right-radius: 0;
  }
  :global(.crm-comment-mention-token) {
    display: inline;
    box-sizing: border-box;
    padding: 0 4px;
    border-radius: 4px;
    color: var(--text-n1);
    background: var(--text-n8);
    box-decoration-break: clone;
    box-decoration-break: clone;
    line-height: 20px;
    word-break: break-all;
    user-select: all;
  }
</style>

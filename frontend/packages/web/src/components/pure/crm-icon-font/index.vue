<template>
  <n-icon v-if="!isGradient" :size="props.size" :color="props.color">
    <svg>
      <use :xlink:href="`#${props.type}`"></use>
    </svg>
  </n-icon>

  <svg v-else :width="sizePx" :height="sizePx" aria-hidden="true" class="crm-icon-gradient-svg">
    <defs>
      <linearGradient :id="gradientId" :x1="x1" :y1="y1" :x2="x2" :y2="y2">
        <stop v-for="(s, i) in stops" :key="i" :offset="s.offset" :stop-color="s.color" />
      </linearGradient>
    </defs>
    <use :xlink:href="`#${props.type}`" :fill="`url(#${gradientId})`" />
  </svg>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { NIcon } from 'naive-ui';

  import '@/assets/icon-font/iconfont';

  const props = defineProps<{
    size?: number;
    type: string;
    color?: string;
  }>();

  const isGradient = computed(() => typeof props.color === 'string' && /gradient\(/i.test(props.color));

  const sizePx = computed(() => (props.size ? `${props.size}px` : '1em'));

  // generate a unique id for the gradient to avoid collisions
  const gradientId = `crm-icon-gradient-${props.type.replace(/[^a-zA-Z0-9_-]/g, '')}-${Math.random()
    .toString(36)
    .slice(2, 8)}`;

  function parseGradient(grad: string) {
    try {
      const inner = grad.replace(/^[^(]*\((.*)\)$/, '$1');
      const parts = inner.split(/,(?![^()]*\))/).map((p) => p.trim());
      // first part may be an angle
      let angle = 90; // default left->right
      const stopsRaw: string[] = [];
      if (/deg$/.test(parts[0]) || /rad$/.test(parts[0]) || /turn$/.test(parts[0])) {
        const a = parts.shift();
        if (a) angle = parseFloat(a) || 90;
      }
      parts.forEach((p) => stopsRaw.push(p));

      const stops = stopsRaw.map((s, idx) => {
        const m = s.match(/^(.*?)\s+([0-9.]+%?)$/);
        if (m) {
          return { color: m[1].trim(), offset: m[2].endsWith('%') ? m[2] : `${m[2]}%` };
        }
        // if no offset provided, distribute evenly
        const offset = `${Math.round((idx / Math.max(1, stopsRaw.length - 1)) * 100)}%`;
        return { color: s.trim(), offset };
      });

      // convert angle to gradient vector
      const a = (parseFloat(String(angle)) - 90) * (Math.PI / 180);
      const cx = 0.5;
      const cy = 0.5;
      const dx = Math.cos(a);
      const dy = Math.sin(a);
      const x1 = Math.round((cx - dx * 0.5) * 1000) / 1000;
      const y1 = Math.round((cy - dy * 0.5) * 1000) / 1000;
      const x2 = Math.round((cx + dx * 0.5) * 1000) / 1000;
      const y2 = Math.round((cy + dy * 0.5) * 1000) / 1000;

      return { stops, x1, y1, x2, y2 };
    } catch (e) {
      return null;
    }
  }

  const parsed = computed(() => {
    if (!isGradient.value || !props.color) return null;
    return parseGradient(props.color as string) || null;
  });

  const stops = computed(
    () =>
      parsed.value?.stops ?? [
        { color: '#000', offset: '0%' },
        { color: '#000', offset: '100%' },
      ]
  );
  const x1 = computed(() => (parsed.value ? parsed.value.x1 : 0));
  const y1 = computed(() => (parsed.value ? parsed.value.y1 : 0));
  const x2 = computed(() => (parsed.value ? parsed.value.x2 : 1));
  const y2 = computed(() => (parsed.value ? parsed.value.y2 : 0));
</script>

<style scoped>
  .crm-icon-gradient-svg {
    display: inline-block;
    vertical-align: middle;
  }
</style>

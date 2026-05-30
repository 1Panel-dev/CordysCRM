import { RouteRecordName, useRouter } from 'vue-router';

export default function useOpenNewPage() {
  const router = useRouter();

  function openNewPage(name: RouteRecordName | undefined, query: Record<string, any> = {}) {
    const queryParams = new URLSearchParams(query).toString();
    window.open(`${window.location.origin}#${router.resolve({ name }).fullPath}?${queryParams}`, '_blank');
  }

  return {
    openNewPage,
  };
}

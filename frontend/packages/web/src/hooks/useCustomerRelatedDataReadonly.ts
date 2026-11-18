import { inject, type InjectionKey, provide, type Ref } from 'vue';

import useUserStore from '@/store/modules/user';

const customerRelatedOwnerOnlyKey: InjectionKey<Readonly<Ref<boolean>>> = Symbol('customerRelatedOwnerOnly');

export function provideCustomerRelatedOwnerOnly(ownerOnly: Readonly<Ref<boolean>>) {
  provide(customerRelatedOwnerOnlyKey, ownerOnly);
}

export default function useCustomerRelatedDataReadonly() {
  const ownerOnly = inject(customerRelatedOwnerOnlyKey, undefined);
  const userStore = useUserStore();
  return (row?: { owner?: string }) => !!ownerOnly?.value && row?.owner !== userStore.userInfo.id;
}

import { MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';

export interface Option {
  label: string;
  value: string;
  children?: Option[];
}

export interface SelectedUsersItem {
  id: string;
  name: string;
  type: MemberSelectTypeEnum;
}

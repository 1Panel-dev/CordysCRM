export interface Option {
  label: string;
  value: string;
  children?: Option[];
}

export interface SelectedUsersParams {
  userIds: string[];
  roleIds: string[];
  deptIds: string[];
}

import { ModuleField } from '@lib/shared/models/customer';

export interface ProductListItem {
  id: string;
  name: string;
  status: string;
  price: number;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  createUserName: string;
  updateUserName: string;
  moduleFields: ModuleField[];
}

export interface SaveProductParams {
  name: string;
  moduleFields: ModuleField[];
}

export interface UpdateProductParams extends SaveProductParams {
  id: string;
}

export interface BatchUpdateProductParams {
  ids: (string | number)[];
  status: string;
  price: number | null;
  moduleFields: ModuleField[];
}

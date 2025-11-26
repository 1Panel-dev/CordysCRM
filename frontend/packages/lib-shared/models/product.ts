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

export interface UpdatePriceParams extends SaveProductParams {
  id: string;
  status: boolean;
}

export interface AddPriceParams extends SaveProductParams {
  status: boolean;
}

// 请求返回结构
export default interface CommonResponse<T> {
  code: number;
  message: string;
  messageDetail: string;
  data: T;
}

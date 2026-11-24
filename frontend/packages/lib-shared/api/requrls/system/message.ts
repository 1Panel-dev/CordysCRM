// 公告
export const GetAnnouncementListUrl = '/announcement/page'; // 公告列表分页查询
export const UpdateAnnouncementUrl = '/announcement/edit'; // 编辑公告
export const AddAnnouncementUrl = '/announcement/add'; // 新建公告
export const GetAnnouncementDetailUrl = '/announcement/get'; // 获取公告详情
export const DeleteAnnouncementUrl = '/announcement/delete'; // 删除公告

// 消息中心
export const GetNotificationListUrl = '/notification/list/all/page'; // 消息中心列表
export const GetNotificationCountUrl = '/notification/count'; // 具体类型具体状态的数量
export const SetNotificationReadUrl = '/notification/read'; // 设置消息已读
export const SetAllNotificationReadUrl = '/notification/read/all'; // 所有信息设置为已读消息

// 消息设置
export const GetMessageTaskSystemTypesUrl = '/message/task/sys';
export const GetMessageTaskCustomFormTypesUrl = '/message/task/custom-form';
export const GetMessageTaskPageUrl = '/message/task/page';
export const AddMessageTaskUrl = '/message/task/add';
export const UpdateMessageTaskUrl = '/message/task/update';
export const BatchUpdateMessageTaskReceiveMethodUrl = '/message/task/batch/update';
export const DeleteMessageTaskUrl = '/message/task/delete';
export const SubscribeMessageUrl = '/sse/subscribe'; // 客户端订阅 SSE 事件流
export const CloseMessageUrl = '/sse/close'; // 客户端关闭 SSE 事件流
export const GetHomeMessageUrl = '/notification/last/list'; // 获取首页消息列表
export const GetUnReadAnnouncement = '/notification/last/announcement/list'; // 获取用户未读公告列表
export const EnableMessageTaskUrl = '/message/task/enable'; // 启用/停用场景

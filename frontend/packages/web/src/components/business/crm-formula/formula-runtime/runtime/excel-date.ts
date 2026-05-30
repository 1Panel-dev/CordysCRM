const DAY_MS = 24 * 60 * 60 * 1000;
const EXCEL_EPOCH_UTC = Date.UTC(1899, 11, 30);

/**
 * 用“本地墙上时间”转 Excel serial
 * 不使用真实时区 instant 语义，避免历史时区偏差
 */
export function localDateToExcelSerial(date: Date): number {
  const utcLike = Date.UTC(
    date.getFullYear(),
    date.getMonth(),
    date.getDate(),
    date.getHours(),
    date.getMinutes(),
    date.getSeconds(),
    date.getMilliseconds()
  );

  return (utcLike - EXCEL_EPOCH_UTC) / DAY_MS;
}

/**
 * Excel serial -> “本地墙上时间” Date 容器
 * 后续必须配合 getUTC* 读取
 */
export function excelSerialToLocalDate(serial: number): Date {
  return new Date(EXCEL_EPOCH_UTC + serial * DAY_MS);
}

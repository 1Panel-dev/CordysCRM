import { ProcessStatusEnum } from "@lib/shared/enums/process";


export type ProcessStatusType = Exclude<ProcessStatusEnum, ProcessStatusEnum.VOIDED>;

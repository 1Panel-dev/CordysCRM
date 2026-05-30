// /formula-runtime/functions/index.ts
import { functionRegistry } from '../function-registry';
import { FnSpec } from '../types';
import AND from './and';
import CONCATENATE from './concatenate';
import DAYS from './days';
import IF from './if';
import IFS from './ifs';
import NOW from './now';
import SUM from './sum';
import TEXT from './text';
import TODAY from './today';

const BUILTIN_FUNCTIONS: Record<string, FnSpec> = {
  SUM: {
    source: 'builtin',
    fn: SUM,
  },

  DAYS: {
    source: 'builtin',
    fn: DAYS,
  },

  CONCATENATE: {
    source: 'builtin',
    fn: CONCATENATE,
  },

  IF: {
    source: 'builtin',
    lazy: true,
    fn: IF,
  },

  IFS: {
    source: 'builtin',
    lazy: true,
    fn: IFS,
  },

  AND: {
    source: 'builtin',
    lazy: true,
    fn: AND,
  },

  TODAY: {
    source: 'builtin',
    fn: TODAY,
  },

  NOW: {
    source: 'builtin',
    fn: NOW,
  },

  TEXT: {
    source: 'builtin',
    fn: TEXT,
  },
};

let initialized = false;

export default function registerBuiltinFunctions() {
  if (initialized) return;

  functionRegistry.batchRegister(BUILTIN_FUNCTIONS);
  initialized = true;
}

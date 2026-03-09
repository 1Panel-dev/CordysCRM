import { FnSpec } from './types';

export class FunctionRegistry {
  private functions = new Map<string, FnSpec>();

  private normalizeName(name: string): string {
    return (name || '').trim().toUpperCase();
  }

  register(name: string, spec: FnSpec): void {
    const normalizedName = this.normalizeName(name);

    if (!normalizedName) {
      throw new Error('[FunctionRegistry] function name is required');
    }

    this.functions.set(normalizedName, spec);
  }

  batchRegister(definitions: Record<string, FnSpec>): void {
    Object.entries(definitions).forEach(([name, spec]) => {
      this.register(name, spec);
    });
  }

  get(name: string): FnSpec | undefined {
    return this.functions.get(this.normalizeName(name));
  }

  has(name: string): boolean {
    return this.functions.has(this.normalizeName(name));
  }

  unregister(name: string): void {
    this.functions.delete(this.normalizeName(name));
  }

  clear(): void {
    this.functions.clear();
  }

  keys(): string[] {
    return Array.from(this.functions.keys());
  }

  entries(): Array<[string, FnSpec]> {
    return Array.from(this.functions.entries());
  }
}

export const functionRegistry = new FunctionRegistry();

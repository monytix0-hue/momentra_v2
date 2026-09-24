import { AsyncLocalStorage } from 'node:async_hooks';

const requestUser = new AsyncLocalStorage<string>();

export function runWithRequestUser<T>(userId: string, fn: () => T): T {
  return requestUser.run(userId, fn);
}

/** User id for the in-flight /v1 request, if auth middleware installed one. */
export function currentRequestUserId(): string | undefined {
  const userId = requestUser.getStore();
  return userId && userId.length > 0 ? userId : undefined;
}

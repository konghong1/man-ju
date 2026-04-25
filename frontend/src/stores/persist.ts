import type { PiniaPluginContext } from "pinia";

const STORAGE_PREFIX = "manju:store:";

function safeParse(value: string) {
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}

export function localStoragePersistPlugin({ store }: PiniaPluginContext) {
  const key = STORAGE_PREFIX + store.$id;
  const cached = localStorage.getItem(key);

  if (cached) {
    const parsed = safeParse(cached);
    if (parsed && typeof parsed === "object") {
      store.$patch(parsed);
    }
  }

  store.$subscribe((_mutation, state) => {
    localStorage.setItem(key, JSON.stringify(state));
  });
}

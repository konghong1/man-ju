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
  if (typeof localStorage === "undefined") {
    return;
  }

  const key = STORAGE_PREFIX + store.$id;
  let storageAvailable = true;

  try {
    const cached = localStorage.getItem(key);
    if (cached) {
      const parsed = safeParse(cached);
      if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
        store.$patch(parsed);
      }
    }
  } catch {
    storageAvailable = false;
  }

  store.$subscribe((_mutation, state) => {
    if (!storageAvailable) {
      return;
    }

    try {
      localStorage.setItem(key, JSON.stringify(state));
    } catch {
      storageAvailable = false;
    }
  });
}

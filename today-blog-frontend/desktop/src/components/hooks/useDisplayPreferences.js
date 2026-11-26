import { useSyncExternalStore } from 'react';
import { preferencesSnapshot, subscribePreferences } from '../../../../common/visitor-preferences';

export function useDisplayPreferences() {
  return useSyncExternalStore(subscribePreferences, preferencesSnapshot);
}

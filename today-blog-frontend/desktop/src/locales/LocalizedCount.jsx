import React from 'react';
import { countMessage } from './index';

export default function LocalizedCount({ count, messageKey }) {
  const [before, after] = countMessage(messageKey, count).split('{count}');
  return <>{before}<b className="red">{count}</b>{after}</>;
}

import React from 'react';
import './RouteLoading.css';

export default function RouteLoading() {
  return (
    <div className="route-loading" role="status" aria-live="polite" aria-label="页面加载中">
      <div className="route-loading__content">
        <div className="route-loading__brand" aria-hidden="true">
          <span className="route-loading__brand-mark">T</span>
          <span className="route-loading__brand-line" />
        </div>
        <div className="route-loading__title" aria-hidden="true" />
        <div className="route-loading__meta" aria-hidden="true" />
        <div className="route-loading__body" aria-hidden="true">
          <span />
          <span />
          <span />
        </div>
        <span className="route-loading__label">页面加载中</span>
      </div>
    </div>
  );
}

/*
 * Copyright 2017 - 2026 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.maintenance;

import infra.context.ApplicationEventPublisher;
import infra.context.ApplicationListener;
import infra.context.availability.ApplicationAvailability;
import infra.context.availability.ReadinessState;
import infra.lang.Descriptive;
import infra.stereotype.Component;
import lombok.RequiredArgsConstructor;

/**
 * 服务运行状态。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/14
 */
@Component
@RequiredArgsConstructor
public class ServerStatus implements Descriptive, ApplicationListener<MaintenanceStatusChangeEvent> {

  private final ApplicationAvailability availability;

  private final ApplicationEventPublisher eventPublisher;

  private volatile MaintenanceStatus maintenanceStatus = MaintenanceStatus.DISABLED;

  public boolean isShuttingDown() {
    return availability.getReadinessState() == ReadinessState.REFUSING_TRAFFIC;
  }

  public boolean isReady() {
    return availability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC;
  }

  public boolean isMaintenance() {
    return maintenanceStatus.enabled();
  }

  public MaintenanceStatus getMaintenanceStatus() {
    return maintenanceStatus;
  }

  public void setMaintenanceStatus(MaintenanceStatus maintenanceStatus) {
    this.maintenanceStatus = maintenanceStatus;
    eventPublisher.publishEvent(new MaintenanceStatusChangeEvent(this, maintenanceStatus));
  }

  @Override
  public String getDescription() {
    return isReady() ? "UP" : (isShuttingDown() ? "DOWN" : "STARTING");
  }

  @Override
  public String toString() {
    return "Status: " + getDescription();
  }

  @Override
  public void onApplicationEvent(MaintenanceStatusChangeEvent event) {
    this.maintenanceStatus = event.getStatus();
  }

}


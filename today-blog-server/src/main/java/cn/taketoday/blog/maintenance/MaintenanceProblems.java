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

import cn.taketoday.blog.problem.Problem;
import infra.http.HttpStatus;

/**
 * 维护
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
public interface MaintenanceProblems {

  Problem MAINTENANCE = Problem.of("maintenance.under_maintenance", HttpStatus.SERVICE_UNAVAILABLE);

  Problem IN_PROGRESS = Problem.of("maintenance.upgrade.in_progress", HttpStatus.CONFLICT);
  Problem START_FAILED = Problem.of("maintenance.upgrade.start_failed", HttpStatus.INTERNAL_SERVER_ERROR);
  Problem SPACE_INSUFFICIENT = Problem.of("maintenance.upgrade.space_insufficient", HttpStatus.CONFLICT);
  Problem DEPLOYMENT_REQUIRED = Problem.of("maintenance.upgrade.deployment_required", HttpStatus.CONFLICT);
  Problem ENVIRONMENT_NOT_READY = Problem.of("maintenance.upgrade.environment_not_ready", HttpStatus.CONFLICT);

  Problem PACKAGE_IN_USE = Problem.of("maintenance.upgrade.package.in_use", HttpStatus.CONFLICT);
  Problem PACKAGE_INVALID = Problem.of("maintenance.upgrade.package.invalid", HttpStatus.BAD_REQUEST);
  Problem PACKAGE_CONSUMED = Problem.of("maintenance.upgrade.package.consumed", HttpStatus.CONFLICT);
  Problem PACKAGE_NOT_FOUND = Problem.of("maintenance.upgrade.package.not_found", HttpStatus.NOT_FOUND);
  Problem PACKAGE_NAME_INVALID = Problem.of("maintenance.upgrade.package.name_invalid", HttpStatus.BAD_REQUEST);
  Problem PACKAGE_NOTE_INVALID = Problem.of("maintenance.upgrade.package.note_invalid", HttpStatus.BAD_REQUEST);
  Problem PACKAGE_SIZE_INVALID = Problem.of("maintenance.upgrade.package.size_invalid", HttpStatus.BAD_REQUEST);

  Problem MAPPING_INVALID = Problem.of("maintenance.upgrade.mapping.invalid", HttpStatus.BAD_REQUEST);

  Problem RECORD_NOT_FOUND = Problem.of("maintenance.upgrade.record.not_found", HttpStatus.NOT_FOUND);

}

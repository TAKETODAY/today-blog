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

package cn.taketoday.blog.cli;

import java.io.IOException;

import cn.taketoday.blog.updater.UpgradeFiles;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.jdbc.RepositoryManager;
import infra.persistence.platform.Platform;
import infra.persistence.support.DefaultEntityManager;
import infra.transaction.support.TransactionTemplate;

/**
 * 只在升级命令中加载数据库配置，不为短生命周期的 CLI 创建连接池。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
@Configuration(proxyBeanMethods = false)
class UpgradePersistenceConfiguration {

  @Bean
  @Lazy
  UpgradeTaskStore upgradeTaskStore(UpgradeFiles files) throws IOException {
    var database = UpgradeDatabase.read(UpgradeDatabase.DEFAULT_CONFIG, files);
    var repositoryManager = new RepositoryManager(database.url(), database.user(), database.password());
    var entityManager = new DefaultEntityManager(repositoryManager, Platform.mysql());
    return new EntityUpgradeTaskStore(entityManager, new TransactionTemplate(repositoryManager.getTransactionManager()));
  }

}

/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.blog.repository;

import org.apache.ibatis.annotations.Param;

import java.util.List;

import cn.taketoday.blog.model.Attachment;
import cn.taketoday.blog.model.form.AttachmentForm;
import cn.taketoday.stereotype.Repository;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 14:48
 */
@Repository
public interface AttachmentRepository extends DefaultRepository<Attachment, Long> {

  int getRecordFilter(@Param("args") AttachmentForm form);

  List<Attachment> filter(@Param("args") AttachmentForm form,
          @Param("pageNow") int pageNow, @Param("pageSize") int pageSize);
}

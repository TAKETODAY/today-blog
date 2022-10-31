package cn.taketoday.blog.repository;

import org.apache.ibatis.annotations.Param;

import cn.taketoday.blog.model.Operation;
import cn.taketoday.stereotype.Repository;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 18:47
 */
@Repository
public interface LoggerRepository extends DefaultRepository<Operation, Long> {

  void deleteByIds(@Param("ids") long[] ids);

}

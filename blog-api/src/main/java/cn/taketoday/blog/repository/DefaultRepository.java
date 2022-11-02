package cn.taketoday.blog.repository;

import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-10 19:40
 */
public interface DefaultRepository<M, ID extends Serializable> {

  /**
   * Save entity to database
   *
   * @param model entity bean
   * @return
   */
  void save(M model);

  /**
   * Save entities to database
   *
   * @param models entity beans
   */
  void saveAll(@Param("models") Collection<M> models);

  /**
   * Save entities to database
   *
   * @param model
   */
  void saveSelective(M model);

  /**
   * @param model
   */
  void delete(M model);

  /**
   * @param model
   */
  void deleteAll(@Param("models") Collection<M> models);

  void deleteAll();

  /**
   * @param id
   */
  void deleteById(ID id);

  /**
   * @param model
   */
  void update(M model);

  /**
   * @param models
   */
  void updateAll(@Param("models") Collection<M> models);

  int getTotalRecord();

  M findById(ID id);

  List<M> findAll();

  List<M> find(@Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

  List<M> findLatest();
}

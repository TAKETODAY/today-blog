package cn.taketoday.blog.utils;

import com.aliyun.oss.OSSClient;

import java.io.File;

import cn.taketoday.blog.config.OssConfig;
import cn.taketoday.stereotype.Singleton;

/**
 * @author TODAY 2021/3/31 21:37
 * @since 3.0
 */
@Singleton
public class RemoteFileOperations {

  private final OssConfig config;
  private volatile OSSClient ossClient;

  public RemoteFileOperations(OssConfig config) {
    this.config = config;
  }

  public boolean isOssEnabled() {
    return config.isEnabled();
  }

  public void removeFile(String filePath) {
    obtainClient().deleteObject(config.getBucket(), getLocation(filePath));
  }

  public void uploadFile(String location, File destFile) {
    obtainClient().putObject(config.getBucket(), getLocation(location), destFile);
  }

  public boolean exists(String location) {
    return obtainClient().doesObjectExist(config.getBucket(), getLocation(location));
  }

  OSSClient obtainClient() {
    OSSClient ossClient = this.ossClient;
    if (ossClient == null) {
      synchronized(this) {
        ossClient = this.ossClient;
        if (ossClient == null) {
          ossClient = config.newClient();
          this.ossClient = ossClient;
        }
      }
    }
    return ossClient;
  }

  protected String getLocation(String key) {
    if (key.startsWith("/")) {
      key = key.substring(1);
    }
    return key;
  }

}
